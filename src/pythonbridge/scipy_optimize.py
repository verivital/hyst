''' optimization module using scipy.basinhopping'''

from scipy import optimize
from multiprocessing import Pool
import time

def opt_multi(func_bounds_list):
    '''optimize a function with several functions and bounds
    func_bounds list is a list of tuples, each tuple is (func, bounds)
    '''

    p = Pool()
    result = p.map(opt_star, func_bounds_list)
    p.close()

    return result

def opt_star(params):
    '''call opt, but untuple the parameters
    '''
    func = params[0]
    bound = params[1]

    return opt(func, bound)

def opt(func, bound, niter=30):
    ''' optimize a function in a given bounds limit, returns [min, max]'''

    stepsize = None
    center = []

    for interval in bound:
        center.append((interval[0] + interval[1]) / 2.0)
        width = interval[1] - interval[0]

        if stepsize is None or width < stepsize:
            stepsize = width

    minimum = optimize.basinhopping(func, \
        center, minimizer_kwargs=dict(method='L-BFGS-B', bounds=bound),\
        niter=niter, stepsize=stepsize).fun

    maximum = -optimize.basinhopping(lambda x: -func(x), \
        center, minimizer_kwargs=dict(method='L-BFGS-B', bounds=bound),\
        niter=niter, stepsize=stepsize).fun

    # scipy bug: the doc says basinhopping returns a Result object,
    # but I sometimes get a float, sometimes a numpy float, some sometimes and numpy array
    minimum = float(minimum)
    maximum = float(maximum)

    return [minimum, maximum]

def __func((x, y)):
    '''test dynamics'''
    return (1 - x * x) * y - x

def __test_par(count):
    '''parallel execution test'''
    start = time.time()
    func_lim = []

    for i in xrange(count):
        lim = [(0, i), (-0.2, -0.1)]
        func_lim.append((__func, lim))

    opt_multi(func_lim)

    dif = time.time() - start
    print count, "parallel optimizations, seconds elapsed:", dif

def __test_seq(count):
    '''sequential execution test'''
    start = time.time()

    for i in xrange(count):
        lim = [(0, i), (-0.2, -0.1)]
        fun = __func # lambda (x, y): ((1 - x * x) * y - x)

        opt(fun, lim)

    dif = time.time() - start
    print count, "single optimizations, seconds elapsed:", dif

def __test():
    '''test the optimization'''
    count = 170

    # we get about a 4:1 ratio which makes sense since it's a four-core machine
    __test_seq(count)
    __test_par(count)

#__test()


