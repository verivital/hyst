'''Python module for performing interval arithmetic based optimization'''
# Stanley Bak
# May 2015

# This module provides a function, evalEq, which
# maximizes an arbitrary sympy function using interval arithmetic
#
# Alternatively, if you have a single function with multiple sets
# of bounds you can call evalEqMulti, which returns a list of
# intervals and is likely more efficient
#
# For example you could do:
#
# x = symbols('x');
# print evalEq(cos(x) + x, {'x':interval(-0.1, 0.1)});
#
# which would print out:
#
# [0.89500416527802562072, 1.1000000000000000888]

from sympy.core import Mul, Expr, Add, Pow, Symbol, Number, Float
from sympy.functions.elementary.trigonometric import sin, cos, tan
from sympy.core import symbols
from sympy import *
from sympy.polys import PolynomialError
from sympy.polys.polyfuncs import horner
from sympy.mpmath import mpi as interval
from sympy.mpmath import iv
import copy

def eval_eq(e, subs=None):
    """returns the interval evaluation of this sympy equation, subs is a
    dictionary mapping variable names to interval/constant values

    for example subs={'x':interval(4,5), 'y':7}
    """

    return eval_eq_multi(e, [subs])[0]

def _simplify_eq(e):
    '''simplify an equation and try to put it into horner form'''
    e = simplify(e)

    try:
        e = horner(e)
    except (PolynomialError, AttributeError):
        pass

    return e

def eval_eq_multi(e, subs_list):
    """returns the interval evaluation of a sympy equation,
    over a set of domains, passed in as a list of substitutions
    each element of subList is a
    dictionary mapping variable names to interval/constant values

    for example {'x':interval(4,5), 'y':7}
    """

    return eval_eq_multi_branch_bound(e, subs_list, None)

def eval_eq_multi_branch_bound(e, subs_list, bound):
    """returns the interval evaluation of a sympy equation,
    over a set of domains, passed in as a list of substitutions
    each element of subList is a
    dictionary mapping variable names to interval/constant values
    bound is the maximum interval width (will be split otherwise), can be None

    for example subs_list={'x':interval(4,5), 'y':7}, bound = 0.1
    """

    e = _simplify_eq(e)

    rv = []

    for subs in subs_list:
        if bound is None:
            rv.append(_eval_eq_direct(e, subs))
        else:
            rv.append(_eval_eq_branch_bound(e, subs, bound))

    return rv

def _eval_eq_branch_bound(e, subs, bound):
    '''do branch and bound interval evaluation'''
    rv = None

    for var, i in subs.iteritems():
        if i.delta > bound:
            # recursive cases, use smaller bounds
            bound_left = copy.deepcopy(subs)
            bound_left[var] = interval(i.a, (i.a + i.b) / 2)
            rv_left = _eval_eq_branch_bound(e, bound_left, bound)

            bound_right = copy.deepcopy(subs)
            bound_right[var] = interval((i.a + i.b)/2, i.b)
            rv_right = _eval_eq_branch_bound(e, bound_right, bound)

            rv = interval(min(rv_left.a, rv_right.a), max(rv_left.b, rv_right.b))
            break

    if rv == None:
        # base case, interval was small enough
        rv = _eval_eq_direct(e, subs)

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
                rv = f(_eval_eq_direct(e.args[0], subs))
                break


    if rv == None:
        raise RuntimeError("Type '" + str(type(e)) + "' is not yet implemented for interval evaluation. " + \
                            "Subexpression was '" + str(e) + "'.")

    return rv

def _test():
    '''runs module unit tests'''

    # Test below - tries every supported subexpression - takes a few seconds
    x = symbols('x')
    eq = sin(x + 0.01)
    #eq = 1*x*x + (0.2-x) / x + sin(x+0.01) + sqrt(x + 1) + cos(x + 0.01) + tan(x + 0.01) - (x+0.1)**(x+0.1)

    print eval_eq(eq, {'x':interval(0.20, 0.21)})

########################################################################
# Test below - makes sure evalEqMulti works as expected
    x = symbols('x')
    eq = x + interval(0.1)
    range1 = {'x':interval(0, 1)}
    range2 = {'x':interval(1, 2)}

    for i in eval_eq_multi(eq, [range1, range2]):
        print i

########################################################################
# Test below - makes sure eval_eq_multi_branch_bound works as expected
    x = symbols('x')
    eq = x*x - 2*x
    range1 = {'x':interval(0, 2)}

    for i in eval_eq_multi_branch_bound(eq, [range1], 0.1):
        print i

#_test()

