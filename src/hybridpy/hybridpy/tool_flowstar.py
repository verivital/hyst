'''Uses flow* to run reachability and gnuplot / gimp to make a plot'''

import subprocess
import re
import os
from hybridpy.hybrid_tool import HybridTool
from hybridpy.hybrid_tool import run_check_stderr
from hybridpy.hybrid_tool import RunCode
from hybridpy.hybrid_tool import tool_main

class FlowstarTool(HybridTool):
    '''container class for running Flow*'''
    def __init__(self):
        HybridTool.__init__(self, 'flowstar', '.flowstar', 'flowstar')

    def _run_tool(self, image_requested):
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

    def parse_output(self, directory, lines, _hypy_out):
        '''
        Parses the tool's output and returns a python object.

        The result object is an ordered dictionary, with:
        'terminated' -> True/False   <-- did errors occur during computation (was 'terminated' printed?)
        'mode_times' -> [(mode1, time1), ...]  <-- list of reach-tmes computed in each mode, in order
        'result' -> 'UNKNOWN'/'SAFE'/None  <-- if unsafe set is used and 'terminated' is false, this stores 
                                              the text after "Result: " in stdout
        'safe' -> True iff 'result' == 'SAFE'
        'gnuplot_oct_data' -> the octogon data in the tool's gnuplot output
        'reachable_taylor_models' -> a list of TaylorModel objects parsed from out.flow
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
        if rv['terminated'] is True:
            rv['result'] = None
        else:
            rv['gnuplot_oct_data'] = parse_gnuplot_data(os.path.join(directory, 'outputs', 'out.plt'))
            rv['reachable_taylor_models'] = parse_taylor_model_data(os.path.join(directory, 'outputs', 'out.flow'))
            
        rv['safe'] = rv['result'] == 'SAFE'

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

def parse_gnuplot_data(filename):
    'extact the gnuplot octogon data from the file and return a string object'

    # octogon data starts after line: 'plot '-' notitle with lines ls 1'
    started = False
    rv = ""

    with open(filename, "r") as f:
        for line in f.readlines():
            if started:
                if not line.startswith("e"): # skip end character
                    rv += line
            elif line.startswith('plot'):
                started = True

    return rv

class TaylorModel(object):
    '''
    A taylor model constrainer extracted from out.flow

    These can be used to create initial states for subsequent Flow* calls
    '''
    
    def __init__(self, mode_name, tm_var_line, model_definition):
        self.mode_name = mode_name
        self.tm_var_line = tm_var_line
        self.model_definition = model_definition

    def as_initial_state(self):
        '''
        return this taylor model as an initial state for use with the 
        '-taylor_init' flag (doesn't include the mode path)
        '''

        # newlines should be replaced by a colon (':')

        rv = self.tm_var_line + "::"
        rv += self.model_definition.replace("\n", ":")

        return rv

def remove_intervals_from_tm(string, tol=1e-9):
    '''
    remove small intervals from the model definition. This is for compatibility
    with the input format expected by Flow* 2.0.0. This function will change
    '[4.500, 4.500]' to '4.500'.

    Returns the modified string.
    '''

    search_start = 0
    start = string.find('[', search_start)
    
    while start != -1:
        comma = string.find(',', start)
        end = string.find(']', start)

        if end == len(string) - 1 or string[end + 1] == '\n':
            # don't replace intervals at the end of lines (required)
            pass
        else:
            assert end != -1
            assert comma != -1

            a = float(string[start + 1:comma])
            b = float(string[comma + 1:end])

            if b - a < tol:
                new_middle = str((a+b)/2)
                before = string[:start]
                after = string[end+1:]

                string = before + new_middle + after

        # setup the next loop
        search_start = start + 1 # don't find the same match
        start = string.find('[', search_start)

    return string

def parse_taylor_model_data(filename):
    '''
    extract the taylor model data from out.flow into TaylorModel objects.

    Returns a list of TayorModel objects.
    '''

    m_init = 0    
    m_waiting_mode = 1
    m_waiting_tm_var = 2
    m_waiting_braces = 3
    m_reading_model = 4

    mode = m_init # finit-state-machine mode
    rv = []

    mode_name = None
    tm_var_line = None
    model_definition = ""

    with open(filename, "r") as f:
        for line in f.readlines():
            line = line.rstrip()

            if mode == m_init:
                if line.startswith('hybrid flowpipes'):
                    mode = m_waiting_mode
            elif mode == m_waiting_mode:
                if not line.startswith('{') and not line.startswith('}') and len(line) > 0:
                    mode_name = line
                    mode = m_waiting_tm_var
            elif mode == m_waiting_tm_var:
                if line.startswith('tm var'):
                    tm_var_line = line
                    mode = m_waiting_braces
            elif mode == m_waiting_braces:
                if line.startswith('{'):
                    mode = m_reading_model
                    model_definition = ""
                elif line.startswith('}'):
                    mode = m_waiting_mode
            elif mode == m_reading_model:
                if line.startswith('}'):
                    # taylor model finished
                    model_definition = remove_intervals_from_tm(model_definition)
                    rv.append(TaylorModel(mode_name, tm_var_line, model_definition))

                    mode = m_waiting_braces
                else:
                    model_definition += line + "\n"
    return rv               
 
if __name__ == "__main__":
    tool_main(FlowstarTool())






