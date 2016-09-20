/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionModifier;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.printers.PySimPrinter.PythonPrinterCustomization;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.python.PythonUtil;
import com.verivital.hyst.util.DynamicsUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;
import com.verivital.hyst.util.StringOperations;

/**
 * Printer for Python-based Hylaa.
 * 
 * @author Stanley Bak (8-2016)
 *
 */
public class HylaaPrinter extends ToolPrinter
{
	private static final String COMMENT_CHAR = "#";

	public HylaaPrinter()
	{
	}

	@Override
	protected String getCommentPrefix()
	{
		return COMMENT_CHAR + " ";
	}

	@Override
	protected String createCommentText(String text)
	{
		return "'''\n" + text + "\n'''";
	}

	public static class HylaaExtraPrintFuncs extends PythonPrinterCustomization
	{
		public HylaaExtraPrintFuncs()
		{
			this.automatonObjectName = "LinearHybridAutomaton";
		}

		@Override
		public ArrayList<String> getImportLines(BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("import numpy as np");

			rv.add("from hylaa.hybrid_automaton import LinearHybridAutomaton, LinearConstraint, HyperRectangle");
			rv.add("from hylaa.engine import HylaaSettings");
			rv.add("from hylaa.engine import HylaaEngine");
			rv.add("from hylaa.plotutil import PlotSettings");

			return rv;
		}

		public ArrayList<String> getPrintTransitions(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// guard
			// trans = ha.new_transition(loc1, loc2)
			// trans.guard_list = [guard]

			rv.add("trans = ha.new_transition(" + at.from.name + ", " + at.to.name + ")");

			if (at.guard != Constant.TRUE)
			{
				ArrayList<Operation> parts = DynamicsUtil.splitConjunction(at.guard);

				for (Operation o : parts)
				{
					ArrayList<String> conds = toLinearConstraints(o, at.parent.variables);

					for (int i = 0; i < conds.size(); ++i)
					{
						rv.add("trans.guard_list.append(" + conds.get(i) + ") # "
								+ o.toDefaultString());
					}
				}
			}

			if (at.reset.size() > 0)
				throw new PreconditionsFailedException(
						"Resets not currently supported in hylaa printer: " + at);

			return rv;
		}

		public ArrayList<String> getPrintModeLines(AutomatonMode am)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add(am.name + " = ha.new_mode('" + am.name + "')");

			try
			{
				rv.add(am.name + ".a_matrix = np.array("
						+ toPythonListList(DynamicsUtil.extractDynamicsMatrixA(am)) + ")");
				rv.add(am.name + ".b_vector = np.array("
						+ toPythonList(DynamicsUtil.extractDynamicsVectorB(am)) + ")");

				// invariant
				// loc1.inv_list = [inv1]
				if (am.invariant != Constant.TRUE)
				{
					ArrayList<Operation> parts = DynamicsUtil.splitConjunction(am.invariant);

					for (Operation o : parts)
					{
						ArrayList<String> invs = toLinearConstraints(o, am.automaton.variables);

						for (int i = 0; i < invs.size(); ++i)
						{
							rv.add(am.name + ".inv_list.append(" + invs.get(i) + ") # "
									+ o.toDefaultString());
						}
					}
				}
			}
			catch (AutomatonExportException e)
			{
				throw new PreconditionsFailedException(e.toString(), e);
			}

			return rv;
		}

