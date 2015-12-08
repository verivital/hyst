/**
 * 
 */
package com.verivital.hyst.printers;



import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;


/**
 * Printer for Flow* models. Based on Chris' Boogie printer.
 * 
 * @author Stanley Bak (8-2014)
 *
 */
public class FlowPrinter extends ToolPrinter
{
	private BaseComponent ha;
	private int DEFAULT_MAX_JUMPS = 999999999;
	
	public FlowPrinter()
	{
		preconditions.skip[PreconditionsFlag.NO_URGENT.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS.ordinal()] = true;
	}
	
	@Override
	protected String getCommentCharacter()
	{
		return "#";
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
	 * Simplify an expression by substituting constants and then doing math simplification
	 * @param e the original expression
	 * @return the modified expression
	 */
	private Expression simplifyExpression(Expression ex)
	{
		Expression subbed = new SubstituteConstantsPass().substituteConstantsIntoExpression(ha.constants, ex);
		
		return AutomatonUtil.simplifyExpression(subbed);
	}
	
	/**
	 * Print the actual Flow* code
	 */
	private void printProcedure() 
	{
		printLine("hybrid reachability");
		printLine("{");
		printVars();
		
		printSettings();
		
		printModes();
		
		printJumps();
		printInitialStates();
		
		printLine("}");
		
		printForbidden();
	}

	private void printForbidden()
	{
		if (config.forbidden.size() > 0)
		{
			printLine("");
			printLine("unsafe set");
			printLine("{");
			
			for (Entry<String, Expression> e : config.forbidden.entrySet())
			{
				String expString = "";
				Expression exp = e.getValue();
				
				if (exp != null)
					expString = getFlowConditionExpression(exp);
				
				printLine(e.getKey() + " {" + expString + "}");
			}
			
			printLine("}");
		}
	}

	private void printSettings()
	{
		printNewline();
		printLine("setting");
		printLine("{");
		
		String[] step = getStepParam();
		
		if (step.length == 1 || step.length == 2 && step[0].equals(step[1]))
			printLine("fixed steps " + step[0]);
		else if (step.length == 2)
			printLine("adaptive steps { min " + step[0] + ", max " + step[1] + " }");
		else
			throw new AutomatonExportException("Param 'step' should have one or two entries: " + toolParams.get("step"));
		
		printLine("time " + getTimeParam());
		
		printLine("remainder estimation " + toolParams.get("remainder"));
		
		if (toolParams.get("precondition").equals("auto"))
		{
			// follow recommendation in 1.2 manual
			if (ha.variables.size() > 3)
				printLine("identity precondition");
			else
				printLine("QR precondition");
		}
		else
			printLine(toolParams.get("precondition") + " precondition");
		
		
		printLine(getPlotParam());
		
		String[] order = getOrderParam();
		
		if (order.length == 1 || order.length == 2 && order[0].equals(order[1]))
			printLine("fixed orders " + order[0]);
		else if (order.length == 2)
			printLine("adaptive orders { min " + order[0] + ", max " + order[1] + " } ");
		else
			throw new AutomatonExportException("Param 'orders' should have one or two entries: " + toolParams.get("orders"));
		
		printLine("cutoff " + toolParams.get("cutoff"));
		printLine("precision " + toolParams.get("precision"));
		printLine("output out");
		
		int jumps = Integer.parseInt(toolParams.get("jumps"));
		
		if (jumps == DEFAULT_MAX_JUMPS && config.settings.spaceExConfig.maxIterations > 0)
			jumps = config.settings.spaceExConfig.maxIterations;
				
		printLine("max jumps " + jumps);
		printLine("print on");
		printLine("}");
	}

	private String[] getOrderParam()
	{
		return toolParams.get("orders").split("-");
	}

	private String getTimeParam()
	{
		String value = toolParams.get("time");
		
		if (value.equals("auto"))
			value = doubleToString(config.settings.spaceExConfig.timeHorizon);
		
		return value;
	}
	
	private String getPlotParam()
	{
		String auto = "gnuplot octagon";
		String value = toolParams.get("plot");
		
		if (value.equals("auto"))
			value = auto;
		
		if (value.equals("gnuplot octagon") || value.equals("gnuplot interval") 
				|| value.equals("matlab interval") || value.equals("matlab octagon"))
			value = value + " " + config.settings.plotVariableNames[0] + "," + config.settings.plotVariableNames[1];
		
		return value;
	}

	private String[] getStepParam()
	{
		String value = toolParams.get("step");
		
		if (value.contains("auto"))
		{
			String autoVal = doubleToString(config.settings.spaceExConfig.samplingTime);
			value = value.replace("auto", autoVal);
		}
		
		return value.split("-");
	}

	/**
	 * Print variable declarations and their initial value assignments
	 * plus a list of all constants
	 */
	private void printVars() 
	{
		printLine("# Vars");

		String varLine = "state var ";
		
		boolean first = true;
		
		for (String v : ha.variables)
		{
			if (first)
				first = false;
			else
				varLine += ", ";
			
			varLine += v;
		}
		
		printLine(varLine);
	}
	
	
	
	/**
	 * Print initial states
	 */
	private void printInitialStates() {
		printNewline();
		printLine("init");
		printLine("{");
		
		for (Entry<String, Expression> e : config.init.entrySet())
		{
			printLine(e.getKey());
			printLine("{");
			printFlowRangeConditions(removeConstants(e.getValue(), ha.constants.keySet()), true);
			printLine("}"); // end  mode
		}
		
		printLine("}"); // end all initial modes
	}

	private static Expression removeConstants(Expression e, Collection <String> constants)
	{
		Operation o = e.asOperation();
		Expression rv = e;
		
		if (o != null)
		{
			if (o.op == Operator.AND)
			{
				Operation rvO = new Operation(Operator.AND);
				
				for (Expression c : o.children)
					rvO.children.add(removeConstants(c, constants));
				
				rv = rvO;
			}
			else if (o.op == Operator.EQUAL)
			{
				Expression left = o.getLeft();
				
				if (left instanceof Variable && constants.contains(((Variable)left).name))
					rv = Constant.TRUE;
			}
		}
		
		return rv;
	}

	/**
	 * Prints the locations with their labels and everything that happens
	 * in them (invariant, flow...)
	 */
	private void printModes() 
	{
		printNewline();
		printLine("modes");
		printLine("{");

		// modename
		boolean first = true;
		
		for (Entry <String, AutomatonMode> e : ha.modes.entrySet()) 
		{
			AutomatonMode mode = e.getValue();
			
			if (first)
				first = false;
			else
				printNewline();
			
			String locName = e.getKey();
			printLine(locName);
			printLine("{");
			
			// From Xin Chen e-mail:
			// linear ode - linear time-invariant, can also have uncertain input
			// "poly ode 1" works more efficient than the others on low degree and low dimension (<=3) ODEs.
			// "poly ode 2" works more efficient than the others on low degree and medium dimension (4~6) ODEs.
			// "poly ode 3" works more efficient than the others on medium or high degree and high dimension ODEs.
			// "nonpoly ode" works with nonlinear terms
			
			if (isNonLinearDynamics(mode.flowDynamics))
				printLine("nonpoly ode");
			else if (isLinearDynamics(mode.flowDynamics))
				printLine("linear ode");
			else if (ha.variables.size() <= 3)
				printLine("poly ode 1");
			else if (ha.variables.size() <= 6)
				printLine("poly ode 2");
			else
				printLine("poly ode 3");
			
			printLine("{");
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet())
			{
				ExpressionInterval ei = entry.getValue();
				ei.setExpression(simplifyExpression(ei.getExpression()));
				
				// be explicit (even though x' == 0 is implied by Flow*)
				printLine(entry.getKey() + "' = " + ei);
			}
			printLine("}");
			
			// invariant
			printLine("inv");
			printLine("{");
			
			Expression inv = simplifyExpression(mode.invariant);
			
			if (!inv.equals(Constant.TRUE))
			{
				printCommentblock("Original invariant: " + inv);
				printLine(getFlowConditionExpression(inv));
			}

			printLine("}"); // end invariant
			
			
			printLine("}"); // end individual mode
		}
		
		printLine("}"); // end all modes
	}
	
