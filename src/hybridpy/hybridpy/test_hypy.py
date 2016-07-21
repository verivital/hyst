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

        e = hypy.Engine()
        e.set_model(model) # sets input model path
        e.set_tool("pysim") # sets tool name to use
        code = e.run(make_image=False)
        self.assertEqual(code, hypy.RUN_CODES.SUCCESS)

    def test_pyrrt(self):
        '''run the pyrrt'''
        
        model = get_script_dir() + "/../../../examples/toy/toy.xml"

        e = hypy.Engine()
        e.set_model(model) # sets input model path
        e.set_tool("pysim") # sets tool name to use
        code = e.run(make_image=False)
        self.assertEqual(code, hypy.RUN_CODES.SUCCESS)

if __name__ == '__main__':
    unittest.main()
