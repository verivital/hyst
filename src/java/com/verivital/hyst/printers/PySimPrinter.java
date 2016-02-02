/**
 * 
 */
package com.verivital.hyst.printers;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;


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
	
	@Override
	protected String createCommentText(String text)
	{
		return "'''\n" + text + "\n'''";
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
		printLine("import hybridpy.pysim.simulate as sim");
		printLine("from hybridpy.pysim.hybrid_automaton import HybridAutomaton");
		printLine("from hybridpy.pysim.hybrid_automaton import HyperRectangle");
				printLine("from hybridpy.pysim.simulate import init_list_to_q_list");
		
		printNewline();
		
		printLine("def define_ha():");
		increaseIndentation();
		printLine("'''make the hybrid automaton, simulate it, and return the result'''");
		printComment("Variable ordering: " + ha.variables);
		printLine("ha = HybridAutomaton()");
		printModes();
		printJumps();
		printNewline();
		printLine("return ha");
		decreaseIndentation();
		printNewline();
		
		printLine("def define_init_states(ha):");
		increaseIndentation();
		printLine("'''returns a list of (mode, HyperRectangle)'''");
		printInit();
		decreaseIndentation();
		printNewline();
		
		printLine("def simulate(max_time=" + getTimeParam() + "):");
		increaseIndentation();
		printLine("'''simulate the automaton from each initial rect'''");
		printSimulate();
		decreaseIndentation();
		printNewline();
		
		printLine("def plot(result, filename='plot.png', dim_x=1, dim_y=0):");
		increaseIndentation();
		printLine("'''plot a simulation result to a file'''");
		printPlot();
		decreaseIndentation();
		printNewline();
		
		// check if main module
		printLine("if __name__ == '__main__':");
		increaseIndentation();
		printLine("plot(simulate())");
		decreaseIndentation();
		printNewline();
	}
	
	private void printInit()
	{
		/*
		# Variable ordering: [x, t, tglobal]
	    rv = []
	
	    r = HyperRectangle([(4.5, 5.5), (0.0, 0.0), (0.0, 0.0)])
	    rv.append((ha.modes['loc1'], r))
	    
	    r = HyperRectangle([(7.5, 8.5), (0.0, 0.0), (0.0, 0.0)])
	    rv.append((ha.modes['loc2'], r))
	    
	    return rv
		 */
		printComment("Variable ordering: " + ha.variables);
		printLine("rv = []");
		printNewline();
		
		for (Entry<String, Expression> e : config.init.entrySet())
		{
			String modeName = e.getKey();
			Expression exp = e.getValue();
			
			try
			{
				printHyperRectangleFromInitExpression(exp);
			}
			catch (AutomatonExportException exception)
			{
				throw new AutomatonExportException("Error printing initial states in mode " + modeName,
						exception);
			}
			
			printLine("rv.append((ha.modes['" + modeName + "'], r))");
			printNewline();
		}
		
		printLine("return rv");
	}
	
	private void printHyperRectangleFromInitExpression(Expression exp)
	{
		// r = HyperRectangle([(4.5, 5.5), (0.0, 0.0), (0.0, 0.0)])
		StringBuilder sb = new StringBuilder("r = HyperRectangle([");
		
		TreeMap <String, Interval> ranges = new TreeMap <String, Interval>();
		
		try
		{
			RangeExtractor.getVariableRanges(exp, ranges);
		} 
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} 
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} 
		
		for (String s : ha.variables)
		{
			if (s != ha.variables.get(0))
				sb.append(", ");
			
			Interval i = ranges.get(s);
			
			if (i == null)
				throw new AutomatonExportException("Initial range for variable " + s + " was not defined");
			
			sb.append("(" + doubleToString(i.min) + ", " + doubleToString(i.max) + ")");
		}
		
		sb.append("])");
		printLine(sb.toString());
	}

	private void printSimulate()
	{
		/*
		ha = define_ha()
    	init_states = define_init_states(ha)
    	q_list = init_list_to_q_list(init_states, center=True, star=True, corners=False)
    	result = sim.simulate_multi(q_list, max_time)
    	
    	return result
		*/
		
		printNewline();
		printLine("ha = define_ha()");
		printLine("init_states = define_init_states(ha)");
		printLine("q_list = init_list_to_q_list(init_states, center=True, star=True, corners=False)");
		printLine("result = sim.simulate_multi(q_list, max_time)");
		printNewline();
		printLine("return result");
	}

	private void printPlot()
	{
		/*
		draw_events = len(result) == 1
    	sim.plot_sim_result_multi(result, dim_x, dim_y, filename, draw_events)
	    */
		
		printNewline();
		printLine("draw_events = len(result) == 1");
		printLine("sim.plot_sim_result_multi(result, dim_x, dim_y, filename, draw_events)");
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
			this.opNames.put(Operator.EQUAL, "==");
			this.opNames.put(Operator.AND, "and");
			this.opNames.put(Operator.OR, "or");
			this.opNames.put(Operator.POW, "**");
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
