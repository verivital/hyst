package com.verivital.hyst.passes.complex;

import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.matlab.MatlabBridge;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.printers.SimulinkStateflowPrinter;
import com.verivital.hyst.util.Classification;

import matlabcontrol.MatlabProxy;

/**
 * Perform order reduction
 * 
 * @author Taylor Johnson (October 2015)
 *
 */
public class OrderReductionPass extends TransformationPass
{
	@Option(name = "-reducedOrder", required = true, usage = "reduced order dimensionality", metaVar = "NUM")
	private int reducedOrder;

	@Override
	public String getName()
	{
		return "Order Reduction (decrease dimensionality) Pass";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "order_reduction";
	}

	@Override
	protected void runPass()
	{
		Hyst.log("Using order reduction params reducedOrder = " + reducedOrder);

		BaseComponent ha = (BaseComponent) config.root;

		Classification cf = new Classification();
		cf.ha = ha;
		cf.setVarID(ha);
		SimulinkStateflowPrinter sp = new SimulinkStateflowPrinter();
		sp.ha = ha;
		sp.setConfig(config);
		// sp.setVarID(ha);
		MatlabProxy proxy;
		try
		{
			proxy = MatlabBridge.getInstance().getProxy();

			proxy.eval("[path_parent,path_current] = fileparts(pwd)");
			proxy.eval("if strcmp(path_current, 'matlab') cd ../; end");
			proxy.eval("[path_parent,path_current] = fileparts(pwd)");
			proxy.eval(
					"if ~strcmp(path_current, 'pass_order_reduction') cd ./matlab/pass_order_reduction; end");

			// currently, only support for continuous linear dynamics
			// todo: LUAN: refactor, move to stateflow printer, write a general
			// function that extracts the variables and matrix, don't construct
			// this variable string thing here
			// todo: test to ensure order of this vector is the same as the
			// matrix below, could be ensured by construction if done in matrix
			// construction function
			for (Entry<String, AutomatonMode> e : ha.modes.entrySet())
			{
				// declare x variable
				String variableString = "";
				for (String v : ha.variables)
				{
					if (cf.varID.get(v) < sp.getAMatrixSize(e.getValue()))
						variableString = variableString + " " + v;
				}

				proxy.eval("syms " + variableString);
				proxy.eval("X = [" + variableString + "]");
				cf.setLinearMatrix(e.getValue());
				// sp.setLinearMatrix(e.getValue());
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
				proxy.eval("ub_" + e.getKey() + " = " + upperBound + ";");

				String inputBound = sp.parseInitialInputBound(e.getValue());
				proxy.eval("ib_" + e.getKey() + " = " + inputBound + ";");

				proxy.eval("sys_" + e.getKey() + " = ss(" + "A_" + e.getKey() + ", " + "B_"
						+ e.getKey() + ", " + "C_" + e.getKey() + ", " + "0)");
				String cmd_string = "[sys_r,lb_r,ub_r,e] = find_specified_reduced_model(sys_"
						+ e.getKey() + ",lb_" + e.getKey() + ",ub_" + e.getKey() + ",ib_"
						+ e.getKey() + "," + reducedOrder + ")";
				proxy.eval(cmd_string);
				// convert to a spaceex model
				proxy.eval(
						"[mA,mC,nB,flow,invariant,initialExpression] = spaceex_model_generation('"
								+ e.getKey() + "_reduced_to_" + reducedOrder + "',sys_r,lb_r,ub_r,"
								+ "ib_" + e.getKey() + ",1,'-t')");

				String flow = (String) proxy.getVariable("flow");
				String invariant = (String) proxy.getVariable("invariant");
				String initialCondition = (String) proxy.getVariable("initialExpression");
				double xSize = ((double[]) proxy.getVariable("mA"))[0];
				int varXSize = (int) xSize;
				double ySize = ((double[]) proxy.getVariable("mC"))[0];
				int varYSize = (int) ySize;
				double iSize = ((double[]) proxy.getVariable("nB"))[0];
				int inputSize = (int) iSize;

				// done with matlab
				proxy.disconnect(); // important: need to disconnect when done,
									// as next call to getInstance can create a
									// new factory if still connected

				// plot output versus time
				String[] plotVars = new String[varYSize + 1];
				plotVars[0] = "time";
				ha.modes.clear();
				ha.variables.clear();
				ha.constants.clear();
				for (int i = 1; i <= varXSize; i++)
				{
					ha.variables.add("x" + i);
				}

				for (int j = 1; j <= varYSize; j++)
				{
					plotVars[j] = "y" + j;
					ha.variables.add(plotVars[j]);
				}

				for (int k = 1; k <= inputSize; k++)
				{
					ha.constants.put("u" + k, null);
				}
				// add global time variable
				ha.variables.add("time");
				ha.constants.put("stoptime", null);

				// generate mode
				ha.createMode(e.getKey(), invariant, flow);

				// put initial conditions
				config.init.clear();
				config.init.put(e.getKey(), FormulaParser.parseInitialForbidden(initialCondition));
				config.settings.plotVariableNames = plotVars;
				config.DO_VALIDATION = false;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			System.out.println(e.getMessage());
		}
		// MatlabBridge.getInstance().close

		// todo: determine list of variables removed, then call the following to
		// drop them
		// removeVariables(ha, varName);

	}

	private void removeVariable(BaseComponent ha, String varName)
	{
		if (!ha.variables.contains(varName))
		{
			throw new AutomatonExportException(
					"Variable " + varName + " does not exist in automaton.");
		}

		ha.variables.remove(varName);

		// remove flows
		for (AutomatonMode am : ha.modes.values())
		{
			am.flowDynamics.remove(varName);
		}

		// todo: remove variables from other places (init, resets, etc.)
	}
}
