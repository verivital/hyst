'''Uses dreach to run reachability for HyPy'''

import os
import shutil

from hybrid_tool import get_script_path
from hybrid_tool import HybridTool
from hybrid_tool import RunCode
from hybrid_tool import run_check_stderr
from hybrid_tool import tool_main

# the path to the dreach executable
TOOL_PATH = get_script_path() + "/dreach/dReal-2.15.01-linux/bin/dReach"

class DReachTool(HybridTool):
    '''Container class for running dReach'''

    def __init__(self):
        HybridTool.__init__(self, "dreach", '.drh', TOOL_PATH)

    def _make_image(self):
        '''makes the image after the tool runs, returns True when no error occurs'''
        print "Skipping make image (generation of .png images is not supported in dReach)"
        return True

    def _run_tool(self):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        # parameter order matters! k must come first
        # use --verbose for verbose printing
        params = [self.tool_path, "-k", "0", self.model_path, "--verbose", "--visualize"]

        print "Calling dreach with params:", params

        if not run_check_stderr(params):
            rv = RunCode.ERROR

        return rv

    def _copy_model(self, temp_folder):
        '''copy the model to the temp folder and sets self.model_path
        overriden to ensure .drh extension
        '''
        model_name = os.path.basename(self.original_model_path)

        if not model_name.endswith(".drh"):
            model_name += ".drh"

        self.model_path = os.path.join(temp_folder, model_name)
        shutil.copyfile(self.original_model_path, self.model_path)

if __name__ == "__main__":
    print "Starting dReach. If you kill the tool early, be sure to kill any spawned " \
      "subprocesses ('killall dReal')."

    tool_main(DReachTool())
