package com.verivital.hyst.passes.complex.hybridize;

/**
 * This is the hybridize mixed-triggered pass from the HSCC 2016 paper,
 * "Scalable Static Hybridization Methods for Analysis of Nonlinear Systems" by Bak et. al
 * 
 * This is the 'raw' version of the transformation, which in the paper is described in 
 * Section 5. The paremeters in the paper are:
 * 
 * a list of splitting elements E_1, ... E_{n-1}, where each
 * element E_i is either a real number to be used for time-triggered
 * splitting, or a PI function to be used for space-triggered
 * splitting (list 1),
 *
 * D_1, ... D_n are the contraction domains (sets) for each
 * new location (list 2), and
 *
 * g_1, ... g_n$ are the dynamics abstraction functions for
 * each location (list 3).
 *
 * In this pass, the g's are automatically computed based on global optimization within
 * the contraction domains and a linear approximation (based on sampling near the center of
 * each contraction domain). Thus, only two lists are paramters of this pass.
 * 
 * Elements of list 1 are single numbers, or comma-separated hyperpoints. The PI-function is
 * taken by considering the hyperplane at the given-points, in the direction of the gradient.
 * For example: '0.1 0.1 0.25,0 0.2'
 * 
 * Elements of list 2 are hyperrectangles, which use commas to separate min and max, and 
 * semicolons to separate dimensions.
 * For example: '0.1,0.2;2.1,2.2 0.2,0.3;2.2,2.3'
 * 
 * In addition to these parameters from the paper, the optimization method can be chosen:
 * 
 * opt the optimization method, one of {basinhopping, interval}
 *
 * @author Stanley Bak
 *
 */
public class HybridizeMTPass
{

}
