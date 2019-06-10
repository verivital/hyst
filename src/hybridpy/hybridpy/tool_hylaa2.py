'''Uses pysim to simulate a hybrid automaton'''

import imp
import os
import sys
import subprocess

from hybridpy.hybrid_tool import HybridTool
from hybridpy.hybrid_tool import RunCode
from hybridpy.hybrid_tool import tool_main

class Hylaa2Tool(HybridTool):
    '''container class for running pysim'''
    
    def __init__(self):
        self._settings = None
        self._run_hylaa = None
        self._result = None
    
        python_path = sys.executable + "3" # path to python3... not 100% correct but should work for now
        
        HybridTool.__init__(self, 'hylaa2', '.py', python_path)

    def _run_tool(self, image_requested):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        try:
            params = [self.tool_path, self.model_path]
            
            if image_requested:
                params.append(self.image_path)

            proc = subprocess.Popen(params)
            proc.wait()

            if proc.returncode != 0:
                print "Error Running Hylaa (return code was " + str(proc.returncode) + ")"
                    
                rv = RunCode.ERROR
        except OSError as e:
            print "Exception while trying to run Hylaa: " + str(e)
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
    tool_main(Hylaa2Tool())










