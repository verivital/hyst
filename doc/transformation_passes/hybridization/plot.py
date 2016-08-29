'''Script for running time-triggered hybridization on the althoff's hscc 2013 vanderpol example
Stanley Bak (05-2016)
'''

import time

# make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
import hybridpy.hypy as hypy

def main():
    '''main entry point for vanderpol_althoff script'''

    print "\nGenerating out_vanderpol_hybridized_plot.png (~17 seconds)..."

    simtype = 'starcorners'
    step = '0.05'
    maxtime = '5.5' # 5.5
    bloat = '0.05' # 0.05 
    picount = '31' # '31'
    pimaxtime = '1'
    opt = "basinhopping" # use 'kodiak' for a verified one

    # skip error modes so spaceex produces a plot, disable to do DCEM check
    skip_error_modes = True
    
    pass_params = '-T ' + maxtime + \
            ' -S ' + simtype + \
            ' -delta_tt ' + step + \
            ' -n_pi ' + picount + \
            ' -delta_pi ' + pimaxtime + \
            ' -epsilon ' + bloat + \
            ' -opt ' + opt

    if skip_error_modes:
        pass_params += ' -noerror'

    print "Using Hybridize Pass Parameters: {}".format(pass_params)
    plot_vanderpol(pass_params)

def plot_vanderpol(pass_params):
    '''plot widths using vanderpol althoff
    '''

    name = 'vanderpol_althoff'

    e = hypy.Engine('spaceex')

    e.set_input(name + '.xml')
    e.set_output('out_vanderpol_hybridized_plot.xml')
    e.add_pass('hybridizemt', pass_params)
        
    start = time.time()
    print 'Running computation... ',
    code = e.run(print_stdout=True, image_path='out_vanderpol_hybridized_plot.png')['code']
    dif = time.time() - start
    print 'done. Finished in ' + str(dif) + ' seconds'

    if code != hypy.RUN_CODES.SUCCESS:
        raise RuntimeError('Error in ' + name + ': ' + str(code))
        
if __name__ == '__main__':
    main()









