'''
Created by Hyst v1.2
Hybrid Automaton in PySim
Converted from file: /home/stan/repositories/hyst/src/tests/regression/models/lorenz/lorenz.xml
Command Line arguments: /home/stan/repositories/hyst/src/tests/regression/models/lorenz/lorenz.xml -o /home/stan/repositories/hyst/src/tests/regression/result/lorenz_pysim.py -pysim
'''

import hybridpy.pysim.simulate as sim
from hybridpy.pysim.simulate import init_list_to_q_list
from hybridpy.pysim.hybrid_automaton import HybridAutomaton
from hybridpy.pysim.hybrid_automaton import HyperRectangle

def define_ha():
    '''make the hybrid automaton and return it'''

    ha = HybridAutomaton()
    ha.variables = ["x", "y", "z"]


    running = ha.new_mode('running')
    running.inv = lambda state: True
    running.der = lambda _, state: [10 + state[1] - state[0], state[0] * (28 - state[2]) - state[1], state[0] * state[1] - 2.6667 * state[2]]
    running.der_interval_list = [[0, 0], [0, 0], [0, 0]]

    return ha

def define_init_states(ha):
    '''returns a list of (mode, HyperRectangle)'''
    # Variable ordering: [x, y, z]
    rv = []

    r = HyperRectangle([(14.999, 15.001), (14.999, 15.001), (35.999, 36.001)])
    rv.append((ha.modes['running'], r))

    return rv


def simulate(max_time=6.5):
    '''simulate the automaton from each initial rect'''

    ha = define_ha()
    init_states = define_init_states(ha)
    q_list = init_list_to_q_list(init_states, center=True, star=True, corners=False, rand=0)
    result = sim.simulate_multi(q_list, max_time)

    return result

def plot(result, filename='plot.png', dim_x=0, dim_y=1):
    '''plot a simulation result to a file'''

    draw_events = len(result) == 1
    shouldShow = False
    sim.plot_sim_result_multi(result, dim_x, dim_y, filename, draw_events, legend=True, title='Simulation', show=shouldShow)

if __name__ == '__main__':
    plot(simulate())

