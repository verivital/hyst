package com.verivital.hyst.printers.hycreate2;

import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.kohsuke.args4j.Option;

import com.stanleybak.hycreate.containers.HyCreateData;
import com.stanleybak.hycreate.containers.ModeData;
import com.stanleybak.hycreate.containers.ModelOptions;
import com.stanleybak.hycreate.containers.ModelPlotOptions;
import com.stanleybak.hycreate.containers.ModelSimulationOptions;
import com.stanleybak.hycreate.containers.ModelSimulationOptions.SimulationType;
import com.stanleybak.hycreate.containers.ModelSimulationOptions.StartingPositionsType;
import com.stanleybak.hycreate.containers.TransitionData;
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
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

public class HyCreate2Printer extends ToolPrinter
{
	@Option(name = "-time", usage = "reachability time", metaVar = "VAL")
	String time = "auto";

	@Option(name = "-step", usage = "sampling time step", metaVar = "VAL")
	String step = "auto";

	@Option(name = "-plot", usage = "x and y plot dimension indicies", metaVar = "DIMX,DIMY")
	String plot = "auto";

	@Option(name = "-sim-only", usage = "only do simulation (no reachability)?", metaVar = "0/1")
	String simOnly = "0";

	private BaseComponent ha;
	private HyCreateData data;

	public HyCreate2Printer()
	{
		preconditions.skip(PreconditionsFlag.NO_URGENT);
		preconditions.skip(PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS);
		preconditions.skip(PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS);
	}

	/**
	 * Sets up internal data structures for HyCreate output. The output is done using an XML printer
	 * on this data structure.
	 * 
	 * @param baseName
	 * @param isSimulation
	 * @param doPlot
	 */
	private void doSetup(String baseName, boolean isSimulation)
	{
		data = new HyCreateData();

		// add comment to global section
		data.setGlobalText(
				"// Made using " + Hyst.TOOL_NAME + " from model file " + originalFilename + "\n"
						+ "// Command line arguments were: " + Hyst.programArguments);

		String version = "File Version 3";

		data.setVersionString(version);
		data.setAutomatonName(baseName);
		data.setDimensions(makeDimensionsString());

		ModelOptions o = data.getOptions();
		ModelPlotOptions p = o.getPlotOptions();
		ModelSimulationOptions s = o.getSimulationOptions();

		o.setReachabilityTime(getParam(time, config.settings.spaceExConfig.timeHorizon));
		o.setTimeStep(getParam(step, config.settings.spaceExConfig.samplingTime));

		if (isSimulation)
		{
			s.setSimulationType(SimulationType.SIMULATION_ONLY);
			s.setEnumerateTransitions(true);
			s.setStartingPositions(StartingPositionsType.CORNERS_AND_MIDPOINTS);
		}
		else
			s.setSimulationType(SimulationType.REACHABILITY_ONLY);

		// do a plot
		p.setVisualizeDuringComputation(false);
		p.setVisualizeAfterComputation(true);

		// plot dimensions requires index
		int xIndex;
		int yIndex;

		if (plot.equals("auto"))
		{
			xIndex = getPlotIndex(true);
			yIndex = getPlotIndex(false);
		}
		else
		{
			String[] parts = plot.split(",");

			if (parts.length != 2)
				throw new AutomatonExportException(
						"Plot parameter expected two comma-separated variable names. Got: " + plot);

			xIndex = getPlotIndex(parts[0]);
			yIndex = getPlotIndex(parts[1]);
		}

		o.getPlotOptions().setPlotXDimensionIndex(xIndex);
		o.getPlotOptions().setPlotYDimensionIndex(yIndex);
	}

	private int getPlotIndex(boolean isX)
	{
		return getPlotIndex(config.settings.plotVariableNames[isX ? 0 : 1]);
	}

	private int getPlotIndex(String dimName)
	{
		int rv = -1;

		for (int i = 0; i < ha.variables.size(); ++i)
		{
			if (ha.variables.get(i).equals(dimName))
			{
				rv = i;
				break;
			}
		}

		if (rv == -1)
			throw new AutomatonExportException(
					"Couldn't find plot dimension index. Name = " + dimName);

		return rv;
	}

	private String makeDimensionsString()
	{
		String rv = "";

		for (String varName : ha.variables)
		{
			if (rv.length() > 0)
				rv += ", ";

			rv += varName;
		}

		return rv;
	}

