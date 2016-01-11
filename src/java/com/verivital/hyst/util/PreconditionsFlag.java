package com.verivital.hyst.util;

/**
 * List of checks. Use Flags.<name>.ordinal() in order to index into the skip array 
 * 
 * CONVERT_ flags are for checks that, upon error, print a message and run a pass 
 * Other flags directly throw PreconditionException upon detecting problems
 */
public enum PreconditionsFlag
{
	// error-raising checks
	NEEDS_ONE_VARIABLE,
	NO_NONDETERMINISTIC_DYNAMICS,
	NO_URGENT,
	ALL_CONSTANTS_DEFINED,
	
	// conversion checks
	CONVERT_NONDETERMINISTIC_RESETS,
	CONVERT_INTERVAL_CONSTANTS,
	CONVERT_TO_FLAT_AUTOMATON,
	CONVERT_DISJUNCTIVE_GUARDS,
	CONVERT_ALL_FLOWS_ASSIGNED, // only run if CONVERT_TO_FLAT_AUTOMATON is not skipped 
}