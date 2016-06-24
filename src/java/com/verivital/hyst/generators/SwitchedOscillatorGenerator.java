package com.verivital.hyst.generators;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;

/**
 * This is the switched oscillator used to evaluate SpaceEx. It has an n-dimensional filter
 * attached.
 * 
 * https://ths.rwth-aachen.de/research/projects/hypro/filtered-oscillator/
 * 
 * x' = -2 * x - 1.4
 * 
 * y' = -y + 0.7
 * 
 * x_1' = 5 * x - 5 * x_1
 * 
 * x_2' = 5 * x_1 - 5 * x_2
 * 
 * x_3' = 5 * x_2 - 5 * x_3
 * 
 * ...
 * 
 * z' = 5 * x_3 - 5 * z
 * 
 * @author Stanley Bak (June 2016)
 *
 */
public class SwitchedOscillatorGenerator extends ModelGenerator
{
	@Option(name = "-dims", required = true, usage = "number of dimensions in filter", metaVar = "NUM")
	private int dims = 1;

	@Option(name = "-x", usage = "x axis variable", metaVar = "VARNAME")
	private String xVar = "x";

	@Option(name = "-y", usage = "y axis variable", metaVar = "VARNAME")
	private String yVar = "y";

	@Option(name = "-strict_guards", usage = "guards use strict inequalities")
	private boolean strictGuards = false;

	@Override
	public String getCommandLineFlag()
	{
		return "oscillator";
	}

	@Override
	public String getName()
	{
		return "Switched Oscillator";
	}

	@Override
	protected Configuration generateModel()
	{
		checkParams();

		BaseComponent ha = new BaseComponent();
		Configuration c = new Configuration(ha);

		ha.variables.add("x");
		ha.variables.add("y");

		for (int d = 1; d < dims; ++d)
			ha.variables.add("x" + d);

		ha.variables.add("z");

		AutomatonMode loc1 = ha.createMode("loc1");
		AutomatonMode loc2 = ha.createMode("loc2");
		AutomatonMode loc3 = ha.createMode("loc3");
		AutomatonMode loc4 = ha.createMode("loc4");

		loc1.invariant = FormulaParser.parseInvariant("x <= 0 && y + 0.714286 * x >= 0");
		loc2.invariant = FormulaParser.parseInvariant("x <= 0 && y + 0.714286 * x <= 0");
		loc3.invariant = FormulaParser.parseInvariant("x >= 0 && y + 0.714286 * x >= 0");
		loc4.invariant = FormulaParser.parseInvariant("x >= 0 && y + 0.714286 * x <= 0");

		if (strictGuards)
		{
			ha.createTransition(loc1, loc3).guard = FormulaParser
					.parseGuard("x == 0 & 0.714286 * x + y >= 0");
			ha.createTransition(loc3, loc4).guard = FormulaParser
					.parseGuard("x >= 0 & 0.714286 * x + y == 0");
			ha.createTransition(loc4, loc2).guard = FormulaParser
					.parseGuard("x == 0 & 0.714286 * x + y <= 0");
			ha.createTransition(loc2, loc1).guard = FormulaParser
					.parseGuard("x <= 0 & 0.714286 * x + y == 0");
		}
		else
		{
			// non-strict guards
			ha.createTransition(loc1, loc3).guard = FormulaParser
					.parseGuard("x >= 0 & 0.714286 * x + y >= 0");
			ha.createTransition(loc3, loc4).guard = FormulaParser
					.parseGuard("x >= 0 & 0.714286 * x + y <= 0");
			ha.createTransition(loc4, loc2).guard = FormulaParser
					.parseGuard("x <= 0 & 0.714286 * x + y <= 0");
			ha.createTransition(loc2, loc1).guard = FormulaParser
					.parseGuard("x <= 0 & 0.714286 * x + y >= 0");
		}

		loc1.flowDynamics.put("x", new ExpressionInterval("-2*x + 1.4"));
		loc1.flowDynamics.put("y", new ExpressionInterval("-y - 0.7"));

		loc2.flowDynamics.put("x", new ExpressionInterval("-2*x - 1.4"));
		loc2.flowDynamics.put("y", new ExpressionInterval("-y + 0.7"));

		loc3.flowDynamics.put("x", new ExpressionInterval("-2*x + 1.4"));
		loc3.flowDynamics.put("y", new ExpressionInterval("-y - 0.7"));

		loc4.flowDynamics.put("x", new ExpressionInterval("-2*x - 1.4"));
		loc4.flowDynamics.put("y", new ExpressionInterval("-y + 0.7"));

		for (AutomatonMode am : new AutomatonMode[] { loc1, loc2, loc3, loc4 })
		{
			String prevVarX = "x";

			for (int d = 1; d < dims; ++d)
			{
				String var = "x" + d;

				am.flowDynamics.put(var, new ExpressionInterval("5*" + prevVarX + "-5*" + var));

				prevVarX = var;
			}

			am.flowDynamics.put("z", new ExpressionInterval("5*" + prevVarX + "-5*z"));
		}

		Expression initExp = FormulaParser
				.parseInitialForbidden("0.2 <= x <= 0.3 & -0.1 <= y <= 0.1 & z == 0");

		for (int d = 1; d < dims; ++d)
		{
			String var = "x" + d;

			initExp = Expression.and(initExp, new Operation(var, Operator.EQUAL, 0));
		}

		c.init.put("loc3", initExp);

		for (AutomatonMode am : new AutomatonMode[] { loc1, loc2, loc3, loc4 })
			c.forbidden.put(am.name, new Operation("y", Operator.GREATEREQUAL, 0.5));

		// settings
		c.settings.plotVariableNames[0] = xVar;
		c.settings.plotVariableNames[1] = yVar;

		c.settings.spaceExConfig.timeHorizon = 4;
		c.settings.spaceExConfig.samplingTime = 0.05;

		return c;
	}

	private void checkParams()
	{
		if (dims < 1)
			throw new AutomatonExportException("Dims in filter must be positive: " + dims);
	}

}
