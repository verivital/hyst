'''Uses pyrrt to run RRT on a hybrid automaton'''

from hybridpy.hybrid_tool import HybridTool
from hybridpy.hybrid_tool import RunCode
from hybridpy.hybrid_tool import tool_main
import imp
import os
import sys

class PyRrtTool(HybridTool):
    '''container class for running pysim'''
    
    _pyrrt_model = None
    _result = None
    
    def __init__(self):
        python_path = sys.executable
        HybridTool.__init__(self, 'pyrrt', '.py', python_path)

    def _run_tool(self):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        filepath = self.model_path
        mod_name, _ = os.path.splitext(os.path.split(filepath)[-1])
        self._pyrrt_model = imp.load_source(mod_name, filepath)

        run = getattr(self._pyrrt_model, 'run')

        self._result = run()

        return rv

    def _make_image(self):
        '''make an image'''

        if self._pyrrt_model == None:
            raise RuntimeError('pyrrt_model unassigned; pyrrt needs _run_tool() to be called before _make_image()')

        plot = getattr(self._pyrrt_model, 'plot')
        plot(self._result, self.image_path)

        return True

    def create_output(self, _):
        '''Assigns to the output object (self.output_obj)

        For pysim, this is the result of running simulate()
        '''

        self.output_obj = self._result
                
if __name__ == "__main__":
    tool_main(PyRrtTool())

