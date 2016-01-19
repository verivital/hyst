'''
Simulation logic for Hybrid Automata
'''

import scipy.integrate as integrate
import matplotlib.pyplot as plt

def simulate(ha, init_state, init_mode, time, num_steps=500):
    '''    
    Simulate the hybrid automaton (ha). Returns a dict with keys {'traces', 'events'} where:
    'traces': [state0, state1, ...]   (where each state is [x_0, x_1, ..., x_n]
    'events': [(name, state), ...
    '''

    if num_steps <= 0:
        raise RuntimeError("Num steps should be greater than zero: " + str(num_steps))

    if time <= 0:
        raise RuntimeError("Time should be greater than zero: " + str(time))

    step_size = time / float(num_steps)
    steps_left = num_steps
    state = init_state
    traces = []
    events = []
    mode_sim = []
    mode = ha.modes.get(init_mode)

    if mode == None:
        raise RuntimeError("Initial mode named '" + str(init_mode) + "' not found in automaton.")

    events.append(('Init', state))

    while steps_left > 0:
        steps_left -= 1
        mode_sim.append(state)

        # check discrete post
        active_transitions = []

        for transition in ha.transitions:
            if transition.from_mode is mode and transition.guard(state):
                active_transitions.append(transition)

        if len(active_transitions) != 0:
            if len(active_transitions) > 1:
                transition_names = ", ".join([str(t) for t in active_transitions])

                print 'Warning: Multiple active transitions in mode ' + str(mode.name) + \
                ' at state ' + str(state) + ': ' + transition_names
                events.append(("Multiple Transitions", state))

            traces.append(mode_sim)
            mode_sim = []

            t = active_transitions[0]
            events.append((str(t), state))
            mode = t.to_mode
            post_state = t.reset(state)

            # resets to None are identity resets
            for dim in xrange(len(state)):
                if post_state[dim] == None:
                    post_state[dim] = state[dim]

            state = post_state

            continue

        # check continuous post
        if mode.inv(state) == False:
            events.append(("False Invariant", state))
            print 'Warning: Invariant became false in mode ' + str(mode.name) + ' at state ' + str(state)
            break

        # do a single step
        sol = integrate.odeint(mode.der, state, [0, step_size])
        state = sol[1].tolist()

    if len(mode_sim) > 0:
        traces.append(mode_sim)

        last_state = mode_sim[len(mode_sim) - 1]
        events.append(("End", last_state))

    return {'traces':traces, 'events':events}

def plot_sim_result(result, filename, dim_x, dim_y, draw_events=True, axis_range=None, draw_func=None):
    ''' plot a simulation result to a file
    result - result object from simulate() call
    filename - where to save the image
    dim_x - the x dimension index
    dim_y - the y dimension index
    draw_events - should events (with labels) be drawn on the plot? (default True)
    axis_range - a 4-tuple of xmin,xmax,ymin,ymax for plotting (default None)
    draw_func - extra draw function called after plotting (default None)
    '''

    traces = result['traces']
    events = result['events']

    if axis_range is not None:
        plt.axis(axis_range)

    if len(traces) == 0:
        raise RuntimeError('Result.traces contains no points')

    num_dims = len(traces[0][0])

    if dim_x < 0 or dim_x >= num_dims:
        raise RuntimeError('X dimension out of bounds: ' + str(dim_x) + ', should be [0, ' + num_dims + ")")
    elif dim_y < 0 or dim_y >= num_dims:
        raise RuntimeError('Y dimension out of bounds: ' + str(dim_y) + ', shold be [0, ' + num_dims + ")")

    for mode_sim in traces:
        x = [val[dim_x] for val in mode_sim]
        y = [val[dim_y] for val in mode_sim]

        plt.plot(x, y)

    if draw_events:
        # possible label locations
        locs = [(15, 20, 'left', 'bottom', 'angle3,angleA=180,angleB=-90'), 
                (0, 25, 'center', 'bottom', 'angle3,angleA=-90,angleB=-89'), 
                (-15, 20, 'right', 'bottom', 'angle3,angleA=0,angleB=-90'), 
                (15, -20, 'left', 'top', 'angle3,angleA=180,angleB=90'), 
                (0, -25, 'center', 'top', 'angle3,angleA=90,angleB=89'), 
                (-15, -20, 'right', 'top', 'angle3,angleA=0,angleB=90'),]
        loc_index = 0

        for event in events:
            msg = event[0]
            state = event[1]
            x = state[dim_x]
            y = state[dim_y]

            location = locs[loc_index]
            loc_x = location[0]
            loc_y = location[1]
            loc_ax = location[2]
            loc_ay = location[3]
            con = location[4]

            # cycle through different label locations
            loc_index += 1

            if loc_index >= len(locs):
                loc_index = 0

            #plt.plot([x], [y], 'o')
            plt.annotate(msg, xy=(x, y), xytext=(loc_x, loc_y), \
                         textcoords='offset points', horizontalalignment=loc_ax, verticalalignment=loc_ay, \
                        arrowprops=dict(arrowstyle="->", connectionstyle=con))
        
    if draw_func is not None:
        draw_func()

    plt.savefig(filename, dpi=300)










