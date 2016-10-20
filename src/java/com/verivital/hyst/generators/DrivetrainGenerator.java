package com.verivital.hyst.generators;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;

/**
 * This a drivetrain model with additional rotating masses, taken from Matthias Althoff, Bruce H.
 * Krogh: "Avoiding geometric intersection operations in reachability analysis of hybrid systems" in
 * HSCC 2012
 * 
 * It originally defines a 7 + 2*theta dimensional system, where theta >= 0 is a user parameter
 * 
 * There is also a control input u , which is set by a benchmark maneuver to: -5 when time is in [0,
 * 0.2], and +5 when time is in [0.2, 2]. To handle this, we add a dimension 't' and a guard when t
 * = 0.2, giving us a 8 + 2*theta dimensional system with 6 modes (3 before time 0.2, and 3 after
 * time 0.2)
 * 
 * @author Stanley Bak (Oct 2016)
 *
 */
public class DrivetrainGenerator extends ModelGenerator
{
	@Option(name = "-theta", usage = "number of additional rotating masses (dims = 9 + 2*theta)", metaVar = "NUM")
	private int theta = 1;

	@Option(name = "-init_scale", usage = "multiplier for the initial states (1 = 100%, 0.05 = 5%)")
	private double initScale = 1.0;

	@Option(name = "-high_input", usage = "force the high input for the entire time interval")
	private boolean forceHighInput = false;

	@Option(name = "-error_guard", usage = "add a guard to an error mode with the given condition")
	private String errorGuard;

	////////////// parameters ////////////////
	final static double switchTime = 0.2;
	final static double[] inputs = new double[] { -5, 5 };

	final static HashMap<String, Double> constants = new HashMap<String, Double>();

	static
	{
		// model constants taken from CORA model (slight differences with his paper, e.g. k_i / k_s)

		constants.put("alpha", 0.03); // backlash size (half gap) [rad]
		constants.put("tau_eng", 0.1); // time constant of the engine [s]
		constants.put("b_l", 5.6); // viscous friction of wheels [Nm/(rad/s)]
		constants.put("b_m", 0.0); // viscous friction of engine [Nm/(rad/s)]
									// NOTE: b_m is 0.02 in CORA (but formulas are modified),
									// is is 0 in the paper and SpaceEx models
		constants.put("b_i", 1.0); // viscous friction of additional inertias [Nm/(rad/s)]
		constants.put("i", 12.0); // transmission ratio, Theta_m/Theta_1 [rad/rad]
		constants.put("k", 10e3); // shaft stiffness [Nm/rad]
		constants.put("k_i", 10e4); // shaft stiffness of additional inertias [Nm/rad]
		constants.put("r", 0.33); // wheel radius [m]
		constants.put("J_l", 140.0); // moment of inertia of wheels and vehicle mass [kgm^2]
		constants.put("J_m", 0.3); // moment of inertia of engine flywheel [kgm^2]
		constants.put("J_i", 0.01); // moment of inertia of additional inertias [kgm^2]

		// control parameters
		constants.put("k_P", 0.0);
		constants.put("k_I", 0.0);
		constants.put("k_D", 0.0);
		constants.put("k_K", 0.5);
		constants.put("k_KD", 0.5);
		constants.put("k_KI", 0.5);
		constants.put("k_KK", 0.0);
	}

	@Override
	public String getCommandLineFlag()
	{
		return "drivetrain";
	}

	@Override
	public String getName()
	{
		return "Drivetrain with Rotating Masses [Althoff12]";
	}

	@Override
	protected Configuration generateModel()
	{
		checkParams();

		BaseComponent ha = makeDrivetrainAutomaton();

		Configuration c = new Configuration(ha);

		// init
		Expression initExp = Constant.TRUE;

		if (!forceHighInput)
			initExp = FormulaParser.parseInitialForbidden("t == 0");

		initExp = Expression.and(initExp, makeInitExpression());

		if (!forceHighInput)
			c.init.put("negAngleInit", initExp);
		else
			c.init.put("negAngle", initExp);

		if (errorGuard != null)
			c.forbidden.put("error", Constant.TRUE);

		// settings
		c.settings.plotVariableNames[0] = "x1";
		c.settings.plotVariableNames[1] = "x2";

		c.settings.spaceExConfig.timeHorizon = 2.0;
		c.settings.spaceExConfig.samplingTime = 5e-4; // 0.0005

		return c;
	}

