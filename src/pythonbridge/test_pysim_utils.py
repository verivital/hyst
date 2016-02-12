'''Unit tests for the pysim utils in pythonbridge'''

import unittest
import pysim_utils as util
from hybridpy.pysim.hybrid_automaton import HybridAutomaton
from hybridpy.pysim.hybrid_automaton import HyperRectangle

def define_ha():
    '''make a test hybrid automaton and return it'''
    ha = HybridAutomaton()

    heater_off_controller_off = ha.new_mode('on')
    heater_off_controller_off.inv = lambda _: True
    heater_off_controller_off.der = lambda _, state: [2.0]

    return ha

class TestPySimUtils(unittest.TestCase):
    'Unit tests for pysim utils'

    def test_star(self):
        'test for star points'
        r = HyperRectangle([(1, 2), (10, 20), (100, 200), (500, 500)])        
        self.assertTrue(len(r.star()) == 8, 'incorrect number of star points')

    def test_simulate_ranges(self):
        'test simulate_ranges function'
        
        ha = define_ha()
        res = util.simulate_ranges(ha, 'on', [0.0], [(0, 2), (1, 3)])

        print "FINAL RESULT = {}".format(res)

        self.assertEqual(res, "0,4;2,6")

if __name__ == '__main__':
    unittest.main()







