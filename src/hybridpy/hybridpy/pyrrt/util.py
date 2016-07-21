#from cvxopt import *
import cvxopt
#from sympy import *
#import math
import numpy as np
import re

A = []
vardictionary = {}
dictionary = {}
# Less than equal too will be given preference  i.e. Ax <= b


def parser(guard_sympy, varorder):
    '''
        Function to transform a set of symbolic equations to a matrix
    '''
    loc = []
    dictionary = {}
    j = 0
    # print varorder
    for vars in varorder:
        dictionary[vars] = j
        j = j + 1

    # print dictionary
    numeq = 0
    loc = []
    for i in range(len(dictionary)):
        loc.append([])
    # print "Temp",loc
    b = []
    for eq in guard_sympy:
        eq = eq.replace(" ", "")
        # print eq
        eq = eq.replace('-', "+-1*")
        # print eq
        eq_1 = re.split('<=', eq)
        opposite = False

        if len(eq_1) == 1:
            eq_1 = re.split('>=', eq)
            opposite = True

        b.append(float(eq_1[1]))
        if opposite:
            b[len(b) - 1] = -1.0 * b[len(b) - 1]

        eq_parts = re.split(r'[+]+', eq_1[0])
        # print "Eq parts: ", eq_parts

        currlen = 0
        for i in eq_parts:
            negative = False or opposite
            if i.startswith('-1'):
                negative = True and not opposite
                i = i.replace("-1*", "")

            i_split = i.split("*")
            # print "Alpha", i_split
            if len(i_split) == 1:
                t = i_split[0]
                i_split[0] = 1.0
                i_split.append(t)

            if i_split[1] in dictionary:
                # print dictionary[i_split[1]], float(i_split[0]);
                # print "chutiyapa", loc
                loc[dictionary[i_split[1]]].append(float(i_split[0]))
                # print "locU", loc
                currlen = len(loc[dictionary[i_split[1]]])
                if negative:
                    loc[dictionary[i_split[1]]][len(loc[dictionary[i_split[1]]]) - 1] = -1.0 * loc[
                        dictionary[i_split[1]]][len(loc[dictionary[i_split[1]]]) - 1]

            else:
                dictionary[i_split[1]] = len(loc)
                loc.append([0.0] * numeq)
                loc[dictionary[i_split[1]]].append(float(i_split[0]))
                if negative:
                    loc[dictionary[i_split[1]]][len(loc[dictionary[i_split[1]]]) - 1] = -1.0 * loc[
                        dictionary[i_split[1]]][len(loc[dictionary[i_split[1]]]) - 1]

            # print "locT", loc

        # print eq_parts

        for i in range(0, len(loc)):
            if len(loc[i]) < currlen:
                loc[i].append(0.0)

        numeq = numeq + 1

    # print "loc",loc
    tmp = [0.0] * len(loc)
    A = [tmp] * numeq
    # print len(loc), numeq

    loc = np.array(loc)
    # print "loc2", loc
    A = np.transpose(loc)
    # for i in range(0, len(loc)):x
    #   for j in range(0, numeq):
    #       A[j][i] = loc[i][j];

    # print "A", A;
    # print "b", b;
    return A, b


def ConvexOpt(pt, guard_sympy, varorder):
    '''
        Takes in the constriant matrix given by Ax <= b and the objective function given by obj and
        performs the optimisation
    '''
    matp, matq = genObj(pt)
    # .................................................... Assuming that an ad
    matg, matrh = parser(guard_sympy, varorder)
    matg = cvxopt.matrix(matg)
    matrh = cvxopt.matrix(matrh)
    matp = cvxopt.matrix(matp)
    matq = cvxopt.matrix(matq)

    # print "G", G
    # print "h", h
    cvxopt.solvers.options['show_progress'] = False
    sol = cvxopt.solvers.qp(matp, matq, matg, matrh)
    pt = np.array(pt)
    ans = np.array(sol['x'])
    ansold = 0.5 * np.dot(np.dot(np.transpose(ans), matp), ans) + \
        np.dot(np.transpose(matq), ans) + np.dot(pt, pt)
    return (ans, ansold)


def genObj(pt):
    '''
        Generates the objective function with the argument as the given point
    '''
    pt = np.array(pt)
    matp = np.identity(len(pt))
    matp = 2 * matp
    matq = -2.0 * pt

    # print "P", P;
    # print "q", q;
    return matp, matq
#parser(["2*x - 3*y <=5", "3*x + 4.5*y - 3.5*z <= 2.2", "4*x - 2*y + 4*z <= 3", "3*x - 2*y + 4.5*z >= 3"]);


# x, y = ConvexOpt([-2.0, 2.1], ["1*x + 0*y <= 2", "1*x + 0*y >= 1"])

# print x, np.sqrt(y[0][0])
