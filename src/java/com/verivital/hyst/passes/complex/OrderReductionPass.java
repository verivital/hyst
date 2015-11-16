package com.verivital.hyst.passes.complex;



import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.printers.StateflowSpPrinter;
import com.verivital.hyst.util.Classification;
import java.util.Map.Entry;
import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;


/**
 * Perform order reduction
 * 
 * @author Taylor Johnson (October 2015)
 *
 */
public class OrderReductionPass extends TransformationPass
{
	private int reducedOrder;
        
	
	@Override
	public String getName()
	{
		return "Order Reduction (decrease dimensionality) Pass";
	}
	
	@Override
	public String getParamHelp()
	{
		return "[<reduced order dimensionality>]";
	}
	
	@Override
	public String getCommandLineFlag()
	{
		return "-order_reduction";
	}

	@Override
	protected void runPass(String params)
	{
		BaseComponent ha = (BaseComponent)config.root;
		
		processParams(ha, params);
		
		
		//Create a proxy, which we will use to control MATLAB
        //MatlabProxyFactory factory = new MatlabProxyFactory();
        //MatlabProxy proxy = factory.getProxy();

        // this will try to reconnect to existing session if possible
        MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder().setUsePreviouslyControlledSession(true).build();

        MatlabProxyFactory factory = new MatlabProxyFactory(options);
        
        Classification cf = new Classification();
        cf.ha = ha;
        cf.setVarID(ha); 
        StateflowSpPrinter sp = new StateflowSpPrinter();
        sp.ha = ha;
        sp.setConfig(config);
        //sp.setVarID(ha); 
        MatlabProxy proxy;
		try {
			proxy = factory.getProxy();
			
	        proxy.eval("disp('hello world'); disp('test123'); clock");
                proxy.eval("[path_parent,path_current] = fileparts(pwd)");
                proxy.eval("if ~strcmp(path_current, 'pass_order_reduction') cd ./matlab/pass_order_reduction; end");

	        // currently, only support for continuous linear dynamics
	        // todo: refactor, move to stateflow printer
	        // todo: test to ensure order of this vector is the same as the matrix below, could be ensured by construction if done in matrix construction function
                for (Entry <String, AutomatonMode> e : ha.modes.entrySet()) {
                        // declare x variable
                        String variableString = "";
                        for (String v : ha.variables) {
                                if (cf.varID.get(v) < sp.getAMatrixSize(e.getValue()))
                                        variableString = variableString + " " + v;
                        }

                        proxy.eval("syms " + variableString);
                        proxy.eval("X = [" + variableString + "]");
                        cf.setLinearMatrix(e.getValue());
                        //sp.setLinearMatrix(e.getValue());
	        	String matlabAMatrix = sp.convertFlowToAMatrix(e.getValue());
	        	proxy.eval("A_" + e.getKey() + " = " + matlabAMatrix + ";");
	        	proxy.eval("A_" + e.getKey() + " * X.'");
	        	
	        	String matlabBMatrix = sp.convertInputToBMatrix(e.getValue());
	        	proxy.eval("B_" + e.getKey() + " = " + matlabBMatrix + "';");
	        	
	        	String matlabCMatrix = sp.convertInvToMatrix(e.getValue());
	        	proxy.eval("C_" + e.getKey() + " = " + matlabCMatrix + ";");
                        
                        String lowerBound = sp.parseInitialLowerBound(e.getValue());
	        	proxy.eval("lb_" + e.getKey() + " = " + lowerBound + ";");
                        
                        String upperBound = sp.parseInitialUpperBound(e.getValue());
	        	proxy.eval("ub_" + e.getKey() + " = " + upperBound+ ";");
                        
                        String inputBound = sp.parseInitialInputBound(e.getValue());
	        	proxy.eval("ib_" + e.getKey() + " = " + inputBound+ ";");
                        
                        
                        proxy.eval("sys_" + e.getKey() + " = ss(" + "A_" + e.getKey() + ", " + "B_" + e.getKey() + ", " + "C_" + e.getKey() + ", " +  "0)");
                        String cmd_string = "[sys_r,lb_r,ub_r,e] = find_specified_reduced_model(sys_"+ e.getKey()+ ",lb_" + e.getKey()+",ub_" + e.getKey()+",ib_" + e.getKey()+ "," + reducedOrder+")";
                        proxy.eval(cmd_string); 
                        // convert to a spaceex model
                        proxy.eval("[mA,mC,nB,flow,invariant,initialExpression] = spaceex_model_generation('" + e.getKey()+ "_reduced_to_"+reducedOrder +"',sys_r,lb_r,ub_r," + "ib_" + e.getKey() +",1,'-t')"); 
                        String flow = (String) proxy.getVariable("flow");
                        String invariant = (String) proxy.getVariable("invariant");
                        String initialCondition = (String) proxy.getVariable("initialExpression");
                        double xSize = ((double[]) proxy.getVariable("mA"))[0];
                        int varXSize = (int)xSize ;
                        double ySize = ((double[]) proxy.getVariable("mC"))[0];
                        int varYSize = (int)ySize ;
                        double iSize = ((double[]) proxy.getVariable("nB"))[0];
                        int inputSize = (int)iSize ;
                        // plot output versus time
                        String[] plotVars = new String[varYSize+1];
                        plotVars[0] = "time";
                        ha.modes.clear();
                        ha.variables.clear();
                        ha.constants.clear();
                        for (int i = 1; i <= varXSize; i++){
                           ha.variables.add("x" + i);
                        }
                        
                        for (int j = 1; j <= varYSize; j++){
                            plotVars[j] = "y" + j;
                            ha.variables.add(plotVars[j]);
                        }
                        
                        for (int k = 1; k <= inputSize; k++){
                            ha.constants.put("u" + k, null);
                        }
                        // add global time variable
                        ha.variables.add("time");
                        ha.constants.put("stoptime", null);
                        
                        // generate mode                      
                        ha.createMode(e.getKey(), invariant, flow);
                        
                        // put initial conditions 
                        config.init.clear();
                        config.init.put(e.getKey(),FormulaParser.getExpression(initialCondition, "initial/forbidden"));                       
                        config.settings.plotVariableNames = plotVars;
                        config.DO_VALIDATION = false;

	        }
	        //Disconnect the proxy from MATLAB
	        proxy.disconnect();
		} catch (MatlabConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (MatlabInvocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// todo: determine list of variables removed, then call the following to drop them
		//removeVariables(ha, varName);
		

	}
	
	private void removeVariable(BaseComponent ha, String varName)
	{
		if (!ha.variables.contains(varName)) {
			throw new AutomatonExportException("Variable " + varName + " does not exist in automaton.");
		}
		
		ha.variables.remove(varName);
		
		// remove flows
		for (AutomatonMode am : ha.modes.values()) {
			am.flowDynamics.remove(varName);
		}
		
		// todo: remove variables from other places (init, resets, etc.)
	}


	private void processParams(BaseComponent ha, String params)
	{
		if (params.trim().length() == 0) {
			params = Integer.toString(ha.variables.size()); // use original automaton dimensionality n
			// TODO: use n - 1, assuming n >= 2?
		}
		
		String[] parts = params.split(";");
		
		if (parts.length != 1)
			throw new AutomatonExportException("Expected 1 pass params separated by ';', instead received: " + params);
		
		try
		{
			reducedOrder = Integer.parseInt(parts[0]);
		}
		catch (NumberFormatException e)
		{
			throw new AutomatonExportException("Error parsing number: " + e, e);
		}
		
		Hyst.log("Using order reduction params reducedOrder = " + reducedOrder);
	}

}
