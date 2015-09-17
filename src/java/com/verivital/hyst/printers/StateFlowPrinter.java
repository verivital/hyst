/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.verivital.hyst.printers;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;

/**
 * Printer for SLSF models. Based on Chris' Boogie printer.
 * @author Luan Nguyen
 */
public class StateFlowPrinter extends ToolPrinter
{
    public BaseComponent ha;
    
    /**
     * 
     */
    @Override
    protected void printAutomaton() 
    {
    	this.ha = (BaseComponent)config.root;
		Expression.expressionPrinter = new StateFlowExpressionPrinter();
		
		// remove this after proper support for multiple initial modes is added
		//if (ha.init.size() != 1)
		//	throw new AutomatonExportException("Flow* printer currently only supports single-initial-state models");
		
		printDocument(originalFilename);
    }
    
	/**
	 * This method starts the actual printing!
	 * Prepares variables etc. and calls printProcedure() to print the BPL code
	 */
	protected void printDocument(String originalFilename) 
	{
		//new SwapVariableNamesPass().run(ha, "t:clock:time:clock_time");

		printCommentblock(Hyst.TOOL_NAME + "\n" +
				"Hybrid Automaton in " + Hyst.TOOL_NAME + "\n" +
				"Converted from file: " + originalFilename + "\n" +
				"Command Line arguments: " + Hyst.programArguments);
		
		Expression.expressionPrinter = new StateFlowExpressionPrinter(); // TODO: move to constructor?
		
		// begin printing the actual program
		//printNewline();
		try {
			printProcedure(originalFilename);
		}
		catch (Exception ex) {
			
		}
	}
	
	/**
	 * Print the actual DReach code
	 */
	private void printProcedure(String originalFilename) throws MatlabConnectionException, MatlabInvocationException
	{
		//printVars();
		
		//printSettings(); // TODO: print to other file or command line call
		
		//printConstants();
		
		//printModes();
		
		//printInitialStates();
		
		//printGoalStates();
		
	    //Create a proxy, which we will use to control MATLAB
	    //MatlabProxyFactory factory = new MatlabProxyFactory();
	    //MatlabProxy proxy = factory.getProxy();
		
		// this will try to reconnect to existing session if possible
		MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();

		MatlabProxyFactory factory = new MatlabProxyFactory(options);

		MatlabProxy proxy = factory.getProxy();

	    //Display 'hello world' just like when using the demo
	    proxy.eval("disp('hello world'); disp('test123'); clock");
	    
	    // TODO: get rid of having to change paths, this is nasty
	    proxy.eval("cd ../spaceex2stateflow");
	    // TODO: fix paths in stateflow converter, just let them take an arbitrary relative path as input
	    // instead of this folder garbage, and this will then be consistent with the rest of the Hyst paths
	    // TODO: pull in actual example name from filename string
	    proxy.eval("SpaceExToStateflow('vanderpol.xml', 'vanderpol.cfg', '--folder', 'vanderpol', '-s')");
	    // TODO: figure out how to get the .mdl file contents as a string so we can pipe its output to stdout

	    //Disconnect the proxy from MATLAB
	    proxy.disconnect();
	    
	    //proxy = factory.getProxy();
	}

    @Override
    public String getCommandLineFlag()
    {
    	return "-stateflow";
    }

    @Override
    protected String getCommentCharacter() {
    	// the matlab comment character is %
    	// the commands in .mdl files are #
        return "#";
    }
    
    public Map <String, String> getDefaultParams()
	{
		LinkedHashMap <String, String> params = new LinkedHashMap <String, String>();
		
		params.put("time", "auto");
		params.put("step", "auto-auto");
		params.put("remainder", "1e-4");
		params.put("precondition", "auto");
		params.put("plot", "auto");
		params.put("orders", "3-8");
		params.put("cutoff", "1e-15");
		params.put("precision", "53");
		params.put("jumps", "99999999");
		params.put("print", "on");
		params.put("aggregation", "parallelotope");
		
		return params;
	}

    /**
     * 
     */
    public StateFlowPrinter() 
    {
    	
    }
    
