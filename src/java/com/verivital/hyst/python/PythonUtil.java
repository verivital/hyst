package com.verivital.hyst.python;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.util.AutomatonUtil;

/**
 * A class containing utility methods which use python.
 *
 * @author Stanley Bak May 2015
 *
 */
public class PythonUtil
{
	public static PythonEvaluatePrinter pyEvaluatePrinter = new PythonEvaluatePrinter();
	public static PythonSympyPrinter pySympyPrinter = new PythonSympyPrinter();

	/**
	 * Optimize a function in a hyper-rectangle using scipy.optimize.basinhopping
	 *
	 * This is a a parallel version the call above. It will use the number of cores available in the
	 * system.
	 *
	 * @param exp_list
	 *            a list of expression to minimize and maximize
	 * @param bounds_list
	 *            a list of interval bounds for each variable used in the expression
	 * @return an list of interval bounds on exp (the results)
	 */
	public static List<Interval> scipyOptimize(List<Expression> expList,
			List<HashMap<String, Interval>> boundsList)
	{
		PythonBridge pb = PythonBridge.getInstance();
		int size = expList.size();

		if (size != boundsList.size())
			throw new AutomatonExportException(
					"expression list and bounds list should be same size");

		// python needs explicit functions (not lambdas) for Pool.map
		String FUNC_PREFIX = "_func";
		String varList = makeVariableList(boundsList.get(0).keySet());

		for (int i = 0; i < size; ++i)
		{
			Expression e = expList.get(i);

			// make sure bounds are provided for all the variables in the
			// expression
			checkAllVariablesHaveBounds(e, boundsList.get(i));

			StringBuilder s = new StringBuilder();
			s.append("def " + FUNC_PREFIX + i + " ((" + varList + ")):\n");
			s.append("    return " + pyEvaluatePrinter.print(e) + "\n");

			String res = pb.sendWithTrailingNewline(s.toString());

			if (res.length() > 0)
				throw new AutomatonExportException(
						"Got result when defining function (didn't expect one): " + res);
		}

		StringBuilder s = new StringBuilder();
		s.append("print opt_multi([");

		for (int i = 0; i < size; ++i)
		{
			s.append("(");

			s.append(FUNC_PREFIX + i + ", ");
			s.append(toPythonIntervalList(boundsList.get(i)));
			s.append("),");
		}

		s.append("])");

		pb.send("from pythonbridge.scipy_optimize import *");
		String result = pb.send(s.toString());

		return parseIntervalListResult(result);
	}

	private static void checkAllVariablesHaveBounds(Expression e, HashMap<String, Interval> bounds)
	{
		Set<String> vars = AutomatonUtil.getVariablesInExpression(e);

		for (String var : vars)
		{
			if (!bounds.containsKey(var))
			{
				throw new AutomatonExportException("Bounds not provided for variable: " + var
						+ " in expression: " + e.toDefaultString());
			}
		}

	}

	/**
	 * Make a variable list string from a set of variables. For example: "x, y, z"
	 * 
	 * @param vars
	 *            the set of variables
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

	/**
	 * Return an expression which declares sympy variables for each symbol in a Hyst Expression
	 * 
	 * @param exp
	 *            the input expression
	 * @return the output expression, like "x,y,z=sympy.symbols('x y z ')"
	 */
	private static String makeExpressionVariableSymbols(Expression exp)
	{
		StringBuilder s = new StringBuilder();
		Collection<String> variables = AutomatonUtil.getVariablesInExpression(exp);

		appendSymbolsDeclaration(s, variables);

		return s.toString();
	}

	/**
	 * Append to the string buffer the python sympy symbols declaration
	 * 
	 * @param s
	 * @param variables
	 */
	private static void appendSymbolsDeclaration(StringBuilder s, Collection<String> variables)
	{
		if (variables.size() > 0)
		{
			String prefix = "";
			for (String var : variables)
			{
				s.append(prefix);
				prefix = ",";
				s.append(var);
			}

			s.append("=sympy.symbols('");

			for (String var : variables)
				s.append(var + " ");

			s.append("');");
		}
	}

	/**
	 * Converts a set of bounds to an interval list in python of the form: [(0,2), (4,5)]
	 * 
	 * @param bounds
	 *            the bounds for all the variables
	 * @return the constructed string
	 */
	private static String toPythonIntervalList(Map<String, Interval> bounds)
	{
		StringBuilder s = new StringBuilder();
		s.append("[");

		for (Interval i : bounds.values())
			s.append("(" + i.min + ", " + i.max + "),");

		s.append("]");

		return s.toString();
	}