	private BaseComponent makeDrivetrainAutomaton()
	{
		BaseComponent rv = new BaseComponent(); // input generator

		for (Entry<String, Double> e : constants.entrySet())
			rv.constants.put(e.getKey(), new Interval(e.getValue()));

		for (int d = 1; d <= 7 + 2 * theta; ++d)
			rv.variables.add("x" + d);

		if (!forceHighInput)
			rv.variables.add("t");

		String[] alphas = new String[] { "-alpha", "-alpha", "alpha" };
		String[] ks = new String[] { "k", "0", "k" };

		// modes under input 2
		AutomatonMode loc1_u2 = rv.createMode("negAngle");
		AutomatonMode loc2_u2 = rv.createMode("deadzone");
		AutomatonMode loc3_u2 = rv.createMode("posAngle");
		AutomatonMode[] transitionLocs = new AutomatonMode[] { loc1_u2, loc2_u2, loc3_u2 };

		loc1_u2.invariant = Constant.TRUE;
		loc2_u2.invariant = Constant.TRUE;
		loc3_u2.invariant = Constant.TRUE;

		if (!forceHighInput)
		{
			AutomatonMode loc1_u1 = rv.createMode("negAngleInit");

			// create input transitions when the time reaches 0.2
			AutomatonMode pre = loc1_u1;
			AutomatonMode post = loc1_u2;

			rv.createTransition(pre, post).guard = FormulaParser.parseGuard("t >= " + switchTime);

			pre.invariant = FormulaParser.parseInvariant("t <= " + switchTime);

			makeDynamics(loc1_u1, alphas[0], ks[0], inputs[0]);
		}

		AutomatonMode loc1 = transitionLocs[0];
		AutomatonMode loc2 = transitionLocs[1];
		AutomatonMode loc3 = transitionLocs[2];

		loc1.invariant = Expression.and(loc1.invariant,
				FormulaParser.parseInvariant("x1 <= -alpha"));
		loc2.invariant = Expression.and(loc2.invariant,
				FormulaParser.parseInvariant("-alpha <= x1 <= alpha"));
		loc3.invariant = Expression.and(loc3.invariant,
				FormulaParser.parseInvariant("alpha <= x1"));

		rv.createTransition(loc1, loc2).guard = FormulaParser.parseGuard("x1 >= -alpha");
		rv.createTransition(loc2, loc3).guard = FormulaParser.parseGuard("x1 >= alpha");

		// dynamics
		for (int i = 0; i < 3; ++i)
		{
			AutomatonMode loc = transitionLocs[i];
			makeDynamics(loc, alphas[i], ks[i], inputs[1]);
		}

		if (errorGuard != null)
		{
			AutomatonMode am = rv.createMode("error");
			am.invariant = Constant.TRUE;
			am.flowDynamics = new LinkedHashMap<String, ExpressionInterval>();

			for (int d = 1; d <= 7 + 2 * theta; ++d)
				am.flowDynamics.put("x" + d, new ExpressionInterval(0));

			if (!forceHighInput)
				am.flowDynamics.put("t", new ExpressionInterval(0));

			rv.createTransition(loc3_u2, am).guard = FormulaParser.parseGuard(errorGuard);
		}

		rv.validate();

		return rv;
	}

