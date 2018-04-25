'''
Unit tests for args builder (part of hypy).
'''

import unittest

# assumes hybridpy is on your PYTHONPATH
from hybridpy.args_builder import BuildGenArgsBuilder
import hybridpy.hypy as hypy

class TestArgsBuilder(unittest.TestCase):
    'Unit tests for args builder'

    def test_build_model(self):
        'test for creating a simple model using the build model generator'
        
        m = BuildGenArgsBuilder(["t", "x"])

        m.add_init_condition("on", "t == 0 && 41 <= x <= 42")
        m.add_error_condition("on", "x >= 80")
        m.set_time_bound(4.5)
        m.add_mode("on", "true", ["1.2 * x", "x * t"])
        m.add_mode("off", "t <= 10", ["0", "0"])
        m.add_transition("on", "off", "x <= t", ["t", "1"])
        
        output = m.get_generator_param()

        sub_args = ['-vars', 't', 'x', 
                    '-time_bound', '4.5',
                    '-init', 'on', 't == 0 && 41 <= x <= 42',
                    '-error', 'on', 'x >= 80',
                    '-modes', 'on', 'true', '1.2 * x', 'x * t', 'off', 't <= 10', '0', '0',
                    '-transitions', 'on', 'off', 'x <= t', 't', '1'
                   ]

        expected = " ".join(["\"" + s + "\"" if " " in s else s for s in sub_args])
        
        self.assertEqual(output, expected)

    def test_build_model_run_hyst(self):
        'generate a model and run it through hypy'
        m = BuildGenArgsBuilder(["t", "x"])

        m.add_init_condition("on", "t == 0 && 41 <= x <= 42")
        m.add_error_condition("on", "x >= 80")
        m.set_time_bound(4.5)
        m.add_mode("on", "true", ["1.2 * x", "x * t"])
        m.add_mode("off", "t <= 10", ["0", "0"])
        m.add_transition("on", "off", "x <= t", ["t", "1"])
        
        e = hypy.Engine('pysim')
        e.set_generator('build', m.get_generator_param())

        res = e.run(run_tool=False)

        self.assertEqual(res['code'], hypy.Engine.SUCCESS)

    def test_build_nondeterministic(self):
        'generate a model with nondeterministic dynamics'

        m = BuildGenArgsBuilder(["t", "x"])

        m.add_init_condition("on", "t == 0 && x == 0")
        m.set_time_bound(1.0)
        m.add_mode("on", "true", ["1", " x + [0.1, 0.2]"])
        
        e = hypy.Engine('flowstar')
        e.set_verbose(True)
        e.set_generator('build', m.get_generator_param())

        res = e.run(run_tool=False, print_stdout=True)

        self.assertEqual(res['code'], hypy.Engine.SUCCESS)

if __name__ == '__main__':
    unittest.main()







