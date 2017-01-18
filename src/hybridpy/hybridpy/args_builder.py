'''
Stanley Bak
August 2016
Hyst Args Builder

These are python interface classes for constructing sub-arguments to Hyst printers/passes/generators for use in hypy, 
as an alternative to doing it as a raw list.
'''

class BuildGenArgsBuilder(object):
    '''
    helper object to make arguments associated with generating a hybrid automaton
    using the 'build' model generator

    make an object of this type, set values, and then on the hypy engine object do:

    hypy_engine.set_generator('build', model_args_builder.get_generator_param())
    '''

    def __init__(self, var_list):
        self.variables = var_list
        self.time_bound = 10

        self.error_args = []
        self.init_args = []

        self.modes = {} # used to ensure unique modes
        self.mode_args = []
        self.transition_args = []

    def get_generator_param(self):
        'get the hyst param for the "build" generator'

        rv = []

        rv.append("-vars")

        for v in self.variables:
            rv.append(v)

        rv.append("-time_bound")
        rv.append(str(self.time_bound))

        assert len(self.init_args) > 0, "initial states were not set with add_init_condition()"
        rv.append("-init")
        rv += self.init_args

        if len(self.error_args) > 0:
            rv.append("-error")
            rv += self.error_args

        assert len(self.mode_args) > 0, "modes were not defined with add_mode()"

        rv.append("-modes")

        rv += self.mode_args

        if len(self.transition_args) > 0:
            rv.append("-transitions")

            rv += self.transition_args

        quoted_args = ["null" if s is None else "\"" + s + "\"" if " " in s else s for s in rv]
        arg_string = " ".join(quoted_args)

        return arg_string

    def add_init_condition(self, mode_name, condition_str):
        'add an initial condition'

        assert str(mode_name) == mode_name, "init_mode must be a string: {}".format(mode_name)
        assert str(condition_str) == condition_str, "init_condition must be a string: {}".format(condition_str)

        self.init_args.append(mode_name)
        self.init_args.append(condition_str)
    
    def add_error_condition(self, mode_name, condition_str):
        'add an error condition'

        assert str(mode_name) == mode_name, "error_mode must be a string: {}".format(mode_name)
        assert str(condition_str) == condition_str, "error_condition must be a string: {}".format(condition_str)

        self.error_args.append(mode_name)
        self.error_args.append(condition_str)

    def set_time_bound(self, tb):
        'assigns the time bound'

        self.time_bound = tb
    
    def add_mode(self, name, invariant, der_list):
        'add a mode'

        assert str(name) == name, "mode name must be a string: {}".format(name)
        assert str(invariant) == invariant, "invariant condition must be a string: {}".format(invariant)
        assert len(der_list) == len(self.variables), "add_mode for '{}' expected {} derivatives but {} were given" \
                                                    .format(name, len(self.variables), len(der_list))

        for der in der_list:
            assert der is None or str(der) == der, "derivatives must be strings: {}".format(repr(der))

        assert self.modes.get(name) is None, "mode '{}' was already in the automaton".format(name)

        self.modes[name] = True
        self.mode_args.append(name)
        self.mode_args.append(invariant)
        self.mode_args += der_list

    def add_transition(self, from_name, to_name, guard, reset_list=None):
        'add a transition'

        assert str(from_name) == from_name, "source mode name must be a string: {}".format(from_name)
        assert str(to_name) == to_name, "destination mode name must be a string: {}".format(to_name)
        assert str(guard) == guard, "guard condition must be a string: {}".format(guard)

        self.transition_args.append(from_name)    
        self.transition_args.append(to_name)
        self.transition_args.append(guard)

        if reset_list != None:
            self.transition_args += reset_list
        else:
            self.transition_args += ["null"] * len(self.variables)







