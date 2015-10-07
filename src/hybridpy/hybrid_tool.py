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

def is_windows():
    '''check if the current platform is windows'''
    return sys.platform == "win32"

def get_script_path():
    '''get the path this script'''
    return os.path.dirname(os.path.realpath(__file__))

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

    if args.image == None:
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
        proc = subprocess.Popen(params, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

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

def run_check_stderr(params, stdin=None, stdout=None):
    '''run a process with a list of params
    returning True if success and False if error or stderr is used
    '''

    process_name = params[0]
    rv = True

    try:
        proc = subprocess.Popen(params, stdin=stdin, stdout=stdout, stderr=subprocess.PIPE)

        for line in iter(proc.stderr.readline, ''):
            output_line = line.rstrip()

            print "stderr: " + output_line

            # if anything is printed to stderr, it's an error
            if rv:
                rv = False
                print "Stderr output detected. Assuming " + process_name + " errored."

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

def get_env_var_path(basename, default_path):
    '''Get a path from an environment variable, or use a default one if
    the environment variable is not defined. The environment variable that will be
    checked is basename.upper() + "_BIN"

    returns None if the final path doesn't exist
    '''

    var = basename.upper() + "_BIN"
    rv = os.environ.get(var)

    if rv is None:
        # not found at environment variable, try hardcoded path
        rv = default_path

    if not os.path.exists(rv):
        print basename + ' not found at path: ' + rv + '.',

        if os.environ.get(var) is None:
            print 'Environment variable ' + var + ' was NOT set.',

        print "Tool will be set as non-runnable."
        rv = None

    return rv

class HybridTool(object):
    '''Base class for hybrid automaton analysis tool'''
    __metaclass__ = abc.ABCMeta

    tool_name = None
    tool_path = None # the path to the tool, None if tool cannot be found

    original_model_path = None
    image_path = None

    model_path = None # set after copying to temp folder
    debug = False # if set to true, will copy temp work folder back to model folder

    default_extension = None
    explicit_temp_dir = None
    output_obj = None
    start_timestamp = None

    def __init__(self, tool_name, default_ext, path):
        '''Initialize the tool for running. This checks if the tool exists
        at the given path, as well as at the toolname_BIN environment variable'''

        self.tool_name = tool_name
        self.default_extension = default_ext
        self.tool_path = get_env_var_path(tool_name, path)

    def load_args(self, args):
        '''initialize the class from a namespace (result of ArgumentParser.parse_args())'''

        self.original_model_path = args.model

        if args.image != "-":
            self.image_path = os.path.realpath(args.image)

        if not os.path.exists(self.original_model_path):
            raise RuntimeError('Model file not found at path: ' + self.original_model_path)

        if args.debug == True:
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

            rv = self._run_tool()

            if rv == RunCode.SUCCESS and self.image_path is not None:
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
            name = os.path.join(tempfile.gettempdir(), self.tool_name + \
                        "_" + str(time.time()) + "_" + str(random.random()))

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

    def got_tool_output(self, line):
        '''a line of output was produced by the tool process. This only gets called if
        an output object is being created. This comes from a call to readline, so it's
        always a single line of output.
        '''

        # add (line, timestamp) to output_obj['lines']
        lines = self.output_obj['lines']
        
        if len(lines) == 0: # no output yet
            self.start_timestamp = time.time()

        timestamp = time.time() - self.start_timestamp
        lines.append((line, timestamp))

    def create_output(self, _):
        '''Assigns to the output object (self.output_obj). It is a dictionary;
        add to it, don't assign the whole object since other parts are made
        while the tool was running, for example using got_tool_output().

        For all tools, obj.lines contains a list of tuples, where the first part is
        a line of stdout output, and the second part is a timestamp in seconds (from time.time())
        
        Tool working files are stored in the passed-in directory.'''
        raise RuntimeError("Tool " + self.tool_name + " did not override create_output()")

    @abc.abstractmethod
    def _run_tool(self):
        '''runs the tool, returns a value in RunCode'''
        return

    @abc.abstractmethod
    def _make_image(self):
        '''makes the image after the tool runs, returns True when no error occurs'''
        return
