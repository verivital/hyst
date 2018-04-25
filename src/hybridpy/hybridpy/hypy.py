'''HybridPy (hypy), python module for running Hybrid Systems Analysis Tools'''

import tempfile
import subprocess
import os
import time
import sys
import argparse
import shutil

import hybridpy.hybrid_tool as hybrid_tool
from hybridpy.hybrid_tool import get_tool_path
from hybridpy.hybrid_tool import random_string

from hybridpy.tool_flowstar import FlowstarTool
from hybridpy.tool_dreach import DReachTool
from hybridpy.tool_spaceex import SpaceExTool
from hybridpy.tool_hycreate import HyCreateTool
from hybridpy.tool_pysim import PySimTool
from hybridpy.tool_hylaa import HylaaTool

# tools for which models can be generated
TOOLS = {'flowstar':FlowstarTool(), 'hycreate':HyCreateTool(), 'spaceex':SpaceExTool(), 
         'dreach':DReachTool(), 'pysim':PySimTool(), 'hylaa':HylaaTool()}

EXIT_CODE_TERM = 143

def _get_all_toolnames():
    ''' get a comma-separated list of all tool names'''
    rv = ''

    for t in TOOLS:
        if len(rv) > 0:
            rv += ", "

        rv += t

    return rv

class OutputHandler(object):
    'Object which is used to collect stdout from tools and possibly print it, save it, or process it'

    def __init__(self, save_output, tool_name, user_func=None):
        self.lines = [] if save_output is True else None
        self.tool_name = tool_name
        self.user_func = user_func

        self._start_time = time.time()

    def reset_time(self):
        'reset timer for user-defined output function'
        self._start_time = time.time()

    def add_line(self, text):
        '''a line of text was output by the tool. Save / print it.'''

        text = text.rstrip()

        if self.lines is not None:
            self.lines.append(text)

        # if a user-defined stdout function was given
        if self.user_func is not None:
            self.user_func(text, time.time() - self._start_time, self.tool_name)

    def stdout_handler(self, pipe):
        '''printer handler for subprocesses'''
        line = pipe.readline()

        while line:
            self.add_line(line)
            line = pipe.readline()

