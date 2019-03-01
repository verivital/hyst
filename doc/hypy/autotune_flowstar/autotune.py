'''Python 2.7 script for demonstrating automatic parameter tuning using Flow* 2.1.0

make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
make sure the path to the flow* binary is on the HYPYPATH environment variable: ~/tools/flowstar

Stanley Bak
Oct 2018
'''

import sys

import hybridpy.hypy as hypy

def run(unsafe_condition, order, step_size):
    '''run flowstar with the given unsafe condition, order, and step size

    returns a 2-tuple: (is_safe, runtime_seconds)
    '''

    # we will add an error condition, and then find parameters in Flow* that prove safety with the error condition
    input_model_file = 'vanderpol.xml'
    output_model_file = 'out_flowstar.model'
    flowstar_hyst_param = '-orders {} -step {} -unsafe "{}"'.format(order, step_size, unsafe_condition)

    e = hypy.Engine('flowstar', flowstar_hyst_param)

    e.set_input(input_model_file)
    
    print_stdout = False
    image_path = None
    
    #### enable these for debugging ####
    #e.set_output('out.model') # output the generated Flow* model to this path
    #e.set_verbose(True) # print hyst verbose output
    #print_stdout=True # print hyst/tool output
    #image_path='out.png' # output image path (requires GIMP is setup according to Hyst README)

    print "{}".format(step_size),
    result = e.run(parse_output=True, image_path=image_path, print_stdout=print_stdout)

    # result is a dictionary object with the following keys:
    # 'code' - exit code - engine.SUCCESS if successful, an engine.ERROR_* code otherwise
    # 'hyst_time' - time in seconds for hyst to run, only returned if run_hyst == True
    # 'tool_time' - time in seconds for tool(+image) to run, only returned if run_tool == True
    # 'time' - total run time in seconds
    # 'stdout' - the list of lines anything produced to stdout, only returned if save_stdout == True
    # 'tool_stdout' - the list of lines the tool produced to stdout, only returned if save_stdout == True
    # 'hypy_stdout' - the list of lines hypy produces to stdout, only returned if save_stdout == True
    # 'output' - tool-specific processed output object, only returned if successful and parse_output == True

    if result['code'] != hypy.Engine.SUCCESS:
        raise RuntimeError('Hypy Error: {}'.format(result['code']))

    runtime = result['tool_time']
    output = result['output']

    #The output object is an ordered dictionary, with:
    # 'terminated' -> True/False   <-- did errors occur during computation (was 'terminated' printed?)
    # 'mode_times' -> [(mode1, time1), ...]  <-- list of reach-tmes computed in each mode, in order
    # 'result' -> 'UNKNOWN'/'SAFE'/None  <-- if unsafe set is used and 'terminated' is false, this stores 
    #                                      the text after "Result: " in stdout
    # 'safe' -> True iff 'result' == 'SAFE'
    # 'gnuplot_oct_data' -> the octogon data in the tool's gnuplot output
    # 'reachable_taylor_models' -> a list of TaylorModel objects parsed from out.flow

    is_safe = output['result'] == "SAFE"

    print "({}) ".format(output['result']),

    return is_safe, runtime

def main():
    '''main entry point'''

    # for a fixed order, find the maximum step size possible that still ensures safety
    min_step_size = 0.01

    for unsafe_condition in ["x >= 0.75", "x >= 0.73"]:
        for order in [3, 5, 7]:
            safe_runtime = None
            num_steps = 1

            print "Finding maximum safe Flow* step size for condition '{}' with TM order {}".format(
                unsafe_condition, order)

            while True:
                safe, runtime = run(unsafe_condition, order, num_steps * min_step_size)

                if not safe:
                    low = num_steps / 2
                    high = num_steps
                    
                    break

                safe_runtime = runtime
                num_steps *= 2

            print ""

            if safe_runtime is None:
                print "Condition '{}' with order {} was unsafe even at minimum step size: {}\n".format(
                    unsafe_condition, order, min_step_size)
                continue

            # binary search between high and low to find the boundary of safety
            # low is always safe, high is always unsafe
            print "Found unsafe step size. Doing binary search between {} and {}.".format(
                low * min_step_size, high * min_step_size)
            while (high - low) > 1:
                mid = (high + low) / 2
                safe, runtime = run(unsafe_condition, order, mid * min_step_size)

                if safe:
                    low = mid
                    safe_runtime = runtime
                else:
                    high = mid

            print ""
            print "Completed Analaysis for condition '{}' with order {}:".format(unsafe_condition, order)
            print "Largest safe step size was {} (runtime {} sec). Step size of {} was unsafe.\n".format(
                low * min_step_size, safe_runtime, high * min_step_size)

if __name__ == '__main__':
    main()
