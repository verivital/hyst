package com.verivital.hyst.printers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.matlab.MatlabBridge;
import com.verivital.hyst.util.Classification;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;

/**
 * Printer for Simulink/Stateflow models with non-semantics preservation and semantics preservation
 * modes
 * 
 * Semantics preservation uses randomness to approximate nondeterministic behavior allowed in hybrid
 * automata
 * 
 * Non-semantics preserving is a basic translation, and in practice, often does preserve semantics
 * (so the name is poor), under the assumption that the original automata is deterministic,
 * non-Zeno, and some other assumptions that need to be clarified by Luan/Christian.
 * 
 * @author Christian Schilling, Luan Nguyen
 */
public class SimulinkStateflowPrinter extends ToolPrinter
{

	@Option(name = "-semantics", usage = "Use semantics preserving printer? 0 = no transformation, 1 = add epsilons", metaVar = "0/1")
	public String semantics = "0";

	// ------------- normal instance fields -------------

	// basecomponent
	public BaseComponent ha;

	// is semantic preservation
	public boolean isSP = true;

	// expression printer
	private final SimulinkStateflowExpressionPrinter m_printer;

	// global option
	private final boolean IS_ADD_EPS;

	// random counter (how many random values are needed at one time?)
	private int m_randoms;
	private String coeff = "0";
	private boolean found = false;
	public Classification cls = new Classification();

	// ------------- variable wrappers -------------

	// stopping signal
	public final VariableWrapper V_stop = new VariableWrapper("a_stop");
	// current time
	public final VariableWrapper V_t = new VariableWrapper("a_t");
	// backtracking information for reading correct data
	public final VariableWrapper V_backtrack = new VariableWrapper("a_backtrack");
	// minimum dwelling time
	public final VariableWrapper V_T = new VariableWrapper("a_T");
	// time the invariant was first violated
	public final VariableWrapper V_tFail = new VariableWrapper("a_tFail");
	// temporary time (for backtracking)
	public final VariableWrapper V_tTmp = new VariableWrapper("at_t");
	// output time
	public final VariableWrapper V_tOut = new VariableWrapper("ao_t");
	// maximum simulation time
	public final VariableWrapper V_maxT = new VariableWrapper("a_MAX_T");
	// transition array
	public final VariableWrapper V_transArray = new VariableWrapper("a_transA");
	// current transition array index
	public final VariableWrapper V_transIdx = new VariableWrapper("a_transIdx");
	// current transition
	public final VariableWrapper V_trans = new VariableWrapper("a_trans");
	// current location
	public final VariableWrapper V_loc = new VariableWrapper("a_loc");
	// current number of resets
	public final VariableWrapper V_resets = new VariableWrapper("a_resets");
	// maximum number of resets
	public final VariableWrapper V_maxResets = new VariableWrapper("a_RESETS");
	// random number
	public final VariableWrapper V_rand = new VariableWrapper("a_rand");
	// random array
	public final VariableWrapper V_randArray = new VariableWrapper("a_randA");
	// initial state index
	public final VariableWrapper V_init = new VariableWrapper("a_init");
	// epsilon for equality handling
	public final VariableWrapper V_eps = new VariableWrapper("a_EPS");
	// random seed (NOTE: special variable not occurring in the Stateflow chart)
	public final VariableWrapper V_seed = new VariableWrapper("a_SEED");

	// --- strings which are only non-null with certain constructor ---

	// common label part for entry states
	private final String STATE_IN_PART;
	private final String TRANS_CHOOSEJ2DWELL_STRING;
	// constant strings
	private final String STATE_CHOOSE_STRING;
	private final String STATE_DWELL_STRING;
	private final String TRANS_DWELL2LEAVE_STRING;
	private final String TRANS_LEAVE2DWELL_STRING;
	private final String TRANS_CHOOSES2DWELL_STRING;
	private final String TRANS_CHOOSEJ2CHOOSES_STRING;
	private final String TRANS_CHOOSES2CHOOSEJ_STRING;
	private final String TRANS_IN2DWELL_STRING;
	private final String TRANS_BACKTRACK2DWELL_STRING;
	private final String TRANS_LEAVE2TRANS_STRING;

	// ------------- normal static fields -------------

	/*
	 * NOTE: this is a bug in Stateflow: it REQUIRES it seems newlines to always be Unix-style,
	 * regardless of Host operating system, so e.g., using, System.lineSeparator() does not work
	 */
	public static final String lineSeparator = "\n";

	// temporary variables prefix (to imitate concurrent assignments)
	public String PREFIX_ASSIGNMENT = "t_";
	// general variables prefix (to avoid name clashes)
	public String PREFIX_VARIABLE = "v_";
	// output variables prefix
	public String PREFIX_OUTPUT = "o_";

	/**
	 * 
	 * 
	 */

	protected void printDocument(String originalFilename)
	{

		printCommentBlock(Hyst.TOOL_NAME + "\n" + "Hybrid Automaton in " + Hyst.TOOL_NAME + "\n"
				+ "Converted from file: " + originalFilename + "\n" + "Command Line arguments: "
				+ Hyst.programArguments);

		Expression.expressionPrinter = new SimulinkStateflowPrinter.SimulinkStateflowExpressionPrinter(
				0); // TODO:
		// move
		// to
		// constructor?

		// begin printing the actual program
		// printNewline();
		try
		{
			printProcedure(originalFilename); // semantics decision is made via
												// toolparams
		}
		catch (Exception ex)
		{

		}
	}

	/**
	 * Calling Matlab from java to
	 */
	public void printProcedure(String originalFilename)
	{
		File f = new File(originalFilename);

		String example_name = f.getName().substring(0, f.getName().lastIndexOf('.')); // strip
																						// extension
		String cmd_string = "SpaceExToStateflow('..\\" + f.getParent() + "\\" + example_name
				+ ".xml'";
		if (semantics.equals("1"))
		{
			System.out.println(
					"Translating with semantics preserving mode with randomness," + semantics);
			cmd_string += ", '-s')";
		}
		else
		{
			System.out.println(
					"Translating with best gusses via non-semantics preserving mode, " + semantics);
			cmd_string += ")";
		}
		System.out.println(cmd_string);

		MatlabProxy proxy = null;
		try
		{
			proxy = MatlabBridge.getInstance().getProxy();

			proxy.eval("[path_parent,path_current] = fileparts(pwd)");

			// TODO: LUAN, please resolve, you should not have pass dependent
			// stuff in the general printer.
			proxy.eval("if strcmp(path_current, 'pass_order_reduction') cd ../../; end");

			proxy.eval("[path_parent,path_current] = fileparts(pwd)");
			proxy.eval("if ~strcmp(path_current, 'matlab') cd ./matlab; end"); // NOTE:
																				// if
																				// the
																				// directory
																				// structure
																				// changes,
																				// this
																				// could
																				// break

			proxy.eval(cmd_string);

			// TODO: figure out how to get the .mdl file contents as a string so
			// we
			// can pipe its output to stdout

			proxy.disconnect();
		}
		catch (MatlabInvocationException e)
		{
			System.err.println(e);
		}
		catch (MatlabConnectionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			// What is the right pattern to handle this cleanup?
			proxy.disconnect();
		}
	}

	/**
	 * return the size of A matrix depending on a set of variable X
	 */
	public int getAMatrixSize(AutomatonMode m)
	{
		Integer size = m.flowDynamics.keySet().size();
		for (String v : ha.variables)
		{
			if (m.flowDynamics.containsKey(v)
					&& m.flowDynamics.get(v).asExpression().equals(new Constant(0)))
			{
				size = size - 1;
			}
		}
		if (m.flowDynamics.keySet().contains("t"))
		{
			size = size - 1;
		}
		return size;
	}

