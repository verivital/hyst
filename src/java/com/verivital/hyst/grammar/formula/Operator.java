/**
 * 
 */
package com.verivital.hyst.grammar.formula;

import java.util.Arrays;
import java.util.List;

/**
 * An operator for an Operation
 * 
 * based on Christopher Dillo (dilloc@informatik.uni-freiburg.de)'s code
 *
 */
public enum Operator
{
	ADD, SUBTRACT, MULTIPLY, DIVIDE, POW, // two children
	AND, OR, EQUAL, LESS, GREATER, LESSEQUAL, GREATEREQUAL, NOTEQUAL, // two
																		// children
	NEGATIVE, LOGICAL_NOT, // one child
	SIN, COS, TAN, EXP, SQRT, LN, // one child
	LOC // loc() function in initial/forbidden modes, these are removed after
		// model is parsed
	;

	/**
	 * Higher values = higher parse priority
	 * 
	 * @param operator
	 * @return an integer priority rating
	 */
	public static int getPriority(Operator operator)
	{
		// lowest priority first
		final Operator[][] order = { { OR }, { AND },
				{ EQUAL, NOTEQUAL, LESS, LESSEQUAL, GREATER, GREATEREQUAL }, { ADD, SUBTRACT },
				{ MULTIPLY, DIVIDE }, { POW }, { TAN, SQRT, SIN, COS, EXP, LN, LOC }, };

		int rv = -1;

		for (int i = 0; i < order.length && rv == -1; ++i)
		{
			for (int j = 0; j < order[i].length; ++j)
			{
				if (order[i][j] == operator)
				{
					rv = i;
					break;
				}
			}
		}

		if (rv == -1)
			throw new RuntimeException("Operator not found: " + operator);

		return rv;
	}

	static final List<Operator> COMPARE_OPS = Arrays.asList(EQUAL, LESS, GREATER, LESSEQUAL,
			GREATEREQUAL, NOTEQUAL);

	public static boolean isComparison(Operator o)
	{
		return COMPARE_OPS.contains(o);
	}

	static final List<Operator> COMMUTATIVE_OPS = Arrays.asList(ADD, MULTIPLY, AND, OR, EQUAL,
			NOTEQUAL);

	public static boolean isCommutative(Operator o)
	{
		return COMMUTATIVE_OPS.contains(o);
	}

	static final List<Operator> BOOLEAN_OPS = Arrays.asList(AND, OR, LOGICAL_NOT, EQUAL, LESS,
			GREATER, LESSEQUAL, GREATEREQUAL, NOTEQUAL);

	public static boolean isBooleanOperator(Operator o)
	{
		return BOOLEAN_OPS.contains(o);
	}

	@Override
	public String toString()
	{
		throw new RuntimeException(
				"Operators should be printed through ExpressionPrinter.printOperator, never directly.");
	}

	/**
	 * Convert to a string using the default printer
	 * 
	 * @return
	 */
	public String toDefaultString()
	{
		return DefaultExpressionPrinter.instance.printOperator(this);
	}
}
