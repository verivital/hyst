'''
Simulation logic for Hybrid Automata
'''

from scipy import integrate
import matplotlib.pyplot as plt
from matplotlib import colors
import matplotlib.patches as mpatches

class SimulationEvent(object):
    '''
    A container object for annotated simulation events.
    '''
    text = None
    point = None
    color = None

    def __init__(self, text, point, color='black'):
        self.text = text
        self.point = point
        self.color = color

    def __str__(self):
        return '[SimulationEvent: ' + self.text + ']'

class ModeSim(object):
    '''
    A container object for a part of a simulation in a single mode
    '''
    points = None # list of points, each point is a list of values for each dimension [x_0, ..., x_n]
    mode_name = None

    def __init__(self, mode_name, points):
        self.points = points
        self.mode_name = mode_name

    def __str__(self):
        return '[ModeSim: ' + self.mode_name + ' - ' + str(self.points) + ']'

def simulate_step(q, delta):
    '''
    Simulate a single step (discrete post or part of a continuous post)
    q is a symbolic state (mode, point), where mode is an AutomatonMode object, and point is [x_0, ..., x_n]
    delta is the amount of continuous post step to do (the reach time increment)

    this returns a tuple (q', events), where q' is the successor symbolic state,
    and events is a list of SimulationEvent (may be of size zero) 
    '''
    rv_events = []
    rv_state = None

    mode = q[0]
    state = q[1]

    # check discrete post
    active_transitions = []

    for transition in mode.transitions:
        if transition.guard(state):
            active_transitions.append(transition)

    if len(active_transitions) != 0:
        if len(active_transitions) > 1:
            transition_names = ", ".join([str(t) for t in active_transitions])

            print 'Warning: Multiple active transitions in mode ' + str(mode.name) + \
              ' at state ' + str(state) + ': ' + transition_names
            rv_events.append(("Multiple Transitions", state, "red"))

        t = active_transitions[0]
        rv_events.append(SimulationEvent(str(t), state, "grey"))
        post_state = t.reset(state)

        # resets to None are identity resets
        for dim in xrange(len(state)):
            if post_state[dim] == None:
                post_state[dim] = state[dim]

        rv_state = (t.to_mode, post_state)

    if rv_state == None: # if there was no discrete post, try continuous post
        if mode.inv(state) == False:
            rv_events.append(SimulationEvent("False Invariant", state, "red"))
            print 'Warning: Invariant became false in mode ' + str(mode.name) + ' at state ' + str(state)
        else:
            # do a single step
            sol = integrate.odeint(mode.der, state, [0, delta]) # pylint false positive
            state = sol[1].tolist()
            rv_state = (mode, state)

    return (rv_state, rv_events)

def _dist(a, b):
    'maximum distance in any dimension (inf norm) between two hyperpoints'
    rv = 0

    if len(a) != len(b):
        raise RuntimeError("hyperpoints have different length in _dist()")

    for i in xrange(len(a)):
        dist = abs(a[i] - b[i])

        if dist > rv:
            rv = dist

    return rv

def init_list_to_q_list(init_states, center=True, star=True, corners=False, tol=1e-9, check_unique=True):
    '''
    Convert a list of initial states (tuple of AutomatonMode, HyperRectangle) to 
    a list of symbolic states (tuple of AutomatonMode, point, where point is [x_0, ..., x_n]) 
    using the provided sample strategy
    center / star / corners are different possible sample strategies
    '''

    rv = []

    for init in init_states:
        mode = init[0]
        rect = init[1]

        if center:
            rv.append((mode, rect.center()))

        if star:
            for point in rect.star():
                rv.append((mode, point))

        if corners:
            for point in rect.unique_corners(tol):
                rv.append((mode, point))

    if check_unique:
        # ensure elements are unique
        for index_cur in xrange(len(rv)-1, -1, -1):
            cur_mode = rv[index_cur][0]
            cur = rv[index_cur][1]
  
            for index_other in xrange(0, index_cur):
                other_mode = rv[index_other][0]
                other = rv[index_other][1]

                if cur_mode == other_mode and _dist(cur, other) < tol:
                    rv.pop(index_cur)
                    break

    return rv

def simulate_multi(q_list, time, num_steps=500):
    '''    
    Simulate the hybrid automaton from multiple initial points
    q_list - a list of symbolic states: (AutomatonMode, point), where point is [x_0, ..., x_n]
    time - the total desired simulation time (discrete events may reduce the actual time)
    num_steps - the amount discrete and continuous post sub-steps
    simtype - a string describing how to sample the initial sets, a concatination of 'center', 'star', and 'corner'

    Returns a list of dicts with keys {'traces', 'events'} where:
    'traces': list of ModeSim objects
    'events': list of SimulationEvent objects
    '''
    rv = []

    for q in q_list:
        rv.append(simulate_one(q, time, num_steps))

    return rv

def mode_name_to_color(mode_to_color, mode_name):
    '''get the color string from a mode name. 
    There is better (manual) color selection early on.
    mode_to_color - a map from saved mode names to colors
    mode_name - the mode name to map
    '''
    rv = None

    first_colors = ['lime', 'blue', 'red', 'cyan', 'magenta', 'black'] 

    rv = mode_to_color.get(mode_name)

    if rv == None:
        if len(mode_to_color) < len(first_colors):
            rv = first_colors[len(mode_to_color)]
        else:
            # use the hash-based approach
            all_colors = colors.cnames.keys()
            index = hash(mode_name) % len(all_colors)
            rv = all_colors[index]

        mode_to_color[mode_name] = rv

    return rv

