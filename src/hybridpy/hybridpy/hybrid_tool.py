'''Hybrid Systems Tool Base Class
Contains general implementation for running tools in hypy
'''

import os
import shutil
import time
import tempfile
import random
import abc
import sys
import signal
import threading
import subprocess
import inspect
import argparse

class RunCode(object):
    '''return value of HybridTool.run()'''

    SUCCESS = 0
    ERROR = 1
    SKIP = 3 # for example, non-affine dynamics in SpaceEx
    TIMEOUT = 143

def random_string():
    '''makes a short random string'''
    
    time_part = str(long(time.clock() * 10000))
    rand_part = str(long(random.random()*10000000))
    
    return time_part + "_" + rand_part


def is_windows():
    '''check if the current platform is windows'''
    return sys.platform == "win32"

def get_script_path(filename=__file__):
    '''get the path this script'''
    return os.path.dirname(os.path.realpath(filename))

def valid_image(s):
    '''check if the given string is a valid output image (.png) path'''

    if not s.endswith(".png") and not s == '-':
        raise argparse.ArgumentTypeError('Image does not end in .png: ' + s)

    return s

def tool_main(tool_obj, extra_args=None):
    '''read tool parameters from argv and run

    extra_args is a list of (flag, help_text)
    returns a value in RunCode.*
    '''

    parser = argparse.ArgumentParser(description='Run ' + tool_obj.tool_name)
    parser.add_argument('model', help='input model file')
    parser.add_argument('image', nargs='?', help='output image file (use "-" to skip)', type=valid_image)
    parser.add_argument('--debug', '-d', action='store_true', help='save intermediate files directory')
    parser.add_argument('--explicit_temp_dir', '-e', nargs='?', help='specify explicitly the temp dir')

    if extra_args is not None:
        for flag, help_text in extra_args:
            parser.add_argument(flag, help=help_text)

    args = parser.parse_args()

    if args.image is None:
        args.image = os.path.splitext(args.model)[0] + '.png'

    tool_obj.load_args(args)

    code = tool_obj.run()

    print "Tool script exit code: " + str(code)
    sys.exit(code)

def _kill_pg(p):
    '''kill a process' processgroup'''
    
    os.killpg(os.getpgid(p.pid), signal.SIGKILL)

def run_tool(tool_obj, model, image, timeout, print_pipe, explicit_temp_dir=None):
    '''run a tool by creating a subprocess and directing output to print_pipe
    image may be None

    returns a value in RunCode.*
    '''

    if is_windows() and timeout is not None:
        print "Timeouts not supported on windows... skipping"
        timeout = None

    rv = RunCode.SUCCESS

    script_file = inspect.getfile(tool_obj.__class__)

    if image is None:
        image = "-" # no image indicator
    
    # -u is unbuffered stdout, may affect performance if there is a lot of output
    params = [sys.executable, "-u", script_file, model, image]
    
    if explicit_temp_dir is not None:
        params.append("--explicit_temp_dir")
        params.append(explicit_temp_dir)

    try:
        proc = subprocess.Popen(params, stdout=subprocess.PIPE)

        if timeout is not None:
            
            timer = threading.Timer(timeout, _kill_pg, [proc])
            timer.daemon = True
            timer.start()

        print_pipe(proc.stdout)
        rv = proc.wait()

        if timeout is not None:
            timer.cancel()

            if rv < 0:
                rv = RunCode.TIMEOUT

    except OSError as e:
        print "Error running tool: " + str(e)
        rv = RunCode.ERROR

    return rv

def run_check_stderr(params, stdin=None):
    '''run a process with a list of params
    returning True if success and False if error or stderr is used
    '''

    process_name = params[0]
    rv = True

    try:
        proc = None

        if stdin is None:
            proc = subprocess.Popen(params, stderr=subprocess.PIPE)
        else:
            proc = subprocess.Popen(params, stdin=stdin, stderr=subprocess.PIPE)

        for line in iter(proc.stderr.readline, ''):
            output_line = line.rstrip()

            print "stderr: " + output_line

            # if anything is printed to stderr, it's an error
            if rv:
                rv = False
                print "Stderr output detected. Assuming tool errored."

        proc.wait()

        if rv: # exit code wasn't set during output
            rv = proc.returncode == 0
    except OSError as e:
        print "Exception while trying to run " + process_name + ": " + str(e)
        rv = False

    if rv:
        print "Program exited successfully."
    else:
        print "Program exited with an error."

    return rv

