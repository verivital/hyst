package com.verivital.hyst.passes.complex.hybridize;

/**
 * This is the hybridize mixed-triggered pass from the HSCC 2016 paper,
 * "Scalable Static Hybridization Methods for Analysis of Nonlinear Systems" by Bak et. al
 * 
 * This is the simulation version of the transformation, which based on simulations calls
 * the raw version, HybridizeMTPass
 * 
 * In the paper, this pass is described in Section 5.1 
 * The parameters in the paper are:
 * 
 * T is the maximum time,
 * 
 * S a simulation strategy, one of {point, star, star-corners}
 * 
 * delta_tt is the simulation time in a time-triggered transformation step,
 * 
 * n_pi is the number of space-triggered transformation steps to use,
 * 
 * delta_pi is the maximum simulation time when performing a space-triggered 
 * transformation step,
 * 
 * epsilon is a bloating term to account for the difference between the simulated points 
 * the set of reachable states.
 *          
 * In addition to these parameters from the paper, the optimization method can be chosen:
 * 
 * opt the optimization method, one of {basinhopping, interval}
 *
 * @author Stanley Bak
 *
 */
public class HybridizeMTSimPass
{

}