	/**
	 * 
	 * 
	 * 
	 * @return the dynamic matrix A of the set of variable X
	 */
	public String convertFlowToAMatrix(AutomatonMode m)
	{
		String rv = "";
		// Classification cls = new Classification();
		double[][] linearMatrix = cls.linearMatrix;
		// Integer size = ha.variables.size();
		Integer size = getAMatrixSize(m);
		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < size; j++)
			{
				rv = rv + Double.toString(linearMatrix[j][i]) + " ";
			}
			rv = rv + ";";
		}
		rv = "[" + rv + "]";
		return rv;
	}

	/**
	 * 
	 * /**
	 * 
	 * @return non zero input matrix B for each location
	 */
	public String convertInputToBMatrix(AutomatonMode m)
	{
		String rv = "";
		// Classification cls = new Classification();
		double[][] linearMatrix = cls.linearMatrix;
		Integer rowLength = getAMatrixSize(m);
		boolean allzero = true;
		String tmp = "";
		Integer colLength = ha.variables.size() + ha.constants.size();
		if (colLength > ha.variables.size())
		{
			for (int j = ha.variables.size(); j < colLength; j++)
			{
				for (int i = 0; i < rowLength; i++)
				{
					tmp = tmp + Double.toString(linearMatrix[i][j]) + " ";
					if (linearMatrix[i][j] != 0)
						allzero = false;
				}
				if (!allzero)
					rv = rv + tmp + ";";
				allzero = true;
				tmp = "";
			}
		}
		rv = "[" + rv + "]";
		return rv;
	}

	/**
	 * 
	 * @return invariant matrix C for each location
	 */
	public String convertInvToMatrix(AutomatonMode m)
	{
		String rv = "";
		// Classification cls = new Classification();
		LinkedHashMap<String, Integer> varID = cls.varID;
		Expression eInv = m.invariant;
		for (String v : ha.variables)
		{
			// skip all variables with non-null dynamics
			// outputs (y = Cx) are those with non defined (havoc) dynamics

			if (!(m.flowDynamics.containsKey(v))
					|| m.flowDynamics.get(v).asExpression().equals(new Constant(0)))
			{

				Expression subEquality = getSubEquality(v, eInv, null); // todo:
																		// did
																		// not
																		// test
																		// much,
																		// probably
																		// pretty
																		// buggy
				for (String s : ha.variables)
				{
					if (varID.get(s) < getAMatrixSize(m))
					{
						findInvCoefficient(s, subEquality.asOperation().getRight());
						rv = rv + coeff + " ";
						found = false;
						coeff = "0";
					}
				}
				rv = rv + ";";
				// TODO: really need to have objects for the vectors and
				// matrices around, we can find this coefficient of 1
				// but now how do we know where to set it...?
			}

		}
		rv = "[" + rv + "]";

		return rv;
	}

	private Expression getSubEquality(String v, Expression e, Expression s)
	{
		if (e instanceof Variable)
		{
			if (e.toString().equals(v))
			{
				return e.getParent();
			}
			else
			{
				return null;
			}
		}
		else if (e instanceof Constant)
		{
			return s;
		}
		else if (e instanceof Operation)
		{
			s = getSubEquality(v, e.asOperation().getLeft(), s);
			if (s != null)
			{
				return s;
			}
			s = getSubEquality(v, e.asOperation().getRight(), s);
			if (s != null)
			{
				return s;
			}
		}
		return null;
	}

	private void findInvCoefficient(String v, Expression e)
	{

		if (!found)
		{
			if (e instanceof Variable)
			{
				if (e.toString().equals(v))
				{
					coeff = "1";
				}
			}
			else if (e instanceof Constant)
			{
				coeff = "0";
			}
			else if (e instanceof Operation)
			{

				Operation o = (Operation) e;
				if (o.op == Operator.MULTIPLY)
				{
					Expression l = o.getLeft();
					Expression r = o.getRight();
					if (r instanceof Variable && l instanceof Constant)
					{
						if (r.toString().equals(v))
						{
							coeff = Double.toString(((Constant) l).getVal());
							if (o.getParent() != null)
							{
								if (o.getParent().op == Operator.SUBTRACT
										&& o.getParent().getRight().equals(o))
									coeff = "-" + coeff;
							}
							found = true;
						}
					}
					else if (l instanceof Variable && r instanceof Constant)
					{
						if (l.toString().equals(v))
						{
							coeff = Double.toString(((Constant) r).getVal());
							found = true;
						}
					}
				}
				else if (o.op == Operator.ADD || o.op == Operator.SUBTRACT)
				{

					if (o.getRight() instanceof Variable || o.getLeft() instanceof Variable)
					{
						if (o.getRight().toString().equals(v) || o.getLeft().toString().equals(v))
						{
							coeff = "1";
							found = true;
						}
					}
					if (o.getRight() instanceof Operation || o.getLeft() instanceof Operation)
					{
						findInvCoefficient(v, o.getRight());
						findInvCoefficient(v, o.getLeft());
					}
				}
			}
		}
	}

	/**
	 * 
	 * @return variables lower bounds
	 */
	public String parseInitialLowerBound(AutomatonMode m)
	{
		String rv = "";
		LinkedHashMap<String, Integer> varID = cls.varID;
		for (Expression ex : config.init.values())
		{
			TreeMap<String, Interval> ranges = getBound(ex);
			for (String s : ha.variables)
			{
				if (varID.get(s) < getAMatrixSize(m))
				{
					for (Entry<String, Interval> e : ranges.entrySet())
					{
						if (e.getKey().equals(s))
							rv = rv + Double.toString(e.getValue().min) + " ";
					}
				}
			}
			rv = rv + ";";
		}
		rv = "[" + rv + "]";
		return rv;
	}

	/**
	 * 
	 * @return variables upper bounds
	 */
	public String parseInitialUpperBound(AutomatonMode m)
	{
		String rv = "";
		LinkedHashMap<String, Integer> varID = cls.varID;
		for (Expression ex : config.init.values())
		{
			TreeMap<String, Interval> ranges = getBound(ex);
			for (String s : ha.variables)
			{
				if (varID.get(s) < getAMatrixSize(m))
				{
					for (Entry<String, Interval> e : ranges.entrySet())
					{
						if (e.getKey().equals(s))
							rv = rv + Double.toString(e.getValue().max) + " ";
					}
				}
			}
			rv = rv + ";";
		}
		rv = "[" + rv + "]";
		return rv;
	}

	/**
	 * 
	 * @return input bounds as a matrix
	 */
	public String parseInitialInputBound(AutomatonMode m)
	{
		String rv = "";
		LinkedHashMap<String, Integer> varID = cls.varID;
		double[][] linearMatrix = cls.linearMatrix;
		boolean allzero = true;
		for (Expression ex : config.init.values())
		{
			TreeMap<String, Interval> ranges = getBound(ex);
			for (String s : ha.constants.keySet())
			{
				for (int i = 0; i < getAMatrixSize(m); i++)
				{
					if (linearMatrix[i][varID.get(s)] != 0)
						allzero = false;
				}
				if (!allzero)
				{
					for (Entry<String, Interval> e : ranges.entrySet())
					{
						if (e.getKey().equals(s))
							rv = rv + Double.toString(e.getValue().min) + " "
									+ Double.toString(e.getValue().max) + " ";
					}
					rv = rv + ";";
				}
				allzero = true;
			}
		}
		rv = "[" + rv + "]";
		return rv;
	}

	/**
	 * 
	 * @return variables and constants bounds
	 */
	private TreeMap<String, Interval> getBound(Expression ex)
	{
		TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();

		try
		{
			RangeExtractor.getVariableRanges(ex, ranges);
		}
		catch (RangeExtractor.EmptyRangeException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		catch (RangeExtractor.ConstantMismatchException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}

		return ranges;
	}

	/**
	 * 
	 * @return modeName to id for non-semantics preservation converter
	 */
	public TreeMap<String, Integer> getID(BaseComponent ha)
	{
		TreeMap<String, Integer> modeNamesToIds = new TreeMap<String, Integer>();
		int id = 1;
		for (String modeName : ha.modes.keySet())
			modeNamesToIds.put(modeName, id++);

		return modeNamesToIds;
	}

	// ------------- helper classes for semantics preservation converter
	// -------------

	/**
	 * Wrapper for a variable in Stateflow. It contains a name and an iterable of properties to be
	 * set.
	 */
	public class VariableWrapper
	{
		// name
		public final String name;
		// properties
		public Collection<VariableProperty> props;

		private static final String SCOPE = "Scope";
		private static final String UPDATE = "UpdateMethod";
		private static final String DATATYPE = "DataType";
		private static final String ARR_SIZE = "Props.Array.Size";
		private static final String COMPLEXITY = "Props.Complexity";

		/**
		 * @param name
		 *            variable name
		 */
		public VariableWrapper(final String name)
		{
			this.name = name;
			this.props = new ArrayList<VariableProperty>(5);
		}

		/**
		 * Sets the standard values common for all variables.
		 * 
		 * @param scope
		 *            variable scope
		 * @param update
		 *            update type
		 * @param datatype
		 *            data type
		 * @param arrSize
		 *            size of the array (typically an empty string)
		 */
		private void setStandardProperties(final String scope, final String update,
				final String datatype, final String arrSize)
		{
			this.props.add(new VariableProperty(SCOPE, scope));
			this.props.add(new VariableProperty(UPDATE, update));
			this.props.add(new VariableProperty(DATATYPE, datatype));
			this.props.add(new VariableProperty(ARR_SIZE, arrSize));

			// do not use complex numbers
			this.props.add(new VariableProperty(COMPLEXITY, "off"));
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder();
			builder.append("<");
			builder.append(this.name);
			builder.append(": ");
			if (this.props != null)
			{
				final Iterator<VariableProperty> it = this.props.iterator();
				if (it.hasNext())
				{
					builder.append(it.next().toString());
					while (it.hasNext())
					{
						builder.append("; ");
						builder.append(it.next().toString());
					}
				}
				else
				{
					builder.append("--");
				}
			}
			else
			{
				builder.append("--");
			}
			builder.append(">");
			return builder.toString();
		}
	}

	/**
	 * String tuple for variables.
	 */
	public class VariableProperty
	{
		public final String name;
		public final String value;

		/**
		 * @param name
		 *            property name
		 * @param value
		 *            property value
		 */
		public VariableProperty(final String name, final String value)
		{
			this.name = name;
			this.value = value;
		}

		@Override
		public String toString()
		{
			final StringBuilder builder = new StringBuilder();
			builder.append(name);
			builder.append(" := ");
			builder.append(value);
			return builder.toString();
		}
	}

	/**
	 * Lower and upper bounds wrapper for variables.
	 */
	public class VariableBound
	{
		// variable name
		final String var;
		// equality constraint
		final boolean isEquality;

		public VariableBound(final String var, final boolean isEquality)
		{
			this.var = var;
			this.isEquality = isEquality;
		}

		@Override
		public int hashCode()
		{
			return var.hashCode();
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof VariableBound))
			{
				return false;
			}
			return ((VariableBound) o).var.equals(var);
		}
	}

	/**
	 * Bounds from above and below.
	 */
	private class IntervalBound extends VariableBound
	{
		// bounds
		private Expression lower;
		private Expression upper;

		/**
		 * @param var
		 *            variable name
		 * @param lower
		 *            lower bound expression
		 * @param upper
		 *            lower bound expression
		 */
		public IntervalBound(final String var, final Expression lower, final Expression upper)
		{
			super(var, false);
			this.lower = lower;
			this.upper = upper;
		}

		/**
		 * @return true iff lower bound was not set before
		 */
		private boolean isLowerFree()
		{
			return (lower == null);
		}

		/**
		 * @return true iff upper bound was not set before
		 */
		private boolean isUpperFree()
		{
			return (upper == null);
		}

		@Override
		public String toString()
		{
			return var + " in [" + lower + ", " + upper + "]";
		}
	}

	/**
	 * Bounds by an equality.
	 */
	private class EqualityBound extends VariableBound
	{
		// equality expression (including the variable and equality)
		private Expression expr;

		/**
		 * @param var
		 *            variable name
		 * @param expr
		 *            equality expression (including the variable and equality)
		 */
		public EqualityBound(final String var, final Expression expr)
		{
			super(var, true);
			this.expr = expr;
		}

		@Override
		public String toString()
		{
			return expr.toString();
		}
	}

	/**
	 * Printer for Stateflow expressions.
	 */
	public class SimulinkStateflowExpressionPrinter extends DefaultExpressionPrinter
	{
		// translate equalities to small intervals?
		private boolean m_isIntervalEquality;
		// add variable prefix?
		private boolean m_isAddPrefix;
		// add epsilon bound to (in)equalities?
		private boolean m_isAddEpsilon;
		// epsilon variable
		private final Variable m_epsilon;
		/*
		 * pretty printing mode: break AND/OR expressions exceeding this threshold deactivated for
		 * value 0
		 */
		private final int m_prettyPrintThreshold;

		public SimulinkStateflowExpressionPrinter(final int prettyPrintThreshold)
		{
			super();
			opNames.put(Operator.AND, "&&");
			opNames.put(Operator.OR, "||");
			// opNames.put(Operator.EQUAL, "==");
			opNames.put(Operator.NOTEQUAL, "!=");
			this.m_epsilon = new Variable(V_eps.name);
			this.m_prettyPrintThreshold = prettyPrintThreshold;
		}

		/**
		 * Prints an expression in Stateflow.
		 * 
		 * @param expr
		 *            expression
		 * @param isAddPrefix
		 *            true iff variable prefix should be used
		 * @param isIntervalEquality
		 *            true iff equalities should become intervals
		 * @param isAddEpsilon
		 *            true iff epsilons should be added to inequalities
		 */
		public String print(final Expression expr, final boolean isAddPrefix,
				final boolean isIntervalEquality, final boolean isAddEpsilon)
		{
			this.m_isIntervalEquality = isIntervalEquality;
			this.m_isAddPrefix = isAddPrefix;
			this.m_isAddEpsilon = isAddEpsilon;

			return super.print(expr);
		}

		@Override
		public String printVariable(Variable variable)
		{
			if (m_isAddPrefix && (!variable.name.equals(V_eps.name)))
			{
				// add variable prefix (except for epsilon variable)
				return PREFIX_VARIABLE + super.printVariable(variable);
			}
			else
			{
				return super.printVariable(variable);
			}
		}

		@Override
		public String printOperation(Operation operation)
		{
			Operator epsilonOperator = null;
			final Operator op = operation.getOperator();
			switch (op)
			{
			case EQUAL:
				if (m_isIntervalEquality)
				{
					/*
					 * special handling for equalities via intervals (lhs - eps <= rhs && lhs + eps
					 * >= rhs)
					 * 
					 * NOTE: This relies on the fact that an equality never occurs below (as a child
					 * of) another equality, i.e., we have no equality between Booleans as in
					 * "true == false".
					 */
					final List<Expression> children = operation.children;
					final Expression childLeft = children.get(0);
					final Expression childRight = children.get(1);
					final Operation minus = new Operation(childLeft, Operator.SUBTRACT, m_epsilon);
					final Operation plus = new Operation(childLeft, Operator.ADD, m_epsilon);
					final Operation leq = new Operation(minus, Operator.LESSEQUAL, childRight);
					final Operation geq = new Operation(plus, Operator.GREATEREQUAL, childRight);
					operation = new Operation(leq, Operator.AND, geq);
					return print(operation);
				}
				break;
			case LESS:
			case LESSEQUAL:
				// add epsilon
				epsilonOperator = Operator.ADD;
				// NOTE: fall-through
			case GREATER:
			case GREATEREQUAL:
				if (epsilonOperator == null)
				{
					// subtract epsilon
					epsilonOperator = Operator.SUBTRACT;
				}

				if (m_isAddEpsilon)
				{
					// either add or subtract epsilon
					final List<Expression> children = operation.children;
					final Operation addition = new Operation(children.get(1), epsilonOperator,
							m_epsilon);
					operation = new Operation(children.get(0), op, addition);
				}
				break;
			case AND:
			case OR:
				// split conjunctions/disjunctions in pretty printing mode
				if (m_prettyPrintThreshold > 0)
				{
					final List<Expression> children = operation.children;
					if (children.size() != 2)
					{
						// only support binary operators
						break;
					}
					/*
					 * recursively print children first NOTE: If the children are split, then the
					 * parents are always split, too. This is because the String object returned has
					 * no notion of line breaks. This is the intended behavior, however.
					 */
					final Expression leftChild = children.get(0);
					final String lhs = print(leftChild);
					final Expression rightChild = children.get(1);
					final String rhs = print(rightChild);

					if (Math.min(lhs.length(), rhs.length()) >= m_prettyPrintThreshold)
					{
						// - decided to split the string -

						// add parentheses depending on operator priority
						final int priority = Operator.getPriority(op);
						final boolean addParenthesesLeft = (leftChild instanceof Operation)
								? (priority > Operator
										.getPriority(((Operation) leftChild).getOperator()))
								: false;
						final boolean addParenthesesRight = (rightChild instanceof Operation)
								? (priority > Operator
										.getPriority(((Operation) rightChild).getOperator()))
								: false;

						// split string
						final StringBuilder builder = new StringBuilder();

						if (addParenthesesLeft)
						{
							builder.append("(");
						}
						builder.append(lhs);
						if (addParenthesesLeft)
						{
							builder.append(") ");
						}
						else
						{
							builder.append(" ");
						}
						builder.append(opNames.get(op));
						builder.append(" ...");
						builder.append(lineSeparator);
						if (addParenthesesRight)
						{
							builder.append("(");
						}
						builder.append(rhs);
						if (addParenthesesRight)
						{
							builder.append(")");
						}
						return builder.toString();
					}
				}
				break;
			default:
				break;
			}
			return super.printOperation(operation);
		}
	}

	// ------------- constructors -------------

	/**
	 * Standard constructor (does not support every feature).
	 */
	public SimulinkStateflowPrinter()
	{
		this.m_printer = new SimulinkStateflowExpressionPrinter(0);
		this.m_randoms = 0;
		Expression.expressionPrinter = m_printer;
		this.IS_ADD_EPS = false;
		STATE_IN_PART = null;
		TRANS_DWELL2LEAVE_STRING = null;
		STATE_CHOOSE_STRING = null;
		TRANS_LEAVE2DWELL_STRING = null;
		TRANS_CHOOSES2DWELL_STRING = null;
		TRANS_CHOOSEJ2CHOOSES_STRING = null;
		TRANS_CHOOSES2CHOOSEJ_STRING = null;
		TRANS_CHOOSEJ2DWELL_STRING = null;
		TRANS_IN2DWELL_STRING = null;
		TRANS_BACKTRACK2DWELL_STRING = null;
		TRANS_LEAVE2TRANS_STRING = null;
		STATE_DWELL_STRING = null;
	}

	/**
	 * Constructor used in the main loop (supports every feature).
	 * 
	 * @param variableNames
	 *            iterable of variable names
	 * @param m_isAddEpsilon
	 *            true iff epsilons should be added to inequalities
	 * @param prettyPrintThreshold
	 *            threshold for breaking long expressions
	 */
	public SimulinkStateflowPrinter(final Iterable<String> variableNames,
			final boolean isAddEpsilon, final int prettyPrintThreshold)
	{
		this.m_printer = new SimulinkStateflowExpressionPrinter(prettyPrintThreshold);
		this.m_randoms = 0;
		Expression.expressionPrinter = m_printer;
		this.IS_ADD_EPS = isAddEpsilon;

		StringBuilder builder;

		/*
		 * forces unix-style line endings (Matlab requires this) (DID NOT WORK TO SET to override
		 * System.lineSeparator(), maybe have to use old style of System.get etc.)
		 */
		// System.setProperty("line.separator", "\n");

		// -- strings depending on input --

		builder = new StringBuilder();
		builder.append("_in");
		builder.append(lineSeparator);
		builder.append("en:");
		builder.append(lineSeparator);
		builder.append(getStoreString(true, variableNames));
		builder.append(lineSeparator);
		STATE_IN_PART = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_transIdx.name);
		builder.append(" >= 0]");
		builder.append(lineSeparator);
		builder.append("{");
		builder.append(V_tFail.name);
		builder.append(" = ");
		builder.append(V_t.name);
		builder.append(";");
		builder.append(lineSeparator);
		builder.append(getStoreString(false, variableNames));
		builder.append(lineSeparator);
		builder.append(V_resets.name);
		builder.append(" = ");
		builder.append(V_resets.name);
		builder.append(" + 1;}");
		TRANS_CHOOSEJ2DWELL_STRING = builder.toString();

		// -- constant strings --

		builder = new StringBuilder();
		builder.append("_choose");
		builder.append(lineSeparator);
		builder.append("en:");
		builder.append(lineSeparator);
		builder.append(V_T.name);
		builder.append(" =  chooseT(");
		builder.append(V_t.name);
		builder.append(", ");
		builder.append(V_T.name);
		builder.append(", ");
		builder.append(V_tFail.name);
		builder.append(", ");
		builder.append(V_resets.name);
		builder.append(", ");
		builder.append(V_maxResets.name);
		builder.append(", ");
		builder.append(V_rand.name);
		builder.append("(1));");
		STATE_CHOOSE_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append(V_t.name);
		builder.append("_dot = 1;");
		builder.append(lineSeparator);
		builder.append(V_tOut.name);
		builder.append(" = ");
		builder.append(V_t.name);
		builder.append(";");
		STATE_DWELL_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_t.name);
		builder.append(" >= ");
		builder.append(V_T.name);
		builder.append("]");
		TRANS_DWELL2LEAVE_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_t.name);
		builder.append(" >= ");
		builder.append(V_maxT.name);
		builder.append("]");
		builder.append(lineSeparator);
		builder.append("{");
		builder.append(V_stop.name);
		builder.append(" = true;}");
		TRANS_LEAVE2DWELL_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_resets.name);
		builder.append(" < ");
		builder.append(V_maxResets.name);
		builder.append("]");
		TRANS_CHOOSES2DWELL_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_transIdx.name);
		builder.append(" > 0]");
		builder.append(lineSeparator);
		builder.append("{");
		builder.append(V_resets.name);
		builder.append(" = 0;");
		builder.append(lineSeparator);
		builder.append(V_T.name);
		builder.append(" = ");
		builder.append(V_maxT.name);
		builder.append(";");
		builder.append(lineSeparator);
		builder.append(V_trans.name);
		builder.append(" = ");
		builder.append(V_transArray.name);
		builder.append("(");
		builder.append(V_transIdx.name);
		builder.append(");");
		builder.append(lineSeparator);
		builder.append(V_transIdx.name);
		builder.append(" = ");
		builder.append(V_transIdx.name);
		builder.append(" - 1;}");
		TRANS_CHOOSEJ2CHOOSES_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_resets.name);
		builder.append(" == ");
		builder.append(V_maxResets.name);
		builder.append("]");
		TRANS_CHOOSES2CHOOSEJ_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_transIdx.name);
		builder.append(" == 0]");
		builder.append(lineSeparator);
		builder.append("{");
		builder.append(V_T.name);
		builder.append(" = ");
		builder.append(V_maxT.name);
		builder.append(";");
		builder.append(lineSeparator);
		builder.append(V_transIdx.name);
		builder.append(" = -1;");
		builder.append(lineSeparator);
		builder.append(V_trans.name);
		builder.append(" = 0;}");
		TRANS_IN2DWELL_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_transIdx.name);
		builder.append(" == -1]");
		builder.append(lineSeparator);
		builder.append("{");
		builder.append(V_stop.name);
		builder.append(" = true;}");
		TRANS_BACKTRACK2DWELL_STRING = builder.toString();

		builder = new StringBuilder();
		builder.append("[");
		builder.append(V_trans.name);
		builder.append(" == ");
		TRANS_LEAVE2TRANS_STRING = builder.toString();
	}

	// ------------- interface methods -------------

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent) config.root;
		Expression.expressionPrinter = new SimulinkStateflowExpressionPrinter(0);

		// remove this after proper support for multiple initial modes is added
		// if (ha.init.size() != 1)
		// throw new AutomatonExportException("Flow* printer currently only
		// supports single-initial-state models");

		printDocument(originalFilename);
	}

	@Override
	public String getToolName()
	{
		return "Stateflow_converter";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "stateflow";
	}

	@Override
	protected String getCommentPrefix()
	{
		return "%";
	}

	// ------------- methods for the main components of an HA -------------

	/**
	 * Converts the dynamics of a mode to semantics-preserving strings. For each variable that is
	 * changed, the returned object contains one string determining the dynamics. Additionally, the
	 * output variables are assigned.
	 * 
	 * @param mode
	 *            automaton mode
	 * @return iterable of Stateflow strings
	 */
	private Iterable<String> getDynamicsIterable(final AutomatonMode mode)
	{
		return getUpdate(mode.flowDynamics, PREFIX_VARIABLE, PREFIX_OUTPUT, "_dot = ");
	}

	/**
	 * Converts the invariant of a mode to a semantics-preserving string.
	 * 
	 * @param mode
	 *            automaton mode
	 * @return Stateflow string
	 */
	public String getInvariantString(final AutomatonMode mode)
	{
		return getCondition(mode.invariant);
	}

	/**
	 * Converts the initial condition of a mode to semantics-preserving strings.
	 * 
	 * @param mode
	 *            automaton mode
	 * @param constants
	 *            constants map
	 * @return iterable of Stateflow strings
	 */
	private Iterable<String> getInitIterable(final String modeName,
			final Map<String, Double> constants)
	{
		final Expression initExpr = config.init.get(modeName);
		final List<String> inits = new LinkedList<String>();
		// no initial location
		if (initExpr == null)
		{
			// TODO What happens for NOP initial conditions like "true"?
			return inits;
		}

		final Iterable<VariableBound> bounds = getBounds(getConjuncts(initExpr), constants);
		final boolean isBounded = checkBoundedness(bounds);
		if (isBounded)
		{
			// System.out.println("The variables are bounded.");
		}
		else
		{
			System.err.println("The variables are not bounded.");
		}
		int randoms = 0;
		for (final VariableBound bound : bounds)
		{
			if (bound instanceof EqualityBound)
			{
				// equality constraint
				inits.add(m_printer.print(((EqualityBound) bound).expr, true, false, false) + ";");
			}
			else if (bound instanceof IntervalBound)
			{
				// interval constraint
				final IntervalBound intBound = (IntervalBound) bound;
				final StringBuilder builder = new StringBuilder();
				builder.append(PREFIX_VARIABLE);
				builder.append(intBound.var);
				builder.append(" = random(");
				builder.append(V_rand.name);
				builder.append("(");
				builder.append(++randoms);
				builder.append("), ");
				builder.append(m_printer.print(intBound.lower, true, false, false));
				builder.append(", ");
				builder.append(m_printer.print(intBound.upper, true, false, false));
				builder.append(");");
				inits.add(builder.toString());
			}
			else
			{
				throw new IllegalArgumentException("Unsupported bound type.");
			}
		}
		m_randoms = Math.max(m_randoms, randoms);
		return inits;
	}

	/**
	 * Converts the transition guard condition to a semantics-preserving string.
	 * 
	 * @param transition
	 *            automaton transition
	 * @return Stateflow string
	 */
	public String getGuardString(final AutomatonTransition transition)
	{
		return getCondition(transition.guard);
	}

	/**
	 * Converts the transition assignment to semantics-preserving strings.
	 * 
	 * @param transition
	 *            automaton transition
	 * @return iterable of Stateflow strings
	 */
	private Iterable<String> getAssignmentIterable(final AutomatonTransition transition)
	{
		return getUpdate(transition.reset, PREFIX_ASSIGNMENT, PREFIX_VARIABLE, " = ");
	}

	/**
	 * Converts the transition assignment to a semantics-preserving string. Convenience unifier of
	 * strings returned by {@link getAssignmentIterable()}.
	 * 
	 * @param transition
	 *            automaton transition
	 * @return Stateflow string
	 */
	public String getAssignmentString(final AutomatonTransition transition)
	{
		return unifyStrings(getAssignmentIterable(transition));
	}

	/**
	 * Converts an update expression to semantics-preserving strings. An update expression is an
	 * expression like a flow or an assignment.
	 * 
	 * @param map
	 *            map from variable name to update expression
	 * @param prefix1
	 *            prefix for first variable
	 * @param prefix2
	 *            prefix for second variable
	 * @param connector1
	 *            connector for first
	 */
	private Iterable<String> getUpdate(final Map<String, ExpressionInterval> map,
			final String prefix1, final String prefix2, final String connector1)
	{
		// empty case
		if (map.isEmpty())
		{
			return new LinkedList<String>();
		}

		final List<String> list1 = new ArrayList<String>(2 * map.size());
		final List<String> list2 = new ArrayList<String>(map.size());

		for (final Entry<String, ExpressionInterval> entry : map.entrySet())
		{
			final String var = entry.getKey();
			final String value = m_printer.print(entry.getValue().asExpression(), true, false,
					false);
			if (!isSP)
			{
				final String valueNoSP = m_printer.print(entry.getValue().asExpression());
				list1.add(var + connector1 + valueNoSP + ";");
			}
			else
			{
				list1.add(prefix1 + var + connector1 + value + ";");
				list2.add(prefix2 + var + " = " + prefix1 + var + ";");
			}
		}

		list1.addAll(list2);
		return list1;
	}

	/**
	 * Converts a conditional expression to a semantics-preserving string. A conditional expression
	 * is an expression like an invariant or a guard.
	 * 
	 * @param expr
	 *            conditional expression
	 */
	private String getCondition(final Expression expr)
	{
		// empty case
		if (expr == null)
		{
			return "";
		}
		if (!isSP)
		{
			return m_printer.print(expr);
		}
		return m_printer.print(expr, true, true, IS_ADD_EPS);
	}

	/*
	 * ----------- methods for creating the actual Stateflow strings ----------- -----------
	 * (states, junctions, and transition labels) -----------
	 */

	/**
	 * Returns the label for the entry state.
	 * 
	 * @param mode
	 *            automaton mode
	 * @param modeId
	 *            mode ID
	 * @param numTransOut
	 *            number of outgoing transitions
	 */
	public String getStateInLabel(final AutomatonMode mode, final int modeId, final int numTransOut)
	{
		final StringBuilder builder = new StringBuilder();

		builder.append(mode.name);
		builder.append(STATE_IN_PART);
		builder.append(V_loc.name);
		builder.append(" = ");
		builder.append(modeId);
		builder.append(";");
		builder.append(lineSeparator);

		if (numTransOut > 0)
		{
			final String sizeString = Integer.toString(numTransOut);
			builder.append(V_transArray.name);
			builder.append(" = permuteA(");
			builder.append(V_transArray.name);
			builder.append(", ");
			builder.append(sizeString);
			builder.append(", ");
			builder.append(V_randArray.name);
			builder.append(");");
			builder.append(lineSeparator);
			builder.append(V_transIdx.name);
			builder.append(" = ");
			builder.append(sizeString);
			builder.append(";");
		}
		else
		{
			builder.append(V_transIdx.name);
			builder.append(" = 0;");
		}

		return builder.toString();
	}

	/**
	 * Returns the label for the dwell state.
	 * 
	 * @param mode
	 *            automaton mode
	 */
	public String getStateDwellLabel(final AutomatonMode mode)
	{
		final StringBuilder builder = new StringBuilder();

		builder.append(mode.name);
		if (isSP)
		{
			builder.append("_dwell");
		}
		builder.append(lineSeparator);
		builder.append("du:");
		builder.append(lineSeparator);
		if (isSP)
		{
			builder.append(STATE_DWELL_STRING);
		}
		final Iterator<String> it = getDynamicsIterable(mode).iterator();
		while (it.hasNext())
		{
			builder.append(lineSeparator);
			builder.append(it.next());
		}

		return builder.toString();
	}

	/**
	 * Returns the label for the choose state.
	 * 
	 * @param mode
	 *            automaton mode
	 */
	public String getStateChooseLabel(final AutomatonMode mode)
	{
		return mode.name + STATE_CHOOSE_STRING;
	}

	/**
	 * Returns the label for the (initial) transition '() -> initial state'.
	 * 
	 * @param numTransMax
	 *            maximum number of transitions
	 * @param numInits
	 *            number of initial locations
	 */
	public String getTransitionInitLabel(final int numTransMax, final int numInits)
	{
		final StringBuilder builder = new StringBuilder();

		builder.append("{");
		builder.append(V_t.name);
		builder.append(" = 0;");
		builder.append(lineSeparator);
		builder.append(V_tOut.name);
		builder.append(" = 0;");
		builder.append(lineSeparator);
		builder.append(V_backtrack.name);
		builder.append(" = 1;");
		builder.append(lineSeparator);
		builder.append(V_stop.name);
		builder.append(" = false;");
		builder.append(lineSeparator);
		builder.append(V_loc.name);
		builder.append(" = 0;");
		builder.append(lineSeparator);

		// don't add if no transitions
		if (numTransMax > 0)
		{
			builder.append(V_transArray.name);
			builder.append(" = ");
			builder.append("[1");
			for (int i = 2; i <= numTransMax; ++i)
			{
				builder.append(", ");
				builder.append(i);
			}
			builder.append("];");
			builder.append(lineSeparator);
		}

		builder.append(V_init.name);
		if (numInits > 1)
		{
			// more than one initial location
			builder.append(" = round(random(");
			builder.append(V_rand.name);
			builder.append("(1), 0.5, ");
			builder.append(numInits);
			builder.append(".499999));");
		}
		else
		{
			// one initial location
			if (numInits != 1)
			{
				throw new IllegalArgumentException("There was no initial location specified.");
			}
			builder.append(" = 1;");
		}
		builder.append("}");

		return builder.toString();
	}

	/**
	 * Returns the label for the initial transition (enters the initial state).
	 * 
	 * @param mode
	 *            automaton mode
	 * @param initIdx
	 *            unique index of the transition
	 * @param constants
	 *            constants map
	 */
	public String getTransitionInit2inLabel(final AutomatonMode mode, final int initIdx,
			final Map<String, Double> constants)
	{
		return getTransitionInit2inLabel(mode.name, initIdx, constants);
	}

	/**
	 * Returns the label for the initial transition (enters the initial state).
	 * 
	 * @param modeName
	 *            automaton mode name
	 * @param initIdx
	 *            unique index of the transition
	 * @param constants
	 *            constants map
	 */
	public String getTransitionInit2inLabel(final String modeName, final int initIdx,
			final Map<String, Double> constants)
	{
		Iterator<String> it = getInitIterable(modeName, constants).iterator();

		// only write the label if the mode is initial
		if (it.hasNext())
		{
			final StringBuilder builder = new StringBuilder();
			if (isSP)
			{
				// condition: chosen transition number equals index
				builder.append("[");
				builder.append(V_init.name);
				builder.append(" == ");
				builder.append(initIdx);
				builder.append("]");
			}

			// assignment
			builder.append(lineSeparator);
			builder.append("{");
			builder.append(it.next());
			while (it.hasNext())
			{
				builder.append(lineSeparator);
				builder.append(it.next());
			}
			builder.append("}");

			return builder.toString();
		}
		else
		{
			return "";
		}
	}

	/**
	 * Returns the label for the transition 'dwell state -> leave junction'.
	 */
	public String getTransitionDwell2leaveLabel()
	{
		return TRANS_DWELL2LEAVE_STRING;
	}

	/**
	 * Returns the label for the transition 'dwell state -> backtrack junction'.
	 * 
	 * @param mode
	 *            automaton mode
	 */
	public String getTransitionDwell2backtrackLabel(final AutomatonMode mode)
	{
		final StringBuilder builder = new StringBuilder();
		builder.append("[~(");
		builder.append(getInvariantString(mode));
		builder.append(")]");
		return builder.toString();
	}

	/**
	 * Returns the label for the transition 'leave junction -> dwell state'.
	 */
	public String getTransitionLeave2dwellLabel()
	{
		return TRANS_LEAVE2DWELL_STRING;
	}

	/**
	 * Returns the label for the transition 'choose state -> dwell state'.
	 */
	public String getTransitionChoose2dwellLabel()
	{
		return TRANS_CHOOSES2DWELL_STRING;
	}

	/**
	 * Returns the label for the transition 'choose junction -> choose state'.
	 */
	public String getTransitionChooseJ2chooseLabel()
	{
		return TRANS_CHOOSEJ2CHOOSES_STRING;
	}

	/**
	 * Returns the label for the transition 'choose state -> transition choose junction'.
	 */
	public String getTransitionChoose2chooseJLabel()
	{
		return TRANS_CHOOSES2CHOOSEJ_STRING;
	}

	/**
	 * Returns the label for the transition 'time choose junction -> dwell state'.
	 */
	public String getTransitionChooseTime2dwellLabel()
	{
		return TRANS_CHOOSEJ2DWELL_STRING;
	}

	/**
	 * Returns the label for the transition 'entry junction -> dwell state'.
	 */
	public String getTransitionInJ2dwellLabel()
	{
		return TRANS_IN2DWELL_STRING;
	}

	/**
	 * Returns the label for the transition 'backtrack junction -> dwell state'.
	 */
	public String getTransitionBacktrack2dwellLabel()
	{
		return TRANS_BACKTRACK2DWELL_STRING;
	}

	/**
	 * Returns the label for the transition 'leave junction -> transition junction'.
	 * 
	 * @param transitionIndex
	 *            index of the transition
	 */
	public String getTransitionLeave2transLabel(final int transitionIndex)
	{
		final StringBuilder builder = new StringBuilder();
		builder.append(TRANS_LEAVE2TRANS_STRING);
		builder.append(transitionIndex);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Returns the label for the transition 'transition junction -> entry state'. A transition may
	 * contain a guard and an assignment, and the target location may have an invariant.
	 * 
	 * @param transition
	 *            automaton transition
	 */
	public String getTransitionTrans2inLabel(final AutomatonTransition transition)
	{
		// guard string
		final String guard = getGuardString(transition);

		// assignment string
		final String assignment = "{" + getAssignmentString(transition) + "}";

		// Booleans indicating whether different pieces exist
		final boolean hasInvariant = (transition.to.invariant != null);
		final boolean hasAssignment = (assignment.length() > 2);
		final boolean hasGuard = (guard.length() > 1);

		// construct the condition string
		StringBuilder condition = new StringBuilder();
		if (hasInvariant)
		{
			if (hasGuard)
			{
				condition.append("[");
				condition.append(guard);
				condition.append(" && ...");
				condition.append(lineSeparator);
			}
			else
			{
				condition.append("[");
			}
			if (hasAssignment)
			{
				condition.append(getWpString(transition));
				condition.append("]");
			}
			else
			{
				condition.append(getInvariantString(transition.to));
				condition.append("]");
			}
		}
		else
		{
			if (hasGuard)
			{
				condition.append("[" + guard + "]");
			}
			else
			{
				// nothing, builder is empty
			}
		}

		// construct the final string
		if (condition.length() > 0)
		{
			if (hasAssignment)
			{
				condition.append(lineSeparator);
				condition.append(assignment);
				return condition.toString();
			}
			else
			{
				return condition.toString();
			}
		}
		else
		{
			if (hasAssignment)
			{
				return assignment;
			}
			else
			{
				return "";
			}
		}
	}

	// ------------- helper methods -------------

	/**
	 * Splits an expression into its conjuncts.
	 * 
	 * @param expr
	 *            expression in conjunctive normal form
	 */
	private Iterable<Operation> getConjuncts(final Expression expr)
	{
		if (!(expr instanceof Operation))
		{
			throw new IllegalArgumentException("The formula should be a proper operation.");
		}

		final LinkedList<Operation> conjuncts = new LinkedList<Operation>();

		final LinkedList<Operation> stack = new LinkedList<Operation>();
		stack.push((Operation) expr);

		while (!stack.isEmpty())
		{
			final Operation formula = stack.pop();
			if (formula.getOperator() == Operator.AND)
			{
				// conjunction, put the conjuncts on the stack
				assert ((formula.getLeft() instanceof Operation) && (formula
						.getRight() instanceof Operation)) : "The subformulae should be proper operations.";
				stack.push((Operation) formula.getRight());
				stack.push((Operation) formula.getLeft());
			}
			else
			{
				// no conjunction, assume there is none anymore
				conjuncts.add(formula);
			}
		}

		return conjuncts;
	}

	/**
	 * Reads variable bounds from a series of conjuncts and puts them into an internal format.
	 * 
	 * Also, equality bounds are sorted such that the left-hand side is a variable. Especially,
	 * "constant assignments" like <code>const = 5</code> are removed, as constants are cared for at
	 * another place already.
	 * 
	 * NOTE: We assume an "intelligent" ordering like a typical assignment in programming languages.
	 * That is, cases like <code>var2 = var1 & var2 = 5</code> must be given in the form
	 * <code>var2 = 5 & var1 = var2</code> so we can write <code>var2 = 5; var1 := var2</code> in a
	 * sequential manner.
	 * 
	 * TODO The above could be supported, but takes some effort and is not likely to be needed at
	 * the moment. A way to implement it is to collect all elements which are equal and find a
	 * suitable topological ordering.
	 * 
	 * NOTE: Strict and non-strict bounds (< vs. <=) are handled the same way.
	 * 
	 * @param expressions
	 *            expressions
	 * @param constants
	 *            constants map
	 */
	private Iterable<VariableBound> getBounds(final Iterable<Operation> expressions,
			final Map<String, Double> constants)
	{
		final LinkedHashMap<String, VariableBound> bounds = new LinkedHashMap<String, VariableBound>();

		loop: for (final Operation conjunct : expressions)
		{
			final Expression lhs = conjunct.getLeft();
			final Expression rhs = conjunct.getRight();

			String varName = null;
			final Expression bound;

			/*
			 * Check that either the lhs or the rhs is of type <code>Variable</code> and that the
			 * name does not appear in the constants map (i.e. it is a non-constant variable).
			 */
			final boolean isVarLeft;
			boolean isConstantVariable = false;
			if (lhs instanceof Variable)
			{
				varName = ((Variable) lhs).name;
				isVarLeft = !(constants.containsKey(varName));
				isConstantVariable = true;
			}
			else
			{
				isVarLeft = false;
			}
			if (isVarLeft)
			{
				bound = rhs;
			}
			else
			{
				final boolean isVarRight;
				if (rhs instanceof Variable)
				{
					varName = ((Variable) rhs).name;
					isVarRight = !(constants.containsKey(varName));
					isConstantVariable = true;
				}
				else
				{
					isVarRight = false;
				}

				if (isVarRight)
				{
					bound = lhs;
				}
				else
				{
					if (isConstantVariable)
					{
						/*
						 * One of the expressions was a constant variable. Here we can simply ignore
						 * the expression.
						 */
						// System.out.println("Skipping initial expression: " +
						// conjunct);
						continue loop;
					}
					else
					{
						throw new IllegalArgumentException(
								"One side of an operation must be a (non-constant) " + "variable.");
					}
				}
			}
			assert (varName != null) : "The null case should lead to an exception.";

			final boolean isLess;

			switch (conjunct.getOperator())
			{
			case EQUAL:
				// equality constraint
				final EqualityBound eqBound;
				if (isVarLeft)
				{
					// use original equality
					eqBound = new EqualityBound(varName, conjunct);
				}
				else
				{
					// swap variable to the left
					eqBound = new EqualityBound(varName, new Operation(rhs, Operator.EQUAL, lhs));
				}
				final VariableBound oldVal = bounds.put(varName, eqBound);
				if (oldVal != null)
				{
					throw new IllegalArgumentException(
							"Equality must not be specified more that once.");
				}
				continue loop;
			case LESS:
			case LESSEQUAL:
				// < / <=
				isLess = true;
				break;
			case GREATER:
			case GREATEREQUAL:
				// > / >=
				isLess = false;
				break;
			default:
				throw new IllegalArgumentException("Unsupported operator.");
			}

			// bounds handling
			final boolean isLowerBound;
			if (isVarLeft)
			{
				// x <> e
				isLowerBound = !isLess;
				/*
				 * isLess: x < e !isLess: x > e
				 */
			}
			else
			{
				// e <> x
				isLowerBound = isLess;
				/*
				 * isLess: e < x !isLess: e > x
				 */
			}

			// set new bound
			final VariableBound oldVal = bounds.get(varName);
			if (oldVal == null)
			{
				final Expression lower, upper;
				if (isLowerBound)
				{
					lower = bound;
					upper = null;
				}
				else
				{
					lower = null;
					upper = bound;
				}
				bounds.put(varName, new IntervalBound(varName, lower, upper));
			}
			else
			{
				if (oldVal instanceof EqualityBound)
				{
					throw new IllegalArgumentException(
							"Bounds must not be specified more that once.");
				}
				final IntervalBound oldInterval = (IntervalBound) oldVal;
				if (isLowerBound)
				{
					if (!oldInterval.isLowerFree())
					{
						throw new IllegalArgumentException(
								"Bounds must not be specified more that once.");
					}
					oldInterval.lower = bound;
				}
				else
				{
					if (!oldInterval.isUpperFree())
					{
						throw new IllegalArgumentException(
								"Bounds must not be specified more that once.");
					}
					oldInterval.upper = bound;
				}
			}
		}

		return bounds.values();
	}

	/**
	 * Checks boundedness of each variable.
	 * 
	 * @param bounds
	 *            variable bounds
	 * @return true iff all variables are bounded from above and below
	 */
	private boolean checkBoundedness(final Iterable<VariableBound> bounds)
	{
		for (final VariableBound bound : bounds)
		{
			// equality constraint
			if (bound instanceof EqualityBound)
			{
				assert (((EqualityBound) bound).expr != null) : "Equality should be set.";
				continue;
			}
			else
			{
				final IntervalBound intBound = (IntervalBound) bound;
				if ((intBound.lower == null) || (intBound.upper == null))
				{
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Converts a sequence of strings into a single, line-separated string.
	 * 
	 * @param strings
	 *            sequence of strings
	 * @return single string containing one line for each original string
	 */
	private String unifyStrings(Iterable<String> strings)
	{
		final Iterator<String> it = strings.iterator();
		if (it.hasNext())
		{
			final StringBuilder builder = new StringBuilder();
			builder.append(it.next());
			while (it.hasNext())
			{
				builder.append(lineSeparator);
				builder.append(it.next());
			}
			return builder.toString();
		}
		else
		{
			return "";
		}
	}

	/**
	 * Recursively applies substitution to an expression. This method converts the input expression
	 * into an expression where each occurrence of a variable is replaced by the respective
	 * expression defined in the substitution map.
	 * 
	 * @param expr
	 *            raw expression
	 * @param var2replace
	 *            map (variable name -> expression)
	 * @return the expression after applying substitution
	 */
	private Expression substitution(final Expression expr,
			final LinkedHashMap<String, ExpressionInterval> var2replace)
	{
		final Expression outExpr;
		if (expr instanceof Variable)
		{
			final ExpressionInterval replace = var2replace.get(((Variable) expr).name);
			if (replace != null)
			{
				outExpr = replace.asExpression();
			}
			else
			{
				outExpr = expr;
			}
		}
		else if (expr instanceof Operation)
		{
			final Operation op = (Operation) expr;
			final Expression lhs = substitution(op.getLeft(), var2replace);
			final Expression rhs = substitution(op.getRight(), var2replace);
			outExpr = new Operation(lhs, op.getOperator(), rhs);
		}
		else
		{
			outExpr = expr;
		}

		return outExpr;
	}

	/**
	 * Returns the maximum number of random variables occurring.
	 */
	public int getRandomNumber()
	{
		return m_randoms;
	}

	/**
	 * Returns the string for storing/restoring variables.
	 * 
	 * @param isStore
	 *            true iff string for storing is wanted
	 * @param vars
	 *            variables
	 */
	private String getStoreString(final boolean isStore, final Iterable<String> vars)
	{
		// set correct prefixes
		String LEFT, RIGHT;
		if (isStore)
		{
			LEFT = PREFIX_ASSIGNMENT;
			RIGHT = PREFIX_VARIABLE;
		}
		else
		{
			LEFT = PREFIX_VARIABLE;
			RIGHT = PREFIX_ASSIGNMENT;
		}

		// normal variables
		final StringBuilder strNormal = new StringBuilder();
		// output variables
		final StringBuilder strOut = new StringBuilder();

		final Iterator<String> it = vars.iterator();
		while (it.hasNext())
		{
			String var = it.next();
			strNormal.append(LEFT);
			strNormal.append(var);
			strNormal.append(" = ");
			strNormal.append(RIGHT);
			strNormal.append(var);
			strNormal.append(";");
			strNormal.append(lineSeparator);

			strOut.append(PREFIX_OUTPUT);
			strOut.append(var);
			strOut.append(" = ");
			strOut.append(PREFIX_VARIABLE);
			strOut.append(var);
			strOut.append(";");
			strOut.append(lineSeparator);
		}

		strNormal.append(strOut);

		if (isStore)
		{
			LEFT = V_tTmp.name;
			RIGHT = V_t.name;
		}
		else
		{
			LEFT = V_t.name;
			RIGHT = V_tTmp.name;
		}
		strNormal.append(LEFT);
		strNormal.append(" = ");
		strNormal.append(RIGHT);
		strNormal.append(";");
		strNormal.append(lineSeparator);
		strNormal.append(V_tOut.name);
		strNormal.append(" = ");
		strNormal.append(V_t.name);
		strNormal.append(";");
		strNormal.append(lineSeparator);
		strNormal.append(V_backtrack.name);
		strNormal.append(" = ");
		strNormal.append(V_backtrack.name);
		if (isStore)
		{
			strNormal.append(" - 1;");

			strNormal.append(lineSeparator);
			strNormal.append(V_tFail.name);
			strNormal.append(" = ");
			strNormal.append(V_maxT.name);
			strNormal.append(";");
		}
		else
		{
			strNormal.append(" + 1;");
		}

		return strNormal.toString();
	}

	/**
	 * Computes the weakest precondition string for an invariant and an assignment. Given a
	 * transition whose assignment is non-empty and whose target location has a non-empty invariant,
	 * the condition under which the transition can be taken is given by the weakest precondition of
	 * the invariant I under the assignment A, wp(I, A).
	 * 
	 * wp(I, A) can be computed by substituting each occurrence of a variable x assigned in A via 'x
	 * = e' by the expression e.
	 * 
	 * Example: A == x = x + 1 I == x - y > 0 ==> wp(I, A) == (x + 1) - y > 0
	 * 
	 * @param transition
	 *            automaton transition
	 */
	private String getWpString(final AutomatonTransition transition)
	{
		if (transition.to.invariant == null)
		{
			throw new IllegalArgumentException("The invariant should be non-empty.");
		}
		if (transition.reset.isEmpty())
		{
			throw new IllegalArgumentException("The assignment should be non-empty.");
		}

		final Expression wp = substitution(transition.to.invariant, transition.reset);
		return getCondition(wp);
	}

	/**
	 * Returns the auxiliary variables introduced by the translation. This function can only be
	 * called after the number of maximum transitions was found.
	 * 
	 * NOTE: A call to this function initializes some data structures. If the function is called
	 * more than once, it should be changed to store the variables to avoid recomputation efforts.
	 * 
	 * @param numTransMax
	 *            maximum number of transitions
	 * @param numRandMax
	 *            maximum number of random variables used
	 * @return iterator over all auxiliary variables
	 */
	public Iterator<VariableWrapper> getVariables(final int numTransMax, final int numRandMax)
	{
		final String SCOPE_L = "Local";
		final String SCOPE_O = "Output";
		final String SCOPE_I = "Input";
		final String SCOPE_P = "Parameter";
		final String UPDATE_D = "Discrete";
		final String UPDATE_C = "Continuous";
		final String DATATYPE_B = "boolean";
		final String DATATYPE_D = "double";
		final String DATATYPE_I = "double"; // TODO how to set integer values?
		final String DATATYPE_A = "double"; // TODO how to set integer values?
		final String ARR_EMPTY = "";

		// list of all variables
		final List<VariableWrapper> variables = new LinkedList<VariableWrapper>();

		// -- individual properties --

		// The first three elements must stay the same
		V_stop.setStandardProperties(SCOPE_O, UPDATE_D, DATATYPE_B, ARR_EMPTY);
		variables.add(V_stop);

		V_tOut.setStandardProperties(SCOPE_O, UPDATE_D, DATATYPE_D, ARR_EMPTY);
		variables.add(V_tOut);

		V_backtrack.setStandardProperties(SCOPE_O, UPDATE_D, DATATYPE_I, ARR_EMPTY);
		variables.add(V_backtrack);

		/*
		 * From here the position and order do not matter
		 * 
		 * Exception: The order of V_rand and V_randArray matters.
		 */

		V_T.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_D, ARR_EMPTY);
		variables.add(V_T);

		V_t.setStandardProperties(SCOPE_L, UPDATE_C, DATATYPE_D, ARR_EMPTY);
		variables.add(V_t);

		V_tFail.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_D, ARR_EMPTY);
		variables.add(V_tFail);

		V_tTmp.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_D, ARR_EMPTY);
		variables.add(V_tTmp);

		V_maxT.setStandardProperties(SCOPE_P, UPDATE_D, DATATYPE_D, ARR_EMPTY);
		variables.add(V_maxT);

		V_transIdx.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_I, ARR_EMPTY);
		variables.add(V_transIdx);

		V_trans.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_I, ARR_EMPTY);
		variables.add(V_trans);

		V_loc.setStandardProperties(SCOPE_O, UPDATE_D, DATATYPE_I, ARR_EMPTY);
		variables.add(V_loc);

		V_resets.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_I, ARR_EMPTY);
		variables.add(V_resets);

		V_maxResets.setStandardProperties(SCOPE_P, UPDATE_D, DATATYPE_I, ARR_EMPTY);
		variables.add(V_maxResets);

		V_init.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_I, ARR_EMPTY);
		variables.add(V_init);

		V_eps.setStandardProperties(SCOPE_P, UPDATE_D, DATATYPE_D, ARR_EMPTY);
		variables.add(V_eps);

		String arrSize;

		assert (numRandMax > 0) : "One random number is always needed.";
		arrSize = "[1, " + numRandMax + "]";
		V_rand.setStandardProperties(SCOPE_I, UPDATE_C, DATATYPE_A, arrSize);
		variables.add(V_rand);

		// the following variables are only added if there are any transitions
		if (numTransMax > 0)
		{
			arrSize = "[1, " + numTransMax + "]";
			V_transArray.setStandardProperties(SCOPE_L, UPDATE_D, DATATYPE_A, arrSize);
			variables.add(V_transArray);

			V_randArray.setStandardProperties(SCOPE_I, UPDATE_D, DATATYPE_A, arrSize);
			variables.add(V_randArray);
		}

		return variables.iterator();
	}
}