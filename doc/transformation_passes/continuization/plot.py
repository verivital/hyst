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
    hyst_param = ('di_discrete_spaceex', 'di_periodic.xml', 'spaceex', None, None, None)

    plot(hyst_param)

def plot_continuized():
    '''run reachability on the continuized version of the model'''

    hyst_param = ('di_continuized_flowstar_piecewise', 'di_cont.xml', 'flowstar', '-orders 8 -aggregation interval', 
                    'continuization', '-var a -period 0.005 -timevar t -times 1.5 5 -bloats 4 4 -noerrormodes')

    plot(hyst_param)

def plot(plot_param):
    '''run a tool and make a plot'''

    name = plot_param[0]
    model = plot_param[1]
    tool_name = plot_param[2]
    tool_param = plot_param[3]
    pass_name = plot_param[4]
    pass_param = plot_param[5]

    e = hypy.Engine(tool_name, tool_param)

    e.set_input(model)

    if pass_name is not None:
        e.add_pass(pass_name, pass_param)

    print 'Running ' + name
    code = e.run(image_path=name + ".png", print_stdout=True)['code']
    print 'Finished ' + name

    if code != hypy.Engine.SUCCESS:
        print '\n\n-------------------\nError in ' + name + ': ' + str(code)

        raise RuntimeError('Error in ' + name + ': ' + str(code))

if __name__ == '__main__':
    main()
