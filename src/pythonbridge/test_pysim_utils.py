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
    heater_off_controller_off.der = lambda _, state: [1, 2.0 * state[0]]

    return ha

class TestPySimUtils(unittest.TestCase):
    'Unit tests for pysim utils'

    def test_star(self):
        'test for star points'
        r = HyperRectangle([(1, 2), (10, 20), (100, 200), (500, 500)])        
        self.assertTrue(len(r.star()) == 8, 'incorrect number of star points')

    def test_simulate_der_range(self):
        'test simulate_der_range function'
        
        ha = define_ha()
        
        # get the range between times 0-2 and times 1-3
        # t' == 1, y' == 2t
        res = util.simulate_der_range(ha, 1, 'on', [0.0, 0.0], [(0, 2), (1, 3)])
        self.assertEqual(res, "0.0,4.0;2.0,6.0")

if __name__ == '__main__':
    unittest.main()







