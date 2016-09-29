package com.verivital.hyst.util;

/**
 * Similar to CmdLineException, but extends from RuntimeException rather than Exception
 * 
 * @author stan
 *
 */
@SuppressWarnings("serial")
public class CmdLineRuntimeException extends RuntimeException
{
	public CmdLineRuntimeException(String string)
	{
		super(string);
	}

	public CmdLineRuntimeException(String string, Exception e)
	{
		super(string, e);
	}
}
