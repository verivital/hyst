'''
Pysim Utility Functions
Stanley Bak (Feb 2016)
'''

from hybridpy.pysim.simulate import simulate_one_time
from hybridpy.pysim.simulate import simulate_one
from hybridpy.pysim.simulate import simulate_multi

def simulate_with_times(q, all_times, max_jumps=500, solver='vode'):
    '''
    simulate a hybrid automaton, with guaranteed points at specific times
    all_times is a sorted list of times where we must have a sample

    returns a list of ModeSims
    '''

    rv = []
    ha = q[0].parent
    last_time = 0

    for time in all_times:
        if time == last_time:
            continue

        delta = time - last_time

        # simulate from q for delta time
        traces = simulate_one(q, delta, max_jumps, solver, reraise_errors=True)['traces']
        
        for t in traces:
            for x in xrange(len(t.times)):
                t.times[x] += last_time

        rv += traces
        last_time = time
        last_ms = traces[-1]
        last_mode = ha.modes[last_ms.mode_name]
        last_point = last_ms.points[-1]
        q = (last_mode, last_point)

    return rv

def simulate_der_range(ha, der_var_index, mode_name, point, time_ranges, max_jumps=500, solver='vode'):
    '''
    simulates a hybrid automaton from a given mode/point, getting the interval range 
    for the derivative of a given variable at der_var_index,
    for each time in a selected list of time ranges

    time_ranges is an array of (min,max) time intervals which correspond to the result

    returns a semi-colon separated list of intervals, where each interval is a comma-seperated
    pair of numbers 'min,max'
    '''

    q = (ha.modes[mode_name], point)

    all_times = []

    for time_range in time_ranges:
        all_times += [time_range[0], time_range[1]]

    all_times.sort()

    state_list = simulate_with_times(q, all_times, max_jumps, solver)

    ranges = [[float("inf"), float("-inf")] for _ in xrange(len(time_ranges))]

    for mode_sim in state_list:
        mode = ha.modes[mode_sim.mode_name]

        # skip urgent modes as derivatives are in transit
        if mode.der is None:
            continue

        for i in xrange(len(mode_sim.times)):
            time = mode_sim.times[i]
            pt = mode_sim.points[i]
        
            der_vector = mode.der(time, pt)
            der_val = der_vector[der_var_index]

            for r_index in xrange(len(ranges)):
                time_range = time_ranges[r_index]
                
                if time < time_range[0] or time > time_range[1]:
                    continue

                r = ranges[r_index]
                r[0] = min(r[0], der_val)
                r[1] = max(r[1], der_val)

    return ranges_to_string(ranges)

def ranges_to_string(ranges):
    ''' converts a list of interval ranges to a semicolon-separated list of
    comma-separated values
    '''

    interval_strings = []

    for i in ranges:
        i_str = "{},{}".format(i[0], i[1])
        interval_strings.append(i_str)

    return ";".join(interval_strings)

def simulate_times(ha, mode_name, point, times, max_jumps=500, solver='vode'):
    '''simulates a hybrid automaton from a given mode/point, getting the state at a list of passed-in times
    returns a semi-colon separated string of mode_name, point_dim_0, point_dim_1, ... , point_dim_n
    '''
    rv = ""

    q = (ha.modes[mode_name], point)
    last_time = 0

    for time in times:
        delta = time - last_time
        last_time = time

        # simulate from q for delta time
        q = simulate_one_time(q, delta, max_jumps, solver)

        entry = q[0].name

        for d in q[1]:
            entry += "," + str(d)

        if len(rv) > 0:
            rv += ";"

        rv += entry

    return rv

def simulate_set_time(ha, mode_names, points, time, max_jumps=500, solver='vode'):
    '''simulates a hybrid automaton from a given set of modes/points, getting the state at a fixed final time
    returns a semi-colon separated string of mode_name, point_dim_0, point_dim_1, ... , point_dim_n
    '''

    trajectories = simulate_multi_trajectory_time(ha, mode_names, points, time, max_jumps=max_jumps, solver=solver)
    last_states = []

    for traj in trajectories.split('|'):
        last_state = traj.split(';')[-1]
        last_states.append(last_state)

    return ';'.join(last_states)

def simulate_multi_trajectory_time(ha, mode_names, points, time, min_steps=100, max_jumps=500, solver='vode'):
    '''simulates a hybrid automaton from a list of modes/points to a maximum time, with a minimum number
    of intermediate steps (which determines a max step size).
    returns the trajectories, separated by '|', where each
    trajectory is a semi-colon separated list of mode_name,point_dim_0,point_dim_1, ... , point_dim_m
    '''

    assert len(mode_names) == len(points)

    max_step = float(time) / float(min_steps)
    
    q_list = []

    for i in xrange(len(mode_names)):
        mode = mode_names[i]
        point = points[i]

        q_list.append((ha.modes[mode], point))

    res_list = simulate_multi(q_list, time, max_jumps=max_jumps, max_step=max_step, solver_name=solver)

    rv_list = []

    for res in res_list:
        traj = []
        
        for mode_sim in res['traces']:
            mode = mode_sim.mode_name
            points = mode_sim.points
        
            for point in points:
                point_dims = [str(d) for d in point]
                point_str = ",".join(point_dims)
                traj.append(mode + "," + point_str)

        
        rv_list.append(';'.join(traj))

    return '|'.join(rv_list)
