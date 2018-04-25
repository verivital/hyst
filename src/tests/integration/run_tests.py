'''
Test scripts for integration tests (run each tool on each example mode
Stanley Bak
'''

import os
import sys
import shutil
import multiprocessing
from hybridpy.hybrid_tool import get_script_path
import hybridpy.hypy as hypy
from hybridpy.hypy import Engine

# timeout for running the tools
TIMEOUT = 2.0

SHOULD_RUN_TOOLS = True
NUM_THREADS = multiprocessing.cpu_count()

if sys.platform == "win32":
    SHOULD_RUN_TOOLS = False
    NUM_THREADS = 1

# the examples to run, relative to this script
MODELS_PATH = get_script_path(__file__) + "/models"

# a list of tools to run: each element is (tool_name, tool_param)
TOOLS = [(hypy_tool_name, None) for hypy_tool_name in hypy.TOOLS]

# extra tools (manual)
#TOOLS.append({'name':'hycreate', 'param':'sim-only=1'})

# the path where to put results (plots/models/work dir), relative to this script
RESULT_PATH = get_script_path(__file__) + "/result"

def run_single((index, total, path, tool_tuple, quit_flag)):
    '''run a single model with a single tool.

    file is the path to the model file
    tool is an entry of the TOOLS list
    '''

    if not quit_flag.value == 0:
        return None

    rv = None

    filename = os.path.split(path)[1]
    model = os.path.splitext(filename)[0]

    (tool_name, tool_param) = tool_tuple

    base = RESULT_PATH + '/' + model + '_' + tool_name

    e = hypy.Engine(tool_name, tool_param)
    e.set_input(path)
    e.set_output(base + hypy.TOOLS[tool_name].default_ext())

    to = TIMEOUT

    print "{}/{} Running {} with {} and timeout {}".format(index, total, model, tool_name, to)
    sys.stdout.flush()

    result = e.run(run_tool=SHOULD_RUN_TOOLS, timeout=to, save_stdout=True, image_path=base + ".png")

    if result['code'] in [Engine.ERROR_TOOL, Engine.ERROR_CONVERSION, Engine.TIMEOUT_CONVERSION]:
        message = "Test failed for {}/{} model {} with {}: {}".format(index, total, model, tool_name, result['code'])

        log = "\n".join(result['stdout'])

        rv = (message, log)
        quit_flag.value = 1

    print "{}/{} Finished {} with {}".format(index, total, model, tool_name)

    return rv

def file_tool_iter(file_list, quit_flag):
    '''iterator over all model files and tools'''

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

    if not os.path.exists(RESULT_PATH):
        os.makedirs(RESULT_PATH)

############################
# start

def main():
    '''main entry point'''
    setup_env()
    skipped_some_tool = False

    files = get_files(MODELS_PATH)
    assert len(files) > 0, "No input .xml files detected in directory: {}".format(MODELS_PATH)

    files.sort() # sort models in alphabetical order

    for (name, _) in TOOLS:
        if hypy.TOOLS[name].tool_path is None:
            print "Warning: {} is not runnable. Tool will be skipped.".format(name)
            skipped_some_tool = True

    if parallel_run(files):
        if not skipped_some_tool:
            print "Done running all integration tests, success."
        else:
            print "Integration test conversion passed, but some tools were not run."
        sys.exit(0)
    else:
        print "Error detected running integration tests."
        sys.exit(1)

if __name__ == "__main__":
    main()

