package com.verivital.hyst.ir;

@SuppressWarnings("serial")
public class AutomatonExportException extends RuntimeException
{

	public AutomatonExportException(String string)
	{
		super(string);
	}

	public AutomatonExportException(String string, Exception e)
	{
		super(string, e);
	}

}
