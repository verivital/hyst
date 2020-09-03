'''
Unit tests for hypy.
'''

import unittest
import os
import tempfile
import shutil

# assumes hybridpy is on your PYTHONPATH
import hybridpy.hypy as hypy
import numpy as np

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

        res = e.run(parse_output=True)
        
        self.assertEqual(res['code'], hypy.Engine.SUCCESS)
        np.testing.assert_allclose(res['output']['interval_bounds'], np.array([[0, 9],[0, 20], [0, 20]]))

    def test_plot_limits(self):
        '''test plot creation with xlim and ylim arguments'''
        model = get_script_dir() + "/../../../examples/toy/toy.xml"

        e = hypy.Engine('pysim')
        e.set_input(model) # sets input model path

        tmpdir = None
        try:
            tmpdir = tempfile.mkdtemp()
            image_path = tmpdir + "/myimage.png"
            res = e.run(image_path=image_path, xlim=[1, 5], ylim=[3, 7])
            self.assertEqual(res['code'], hypy.Engine.SUCCESS)
            self.assertTrue(os.path.exists(image_path))
            # Note: the resulting image contents are not tested
        finally:
            if tmpdir:
                shutil.rmtree(tmpdir)

if __name__ == '__main__':
    unittest.main()
