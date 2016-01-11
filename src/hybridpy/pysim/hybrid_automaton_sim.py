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
            state = t.reset(state)
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
    elif dim_x < 0 or dim_x >= len(traces[0]):
        raise RuntimeError('X dimension out of bounds: ' + str(dim_x))
    elif dim_y < 0 or dim_y >= len(traces[0]):
        raise RuntimeError('Y dimension out of bounds: ' + str(dim_x))

    for mode_sim in traces:
        x = [val[dim_x] for val in mode_sim]
        y = [val[dim_y] for val in mode_sim]

        plt.plot(x, y)

    if draw_events:
        above = True

        for event in events:
            msg = event[0]
            state = event[1]
            x = state[dim_x]
            y = state[dim_y]

            # alternate labels between above and below
            y_offset = 20
            above = not above

            if above:
                y_offset = -20

            plt.annotate(msg, xy=(x, y), xytext=(20, y_offset), \
                         textcoords='offset points', horizontalalignment='left', \
                        arrowprops=dict(arrowstyle="->", connectionstyle="arc3,rad=.6"))
        
    if draw_func is not None:
        draw_func()

    plt.savefig(filename)





