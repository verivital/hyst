'''
This module provides a functions for bounding a function using interval arithmetic.

eval_eq evaluate one function with one list of intervals for each variable

eval_eqs evaluates several functions (in parallel), each with a list of intervals

eval_eqs_bounded evaluates several functions (in parallel), each with a list of intervals,
with a guaranteed bound on error. The error bound is done by branching the intervals
until the error is small enough.

For example you could do:

x = symbols('x');
print evalEq(cos(x) + x, {'x':interval(-0.1, 0.1)});

which would print out:

[0.89500416527802562072, 1.1000000000000000888]

'''
# Stanley Bak
# May 2015

from sympy.core import Mul, Expr, Add, Pow, Symbol, Number, Float
from sympy.functions.elementary.trigonometric import sin, cos, tan
from sympy.core import symbols
from sympy.polys import PolynomialError
from sympy.polys.polyfuncs import horner

try:
    # windows
    from mpmath import mpi as interval, mpf
    from mpmath import iv
except:
    # linux/other
    from sympy.mpmath import mpi as interval, mpf
    from sympy.mpmath import iv

from multiprocessing import Pool
import copy
import scipy_optimize
from sympy import *

def eval_eq(e, subs=None):
    """returns the interval evaluation of this sympy equation, subs is a
    dictionary mapping variable names to interval/constant values

    for example subs={'x':interval(4,5), 'y':7}
    """

    return eval_eqs([e], [subs])[0]

def _simplify_eq(e):
    '''simplify an equation and try to put it into horner form'''
    e = simplify(e)

    try:
        e = horner(e)
    except (PolynomialError, AttributeError):
        pass

    return e

def eval_eqs(e_list, subs_list):
    """returns the interval evaluation of a list of sympy equations,
    over a set of domains, passed in as a list of substitutions
    each element of subList is a
    dictionary mapping variable names to interval/constant values

    for example {'x':interval(4,5), 'y':7}
    """

    return eval_eqs_bounded(e_list, subs_list)

def eval_eqs_bounded(e_list, subs_list, bound=None, use_basinhopping=False, use_corners=True, multithreaded=True):
    """returns the interval evaluation of a list of sympy equations,
    over a set of domains, passed in as a list of substitutions
    each element of subList is a
    dictionary mapping variable names to interval/constant values
    bound is the maximum error (interval may be split otherwise), can be None

    Internally, this function first under-approximates the result using basin-hopping or an interval evaluation,
    and then may split to ensure the error bound (difference between inteval bound and basin-hopping bound)
    """
    assert len(e_list) == len(subs_list)

    param_list = []

    for i in xrange(len(e_list)):
        e = e_list[i]
        subs = subs_list[i]

        param_list.append((e, subs, bound, use_basinhopping, use_corners))

    if multithreaded:
        p = Pool()
        rv = p.map(_optimize_single, param_list)
        p.close()
    else:
        rv = [_optimize_single(p) for p in param_list]

    return rv

def _eval_at_middle(e, subs):
    '''evaluate an expression in the corners of an interval range'''

    new_subs = {}

    for var, i in subs.items():
        new_subs[var] = (i[0] + i[1]) / 2.0

    return _eval_eq_direct(e, new_subs)

def _eval_at_corners(e, subs):
    '''evaluate an expression in the middle of an interval range'''
    rv = None

    max_iterator = 1

    for _ in xrange(len(subs)):
        max_iterator *= 2

    for i in xrange(max_iterator):
        new_subs = {}

        for var, item in subs.items():
            new_subs[var] = item[i % 2]
            i /= 2

        val = _eval_eq_direct(e, new_subs)

        if rv is None:
            rv = val

        if val.a < rv.a:
            rv = interval(val.a, rv.b)
        
        if val.b > rv.b:
            rv = interval(rv.a, val.b)

    return rv

def _basinhopping(e, subs):
    '''use scipy basinhopping to get an underestimate
    e is a sympy expression
    subs is the substitution map
    '''

    symbol_list = []
    limits = []

    for var, lim in subs.items():
        symbol_list.append(symbols(var))
        limits.append(lim)

    def eval_func(var_list):
        '''eval func used for optimization'''

        sub_list = {}
        for i in xrange(len(var_list)):
            sub_list[symbol_list[i]] = var_list[i]

        return float(e.evalf(subs=sub_list))

    i = scipy_optimize.opt(eval_func, limits, niter=10)

    return interval(i[0], i[1])

