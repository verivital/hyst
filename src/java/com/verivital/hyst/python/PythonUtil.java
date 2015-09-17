package com.verivital.hyst.python;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * A class containing utility methods which use python.
 *
 * @author Stanley Bak
 * May 2015
 *
 */
public class PythonUtil
{
	public static PythonExpressionPrinter pyPrinter = new PythonExpressionPrinter();

	/**
	 * Optimize a function in a hyper-rectangle using interval arithmetic.
	 *
	 *  In performance tests, using a fairly simple expression (2*x + y - x) with bounds x in [0, 1], y in [-0.2, -0.1],
	 *  I measured 20 calls a second. In native python this is about 100 times a second. If more performance is needed, and
	 *  you're evaluating the same equations with many different bounds, use the intervalOptimizeMulti function instead.
	 *
	 * @param pb the PythonBridge interface to use
	 * @param exp the expression to minimize and maximize
	 * @param bounds the interval bounds for each variable used in the expression
	 * @return an interval bounds on exp
	 */
	public static Interval intervalOptimize(PythonBridge pb, Expression exp, Map<String, Interval> bounds)
	{
		StringBuilder s = new StringBuilder();
		s.append(makeExpressionVariableSymbols(exp));

		s.append("print eval_eq(");
		s.append(pyPrinter.print(exp));
		s.append(",");
		s.append(toPythonIntervalMap(bounds));
		s.append(")");

		pb.send("from pythonbridge.interval_optimize import *");
		String result = pb.send(s.toString());

		return parseIntervalResult(result);
	}

	/**
	 * Optimize a function in a hyper-rectangle using scipy.optimize.basinhopping
	 *
	 *  In performance tests, using a fairly simple expression (2*x + y - x) with bounds x in [0, 1], y in [-0.2, -0.1],
	 *  I measured 55 calls a second. In native python this is also about 55 times a second, so I didn't write a multi version
	 *  of this function.
	 *
	 * @param pb the PythonBridge interface to use
	 * @param exp the expression to minimize and maximize
	 * @param bounds the interval bounds for each variable used in the expression
	 * @return an interval bounds on exp
	 */
	public static Interval scipyOptimize(PythonBridge pb, Expression exp, HashMap<String, Interval> bounds)
	{
		StringBuilder s = new StringBuilder();

		s.append("print opt(lambda (");
		s.append(makeVariableList(bounds.keySet()));
		s.append("): (");

		s.append(pyPrinter.print(exp));
		s.append("),");
		s.append(toPythonIntervalList(bounds));
		s.append(")");

		pb.send("from pythonbridge.scipy_optimize import *");
		String result = pb.send(s.toString());

		return parseIntervalResult(result);
	}

	/**
	 * Make a variable list string from a set of variables. For example:
	 * "x, y, z"
	 * @param vars the set of variables
	 * @return the constructed string
	 */
	private static String makeVariableList(Set<String> vars)
	{
		StringBuilder rv = new StringBuilder();

		for (String v : vars)
		{
			if (rv.length() > 0)
				rv.append(", ");

			rv.append(v);
		}

		return rv.toString();
	}

	private static String makeExpressionVariableSymbols(Expression exp)
	{
		StringBuilder s = new StringBuilder();
		Collection <String> variables = AutomatonUtil.getVariablesInExpression(exp);

		String prefix = "";
		for (String var : variables){
			s.append(prefix);
			prefix = ",";
			s.append(var);
		}


		s.append("=symbols('");

		for (String var : variables)
			s.append(var + " ");

		s.append("');");

		return s.toString();
	}

	/**
	 * Converts a set of bounds to an interval list in python of the form:
	 * [(0,2), (4,5)]
	 * @param bounds the bounds for all the variables
	 * @return the constructed string
	 */
	public static String toPythonIntervalList(Map<String, Interval> bounds)
	{
		StringBuilder s = new StringBuilder();
		s.append("[");

		for (Interval i : bounds.values())
			s.append("(" + i.min + ", " + i.max + "),");

		s.append("]");

		return s.toString();
	}

	/**
	 * Converts a set of bounds to an interval map in python of the form:
	 * {'x':interval(2,4), 'y':interval(4,5)}
	 * @param bounds the bounds for all the variables
	 * @return the constructed string
	 */
	public static String toPythonIntervalMap(Map<String, Interval> bounds)
	{
		StringBuilder s = new StringBuilder();
		s.append("{");

		for (Entry<String, Interval> e : bounds.entrySet())
		{
			String varName = e.getKey();
			Interval i = e.getValue();

			s.append("'" + varName + "':interval(" + i.min + ", " + i.max + "),");
		}

		s.append("}");

		return s.toString();
	}