		/**
		 * Convert a conditions to a list of 'LinearConstraint()' initialization strings
		 * 
		 * @param condition
		 *            a basic condition
		 * @param vars
		 *            the variables
		 * 
		 * @return a list of 'LinearConstraint()' strings (one constraints may produce multiple
		 *         linear constraints)
		 */
		private ArrayList<String> toLinearConstraints(Operation o, ArrayList<String> vars)
		{
			ArrayList<String> rv = new ArrayList<String>();

			Operator op = o.op;

			// extract the variable vector on the left and right hand sides
			ArrayList<Double> leftVec = DynamicsUtil.extractLinearVector(o.getLeft(), vars);
			double leftVal = DynamicsUtil.extractLinearValue(o.getLeft());

			ArrayList<Double> rightVec = DynamicsUtil.extractLinearVector(o.getRight(), vars);
			double rightVal = DynamicsUtil.extractLinearValue(o.getRight());

			// normal form has all variables on left and all constants on the right
			for (int i = 0; i < leftVec.size(); ++i)
				leftVec.set(i, leftVec.get(i) - rightVec.get(i));

			rightVal -= leftVal;
			// now, only work with leftVec and rightVal

			if (op == Operator.LESS || op == Operator.LESSEQUAL || op == Operator.EQUAL)
			{
				StringBuilder str = new StringBuilder("LinearConstraint(");
				str.append(toPythonList(leftVec));
				str.append(", " + ToolPrinter.doubleToString(rightVal));
				str.append(")");
				rv.add(str.toString());
			}

			if (op == Operator.GREATER || op == Operator.GREATEREQUAL || op == Operator.EQUAL)
			{
				for (int i = 0; i < leftVec.size(); ++i)
					leftVec.set(i, -leftVec.get(i));

				StringBuilder str = new StringBuilder("LinearConstraint(");
				str.append(toPythonList(leftVec));
				str.append(", " + ToolPrinter.doubleToString(-rightVal));
				str.append(")");
				rv.add(str.toString());
			}

			if (op != Operator.EQUAL && op != Operator.LESS && op != Operator.LESSEQUAL
					&& op != Operator.GREATER && op != Operator.GREATEREQUAL)
				throw new AutomatonExportException(
						"Not a linear condition: " + o.toDefaultString());

			return rv;
		}

		private String toPythonList(ArrayList<Double> list)
		{
			return "[" + StringOperations.join(", ", list.toArray(new Double[] {})) + "]";
		}

		private String toPythonListList(ArrayList<ArrayList<Double>> matrix)
		{
			ArrayList<String> convertedLists = new ArrayList<String>();

			for (ArrayList<Double> row : matrix)
				convertedLists.add(toPythonList(row));

			return "[" + StringOperations.join(", ", convertedLists.toArray(new String[] {})) + "]";
		}

		@Override
		public ArrayList<String> getExtraTransitionPrintLines(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// add the symbolic guard
			// String s = rrtSymbolicPyinter.print(at.guard);
			// rv.add("t.guard_strings = [" + s + "]");

			return rv;
		}
	}

	private static ExpressionModifier em = new ExpressionModifier()
	{
		@Override
		public Expression modifyExpression(Expression e)
		{
			return PythonUtil.pythonSimplifyExpressionChop(e, 1e-9);
		}
	};

	@Override
	protected void printAutomaton()
	{
		new SimplifyExpressionsPass().runVanillaPass(config, "");

		// simplify all the expressions using python
		if (PythonBridge.hasPython())
			ExpressionModifier.modifyBaseComponent((BaseComponent) config.root, em);

		this.printCommentHeader();

		printNewline();

		printLine(PySimPrinter.automatonToString(config, new HylaaExtraPrintFuncs()));

		int xDim = config.root.variables.indexOf(config.settings.plotVariableNames[0]);
		int yDim = config.root.variables.indexOf(config.settings.plotVariableNames[1]);

		printLine("def main():");
		increaseIndentation();
		printLine("'runs hylaa on the model'");
		printLine("ha = define_ha()");
		printLine("init = define_init_states(ha)");
		printNewline();

		/*
		 * plot_settings = PlotSettings()
		 * 
		 * plot_settings.plot_mode = PlotSettings.PLOT_FULL plot_settings.xdim = 0
		 * plot_settings.ydim = 1
		 * 
		 * settings = HylaaSettings(step=0.25, max_time=10.0, plot_settings=plot_settings)
		 * 
		 * engine = HylaaEngine(ha, settings) engine.run(init)
		 */

		printLine("plot_settings = PlotSettings()");
		printLine("plot_settings.plot_mode = PlotSettings.PLOT_FULL");
		printLine("plot_settings.xdim = " + xDim);
		printLine("plot_settings.ydim = " + yDim);
		printNewline();

		double step = config.settings.spaceExConfig.samplingTime;
		double maxTime = config.settings.spaceExConfig.timeHorizon;

		printLine("settings = HylaaSettings(step=" + step + ", max_time=" + maxTime
				+ ", plot_settings=plot_settings)");
		printNewline();

		printLine("engine = HylaaEngine(ha, settings)");
		printLine("engine.run(init)");
		decreaseIndentation();
		printNewline();

		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("main()");
		decreaseIndentation();
		printNewline();
	}

	@Override
	public String getToolName()
	{
		return "Hylaa";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "hylaa";
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public String getExtension()
	{
		return ".py";
	}
}