class Engine(object):
    '''HyPy engine. Runs a hybrid systems tool'''

    # run() error codes
    SUCCESS = 'Success'
    ERROR_TOOL = 'Error (Tool)'
    ERROR_UNSUPPORTED = 'Error (Unsupported Dynamics)'
    ERROR_CONVERSION = 'Error (Conversion)'
    TIMEOUT_CONVERSION = 'Timeout (Conversion)'
    TIMEOUT_TOOL = 'Timeout (Tool)'

    def __init__(self, printer_name, printer_param=None):
        assert str(printer_name) == printer_name, "Printer name must be a string"

        if printer_param is None:
            printer_param = ""
        else:
            assert str(printer_param) == printer_param, "Printer param must be a string: {}".format(repr(printer_param))

        name = printer_name.lower()

        if not name in TOOLS:
            raise RuntimeError('Unknown tool name: ' + name)

        self.printer = (printer_name, printer_param)
        self.input_ = (None, None) # 2-tuple: (xml_file, cfg_file [optional])
        self.gen = (None, None)
        self.passes = [] # list of 2-tuples
        self.output = None # path where to save a copy of the model hyst creates (optional)
        self.additional_hyst_params = [] # manually-specified parameters 
        self.debug = False
        self.verbose = False

    def set_debug(self, is_debug):
        'set debug printing mode'
        self.debug = is_debug

    def set_verbose(self, is_verbose):
        'set verbose printing mode'
        self.verbose = is_verbose

    def set_input(self, xml_path, cfg_path=None):
        '''Set the input model file'''
        self.input_ = (xml_path, cfg_path)

    def set_generator(self, gen_name, gen_param=""):
        '''set the name of the model generator and param'''
        assert str(gen_name) == gen_name, "Generator name must be a string"
        assert str(gen_param) == gen_param, "Generator param must be a string, got {}".format(type(gen_param))
    
        self.gen = (gen_name, gen_param)

    def add_pass(self, pass_name, pass_param=""):
        '''add a model transformation pass by name and its param'''
        assert str(pass_name) == pass_name, "Pass name must be a string"
        assert str(pass_param) == pass_param, "Pass param must be a string"

        self.passes += [(pass_name, pass_param)]    
    
    def set_output(self, path):
        '''Set the path for saving the model file (optional)'''
        self.output = path

    def set_additional_hyst_params(self, params):
        '''Sets manually-specified hyst params'''
        self.additional_hyst_params = params

    def _run_hyst(self, hypy_out, hyst_out):
        '''
        runs hyst on the model,

        hypy_out is an OutputHandler which gets hypy uses to produce output
        hyst_out is an OutputHandler capturing hyst's output

        returns a code in Engine.Error_*
        '''
        rv = Engine.SUCCESS

        hypy_out.add_line("Using Hyst to convert {} for {}.".format(
            "generated model" if self.input_[0] is None else 
            "model '" + self.input_[0] + "'", self.printer[0]))

        hyst_path = get_tool_path('Hyst.jar')

        if hyst_path is None:
            raise RuntimeError('Hyst not found. Did you add the directory with Hyst.jar to HYPYPATH?')

        params = ['java', '-jar', hyst_path]

        if self.debug:
            params.append('-debug')

        if self.verbose:
            params.append('-verbose')

        if self.input_[0] is not None and self.gen[0] is not None:
            raise RuntimeError("Input file provided and model generation selected. These options are incompatible.")

        if self.input_[0] is not None:
            params += ['-i', self.input_[0]]

            if self.input_[1] is not None:
                params.append(self.input_[1]) # cfg file
        elif self.gen[0] is not None:
            params += ['-gen', self.gen[0], self.gen[1]]
        else:
            raise RuntimeError("No input file provided and no model generation params given.")

        if len(self.passes) > 0:
            params.append("-passes")

            for (pass_name, param) in self.passes:
                params += [pass_name, param]
        
        params += ['-o', self.output]
        params += ['-tool', self.printer[0], self.printer[1]]

        params += self.additional_hyst_params

        quoted_params = ["'" + param + "'" if (' ' in param or len(param) == 0) else param for param in params]
        hypy_out.add_line("Hyst command: {}".format(" ".join(quoted_params)))

        try:
            proc = subprocess.Popen(params, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            hyst_out.stdout_handler(proc.stdout)
            code = proc.wait()

            if code == 2: # Hyst exit code 2 = preconditions not met for printer
                rv = Engine.ERROR_UNSUPPORTED
            elif code != 0:
                rv = Engine.ERROR_CONVERSION
                hypy_out.add_line('Error: Hyst returned nonzero exit code: {}.\n'.format(code))
        except OSError as e:
            hypy_out.add_line('Error while running Hyst: {}\n'.format(e))
            rv = Engine.ERROR_CONVERSION

        return rv

    def run(self, run_hyst=True, run_tool=True, timeout=None, image_path=None, save_stdout=False, print_stdout=False, 
            stdout_func=None, parse_output=False):
        '''
        Converts the model in Hyst, runs it with the appropriate tool, 
        produces a plot image, and python results object.

        non-obvious parameters:
        stdout_func - a 3-param user function for processing of stream stdout. Params are: line, time, tool_name
        parse_output - should tool's output be parsed into a python object? If True, 'output' in the result is set.
                       using this option forces save_stdout to True

        returns a dictionary object with the following keys:
        'code' - exit code - engine.SUCCESS if successful, an engine.ERROR_* code otherwise
        'hyst_time' - time in seconds for hyst to run, only returned if run_hyst == True
        'tool_time' - time in seconds for tool(+image) to run, only returned if run_tool == True
        'time' - total run time in seconds
        'stdout' - the list of lines anything produced to stdout, only returned if save_stdout == True
        'tool_stdout' - the list of lines the tool produced to stdout, only returned if save_stdout == True
        'hypy_stdout' - the list of lines hypy produces to stdout, only returned if save_stdout == True
        'output' - tool-specific processed output object, only returned if successful and parse_output == True
        '''

        start_time = time.time()
        tool = TOOLS.get(self.printer[0])

        if self.output is None:
            self.output = os.path.join(tempfile.gettempdir(), self.printer[0] + \
                    "_" + random_string() + tool.default_ext())

        rv = {}
        rv['code'] = Engine.SUCCESS
        stdout_lines = None

        if parse_output:
            save_stdout = True
        
        if save_stdout:
            stdout_lines = []

        # wrapper function to capture all stdout in order
        def stdout_wrapper(line, secs, tool):
            'wrapper to capture lines produced by stdout from any tool'
    
            if stdout_lines is not None:
                stdout_lines.append(line)

            if stdout_func is not None:
                stdout_func(line, secs, tool)

            if print_stdout:
                print line
                sys.stdout.flush() # flush after each line

        hypy_out = OutputHandler(save_stdout, 'hypy', user_func=stdout_wrapper)

        if run_hyst is False:
            self.output = self.input_[0] # running the tool directly (no hyst)
        else:
            hypy_out.add_line("Running Hyst...")
            hyst_start_time = time.time()
            hyst_out = OutputHandler(save_stdout, 'hyst', user_func=stdout_wrapper)
            rv['code'] = self._run_hyst(hypy_out, hyst_out)
            rv['hyst_time'] = time.time() - hyst_start_time
            hypy_out.add_line("Seconds for Hyst conversion: {}\n".format(rv['hyst_time']))

            if save_stdout:
                rv['hyst_stdout'] = hyst_out.lines

        if rv['code'] == Engine.SUCCESS and run_tool:
            temp_dir = None

            if parse_output:
                temp_dir = os.path.join(tempfile.gettempdir(), "hypy_" + random_string())

            tool_start_time = time.time()

            tool_out = OutputHandler(save_stdout, self.printer[0], user_func=stdout_wrapper)
            code = hybrid_tool.run_tool(tool, self.output, image_path, timeout, tool_out.stdout_handler, temp_dir)

            rv['tool_time'] = time.time() - tool_start_time

            if code == hybrid_tool.RunCode.TIMEOUT:
                rv['code'] = Engine.TIMEOUT_TOOL
            elif code == hybrid_tool.RunCode.SKIP:
                rv['code'] = Engine.ERROR_UNSUPPORTED
            elif code != hybrid_tool.RunCode.SUCCESS:
                rv['code'] = Engine.ERROR_TOOL
            elif parse_output:
                rv['output'] = tool.parse_output(temp_dir, tool_out.lines, hypy_out)
             
            if temp_dir is not None:   
                shutil.rmtree(temp_dir)

            if save_stdout:
                rv['tool_stdout'] = tool_out.lines

        if save_stdout:
            rv['stdout'] = stdout_lines

        rv['time'] = time.time() - start_time
        hypy_out.add_line("Hypy Elapsed Seconds: {}\n".format(rv['time']))
        hypy_out.add_line("Hypy Result: {}\n".format(rv['code']))

        return rv

def main():
    '''if hypy is run directly'''

    parser = argparse.ArgumentParser(description='Use Hypy to run a tool.')
    parser.add_argument('tool', help='name of the tool (' + _get_all_toolnames() + ')')
    parser.add_argument('model', help='input model file')
    parser.add_argument('image', nargs='?', help='output image file')
    parser.add_argument('--output', '-o', metavar='PATH', help='output model file')
    parser.add_argument('--parse_output', '-po', action='store_true', help='print the parsed tool-specific output?')
    parser.add_argument('--image_tool', '-it', metavar='PATH', help='path to tool which displays image')
    parser.add_argument('--timeout', '-to', metavar='SECONDS', type=float, \
                        help='sets timeout (seconds) for running the tool (Hyst runs without timeout)')
    parser.add_argument('tool_param', nargs='*', help='tool parameter passed to Hyst')

    args = parser.parse_args()

    tool_name = args.tool
    model_path = args.model
    image_path = args.image
    model_save_path = args.output
    image_tool = args.image_tool
    timeout = args.timeout
    parse_output = args.parse_output is True
    tool_param = " ".join(args.tool_param)

    if image_path is not None and not image_path.endswith('.png'):
        print "Expected image path to end in .png. Instead got: " + image_path
        sys.exit(1)

    # run hypy with the given parameters
    e = Engine(tool_name, tool_param)

    if model_path != None:
        e.set_input(model_path)

    if model_save_path is not None:
        e.set_output(model_save_path)

    result = e.run(print_stdout=True, timeout=timeout, image_path=image_path, parse_output=parse_output)

    if result['code'] == Engine.SUCCESS and image_path is not None and image_tool is not None:
        # plot it
        params = image_tool.split(" ")
        params.append(image_path)
        
        if subprocess.call(params) != 0:
            print "Hypy: Error running image tool (nonzero exit code): " + str(params)

    if result['code'] == Engine.SUCCESS and parse_output:
        print "Parsed output:", result['output']

    return result['code']

if __name__ == "__main__":
    if main() == Engine.SUCCESS:
        sys.exit(0)
    else:
        sys.exit(1)        
