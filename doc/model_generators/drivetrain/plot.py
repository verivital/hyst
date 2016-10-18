'''Script for generating drivetrain benchmark and running with pysim'''

# make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
import hybridpy.hypy as hypy

def main():
    '''main entry point'''

    theta = 2
    gen_drivetrain(theta)
    
def gen_drivetrain(theta):
    'generate a drivetrain benchmark instance and plot a simulation'

    title = "Drivetrain (Theta={})".format(theta)
    image_path = "drivetrain_theta{}.png".format(theta)
    output_path = "generated_drivetrain{}.py".format(theta)
    gen_param = '-theta {}'.format(theta)
    
    tool_param = "-rand 10 -title {}".format(title)

    e = hypy.Engine('pysim', tool_param)
    e.set_generator('drivetrain', gen_param)
    e.set_output(output_path)
    
    print 'Running ' + title
    e.run(print_stdout=True, image_path=image_path)
    print 'Finished ' + title
    res = e.run()

    if res['code'] != hypy.Engine.SUCCESS:
        raise RuntimeError('Error in ' + title + ': ' + str(res['code']))

if __name__ == '__main__':
    main()