	private void doInitialStates()
	{
		String initString = "";

		// initial states are hyperrectangle mode
		for (Entry<String, Expression> e : config.init.entrySet())
		{
			TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();

			try
			{
				RangeExtractor.getVariableRanges(e.getValue(), ranges);
			}
			catch (EmptyRangeException ex)
			{
				throw new AutomatonExportException("Empty variable range in initial states", ex);
			}
			catch (ConstantMismatchException ex)
			{
				throw new AutomatonExportException("Constant mismatch in initial states", ex);
			}
			catch (UnsupportedConditionException ex)
			{
				throw new AutomatonExportException("Non-box initial states", ex);
			}

			boolean first = true;

			for (String v : ha.variables)
			{
				Interval range = ranges.get(v);

				if (range == null)
				{
					Hyst.logError("Warning: initial range not defined for variable: " + v
							+ " approximating using min and max double value.");

					range = new Interval(-Double.MAX_VALUE, Double.MAX_VALUE);
				}

				if (!first)
					initString += " ; ";
				else
					first = false;

				initString += range.min + " , " + range.max;
			}

			initString += " ; " + e.getKey() + "\n";
		}

		data.setInitialStates(initString);
	}

	private void doJumps()
	{
		for (AutomatonTransition t : ha.transitions)
		{
			String fromName = t.from.name;
			String toName = t.to.name;

			TransitionData td = new TransitionData();
			td.setFrom(fromName);
			td.setTo(toName);

			td.setGuard(conditionExpressionText(t.guard, "guard"));

			if (t.reset.size() > 0)
			{
				String reset = getResetText(t.reset);
				td.setReset(reset);
			}

			data.getTransitions().add(td);
		}
	}

	/**
	 * Get the condition (guard/invariant) code by iterating all min / max combinations of
	 * variables. If any of them is satisfied, the condition is deemed true
	 * 
	 * @param exp
	 *            the condition expression
	 * @return the code string
	 */
	private String conditionMinMaxCombinations(Expression exp)
	{
		StringBuilder sb = new StringBuilder("boolean sat = false;\n\n");

		ArrayList<String> vars = new ArrayList<String>();
		vars.addAll(AutomatonUtil.getVariablesInExpression(exp));

		int maxIterator = 1;

		for (int dimIndex = 0; dimIndex < vars.size(); ++dimIndex)
			maxIterator *= 2;

		// next iterate from 0 to maxIterator (try each bit-array combination)
		for (int iterator = 0; iterator < maxIterator; ++iterator)
		{
			Expression iExpression = exp.copy();

			// extract each dimension's boolean true/false values from iterator
			int mask = 0x01;
			for (int dimIndex = 0; dimIndex < vars.size(); ++dimIndex)
			{
				boolean isMin = (iterator & mask) == 0;
				mask = mask << 1;

				// assign the current dimension of the point point
				String var = vars.get(dimIndex);
				String varSubbed = var + "." + (isMin ? "min" : "max");

				iExpression = AutomatonUtil.substituteVariable(iExpression, var,
						new Variable(varSubbed));
			}

			// e is the new expression
			sb.append("sat = sat || (" + iExpression + ");\n");
		}

		sb.append("\nreturn sat;");

		return sb.toString();
	}

	/**
	 * Get the code for checking a condition. If this is linear, it's exact. Otherwise we iterate
	 * min/max combinations
	 * 
	 * @param e
	 *            the condition expresssion
	 * @param desc
	 *            the description, like "guard" or "invariant"
	 * @return the java code to do the check
	 */
	private String conditionExpressionText(Expression e, String desc)
	{
		String rv;

		if (e == Constant.TRUE)
		{
			rv = "return true;";
		}
		else
		{
			String note = "// " + desc + " is: " + e.toDefaultString();
			String check;

			try
			{
				LinearExpressionSet invSet = new LinearExpressionSet(e, ha.variables);
				check = "return " + getLinearConditionCode(invSet) + ";";
			}
			catch (AutomatonExportException ex)
			{
				// maybe it's a nonlinear
				Hyst.logError("Nonlinear " + desc + " detected: " + e.toDefaultString());
				Hyst.logError("Iterating min/max combinations");

				check = conditionMinMaxCombinations(e);
			}

			rv = note + "\n\n" + check;
		}

		return rv;
	}

	private String getLinearConditionCode(LinearExpressionSet les)
	{
		String rv = "";

		for (LinearExpression le : les.expressions)
		{
			if (rv.length() > 0)
				rv += " && ";

			rv += encodeLinearExpression(le);
		}

		return rv;
	}

