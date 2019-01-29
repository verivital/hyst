'''
Simulation logic for Hybrid Automata
'''

import matplotlib.pyplot as plt
from matplotlib import colors
import random
from scipy.integrate import ode # pylint false positive
import numpy as np

class PySimSettings(object):
    'A pysim settings containts'

    def __init__(self):
        self.max_time = 1.0
        self.step = 1.0
        self.dim_x = 0
        self.dim_y = 1
        self.filename = "plot.png"

class SimulationException(Exception):
    'An error which stops the simulation from progressing'
    pass

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
        return '[SimulationEvent: {}({}) at {!s}]'.format(self.text, self.color, self.point)

class ModeSim(object):
    '''
    A container object for a part of a simulation in a single mode
    '''
    points = None # list of points, each point is a list of values for each dimension [x_0, ..., x_n]
    times = None # list of times, corresponding to the points list. These are absolute times.
    mode_name = None

    def __init__(self, mode_name, points, times):
        self.points = points
        self.mode_name = mode_name
        self.times = times

    def __str__(self):
        return '[ModeSim: ' + self.mode_name + ' - ' + str(self.points) + ']'

    def __repr__(self):
        return str(self)

def get_active_transitions(mode, state):
    '''
    get the transitions that are active in the given state
    returns a list of active (guard is true) transitions
    '''

    active_transitions = []

    for transition in mode.transitions:
        if transition.guard(state):
            active_transitions.append(transition)

    return active_transitions

def find_event_bisection(solver, event_func, init_value, init_time, cross_delta, cross_value, tol=1e-9):
    '''
    do a binary search to find a discrete event's time / value
    this modifies the solver object, use solver.t and solver.y to access result 
    event_func is a function which takes in the state, and returns True if the event occurred
    upon returning, event_func(solver.t) should be True (assuming it was true when this was called)

    may raise a SimulationException on ODE solver erorrs
    '''

    if cross_delta < tol:
        # base case, accuracy is good enough
        solver.set_initial_value(cross_value, init_time + cross_delta)
    else:
        # check the middle
        mid_delta = cross_delta / 2.0
        mid_time = init_time + mid_delta
        
        solver.set_initial_value(init_value, init_time)
        solver.integrate(mid_time)

        if not solver.successful() or solver.t != mid_time:
            # ode error occured
            raise SimulationException("ODE solver error occurred while finding event time")

        mid_value = solver.y

        if event_func(mid_value):
            return find_event_bisection(solver, event_func, init_value, init_time, mid_delta, mid_value, tol)
        else:
            return find_event_bisection(solver, event_func, mid_value, mid_time, mid_delta, cross_value, tol)

def solver_over_max_time(solver, max_time, init_time, init_state):
    '''
    The solver went over the maximum solving time, interpolate between the last two pointer to be
    exactly at the final time.
    Modifies solver in place, use solver.y to get the result
    '''

    delta = solver.t - init_time
    desired_delta = max_time - init_time
    frac = float(desired_delta) / delta

    frac_state = []

    for i in xrange(len(init_state)):
        delta_state = solver.y[i] - init_state[i]
        frac_state.append(init_state[i] + frac * delta_state)

    solver.set_initial_value(frac_state, max_time)

def simulate_step(mode, solver, max_time, events, jump_error_tol=1e-9):
    '''
    Simulate a single step (discrete post or part of a continuous post)
    mode is the start AutomatonMode object
    solver is the ode solver object to use, at the current state/time (modified in place)
    max_time the maximum time we're simulating for
    events is the list of events, which may get appended (modified in place)

    solver is modified in place in the case of a continuous post
    In the case of a discrete post, use post_jump_q to reinitialize solver in the new mode

    returns post_jump_q, the successor state if it was a discrete step, None otherwise

    may raise a SimulationException on ODE Solver Errors, or if the invariant
    becomes false
    '''
    rv_post_jump_q = None

    # check discrete post
    active_transitions = get_active_transitions(mode, solver.y)

    if len(active_transitions) != 0:
        if len(active_transitions) > 1:
            transition_names = ", ".join([str(t) for t in active_transitions])

            # todo: we want some way to programatically disable this printing
            print 'Warning: Multiple active transitions in mode ' + str(mode.name) + \
              ' at state ' + str(solver.y) + ': ' + transition_names

            events.append(SimulationEvent("Multiple Transitions", solver.y, "red"))

        t = active_transitions[0]
        events.append(SimulationEvent(str(t), solver.y, "grey"))
        post_state = t.reset(solver.y)

        # resets to None are identity resets
        for dim in xrange(len(post_state)):
            if post_state[dim] is None:
                post_state[dim] = solver.y[dim]

        rv_post_jump_q = (t.to_mode, post_state)
    elif not mode.inv(solver.y): # don't use 'is False'
        raise SimulationException('Invariant became false')
    else:
        # continuous post
        init_time = solver.t
        init_state = solver.y
        solver.integrate(max_time, step=True)

        if not solver.successful():
            raise SimulationException("ODE solver error during continuous post")

        # the solver's step might go over the maximum time we want. if so, interpolate the state        
        if solver.t > max_time:
            solver_over_max_time(solver, max_time, init_time, init_state)

        # note: don't use 'is False', since the result is a numpy._bool
        is_invariant_false = lambda state: not mode.inv(state)
        is_transition_enabled = lambda state: len(get_active_transitions(mode, state)) > 0
    
        for event_func in is_invariant_false, is_transition_enabled:

            if event_func(solver.y):
                step_size = solver.t - init_time

                find_event_bisection(solver, event_func, init_state, init_time, 
                                     step_size, solver.y, tol=jump_error_tol)

        # check for unbounded state
        bounds = 1e15
        state = solver.y

        for val in state:
            if abs(val) > bounds:
                raise SimulationException("Continuous post reached unreasonably large state; " + 
                                          "may cause floating-point issues.")

    return rv_post_jump_q

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

