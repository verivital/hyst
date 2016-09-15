'''
Created by Hyst v1.3
Hybrid Automaton in PySim
Converted from file: 
Command Line arguments: -gen nav "-matrix -1.2 0.1 0.1 -1.2 -i_list 2 2 A 4 3 4 B 2 4 -width 3 -startx 0.5 -starty 1.5 -noise 0.1" -o nav_fig1b.py -tool pysim "-corners True -legend False -rand 100 -time 5 -title nav_fig1b"
'''

import hybridpy.pysim.simulate as sim
from hybridpy.pysim.hybrid_automaton import HybridAutomaton
from hybridpy.pysim.hybrid_automaton import HyperRectangle
from hybridpy.pysim.simulate import init_list_to_q_list
from sympy.core import symbols
from sympy import And, Or

def define_ha():
    '''make the hybrid automaton and return it'''
    # Variable ordering: [x, y, xvel, yvel]

    sym_x, sym_y, sym_xvel, sym_yvel = symbols('x y xvel yvel ')

    ha = HybridAutomaton()

    mode_0_0 = ha.new_mode('mode_0_0')
    mode_0_0.inv = lambda state: state[0] <= 1 and state[1] <= 1
    mode_0_0.inv_sympy = And(sym_x <= 1, sym_y <= 1)
    mode_0_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 1) + 0.1 * (state[3] - 0.00000000000000006123233995736766), 0.1 * (state[2] - 1) + -1.2 * (state[3] - 0.00000000000000006123233995736766)]
    mode_0_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_0 = ha.new_mode('mode_1_0')
    mode_1_0.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] <= 1
    mode_1_0.inv_sympy = And(And(sym_x >= 1, sym_x <= 2), sym_y <= 1)
    mode_1_0.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 1) + 0.1 * (state[3] - 0.00000000000000006123233995736766), 0.1 * (state[2] - 1) + -1.2 * (state[3] - 0.00000000000000006123233995736766)]
    mode_1_0.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_0 = ha.new_mode('mode_2_0')
    mode_2_0.inv = lambda state: state[0] >= 2 and state[1] <= 1
    mode_2_0.inv_sympy = And(sym_x >= 2, sym_y <= 1)
    mode_2_0.der = lambda _, state: [0, 0, 0, 0]
    mode_2_0.der_interval_list = [[0, 0], [0, 0], [0, 0], [0, 0]]

    mode_0_1 = ha.new_mode('mode_0_1')
    mode_0_1.inv = lambda state: state[0] <= 1 and state[1] >= 1 and state[1] <= 2
    mode_0_1.inv_sympy = And(And(sym_x <= 1, sym_y >= 1), sym_y <= 2)
    mode_0_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.00000000000000012246467991473532) + 0.1 * (state[3] - -1), 0.1 * (state[2] - 0.00000000000000012246467991473532) + -1.2 * (state[3] - -1)]
    mode_0_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_1_1 = ha.new_mode('mode_1_1')
    mode_1_1.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 1 and state[1] <= 2
    mode_1_1.inv_sympy = And(And(And(sym_x >= 1, sym_x <= 2), sym_y >= 1), sym_y <= 2)
    mode_1_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.7071067811865476) + 0.1 * (state[3] - -0.7071067811865475), 0.1 * (state[2] - 0.7071067811865476) + -1.2 * (state[3] - -0.7071067811865475)]
    mode_1_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_1 = ha.new_mode('mode_2_1')
    mode_2_1.inv = lambda state: state[0] >= 2 and state[1] >= 1 and state[1] <= 2
    mode_2_1.inv_sympy = And(And(sym_x >= 2, sym_y >= 1), sym_y <= 2)
    mode_2_1.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.00000000000000012246467991473532) + 0.1 * (state[3] - -1), 0.1 * (state[2] - 0.00000000000000012246467991473532) + -1.2 * (state[3] - -1)]
    mode_2_1.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_0_2 = ha.new_mode('mode_0_2')
    mode_0_2.inv = lambda state: state[0] <= 1 and state[1] >= 2
    mode_0_2.inv_sympy = And(sym_x <= 1, sym_y >= 2)
    mode_0_2.der = lambda _, state: [0, 0, 0, 0]
    mode_0_2.der_interval_list = [[0, 0], [0, 0], [0, 0], [0, 0]]

    mode_1_2 = ha.new_mode('mode_1_2')
    mode_1_2.inv = lambda state: state[0] >= 1 and state[0] <= 2 and state[1] >= 2
    mode_1_2.inv_sympy = And(And(sym_x >= 1, sym_x <= 2), sym_y >= 2)
    mode_1_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 1) + 0.1 * (state[3] - 0.00000000000000006123233995736766), 0.1 * (state[2] - 1) + -1.2 * (state[3] - 0.00000000000000006123233995736766)]
    mode_1_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    mode_2_2 = ha.new_mode('mode_2_2')
    mode_2_2.inv = lambda state: state[0] >= 2 and state[1] >= 2
    mode_2_2.inv_sympy = And(sym_x >= 2, sym_y >= 2)
    mode_2_2.der = lambda _, state: [state[2], state[3], -1.2 * (state[2] - 0.00000000000000012246467991473532) + 0.1 * (state[3] - -1), 0.1 * (state[2] - 0.00000000000000012246467991473532) + -1.2 * (state[3] - -1)]
    mode_2_2.der_interval_list = [[0, 0], [0, 0], [-0.1, 0.1], [-0.1, 0.1]]

    t = ha.new_transition(mode_0_0, mode_1_0)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x >= 1

    t = ha.new_transition(mode_0_0, mode_0_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y >= 1

    t = ha.new_transition(mode_1_0, mode_0_0)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x <= 1

    t = ha.new_transition(mode_1_0, mode_2_0)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x >= 2

    t = ha.new_transition(mode_1_0, mode_1_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y >= 1

    t = ha.new_transition(mode_2_0, mode_1_0)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x <= 2

    t = ha.new_transition(mode_2_0, mode_2_1)
    t.guard = lambda state: state[1] >= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y >= 1

    t = ha.new_transition(mode_0_1, mode_1_1)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x >= 1

    t = ha.new_transition(mode_0_1, mode_0_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y <= 1

    t = ha.new_transition(mode_0_1, mode_0_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y >= 2

    t = ha.new_transition(mode_1_1, mode_0_1)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x <= 1

    t = ha.new_transition(mode_1_1, mode_2_1)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x >= 2

    t = ha.new_transition(mode_1_1, mode_1_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y <= 1

    t = ha.new_transition(mode_1_1, mode_1_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y >= 2

    t = ha.new_transition(mode_2_1, mode_1_1)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x <= 2

    t = ha.new_transition(mode_2_1, mode_2_0)
    t.guard = lambda state: state[1] <= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y <= 1

    t = ha.new_transition(mode_2_1, mode_2_2)
    t.guard = lambda state: state[1] >= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y >= 2

    t = ha.new_transition(mode_0_2, mode_1_2)
    t.guard = lambda state: state[0] >= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x >= 1

    t = ha.new_transition(mode_0_2, mode_0_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y <= 2

    t = ha.new_transition(mode_1_2, mode_0_2)
    t.guard = lambda state: state[0] <= 1
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x <= 1

    t = ha.new_transition(mode_1_2, mode_2_2)
    t.guard = lambda state: state[0] >= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x >= 2

    t = ha.new_transition(mode_1_2, mode_1_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y <= 2

    t = ha.new_transition(mode_2_2, mode_1_2)
    t.guard = lambda state: state[0] <= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_x <= 2

    t = ha.new_transition(mode_2_2, mode_2_1)
    t.guard = lambda state: state[1] <= 2
    t.reset = lambda state: [None, None, None, None]
    t.guard_sympy = sym_y <= 2

    return ha

def define_init_states(ha):
    '''returns a list of (mode, HyperRectangle)'''
    # Variable ordering: [x, y, xvel, yvel]
    rv = []

    r = HyperRectangle([(0.5, 0.5), (1.5, 1.5), (-1, 1), (-1, 1)])
    rv.append((ha.modes['mode_0_1'], r))

    return rv


def simulate(init_states, max_time=5):
    '''simulate the automaton from each initial rect'''

    q_list = init_list_to_q_list(init_states, center=True, star=True, corners=True, rand=100)
    result = sim.simulate_multi(q_list, max_time)

    return result

def plot(result, init_states, filename='plot.png', dim_x=0, dim_y=1):
    '''plot a simulation result to a file'''

    draw_events = len(result) == 1
    shouldShow = False
    sim.plot_sim_result_multi(result, dim_x, dim_y, filename, draw_events, legend=False, title='nav_fig1b', show=shouldShow, init_states=init_states)

if __name__ == '__main__':
    ha = define_ha()
    init_states = define_init_states(ha)
    plot(simulate(init_states), init_states)

