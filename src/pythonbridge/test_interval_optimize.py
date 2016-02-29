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
        print "result = " + str(result)

        expected = [math.sin(0.211), math.sin(0.22)]
        print "expected = " + str(result)
        self.assertAlmostEquals(result, expected)


if __name__ == '__main__':
    unittest.main()







