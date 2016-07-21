import sys
#import os
#import matplotlib.pyplot as plt
import numpy as np
import scipy
#from scipy.integrate import odeint
#import math
#import importlib
from util import *
import util
import random
#import time
#from cvxopt import matrix, solvers
from scipy.optimize import linprog
import matplotlib.pyplot as plt

#t = np.linspace(0, 0.2, 4)


#####################################################################
# END OF IMPORT STATEMENTS #
#####################################################################

'''
    Class Node for representing a Node in the RRT. Maintains a parent pinter
    and a list of pointers to children.
'''


class Node:

    def __init__(self, par, Id, disc, cont):
        '''
            Params : parent's id, own id, discrete mode values, continuous mode
            values
        '''
        self.par = par
        self.childList = []
        self.id = Id
        self.disc = disc  # Stands for the mode itself, instead of the modename
        self.cont = cont
        self.dictOfModes = []

    def compare(self, other):
        '''
            Checks for equality of two nodes
        '''
        if self.par == other.par and self.disc.name == other.disc.name:
            if all(x in self.cont for x in other.cont):
                return True
            else:
                return False
        else:
            return False

    def greater(self, other):
        '''
            Defines a partial order on the nodes by defining the greater
            than operation on two nodes
        '''
        if self.disc > other.disc:
            return True
        else:
            return False

    def addChild(self, id):
        '''
            Adds a child to the child List of a node
        '''
        self.childList.append(id)


##########################################################################
        # END OF THE NODE CLASS #
##########################################################################


def bfs(adj, nodetwodisc, dict):
    '''
        Method to perform a BFS on the states of an Hybrid Automaton,
        Params: adjacency list adj, the starting point nodetwodisc, dictionary
        representing the mode to int mapping
        Returns : a vector jumps, jumps[i] = distance of node i form the
        node nodetwodisc, in discrete jumps
    '''
    visited = [False] * len(adj)
    visited[dict[nodetwodisc]] = True
    list = []
    list.append(dict[nodetwodisc])
    jumps = [0] * len(adj)
    jumps[dict[nodetwodisc]] = 0
    while len(list) > 0:
        visited[list[0]] = True
        for i in adj[list[0]]:
            if not visited[i]:
                list.append(i)
                visited[i] = True
                jumps[i] = jumps[list[0]] + 1
        list.pop(0)

    return jumps

##########################################################################
    # END OF BFS PROCEDURE #
##########################################################################

##########################################################################
    # FINDING A RANDOM POINT #
##########################################################################


def findlambda(discrand, lambdalow, lambdahigh, randompt):
    '''
        Finds the maximum value of lambda such that the point x
        + lambda*[1,1,1,...] is at the boundary
    '''
    mid = (lambdalow + lambdahigh) / 2.0
    perturbation = [mid] * len(randompt)

    if lambdahigh - lambdalow <= 0.05:
        return lambdalow

    if discrand.inv([randompt[i] + perturbation[i]
                     for i in range(len(perturbation))]):
        return findlambda(discrand, mid, lambdahigh, randompt)
    else:
        return findlambda(discrand, lambdalow, mid, randompt)


def findlambdamin(discrand, lambdalow, lambdahigh, randompt):
    '''
        Finds the minimum value of lambda such that the point x
        + lambda*[1,1,1,...] is at the boundary
    '''
    mid = (lambdalow + lambdahigh) / 2.0
    perturbation = [mid] * len(randompt)

    if lambdahigh - lambdalow <= 0.05:
        return lambdalow

    if discrand.inv([randompt[i] + perturbation[i]
                     for i in range(len(perturbation))]):
        return findlambdamin(discrand, lambdalow, mid, randompt)
    else:
        return findlambdamin(discrand, mid, lambdahigh, randompt)


