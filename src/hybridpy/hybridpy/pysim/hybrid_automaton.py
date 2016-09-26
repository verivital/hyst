'''
Hybrid Automaton Simulation Library
Stanley Bak (12-2015)
'''

class HyperRectangle(object):
    'An n-dimensional box'

    dims = None # list of tuples

    def __init__(self, dims):
        self.dims = dims

    def center(self):
        'Returns a point in the center of the box'
        rv = []

        for d in self.dims:
            rv.append((d[0] + d[1]) / 2.0)

        return rv

    def star(self):
        'Returns a list of the so-called star points of this box (2*dims of them)'
        center = self.center()
        num_dims = len(self.dims)
        rv = []
        
        for index in xrange(num_dims):
            # min edge in dimension d
            pt = list(center)
            pt[index] = self.dims[index][0]
            rv.append(pt)

            # max edge in dimension d
            pt = list(center)
            pt[index] = self.dims[index][1]
            rv.append(pt)

        return rv

    def unique_corners(self, tol=1e-9):
        'Returns a list of the unique corner points of this box (up to 2^dims of them)'
        rv = []
        num_dims = len(self.dims)

        # compute max iterator index and make is_flat list
        max_iterator = 1
        is_flat = []

        for d in xrange(num_dims):
            if abs(self.dims[d][0] - self.dims[d][1]) > tol:
                is_flat.append(False)
                max_iterator *= 2
            else:
                is_flat.append(True)

        for it in xrange(max_iterator):
            point = []
            
            # construct point
            for d in xrange(num_dims):
                if is_flat[d]:
                    point.append(self.dims[d][0])
                else:
                    min_max_index = it % 2
                    point.append(self.dims[d][min_max_index])
                    it /= 2

            # append constructed point
            rv.append(point)

        return rv

class AutomatonMode(object):
    'A single mode of a hybrid automaton'
    parent = None # the HybridAutomaton
    name = None
    der = None # function returning derivative at a point
    inv = None # function taking a state and returning true/false
    transitions = None # outgoing transitions
    
    der_interval_list = [] # list of pairs, the derivative's interval uncertainty for each dim

    def __init__(self, parent, name):
        self.parent = parent
        self.name = name
        self.transitions = []

    def __str__(self):
        return '[AutomatonMode: ' + self.name + ']'

class AutomatonTransition(object):
    'A transition of a hybrid automaton'
    parent = None # the HybridAutomaton
    from_mode = None
    to_mode = None
    name = None

    guard = None # function taking a state and returning true/false
    reset = None # function taking a state and returning a new state

    def __init__(self, parent, from_mode, to_mode, name=None):
        self.parent = parent
        self.from_mode = from_mode
        self.to_mode = to_mode
        self.name = name
        from_mode.transitions.append(self)

    def __str__(self):
        rv = self.name
        
        if rv is None:
            rv = self.from_mode.name + " -> " + self.to_mode.name

        return rv

class HybridAutomaton(object):
    'The hybrid automaton'

    modes = {}
    transitions = []
    variables = [] # list of strings

    def __init__(self):
        pass

    def new_mode(self, name):
        '''add a mode'''
        m = AutomatonMode(self, name)
        self.modes[m.name] = m
        return m

    def new_transition(self, from_mode, to_mode, name=None):
        '''add a transition'''
        t = AutomatonTransition(self, from_mode, to_mode, name)
        self.transitions.append(t)

        return t





