'''Regression test script'''
# Stanley Bak, December 2014

# Usage - set appropriate globals and then run directly with python

# make sure you set the appropriate path to EXAMPLES

import os
import sys
import shutil
import multiprocessing

def get_script_path():
    '''get the path this script'''
    return os.path.dirname(os.path.realpath(__file__))

def get_hypy_path():
    '''get the path to the hybridpy directory'''
    rv = os.path.realpath(get_script_path() + '../../../../lib/tools')

    if not os.path.exists(rv):
        raise RuntimeError("hypy path is wrong")

    return rv

sys.path.append(get_hypy_path())
import hybridpy as hypy

# timeout for running the tools
TIMEOUT = 2.0

SHOULD_RUN_TOOLS = True
NUM_THREADS = multiprocessing.cpu_count()

if sys.platform == "win32":
    SHOULD_RUN_TOOLS = False
    NUM_THREADS = 1

# the examples to run, relative to this script
MODELS_PATH = get_script_path() + "/models"

# the tools and the scripts / parameters needed to run them
TOOLS = []

# add all tools from hypy
for key in hypy.TOOLS:
    TOOLS.append({'name':key})

# extra tools (manual)
#TOOLS.append({'name':'hycreate', 'param':'sim-only=1'})

# the path where to put results (plots/models/work dir), relative to this script
RESULT_PATH = get_script_path() + "/result"

def run_single((index, total, path, tool, quit_flag)):
    '''run a single model with a single tool.

    file is the path to the model file
    tool is an entry of the TOOLS list
    '''

    if not quit_flag.value == 0:
        return None

    rv = None

    filename = os.path.split(path)[1]
    model = os.path.splitext(filename)[0]

    tool_name = tool['name']
    param = tool.get('param')

    index_str = str(index+1) + "/" + str(total)
    print index_str + " Running " + model + " with " + tool_name
    sys.stdout.flush()

    e = hypy.Engine()

    e.set_timeout(TIMEOUT)
    e.set_save_terminal_output(True)

    e.set_model(path)
    e.set_tool(tool_name)

    if not param is None:
        e.set_tool_params(['-tp', param])

    base = RESULT_PATH + '/' + model + '_' + tool_name
    e.set_output_image(base + '.png')
    e.set_save_model_path(base + hypy.TOOLS[tool_name].default_ext())

    code = e.run(SHOULD_RUN_TOOLS)

    if code in hypy.get_error_run_codes():
        message = "Test failed for " + index_str + " model " + model + " with " + tool_name + ": " + str(code)
        log = e.get_terminal_output()
        rv = (message, log)
        quit_flag.value = 1

    print index_str + " Finished " + model + " with " + tool_name

    return rv

def file_tool_iter(file_list, quit_flag):
    '''iterator over all model files and tools'''
    assert len(file_list) > 0

    index = 0
    total = len(TOOLS) * len(file_list)

    # first run each tool once on the first file
    f = file_list[0]

    for tool in TOOLS:
        yield (index, total, f, tool, quit_flag)
        index = index + 1

    # then run one tool on all models
    for tool in TOOLS:
        for f_index in xrange(1, len(file_list)):
            f = file_list[f_index]
            yield (index, total, f, tool, quit_flag)
            index = index + 1

def parallel_run(file_list):
    '''run the passed in files with all the tools (in parallel)'''

    results = None
    manager = multiprocessing.Manager()
    quit_flag = manager.Value('i', 0)

    if NUM_THREADS > 1:
        print "Using " + str(NUM_THREADS) + " parallel processes."
        pool = multiprocessing.Pool(processes=NUM_THREADS)

        # multi-threaded version
        results = pool.map(run_single, file_tool_iter(file_list, quit_flag), chunksize=1)

    else:
        print "Using single-threaded tests"
        results = [run_single(i) for i in file_tool_iter(file_list, quit_flag)]

    rv = True

    for r in results:
        if not r is None:
            (message, log) = r

            print message

            if rv == True:
                print "\nLog:\n" + log + "\n\n"
                rv = False

    return rv

def get_files(filename):
    '''get all the .xml files in the passed-in directory recursively'''
    rv = []

    if os.path.isdir(filename):
        # recursive case
        file_list = os.listdir(filename)

        for f in file_list:
            rv += get_files(filename + "/" + f)
    elif filename.endswith(".xml"):
        # base case, run single example
        rv.append(filename)

    return rv

def setup_env():
    '''remove the results directory and create it if it doesn't exist'''
    print "Script results (plots / model files / work dirs) will be saved to:", RESULT_PATH

    # Clear results path
    if os.path.exists(RESULT_PATH):
        shutil.rmtree(RESULT_PATH)

    if os.path.exists(RESULT_PATH) == False:
        os.makedirs(RESULT_PATH)

############################
# start

def main():
    '''main entry point'''
    setup_env()

    files = get_files(MODELS_PATH)

    if parallel_run(files):
        print "Done running all regression tests, success."
        sys.exit(0)
    else:
        print "Error detected running regression tests."
        sys.exit(1)

if __name__ == "__main__":
    main()

