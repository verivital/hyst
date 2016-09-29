'''Uses pysim to simulate a hybrid automaton'''

import imp
import os
import sys

from hybridpy.hybrid_tool import HybridTool
from hybridpy.hybrid_tool import RunCode
from hybridpy.hybrid_tool import tool_main

class HylaaTool(HybridTool):
    '''container class for running pysim'''
    
    def __init__(self):
        self._pysim_model = None
        #self._result = None
        self._init_states = None # saved befetween run_tool and make_image

        python_path = sys.executable
        HybridTool.__init__(self, 'pysim', '.py', python_path)

    def _run_tool(self):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        filepath = self.model_path
        mod_name, _ = os.path.splitext(os.path.split(filepath)[-1])
        self._pysim_model = imp.load_source(mod_name, filepath)

        define_ha = getattr(self._pysim_model, 'define_ha')
        define_init_states = getattr(self._pysim_model, 'define_init_states')

        #sim = getattr(self._pysim_model, 'simulate')

        self._init_states = define_init_states(define_ha())
        #self._result = sim(self._init_states)

        return rv

    def _make_image(self):
        '''make an image'''

        raise RuntimeError("Unimplemented")

        #if self._pysim_model == None:
        #    raise RuntimeError('pysim_model unassigned; pysim needs _run_tool() to be called before _make_image()')

        #plot = getattr(self._pysim_model, 'plot')
        #plot(self._result, self._init_states, self.image_path)

        return True

    def parse_output(self, dummy_directory, dummy_lines, dummy_hypy_out):
        '''returns the parsed output object

        For hylaa, this is the hylaa engine object 
        '''

        return self._result
                
if __name__ == "__main__":
    tool_main(HylaaTool())










