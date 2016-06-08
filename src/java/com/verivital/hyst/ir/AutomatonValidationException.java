package com.verivital.hyst.ir;

/**
 * An AutomatonValidationException represents an error internal to the tool, thus they should never
 * happen. Assumptions are made about the internal HybridAutomaton representation that are checked
 * by the validate() functions, which are called after model parsing and each model transformation
 * pass. If these assumptions do not hold (due to internal programming errors), an
 * AutomatonValidationException is thrown.
 * 
 * @author Stanley Bak
 */
@SuppressWarnings("serial")
public class AutomatonValidationException extends RuntimeException
{
	public AutomatonValidationException(String string)
	{
		super(string);
	}

	public AutomatonValidationException(String string, RuntimeException e)
	{
		super(string, e);
	}

}
