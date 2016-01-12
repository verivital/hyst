/**
 * 
 */
package com.verivital.hyst.printers;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.PreconditionsFlag;


/**
 * Printer for Python-based simulation models.
 * 
 * @author Stanley Bak (1-2015)
 *
 */
public class PySimPrinter extends ToolPrinter
{
	private BaseComponent ha;
	
	public PySimPrinter()
	{
		preconditions.skip(PreconditionsFlag.NO_URGENT); // skip the 'no urgent modes' check
	}
	
	@Override
	protected String getCommentPrefix()
	{
		return "# ";
	}
	
	/**
	 * This method starts the actual printing!
	 * Prepares variables etc. and calls printProcedure() to print the BPL code
	 */
	private void printDocument(String originalFilename) 
	{
		this.printCommentHeader();
		
		// begin printing the actual program
		printNewline();
		printProcedure();
	}
	
	/**
	 * Custom printing for comment blocks (used for header)
	 */
	protected void printCommentblock(String comment) 
	{
		String s = "'''\n" + comment + "\n'''";
		
		if (outputType == OutputType.STDOUT || outputType == OutputType.FILE)
			outputStream.println(s);
		else if (outputType == OutputType.GUI)
			outputFrame.addOutput(s);
	}
	
	private void printModes() 
	{
		for (AutomatonMode am : ha.modes.values())
		{
			printNewline();
			
			/*
			 one = ha.new_mode('one')
    		 one.der = lambda state, _: [2, 1]
             one.inv = lambda(x): x[0] <= 2
			*/
			
			printLine(am.name + " = ha.new_mode('" + am.name + "')");
			printLine(am.name + ".inv = lambda state: " + am.invariant);

			if (!am.urgent)
				printLine(am.name + ".der = lambda state, _: " + getMapString(am.flowDynamics));
		}
	}
	
	/**
	 * Gets a map string. Null values get mapped to the variable name
	 * if x' == x+1 and y' == y-x, this would give: '[x + 1,  y - x]'
	 * @param map
	 * @return the mapped string
	 */
	private String getMapString(Map <String, ExpressionInterval> map)
	{
		StringBuffer rv = new StringBuffer();
		rv.append("[");
		
		for (String var : ha.variables)
		{
			if (rv.length() > 1)
				rv.append(", ");
			
			ExpressionInterval ei = map.get(var);
			
			if (ei == null)
				rv.append("None");
			else
				rv.append(ei.getExpression());
		}
		
		rv.append("]");
		
		return rv.toString();
	}

	private void printJumps()
	{
		printNewline();
		
		/*
		t = ha.new_transition(one, two)
	    t.guard = lambda(x): x[0] >= 2
	    t.reset = lambda(x): (x[0] + 1, x[1]) 
		*/
		
		for (AutomatonTransition at : ha.transitions)
		{
			printNewline();
			
			printLine("t = ha.new_transition(" + at.from.name + ", " + at.to.name + ")");
			printLine("t.guard = lambda state: "+ at.guard);
			printLine("t.reset = lambda state: "+ getMapString(at.reset));
		}
	}
	
	/**
	 * Print the actual Flow* code
	 */
	private void printProcedure() 
	{
		printLine("import hybridpy.pysim.simulate as pysim");
		printLine("from hybridpy.pysim.hybrid_automaton import HybridAutomaton");
		printNewline();
		
		printLine("def simulate():");
		increaseIndentation();
		printLine("'''make the hybrid automaton, simulate it, and return the result'''");
		printComment("Variable ordering: " + ha.variables);
		printLine("ha = HybridAutomaton()");
		printModes();
		printJumps();
		printSimulate();
		printLine("return result");
		decreaseIndentation();
		printNewline();
		
		printLine("def plot(result, filename='plot.png'):");
		increaseIndentation();
		printPlot();
		decreaseIndentation();
		printNewline();
		
		// check if main module
		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("plot(simulate())");
		decreaseIndentation();
	}
	
	private void printSimulate()
	{
		/*
		init = [1, 0]
    	init_mode = 'one'
    	max_time = 5.0
    	result = ha.simulate(init, init_mode, max_time)
		*/
		
		printNewline();
		HyperPoint initPt = AutomatonUtil.getInitialPoint(ha, config);
		printLine("init = [" + join(", ", initPt.dims) + "]");
		printLine("init_mode = '" + config.init.keySet().iterator().next() + "'");
		printLine("max_time = " + getTimeParam());
		printLine("result = pysim.simulate(ha, init, init_mode, max_time)");
		printNewline();
	}
	
	private String join(String sep, double[] vals)
	{
		StringBuffer rv = new StringBuffer();
		
		for (double d : vals)
		{
			if (rv.length() > 0)
				rv.append(sep);
			
			rv.append(d);
		}
		
		return rv.toString();
	}

	private void printPlot()
	{
		/*
		dim_x = 2
    	dim_y = 3
    	axis_range = [-0.02, 0, -0.03, 0.02]

    	plot_sim_result(result, 'plot.png', dim_x, dim_y, axis_range, draw_func=draw)
	    */
		
		int xIndex = ha.variables.indexOf(config.settings.plotVariableNames[0]);
		int yIndex = ha.variables.indexOf(config.settings.plotVariableNames[1]);
		
		printLine("dim_x = " + xIndex);
		printLine("dim_y = " + yIndex);
		printLine("pysim.plot_sim_result(result, filename, dim_x, dim_y)");
	}

	private String getTimeParam()
	{
		String value = toolParams.get("time");
		
		if (value.equals("auto"))
			value = doubleToString(config.settings.spaceExConfig.timeHorizon);
		
		return value;
	}

	@Override
	protected void printAutomaton()
	{	
		this.ha = (BaseComponent)config.root;
		Expression.expressionPrinter = new PySimExpressionPrinter();

		printDocument(originalFilename);
	}
	
	private class PySimExpressionPrinter extends DefaultExpressionPrinter
	{
		ArrayList <String> vars = ha.variables;
		Map<String, Interval> constants = ha.constants;
		String BASE = "state";
		
		public PySimExpressionPrinter()
		{
			this.opNames.put(Operator.AND, "and");
			this.opNames.put(Operator.OR, "or");
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
		
		@Override
		protected String printVariable(Variable v)
		{
			String rv = null;
			String name = v.name;
			
			int index = vars.indexOf(name);
			
			if (index == -1)
			{
				Interval value = constants.get(name);
				
				if (value == null)
					throw new AutomatonExportException("PySimPrinter tried to " +
						"print variable/constant not found in base component: '" + name + "'");
				
				rv = "" + value.middle();
			}
			else
				rv = BASE + "[" + index + "]";
			
			return rv;
		}
	}

	@Override
	public String getToolName()
	{
		return "PySim";
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-pysim";
	}
	
	@Override
	public boolean isInRelease()
	{
		return true;
	}
	
	public Map <String, String> getDefaultParams()
	{
		LinkedHashMap <String, String> toolParams = new LinkedHashMap <String, String>();
		
		toolParams.put("time", "auto");
		toolParams.put("step", "auto");

		return toolParams;
	}
	
	@Override
	public String getExtension()
	{
		return ".py";
	}
}
