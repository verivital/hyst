'''Unit tests for the interval optimize utils in pythonbridge'''

import unittest
import math
from sympy.core import symbols
from sympy.functions.elementary.trigonometric import sin
from sympy.mpmath import mpi as interval
import interval_optimize as opt

class TestIntervalOptimize(unittest.TestCase):
    'Unit tests for pysim utils'

    def test_eval_eq(self):
        'test simple evaluation'
        x = symbols('x')
        eq = sin(x + 0.01)
        #eq = 1*x*x + (0.2-x) / x + sin(x+0.01) + sqrt(x + 1) + cos(x + 0.01) + tan(x + 0.01) - (x+0.1)**(x+0.1)

        result = opt.eval_eq(eq, {'x':interval(0.20, 0.21)})

        expected = [math.sin(0.21), math.sin(0.22)]

        self.assertAlmostEquals(result.a, expected[0])
        self.assertAlmostEquals(result.b, expected[1])

    def test_eval_eq_multi(self):
        '''test for the eval_eq_multi function'''
        x = symbols('x')
        eq1 = x + interval(0.1)
        eq2 = x + interval(0.2)
        range1 = {'x':interval(0, 1)}
        range2 = {'x':interval(1, 2)}

        # todo: this test currently fails because _multi was only coded
        # for a SINGLE equation, not multiple ones, update it!!!!
        res = opt.eval_eq_multi([eq1, eq2], [range1, range2])

        self.assertAlmostEquals(res[0].a, 0.1)
        self.assertAlmostEquals(res[0].b, 1.1)
        self.assertAlmostEquals(res[1].a, 1.2)
        self.assertAlmostEquals(res[1].b, 2.2)

if __name__ == '__main__':
    unittest.main()







