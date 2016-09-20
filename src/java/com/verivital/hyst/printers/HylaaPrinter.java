/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.printers.PySimPrinter.ExtraPrintFuncs;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

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

	public static class HylaaExtraPrintFuncs extends ExtraPrintFuncs
	{
		@Override
		public ArrayList<String> getImportLines(BaseComponent ha)
		{
			ArrayList<String> rv = new ArrayList<String>();

			rv.add("from hylaa.hybrid_automaton import HybridAutomaton");
			rv.add("from hylaa.hybrid_automaton import HyperRectangle");
			rv.add("import hylaa.engine as hylaa");

			return rv;
		}

		@Override
		public ArrayList<String> getExtraModePrintLines(AutomatonMode am)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// add the jacobian
			rv.add("def jac(_. state):");
			rv.add("    'symbolic jacobian'\n");

			rv.add("    return [");

			for (String row : am.automaton.variables)
			{
				Expression der = am.flowDynamics.get(row).asExpression();

				StringBuffer line = new StringBuffer();
				line.append("           [");

				for (String col : am.automaton.variables)
				{
					Expression e = null;

					try
					{
						// find variable 'col' in expression 'der'
						e = findMultiplier(col, der);
					}
					catch (AutomatonExportException ex)
					{
						throw new PreconditionsFailedException(
								"Error extracting linear dynamics for variable '" + col
										+ "' in derivative expression: '" + der.toDefaultString()
										+ "'",
								ex);
					}

					if (e != null)
					{
						double val = AutomatonUtil.evaluateConstant(e);
						line.append("" + val + ", ");
					}
					else
						line.append("0, ");

				}

				line.append("],");

				rv.add(line.toString());
			}

			rv.add("    ]");

			return rv;
		}

		private Expression findMultiplier(String varName, Expression summation)
		{
			Expression rv = null;

			if (summation instanceof Operation)
			{
				Operation o = summation.asOperation();
				Operator op = o.op;

				if (op == Operator.NEGATIVE)
				{
					rv = findMultiplier(varName, o.children.get(0));

					if (rv != null)
						rv = new Operation(Operator.NEGATIVE, rv);
				}
				else if (op == Operator.MULTIPLY)
				{
					Expression left = o.getLeft();
					Expression right = o.getRight();

					if (left instanceof Variable && right instanceof Variable)
						throw new AutomatonExportException(
								"Unsupported variable-variable term in linear derivative: '"
										+ o.toDefaultString() + "'");
					else if (left instanceof Variable)
					{
						if (((Variable) left).name.equals(varName))
							rv = right;
					}
					else if (right instanceof Variable)
					{
						if (((Variable) right).name.equals(varName))
							rv = left;
					}
					else if (left instanceof Constant && right instanceof Constant)
					{
						// allowed, doesn't affect rv
					}
					else
						throw new AutomatonExportException(
								"Unsupported term in linear derivative: '" + o.toDefaultString()
										+ "'");
				}
				else if (op == Operator.ADD || op == Operator.SUBTRACT)
				{
					Expression left = o.getLeft();
					Expression right = o.getRight();

					Expression leftRv = findMultiplier(varName, left);
					Expression rightRv = findMultiplier(varName, right);

					if (leftRv != null && rightRv != null)
						throw new AutomatonExportException("Unsupported term in linear derivative ("
								+ varName + " in multiple places): " + summation.toDefaultString());
					else if (leftRv != null)
						rv = leftRv;
					else if (rightRv != null)
						rv = rightRv;

					if (rv != null && op == Operator.SUBTRACT)
						rv = new Operation(Operator.NEGATIVE, rv);
				}
				else
					throw new AutomatonExportException(
							"Unsupported operation in linear derivative (expecting +/-/*): '"
									+ o.toDefaultString());
			}
			else if (summation instanceof Constant)
			{
				// allowed, doesn't affect things
			}
			else if (summation instanceof Variable)
			{
				// maybe variable by itself

				Variable v = (Variable) summation;

				if (v.name.equals(varName))
					rv = new Constant(1);
			}
			else
				throw new AutomatonExportException(
						"Unsupported expression type (" + summation.getClass()
								+ ") in linear derivative (expecting sum of multiples): '"
								+ summation.toDefaultString() + "'");

			return rv;
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

	@Override
	protected void printAutomaton()
	{
		new SimplifyExpressionsPass().runVanillaPass(config, "");

		this.printCommentHeader();

		printNewline();

		printLine(PySimPrinter.automatonToString(config, new HylaaExtraPrintFuncs()));

		int xDim = config.root.variables.indexOf(config.settings.plotVariableNames[0]);
		int yDim = config.root.variables.indexOf(config.settings.plotVariableNames[1]);

		printLine("def run(dim_x=" + xDim + ", dim_y=" + yDim + "):");
		increaseIndentation();
		printLine("'runs hylaa on the model and returns a result object'");
		printLine("ha = define_ha()");
		printLine("return hylaa.run(dim_x, dim_y, define_init_states(ha), ha)");
		decreaseIndentation();
		printNewline();

		printLine("def plot(nodes, image_path):");
		increaseIndentation();
		printLine("'plot a result object produced by run()'");
		printLine("pass");
		decreaseIndentation();
		printNewline();

		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("plot(run(), 'out.png')");
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
