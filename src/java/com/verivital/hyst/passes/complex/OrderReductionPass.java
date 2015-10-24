package com.verivital.hyst.passes.complex;


import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.junit.Assert;

import com.verivital.hyst.grammar.formula.Constant;
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
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.printers.StateflowSpPrinter;

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

        StateflowSpPrinter sp = new StateflowSpPrinter();
        sp.ha = ha;
        
        MatlabProxy proxy;
		try {
			proxy = factory.getProxy();
			
			//Display 'hello world' just like when using the demo
	        proxy.eval("disp('hello world'); disp('test123'); clock");
	        
	        // todo: refactor, move to stateflow printer
	        // todo: test to ensure order of this vector is the same as the matrix below, could be ensured by construction if done in matrix construction function
	        String variableString = "";
	        for (String v : ha.variables) {
	        	variableString = variableString + " " + v;
	        }
	        
	        proxy.eval("syms " + variableString);
	        proxy.eval("X = [" + variableString + "]");
	        
	        for (Entry <String, AutomatonMode> e : ha.modes.entrySet()) {
	        	String matlabAMatrix = sp.convertFlowToMatrix(e.getValue());
	        	proxy.eval("A_" + e.getKey() + " = " + matlabAMatrix + ";");
	        	proxy.eval("A_" + e.getKey() + " * X.'");
	        }
	        
	        // TODO: get inputs, I guess these are going to be constants from looking at the example model (e.g., u has constant dynamics for building) ; do the same dynamics matrix function to get the B vector, etc.
	        // TODO: do similar for the C matrix from the invariant

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
