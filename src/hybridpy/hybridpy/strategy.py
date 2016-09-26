'''
Stanley Bak
Sept 2016
Hypy Strategy File

A strategy is an an automated execution of multipl runs of various tools, in order to
try to solve a reachability problem. For example, automated parameter tuning for a
particular tool could be encoded as a strategy.
'''

import abc
import time
import os
import imp
import tempfile

from hybridpy.hypy import Engine
from hybridpy.hybrid_tool import random_string

class HypyStrategy(object):
    'an abstract strategy class'
    __metaclass__ = abc.ABCMeta

    def __init__(self):
        self.num_runs = 0

        self.input_ = (None, None) # 2-tuple: (xml_file, cfg_file [optional])
        self.gen = (None, None)
        self.passes = [] # list of 2-tuples
        self.output = None # path where to save a copy of the model hyst creates (optional)
        self.additional_hyst_params = [] # manually-specified parameters 
        self.debug = False
        self.verbose = False

        self.make_engine_count = 0 # number of times make_engine was called
        self.image_base = None
        self.print_stdout = None

    def set_debug(self, is_debug):
        'set debug printing mode'
        self.debug = is_debug

    def set_verbose(self, is_verbose):
        'set verbose printing mode'
        self.verbose = is_verbose

    def set_input(self, xml_path, cfg_path=None):
        '''Set the input model file'''
        self.input_ = (xml_path, cfg_path)

    def set_generator(self, gen_name, gen_param=""):
        '''set the name of the model generator and param'''
        assert str(gen_name) == gen_name, "Generator name must be a string"
        assert str(gen_param) == gen_param, "Generator param must be a string, got {}".format(type(gen_param))
    
        self.gen = (gen_name, gen_param)

    def set_output(self, path):
        '''Set the path for saving the model file (optional)'''
        self.output = path

    def run(self, image_path=None, print_stdout=False):
        '''
        run the strategy on the input model. Returns the Hypy result object on
        the last run of the tool.
        '''

        self.image_base = image_path
        self.print_stdout = print_stdout

        start = time.time()

        res = self._run()

        dif = time.time() - start

        print "Hypy Strategy Time: {:.2f} sec".format(dif)

        return res

########### Strategy Implementation Helper Methods Below #############

    def make_engine(self, printer_name, printer_params=None, additional_hyst_params=None):
        '''
        Make a hypy engine object with the defined parameters
        '''
        self.make_engine_count += 1

        e = Engine(printer_name, printer_params)

        e.set_verbose(self.verbose)
        e.set_debug(self.debug)

        if additional_hyst_params is not None:
            e.set_additional_hyst_params(additional_hyst_params)

        if self.input_[0] is not None:
            e.set_input(self.input_[0], self.input_[1])
        elif self.gen[0] is not None:
            e.set_generator(self.gen[0], self.gen[1])
        else:
            raise RuntimeError('input or generator must be set prior to calling make_engine()')

        if self.output is not None:
            # construct a filename combining self.output and make_engine_count
            parts = os.path.splitext(self.output)

            filename = parts[0] + str(self.make_engine_count) + parts[1]
            e.set_output(filename)

        return e

    def run_engine(self, engine, run_hyst=True, run_tool=True, timeout=None, save_stdout=False, 
                   print_stdout=False, stdout_func=None, parse_output=False):
        '''
        The suggested way for a strategy implementation to run a hypy.Engine. This auto-populates
        the image and print_stdout.
        '''

        assert isinstance(engine, Engine)
        
        return engine.run(run_hyst=run_hyst, run_tool=run_tool, timeout=timeout, image_path=self._get_image_path(), 
                          save_stdout=save_stdout, print_stdout=print_stdout or self.print_stdout, 
                          stdout_func=stdout_func, parse_output=parse_output)

    def _get_image_path(self):
        '''
        Get the image path for the current run. If the user passed None into the image_path parameter 
        of run(), this returns None.

        This increments based on the number of times make_engine() is called.
        '''   

        rv = None

        if self.image_base is not None:
            parts = os.path.splitext(self.image_base)
            rv = parts[0] + str(self.make_engine_count) + parts[1]
    
        return rv

    def model_to_objects(self, verbose=False, debug=False):
        '''
        Take the current model, print it to pysim with hyst, and return the result python objects:

        returns a 3-tuple (HybridAutomaton, List of HyperRectangle, PysimSettings). 

        This is useful to extract model parameters, such as the reachability time or time step, 
        or to process the model in python.
        '''

        e = Engine('pysim')

        filename = os.path.join(tempfile.gettempdir(), "pysim_" + random_string() + ".py")
        e.set_output(filename)

        e.set_verbose(verbose)
        e.set_debug(debug)

        if self.input_[0] is not None:
            e.set_input(self.input_[0], self.input_[1])
        elif self.gen[0] is not None:
            e.set_generator(self.gen[0], self.gen[1])
        else:
            raise RuntimeError('input or generator must be set prior to calling model_as_object()')

        result = e.run(run_tool=False)

        if result['code'] != Engine.SUCCESS:
            raise RuntimeError('Printing model to pysim failed: {}'.format(result))

        mod_name, _ = os.path.splitext(os.path.split(filename)[-1])
        pysim_model = imp.load_source(mod_name, filename)

        define_settings = getattr(pysim_model, 'define_settings')
        define_ha = getattr(pysim_model, 'define_ha')
        define_init_states = getattr(pysim_model, 'define_init_states')

        ha = define_ha()

        return (ha, define_init_states(ha), define_settings())

    @abc.abstractmethod
    def _run(self):
        '''
        implementation of the strategy; returns a hypy result object from the last
        run of the strategy.
        '''
        return None


class FlowstarAutotune(HypyStrategy):
    '''
    Automatic tuning strategy for Flow*. Tries progressively more precise parameters
    until completion.
    '''

    def _run(self):

        _, init_list, settings = self.model_to_objects()

        assert len(init_list) == 1

        for num_steps in [10, 100, 1000]:
            params = ''
            step = settings.max_time / num_steps
            print "Trying Step {}".format(step)

            params += '-step ' + str(step)

            e = self.make_engine('flowstar', params)

            res = self.run_engine(e, print_stdout=True, parse_output=True)

            if res['code'] == Engine.SUCCESS:
                break

        return res





