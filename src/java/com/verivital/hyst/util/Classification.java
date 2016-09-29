/**
 *
 */
package com.verivital.hyst.util;

import static com.verivital.hyst.util.AutomatonUtil.simplifyExpression;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;

/**
 * classify components
 *
 *
 *
 */
public class Classification
{

	/**
	 * type of automaton
	 *
	 * HACK: we are also going to use these to classify locations for now as well, e.g., a given
	 * automaton could have a location that has linear dynamics, and another that has nonlinear; we
	 * may want to use some analysis methods in different locations based on this
	 *
	 * TODO: this is probably going to be simpler if we use OO features to set up a hierarchy of
	 * automaton types, as e.g., hybrid, nonlinear, nondeterministic is probably the most general
	 * class
	 *
	 * @author tjohnson
	 *
	 *
	 */
	public enum AutomatonType
	{

		DISCRETE_FINITE, // no flows or all flows 0, and no variables,
							// equivalent to a finite state machine
		DISCRETE_EXTENDED, // no flows or all flows 0, possibly with variables
							// (e.g., could have real variables with zero
							// derivatives), roughly equivalent to extended
							// finite state machine
		CONTINUOUS_TIMED, // 1 location, \dot{x} = 1 for all variables x \in Var
		CONTINUOUS_RECTANGULAR, // 1 location, \dot{x} \in [a,b] for all
								// variables x \in Var
		CONTINUOUS_LINEAR, // 1 location, \dot{x} = Ax for all variables x \in
							// Var
		CONTINUOUS_AFFINE, // 1 location, \dot{x} = Ax + b for all variables x
							// \in Var
		CONTINUOUS_NONLINEAR, // 1 location, \dot{x} = f(x) for all variables x
								// \in Var
		// TODO: add classification for almost-continuous: same dynamics in
		// every mode, only identity resets
		TIMED, // multiple locations, \dot{x} = 1
		TIMED_MULTIRATE, // multiple locations, different locations may have
							// \dot{x} = a or \dot{x} = b, etc.
		HYBRID, HYBRID_RECTANGULAR, HYBRID_INITIALIZED_RECTANGULAR, HYBRID_LINEAR, HYBRID_AFFINE, HYBRID_MULTIAFFINE, HYBRID_NONLINEAR_POLYNOMIAL, // general
																																					// polynomials
		HYBRID_NONLINEAR_SPECIAL, // special functions such as transcendentals
									// and trig functions like sin, cos, exp,
									// ln, etc.
		// TODO: variable structure systems, piecewise linear / affine systems,
		// etc. It would actually be interesting to have a survey paper of all
		// the types of automata...
		// TODO: stochastic ones, etc.
	}

	public static LinkedHashMap<String, Integer> varID;
	public static double[][] linearMatrix;
	public static BaseComponent ha;

	/**
	 * Classify whether an automaton is deterministic or not
	 *
	 * @author tjohnson
	 *
	 */
	public enum AutomatonDeterminism
	{

		DETERMINISTIC, NONDETERMINISTIC,
		// TODO: further classifications into sources of nondeterminism
	}

	/**
	 *
	 * @param n
	 * @return
	 */
	/*
	 * // TODO: can add something like this later, will for now just iterate over network in matlab
	 * public static List<AutomatonType> classifyNetwork(NetworkComponent n) { for (Entry<String,
	 * ComponentInstance> c : n.children.entrySet()) {
	 * 
	 * } }
	 */
	public static AutomatonType classifyAutomaton(BaseComponent c)
	{
		if (c.variables.size() == 0)
		{
			return AutomatonType.DISCRETE_FINITE;
		}
		else
		{
			boolean allNull = true;

			for (Entry<String, AutomatonMode> m : c.modes.entrySet())
			{
				for (String v : c.variables)
				{
					if (m.getValue().flowDynamics.containsKey(v))
					{
						if (m.getValue().flowDynamics.get(v) != null)
						{
							ExpressionInterval ei = m.getValue().flowDynamics.get(v);
							if (!ei.getExpression().equals(new Constant(0))
									|| ei.getInterval() != null)
							{
								allNull = false;
								break;
							}
						}
					}
				}
			}

			if (allNull)
			{
				return AutomatonType.DISCRETE_FINITE;
			}
			else
			{
				return AutomatonType.HYBRID;
			}
		}
	}

	public static AutomatonType classifyFlow(ExpressionInterval ei)
	{

		AutomatonType rv = null;
		Expression e = ei.getExpression();

		if (e instanceof Operation)
		{
			Operation o = (Operation) e;
			Expression l = o.getLeft();
			Expression r = o.getRight();

			/*
			 * if (l instanceof Variable && r instanceof Constant) { rv = AutomatonType.LINEAR; }
			 * 
			 * else if (l instanceof Variable && r instanceof Variable) { if (o.op == Operator.ADD |
			 * o.op == Operator.SUBTRACT) { rv = AutomatonType.LINEAR; } else rv =
			 * AutomatonType.NONLINEAR;
			 * 
			 * }
			 */
		}

		return rv;
	}

	public static boolean isLinearDynamics(LinkedHashMap<String, ExpressionInterval> flowDynamics)
	{
		boolean rv = true;

		for (ExpressionInterval e : flowDynamics.values())
		{
			if (!isLinearExpression(e.getExpression()))
			{
				rv = false;
				break;
			}
		}

		return rv;
	}