def init_list_to_q_list(init_states, center=True, star=True, corners=False, tol=1e-9, check_unique=True, rand=0):
    '''
    Convert a list of initial states (tuple of AutomatonMode, HyperRectangle) to 
    a list of symbolic states (tuple of AutomatonMode, point, where point is [x_0, ..., x_n]) 
    using the provided sample strategy
    center / star / corners are different possible sample strategies
    rand=# will do that many random samples per init state (where invariant is true)
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

        if rand > 0:
            random.seed()

            max_rand_attempts = 100 * rand
            num_added = 0

            for _ in xrange(max_rand_attempts):
                point = []

                for dim in rect.dims:
                    val = dim[0] + random.random() * (dim[1] - dim[0])
                    point.append(val)

                # only accept points inside the invariant
                if not mode.inv(point): # don't use 'is False'
                    continue

                rv.append((mode, point))
                num_added += 1

                if num_added >= rand:
                    break

            if num_added != rand:
                raise RuntimeError("Could not generate {} points inside the invariant of mode {} after {} attempts".
                                   format(rand, mode.name, max_rand_attempts))
                

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

def simulate_multi(q_list, end_time, max_jumps=500, max_step=None, solver_name='vode', print_log=False):
    '''
    Simulate the hybrid automaton from multiple initial points
    q_list - a list of symbolic states: (AutomatonMode, point), where point is [x_0, ..., x_n]
    end_time - the total desired simulation time (discrete events may reduce the actual time)
    print_log - should a log of states be printed to stdout?

    Returns a list of dicts where each dict:
    'traces': list of ModeSim objects
    'events': list of SimulationEvent objects
    '''
    rv = []

    for q_index in xrange(len(q_list)):
        q = q_list[q_index]

        if print_log:
            print "Simulation {}/{} starting in mode '{}': {}".format(q_index+1, len(q_list), q[0].name, q[1])
        
        obj = simulate_one(q, end_time, max_jumps, solver_name=solver_name, max_step=max_step, print_log=print_log)
        rv.append(obj)

    return rv

def get_ordered_plot_colors():
    'return a list of valid colors, in order'

    rv = []

    # remove any colors with 'white' or 'yellow in the name
    skip_colors_substrings = ['white', 'yellow']
    skip_colors_exact = ['black', 'red', 'blue']

    for col in colors.cnames:
        skip = False

        for col_substring in skip_colors_substrings:
            if col_substring in col:
                skip = True
                break

        if not skip and not col in skip_colors_exact:
            rv.append(col)

    # we'll re-add these later; remove them before shuffling
    first_colors = ['lime', 'cyan', 'orange', 'magenta', 'green']

    for col in first_colors:
        rv.remove(col)

    # deterministic shuffle of all remaining colors
    random.seed(0)
    random.shuffle(rv)

    # prepend first_colors so they get used first
    rv = first_colors + rv

    return rv


def mode_name_to_color(mode_to_color, mode_name):
    '''get the color string from a mode name. 
    There is better (manual) color selection early on.
    mode_to_color - a map from saved mode names to colors
    mode_name - the mode name to map
    '''
    rv = None

    first_colors = ['lime', 'cyan', 'orange', 'magenta', 'green'] 

    rv = mode_to_color.get(mode_name)

    if rv is None:
        if len(mode_to_color) < len(first_colors):
            rv = first_colors[len(mode_to_color)]
        else:
            all_colors = get_ordered_plot_colors()

            index = hash(mode_name) % len(all_colors)
            rv = all_colors[index]
            #print "rv color = " + str(rv)

        mode_to_color[mode_name] = rv

    return rv

def simulate_one_time(q, time, max_jumps=500, solver_name='vode'):
    '''
    Simulate for the given time.

    Returns the final symbolic state.

    throws a SimulationException if the simulation didn't complete
    '''

    ha = q[0].parent
    res = simulate_one(q, time, max_jumps, solver_name, reraise_errors=True)

    mode_sim = res['traces'][-1]
    mode = ha.modes[mode_sim.mode_name]
    point = mode_sim.points[-1]

    return (mode, point)

def simulate_one(q, end_time, max_jumps=500, solver_name='vode', jump_error_tol=None, 
                 reraise_errors=False, max_step=None, print_log=False):
    '''
    Simulate the hybrid automaton from a single initial point 
    q - a symbolic state: (AutomatonMode, point), where point is [x_0, ..., x_n]
    end_time - the total desired simulation time (discrete events may reduce the actual time)
    max_jumps - the maximum number of discrete sub-steps, if None, a default 1e-10 is used
    solver_name - the ode solver to use (parameter of scipy's set_integrator)
    jump_error_tol - the time-error allowed on jumps
    reraise_errors - should fatal simulation errors be raised as SimulationExceptions? if False they're printed out
    max_step - the maximum step time. If None, (end_time / 100) is used.

    Returns a dict with keys {'traces', 'events'} where:
    'traces': list of ModeSim objects
    'events': list of SimulationEvent objects
    '''

    if jump_error_tol is None:
        jump_error_tol = max(1e-10, float(end_time) / 1e10)

    if max_step is None:
        max_step = end_time / 100.0

    if max_jumps <= 0:
        raise RuntimeError("max_jumps should be greater than zero: " + str(max_jumps))

    if end_time < 0:
        raise RuntimeError("max_time should be greater than zero: {!s}".format(end_time))

    mode = q[0]
    solver = ode(mode.der)
    solver.set_integrator(solver_name, max_step=max_step)
    solver.set_initial_value(q[1], 0)

    jumps_left = max_jumps
    traces = []
    events = []
    points = [] # part of the current mode_sim
    times = []
    
    points.append(solver.y)
    times.append(solver.t)
    traces.append(ModeSim(mode.name, points, times))

    if mode is None:
        raise RuntimeError("Initial mode was not set.")

    events.append(SimulationEvent('Init', solver.y))

    try:
        while solver.t < end_time:
            post_jump_q = simulate_step(mode, solver, end_time, events, jump_error_tol)

            if post_jump_q is not None: # discrete post
                mode = post_jump_q[0]
                state = post_jump_q[1]
                jump_time = times[-1]

                points = [state]
                times = [jump_time]
                traces.append(ModeSim(mode.name, points, times))

                if print_log:
                    print "{}: Jump to mode '{}' and state: {}".format(jump_time, mode.name, str(state))
                
                jumps_left -= 1

                if jumps_left < 0:
                    raise SimulationException('Max jumps ({}) reached'.format(max_jumps))

                solver = ode(mode.der)
                solver.set_integrator(solver_name, max_step=max_step)
                solver.set_initial_value(state, jump_time)

            else: # continuous post
                points.append(solver.y)
                times.append(solver.t)

        last_state = points[-1]

        if print_log:
            print "{}: Last State: {}".format(end_time, str(last_state))

        events.append(SimulationEvent("End", last_state))   
    except SimulationException as e:
        events.append(SimulationEvent(str(e), solver.y, "red"))

        if reraise_errors:
            raise e
        elif print_log:
            print "Warning: {} (SimulationException) in mode {} at state {}".format(
                str(e), mode.name, solver.y)

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
            #(0, 25, 'center', 'bottom', 'angle,angleA=-90,angleB=-89,rad=' + rad), 
            (-15, 20, 'right', 'bottom', 'angle,angleA=0,angleB=-90,rad=' + rad), 
            (15, -20, 'left', 'top', 'angle,angleA=180,angleB=90,rad=' + rad), 
            #(0, -25, 'center', 'top', 'angle,angleA=90,angleB=89,rad=' + rad), 
            (-15, -20, 'right', 'top', 'angle,angleA=0,angleB=90,rad=' + rad),]

    msg = event.text
    state = event.point
    color = event.color
    x = state[dim_x]

    if isinstance(dim_y, int):
        y = state[dim_y]
    else:
        y = dim_y(state)

    location = locs[loc_index]
    loc_x = location[0]
    loc_y = location[1]
    loc_ax = location[2]
    loc_ay = location[3]

    con = location[4]

    plt.annotate(msg, xy=(x, y), xytext=(loc_x, loc_y), color=color, 
                 textcoords='offset points', horizontalalignment=loc_ax, verticalalignment=loc_ay,
                 arrowprops=dict(color=color, arrowstyle="->", connectionstyle=con))

    # return the next label index
    rv = loc_index + 1

    if rv >= len(locs):
        rv = 0

    return rv

def plot_init_states(init_states, mode_to_color, dim_x, dim_y):
    'plot the initial rectangles'

    if isinstance(dim_y, int):
        for (mode, hr) in init_states:
            color = mode_name_to_color(mode_to_color, mode.name)

            xs = []
            ys = []

            xs.append(hr.dims[dim_x][0])
            xs.append(hr.dims[dim_x][0])
            xs.append(hr.dims[dim_x][1])
            xs.append(hr.dims[dim_x][1])
            xs.append(hr.dims[dim_x][0])

            ys.append(hr.dims[dim_y][0])
            ys.append(hr.dims[dim_y][1])
            ys.append(hr.dims[dim_y][1])
            ys.append(hr.dims[dim_y][0])
            ys.append(hr.dims[dim_y][0])

            plt.plot(xs, ys, color=color)

def plot_sim_result_multi(result_list, dim_x, dim_y, filename=None, 
                          draw_events=True, axis_range=None, draw_func=None, legend=True, 
                        show=False, title=None, init_states=None):
    '''plot mutliple simulations
    result_list - the result for simulate_multi
    '''
    num_results = len(result_list)

    mode_to_color = {}

    if title is not None:
        plt.title(title)

    if axis_range is not None:
        plt.axis(axis_range)

    for index in xrange(num_results):
        result = result_list[index]
        _plot_sim_result_one(result, dim_x, dim_y, draw_events, mode_to_color)

    if init_states is not None:
        plot_init_states(init_states, mode_to_color, dim_x, dim_y)

    if legend:
        handles, labels = plt.gca().get_legend_handles_labels()
        new_labels, new_handles = [], []
        for handle, label in zip(handles, labels):
            if label not in new_labels:
                new_labels.append(label)
                new_handles.append(handle)
        
        if len(new_handles) < 10:
            plt.legend(new_handles, new_labels, loc='best')
        else:
            print "Warning: skipping legend (too many modes)"

    if draw_func is not None:
        draw_func()

    if filename != None:
        plt.savefig(filename, dpi=300)
    
    if show:
        plt.show()

def _plot_sim_result_one(result, dim_x, dim_y, draw_events=True, mode_to_color=None):
    '''plot a simulation result to a file
    result - result object from simulate_one() call
    dim_x - the x dimension index
    dim_y - the y dimension index, or a function to call on the state which produces a y value
    draw_events - should events (with labels) be drawn on the plot? (default True)
    mode_to_color - a map of modes to colors
    '''

    traces = result['traces']
    events = result['events']

    if len(traces) == 0:
        raise RuntimeError('Result.traces contains no points')

    num_dims = len(traces[0].points[0])

    if dim_x < 0 or dim_x >= num_dims:
        raise RuntimeError('X dimension out of bounds: ' + str(dim_x) + ', should be [0, ' + str(num_dims) + ")")
    elif isinstance(dim_y, int) and (dim_y < 0 or dim_y >= num_dims):
        raise RuntimeError('Y dimension out of bounds: ' + str(dim_y) + ', shold be [0, ' + str(num_dims) + ")")

    for mode_sim in traces:
        x = [val[dim_x] for val in mode_sim.points]

        if isinstance(dim_y, int):
            y = [val[dim_y] for val in mode_sim.points]
        else:
            y = [dim_y(state) for state in mode_sim.points]

        color = mode_name_to_color(mode_to_color, mode_sim.mode_name)

        plt.plot(x, y, color=color, label=mode_sim.mode_name)

    # annotate events
    loc_index = 0

    for event in events:
        if draw_events:
            loc_index = _annotate(event, dim_x, dim_y, loc_index)
        
        if event.color == 'red':
            # draw a red 'x'
            state = event.point
            x = [state[dim_x]]
            
            if isinstance(dim_y, int):
                y = [state[dim_y]]
            else:
                y = [dim_y(state)]

            plt.plot(x, y, 'xr')

def interval_bounds_from_sim_result_multi(result_list):
    '''
    return the componentwise interval bounds [min(x_i), max(x_i)] 
    as np.array with shape (n, 2), where n is the number of states
    '''
    assert len(result_list) > 0, 'empty list of simulations; cannot compute maximum'
    num_states = len(result_list[0]['traces'][0].points[0])
    state_max = np.zeros((num_states, 1));
    state_min = np.zeros((num_states, 1));
    for result in result_list:
        for mode_sim in result['traces']:
            for dim in range(num_states):
                state_max[dim] = max(state_max[dim], max([val[dim] for val in mode_sim.points]))
                state_min[dim] = min(state_min[dim], min([val[dim] for val in mode_sim.points]))
    return np.block([state_min, state_max])








