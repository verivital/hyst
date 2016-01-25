'''Unit tests for the pysim module'''

import unittest
from hybridpy.pysim.hybrid_automaton import HyperRectangle
from hybridpy.pysim.simulate import init_list_to_q_list

class TestPySim(unittest.TestCase):
    'Unit tests for pysim'

    def test_init_unique(self):
        'test uniqueness of initial states'
        r = HyperRectangle([(1, 1), (2, 2), (3, 3)])

        init_states = [('first', r), ('second', r), ('first', r)] 

        q_list = init_list_to_q_list(init_states, center=True, star=True, corners=True)

        self.assertTrue(len(q_list) == 2, "converted initial sim states have two points")

    def test_star(self):
        'test for star points'
        r = HyperRectangle([(1, 2), (10, 20), (100, 200), (500, 500)])        
        self.assertTrue(len(r.star()) == 8, 'incorrect number of star points')

    def test_unique_corners(self):
        'test for unique corner points'
        r = HyperRectangle([(1, 2), (10, 20), (100, 200), (500, 500)])        
        corners = r.unique_corners()

        self.assertTrue(len(corners) == 8, 'incorrect number of unique corner points')

        # make sure they're unique
        s = {}

        for c in corners:
            s[str(c)] = True

        self.assertTrue(len(s) == 8, 'unique_corners() did not give unique points')

if __name__ == '__main__':
    unittest.main()
