'''HybridPy (hypy), python module for running Hybrid Systems Analysis Tools'''

import tempfile
import subprocess
import os
import time
import sys
import random
import argparse
import shutil

import hybrid_tool
from hybrid_tool import get_script_path
from hybrid_tool import get_env_var_path

from tool_flowstar import FlowstarTool
from tool_dreach import DReachTool
from tool_spaceex import SpaceExTool
from tool_hycreate import HyCreateTool

# path the the Hyst jar file
DEFAULT_HYST_PATH = get_script_path() + '/../Hyst.jar'

# tools for which models can be generated
TOOLS = {'flowstar':FlowstarTool(), 'hycreate':HyCreateTool(), \
         'spaceex':SpaceExTool(), 'dreach':DReachTool()}

# return codes for Engine.run()
def enum(**enums):
    '''return codes for Engine.run()'''
    return type('Enum', (), enums)

RUN_CODES = enum(SUCCESS='Success', ERROR_TOOL='Error (Tool)', \
    ERROR_UNSUPPORTED='Error (Unsupported Dynamics)', ERROR_CONVERSION='Error (Conversion)', \
    TIMEOUT_CONVERSION='Timeout (Conversion)', TIMEOUT_TOOL='Timeout (Tool)')

EXIT_CODE_TERM = 143

def get_error_run_codes():
    '''get all the RunCode.* error values'''
    return [RUN_CODES.ERROR_TOOL, RUN_CODES.ERROR_CONVERSION, RUN_CODES.TIMEOUT_CONVERSION]

def _get_all_toolnames():
    ''' get a comma-separated list of all tool names'''
    rv = ''

    for t in TOOLS:
        if len(rv) > 0:
            rv += ", "

        rv += t

    return rv

