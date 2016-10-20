'''Script for generating drivetrain benchmark and running with pysim'''

# make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
import hybridpy.hypy as hypy

def main():
    '''main entry point'''

    theta = 1

    print "todo: remove hylaa gen here"
    #gen_drivetrain_pysim(theta)
    gen_drivetrain_hylaa(theta)   
 
def gen_drivetrain_pysim(theta):
    'generate a drivetrain benchmark instance and plot a simulation'

    title = "Drivetrain (Theta={})".format(theta)
    image_path = "pysim_drivetrain_theta{}.png".format(theta)
    output_path = "pysim_drivetrain{}.py".format(theta)
    gen_param = '-theta {} -high_input'.format(theta)
    
    tool_param = "-rand 10 -title \"{}\"".format(title)

    e = hypy.Engine('pysim', tool_param)
    e.set_generator('drivetrain', gen_param)
    e.set_output(output_path)
    e.set_verbose(True)
    
    #e.add_pass("sub_constants", "")
    #e.add_pass("simplify", "-p")
    
    print 'Running ' + title
    e.run(print_stdout=True, image_path=image_path)
    print 'Finished ' + title
    res = e.run()

    if res['code'] != hypy.Engine.SUCCESS:
        raise RuntimeError('Error in ' + title + ': ' + str(res['code']))

def gen_drivetrain_hylaa(theta):
    'generate a drivetrain benchmark instance for hylaa'

    title = "Drivetrain (Theta={})".format(theta)
    gen_param = '-theta {} -high_input'.format(theta)
    
    e = hypy.Engine('hylaa', '-python_simplify')
    e.set_generator('drivetrain', gen_param)
    e.set_output('hylaa_drivetrain{}.py'.format(theta))
    
    print 'Running ' + title
    e.run(print_stdout=True, run_tool=False)
    print 'Finished ' + title
    res = e.run()

    if res['code'] != hypy.Engine.SUCCESS:
        raise RuntimeError('Error in ' + title + ': ' + str(res['code']))

if __name__ == '__main__':
    main()
