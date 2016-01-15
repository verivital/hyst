# hypy (hybridpy) project
# Stanley Bak 
# Jan 2015

#### Description

Hypy can be used to run Hyst (including its transformation passes), as well as various reachability and simulation tools, and then interpret their output. This can then be looped in a python script, enabling high-level hybrid systems analysis which involves running tools multiple times.

#### Setup

For easy usage, point your PYTHONPATH environment variable to this directory. To setup hypy to run the tools, you'll need to define environment variables to the appropriate binaries. At a minimum (for conversion), you must set HYST_BIN to point to the Hyst jar file. On Ubuntu, here's what I have in my .profile to do the setup:


export HYST_BIN="/home/stan/repositories/hyst/src/Hyst.jar"
export FLOWSTAR_BIN="/home/stan/tools/flowstar-1.2.3/flowstar"
export SPACEEX_BIN="/home/stan/tools/spaceex/spaceex"
export DREACH_BIN="/home/stan/tools/dreach/dReal-2.15.01-linux/bin/dReach"
export HYCREATE_BIN="/home/stan/tools/HyCreate2.8/HyCreate2.8.jar"

export PYTHONPATH="${PYTHONPATH}:/home/stan/repositories/hyst/src/hybridpy"

#### Pysim

Pysim is a simple simulation library for a hybrid automata. It is written in python, and can be run using hypy.

pysim dependencies:

* scipy: http://www.scipy.org/install.html

* matplotlib:  http://matplotlib.org/users/installing.html

#### Example

A simple hypy script to test if it's working (you may need to adjust your path to the model file) is:


'''
Test hypy on the toy model (path may need to be adjusted)
'''

# assumes hybridpy is on your PYTHONPATH
import hybridpy.hypy as hypy

def main():
    '''run the toy model and produce a plot'''
    
    model = "/home/stan/repositories/hyst/examples/toy/toy.xml"
    out_image = "toy_output.png"
    tool = "pysim" # pysim is the built-in simulator; try flowstar or spaceex

    e = hypy.Engine()
    e.set_model(model) # sets input model path
    e.set_tool(tool) # sets tool name to use
    #e.set_print_terminal_output(True) # print output to terminal? 
    #e.set_save_model_path(converted_model_path) # save converted model?
    e.set_output_image(out_image) # sets output image path
    #e.set_tool_params(["-tp", "jumps=2"]) # sets parameters for hyst conversion

    code = e.run()

    if code != hypy.RUN_CODES.SUCCESS:
        print "engine.run() returned error: " + str(code)
        exit(1)
        
    print "Completed successfully"

if __name__ == "__main__":
    main()