	private static Interval parseIntervalResult(String str)
	{
		if (!str.startsWith("[") || !str.endsWith("]"))
			throw new AutomatonExportException("Malformed interval, doesn't start/end with [ and ]: " + str);

		String trimmed = str.substring(1, str.length() - 1);
		String[] parts = trimmed.split(",");

		if (parts.length != 2)
			throw new AutomatonExportException("Malformed interval, expected two parts:" + str);

		double min = 0;
		double max = 0;

		try
		{
			min = Double.parseDouble(parts[0]);
			max = Double.parseDouble(parts[1]);
		}
		catch (NumberFormatException e)
		{
			throw new AutomatonExportException("Malformed result, expected similar to [0, 1], got: " + str, e);
		}

		return new Interval(min, max);
	}

	/**
	 * Optimize a function in a hyper-rectangle using interval arithmetic over multiple domains.
	 *
	 * This is a more optimized call than intervalOptimize if you're planning on optimizing the same function but
	 * just changing the variable bounds.
	 *
	 * In performance tests, using a fairly simple expression (2*x + y - x) with bounds x in [0, 1], y in [-0.2, -0.1],
	 * I measured 600 interval evaluations per second (300x faster than using intervalOptimize), versus
	 * 1000 calls in native python (10x faster than single evaluations). The slowdown is primarily in Java
	 * (the python execution time through the bridge matches).
	 *
	 * @param pb the PythonBridge interface to use
	 * @param exp the expression to minimize and maximize
	 * @param boundsList a list of interval bounds for each variable used in the expression
	 * @return a list of resultant interval bounds
	 */
	public static List<Interval> intervalOptimizeMulti(PythonBridge pb, Expression exp,
			List<Map<String, Interval>> boundsList)
	{
		ArrayList <Interval> rv = new ArrayList <Interval>(boundsList.size());

		StringBuilder s = new StringBuilder();
		s.append(makeExpressionVariableSymbols(exp));

		s.append("[str(v) for v in eval_eq_multi(");
		s.append(pyPrinter.print(exp));
		s.append(",[");

		for (Map<String, Interval> bounds : boundsList)
		{
			s.append(toPythonIntervalMap(bounds));
			s.append(",");
		}

		s.append("])]");

		pb.send("from pythonbridge.interval_optimize import *");
		String result = pb.send(s.toString());

		if (!result.startsWith("[") || !result.endsWith("]"))
			throw new AutomatonExportException("Malformed result, doesn't start/end with [ and ]: " + result);

		result = result.substring(1, result.length() - 1);

		int curIndex = 0;

		// reconstruct the intervals
		for (int i = 0; i < boundsList.size(); ++i)
		{
			int first = result.indexOf("'", curIndex);
			int second = result.indexOf("'", first+1);

			if (first == -1 || second == -1)
				throw new AutomatonExportException("Malformed result, not enough intervals returned: " + result);

			String part = result.substring(first+1, second);
			rv.add(parseIntervalResult(part));

			curIndex = second + 1;
		}

		return rv;
	}

	/**
	 * Optimize a function in a hyper-rectangle using interval arithmetic over multiple domains. This
	 * uses smaller domains to get a better result, using a user specified maximum domain width.
	 *
	 * @param pb the PythonBridge interface to use
	 * @param exp the expression to minimize and maximize
	 * @param boundsList a list of interval bounds for each variable used in the expression
	 * @param maxSize the maximum width of a domain, in any dimension
	 * @return a list of resultant interval bounds
	 */
	public static List<Interval> intervalOptimizeMulti_bb(PythonBridge pb, Expression exp,
			List<Map<String, Interval>> boundsList, double maxSize)
	{
		ArrayList <Interval> rv = new ArrayList <Interval>(boundsList.size());

		StringBuilder s = new StringBuilder();
		s.append(makeExpressionVariableSymbols(exp));

		s.append("[str(v) for v in eval_eq_multi_branch_bound(");
		s.append(pyPrinter.print(exp));
		s.append(",[");

		for (Map<String, Interval> bounds : boundsList)
		{
			s.append(toPythonIntervalMap(bounds));
			s.append(",");
		}

		s.append("]," + maxSize + ")]");

		pb.send("from pythonbridge.interval_optimize import *");
		String result = pb.send(s.toString());

		if (!result.startsWith("[") || !result.endsWith("]"))
			throw new AutomatonExportException("Malformed result, doesn't start/end with [ and ]: " + result);

		result = result.substring(1, result.length() - 1);

		int curIndex = 0;

		// reconstruct the intervals
		for (int i = 0; i < boundsList.size(); ++i)
		{
			int first = result.indexOf("'", curIndex);
			int second = result.indexOf("'", first+1);

			if (first == -1 || second == -1)
				throw new AutomatonExportException("Malformed result, not enough intervals returned: " + result);

			String part = result.substring(first+1, second);
			rv.add(parseIntervalResult(part));

			curIndex = second + 1;
		}

		return rv;
	}

	private static class PythonExpressionPrinter extends DefaultExpressionPrinter
	{
		PythonExpressionPrinter()
		{
			super();
			opNames.put(Operator.POW, "**");
		}
	}
}
