'''
Hybrid Automaton Simulation Library
Stanley Bak (12-2015)
'''

class _Mode(object):
    'A single mode of a hybrid automaton'
    name = None
    der = None # function returning derivative at a point
    inv = None # function taking a state and returning true/false

    def __init__(self, name):
        self.name = name

class _Transition(object):
    'A transition of a hybrid automaton'
    from_mode = None
    to_mode = None
    name = None

    guard = None # function taking a state and returning true/false
    reset = None # function taking a state and returning a new state

    def __init__(self, from_mode, to_mode, name=None):
        self.from_mode = from_mode
        self.to_mode = to_mode
        self.name = name

    def __str__(self):
        rv = self.name
        
        if rv == None:
            rv = self.from_mode.name + " -> " + self.to_mode.name

        return rv

class HybridAutomaton(object):
    'The hybrid automaton'

    modes = {}
    transitions = []

    def __init__(self):
        pass

    def new_mode(self, name):
        '''add a mode'''
        m = _Mode(name)
        self.modes[m.name] = m
        return m

    def new_transition(self, from_mode, to_mode, name=None):
        '''add a transition'''
        t = _Transition(from_mode, to_mode, name)
        self.transitions.append(t)
        return t

    