	public static boolean isLinearExpression(Expression e)
	{
		boolean rv = true;

		Operation o = e.asOperation();

		if (o != null)
		{
			if (o.op == Operator.MULTIPLY)
			{
				int numVars = 0;

				for (Expression c : o.children)
				{
					int count = countVariablesMultNeg(c);

					if (count != Integer.MAX_VALUE)
					{
						numVars += count;
					}
					else
					{
						rv = false;
						break;
					}
				}

				if (numVars > 1)
				{
					rv = false;
				}
			}
			else if (o.op == Operator.ADD || o.op == Operator.SUBTRACT)
			{
				for (Expression c : o.children)
				{
					if (!isLinearExpression(c))
					{
						rv = false;
						break;
					}
				}
			}
			else if (o.op == Operator.NEGATIVE)
			{
				rv = isLinearExpression(o.children.get(0));
			}
			else
			{
				rv = false;
			}
		}

		return rv;
	}

	/**
	 * Recursively count the number of variables. only recurse if we have multiplication, or
	 * negation, otherwise return Integer.MAX_VALUE
	 *
	 * @param e
	 *            the expression
	 * @return the number of variables
	 */
	private static int countVariablesMultNeg(Expression e)
	{
		int rv = 0;
		Operation o = e.asOperation();

		if (o != null)
		{
			if (o.op == Operator.MULTIPLY || o.op == Operator.NEGATIVE)
			{
				for (Expression c : o.children)
				{
					int count = countVariablesMultNeg(c);

					if (count == Integer.MAX_VALUE)
					{
						rv = Integer.MAX_VALUE;
					}
					else
					{
						rv += count;
					}
				}
			}
			else
			{
				rv = Integer.MAX_VALUE;
			}
		}
		else if (e instanceof Variable)
		{
			rv = 1;
		}

		return rv;
	}

	/**
	 * merge all variables and constants
	 */
	public void setVarID(BaseComponent ha)
	{
		varID = new LinkedHashMap<String, Integer>();
		int id = 0;
		for (String v : ha.variables)
		{
			varID.put(v, id++);
		}
		for (String c : ha.constants.keySet())
		{
			varID.put(c, id++);
		}
	}

	/**
	 * set general A matrix for each mode
	 */
	public void setLinearMatrix(AutomatonMode m)
	{
		if (!isLinearDynamics(m.flowDynamics))
		{
			throw new AutomatonValidationException("this is not a linear automaton");
		}
		else
		{
			int size = varID.size();
			int i = 0;
			linearMatrix = new double[ha.variables.size()][size];
			for (ExpressionInterval ei : m.flowDynamics.values())
			{
				Expression e = simplifyExpression(ei.getExpression());
				findCoefficient(i, e);
				i++;
			}
		}
	}

	/**
	 * find coefficients for all variables and constants of the linear expressions
	 */
	private void findCoefficient(int i, Expression e)
	{

		if (e instanceof Variable)
		{
			if (e.getParent() != null)
			{
				if (e.getParent().op == Operator.SUBTRACT)
				{
					linearMatrix[i][varID.get(e.toDefaultString())] = -1;
				}
			}
			else
			{
				linearMatrix[i][varID.get(e.toDefaultString())] = 1;
			}
		}
		else if (e instanceof Operation)
		{
			Operation o = (Operation) e;

			if (o.op == Operator.NEGATIVE)
			{
				Expression child = o.children.get(0);
				linearMatrix[i][varID.get(child.toDefaultString())] = -1;
			}
			if (o.op == Operator.MULTIPLY)
			{
				Expression l = o.getLeft();
				Expression r = o.getRight();
				if (r instanceof Variable && l instanceof Constant)
				{
					linearMatrix[i][varID.get(r.toDefaultString())] = ((Constant) l).getVal();
					if (o.getParent() != null)
					{
						if (o.getParent().op == Operator.SUBTRACT
								&& o.getParent().getRight().equals(o))
						{
							linearMatrix[i][varID.get(r.toDefaultString())] = -linearMatrix[i][varID
									.get(r.toDefaultString())];
						}
					}
				}
				else if (l instanceof Variable && r instanceof Constant)
				{
					linearMatrix[i][varID.get(l.toDefaultString())] = ((Constant) r).getVal();
					if (o.getParent() != null)
					{
						if (o.getParent().op == Operator.SUBTRACT
								&& o.getParent().getRight().equals(o))
						{
							linearMatrix[i][varID.get(l.toDefaultString())] = -linearMatrix[i][varID
									.get(l.toDefaultString())];
						}
					}
				}
			}
			else if (o.op == Operator.ADD || o.op == Operator.SUBTRACT)
			{
				if (o.getRight() instanceof Variable)
				{
					linearMatrix[i][varID.get(o.getRight().toDefaultString())] = 1;
				}
				if (o.getLeft() instanceof Variable)
				{
					linearMatrix[i][varID.get(o.getLeft().toDefaultString())] = 1;
				}
				if (o.getRight() instanceof Operation || o.getLeft() instanceof Operation)
				{
					findCoefficient(i, o.getRight());
					findCoefficient(i, o.getLeft());
				}
			}
		}
	}

}
