'''
Pysim Utility Functions
Stanley Bak (Feb 2016)
'''

from hybridpy.pysim.simulate import simulate_one_time
from hybridpy.pysim.simulate import simulate_one

def simulate_with_times(q, all_times, max_jumps=500, solver='vode'):
    '''
    simulate a hybrid automaton, with guaranteed points at specific times
    all_times is a sorted list of times where we must have a sample

    returns a list of ModeSims
    '''

    rv = []
    last_time = 0

    print "all_times = {}".format(repr(all_times))

    for time in all_times:
        delta = time - last_time

        # simulate from q for delta time
        traces = simulate_one(q, delta, max_jumps, solver, reraise_errors=True)['traces']
        
        TODO."working here, basically we need to offset all the times by last_time"
        traces = [ModeSim(name, points, times) ]
        
        rv += traces
        last_time = time

    print "simulate_with_times returning {}".format(repr(rv))

    return rv

def simulate_ranges(ha, mode_name, point, time_ranges, max_jumps=500, solver='vode'):
    '''
    simulates a hybrid automaton from a given mode/point, getting the hyperrectangles reached
    within a passed-in list of time ranges

    time_ranges is an array or (min,max) time intervals which correspond to the result

    returns a semi-colon-seperated list of hyperrectangles
    each hyperrectangle is a list of comma-separated values of size 2*N, where
    N is the number of dimensions. These are min1,max1,min2,max2,... with the 
    min and max values for each dimension.
    '''

    q = (ha.modes[mode_name], point)

    all_times = []

    for time_range in time_ranges:
        all_times += [time_range[0], time_range[1]]

    all_times.sort()

    state_list = simulate_with_times(q, all_times, max_jumps, solver)

    num_dims = len(point)
    ranges = []

    for _ in xrange(len(time_ranges)):
        box = []
         
        for _ in xrange(num_dims):
            box.append([float("inf"), float("-inf")])
            
        ranges.append(box)

    for mode_sim in state_list:

        for i in xrange(len(mode_sim.times)):
            time = mode_sim.times[i]
            pt = mode_sim.points[i]

            for r_index in xrange(len(ranges)):
                time_range = time_ranges[r_index]
                
                if time < time_range[0] or time > time_range[1]:
                    continue

                r = ranges[r_index]

                for dim_index in xrange(num_dims):
                    val = pt[dim_index]

                    r[dim_index][0] = min(r[dim_index][0], val)
                    r[dim_index][1] = max(r[dim_index][1], val)

    print "result = {}", repr(ranges)

    return ranges_to_string(ranges, num_dims)

def ranges_to_string(ranges, num_dims):
    ''' converts a list of interval ranges to a semicolon-separated list of
    comma-separated values
    '''

    box_strings = []

    for box in ranges:
        point_str_list = ["{},{}".format(box[dim][0], box[dim][1]) for dim in xrange(num_dims)]
        box_str = ",".join(point_str_list)
        box_strings.append(box_str)

    return ";".join(box_strings)

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