def randomGenNew(mRRT):
    '''
        Randomly generate  a point given the RRT object
    '''
    discrand = random.choice(mRRT.ha.modes.keys())
    discrand = mRRT.ha.modes[discrand]

    if discrand not in mRRT.extremepoints:
        '''
            Solve the Ax<=b equation to get the random point
        '''
        randompt = mRRT.linearsolutions[discrand]
        lamda = 5.0
        lambdamax = findlambda(discrand, 0.0, 5.0, randompt)
        lambdamin = findlambdamin(discrand, -5.0, 0.0, randompt)
        lamda = random.uniform(lambdamin, lambdamax)
        perturbation = [lamda] * len(randompt)
        randompt = [randompt[i] + perturbation[i]
                    for i in range(len(randompt))]

        return Node(-2, -2, discrand, randompt)

    else:
        randompt = []
        for i in range(len(mRRT.extremepoints[discrand])):
            randompt.append(random.random() *
                            (mRRT.extremepoints[discrand][i][1] -
                             mRRT.extremepoints[discrand][i][0]) +
                            mRRT.extremepoints[discrand][i][0])

        '''
            Perturb the randompoint generated a bit
            such that it still lies inside the region
        '''
        lamda = 5.0
        '''
            Find the maximum lambda such that the vector
            x + labda*[1,1,1....]
            till lies inside the region and then choose the
            value of lambda from these set of values
        '''
        lambdamax = findlambda(discrand, 0.0, 5.0, randompt)
        lambdamin = findlambdamin(discrand, -5.0, 0.0, randompt)
        lamda = random.uniform(lambdamin, lambdamax)
        perturbation = [lamda] * len(randompt)
        # print "LAMBDA", lambdamin, lambdamax;
        randompt = [randompt[i] + perturbation[i]
                    for i in range(len(randompt))]

        return Node(-2, -2, discrand, randompt)

##########################################################################

##########################################################################
        # DEFINITON OF THE RRT CLASS
##########################################################################


'''
    Class RRT for representing and operating upon RRTs
'''