	/**
	 * Converts a set of bounds to an interval map in python of the form: {'x':interval(2,4),
	 * 'y':interval(4,5)}
	 * 
	 * @param bounds
	 *            the bounds for all the variables
	 * @return the constructed string
	 */
	private static String toPythonIntervalMap(Map<String, Interval> bounds)
	{
		StringBuilder s = new StringBuilder();
		s.append("{");

		for (Entry<String, Interval> e : bounds.entrySet())
		{
			String varName = e.getKey();
			Interval i = e.getValue();

			s.append("'" + varName + "':(" + i.min + ", " + i.max + "),");
		}

		s.append("}");

		return s.toString();
	}

	private static List<Interval> parseIntervalListResult(String str)
	{
		if (!str.startsWith("[") || !str.endsWith("]"))
			throw new AutomatonExportException("Python result wasn't a list: " + str);

		String rem = str.substring(1, str.length() - 1);
		int index = 0;
		ArrayList<Interval> rv = new ArrayList<Interval>();

		while (index < rem.length())
		{
			if (rem.charAt(index) == ',')
				++index;

			if (rem.charAt(index) == ' ')
				++index;

			if (rem.charAt(index) != '[')
				throw new AutomatonExportException(
						"Malformed interval list (expected '['): " + str);
			else
				++index;

			int commaIndex = rem.indexOf(',', index);
			int rightIndex = rem.indexOf(']', index);

			String first = rem.substring(index, commaIndex);
			String second = rem.substring(commaIndex + 1, rightIndex);

			try
			{
				Interval i = new Interval(Double.parseDouble(first), Double.parseDouble(second));
				rv.add(i);
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("Error formatting python optimization result",
						e);
			}

			index = rightIndex + 1;
		}

		return rv;
	}

	/**
	 * Optimize a function in a hyper-rectangle using interval arithmetic over multiple domains.
	 *
	 * This is a more optimized call than intervalOptimize if you're planning on optimizing the same
	 * function but just changing the variable bounds.
	 *
	 * In performance tests, using a fairly simple expression (2*x + y - x) with bounds x in [0, 1],
	 * y in [-0.2, -0.1], I measured 600 interval evaluations per second (300x faster than using
	 * intervalOptimize), versus 1000 calls in native python (10x faster than single evaluations).
	 * The slowdown is primarily in Java (the python execution time through the bridge matches).
	 *
	 * @param pb
	 *            the PythonBridge interface to use
	 * @param exp
	 *            the expression to minimize and maximize
	 * @param boundsList
	 *            a list of interval bounds for each variable used in the expression
	 * @return a list of resultant interval bounds
	 */
	public static List<Interval> intervalOptimize(List<Expression> exps,
			List<HashMap<String, Interval>> boundsList)
	{
		return intervalOptimizeBounded(exps, boundsList, 0);
	}

	/**
	 * Simpliy an expression using python, chopping values to zero smaller than some tolerance
	 * 
	 * @param e
	 *            the expression to simplify
	 * @param tol
	 *            the tolerance for chopping, some small value like 1e-8
	 * @return the simplified expression
	 */
	public static Expression pythonSimplifyExpressionChop(Expression e, double tol)
	{
		// proceed in two phases, first simplify, then chop, then simplify
		e = pythonSimplifyExpression(e);

		e = chop(e, tol);

		e = pythonSimplifyExpression(e);

		return e;
	}

	/**
	 * Chop an expression (set values close to zero to zero)
	 * 
	 * @param e
	 *            the original expression
	 * @param tol
	 *            the tolerance (how close to zero should we chop), for example 1e-8
	 * @return the chopped expression (not simplified!)
	 */
	private static Expression chop(Expression e, double tol)
	{
		Expression rv = e;

		if (e instanceof Constant)
		{
			Constant c = (Constant) e;

			if (c.getVal() >= -tol && c.getVal() <= tol)
				rv = new Constant(0);
		}
		else if (e instanceof Operation)
		{
			Operation o = e.asOperation();
			Operation rvOp = new Operation(o.op);

			for (Expression c : o.children)
				rvOp.children.add(chop(c, tol));

			rv = rvOp;
		}

		return rv;
	}

	public static Expression pythonSimplifyNumber(Expression e)
	{
		PythonBridge pb = PythonBridge.getInstance();
		StringBuilder s = new StringBuilder();

		String symbols = makeExpressionVariableSymbols(e);

		if (symbols.length() > 0)
			s.append(symbols);

		s.append("from sympy import S;");
		s.append("sympy.simplify(sympy.factor(");
		s.append(pySympyPrinter.print(e));
		s.append("))");

		String result = pb.send(s.toString());

		// substitute back
		result = result.replace("**", "^");
		return FormulaParser.parseValue(result);
	}

