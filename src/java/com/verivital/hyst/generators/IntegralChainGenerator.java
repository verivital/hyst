package com.verivital.hyst.generators;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;

/**
 * Creates a chain of integrators, where x0' == 1, x1' == x0, x2' == x1, ...
 * @author Stanley Bak (May 2016)
 *
 */
public class IntegralChainGenerator extends ModelGenerator 
{
	@Option(name="-dims",required=true,usage="number of dimensions", metaVar="NUM")
	private int numDims = 1;
	
	@Option(name="-mode_name",usage="name of the mode", metaVar="NAME")
	private String modeName = "on";
	
	@Option(name="-var_prefix",usage="variable name prefix", metaVar="NAME")
	private String varPrefix = "var";
	
	@Override
	public String getCommandLineFlag() 
	{
		return "integrator_chain";
	}

	@Override
	public String getName() 
	{
		return "Chain of Integrators";
	}

	@Override
	protected Configuration generateModel() 
	{
		checkParams();
		
		BaseComponent ha = new BaseComponent();
		Configuration c = new Configuration(ha);
		
		AutomatonMode mode = ha.createMode(modeName);
		mode.invariant = Constant.TRUE;
		Expression initExp = Constant.TRUE;
		Expression prevDimDer = new Constant(1);
		
		for (int i = 0; i < numDims; ++i)
		{
			String varName = varPrefix + i;
			
			ha.variables.add(varName);
			initExp = Expression.and(initExp, new Operation(varName, Operator.EQUAL, 0));
			
			// var_n' == var_{n+1}
			mode.flowDynamics.put(varName, new ExpressionInterval(prevDimDer));
			prevDimDer = new Variable(varName);
		}
		
		c.init.put(modeName, initExp);
		
		// assign plot variables
		c.settings.plotVariableNames[0] = varPrefix + "0";
		c.settings.plotVariableNames[1] = varPrefix + (numDims - 1);

		return c;
	}

	private void checkParams() 
	{
		if (numDims < 1)
			throw new AutomatonExportException("Number of dimensions must be positive: " + numDims);
	}

}
