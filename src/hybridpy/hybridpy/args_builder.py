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
    initial_rect = None
    time_bound = 10

    error_args = []
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

        rv.append("-init")

        for v in self.initial_rect:
            rv.append(str(v[0]))
            rv.append(str(v[1]))

        if len(self.error_args) > 0:
            rv.append("-error")
            rv += self.error_args

        assert len(self.mode_args) > 0

        rv.append("-modes")

        rv += self.mode_args

        if len(self.transition_args) > 0:
            rv.append("-transitions")

            rv += self.transition_args

        arg_string = " ".join(["\"" + s + "\"" for s in rv])

        return ['-generate', 'build'] + [arg_string]

    def set_initial_rect(self, rect):
        'assign the initial states'

        assert len(self.variables) == len(rect)
        self.initial_rect = rect

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

    def add_error_condition(self, mode, condition):
        'add an error condition'

        self.error_args.append(mode)
        self.error_args.append(condition)










