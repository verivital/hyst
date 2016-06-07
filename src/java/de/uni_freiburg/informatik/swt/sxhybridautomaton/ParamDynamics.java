package de.uni_freiburg.informatik.swt.sxhybridautomaton;

/**
 * Defines the dynamics of a Param's value. CONST values do not change while
 * EXPLICIT values have to be assigned in every Transition. ANY values can, but
 * do not have to be assigned.
 *
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 * 
 */
public enum ParamDynamics
{
	CONST, ANY, EXPLICIT
}