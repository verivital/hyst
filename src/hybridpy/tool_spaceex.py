'''Run SpaceEx and make a plot module for HyPy
'''

import sys
import subprocess
import threading
import os
import shutil
import re
import collections

from hybrid_tool import HybridTool
from hybrid_tool import RunCode
from hybrid_tool import get_script_path
from hybrid_tool import tool_main

# the path to the spaceex executable
TOOL_PATH = get_script_path() + "/spaceex/spaceex"

class SpaceExTool(HybridTool):
    '''Container class for running SpaceEx'''

    original_cfg_path = None
    cfg_path = None

    printed_error = False
    not_affine = False

    def __init__(self):
        HybridTool.__init__(self, "spaceex", '.xml', TOOL_PATH)

    def _print_pipe(self, is_std_err, pipe):
        '''print the output from a pipe (from a subprocess), setting
        class variables if non-affine dynamics or stderr is printed to
        '''
        err_line = None

        while True:
            line = pipe.readline()

            if line == '':
                break

            if is_std_err:
                print "[stderr] ",

            print line,

            if is_std_err:
                sys.stderr.flush()
            else:
                sys.stdout.flush()

            if "following are not affine dynamics" in line:
                self.not_affine = True
                self.printed_error = True
                err_line = None

            if self.printed_error == False and is_std_err and len(line) > 0:
                self.printed_error = True
                err_line = line

        if err_line != None:
            print "stderr output detected: '" + err_line + "'"

    def _run_tool(self):
        '''actually run the subprocess, parsing stdout and stderr appropriately
        returns a value in RunCode
        '''
        rv = RunCode.SUCCESS

        # TOOL_PATH -m $MODELNAME.xml --config $MODELNAME.cfg --output-file plots/$MODELNAME-plot.txt -v d
        params = [self.tool_path, '-m', self.model_path, '--config', self.cfg_path, '--output-file', \
                  'plotdata.txt', '-v', 'd']

        try:
            proc = subprocess.Popen(params, stderr=subprocess.PIPE, stdout=subprocess.PIPE)

            self.printed_error = False
            self.not_affine = False

            p1 = threading.Thread(target=self._print_pipe, args=(False, proc.stdout,))
            p1.start()
            p2 = threading.Thread(target=self._print_pipe, args=(True, proc.stderr,))
            p2.start()

            p1.join()
            p2.join()

            if self.not_affine:
                print "SpaceEx exited due to non-affine dynamics."
                rv = RunCode.SKIP
            elif self.printed_error:
                print "Error was printed to stdout or stderr"
                rv = RunCode.ERROR

            proc.wait()

            print "SpaceEx return code was " + str(proc.returncode)

            if rv == RunCode.SUCCESS: # exit code wasn't set during output
                if proc.returncode != 0:
                    print "Unknown return code"
                    rv = RunCode.ERROR
                else:
                    print "SpaceEx successfully completed."

        except OSError as e:
            print "OSError while trying to run " + str(params) + "\nError: " + str(e)
            rv = RunCode.ERROR

        return rv

    def _make_image(self):
        rv = True

        # graph -T "ps" -BC -q 0.5 plots/$MODELNAME-plot.txt > plots/$MODELNAME-plot.ps
        params = ["graph", "-T", "png", "-BC", "-q", "0.25", "plotdata.txt"]

        print "Plotting with command 'graph' (plotutils), params: %s"%str(params)

        try:
            fout = open(self.image_path, "w")
            code = subprocess.call(params, stdout=fout)

            if code != 0:
                rv = False
                print "Error, return code from plot was:", code

        except OSError as e:
            print "Exception while writing image path: " + str(e)
            rv = False

        if rv:
            print "Plot succeeded"
        else:
            print "Plot errored"

        return rv

    def load_args(self, args):
        '''initialize the class from a namespace (result of ArgumentParser.parse_args())'''
        HybridTool.load_args(self, args)

        # also setup the extra cfg path
        cfg_path = args.cfg

        if cfg_path is None:
            cfg_path = os.path.splitext(self.original_model_path)[0] + ".cfg"

        cfg_path = os.path.realpath(cfg_path)

        if not os.path.exists(cfg_path):
            raise RuntimeError('cfg file not found at path: ' + self.original_cfg_path)

        self.original_cfg_path = cfg_path

    def _copy_model(self, temp_folder):
        '''copy the model to the temp folder and set class path variables (override)'''

        HybridTool._copy_model(self, temp_folder)

        self.cfg_path = self.model_path + ".cfg"

        shutil.copyfile(self.original_cfg_path, self.cfg_path)

    def create_output(self, directory):
        '''Assigns to the output object (self.output_obj)
        For SpaceEx, this expects output-format=INTV

        The result object is an ordered dictionary, with
        'lines' -> [(line1, timestamp1), ...]                    <-- stdout lines (automatically created)
        'variables'->{var1 -> (min, max), ...}                   <-- range of each variable in all locations
        'locations'->{loc1 -> {var1 -> (min, max), ...}, ...}    <-- range of each variable in each location
        'fixpoint' -> True/False   <-- True if 'Found fixpoint after' found on stdout
                                       False if 'without finding fixpoint' found on stdout
                                       unassigned if neither
        '''

        ### create 'fixpoint' from stdout
        for (line, _) in reversed(self.output_obj['lines']):
            if 'Found fixpoint after' in line:
                self.output_obj['fixpoint'] = True
                break
            elif 'without finding fixpoint' in line:
                self.output_obj['fixpoint'] = False
                break
        
        ### create 'variables' and 'locations' from plotdata.txt
        filename = directory + '/plotdata.txt'
        var = collections.OrderedDict() # map from var_name -> (min, max)
        loc = collections.OrderedDict() # map from loc_name->{map from var_name -> (min,max)}

        # state machine when reading file
        state_init = 0
        state_vars = 1
        state_locs = 2
        state = state_init

        with open(filename, 'r') as f:
            for line in f:
                line = line.strip() # remove trailing newline
                
                if len(line) == 0: # skip blank lines
                    continue

                if state == state_init:
                    if line.startswith('Bounds on the variables over the entire set'):
                        state = state_vars
                    elif line.startswith('empty set'):
                        state = state_locs # prevents error that file format was wrong
                        break
                elif state == state_vars:
                    if line.startswith('Location-wise bounds on the variables'):
                        state = state_locs
                    else:
                        self._read_var_line(line, var)
                elif state == state_locs:
                    self._read_loc_line(line, loc)

        if state == state_init:
            raise RuntimeError('Format of plotdata.txt was wrong. Did you use output-format=INTV?')

        # assign to output object (don't overwrite object)
        self.output_obj['locations'] = loc
        self.output_obj['variables'] = var

    # line = 'Location: loc(Heater)==heater_on & loc(Controller)==controller_on'
    loc_re = re.compile(r'loc\([^)]*\)==([^ ]+)') # use with findall
    loc_obj = None
        
    def _read_loc_line(self, line, result):
        '''read a line in the result file in the location section. Parse it and store in result.'''

        if line.startswith("Location:"):
            parts = self.loc_re.findall(line)

            if len(parts) == 0:
                raise RuntimeError("Error parsing location name in result line: " + line)

            loc_name = ".".join(parts)
            self.loc_obj = {}
            result[loc_name] = self.loc_obj
        else:
            self._read_var_line(line, self.loc_obj)
        
    # sys1.t: [3.89966e-15,50.0001]
    var_re = re.compile(r'([^.]+)\.([^:]+)\: \[([^,]+),([^\]]+)\]')
        
    def _read_var_line(self, line, result):
        '''read a line in the result file in the variables section. Parse it and store in result.'''        

        if line.startswith("SPACETIME_TIME"):
            pass
        else:
            m = self.var_re.match(line)

            if m is None or len(m.groups()) != 4:
                raise RuntimeError("Variable regular expression result didn't match line: " + line)

            var = m.group(2)
            imin = float(m.group(3))
            imax = float(m.group(4))

            result[var] = (imin, imax)

def _main():
    '''main func for running SpaceEx'''
    
    extra_args = [('-cfg', 'cfg file')]
    tool_main(SpaceExTool(), extra_args)

if __name__ == "__main__":
    _main()