def get_tool_path(filename, print_errors=True):
    '''
    Get a path for tool's executable file. This will search directories on HYPYPATH, followed by PATH.

    returns the full path to the tool, or None 
    '''
    
    rv = None
    errors = []

    if os.path.exists(filename):
        rv = filename
    else:
        for env_var_name in ["HYPYPATH", "PATH"]:
            env_var_value = os.environ.get(env_var_name)

            if env_var_value is None:
                if print_errors:
                    errors.append('Environment variable ' + env_var_name + ' was not set.')

                continue

            for dir_name in env_var_value.split(os.pathsep):

                try_path = os.path.join(dir_name, filename)

                if os.path.exists(try_path):
                    rv = try_path
                    break

            if rv is not None:
                break

            errors.append("No file named '" + filename + "' was found in any of the directories in " + env_var_name)

    if rv is None and print_errors:
        print "Warning: Could not find '" + filename + "' on your system:"

        for line in errors:
            print " " + line

    return rv

class HybridTool(object):
    '''Base class for hybrid automaton analysis tool'''
    __metaclass__ = abc.ABCMeta

    def __init__(self, tool_name, default_ext, tool_executable):
        '''Initialize the tool for running.'''

        self.tool_name = tool_name
        self.default_extension = default_ext
        self.tool_path = get_tool_path(tool_executable)

        if self.tool_path is None:
            print "Tool '" + tool_name + "' is not runnable."

        self.original_model_path = None
        self.image_path = None

        self.model_path = None # set after copying to temp folder
        self.debug = False # if set to true, will copy temp work folder back to model folder

        self.explicit_temp_dir = None
        self.start_timestamp = None

    def load_args(self, args):
        '''initialize the class from a namespace (result of ArgumentParser.parse_args())'''

        self.original_model_path = args.model
        
        if args.image != "-":
            self.image_path = os.path.realpath(args.image)

        if not os.path.exists(self.original_model_path):
            raise RuntimeError('Model file not found at path: ' + self.original_model_path)

        if args.debug is True:
            self.set_debug(True)

        if args.explicit_temp_dir is not None:
            self.set_explicit_temp_dir(args.explicit_temp_dir)

    def set_debug(self, d):
        '''set the debug flag (will copy temp directory to current working directory)'''
        self.debug = d

    def set_explicit_temp_dir(self, directory):
        '''set the temp directory where computation will be performed'''
        self.explicit_temp_dir = directory

    def run(self):
        '''runs the tool and visualization
        if the tool cannot be run (tool_path == None), prints an error and returns RunCode.SKIP

        returns a value in RunCode.*
        '''

        if self.tool_path == None:
            var = self.tool_name.upper() + "_BIN"
            print self.tool_name + ' cannot be run; skipping. Did you set ' + var + '?'
            return RunCode.SKIP

        if not is_windows() and os.getpid() != os.getpgid(os.getpid()):
            os.setsid() # create a new session id for process group termination

        rv = RunCode.SUCCESS
        old_dir = os.getcwd()
        real_path = os.path.realpath(self.original_model_path)
        temp_dir = self._make_temp_dir()

        try:
            self._copy_model(temp_dir)
            os.chdir(temp_dir)

            image_requested = self.image_path is not None
            rv = self._run_tool(image_requested)

            if rv == RunCode.SUCCESS and image_requested:
                if not self._make_image():
                    rv = RunCode.ERROR

        finally:
            if self.debug:
                # copy the temp folder to the current working directory
                dest_dir = os.path.dirname(real_path)
                dest = dest_dir + "/debug"

                if os.path.exists(dest):
                    shutil.rmtree(dest)

                shutil.copytree(temp_dir, dest)

            os.chdir(old_dir)

            # delete the temp dir, if it wasn't explicitly provided
            if self.explicit_temp_dir is None:
                shutil.rmtree(temp_dir)

        return rv

    def _make_temp_dir(self):
        '''make a temporary directory for the computation and return its path'''
        if self.explicit_temp_dir is not None:
            name = self.explicit_temp_dir
        else:
            name = os.path.join(tempfile.gettempdir(), self.tool_name + "_" + random_string())
                       
        os.makedirs(name)

        return name

    def _copy_model(self, temp_folder):
        '''copy the model to the temp folder and sets self.model_path'''
        model_name = os.path.basename(self.original_model_path)

        self.model_path = os.path.join(temp_folder, model_name)
        shutil.copyfile(self.original_model_path, self.model_path)

    def default_ext(self):
        '''get the default extension (suffix) for models of this tool'''
        return self.default_extension

    def parse_output(self, _directory, _tool_stdout_lines, _hypy_out):
        '''Create and return the output object. It should be a tool-specific dictionary.
        
        directory - the path to a folder which contains the tool's output
        lines - a list of stdout output lines produced by the tool
        hypy_out - an OutputHandler object for printing debug/status information. Use add_line() to output text.
        '''
        raise RuntimeError("Tool " + self.tool_name + " did not override parse_output()")

    @abc.abstractmethod
    def _run_tool(self, image_requested):
        '''runs the tool, returns a value in RunCode'''
        return

    @abc.abstractmethod
    def _make_image(self):
        '''makes the image after the tool runs, returns True when no error occurs'''
        return