	private String encodeLinearExpression(LinearExpression le)
	{
		List<String> vars = ha.variables;

		ArrayList<String> minPoint = new ArrayList<String>();
		ArrayList<String> maxPoint = new ArrayList<String>();

		for (int v = 0; v < vars.size(); ++v)
		{
			String var = vars.get(v);
			double c = le.coefficients[v];

			if (c >= 0)
			{
				minPoint.add("$" + var + ".min");
				maxPoint.add("$" + var + ".max");
			}
			else
			{
				maxPoint.add("$" + var + ".min");
				minPoint.add("$" + var + ".max");
			}
		}

		String rv = "";

		if (le.type == Operator.LESS)
			rv = encodeDotProduce(minPoint, le.coefficients) + " < " + le.rhs;
		else if (le.type == Operator.LESSEQUAL)
			rv = encodeDotProduce(minPoint, le.coefficients) + " <= " + le.rhs;
		else if (le.type == Operator.GREATER)
			rv = encodeDotProduce(maxPoint, le.coefficients) + " > " + le.rhs;
		else if (le.type == Operator.GREATEREQUAL)
			rv = encodeDotProduce(maxPoint, le.coefficients) + " >= " + le.rhs;
		else if (le.type == Operator.EQUAL)
			rv = "(" + encodeDotProduce(minPoint, le.coefficients) + " <= " + le.rhs + ") && ("
					+ encodeDotProduce(maxPoint, le.coefficients) + " >= " + le.rhs + ")";
		else if (le.type == Operator.NOTEQUAL)
			rv = "(" + encodeDotProduce(minPoint, le.coefficients) + " != " + le.rhs + ") || ("
					+ encodeDotProduce(maxPoint, le.coefficients) + " != " + le.rhs + ")";
		else
			throw new AutomatonExportException(
					"Hycreate export doesn't support linear expressions of type: " + le.type
							+ " in expression: " + le);

		return rv;
	}

	private String encodeDotProduce(ArrayList<String> pt, double[] coefficients)
	{
		String sum = "";

		for (int i = 0; i < pt.size(); ++i)
		{
			String dim = pt.get(i);
			double d = coefficients[i];

			if (d == 0)
				continue;

			if (sum.length() > 0)
				sum += " + ";

			sum += d + "*" + dim;
		}

		return sum.length() == 0 ? "0" : sum;
	}

	/**
	 * Get the HyCreate reset text. Only a small class of expressions are directly supported: x :=
	 * constant + [a, b] or x := var + [a, b]
	 * 
	 * @param exp
	 *            the reset map
	 * @return
	 */
	private String getResetText(Map<String, ExpressionInterval> reset)
	{
		String rv = "";

		for (Entry<String, ExpressionInterval> e : reset.entrySet())
		{
			String variableName = e.getKey();
			Expression exp = e.getValue().getExpression();
			Interval i = e.getValue().getInterval();

			if (exp instanceof Constant)
			{
				double val = ((Constant) exp).getVal();

				if (i == null)
					rv += "$" + variableName + ".set(" + val + ");\n";
				else
					rv += "$" + variableName + ".set(" + (val + i.min) + ", " + (val + i.max)
							+ ");\n";
			}
			else if (exp instanceof Variable)
			{
				String rhsName = ((Variable) exp).name;
				// x := y + [min, max]

				if (i == null)
					rv = variableName + ".set($" + rhsName + ".min, $" + rhsName + ".max);";
				else
				{
					String min = "$" + rhsName + ".min + " + i.min;
					String max = "$" + rhsName + ".max + " + i.max;

					rv += "$" + variableName + ".set(" + min + ", " + max + ");\n";
				}
			}
			else if (isSimpleAddSub(exp))
			{
				// simple operations like "x + 1" or "y - 5"

				Operation o = exp.asOperation();
				Operator op = o.op;

				String strOp = op == Operator.ADD ? "+" : "-";
				String var = ((Variable) o.getLeft()).name;
				double val = ((Constant) o.getRight()).getVal();

				rv += "$" + variableName + ".set($" + var + ".min " + strOp + " " + val + ", $"
						+ var + ".max " + strOp + " " + val + ");\n";
			}
			else
			{
				Hyst.logError(
						"Complex expression found in reset assignment: " + exp.toDefaultString());
				Hyst.logError("Iterating min/max combinations (unsound if reset is nonlinear).");
				StringBuilder sb = new StringBuilder();

				sb.append("\n// Original expression: " + exp + "\n");
				sb.append("$" + variableName + ".set(Double.MAX_VALUE, -Double.MAX_VALUE);\n");

				// try all possiblities of min/max
				ArrayList<String> vars = new ArrayList<String>();
				vars.addAll(AutomatonUtil.getVariablesInExpression(exp));

				int maxIterator = 1;

				for (int dimIndex = 0; dimIndex < vars.size(); ++dimIndex)
					maxIterator *= 2;

				// next iterate from 0 to maxIterator (try each bit-array
				// combination)
				for (int iterator = 0; iterator < maxIterator; ++iterator)
				{
					Expression iExpression = exp.copy();

					// extract each dimension's boolean true/false values from
					// iterator
					int mask = 0x01;
					for (int dimIndex = 0; dimIndex < vars.size(); ++dimIndex)
					{
						boolean isMin = (iterator & mask) == 0;
						mask = mask << 1;

						// assign the current dimension of the point point
						String var = vars.get(dimIndex);
						String varSubbed = var + "." + (isMin ? "min" : "max");

						iExpression = AutomatonUtil.substituteVariable(iExpression, var,
								new Variable(varSubbed));
					}

					// e is the new expression
					sb.append("$" + variableName + ".expand(" + iExpression + ");\n");

				}

				rv = sb.toString();
			}
		}

		return rv;
	}