class RRT:

    def __init__(self, ha, initial, mode):
        '''
            Params : ha is the hybrid automaton, initial : the set
            of initial states, passed as a list of values of continuous
            variables, mode : the name of the discrete Mode
            from which the points are chosen
        '''
        self.ha = ha
        self.start = initial
        self.Nodes = []
        self.varorder = ha.variables
        #self.counter = 0;

        '''
            Stores the points which minimise the value of Ax <=b
            (LP for the guard) between a pair of states,
            The first value is a boolean value indicating whether the
            minimum value of LP between the pair of
            states has been updated (i.e. the value ahead in the list
            is updated or not), The seocn element of
            the list is the minimum value of the LP, the third element
            onwards, the list has the points which
            lead to this minimum value of LP.
        '''
        self.statetable = {}
        for i in self.ha.modes:
            for j in self.ha.modes:
                self.statetable[(i, j)] = [False, float('inf'), None]

        '''
            Pending points is a dictionary which stores the list of pending
            points, the points which have been
            added into the tree but whose LP has been calculated yet, and
            needs to be calculated at the time of
            using that mode.
        '''
        self.pendingpoints = {}
        for i in self.ha.modes:
            self.pendingpoints[i] = []

        '''
        Stores the  dictionary whith each mode as key and nodes segregated
        according to the modes.
        '''
        self.classifiedpoints = {}
        for modes in self.ha.modes:
            self.classifiedpoints[self.ha.modes[modes]] = []

        ans = -1

        # Put the initial nodes into the tree
        '''
            Put the initial nodes into the tree and maintain the statetable
            and the Classified Points
        '''
        for val in initial:
            tmp = Node(-1, 0, mode, val)
            self.Nodes.append(tmp)
            self.classifiedpoints[mode].append(tmp)
            for transition in mode.transitions:
                t = self.distsymbolic(tmp, transition.guard_strings)
                if t < self.statetable[
                    (transition.from_mode.name,
                     transition.to_mode.name)][1]:
                    self.statetable[
                        (transition.from_mode.name,
                         transition.to_mode.name)][1] = t
                    self.statetable[
                        (transition.from_mode.name,
                         transition.to_mode.name)][0] = True
                    self.statetable[
                        (transition.from_mode.name, transition.to_mode.name)][
                        2:] = [tmp]
                elif t == self.statetable[(transition.from_mode.name, transition.to_mode.name)][1]:
                    self.statetable[
                        (transition.from_mode.name,
                         transition.to_mode.name)].append(tmp)

        '''
            self.dict : store the mode vs int mapping for the automaton
            self.reversedict : store the int vs mode mapping for the automaton
        '''
        self.dict = {}
        self.reversedict = {}

        j = 0
        for modes in self.ha.modes:
            self.dict[self.ha.modes[modes]] = j
            self.reversedict[j] = self.ha.modes[modes]
            j = j + 1

        '''
            adj and adjrev store the adjacency lists and the reverse of it respectively, (reverse means reverse edges)
        '''
        self.adj = self.ConvertAutomataToDAG()
        self.adjrev = self.ConvertAutomataToReverseDAG()

        # Make a transition dict for the automaton
        '''
            Stores the dictionary of pair of nodes as the key and the
            transition as the value.
        '''
        self.transitiondict = {}
        for transitions in self.ha.transitions:
            self.transitiondict[
                (transitions.from_mode, transitions.to_mode)] = transitions

        # Maintain a list of control Signals in a dictionary modewise
        '''
            Maintain the list of control signals in a dictionary with key
            as the modes
        '''
        self.controlsig = {}
        for mode in self.ha.modes:
            der_list = self.ha.modes[mode].der_interval_list
            dimlist = [[]] * len(der_list)
            for i in range(0, len(der_list)):
                dimlist[i] = np.linspace(der_list[i][0], der_list[i][1], 4)
                dimlist[i] = list(set(dimlist[i]))

            self.controlsig[self.ha.modes[mode]] = recurse(dimlist, 0)

        '''
            Store the equations for invariants of the modes as matrices
            because these need to be used many times.
        '''
        self.parsedequations = {}
        for modes in self.ha.modes:
            A, b = util.parser(
                self.ha.modes[modes].inv_strings, self.ha.variables)
            self.parsedequations[self.ha.modes[modes]] = (A, b)

        '''
            Store the solutions to the above parsedequations matrices,
            and store them, Used while applying randomGenNew()
        '''
        self.linearsolutions = {}
        for modes in self.ha.modes:

            c = np.zeros(len(self.Nodes[0].cont))
            res = linprog(
                c, A_ub=self.parsedequations[
                    self.ha.modes[modes]][0], b_ub=self.parsedequations[
                    self.ha.modes[modes]][1])
            print res.x
            ans = res.x
            self.linearsolutions[self.ha.modes[modes]] = ans
            
        '''
            A dynamic dictionary to store the bounding box coordinates for
            the mode. Updated dynamically when nodes are added into the tree.
        '''
        self.extremepoints = {}
        for nodes in self.Nodes:
            if nodes.disc in self.extremepoints:
                for i in range(len(self.extremepoints[nodes.disc])):
                    self.extremepoints[nodes.disc][i][0] = min(
                        self.extremepoints[nodes.disc][i][0], nodes.cont[i])
                    self.extremepoints[nodes.disc][i][1] = max(
                        self.extremepoints[nodes.disc][i][1], nodes.cont[i])

            else:
                self.extremepoints[nodes.disc] = []
                for i in range(len(nodes.cont)):
                    self.extremepoints[nodes.disc].append(
                        [nodes.cont[i], nodes.cont[i]])

        return

    def dist(self, nodeone, nodetwo):
        ''' Continuous distance between two nodes '''
        ''' Used for two flows with the same mode '''
        sum = 0.0
        for i in range(0, len(nodeone.cont)):
            sum = sum + (nodeone.cont[i] - nodetwo.cont[i])**2

        return np.sqrt(sum)

    def ConvertAutomataToDAG(self):
        '''
            Converts the hybrid automaton to a Directed Graph (can be cyclic)
            and returns the adjacency list
        '''
        adj = []
        j = 0
        for i in range(0, len(self.ha.modes)):
            adj.append([])

        for i in range(0, len(self.ha.transitions)):
            st = self.ha.transitions[i].from_mode
            en = self.ha.transitions[i].to_mode
            adj[self.dict[st]].append(self.dict[en])

        return adj

    def ConvertAutomataToReverseDAG(self):
        '''
            Reverses the directions of edges and then converts the automaton
            to a
            directed graph and returns the adjacency list
        '''
        adj = []
        for i in range(0, len(self.ha.modes)):
            adj.append([])

        for i in range(0, len(self.ha.transitions)):
            st = self.ha.transitions[i].from_mode
            en = self.ha.transitions[i].to_mode
            adj[self.dict[en]].append(self.dict[st])

        return adj

    def distsymbolic(self, nodeone, guard):
        '''
            guard represented symbolically here, guard is a list of the
            boundaries. i.e. a set of equalities
            But we use the invariant set of the object on the other side
            of the boundary because, the convexity
            itself ensures this.
        '''
        #self.counter = self.counter + 1;
        #start = time.time()
        x, y = util.ConvexOpt(nodeone.cont, guard, self.varorder)
        #end = time.time()
        return np.sqrt(y[0][0])

    def distsymbolicNew(self, nodeone, guard):
        '''
            guard represented symbolically here, guard is a list of
            the boundaries. i.e. a set of equalities
            But we use the invariant set of the object on the
            other side of the boundary because, the convexity
            itself ensures this.
            Returns the point which minimises the distance to the guard
        '''
        #self.counter = self.counter + 1;
        #start = time.time()
        x, y = util.ConvexOpt(nodeone.cont, guard, self.varorder)
        #end = time.time()
        return x

    def compositedist(self, nodeone, nodetwo, jumps):
        '''
            Returns the composite distance between two nodes nodeone
            and nodetwo
        '''
        if nodeone.disc.name == nodetwo.disc.name:
            return (0, self.dist(nodeone, nodetwo))
        else:

            mindist = float('inf')

            for transition in nodeone.disc.transitions:
                if jumps[
                    self.dict[
                        nodeone.disc]] > jumps[
                    self.dict[
                        transition.to_mode]]:
                    guard = transition.guard_strings
                    t = self.distsymbolic(nodeone, guard)
                    if t < mindist:
                        mindist = t

            return (jumps[self.dict[nodeone.disc]], mindist)

    def nearestneighbour(self, nodeone):
        ''' Nearest neighbour to nodeone from the nodes present in the RRT'''
        '''
            The algo is that we first separate out nodes lying in
            modes which have more discrete
            distance than the first node in the list of nodes added
            in the tree. Now from the remaining
            modes, we have to find the minimising point.
            We already maintain a statetable such that
            statetable[i][j] is a list containing the values :
            statetable[i][j][0] = True or False, indica
            ting if the minimum distance between points in the tree
            in mode i and the guard of i-j is up-to-date
            or not. The second element of this list is the value of
            this minimum Distance and the third element
            onwards is the list of all distance minimising points.

            Pending points is a dictionary having a list of points
            which have been added to the tree but not
            used in the updation of the statetable. This is done
            because, some points might be added to the tree
            but might not be used anytime for distance calculation
            because the modes in which they lie in
            never get called for distance minimisation. So, we
            update the minimum distance only if the mode
            is called. (THis is an optimisation to minimise
            number of LP solver calls).

            Every time we check if the pending points list for
            that mode is empty or not, if not we update the
            statetable first, set pending points to empty and
            then compare the distances with other modes.
        '''

        jumps = bfs(self.adjrev, nodeone.disc, self.dict)

        minnode = -1
        mindiscdist = len(self.adj) + 1
        mincontdist = float('inf')
        equals = []

        ''' Stores the modes which have lesser distance the first node'''
        jumpswithLesserDistance = []

        ''' Do the distance finding procedure for the first node in the
            tree : mRRT.Nodes[0]'''
        for node in self.Nodes:

            if jumps[self.dict[node.disc]] < mindiscdist:
                minnode = node
                mindiscdist = jumps[self.dict[node.disc]]
                t, mincontdist = self.compositedist(node, nodeone, jumps)
                equals = []
                equals.append(minnode)
                # print "Composite Distance : ", mincontdist
            elif jumps[self.dict[node.disc]] == mindiscdist:
                # t = self.dist(node, nodeone);
                t, u = self.compositedist(node, nodeone, jumps)
                if u < mincontdist:
                    mincontdist = u
                    minnode = node
                    equals = []
                    equals.append(minnode)
                elif u == mincontdist:
                    equals.append(node)
            break

        ''' Store thr modes which have lesser distance than the 
            first mode above '''
        for modes in self.ha.modes:
            if jumps[self.dict[self.ha.modes[modes]]] <= mindiscdist:
                jumpswithLesserDistance.append(self.ha.modes[modes])

        ''' Now, finding the minimum Distance point from the npoints in
            modes with lesser discrete distance '''
        for modes in jumpswithLesserDistance:

            ''' Handle the case of having points in the required target
                mode differently
                equals : contains all the points with the same distance,
                and we randomly choose out of these.
            '''
            if jumps[self.dict[modes]] == 0:
                j = 0
                for nodes in self.classifiedpoints[modes]:
                    if j == 0:
                        mindiscdist = 0
                        mincontdist = float('inf')
                        j = 1
                        equals = []
                    if self.dist(nodes, nodeone) < mincontdist:
                        mincontdist = self.dist(nodes, nodeone)
                        equals = []
                        equals.append(nodes)
                    elif self.dist(nodes, nodeone) == mincontdist:
                        equals.append(nodes)

                return random.choice(equals)

            if (jumps[self.dict[modes]] < mindiscdist) and (len(
                    self.classifiedpoints[modes]) > 0 or len(self.pendingpoints[modes.name]) > 0):
                # print "Got in here"
                if len(self.pendingpoints[modes.name]) == 0:
                    j = 0
                    for transition in modes.transitions:
                        if jumps[
                                self.dict[
                                    transition.to_mode]] < jumps[
                                self.dict[modes]]:
                            if j == 0:
                                equals = []
                                mincontdist = float('inf')
                                mindiscdist = jumps[self.dict[modes]]
                                minnode = None
                                j = 1
                            if self.statetable[(modes.name, transition.to_mode.name)][
                                    1] < mincontdist:
                                equals = []
                                mincontdist = self.statetable[
                                    (modes.name, transition.to_mode.name)][1]
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                # print "1" , minnode
                                equals = equals + minnode
                            elif self.statetable[(modes.name, transition.to_mode.name)][1] == mincontdist:
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                equals = equals + minnode
                                # print "2", minnode;

                else:
                    '''
                        Handle points if some points have been added and not
                        been taken into account for
                        finding the minimum distance.
                    '''
                    for i in range(len(self.pendingpoints[modes.name])):
                        for transition in modes.transitions:
                            t = self.distsymbolic(self.pendingpoints[modes.name][
                                                  i], transition.guard_strings)
                            if self.statetable[
                                    (modes.name, transition.to_mode.name)][1] > t:
                                self.statetable[
                                    (modes.name, transition.to_mode.name)][1] = t
                                self.statetable[
                                    (modes.name, transition.to_mode.name)][
                                    2:] = [
                                    self.pendingpoints[
                                        modes.name][i]]

                            elif self.statetable[(modes.name, transition.to_mode.name)][1] == t:
                                self.statetable[(modes.name, transition.to_mode.name)].append(
                                    self.pendingpoints[modes.name][i])

                            self.statetable[
                                (modes.name, transition.to_mode.name)][0] = True

                    self.pendingpoints[modes.name] = []

                    #mincontdist = float('inf');
                    j = 0
                    #minnode = None;
                    for transition in modes.transitions:
                        if jumps[
                                self.dict[
                                    transition.to_mode]] < jumps[
                                self.dict[modes]]:
                            if j == 0:
                                j = 1
                                equals = []
                                mincontdist = float('inf')
                                mindiscdist = jumps[self.dict[modes]]
                                minnode = None
                            if self.statetable[(modes.name, transition.to_mode.name)][
                                    1] < mincontdist:
                                equals = []
                                mincontdist = self.statetable[
                                    (modes.name, transition.to_mode.name)][1]
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                # print "3", minnode
                                equals = equals + minnode
                            elif self.statetable[(modes.name, transition.to_mode.name)][1] == mincontdist:
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                # print "4", minnode
                                equals = equals + minnode

            elif jumps[self.dict[modes]] == mindiscdist and (len(self.classifiedpoints[modes]) > 0 or len(self.pendingpoints[modes.name]) > 0):
                if len(self.pendingpoints[modes.name]) == 0:
                    for transition in modes.transitions:
                        if jumps[
                                self.dict[
                                    transition.to_mode]] < jumps[
                                self.dict[modes]]:
                            if self.statetable[(modes.name, transition.to_mode.name)][
                                    1] < mincontdist:
                                mincontdist = self.statetable[
                                    (modes.name, transition.to_mode.name)][1]
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                equals = []
                                # print "%", minnode
                                equals = equals + minnode
                            elif self.statetable[(modes.name, transition.to_mode.name)][1] == mincontdist:
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                # print "6", mincontdist, minnode
                                equals = equals + minnode

                else:
                    for i in range(len(self.pendingpoints[modes.name])):
                        for transition in modes.transitions:
                            t = self.distsymbolic(self.pendingpoints[modes.name][
                                                  i], transition.guard_strings)
                            if self.statetable[
                                    (modes.name, transition.to_mode.name)][1] > t:
                                self.statetable[
                                    (modes.name, transition.to_mode.name)][1] = t
                                self.statetable[
                                    (modes.name, transition.to_mode.name)][
                                    2:] = [
                                    self.pendingpoints[
                                        modes.name][i]]

                            elif self.statetable[(modes.name, transition.to_mode.name)][1] == t:
                                self.statetable[(modes.name, transition.to_mode.name)].append(
                                    self.pendingpoints[modes.name][i])

                            self.statetable[
                                (modes.name, transition.to_mode.name)][0] = True

                    self.pendingpoints[modes.name] = []

                    for transition in modes.transitions:
                        if jumps[
                                self.dict[
                                    transition.to_mode]] < jumps[
                                self.dict[modes]]:
                            if self.statetable[(modes.name, transition.to_mode.name)][
                                    1] < mincontdist:
                                mincontdist = self.statetable[
                                    (modes.name, transition.to_mode.name)][1]
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                equals = []
                                # print "7", minnode
                                equals = equals + minnode
                            elif self.statetable[(modes.name, transition.to_mode.name)][1] == mincontdist:
                                minnode = self.statetable[
                                    (modes.name, transition.to_mode.name)][2:]
                                # print "8", minnode
                                equals = equals + minnode

        x = random.choice(equals)
        return x

    def temp(self, t, initial, control, mode):
        '''
            Adds the control signals / uncertainities
            to the derivative values.
        '''

        d = mode.der(initial)
        for i in range(len(d)):
            f = random.random()
            d[i] = 1.0 * d[i] + 2.0 * control[i]
        return d

    def odeSolverNew(self, initNode, t, control, jumps=None):
        '''
            Main function for the odeSolver. The system of differential
            equations is defined by the temp
            function, which is defined above. The initial value
            is initNode.cont
        '''

        solver = scipy.integrate.ode(
            self.temp).set_integrator('vode', method='bdf')
        solver.set_initial_value(initNode.cont, t[0]).set_f_params(
            control, initNode.disc)
        k = 1
        mode = modet = initNode.disc
        prev = ans = initNode.cont

        multiple = False
        moden = [modet]

        '''
            Determining the set of modes which can be true at the beginning.
        '''
        for transition in initNode.disc.transitions:
            if transition.guard(initNode.cont):
                moden.append(transition.to_mode)
                multiple = True

        # regular extension and then put points in all the modes possible
        done = False
        returnList = []
        modeSet = set(moden)
        '''
            Iterate over the length of the time specifying vector untill
            you get the conditions as
            described in the algo, capturing all the important points.
        '''
        #importantPoints = []
        while solver.successful() and k < len(t):
            solver.integrate(t[k])
            prev = ans
            ans = solver.y

            for transition in initNode.disc.transitions:
                if transition.guard(ans) and transition.to_mode not in modeSet:
                    returnList.append((ans, transition.to_mode))
                    for modes in modeSet:
                        returnList.append((ans, modes))
                if transition.guard(prev) and not transition.guard(ans):
                    returnList.append((prev, transition.to_mode))
                    for modes in modeSet:
                        returnList.append((prev, modes))

        # while solver.successful() and k < len(t):
        #   solver.integrate(t[k]);
        #   prev = ans;
        #   ans = solver.y;
        #   done = False;

            # for transition in initNode.disc.transitions:
            #   if (transition.guard(ans) and transition.to_mode not in modeSet):
            #       mode = transition.to_mode;
            #       done = True;

            # if done:
            #   returnList.append((ans, mode));
            #   for modes in modeSet:
            #       returnList.append((ans, modes));
            #   return returnList;
            #   break;

            if not mode.inv(ans):
                for modes in self.ha.modes:
                    if self.ha.modes[modes].inv(ans):
                        returnList.append((ans, self.ha.modes[modes]))
                for modes in modeSet:
                    returnList.append((prev, modes))
                return returnList

            k = k + 1

        for modes in modeSet:
            returnList.append((ans, modes))
        return returnList

    def mindistFinderNew(self, nodeone, node):
        '''
            Finding the minimum Distance for the control signals.
            lom is the list returned by the ode Solver. We have 
            assigned the parameter called dictOfModes in
            the  Class Node, which basically holds the point
            closest to the continuous state of the node.
            lying on the guard. The guard here is the guard between
            a point in the list returned by the ode solver
            and the node next to it in the path from that point to
            the mode with the random point. Again,
            like the nearestneighbour function, the values stored in
            the dictofModes is calculated only if it is
            required, and so we check once if the bvalues are
            calculated or not.
        '''

        t = np.linspace(0, 0.2, 4)
        mincontdist = float('inf')
        jumps = bfs(self.adjrev, node.disc, self.dict)
        mindiscdist = len(self.adj) + 10
        mincontpart = []
        mindiscpart = -1

        signal = []
        for i in range(len(self.controlsig[nodeone.disc][0])):
            signal.append(-1)

        if len(nodeone.dictOfModes) == 0:
            nodeone.dictOfModes = []
            for i in range(len(self.ha.modes)):
                nodeone.dictOfModes.append([])

        cnt = 0
        for sig in self.controlsig[nodeone.disc]:
            lom = self.odeSolverNew(nodeone, t, sig, jumps)
            mintempdist = len(self.adj) + 10
            mintempcont = float('inf')
            minsol = float('inf')

            for i in lom:
                #cnt = cnt+1;
                val = 0
                if isinstance(i[0], int):
                    continue

                if jumps[self.dict[i[1]]] < mintempdist:
                    mintempdist = jumps[self.dict[i[1]]]
                    if jumps[self.dict[i[1]]] == 0:
                        mintempdist = 0
                        mintempcont = self.dist(Node(-1, -1, i[1], i[0]), node)
                        minsol = i

                    else:
                        if len(nodeone.dictOfModes[self.dict[i[1]]]) == 0:
                            minguard = -2
                            mindist = float('inf')
                            for transition in i[1].transitions:
                                if jumps[
                                    self.dict[
                                        transition.to_mode]] < mintempdist:
                                    cnt = cnt + 1
                                    tim = self.distsymbolic(
                                        nodeone, transition.guard_strings)
                                    # print ti;
                                    if tim <= mindist:
                                        mindist = tim
                                        minguard = transition.guard_strings

                            if minguard == -2:
                                a = 1
                                continue
                            nodeone.dictOfModes[
                                self.dict[
                                    i[1]]] = self.distsymbolicNew(
                                nodeone, minguard)

                        mintempcont = self.dist(
                            Node(-1, -1, i[1], i[0]), Node(-1, -1, -1, nodeone.dictOfModes[self.dict[i[1]]]))
                        minsol = i

                elif jumps[self.dict[i[1]]] == mintempdist:
                    if jumps[self.dict[i[1]]] == 0:
                        tim = self.dist(Node(-1, -1, i[1], i[0]), node)
                        if mintempcont > tim:
                            mintempcont = tim
                            minsol = i

                    else:
                        if len(nodeone.dictOfModes[self.dict[i[1]]]) == 0:
                            minguard = -2
                            mindist = float('inf')
                            for transition in i[1].transitions:
                                if jumps[
                                    self.dict[
                                        transition.to_mode]] < mintempdist:
                                    cnt = cnt + 1
                                    tim = self.distsymbolic(
                                        nodeone, transition.guard_strings)
                                    if tim <= mindist:
                                        mindist = tim
                                        minguard = transition.guard_strings

                            if minguard == -2:
                                a = 1
                                continue
                            nodeone.dictOfModes[
                                self.dict[
                                    i[1]]] = self.distsymbolicNew(
                                nodeone, minguard)

                        tim = self.dist(
                            Node(-1, -1, i[1], i[0]), Node(-1, -1, -1, nodeone.dictOfModes[self.dict[i[1]]]))
                        if mintempcont > tim:
                            mintempcont = tim
                            minsol = i

            if isinstance(minsol, int) or isinstance(minsol, float):
                sys.exit(0)
                continue

            mode = minsol[1]
            sol = minsol[0]

            if minsol[1] == -1:
                continue

            if isinstance(minsol[0], int):
                continue

            if jumps[self.dict[mode]] < mindiscdist:
                mindiscdist = jumps[self.dict[mode]]
                if jumps[self.dict[mode]] == 0:
                    mindiscdist = 0
                    mincontdist = self.dist(Node(-1, -1, i[1], i[0]), node)
                    mincontpart = sol
                    mindiscpart = 0
                    signal = sig

                else:
                    if len(nodeone.dictOfModes[self.dict[mode]]) == 0:
                        minguard = -2
                        mindist = float('inf')
                        for transition in mode.transitions:
                            if jumps[
                                self.dict[
                                    transition.to_mode]] < mintempdist:
                                cnt += 1
                                tim = self.distsymbolic(
                                    nodeone, transition.guard_strings)
                                if tim <= mindist:
                                    mindist = ti
                                    minguard = transition.guard_strings

                        if minguard == -2:
                            a = 1
                            continue
                        cnt += 1
                        nodeone.dictOfModes[
                            self.dict[mode]] = self.distsymbolicNew(
                            nodeone, minguard)

                    mincontdist = self.dist(
                        Node(-1, -1, mode, sol), Node(-1, -1, -1, nodeone.dictOfModes[self.dict[mode]]))
                    mindiscpart = mindiscdist
                    signal = sig
                    mincontpart = sol

            elif jumps[self.dict[mode]] == mindiscdist:
                if jumps[self.dict[mode]] == 0:
                    tim = self.dist(Node(-1, -1, mode, sol), node)
                    if mincontdist > tim:
                        mincontdist = tim
                        mincontpart = sol
                        signal = sig

                else:
                    if len(nodeone.dictOfModes[self.dict[mode]]) == 0:
                        minguard = -2
                        mindist = float('inf')
                        for transition in mode.transitions:
                            if jumps[
                                self.dict[
                                    transition.to_mode]] < mintempdist:
                                tim = self.distsymbolic(
                                    nodeone, transition.guard_strings)
                                cnt += 1
                                if tim <= mindist:
                                    mindist = ti
                                    minguard = transition.guard_strings

                        if minguard == -2:
                            a = 1
                            continue
                        nodeone.dictOfModes[
                            self.dict[mode]] = self.distsymbolicNew(
                            nodeone, minguard)

                    tim = self.dist(
                        Node(-1, -1, mode, sol), Node(-1, -1, -1, nodeone.dictOfModes[self.dict[mode]]))
                    if mincontdist > tim:
                        mincontdist = tim
                        mincontpart = sol
                        signal = sig

        # returns the signal which gives the closest set of
        # nodes to the random
        # point
        return signal

    def extendNew(self):
        '''
            Main Function that invokes other functions and
            combines the result to the RRT
        '''
        t = np.linspace(0, 0.2, 4)
        node = randomGenNew(self)
        near = self.nearestneighbour(node)
        u = self.mindistFinderNew(near, node)
        for val in u:
            if val == -1:
                return

        jumps = bfs(self.adjrev, node.disc, self.dict)

        resultlist = self.odeSolverNew(near, t, u, jumps)
        for node in resultlist:
            tobeadded = Node(near.id, len(self.Nodes), node[1], node[0])
            for i in range(0, len(self.classifiedpoints[tobeadded.disc])):
                if self.classifiedpoints[tobeadded.disc][i].compare(tobeadded):
                    return

            if tobeadded.disc in self.extremepoints:
                for i in range(len(self.extremepoints[tobeadded.disc])):
                    self.extremepoints[
                        tobeadded.disc][i][0] = min(
                        self.extremepoints[
                            tobeadded.disc][i][0],
                        tobeadded.cont[i])
                    self.extremepoints[
                        tobeadded.disc][i][1] = max(
                        self.extremepoints[
                            tobeadded.disc][i][1],
                        tobeadded.cont[i])

            else:
                self.extremepoints[tobeadded.disc] = []
                for i in range(len(tobeadded.cont)):
                    self.extremepoints[tobeadded.disc].append(
                        [tobeadded.cont[i], tobeadded.cont[i]])

            self.pendingpoints[node[1].name].append(tobeadded)
            for transition in node[1].transitions:
                self.statetable[(node[1].name, transition.to_mode.name)][
                    0] = False

            self.Nodes.append(tobeadded)
            self.classifiedpoints[node[1]].append(tobeadded)
            self.Nodes[near.id].addChild(tobeadded.id)
        return


