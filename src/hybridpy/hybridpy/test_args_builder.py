'''
Unit tests for args builder (part of hypy).
'''

import unittest

# assumes hybridpy is on your PYTHONPATH
from hybridpy.args_builder import ModelArgsBuilder
import hybridpy.hypy as hypy

class TestArgsBuilder(unittest.TestCase):
    'Unit tests for args builder'

    def test_build_model(self):
        'test for creating a simple model using the build model generator'
        
        m = ModelArgsBuilder(["t", "x"])

        m.set_initial_rect([(0, 0), (41, 42)])
        m.add_error_condition("on", "x > 80")
        m.set_time_bound(4.5)
        m.add_mode("on", "true", ["1.2 * x", "x * y"])
        m.add_mode("off", "t < 10", ["0", "0"])
        m.add_transition("on", "off", "x < t", ["t", "1"])
        
        output = m.get_hyst_params()

        sub_args = ['-vars', 't', 'x', 
                    '-time_bound', '4.5',
                    '-init', '0', '0', '41', '42',
                    '-error', 'on', 'x > 80',
                    '-modes', 'on', 'true', '1.2 * x', 'x * y', 'off', 't < 10', '0', '0',
                    '-transitions', 'on', 'off', 'x < t', 't', '1'
                    ]

        arg_string = " ".join(["\"" + s + "\"" for s in sub_args])
        
        expected = ['-generate', 'build', arg_string]

        self.assertEqual(output, expected)

    def test_build_model_run_hyst(self):
        'generate a model and run it through hypy'
        m = ModelArgsBuilder(["t", "x"])

        m.set_initial_rect([(0, 0), (41, 42)])
        m.add_error_condition("on", "x > 80")
        m.set_time_bound(4.5)
        m.add_mode("on", "true", ["1.2 * x", "x * y"])
        m.add_mode("off", "t < 10", ["0", "0"])
        m.add_transition("on", "off", "x < t", ["t", "1"])
        
        e = hypy.Engine()

        e.set_tool('pysim')
        e.set_output_image('out.png')

        e.set_tool_params(m.get_hyst_params())

        code = e.run(run_tool=False)

        self.assertEqual(code, hypy.RUN_CODES.SUCCESS)

if __name__ == '__main__':
    unittest.main()
