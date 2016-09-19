'''
Unit tests for hypy.
'''

import unittest
import os

# assumes hybridpy is on your PYTHONPATH
import hybridpy.hypy as hypy


def get_script_dir():
    '''get the dir path this script'''
    return os.path.dirname(os.path.realpath(__file__))

class TestHypy(unittest.TestCase):
    'Unit tests for hypy'

    def test_pysim(self):
        '''run the simple example from the hyst readme'''
        
        model = get_script_dir() + "/../../../examples/toy/toy.xml"

        e = hypy.Engine('pysim')
        e.set_input(model) # sets input model path

        res = e.run()
        
        self.assertEqual(res['code'], hypy.Engine.SUCCESS)

if __name__ == '__main__':
    unittest.main()
