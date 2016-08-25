'''
Created by Hyst v1.2
Hybrid Automaton in PyRrt
Converted from file: /home/stan/repositories/hyst/src/tests/regression/models/lorenz/lorenz.xml
Command Line arguments: /home/stan/repositories/hyst/src/tests/regression/models/lorenz/lorenz.xml -o /home/stan/repositories/hyst/src/tests/regression/result/lorenz_pyrrt.py -pyrrt
'''
import hybridpy.pyrrt.expt_opt as rrt

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
    running.inv_strings = ['0 == 0']

    return ha

def define_init_states(ha):
    '''returns a list of (mode, HyperRectangle)'''
    # Variable ordering: [x, y, z]
    rv = []

    r = HyperRectangle([(14.999, 15.001), (14.999, 15.001), (35.999, 36.001)])
    rv.append((ha.modes['running'], r))

    return rv


def run(num_iterations=100):
    'runs rrt on the model and returns a result object'
    return rrt.run(num_iterations, define_init_states, define_ha)

def plot(nodes, image_path):
    'plot a result object produced by run()'
    rrt.plot(nodes, image_path)

if __name__ == '__main__':
    plot(run(), 'out.png')

