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
    
    hyst_params = ['-hybridizemt', \
            '-T ' + maxtime + 
            ' -S ' + simtype + 
            ' -delta_tt ' + step + 
            ' -n_pi ' + picount +
            ' -delta_pi ' + pimaxtime + 
            ' -epsilon ' + bloat + 
            ' -opt ' + opt]

    if skip_error_modes:
        hyst_params[1] += ' -noerror'

    print "Using Parameters: " + str(hyst_params)
    plot_vanderpol(hyst_params)

def plot_vanderpol(hyst_params):
    '''plot widths using vanderpol althoff
    '''

    name = 'vanderpol_althoff'

    e = hypy.Engine()

    e.set_tool_params(hyst_params)          
    e.set_save_model_path('out_vanderpol_hybridized_plot.xml')
    e.set_output_image('out_vanderpol_hybridized_plot.png')

    e.set_print_terminal_output(True)
    e.set_model(name + '.xml')
    e.set_tool('spaceex')
        
    start = time.time()
    print 'Running computation... ',
    code = e.run(make_image=True)
    dif = time.time() - start
    print 'done. Finished in ' + str(dif) + ' seconds'

    if code != hypy.RUN_CODES.SUCCESS:
        raise RuntimeError('Error in ' + name + ': ' + str(code))
        
if __name__ == '__main__':
    main()









