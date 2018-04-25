package com.verivital.hyst.generators;

import java.util.List;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
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
import com.verivital.hyst.util.DoubleArrayOptionHandler;
import com.verivital.hyst.util.ExpressionValueArrayOptionHandler;

/**
 * Creates a chain of integrators. Based on "Chains of Integrators as a Benchmark for Scalability of
 * Hybrid Control Synthesis", Livingston et. al, ARCH 2016
 * 
 * Paramters are # of parallel chains (n) Length of chain m Input for the last dimension in each
 * chain (u0 ... um)
 * 
 * timeVar, optional, (should an explicit 'time dimension be added? parameter is the name) xi, input
 * noise list, optional, the additive noise for each input
 * 
 * where x0' == 1, x1' == x0, x2' == x1, ..., xn = u
 * 
 * @author Stanley Bak (May 2016)
 *
 */
public class IntegralChainGenerator extends ModelGenerator
{
	@Option(name = "-N", required = true, usage = "number of parallel chains", metaVar = "NUM")
	private int n = 1;

	@Option(name = "-M", required = true, usage = "length of each chain", metaVar = "NUM")
	private int m = 1;

	@Option(name = "-U", required = true, usage = "input expression for each chain (list of length N), "
			+ "use parenthesis if there are parsing errors", handler = ExpressionValueArrayOptionHandler.class, metaVar = "U_0 U_1 ... U_N")
	private List<Expression> uList;

	@Option(name = "-xi", usage = "noise magnitude for each input expression (list of length N)", handler = DoubleArrayOptionHandler.class, metaVar = "xi_0 xi_1 ... xi_N")
	private List<Double> xiList = null;

	@Option(name = "-time_var", usage = "the name of the time variable to add, if desired", metaVar = "NAME")
	private String timeVar = null;

	@Option(name = "-var_prefix", usage = "variable name prefix", metaVar = "NAME")
	private String varPrefix = "x";

	@Option(name = "-mode_name", usage = "the name of the mode", metaVar = "NAME")
	private String modeName = "on";

	@Override
	public String getCommandLineFlag()
	{
		return "integrator_chain";
	}

	@Override
	public String getName()
	{
		return "Chain of Integrators [Livingston16]";
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

		if (timeVar != null)
		{
			ha.variables.add(timeVar);
			initExp = (new Operation(timeVar, Operator.EQUAL, 0));
			mode.flowDynamics.put(timeVar, new ExpressionInterval(1));
		}

		// first variable in each chain is x_0, x_1, x_2, ... first der is
		// x_0_der1, ect.

		for (int chainNum = 0; chainNum < n; ++chainNum)
		{
			for (int numInChain = 0; numInChain < m; ++numInChain)
			{
				String varName = varPrefix + "_" + chainNum;

				if (numInChain != 0)
					varName += "_der" + numInChain;

				ha.variables.add(varName);
				initExp = Expression.and(initExp, new Operation(varName, Operator.EQUAL, 0));

				// var_n' == var_{n+1}
				if (numInChain != (m - 1))
				{
					String nextVar = varPrefix + "_" + chainNum + "_der" + (numInChain + 1);
					mode.flowDynamics.put(varName, new ExpressionInterval(new Variable(nextVar)));
				}
				else
				{
					// der is the input + xi
					Interval i = null;

					if (xiList != null)
						i = new Interval(-xiList.get(chainNum), xiList.get(chainNum));

					mode.flowDynamics.put(varName, new ExpressionInterval(uList.get(chainNum), i));
				}
			}
		}

		c.init.put(modeName, initExp);

		// assign plot variables
		if (timeVar != null)
		{
			c.settings.plotVariableNames[0] = timeVar;
			c.settings.plotVariableNames[1] = varPrefix + "_0";
		}
		else
		{
			// only one variable is guaranteed to exist...
			c.settings.plotVariableNames[0] = varPrefix + "_0";
			c.settings.plotVariableNames[1] = varPrefix + "_" + (n - 1);
		}

		return c;
	}

	private void checkParams()
	{
		if (n < 1)
			throw new AutomatonExportException("Number of chains positive: " + n);

		if (m < 1)
			throw new AutomatonExportException("Chain length must be positive: " + m);

		if (uList.size() != n)
			throw new AutomatonExportException("Input list size (" + uList.size()
					+ ") must equal number of chains (" + n + ")");

		if (xiList != null && xiList.size() != n)
			throw new AutomatonExportException("Xi list (input noise) size (" + uList.size()
					+ ") must equal number of chains (" + n + ")");
	}

}