class Engine(object):
    '''HyPy engine. Runs a hybrid systems tool'''
    model_path = None
    tool_name = None
    image_path = None
    timeout_tool = None
    save_model_path = None
    tool_params = []
    running_tool = None # this is the hybrid_tool object, assigned only running tool process

    process_return_code = None
    print_output = False
    save_output = False
    output_lines = []
    process_output_dir = None
    result_obj = None # the result object, set if set_process_output is ture

    def __init__(self):
        pass

    def set_create_result(self, val):
        '''Sets whether hypy should perform tool-specific processing of the output.

        If set, you can get the (tool-specific) result object by calling get_result()
        '''
        if val == False:
            self.process_output_dir = None
        else:
            self.process_output_dir = "" # if it's not None it gets regenerated every time we run

    def get_result(self):
        '''Get the processed result object, only valid if set_create_result(True) was called'''

        if self.result_obj is None and self.process_output_dir is None:
            raise RuntimeError("Result was None; did you call set_create_result(True)?")

        return self.result_obj

    def set_save_model_path(self, path):
        '''Set the path for saving the model file (optional)'''
        self.save_model_path = path

    def set_model(self, path):
        '''Set the input model file'''
        self.model_path = path

    def set_save_terminal_output(self, val):
        '''should terminal output (stdout / stderr) be saved? If
        enabled, it can be retrieved after a call to run() by calling
        get_terminal_output()'''
        self.save_output = val

    def set_print_terminal_output(self, val):
        '''should terminal output (stdout / stderr) be printed?'''
        self.print_output = val

    def set_tool(self, name):
        '''set the name of the tool for the conversion'''
        name = name.lower()

        if not name in TOOLS:
            raise RuntimeError('Unknown tool name: ' + name)

        self.tool_name = name

    def set_output_image(self, path):
        '''Set the path to the image outout'''
        self.image_path = path

    def set_timeout(self, seconds):
        '''Set the timeout for both the conversion and the tool'''
        self.timeout_tool = seconds

    def set_tool_params(self, params):
        '''set additional tool params to pass to Hyst. params is a list of parameters'''
        self.tool_params = params

    def get_terminal_output(self):
        '''get the tool's output to stdout'''
        assert self.save_output, "get_terminal_output() was called but set_save_terminal_output(True) was not"

        return "".join(self.output_lines)

    def _add_terminal_output(self, text):
        '''a line of text was output by the tool. Save / print it.'''

        if self.print_output:
            print text,

        if self.save_output:
            self.output_lines.append(text)

        # if we're creating an output object, and the tool is running
        if self.process_output_dir is not None and self.running_tool is not None:
            self.running_tool.got_tool_output(text)

    def get_process_return_code(self):
        '''Get the return code of the last external process run by hypy'''
        return self.process_return_code

    def _stdout_handler(self, pipe):
        '''printer handler for subprocesses'''
        line = pipe.readline()

        while line:
            self._add_terminal_output(line)
            line = pipe.readline()

    def _run_hyst(self):
        '''runs hyst on the model, returning a code in RUN_CODES'''
        rv = RUN_CODES.SUCCESS
        start_time = time.time()
        format_flag = "-" + self.tool_name

        self.output_lines = []
        self._add_terminal_output("Running " + self.tool_name + " on model " + self.model_path + "\n")

        hyst_path = get_env_var_path('hyst', DEFAULT_HYST_PATH)
        params = ['java', '-jar', hyst_path, self.model_path]
        params += self.tool_params
        params += ['-o', self.save_model_path, format_flag] # do after to override any user flags

        self._add_terminal_output("Hyst command: " + str(params) + "\n")

        try:
            proc = subprocess.Popen(params, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            self._stdout_handler(proc.stdout)
            code = proc.wait()

            if code == 2: # Hyst exit code 2 = preconditions not met for printer
                rv = RUN_CODES.ERROR_UNSUPPORTED
            elif proc.wait() != 0:
                rv = RUN_CODES.ERROR_CONVERSION
                self._add_terminal_output('Error: Hyst returned nonzero exit code.\n')

            dif = time.time() - start_time
        except OSError as e:
            self._add_terminal_output('Error while running Hyst: ' + e + '\n')
            rv = RUN_CODES.ERROR_CONVERSION

        self._add_terminal_output("Seconds for Hyst conversion: " + str(dif) + '\n')

        return rv

    def run(self, run_hyst=True, run_tool=True, make_image=True):
        '''Converts the model in Hyst (optional) and runs it with the appropriate
        tool (optional) to produce a plot file.

        returns SUCCESS if successful, otherwise can return one of the
        ERROR_* codes
        '''

        assert self.model_path != None, 'set_model() should be called before run()'
        assert self.tool_name != None, 'set_tool() should be called before run()'

        image_path = None
        
        if make_image == True:
            if self.image_path is None:
                image_path = os.path.splitext(self.model_path)[0] + ".png"
            else:
                image_path = self.image_path

        start_time = time.time()
        tool = TOOLS.get(self.tool_name)

        if self.save_model_path is None:
            self.save_model_path = os.path.join(tempfile.gettempdir(), self.tool_name + \
                    "_" + str(time.time()) + "_" + str(random.random()) + tool.default_ext())

        rv = RUN_CODES.SUCCESS

        if run_hyst == False:
            self.save_model_path = self.model_path
        else:
            rv = self._run_hyst() # will assign save_model_path

        if rv == RUN_CODES.SUCCESS and run_tool:

            if self.process_output_dir is not None:
                self.process_output_dir = os.path.join(tempfile.gettempdir(), "hypy_" + \
                                        str(time.time()) + "_" + str(random.random()))
                tool.output_obj = {} # new output object created
                tool.output_obj['lines'] = [] # (stdout_line, timestamp) tuple list

            self.running_tool = tool
            code = hybrid_tool.run_tool(tool, self.save_model_path, image_path, \
                                        self.timeout_tool, self._stdout_handler,
                                        self.process_output_dir)
            self.running_tool = None

            if code == hybrid_tool.RunCode.TIMEOUT:
                rv = RUN_CODES.TIMEOUT_TOOL
            elif code == hybrid_tool.RunCode.SKIP:
                rv = RUN_CODES.ERROR_UNSUPPORTED
            elif code != hybrid_tool.RunCode.SUCCESS:
                rv = RUN_CODES.ERROR_TOOL
            elif self.process_output_dir is not None:
                tool.create_output(self.process_output_dir)

                self.result_obj = tool.output_obj
                shutil.rmtree(self.process_output_dir)

        dif_time = time.time() - start_time
        self._add_terminal_output("Elapsed Seconds: " + str(dif_time) + "\n")
        self._add_terminal_output("Result: " + str(rv) + "\n")

        return rv

def main():
    '''if hypy is run directly'''

    parser = argparse.ArgumentParser(description='Use Hypy to run a tool.')
    parser.add_argument('tool', help='name of the tool (' + _get_all_toolnames() + ')')
    parser.add_argument('model', help='input model file')
    parser.add_argument('image', nargs='?', help='output image file')
    parser.add_argument('--output', '-o', metavar='PATH', help='output model file')
    parser.add_argument('--timeout', '-to', metavar='SECONDS', type=float, \
                        help='sets timeout (seconds) for running the tool (Hyst runs without timeout)')
    parser.add_argument('tool_param', nargs='*', help='tool parameter passed to Hyst')

    args = parser.parse_args()

    tool_name = args.tool
    model_path = args.model
    image_path = args.image
    model_save_path = args.output
    timeout = args.timeout
    tool_params = args.tool_param

    if image_path is not None and not image_path.endswith('.png'):
        print "Expected image path to end in .png. Instead got: " + image_path
        sys.exit(1)

    # run hypy with the given parameters
    e = Engine()
    e.set_print_terminal_output(True)

    e.set_model(model_path)
    e.set_tool(tool_name)
    e.set_tool_params(tool_params)

    e.set_output_image(image_path)

    if model_save_path is not None:
        e.set_save_model_path(model_save_path)

    if timeout is not None:
        e.set_timeout(timeout)

    return e.run()

if __name__ == "__main__":
    main()
