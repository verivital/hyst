/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.printers.PySimPrinter.ExtraPrintFuncs;
import com.verivital.hyst.util.PreconditionsFlag;

/**
 * Printer for Python-based hybrid automata RRT.
 * 
 * @author Stanley Bak (5-2016)
 *
 */
public class PyRrtPrinter extends ToolPrinter
{
	private static RrtSymbolicPrinter rrtSymbolicPyinter = new RrtSymbolicPrinter();

	private static final String COMMENT_CHAR = "#";

	public PyRrtPrinter()
	{
		preconditions.skip(PreconditionsFlag.NO_URGENT); // skip the no-urgent-modes check
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

	public static class PyRrtExtraPrintFuncs extends ExtraPrintFuncs
	{
		@Override
		public ArrayList<String> getExtraModePrintLines(AutomatonMode am)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// add the symbolic invariant
			String s = rrtSymbolicPyinter.print(am.invariant);
			rv.add(am.name + ".inv_strings = [" + s + "]");

			return rv;
		}

		@Override
		public ArrayList<String> getExtraTransitionPrintLines(AutomatonTransition at)
		{
			ArrayList<String> rv = new ArrayList<String>();

			// add the symbolic guard
			String s = rrtSymbolicPyinter.print(at.guard);
			rv.add("t.inv_strings = [" + s + "]");

			return rv;
		}
	}

	@Override
	protected void printAutomaton()
	{
		this.printCommentHeader();

		printNewline();

		printLine(PySimPrinter.automatonToString(config, new PyRrtExtraPrintFuncs()));

	}

	private static class RrtSymbolicPrinter extends DefaultExpressionPrinter
	{
		public RrtSymbolicPrinter()
		{
			this.opNames.put(Operator.EQUAL, "==");
			this.opNames.put(Operator.AND, "and");
			this.opNames.put(Operator.OR, "or");
			this.opNames.put(Operator.POW, "**");
		}

		@Override
		protected String printOperation(Operation o)
		{
			String s = null;

			if (o.op == Operator.AND)
				s = "\"" + print(o.getLeft()) + "\", \"" + print(o.getRight()) + "\", ";
			else if (o.op == Operator.OR)
				throw new AutomatonExportException("-or- operation not allowed by printer");
			else
				s = super.printOperation(o);

			return s;
		}

		@Override
		protected String printTrue()
		{
			return "True";
		}

		@Override
		protected String printFalse()
		{
			return "False";
		}
	}

	@Override
	public String getToolName()
	{
		return "PyRrt";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "-pyrrt";
	}

	@Override
	public boolean isInRelease()
	{
		return true;
	}

	@Override
	public Map<String, String> getDefaultParams()
	{
		LinkedHashMap<String, String> toolParams = new LinkedHashMap<String, String>();

		toolParams.put("time", "auto");
		toolParams.put("step", "auto");
		toolParams.put("legend", "True");
		toolParams.put("center", "True");
		toolParams.put("star", "True");
		toolParams.put("corners", "False");
		toolParams.put("rand", "0");
		toolParams.put("title", "Simulation");

		return toolParams;
	}

	@Override
	public String getExtension()
	{
		return ".py";
	}
}
