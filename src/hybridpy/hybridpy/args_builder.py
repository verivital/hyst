'''
Stanley Bak
August 2018
Hyst Args Builder

This is a python interface for constructing command line arguments for Hyst, as an
alternative to doing it as a raw list.
'''

class ModelArgsBuilder(object):
    'make arguments associated with generating a hybrid automaton'

    variables = None
    time_bound = 10

    error_args = []
    init_args = []

    mode_args = []
    transition_args = []

    def __init__(self, var_list):
        self.variables = var_list

    def get_hyst_params(self):
        'get a list of params for passing into hyst'

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

        arg_string = " ".join(["\"" + s + "\"" for s in rv])

        return ['-generate', 'build'] + [arg_string]

    def add_init_condition(self, mode_name, init_exp):
        'add an initial condition'

        self.init_args.append(mode_name)
        self.init_args.append(init_exp)
    
    def add_error_condition(self, mode_name, condition_string):
        'add an error condition'

        self.error_args.append(mode_name)
        self.error_args.append(condition_string)

    def set_time_bound(self, tb):
        'assigns the time bound'

        self.time_bound = tb

    def add_mode(self, name, invariant, der_list):
        'add a mode'

        self.mode_args.append(name)
        self.mode_args.append(invariant)
        self.mode_args += der_list

    def add_transition(self, from_name, to_name, guard, reset_list):
        'add a transition'

        self.transition_args.append(from_name)    
        self.transition_args.append(to_name)
        self.transition_args.append(guard)

        if reset_list != None:
            self.transition_args += reset_list
        else:
            self.transition_args += ["null"] * len(self.variables)







