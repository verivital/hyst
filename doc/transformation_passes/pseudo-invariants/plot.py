'Script for pseudo-invariant demo'

# make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
import hybridpy.hypy as hypy

def main():
    'run both without and with pi'

    print "Running original model to produce orignal.png (~12 seconds)"
    run_without_pi()

    print "Running WITH pseudo-invariants to produce pi.png (~11 seconds)"
    run_with_pi()

    print "Done."

def run_without_pi():
    'run without pi pass'

    e = hypy.Engine('flowstar')
    e.set_input('neuron.xml')
    code = e.run(image_path='original.png')['code']

    if code != hypy.Engine.SUCCESS:
        raise RuntimeError('Error:' + str(code))

def run_with_pi():
    'run with pi pass'

    e = hypy.Engine('flowstar')
    e.add_pass('pi_sim', '-times 2.0 1.0')
    e.set_input('neuron.xml')

    code = e.run(image_path='pi.png')['code']

    if code != hypy.Engine.SUCCESS:
        raise RuntimeError(str(code))

if __name__ == "__main__":
    main()









