/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.printers.PySimPrinter.ExtraPrintFuncs;
import com.verivital.hyst.util.PreconditionsFlag;

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

			// add the symbolic invariant
			// String s = rrtSymbolicPyinter.print(am.invariant);
			// rv.add(am.name + ".inv_strings = [" + s + "]");

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
		return "-hylaa";
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

		// toolParams.put("time", "auto"); // unused currently
		// toolParams.put("step", "auto"); // unused currently
		// toolParams.put("jumps", "10");

		return toolParams;
	}

	@Override
	public String getExtension()
	{
		return ".py";
	}
}