    /**
     * Simplify an expression by substituting constants and then doing math simplification
     * @param e the original expression
     * @return the modified expression
     */
    /*
    public Expression simplifyExpression(Expression e)
    {
            ValueSubstituter vs = new ValueSubstituter(ha.constants);
            Expression subbed = vs.substitute(e);

            return SimplifyExpressionsPass.simplifyExpression(subbed);
    }
    /**
    * 
    * @return flow dynamics
    */
    public List<String> getFlowDynamics(BaseComponent ha) 
    {
    	//Expression.expressionPrinter = new StateFlowExpressionPrinter();

		List<String> flowDynamics = new  ArrayList<String>();
		for (Entry <String, AutomatonMode> e : ha.modes.entrySet()) 
		{
			AutomatonMode mode = e.getValue();
			
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet()) {
			    flowDynamics.add(entry.getKey() + "_dot = " + entry.getValue().asExpression().toString() + ";");                
			}
		}
		return flowDynamics;    
    }
    
    /**
    * 
    * @return modeName to id
    */
    public TreeMap<String,Integer> getID(BaseComponent ha) 
    {
            TreeMap<String,Integer> modeNamesToIds = new TreeMap<String, Integer>(); 
            int id = 1;
            for (String modeName : ha.modes.keySet()) 
			modeNamesToIds.put(modeName, id++);
            
            return modeNamesToIds;    
    }
    /** 
    * 
    * @return guard
    */
    public LinkedHashMap <AutomatonTransition, String> getGuardCondition(BaseComponent ha) 
    {
            LinkedHashMap <AutomatonTransition, String> guardCondition = new LinkedHashMap <AutomatonTransition, String>();
            for (AutomatonTransition t : ha.transitions)
		{			
			Expression guard = t.guard;
			
			if (!guard.equals(Constant.TRUE)) 
			{
				//guardCondition.add(guard.toString());
                            guardCondition.put(t, guard.toString());
			}
					
		}
        return guardCondition;    
    }
    /**
    * 
    * @return assignment
    */
   public LinkedHashMap <AutomatonTransition, String> getReset(BaseComponent ha) 
    {
            LinkedHashMap <AutomatonTransition, String> reset = new LinkedHashMap <AutomatonTransition, String>();
            String resetTmp = "";
            for (AutomatonTransition t : ha.transitions)
            {			
                    for (Entry<String, ExpressionInterval> e : t.reset.entrySet())
                    {
                        ExpressionInterval ei = e.getValue();
                        if (ei.getInterval() == null)
                        {
                                resetTmp =  resetTmp + e.getKey() + " = " + e.getValue().asExpression() + ";";
                        }
                        else
                        {
                                Hyst.log("Warning: interval semantics not supported in matlab for ExpressionInterval " + ei + "; choosing value in range");
                                Interval i = ei.getInterval();
   
                                // interval is nonnull and i.max is not equal to i.min, nondeterministic reset
                      
                                if (i.min != i.max)
                                {   
                                    if (i.min < 0)
                                    {
                                        Expression randInv = new Operation(Operator.ADD, new Variable(e.getKey() + " = " + e.getValue().getExpression() + i.min), new Variable("rand*(" + i.max +"-(" + i.min +"))"));
                                        resetTmp =  resetTmp + randInv +";";
                                    }
                                    else
                                    { 
                                        Expression randInv = new Operation(Operator.ADD, new Variable(e.getKey() + " = " + e.getValue().getExpression()), new Operation(Operator.ADD, new Constant(i.min), 
                                                            new Variable("rand*(" + i.max +" - "+ i.min +")")));
                                        resetTmp =  resetTmp + randInv +";";
                                    }
                                }
                                else
                                {
                                    resetTmp =  resetTmp + e.getKey() + " = " + e.getValue().getExpression()+ ";";
                                }                    
                        }
                    }	
                    reset.put(t, resetTmp);
                    resetTmp = "";
            }
            
            return reset;    
    }
    /**
    * 
    * @return initial conditions
    */
    public LinkedHashMap <String, String> getInitialCondition()
    {
        LinkedHashMap <String, String> init = new LinkedHashMap <String, String>();
        String str = config.init.keySet().iterator().next();
        init.put(str, "null");

        TreeMap <String, Interval> ranges = new TreeMap <String, Interval>();
        Expression iniVarExp = config.init.values().iterator().next();
        
        try
        {
        	RangeExtractor.getVariableRanges(iniVarExp, ranges);
        } 
        catch (EmptyRangeException e) // changed multicatch to maintain java 1.6 compatability
        {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
        }
        catch (ConstantMismatchException e)
        {
        	throw new AutomatonExportException(e.getLocalizedMessage(), e);
        }
        
        for (Entry<String, Interval> e : ranges.entrySet())
        {
                String varName = e.getKey();
                Interval inter = e.getValue();
                init.put(varName,Double.toString(inter.min));
        }
        
        if (!ha.constants.isEmpty()){
            for (Entry <String, Interval> e : ha.constants.entrySet()){
                init.put(e.getKey(),Double.toString(e.getValue().asConstant()));
            }
        }
        
        return init;
    }
    
    
    /**
     * custom printer for Stateflow expressions
     * @author tjohnson
     *
     */
 	public static class StateFlowExpressionPrinter extends DefaultExpressionPrinter
 	{
 		public StateFlowExpressionPrinter()
 		{
 			super();
 			
 			opNames.put(Operator.AND, "&&");
 			opNames.put(Operator.OR, "||");
 			opNames.put(Operator.EQUAL, "==");
 		}
 		/*
 		public String printOperation(Operation o)
 		{
 			String rv = null;
 			
 			Operator op = o.op;
 			
 			// dreach expects a mix of infix and prefix
 			switch (op) 
 			{
 				case MULTIPLY :
 				case DIVIDE :
 				case ADD :
 				case SUBTRACT : {
 					// default
 					rv = "(" + this.opNames.get(op) + ")";
 					break;
 				}
 				case EQUAL :
 				case LESS :
 				case GREATER :
 				case LESSEQUAL :
 				case GREATEREQUAL :
 				case NOTEQUAL : {
 					// infix
 					rv = "(" + o.getLeft() + " " + this.opNames.get(op) + " " + o.getRight() + ")";
 					break;
 				}
 				case NEGATIVE:
 					rv = "-" + o.children.get(0);
 					break;
 					
 					// TODO: do we need to override this?
 				default : {
 					rv = "(";
 					
					int i = o.children.size();
					for (Expression e : o.children) {
						if (i == 1) {
							rv += this.print(e);
							//rv += e;
						}
						else
						{
							rv += this.print(e) + " " + this.opNames.get(op) + " "; // infix
							//rv += e + " " + this.opNames.get(op) + " "; // infix
						}
						i--;
					}
					rv += ")";
 					break;
 				}
 			}
 			return rv;
 		}*/
 	}


    public String getFlowConditionExpression(Expression e)
    {
    	return e.toString();
    	
    	
    	/*
        String rv = "";
        if (e instanceof Operation) 
        {
                Operation o = (Operation) e;
                String op;
                switch (o.getOperator()) {
                case MULTIPLY :
                        op = " * ";
                        break;
                case DIVIDE :
                        op = " / ";
                        break;
                case ADD :
                        op = " + ";
                        break;
                case SUBTRACT :
                        op = " - ";
                        break;
                case EQUAL :
                        op = " == ";
                        break;
                case LESS :
                        op = " < ";
                        break;
                case GREATER :
                        op = " > ";
                        break;
                case LESSEQUAL :
                        op = " <= ";
                        break;
                case GREATEREQUAL :
                        op = " >= ";
                        break;
                case NOTEQUAL :
                        op = " != ";
                        break;
                case AND :
                        op = " && ";
                        break;
                case OR :
                        op = " || ";
                        break;
                default :
                        op = " ?? ";
                }

                rv = getFlowConditionExpression(o.getLeft()) +
                                op + getFlowConditionExpression(o.getRight());


                boolean printBraces = false;
                // check parantheses
                Operation parent = o.getParent();
                if (parent != null) {
                        printBraces = Operator.getPriority(parent.getOperator())
                                        > 
                                        Operator.getPriority(o.getOperator());
                }

                if (printBraces) {
                        return "(" + rv + ")";
                } else {
                        return rv;
                }
        }
        else{
            return "[NULL]";  
        }
        */
    }
    

	@Override
	public String getToolName()
	{
		return "Stateflow";
	}
	
	@Override
	public boolean isInRelease()
	{
		return false; // when the printer is stable, set to true
	}
}
   

