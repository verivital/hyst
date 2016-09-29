package com.verivital.hyst.util;

/**
 * List of checks. Use Flags.<name>.ordinal() in order to index into the skip array
 * 
 * CONVERT_ flags are for checks that, upon error, print a message and run a pass Other flags
 * directly throw PreconditionException upon detecting problems
 */
public enum PreconditionsFlag
{
	// error-raising checks
	NEEDS_ONE_VARIABLE, NO_NONDETERMINISTIC_DYNAMICS, NO_URGENT, ALL_CONSTANTS_DEFINED, CONVERT_DISJUNCTIVE_INIT_FORBIDDEN,

	// conversion checks
	CONVERT_NONDETERMINISTIC_RESETS, CONVERT_INTERVAL_CONST_TO_VAR, CONVERT_TO_FLAT_AUTOMATON, CONVERT_DISJUNCTIVE_GUARDS, CONVERT_ALL_FLOWS_ASSIGNED, CONVERT_CONSTANTS_TO_VALUES,

	// only linear/basic nonlinear (sine, ^, ect.) operators allowed in init,
	// flows, resets, guards...
	// this one can fail, if the conversion is not supported
	CONVERT_BASIC_OPERATORS,
}