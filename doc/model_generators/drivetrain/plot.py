'''Script for generating drivetrain benchmark and running with pysim'''

# make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
import hybridpy.hypy as hypy

def main():
    '''main entry point'''

    theta = 1

    gen_drivetrain_pysim(theta)
 
def gen_drivetrain_pysim(theta):
    'generate a drivetrain benchmark instance and plot a simulation'

    title = "Drivetrain (Theta={})".format(theta)
    image_path = "pysim_drivetrain_theta{}.png".format(theta)
    output_path = "pysim_drivetrain{}.py".format(theta)
    gen_param = '-theta {} -init_points 10'.format(theta)

    tool_param = "-title \"{}\" -xdim 0 -ydim 2".format(title)

    e = hypy.Engine('pysim', tool_param)
    e.set_generator('drivetrain', gen_param)
    #e.set_output(output_path)
    #e.set_verbose(True)
    
    #e.add_pass("sub_constants", "")
    #e.add_pass("simplify", "-p")
    
    print 'Running ' + title
    res = e.run(print_stdout=True, image_path=image_path)
    print 'Finished ' + title

    if res['code'] != hypy.Engine.SUCCESS:
        raise RuntimeError('Error in ' + title + ': ' + str(res['code']))

if __name__ == '__main__':
    main()
