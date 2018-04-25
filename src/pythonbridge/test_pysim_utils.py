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

    def test_sim_der_range2(self):
        'test for continuization simulation with urgent mode'

        def define_ha_test():
            '''make the hybrid automaton and return it'''
            # Variable ordering: [x, v, a, t]
            ha = HybridAutomaton()
            on = ha.new_mode('on')
            on.inv = lambda state: True
            on.der = lambda _, state: [state[1], state[2], -10 * state[1] - 3 * state[2], 1]
            init = ha.new_mode('init')
            init.inv = lambda state: True
            t = ha.new_transition(init, on)
            t.guard = lambda state: True
            t.reset = lambda state: [None, None, 10 * (1 - state[0]) + 3 * -state[1], None]
            return ha

        # actually we just want to make sure we don't get an exception here
        res = util.simulate_der_range(define_ha_test(), 2, 'init', [0, 0.05, 0, 0], [(0.0, 1.5), (1.5, 5.0)])

        if res.find(";") == -1:
            self.fail("unexpected result")

    def test_sim_overflow_error(self):
        'regression test for an overflow error that was encountered'

        ha = HybridAutomaton()
        ha.variables = ["barrier_clock", "x", "y"]
        on = ha.new_mode('on')

        def inv(state):
            'state invariant'
        
            rv = (state[1] ** 2) + (state[2] ** 2) - 9 <= 0.0001

            #print "inv returning {}".format(rv)

            return rv

        on.inv = inv
        on.der = lambda _, state: [1, -state[2], -(-state[1] + state[2] * (-state[1] ** 2 + 1))]
        on.der_interval_list = [[0, 0], [0, 0], [0, 0]]
        error = ha.new_mode('error')
        error.inv = lambda state: True
        error.der = lambda _, state: [0, 0, 0]
        error.der_interval_list = [[0, 0], [0, 0], [0, 0]]
        t = ha.new_transition(on, error)
        t.guard = lambda state: state[0] >= 5
        t.reset = lambda state: [None, None, None]

        res = util.simulate_multi_trajectory_time(ha, ['on'], [[0.0, 0.0, -2.9142074584950004]], 5.0001)
        
        # two points before, invariant should still be true
        point = res.split(';')[-2]

        _, t, x, y = point.split(",")

        self.assertTrue(inv([float(t), float(x), float(y)]), msg="Invariant should be true at last point")

if __name__ == '__main__':
    unittest.main()







