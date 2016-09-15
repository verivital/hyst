'''Script for running time-triggered hybridization on the althoff's hscc 2013 vanderpol example
Stanley Bak (05-2016)
'''

import time

# make sure hybridpy is on your PYTHONPATH: hyst/src/hybridpy
import hybridpy.hypy as hypy

def main():
    '''main entry point for vanderpol_althoff script'''

    pass_params = make_pass_params()

    print "\nGenerating out_vanderpol_hybridized_plot.png (~13 seconds)..."
    plot(pass_params)

    print "\nChecking if domain contraction error modes are reachable (~11 seconds)..."
    check_dcem(pass_params)

def make_pass_params():
    'make the hybridization pass parameters for hyst'

    simtype = 'starcorners'
    step = '0.05'

    maxtime = '5.5' # 5.5
    bloat = '0.05' # 0.05 
    picount = '31' # '31'
    pimaxtime = '1'
    opt = "basinhopping" # use 'kodiak' for a verified one

    pass_params = '-T ' + maxtime + \
            ' -S ' + simtype + \
            ' -delta_tt ' + step + \
            ' -n_pi ' + picount + \
            ' -delta_pi ' + pimaxtime + \
            ' -epsilon ' + bloat + \
            ' -opt ' + opt

    return pass_params
    
def check_dcem(pass_params):
    '''check for domain contraction error modes (dcem)'''

    e = hypy.Engine('spaceex')

    e.set_input('vanderpol_althoff.xml')
    e.add_pass('hybridizemt', pass_params)
        
    start = time.time()
    print 'Running DCEM checking computation... ',
    result = e.run(parse_output=True)
    dif = time.time() - start

    print 'done. Finished in ' + str(dif) + ' seconds'

    if result['output']['safe'] != True:
        raise RuntimeError("Domain contraction error modes (DCEMs) were reachable.")
    else:
        print "Success: DCEMs were not reachable."

    if result['code'] != hypy.Engine.SUCCESS:
        raise RuntimeError("Hyst resturn code was: " + str(result['code']))


def plot(pass_params):
    'make the plot'

    pass_params += ' -noerror' # skip error modes since we're plotting

    e = hypy.Engine('spaceex')

    e.set_input('vanderpol_althoff.xml')
    e.set_output('out_vanderpol_hybridized_plot.xml')
    e.add_pass('hybridizemt', pass_params)
        
    start = time.time()
    print 'Running computation + plot... ',
    code = e.run(image_path='out_vanderpol_hybridized_plot.png')['code']
    dif = time.time() - start
    print 'done. Finished in ' + str(dif) + ' seconds'

    if code != hypy.Engine.SUCCESS:
        raise RuntimeError('Error: ' + str(code))


        
if __name__ == '__main__':
    main()