	public static Expression pythonSimplifyRecursively(Expression e)
	{
		Expression rv = e;

		if (e instanceof Operation)
		{
			Operation o = e.asOperation();

			if (Operator.isBooleanOperator(o.op))
			{
				// split!
				Operation rvOp = new Operation(o.op);
				rv = rvOp;

				for (Expression child : o.children)
				{
					Expression newE = pythonSimplifyRecursively(child);
					rvOp.children.add(newE);
				}
			}
			else if (o.op == Operator.LOC)
				; // skip
			else
				rv = pythonSimplifyNumber(e);
		}

		return rv;
	}

	/**
	 * Use python-sympy to simplify an expression.
	 * 
	 * @param e
	 *            the input expression
	 * @return the output expression
	 */
	public static Expression pythonSimplifyExpression(Expression e)
	{
		Expression rv = e;

		// explicitly reject these, we don't want nondeterminism
		if (!PythonBridge.hasPython())
			throw new AutomatonExportException(
					"pythonSimplifyExpression called, but python was not enabled");

		// optimization: only simplify if it's an operation
		if (e instanceof Operation && AutomatonUtil.expressionContainsOnlyAllowedOps(e,
				AutomatonUtil.OPS_LINEAR, AutomatonUtil.OPS_NONLINEAR, AutomatonUtil.OPS_BOOLEAN,
				AutomatonUtil.OPS_LOC))
		{
			// split conjunctions recursively
			rv = pythonSimplifyRecursively(e);
		}

		return rv;
	}

	/**
	 * Optimize a function in a hyper-rectangle using interval arithmetic over possibly multiple
	 * domains. This uses smaller domains to have a guaranteed overapproximation error.
	 *
	 * @param pb
	 *            the PythonBridge interface to use
	 * @param expList
	 *            the expression list to minimize and maximize
	 * @param boundsList
	 *            a list of interval bounds for each variable in each expression
	 * @param maxError
	 *            the maximum error, use 0 or negative if you don't want an error bound
	 * @return a list of resultant interval bounds
	 */
	public static List<Interval> intervalOptimizeBounded(List<Expression> expList,
			List<HashMap<String, Interval>> boundsList, double maxError)
	{
		if (expList.size() != boundsList.size())
			throw new AutomatonExportException("number of expression(" + expList.size()
					+ ") and number of bounds (" + boundsList.size() + ") must match.");

		PythonBridge pb = PythonBridge.getInstance();

		StringBuilder s = new StringBuilder();

		HashSet<String> allVariables = new HashSet<String>();

		for (Expression e : expList)
			allVariables.addAll(AutomatonUtil.getVariablesInExpression(e));

		// x,y = sympy.symbols('x y')
		appendSymbolsDeclaration(s, allVariables);

		s.append("eval_eqs_bounded([");

		for (Expression e : expList)
		{
			s.append(pyEvaluatePrinter.print(e));
			s.append(",");
		}

		s.append("],[");

		for (Map<String, Interval> bounds : boundsList)
		{
			s.append(toPythonIntervalMap(bounds));
			s.append(",");
		}

		s.append("],");

		String errorStr = "None";

		if (maxError > 0)
			errorStr = "" + maxError;

		s.append(errorStr + ")");

		pb.send("from pythonbridge.interval_optimize import *");
		String result = pb.send(s.toString());

		return parseIntervalListResult(result);
	}

	/**
	 * Used for printing expressions that can be evaluated in Python (like math.sin(10))
	 * 
	 * @author Stanley Bak
	 *
	 */
	private static class PythonEvaluatePrinter extends DefaultExpressionPrinter
	{
		PythonEvaluatePrinter()
		{
			super();
			opNames.put(Operator.POW, "**");
			opNames.put(Operator.SQRT, "math.sqrt");
			opNames.put(Operator.SIN, "math.sin");
			opNames.put(Operator.COS, "math.cos");
			opNames.put(Operator.EXP, "math.exp");
			opNames.put(Operator.LN, "math.log");
			opNames.put(Operator.TAN, "math.tan");
		}
	}

	/**
	 * Used for printing sympy expressions in python
	 * 
	 * @author Stanley Bak
	 *
	 */
	private static class PythonSympyPrinter extends DefaultExpressionPrinter
	{
		PythonSympyPrinter()
		{
			super();
			opNames.put(Operator.POW, "**");
			opNames.put(Operator.SQRT, "sympy.sqrt");
			opNames.put(Operator.SIN, "sympy.sin");
			opNames.put(Operator.COS, "sympy.cos");
			opNames.put(Operator.EXP, "sympy.exp");
			opNames.put(Operator.LN, "sympy.log");
			opNames.put(Operator.TAN, "sympy.tan");
		}

		@Override
		protected String printConstantValue(double d)
		{
			return "S('" + constFormatter.format(d) + "')";
		}
	}
}