def simulate_one(q, time, num_steps=500):
    '''    
    Simulate the hybrid automaton from a single initial point 
    q - a symbolic state: (AutomatonMode, point), where point is [x_0, ..., x_n]
    time - the total desired simulation time (discrete events may reduce the actual time)
    num_steps - the amount discrete and continuous post sub-steps
    simtype - a string describing how to sample the initial sets, a concatination of 'center', 'star', and 'corner'

    Returns a dict with keys {'traces', 'events'} where:
    'traces': list of ModeSim objects
    'events': list of SimulationEvent objects
    '''

    if num_steps <= 0:
        raise RuntimeError("num_steps should be greater than zero: " + str(num_steps))

    if time <= 0:
        raise RuntimeError("Time should be greater than zero: " + str(time))

    step_size = time / float(num_steps)
    steps_left = num_steps
    traces = []
    events = []
    points = [] # part of the current mode_sim
    
    mode = q[0]
    state = q[1]
    points.append(state)
    traces.append(ModeSim(mode.name, points))

    if mode == None:
        raise RuntimeError("Initial mode was not set.")

    events.append(SimulationEvent('Init', state))

    while steps_left > 0:
        steps_left -= 1

        (q, new_events) = simulate_step(q, step_size)
        events += new_events
        mode = q[0]
        state = q[1]

        if len(new_events) > 0: # discrete post
            points = []
            points.append(state)
            traces.append(ModeSim(mode.name, points))
        else: # continuous post
            points.append(state)

    last_state = points[len(points) - 1]
    events.append(SimulationEvent("End", last_state))

    return {'traces':traces, 'events':events}

def _annotate(event, dim_x, dim_y, loc_index=0):
    '''annotate the plot.
    event - the SimulationEvent to add
    loc_index - the annotation index

    this returns a new annotation index
    '''
    # possible label locations
    rad = '80'
    locs = [(15, 20, 'left', 'bottom', 'angle,angleA=180,angleB=-90,rad=' + rad), 
            (0, 25, 'center', 'bottom', 'angle,angleA=-90,angleB=-89,rad=' + rad), 
            (-15, 20, 'right', 'bottom', 'angle,angleA=0,angleB=-90,rad=' + rad), 
            (15, -20, 'left', 'top', 'angle,angleA=180,angleB=90,rad=' + rad), 
            (0, -25, 'center', 'top', 'angle,angleA=90,angleB=89,rad=' + rad), 
            (-15, -20, 'right', 'top', 'angle,angleA=0,angleB=90,rad=' + rad),]

    msg = event.text
    state = event.point
    color = event.color
    x = state[dim_x]
    y = state[dim_y]

    location = locs[loc_index]
    loc_x = location[0]
    loc_y = location[1]
    loc_ax = location[2]
    loc_ay = location[3]
    con = location[4]

    #plt.plot([x], [y], 'o')
    plt.annotate(msg, xy=(x, y), xytext=(loc_x, loc_y), color=color, 
                 textcoords='offset points', horizontalalignment=loc_ax, verticalalignment=loc_ay,
                 arrowprops=dict(color=color, arrowstyle="->", connectionstyle=con))

    # return the next label index
    rv = loc_index + 1

    if rv >= len(locs):
        rv = 0

    return rv

def plot_sim_result_multi(result_list, dim_x, dim_y, filename=None, 
                          draw_events=True, axis_range=None, draw_func=None, legend=True):
    '''plot mutliple simulations
    result_list - the result for simulate_multi
    '''
    num_results = len(result_list)

    mode_to_color = {}

    if axis_range is not None:
        plt.axis(axis_range)

    for index in xrange(num_results):
        result = result_list[index]
        _plot_sim_result_one(result, dim_x, dim_y, draw_events, mode_to_color)

    if legend:
        handles, labels = plt.gca().get_legend_handles_labels()
        new_labels, new_handles = [], []
        for handle, label in zip(handles, labels):
            if label not in new_labels:
                new_labels.append(label)
                new_handles.append(handle)
        
        if len(new_handles) < 10:
            plt.legend(new_handles, new_labels)
        else:
            print "warning: skipping legend (too many modes)"

    if draw_func is not None:
        draw_func()

    if filename != None:
        plt.savefig(filename, dpi=300)
    else:
        plt.show()

def _plot_sim_result_one(result, dim_x, dim_y, draw_events=True, mode_to_color=None):
    '''plot a simulation result to a file
    result - result object from simulate_one() call
    dim_x - the x dimension index
    dim_y - the y dimension index
    draw_events - should events (with labels) be drawn on the plot? (default True)
    mode_to_color - a map of modes to colors
    '''

    traces = result['traces']
    events = result['events']

    if len(traces) == 0:
        raise RuntimeError('Result.traces contains no points')

    num_dims = len(traces[0].points[0])

    if dim_x < 0 or dim_x >= num_dims:
        raise RuntimeError('X dimension out of bounds: ' + str(dim_x) + ', should be [0, ' + num_dims + ")")
    elif dim_y < 0 or dim_y >= num_dims:
        raise RuntimeError('Y dimension out of bounds: ' + str(dim_y) + ', shold be [0, ' + num_dims + ")")

    for mode_sim in traces:
        x = [val[dim_x] for val in mode_sim.points]
        y = [val[dim_y] for val in mode_sim.points]
        color = mode_name_to_color(mode_to_color, mode_sim.mode_name)

        plt.plot(x, y, color=color, label=mode_sim.mode_name)

    if draw_events:
        loc_index = 0

        for event in events:
            loc_index = _annotate(event, dim_x, dim_y, loc_index)
        
    









