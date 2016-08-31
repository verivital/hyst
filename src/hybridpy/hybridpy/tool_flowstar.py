'''Uses flow* to run reachability and gnuplot / gimp to make a plot'''

import subprocess
import re
import time
from hybridpy.hybrid_tool import HybridTool
from hybridpy.hybrid_tool import run_check_stderr
from hybridpy.hybrid_tool import RunCode
from hybridpy.hybrid_tool import tool_main

class FlowstarTool(HybridTool):
    '''container class for running Flow*'''
    def __init__(self):
        HybridTool.__init__(self, 'flowstar', '.flowstar', 'flowstar')

    def _run_tool(self):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        # flowstar reads from stdin
        try:
            f = open(self.model_path, 'r')
        except IOError as e:
            print "Could not read from model file: " + self.model_path + " (" + e.strerror + ")"
            rv = RunCode.ERROR

        with f:
            # run through stdbuf to prevent flow* stdout i/o buffering
            if not run_check_stderr(["stdbuf", "-oL", self.tool_path], stdin=f):
                print "Error running flowstar tool"
                rv = RunCode.ERROR

        return rv

    def _make_image(self):
        '''makes the image after the tool runs, returns True on success'''
        rv = True
        print "Plotting with gnuplot..."

        try:
            exit_code = subprocess.call(["gnuplot", "outputs/out.plt"])

            if exit_code != 0:
                print "Gnuplot errored; exit code: " + str(exit_code)
                rv = False

        except OSError as e:
            print "Exception while trying to run gnuplot: " + str(e)
            rv = False

        # run gimp to convert eps to png
        if rv:
            print "Converting output using GIMP..."

            script_fu = '(gimp-file-load RUN-NONINTERACTIVE "images/out.eps" "images/out.eps")'
            script_fu += '(gimp-image-rotate 1 ROTATE-90)'
            script_fu += '(gimp-file-save RUN-NONINTERACTIVE 1 (car (gimp-image-get-active-layer 1)) "' \
                    + self.image_path + '" "' + self.image_path + '")'
            script_fu += '(gimp-quit TRUE)'

            exit_script_fu = '(gimp-quit TRUE)' # this is done separately in case the first part errors
            # gimp -i -b <script>

            params = ["gimp", "-i", "-b", script_fu, "-b", exit_script_fu]

            if not run_check_stderr(params):
                print "Gimp errored"
                rv = False

        return rv

    def parse_output(self, dummy_directory, lines, dummy_hypy_out):
        '''Parses the tool's output and returns a python object

        The result object is an ordered dictionary, with:
        'terminated' -> True/False   <-- did errors occur during computation (was 'terminated' printed?)
        'mode_times' -> [(mode1, time1), ...]  <-- list of reach-tmes computed in each mode, in order
        'result' -> 'UNKOWN'/'SAFE'/None  <-- if unsafe set is used and 'terminated' is false, this stores 
                                              the text after "Result: " in stdout
        '''

        rv = {'terminated': None, 'mode_times': None, 'result': None}

        # terminated
        rv['terminated'] = False
        
        for line in reversed(lines):
            if 'terminated' in line.lower():
                rv['terminated'] = True

            if line.startswith('Result: '):
                rest = line[8:]
                rv['result'] = rest

        # force result to None if the tool was terminated early
        if rv['terminated'] == True:
            rv['result'] = None

        # mode_times
        mode_times = []
        step_regexp = re.compile('mode: ([^,]*).*?step = ([^,]*)')
        step_nomode_regexp = re.compile('.*?step = ([^,]*)')

        next_mode_string = 'Dealing with the jump from'
        end_string = "time cost"

        step_total = 0.0
        cur_mode = None
        
        for line in lines:
            if next_mode_string in line or end_string in line:
                if cur_mode is not None:
                    mode_times.append((cur_mode, step_total))

                cur_mode = None
                step_total = 0.0
            else:
                # not a jump, check if it's a step statement
                res = step_regexp.match(line)

                if res is not None:
                    (cur_mode, t) = res.groups()
                    step_total += float(t)
                else:
                    # try to match step without mode name
                    res = step_nomode_regexp.match(line)

                    if res is not None:
                        t = res.group(1)
                        cur_mode = ""
                        step_total += float()
        
        rv['mode_times'] = mode_times

        return rv
                
if __name__ == "__main__":
    tool_main(FlowstarTool())

