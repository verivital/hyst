'''Script for running continuization for the double integrator example'''

# make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
import hybridpy.hypy as hypy

def main():
    '''main entry point'''

    print "Running discrete reacability in SpaceEx, making di_discrete_spaceex.png (~200 second)"
    plot_discrete()

    print "Running on continuized version using Flow* (~4 seconds)"
    plot_continuized()

def plot_discrete():
    '''run reachability on the original, discretely actuated model'''
    hyst_param = ('di_discrete_spaceex', 'di_periodic.xml', 'spaceex', None)

    plot(hyst_param)

def plot_continuized():
    '''run reachability on the continuized version of the model'''

    hyst_param = ('di_continuized_flowstar_piecewise', 'di_cont.xml', 'flowstar', ['-tp', 
                    'orders=8:aggregation=interval', '-pass_continuization', 
                    '-var a -period 0.005 -timevar t -times 1.5 5 -bloats 4 4 -noerrormodes'])

    plot(hyst_param)

def plot(plot_param):
    '''run a tool and make a plot'''

    name = plot_param[0]
    model = plot_param[1]
    tool = plot_param[2]
    tool_params = plot_param[3]

    e = hypy.Engine()
    e.set_save_terminal_output(True)

    #e.set_save_model_path("out.model")

    e.set_model(model)
    e.set_tool(tool)

    if tool_params is not None:
        e.set_tool_params(tool_params)

    e.set_print_terminal_output(True)

    e.set_output_image(name + '.png')

    print 'Running ' + name
    code = e.run()

    print 'Finished ' + name

    if code != hypy.RUN_CODES.SUCCESS:
        print '\n\n-------------------\nError in ' + name + ': ' + str(code) + "; terminal output was:"
        print e.get_terminal_output()

        raise RuntimeError('Error in ' + name + ': ' + str(code))

if __name__ == '__main__':
    main()
