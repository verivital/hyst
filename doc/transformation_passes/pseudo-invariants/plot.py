'Script for pseudo-invariant demo'

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

    e = hypy.Engine()

    e.set_print_terminal_output(False)
    e.set_model('neuron.xml')
    e.set_tool('flowstar')
    e.set_output_image('original.png')
    code = e.run()

    if code != hypy.RUN_CODES.SUCCESS:
        raise RuntimeError('Error:' + str(code))

def run_with_pi():
    'run with pi pass'

    hyst_params = ['-pass_pi_sim', '-times 2.0 1.0']
    e = hypy.Engine()

    e.set_print_terminal_output(False)
    e.set_model('neuron.xml')
    e.set_tool('flowstar')
    e.set_tool_params(hyst_params)
    e.set_output_image('pi.png')
    code = e.run()

    if code != hypy.RUN_CODES.SUCCESS:
        raise RuntimeError('Error:' + str(code))

if __name__ == "__main__":
    main()









