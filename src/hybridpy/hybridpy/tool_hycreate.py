'''Uses Hycreate2 to run reachability and plot a file'''
# Stanley Bak, September 2014

import subprocess
import shutil
from hybridpy.hybrid_tool import HybridTool
from hybridpy.hybrid_tool import RunCode
from hybridpy.hybrid_tool import tool_main

# flag to pass into hycreate, use -b for batch mode and -bd for debug batch mode
HYCREATE_BATCH_MODE_FLAG = "-b"

class HyCreateTool(HybridTool):
    '''Container class for running Flow*'''

    def __init__(self):
        HybridTool.__init__(self, 'hycreate', '.hyc2', 'HyCreate2.8.jar')

    def _run_tool(self, image_requested):
        '''runs the tool, returns a value in RunCode'''
        rv = RunCode.SUCCESS

        try:
            params = ["java", "-classpath", self.tool_path, "main.Main", \
							HYCREATE_BATCH_MODE_FLAG, self.model_path]

            proc = subprocess.Popen(params)
            proc.wait()

            if proc.returncode != 0:
                print "Error Running HyCreate (java return code was " + str(proc.returncode) + ")"
                
                if proc.returncode == 1:
                    print "Did you assign HYCREATE_BIN?"
                    
                rv = RunCode.ERROR
        except OSError as e:
            print "Exception while trying to run HyCreate2: " + str(e)
            rv = RunCode.ERROR

        return rv

    def _make_image(self):
        '''makes the image after the tool runs, returns True on success'''
        rv = True

        from_path = "result/reachability.png"
        to_path = self.image_path

        try:
            shutil.copyfile(from_path, to_path)
        except IOError:
            rv = False

        return rv

if __name__ == "__main__":
    tool_main(HyCreateTool())
