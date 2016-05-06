'''Unit tests for the pysim utils in pythonbridge'''

import unittest
import pysim_utils as util
from hybridpy.pysim.hybrid_automaton import HybridAutomaton
from hybridpy.pysim.hybrid_automaton import HyperRectangle

def define_ha():
    '''make a test hybrid automaton and return it'''
    ha = HybridAutomaton()

    m1 = ha.new_mode('on')
    m1.inv = lambda _: True
    m1.der = lambda _, state: [1, 2.0 * state[0]]

    m2 = ha.new_mode('off')
    m2.inv = lambda _: True
    m2.der = lambda _, state: [-1.0, -2.0]

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

    def test_simulate_set_time(self):
        'test for simulating a set of states for a fixed time'

        time = 2
        ha = define_ha()

        mode_names = ['on', 'on', 'off']
        points = [(0, 0), (1, 1.5), (1, 2)]

        res = util.simulate_set_time(ha, mode_names, points, time)
        part1, part2, part3 = res.split(';')
        mode1, x1, y1 = part1.split(',')
        mode2, x2, y2 = part2.split(',')
        mode3, x3, y3 = part3.split(',')
 
        self.assertEqual(mode1, "on")
        self.assertAlmostEqual(float(x1), 2.0, places=3)
        self.assertAlmostEqual(float(y1), 4.0, places=3)

        self.assertEqual(mode2, "on")
        self.assertAlmostEqual(float(x2), 3.0, places=3)
        self.assertAlmostEqual(float(y2), 9.5, places=3)

        self.assertEqual(mode3, "off")
        self.assertAlmostEqual(float(x3), -1.0, places=3)
        self.assertAlmostEqual(float(y3), -2.0, places=3)

    def test_sim_traj_time(self):
        'test for simulating points and getting back trajectories'

        time = 2
        ha = define_ha()

        res_list = util.simulate_multi_trajectory_time(ha, ['on', 'on'], [(0, 0), (0, 0)], time, min_steps=20)

        self.assertTrue(len(res_list.split('|')) == 2)

        for traj in res_list.split('|'):
            self.assertGreater(len(traj), 19)

            for ss in traj.split(';'):
                mode, x, y = ss.split(',')

                self.assertEquals(mode, 'on')

                # solution is y = x^2
                self.assertAlmostEqual(float(x) * float(x), float(y), places=2)

if __name__ == '__main__':
    unittest.main()







