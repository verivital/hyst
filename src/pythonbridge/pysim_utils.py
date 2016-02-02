'''
Pysim utility functions
Stanley Bak (Feb 2016)
'''

from hybridpy.pysim.simulate import simulate_one_time as sim_time

def simulate_times(ha, mode_name, point, times, num_steps=10):
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
        q = sim_time(q, delta, num_steps)

        if q == None: # invariant became false
            break

        entry = q[0].name

        for d in q[1]:
            entry += "," + str(d)

        if len(rv) > 0:
            rv += ";"

        rv += entry

    return rv








