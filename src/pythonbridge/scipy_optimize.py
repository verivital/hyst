''' optimization module using scipy.basinhopping'''

from scipy import optimize
import time

def opt_multi(func, bounds_list, niter=50):
    '''optimize a function with several bounds'''
    rv = []

    for bounds in bounds_list:
        result = opt(func, bounds, niter)
        rv.append(result)

    return rv


def opt(func, bound, niter=50):
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

    return [minimum, maximum]

def __test():
    ''' test the optimization'''
    lim1 = [(0.9, 1.1), (-0.51, -0.49)]
    lim2 = [(0.8, 1.1), (-0.81, -0.69)]

    print opt_multi(lambda (x, y): ((1 - x * x) * y - x), [lim1, lim2])

	# time measurement (~1 second)
    start = time.clock()

    for i in xrange(60):
        lim = [(0, 1), (-0.2, -0.1)]
        fun = lambda (x, y): ((1 - x * x) * y - x)

        opt(fun, lim)

    dif = time.clock() - start
    print "seconds elapsed:", dif

#__test()


