'''Unit tests for the interval optimize utils in pythonbridge'''

import unittest
import math
from sympy.core import symbols
from sympy.functions.elementary.trigonometric import sin
import interval_optimize as opt
import scipy_optimize

def _opt_fun((x, y)):
    'test function to optimize for scipy'
    return (1 - x * x) * y - x

def _func0((x)):
    'another test function'
    return x ** 2 - (0.536 * x - 0.07182)

def _func1((x)):
    'another test function'
    return x ** 2 - (0.619 * x - 0.09579025)

class TestIntervalOptimize(unittest.TestCase):
    'Unit tests for pysim utils'

    def test_scipy(self):
        'test scipy optimization'
        lim = [(0, 1), (0, 1)]

        fun = _opt_fun 

        result = scipy_optimize.opt_multi([(fun, lim)])[0]

        expected = [-1, 1]
        self.assertAlmostEquals(result[0], expected[0])
        self.assertAlmostEquals(result[1], expected[1])

    def test_scipy_internal(self):
        'test the internal optimization in scipy'

        result = scipy_optimize.opt(_func0, [(0.2, 0.336)])

        expected = [0, 0.00462]
        self.assertAlmostEquals(result[0], expected[0], places=4)
        self.assertAlmostEquals(result[1], expected[1], places=4)


    def test_scipy2(self):
        'test scipy optimization on a more complex function'

        result = scipy_optimize.opt_multi([(_func0, [(0.2, 0.336),]), (_func1, [(0.236, 0.383),]),])

        expected = [[0, 0.00462], [0, 0.0054]]

        for i in xrange(2):
            self.assertAlmostEquals(result[i][0], expected[i][0], places=4)
            self.assertAlmostEquals(result[i][1], expected[i][1], places=4)

    def test_eval_eq(self):
        'test simple evaluation'
        x = symbols('x')
        eq = sin(x + 0.01)
        #eq = 1*x*x + (0.2-x) / x + sin(x+0.01) + sqrt(x + 1) + cos(x + 0.01) + tan(x + 0.01) - (x+0.1)**(x+0.1)

        result = opt.eval_eq(eq, {'x':(0.20, 0.21)})

        expected = [math.sin(0.21), math.sin(0.22)]

        self.assertAlmostEquals(result[0], expected[0])
        self.assertAlmostEquals(result[1], expected[1])

    def test_eval_eqs(self):
        '''test for the eval_eqs function'''
        x = symbols('x')
        eq1 = x + 0.1
        eq2 = x + 0.2
        range1 = {'x':(0, 1)}
        range2 = {'x':(1, 2)}

        res = opt.eval_eqs([eq1, eq2], [range1, range2])

        self.assertAlmostEquals(res[0][0], 0.1)
        self.assertAlmostEquals(res[0][1], 1.1)
        self.assertAlmostEquals(res[1][0], 1.2)
        self.assertAlmostEquals(res[1][1], 2.2)

    def test_eval_eqs_bounded(self):
        '''test for the eval_eq_multi branch & bound function'''
        x = symbols('x')
        y = symbols('y')
        eq1 = x*x - 2*x + y
        range1 = {'x':(1, 2), 'y':(1, 2)}
        # real answer = [0, 2]

        res1 = opt.eval_eqs([eq1], [range1])[0]
        res2 = opt.eval_eqs_bounded([eq1], [range1], 0.5)[0]
        res3 = opt.eval_eqs_bounded([eq1], [range1], 0.01, use_basinhopping=True, use_corners=False)[0]

        # bottom bound converges to 0
        self.assertTrue(res1[0] < res2[0])
        self.assertTrue(res2[0] < res3[0])
        self.assertTrue(res3[0] < 0)

        # in this case, top bound is correct
        self.assertTrue(res1[1] == res2[1])
        self.assertTrue(res2[1] == res3[1])
        self.assertTrue(res3[1] == 2)

        # check accuracy of bottom bound matches parameter
        self.assertTrue(res2[0] >= -0.5)
        self.assertTrue(res3[0] >= -0.01)

    def test_optimize_two(self):
        '''test which optimizes two functions using intervals'''

        y = symbols('y')
        x = symbols('x')

        res = opt.eval_eqs_bounded([2 * x + y - x], [{'y':(-0.2, -0.1), 'x':(0.0, 1.0),}], None, 
            use_basinhopping=True, use_corners=False)

        self.assertTrue(len(res) == 1)

if __name__ == '__main__':
    unittest.main()







