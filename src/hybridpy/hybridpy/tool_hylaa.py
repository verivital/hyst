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
        self._settings = None
        self._run_hylaa = None
        self._result = None
    
        python_path = sys.executable
        HybridTool.__init__(self, 'pysim', '.py', python_path)

    def _run_tool(self, image_requested):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        filepath = self.model_path
        mod_name, _ = os.path.splitext(os.path.split(filepath)[-1])
        loaded_module = imp.load_source(mod_name, filepath)

        self._run_hylaa = getattr(loaded_module, 'run_hylaa')
        define_settings = getattr(loaded_module, 'define_settings')

        self._settings = define_settings()
        
        if image_requested:
            from hylaa.plotutil import PlotSettings
            self._settings.plot.plot_mode = PlotSettings.PLOT_IMAGE
            self._settings.plot.filename = self.image_path
        
        try:
            self._result = self._run_hylaa(self._settings)
        except AssertionError as e:
            if "not yet supported" in str(e):
                rv = RunCode.SKIP
            else:
                rv = RunCode.ERROR
                
        return rv

    def _make_image(self):
        '''make an image. For Hylaa, the image will already be made during computation.'''

        return True

    def parse_output(self, dummy_directory, lines, dummy_hypy_out):
        '''returns the parsed output object

        For hylaa, this is the hylaa engine object of the most recent run.
        '''
        rv = {'safe':None}

        for line in reversed(lines):
            if 'Result: Error modes are NOT reachable.' in line:
                rv['safe'] = True
            elif 'Result: Error modes are reachable.' in line:
                rv['safe'] = False

        return rv

if __name__ == "__main__":
    tool_main(HylaaTool())