	private void makeDynamics(AutomatonMode loc, String alpha, String k, double u_value)
	{
		String v = "k_K*(i*x4 - x7) + k_KD*(i*" + u_value + " - 1/J_m*(x2 - 1/i*" + k + "*(x1 - ("
				+ alpha + ")) - b_m*x7)) + k_KI*(i*x3 - i*(x1+ x8))";

		if (!forceHighInput)
			loc.flowDynamics.put("t", new ExpressionInterval(1));

		loc.flowDynamics.put("x1", new ExpressionInterval("1/i*x7 - x9"));
		loc.flowDynamics.put("x2", new ExpressionInterval("(" + v + " - x2)/tau_eng"));
		loc.flowDynamics.put("x3", new ExpressionInterval("x4"));
		loc.flowDynamics.put("x4", new ExpressionInterval(u_value));
		loc.flowDynamics.put("x5", new ExpressionInterval("x6"));

		String xBeforeLast = "x" + (7 + 2 * theta - 1);

		loc.flowDynamics.put("x6",
				new ExpressionInterval("1/J_l*(k_i*(" + xBeforeLast + " - x5) - b_l*x6)"));

		loc.flowDynamics.put("x7", new ExpressionInterval(
				"1/J_m*(x2 - 1/i*" + k + "*(x1 - (" + alpha + ")) - b_m*x7)"));

		if (theta >= 1)
		{
			loc.flowDynamics.put("x8", new ExpressionInterval("x9"));

			String nextOne = theta > 1 ? "x10" : "x5";

			loc.flowDynamics.put("x9", new ExpressionInterval(
					"J_i*(" + k + "*(x1 - (" + alpha + ")) - k_i*(x8 - " + nextOne + "))"));
		}

		if (theta >= 2)
		{
			for (int t = 2; t <= theta; ++t)
			{
				int index = 7 + 2 * t - 1;

				loc.flowDynamics.put("x" + index, new ExpressionInterval("x" + (index + 1)));

				String nextOne = theta > t ? "x" + (index + 2) : "x5";

				loc.flowDynamics.put("x" + (index + 1),
						new ExpressionInterval("J_i*(k_i*(x" + (index - 2) + " - x" + index
								+ ") - k_i*(x" + index + " - " + nextOne + "))"));

				// loc1.flowDynamics.put("x10", new ExpressionInterval("x11"));
				// loc1.flowDynamics.put("x11",
				// new ExpressionInterval("J_i*(k_i*(x8 - x10) - k_i*(x10 - x5))"));
			}
		}
	}

	private void checkParams()
	{
		if (theta < 0)
			throw new AutomatonExportException("theta must be nonnegative: " + theta);

		if (initScale < 0)
			throw new AutomatonExportException("init_scale must be nonnegative: " + theta);
	}

	private Expression makeInitExpression()
	{
		Expression rv = Constant.TRUE;

		double[] center = { -0.0432, -11, 0, 30, 0, 30, 360, -0.00132, 30 };
		double[] generator = { 0.0056, 4.67, 0, 10, 0, 10, 120, 0.0006, 10 };

		if (initScale == 0)
		{
			for (int d = 0; d < 7 + 2 * theta; ++d)
			{
				double val;

				if (d < center.length)
					val = center[d];
				else
					val = center[7 + (d + 1) % 2]; // 9 goes to 7, 10 goes to 8, 11 goes to 7

				rv = Expression.and(rv,
						FormulaParser.parseInitialForbidden("x" + (d + 1) + " = " + val));
			}
		}
		else
		{
			// create n-1 inequalities to define the initial line
			// formula: y-y1 = (y2-y1) / (x2-x1) * (x-x1)

			// use the difference in x3 for the denominator
			// dim 3 was chosen because x2-x1 is 20, so the generator scales won't be multiplied or
			// divided too much (20 is close to 1... in terms of floating point error)
			int denominatorDimIndex = 3;
			double x1 = center[denominatorDimIndex] - generator[denominatorDimIndex];
			double x2 = center[denominatorDimIndex] + generator[denominatorDimIndex];
			String denVar = "x" + (denominatorDimIndex + 1);

			for (int d = 0; d < 7 + 2 * theta; ++d)
			{
				if (d == denominatorDimIndex) // skip denominator variable
					continue;

				double c;
				double g;

				if (d < center.length)
				{
					c = center[d];
					g = generator[d];
				}

				else
				{
					int index = 7 + (d + 1) % 2; // 9 goes to 7, 10 goes to 8, 11 goes to 7
					c = center[index];
					g = generator[index];
				}

				double y1 = c - initScale * g;
				double y2 = c + initScale * g;

				// formula: y-y1 = (y2-y1) / (x2-x1) * (x-x1)
				String curVar = "x" + (d + 1);
				String exp = curVar + " - (" + y1 + ") = " + (y2 - y1) / (x2 - x1) + " * (" + denVar
						+ " - (" + x1 + "))";

				rv = Expression.and(rv, FormulaParser.parseInitialForbidden(exp));
			}

			// finally: add two bounds on denVar for the sides
			String exp = x1 + " <= " + denVar + " <= " + x2;
			rv = Expression.and(rv, FormulaParser.parseInitialForbidden(exp));
		}

		return rv;
	}
}