##########################################################################
        ## FUNCTIONS TO PROVIDE COMPATIBILITY ##
##########################################################################

def recurse(dimList, i):
    '''
        recursive function for generation of sampled
        points in a hyper-rectangle
    '''
    if i == len(dimList) - 1:
        return [[x] for x in dimList[i]]

    else:
        result_upto_now = recurse(dimList, i + 1)
        ans = []
        for m in range(0, len(dimList[i])):
            for j in range(0, len(result_upto_now)):
                ans.append([dimList[i][m]] + result_upto_now[j])

        return ans


def run(numIter, define_init_states, define_ha):
    '''
        Function to transalate nav_*.py into the
        format required for RRT
    '''
    ha = define_ha()
    init_states = define_init_states(ha)
    dims = init_states[0][1].dims
    # print "Dims", dims
    dimlist = []
    for i in range(len(dims)):
        dimlist.append([])

    for i in range(0, len(dims)):
        dimlist[i] = np.linspace(dims[i][0], dims[i][1], 5)
        dimlist[i] = list(set(dimlist[i]))

    list_of_initial_states = recurse(dimlist, 0)
    my_rrt = RRT(ha, list_of_initial_states, init_states[0][0])

    for i in range(0, numIter):
        my_rrt.extendNew()
        print i

    return my_rrt.Nodes


def plot(nodelist, filename, dim_x=1, dim_y=0, xmin=0, xmax=5, ymin=0, ymax=5):
    '''
        Used to plot the given nodes on a matplotlib plot
        '''
    plt.grid(linestyle='-', linewidth='1')
    plt.scatter(nodelist[0].cont[dim_x], nodelist[0].cont[dim_y])
    for i in range(0, len(nodelist)):
        list = nodelist[i].cont
        #plt.scatter(list[dim_x], list[dim_y]);
        print "List: " + str(list[dim_x]), str(list[dim_y])
        for j in range(0, len(nodelist[i].childList)):
            list1 = nodelist[(nodelist[i].childList[j])].cont
            #plt.scatter(list1[dim_x], list1[dim_y])
            # print "Sublist"
            plt.plot([list[dim_x], list1[dim_x]], [list[dim_y], list1[dim_y]])

    plt.axis([xmin, xmax, ymin, ymax])
    plt.savefig(filename)
    #plt.show()


# # translate(5000);