def _optimize_single(opt_params):
    '''mapped function for optimization of a single function in a single domain
    
    opt_params is a tuple (expression, subs, bound, use_basinhopping, use_corners)
    returns the interval
    '''

    (e, subs, bound, use_basinhopping, use_corners) = opt_params
    assert not (use_basinhopping and use_corners)

    rv = None
    e = _simplify_eq(e)

    if bound is None:
        rv = _eval_eq_direct(e, subs)
    else:
        under_approx = None

        if use_corners:
            under_approx = _eval_at_corners(e, subs)
        elif use_basinhopping:
            under_approx = _basinhopping(e, subs)
        else:
            under_approx = _eval_at_middle(e, subs)

        # under_approx param should be an array of size 1, since it gets updated
        rv = _eval_eq_bounded(e, subs, bound, [under_approx])

    # convert rv from interval to tuple (for pickling)
    return [float(mpf(rv.a)), float(mpf(rv.b))]

def _eval_eq_bounded(e, subs, error_bound, under_approx, split_dim=0):
    '''do bounded interval evaluation
    error_bound is the desired error bound
    under_approx is an array of size 1, which is an under-approximation  on the result 
                of the interval evaluation, and may be updated as we go
    split_dim is the next dimension to split (this gets cycled)
    '''
    rv = _eval_eq_direct(e, subs)
    variables = subs.keys()

    # adjust the under approximation
    if rv.b < under_approx[0].a:
        under_approx[0] = interval(rv.b, under_approx[0].b)

    if rv.a > under_approx[0].b:
        under_approx[0] = interval(under_approx[0].a, rv.a)

    if rv.a < (under_approx[0].a - error_bound) or rv.b > (under_approx[0].b + error_bound):
        # split along split_dim
        var = variables[split_dim]
        i = subs[var]
        next_split_dim = (split_dim + 1) % len(subs)
        
        bound_left = copy.deepcopy(subs)
        bound_left[var] = (i[0], (i[0] + i[1]) / 2.0)
        
        bound_right = copy.deepcopy(subs)
        bound_right[var] = ((i[0] + i[1])/2.0, i[1])        

        rv_left = _eval_eq_bounded(e, bound_left, error_bound, under_approx, next_split_dim)
        rv_right = _eval_eq_bounded(e, bound_right, error_bound, under_approx, next_split_dim)

        rv = interval(min(rv_left.a, rv_right.a), max(rv_left.b, rv_right.b))

    return rv


def _eval_eq_direct(e, subs=None):
    '''do the actual interval evaluation'''
    rv = None

    if not isinstance(e, Expr):
        raise RuntimeError("Expected sympy Expr: " + repr(e))

    if isinstance(e, Symbol):
        if subs == None:
            raise RuntimeError("Symbol '" + str(e) + "' found but no substitutions were provided")

        val = subs.get(str(e))

        if val == None:
            raise RuntimeError("No substitution was provided for symbol '" + str(e) + "'")

        rv = val
    elif isinstance(e, Number):
        rv = interval(Float(e))
    elif isinstance(e, Mul):
        rv = interval(1)

        for child in e.args:
            rv *= _eval_eq_direct(child, subs)
    elif isinstance(e, Add):
        rv = interval(0)

        for child in e.args:
            rv += _eval_eq_direct(child, subs)
    elif isinstance(e, Pow):
        term = _eval_eq_direct(e.args[0], subs)
        exponent = _eval_eq_direct(e.args[1], subs)

        rv = term**exponent
    else:
        # interval function evaluation (like sin)
        func_map = [(sin, iv.sin), (cos, iv.cos), (tan, iv.tan)]

        for entry in func_map:
            t = entry[0] # type
            f = entry[1] # function to call

            if isinstance(e, t):
                inner_arg = _eval_eq_direct(e.args[0], subs) 
                rv = f(inner_arg)
                break

    if rv == None:
        raise RuntimeError("Type '" + str(type(e)) + "' is not yet implemented for interval evaluation. " + \
                            "Subexpression was '" + str(e) + "'.")

    return rv




