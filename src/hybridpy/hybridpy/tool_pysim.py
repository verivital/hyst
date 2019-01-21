'''Uses pysim to simulate a hybrid automaton'''

import imp
import os
import sys
import pickle
import tempfile

from hybridpy.hybrid_tool import HybridTool
from hybridpy.hybrid_tool import RunCode
from hybridpy.hybrid_tool import tool_main

import hybridpy.pysim.simulate

class PySimTool(HybridTool):
    '''container class for running pysim'''
    
    def __init__(self):
        self._pysim_model = None
        self._result = None
        self._init_states = None # saved befetween run_tool and make_image
        self._settings = None

        python_path = sys.executable
        HybridTool.__init__(self, 'pysim', '.py', python_path)

    def _run_tool(self, image_requested):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        filepath = self.model_path
        mod_name, _ = os.path.splitext(os.path.split(filepath)[-1])
        self._pysim_model = imp.load_source(mod_name, filepath)

        define_ha = getattr(self._pysim_model, 'define_ha')
        define_settings = getattr(self._pysim_model, 'define_settings')
        define_init_states = getattr(self._pysim_model, 'define_init_states')

        # plot(simulate(init_states, settings), init_states, settings)

        sim = getattr(self._pysim_model, 'simulate')

        self._settings = define_settings()

        automaton = define_ha()
        self._init_states = define_init_states(automaton)
        self._result = sim(self._init_states, self._settings)

        output = {'variables': automaton.variables,
                  'simulations': self._result,
                  'interval_bounds': hybridpy.pysim.simulate.interval_bounds_from_sim_result_multi(self._result)}
        # We need to pass the result via stdout (see note in parse_output()).
        # To do that, we serialize and write it to a file and print the filename.
        with tempfile.NamedTemporaryFile(prefix='pysim_result',suffix='.pickle', delete=False) as filehandle:
            pickle.dump(output, filehandle)
            print "PYSIM_RESULT_FILE=" + filehandle.name
        return rv

    def _make_image(self):
        '''make an image'''

        if self._pysim_model is None:
            raise RuntimeError('pysim_model unassigned; pysim needs _run_tool() to be called before _make_image()')

        plot = getattr(self._pysim_model, 'plot')
        plot(self._result, self._init_states, self.image_path, self._settings)

        return True

    def parse_output(self, dummy_directory, lines, dummy_hypy_out):
        '''returns the parsed output object

        For pysim, this returns a dictionary created by run_tool():
        { 'simulations': <the result of running simulate()>,
          'variables': <list of state variable names>,
          'interval_bounds': interval bounds [x_min, x_max] of the state variables, as np.array with shape (n, 2)
        }
        Further entries may be added in the future.
        '''
        # NOTE: This function is not called on the same tool object as _run_tool and _make_image because these are run in a subprocess,
        # whereas parse_output is run in the main python process that called hypy.Engine.run().
        # Additionally, the temporary directory gets deleted before parse_output is called.
        # Therefore, the data must be passed via stdout.
        
        prefix = "PYSIM_RESULT_FILE="
        for line in lines:
            if line.startswith(prefix):
                # import data printed from _run_tool()
                path = line[len(prefix):]
                with open(path, 'rb') as f:
                    self._result = pickle.load(f)
                os.remove(path)
            
        assert self._result is not None, "could not find {} in output".format(prefix)
        return self._result
                
if __name__ == "__main__":
    tool_main(PySimTool())