	// simple operations like "x + 1" or "y - 5"
	private boolean isSimpleAddSub(Expression exp)
	{
		boolean rv = false;
		Operation o = exp.asOperation();

		if (o != null)
		{
			Operator op = o.op;

			if (op == Operator.ADD || op == Operator.SUBTRACT)
			{
				if (o.getLeft() instanceof Variable && o.getRight() instanceof Constant)
					rv = true;
			}
		}

		return rv;
	}

	private void doModes()
	{
		int numVars = ha.variables.size();

		for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
		{
			String name = e.getKey();
			AutomatonMode mode = e.getValue();

			ModeData mData = new ModeData(numVars);
			ArrayList<String> ders = mData.getDerivative();

			// derivative, invariant
			for (int v = 0; v < ha.variables.size(); ++v)
			{
				String varName = ha.variables.get(v);
				String derText = getIntervalExpressionCode(mode.flowDynamics.get(varName));

				ders.set(v, "return " + derText);
			}

			mData.setInvariant(conditionExpressionText(mode.invariant, "invariant"));

			data.getModes().put(name, mData);
		}
	}

	/**
	 * Get the text to construct a new interval from this interval expression
	 * 
	 * @param ei
	 *            the interval expression
	 * @return the code text, like "new Interval(a,b);"
	 */
	private String getIntervalExpressionCode(ExpressionInterval ei)
	{
		String text = null;
		String eStr = ei.getExpression().toString();
		Interval i = ei.getInterval();

		if (i != null)
			text = "new Interval(" + eStr + " + " + i.min + ", " + eStr + " + " + i.max + ");";
		else
			text = "new Interval(" + eStr + ", " + eStr + ");";

		return text;
	}

	@Override
	protected void printAutomaton()
	{
		this.ha = (BaseComponent) config.root;

		Expression.expressionPrinter = new HyCreateExpressionPrinter();

		// convert urgent transitions
		AutomatonUtil.convertUrgentTransitions(ha, config);

		// substitute constants and simplify
		new SubstituteConstantsPass().runTransformationPass(config, null);
		new SimplifyExpressionsPass().runTransformationPass(config, null);

		boolean isSimulation = !getParam(simOnly, 0).equals("0");

		doSetup(baseName, isSimulation);

		// HyCreate currently requires continuous variables
		if (config.settings.plotVariableNames[0] == null
				|| config.settings.plotVariableNames[1] == null)
			throw new AutomatonExportException("HyCreate2 requires two plot variables.");

		doModes();

		doJumps();

		doInitialStates();

		// export it!
		String charset = Charset.defaultCharset().name();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();

		XMLEncoder e = new XMLEncoder(new BufferedOutputStream(stream), charset, true, 0);
		e.writeObject(data);
		e.close();

		try
		{
			printLine(stream.toString(charset));
		}
		catch (UnsupportedEncodingException er)
		{
			throw new AutomatonExportException("Error exporting xml", er);
		}
	}

	private String getParam(String val, double defVal)
	{
		return val.equals("auto") ? "" + defVal : val;
	}

	@Override
	public String getToolName()
	{
		return "HyCreate2";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "hycreate";
	}

	@Override
	protected String getCommentPrefix()
	{
		return "//";
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	public Map<String, String> getDefaultParams()
	{
		LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();

		params.put("time", "auto");
		params.put("step", "auto");
		params.put("plot", "auto");
		params.put("sim-only", "0");

		return params;
	}

	@Override
	public String getExtension()
	{
		return ".hyc2";
	}

	private static class HyCreateExpressionPrinter extends DefaultExpressionPrinter
	{
		public HyCreateExpressionPrinter()
		{
			opNames.put(Operator.COS, "Math.cos");
			opNames.put(Operator.SIN, "Math.sin");
			opNames.put(Operator.TAN, "Math.tan");
			opNames.put(Operator.LN, "Math.ln");
			opNames.put(Operator.SQRT, "Math.sqrt");
			opNames.put(Operator.EXP, "Math.exp");

			opNames.put(Operator.AND, "&&");
			opNames.put(Operator.OR, "||");
		}

		@Override
		public String printVariable(Variable v)
		{
			return "$" + v.name;
		}

		@Override
		public String printOperation(Operation o)
		{
			// custom printing for pow operators
			String rv = null;

			if (o.op == Operator.POW)
				rv = "Math.pow(" + print(o.getLeft()) + ", " + print(o.getRight()) + ")";
			else
				rv = super.printOperation(o);

			return rv;
		}
	};
}
