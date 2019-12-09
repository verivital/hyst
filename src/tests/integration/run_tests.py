'''
Test scripts for integration tests (run each tool on each example mode
Stanley Bak
'''
from __future__ import print_function
from builtins import str
from builtins import range

import os
import sys
import shutil
import multiprocessing
import hybridpy.hybrid_tool
from hybridpy.hybrid_tool import get_script_path
import hybridpy.hypy as hypy
from hybridpy.hypy import Engine

# timeout for running the tools
TIMEOUT = 2.0

SHOULD_RUN_TOOLS = True
# autodetect number of threads (override with NUM_THREADS)
NUM_THREADS = int(os.environ.get("NUM_THREADS", multiprocessing.cpu_count()))

if sys.platform == "win32":
    SHOULD_RUN_TOOLS = False
    NUM_THREADS = 1

# the examples to run, relative to this script
MODELS_PATH = get_script_path(__file__) + "/models"

# a list of tools to run: each element is (tool_name, tool_param)
TOOLS = [(hypy_tool_name, None) for hypy_tool_name in hypy.TOOLS]
# manually set / add tools (tool name, hyst printer parameters)
# TOOLS=[]
# TOOLS.append(('spaceex', '-output-format INTV'))
# TOOLS.append(('hylaa', ''))

# the path where to put results (plots/models/work dir), relative to this script
RESULT_PATH = get_script_path(__file__) + "/result"

def run_single(xxx_todo_changeme):
    '''run a single model with a single tool.

    file is the path to the model file
    tool is an entry of the TOOLS list
    '''
    (index, total, path, tool_tuple, quit_flag) = xxx_todo_changeme
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

    current_timeout = TIMEOUT

    allowed_results = [Engine.SUCCESS]
    NONLINEAR_MODELS = ['biology7d', 'biology9d', 'brusselator', 'coupled_vanderpol', 'vanderpol', 'lorenz', 'stable_3d', 'neuron']
    # Ignore rules: Which errors are okay? Which models need a longer timeout?
    if tool_name == 'hylaa' and model in NONLINEAR_MODELS + ['pd_lut_linear']:
        # unsupported dynamics for hylaa
        allowed_results += [Engine.ERROR_UNSUPPORTED]
    if tool_name == 'dreach' and model in NONLINEAR_MODELS + ['cont_approx', 'pd_lut_linear']:
        # unsupported dynamics
        allowed_results += [Engine.ERROR_UNSUPPORTED]
    if tool_name == 'spaceex' and model in NONLINEAR_MODELS:
        # unsupported dynamics
        allowed_results += [Engine.ERROR_UNSUPPORTED]

    # if tool_name == 'my_broken_tool' and model in ['foo', 'bar']:
    #    allowed_results += [Engine.ERROR_UNSUPPORTED, Engine.ERROR_TOOL, Engine.ERROR_CONVERSION]
    if tool_name == 'hylaa' and model == 'toy_diverging':
        # Hylaa doesn't abort the computation for this diverging automaton
        allowed_results += [Engine.TIMEOUT_TOOL]    
    elif 'toy' in model:
        # for the trivial examples, give plenty of time,
        # do not allow a timeout
        current_timeout += 60
    else:
        # for nontrivial examples, timeout is okay; we are happy if the tool starts without crashing
        # FIXME: is this really what we want?
        allowed_results += [Engine.TIMEOUT_TOOL]
    
    print("{}/{} Running {} with {} and timeout {}".format(index, total, model, tool_name, current_timeout))
    sys.stdout.flush()

    
    # enable 'parse_output' if the tool overrides the parse_output() method
    tool_supports_parse_output = (hypy.TOOLS[tool_name].parse_output.__code__ is not hybridpy.hybrid_tool.HybridTool.parse_output.__code__)

    result = e.run(run_tool=SHOULD_RUN_TOOLS, timeout=current_timeout, save_stdout=True, image_path=base + ".png", parse_output=tool_supports_parse_output)


    if result['code'] not in allowed_results:
        message = "Test failed for {}/{} model {} with {}: {}. (If this is not an error, add an ignore rule to src/tests/integration/run_test.py.".format(index, total, model, tool_name, result['code'])

        log = "\n".join(result['stdout'])

        rv = (message, log)
        quit_flag.value = 1
    elif result['code'] != Engine.SUCCESS:
        print("Notice: Ignoring test result for {}/{} model {} with {}: {}".format(index, total, model, tool_name, result['code']))        

    print("{}/{} Finished {} with {}".format(index, total, model, tool_name))

    return rv
def file_tool_iter(file_list, quit_flag):
    '''iterator over all model files and tools'''

    speedup = False
    if os.environ.get('SKIP_MOST_TESTS'):
        print("Skipping most tests because SKIP_MOST_TESTS is set")
        speedup = True
    
    index = 0
    total = len(TOOLS) * len(file_list)

    # first run each tool once on the first file
    f = file_list[0]

    for tool in TOOLS:
        yield (index, total, f, tool, quit_flag)
        index = index + 1
    
    if speedup:
        print("Skipping remaining tests")
        return
    # then run one tool on all models
    for tool in TOOLS:
        for f_index in range(1, len(file_list)):
            f = file_list[f_index]
            yield (index, total, f, tool, quit_flag)
            index = index + 1

def parallel_run(file_list):
    '''run the passed in files with all the tools (in parallel)'''

    results = None
    manager = multiprocessing.Manager()
    quit_flag = manager.Value('i', 0)

    if NUM_THREADS > 1:
        print("Using " + str(NUM_THREADS) + " parallel processes.")
        pool = multiprocessing.Pool(processes=NUM_THREADS)

        # multi-threaded version
        results = pool.map(run_single, file_tool_iter(file_list, quit_flag), chunksize=1)

    else:
        print("Using single-threaded tests")
        results = [run_single(i) for i in file_tool_iter(file_list, quit_flag)]

    rv = True

    for r in results:
        if not r is None:
            (message, log) = r

            print(message)

            if rv == True:
                print("\nLog:\n" + log + "\n\n")
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
    print("Script results (plots / model files / work dirs) will be saved to:", RESULT_PATH)

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
            print("Warning: {} is not runnable. Tool will be skipped.".format(name))
            skipped_some_tool = True

    if parallel_run(files):
        if not skipped_some_tool:
            print("Done running all integration tests, success.")
        else:
            print("Integration test conversion passed, but some tools were not run.")
        sys.exit(0)
    else:
        print("Error detected running integration tests.")
        sys.exit(1)

if __name__ == "__main__":
    main()

