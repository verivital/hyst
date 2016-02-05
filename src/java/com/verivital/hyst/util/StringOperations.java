package com.verivital.hyst.util;

import com.verivital.hyst.printers.ToolPrinter;

/**
 * Utility class for string operations
 * @author stan
 *
 */
public class StringOperations
{
	// String.join was added in Java 1.8
	public static String join(String sep, String[] list)
	{
		StringBuilder rv = new StringBuilder();

		for (String s : list)
		{
			if (rv.length() > 0)
				rv.append(sep);
				
			rv.append(s);
		}

		return rv.toString();
	}
	
	public static String join(String sep, int[] list)
	{
		String[] strings = new String[list.length];
		
		for (int i = 0; i < list.length; ++i)
			strings[i] = Integer.toString(list[i]);
		
		return join(sep, strings);
	}
	
	public static String join(String sep, double[] list)
	{
		String[] strings = new String[list.length];
		
		for (int i = 0; i < list.length; ++i)
			strings[i] = ToolPrinter.doubleToString(list[i]);
		
		return join(sep, strings);
	}
	
	public static String join(String sep, Double[] list)
	{
		String[] strings = new String[list.length];
		
		for (int i = 0; i < list.length; ++i)
			strings[i] = ToolPrinter.doubleToString(list[i]);
		
		return join(sep, strings);
	}
}