	private boolean isNonLinearDynamics(LinkedHashMap<String, ExpressionInterval> flowDynamics)
	{
		boolean rv = false;
		
		for (ExpressionInterval entry : flowDynamics.values())
		{
			Expression e = entry.getExpression();
			
			byte classification = AutomatonUtil.classifyExpressionOps(e);
			
			if ((classification | AutomatonUtil.OPS_NONLINEAR) != 0)
			{
				rv = true;
				break;
			}
		}
		
		return rv;
	}

	private boolean isLinearDynamics(LinkedHashMap<String, ExpressionInterval> flowDynamics)
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
						numVars += count;
					else
					{
						rv = false;
						break;
					}
				}
				
				if (numVars > 1)
					rv = false;
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
				rv = isLinearExpression(o.children.get(0));
			else
				rv = false;
		}
		
		return rv;
	}

	/**
	 * Recursively count the number of variables. only recurse if we have
	 * multiplication, or negation, otherwise return Integer.MAX_VALUE
	 * @param e the expression
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
						rv = Integer.MAX_VALUE;
					else
						rv += count;
				}
			}
			else
				rv = Integer.MAX_VALUE;
		}
		else if (e instanceof Variable)
			rv = 1;
		
		return rv;
	}

	public static String getFlowConditionExpression(Expression e)
	{
		String rv = null;
		
		try
		{
			rv = getFlowConditionExpressionRec(e);
		}
		catch (AutomatonExportException ex)
		{
			throw new AutomatonExportException("Error with expression:" + e , ex);
		}
		
		return rv;
	}

	private static String getFlowConditionExpressionRec(Expression e)
	{
		String rv = "";
		// replace && with '   ' and then print as normal
		
		if (e instanceof Operation)
		{
			Operation o = (Operation)e;
			
			if (o.op == Operator.AND)
			{
				rv += getFlowConditionExpressionRec(o.getLeft());
				rv += "   ";
				rv += getFlowConditionExpressionRec(o.getRight());
			}
			else if (o.op == Operator.EQUAL)
			{
				rv += getFlowConditionExpressionRec(o.getLeft());
				rv += " = ";
				rv += getFlowConditionExpressionRec(o.getRight());
			}
			else if (o.op == Operator.OR)
			{
				throw new AutomatonExportException("Flow* printer doesn't support OR operator. " +
						"Consider using a Hyst pass to eliminate disjunctions)");
			}
			else if (Operator.isComparison(o.op))
			{
				Operator op = o.op;
				
				// Flow doesn't like < or >... needs <= or >=
				if (op.equals(Operator.GREATER) || op.equals(Operator.LESS) || op.equals(Operator.NOTEQUAL))
					throw new AutomatonExportException("Flow* printer doesn't support operator " + op);

				// make sure it's of the form p ~ c
				if (o.children.size() == 2 && o.getRight() instanceof Constant)
					rv = e.toString();
				else
				{
					// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
					
					rv += getFlowConditionExpression(o.getLeft());
					rv += " - (" + getFlowConditionExpression(o.getRight());
					rv += ") " + Expression.expressionPrinter.printOperator(op) + " 0";
				}
			}
			else
				rv = e.toString();
		}
		else 
			rv = e.toString();
		
		return rv;
	}
	
	private void printFlowRangeConditions(Expression ex, boolean isAssignment)
	{
		TreeMap <String, Interval> ranges = new TreeMap <String, Interval>();
		
		try
		{
			RangeExtractor.getVariableRanges(ex, ranges);
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
		
		for (Entry<String, Interval> e : ranges.entrySet())
		{
			String varName = e.getKey();
			Interval inter = e.getValue();
			
			if (isAssignment)
				printLine(varName + " in [" + 
						doubleToString(inter.min) + ", " + 
						doubleToString(inter.max) + "]");
			else
			{
				// it's a comparison
				
				if (inter.min == inter.max)
					printLine(varName + " = " + doubleToString(inter.min));
				else 
				{
					if (inter.min != -Double.MAX_VALUE)
						printLine(varName + " >= " + doubleToString(inter.min));
				
					if (inter.max != Double.MAX_VALUE)
						printLine(varName + " <= " + doubleToString(inter.max));
				}
			}
		}
	}

	private void printJumps()
	{
		printNewline();
		printLine("jumps");
		printLine("{");
		
		boolean first = true;
		
		for (AutomatonTransition t : ha.transitions)
		{
			Expression guard = simplifyExpression(t.guard);
			
			if (guard == Constant.FALSE)
				continue;
			
			if (first)
				first = false;
			else
				printNewline();
			
			String fromName = t.from.name;
			String toName = t.to.name;
			
			printLine(fromName + " -> " + toName);
			printLine("guard");
			printLine("{");
			
			if (!guard.equals(Constant.TRUE))
			{
				printCommentblock("Original guard: " + t.guard);
				printLine(getFlowConditionExpression(guard));
			}
			
			printLine("}");
			
			printLine("reset");
			printLine("{");
			
			for (Entry<String, ExpressionInterval> e : t.reset.entrySet())
			{
				ExpressionInterval ei = e.getValue();
				ei.setExpression(simplifyExpression(ei.getExpression()));
				printLine(e.getKey() + "' := " + ei);
			}
			
			printLine("}");
			
			if (toolParams.get("aggregation").equals("parallelotope"))
				printLine("parallelotope aggregation {}");
			else if (toolParams.get("aggregation").equals("interval"))
				printLine("interval aggregation");
			else
				throw new AutomatonExportException("Unknown aggregation method: " + toolParams.get("aggregation"));
		}
		
		printLine("}");
	}
	
	@Override
	protected void printAutomaton()
	{	
		this.ha = (BaseComponent)config.root;
		Expression.expressionPrinter = DefaultExpressionPrinter.instance;

		if (ha.modes.containsKey("init"))
			throw new AutomatonExportException("mode named 'init' is not allowed in Flow* printer");
		
		if (ha.modes.containsKey("start"))
			throw new AutomatonExportException("mode named 'start' is not allowed in Flow* printer");
		
		if (config.init.size() > 1)
		{
			Hyst.log("Multiple initial modes detected (not supported by Flow*). Converting to single urgent one.");
			convertInitialModes(config);
		}
		
		AutomatonUtil.convertUrgentTransitions(ha, config);
		
		printDocument(originalFilename);
	}

	/**
	 * Use urgent modes to convert a configuration with multiple initial modes to one with a single initial mode
	 * @param c the configuration to convert
	 */
	public static void convertInitialModes(Configuration c)
	{
		final String INIT_NAME = "_init";
		BaseComponent ha = (BaseComponent)c.root;
		Collection <String> constants = ha.constants.keySet();
		
		AutomatonMode init = ha.createMode(INIT_NAME);
		init.invariant = Constant.TRUE;
		init.urgent = true;
		init.flowDynamics = null;
		
		for (Entry<String, Expression> e : c.init.entrySet())
		{
			String modeName = e.getKey();
			AutomatonTransition at = ha.createTransition(init, ha.modes.get(modeName)); 
			at.guard = Constant.TRUE;
			
			Expression resetExp = removeConstants(e.getValue(), constants);
			
			TreeMap <String, Interval> ranges = new TreeMap <String, Interval>(); 
			try
			{
				RangeExtractor.getVariableRanges(e.getValue(), ranges);
			} 
			catch (EmptyRangeException e1)
			{
				throw new AutomatonExportException("Empty range in initial mode: " + modeName, e1);
			}
			catch (ConstantMismatchException e2)
			{
				throw new AutomatonExportException("Constant mismatch in initial mode: " + modeName, e2);
			}
			catch (UnsupportedConditionException e2)
			{
				throw new AutomatonExportException("Non-box initial mode: " + modeName, e2);
			} 
			
			Collection <String> vars = AutomatonUtil.getVariablesInExpression(resetExp);
			
			for (String var : vars)
			{
				Interval i = ranges.get(var);
				
				if (i == null)
					throw new AutomatonExportException("Variable " + var + " not defined in initial mode " + modeName);
				
				at.reset.put(var, new ExpressionInterval(new Constant(0), i));
			}
		}
		
		Expression firstReachableState = c.init.values().iterator().next();
		c.init.clear();
		c.init.put(INIT_NAME, firstReachableState);
		
		c.validate();
	}

	@Override
	public String getToolName()
	{
		return "Flow*";
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-flowstar";
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
		toolParams.put("step", "auto-auto");
		toolParams.put("remainder", "1e-4");
		toolParams.put("precondition", "auto");
		toolParams.put("plot", "auto");
		toolParams.put("orders", "3-8");
		toolParams.put("cutoff", "1e-15");
		toolParams.put("precision", "53");
		toolParams.put("jumps", "" + DEFAULT_MAX_JUMPS);
		toolParams.put("print", "on");
		toolParams.put("aggregation", "parallelotope");


		return toolParams;
	}
	
	@Override
	public String getExtension()
	{
		return ".flowstar";
	}
}
