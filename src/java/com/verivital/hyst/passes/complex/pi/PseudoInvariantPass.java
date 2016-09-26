package com.verivital.hyst.passes.complex.pi;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.internalpasses.ConvertFromStandardForm;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.HyperPointArrayOptionHandler;
import com.verivital.hyst.util.StringOperations;

/**
 * This pass splits the initial mode into two using the technique of pseudo-invariants:
 * "Reducing the Wrapping Effect in Flowpipe Construction Using Pseudo-Invariants" , CyPhy 2014, Bak
 * 2014
 * 
 * The parameters define the list of (mode, point, direction), which define the mode to apply the
 * transformation, the continuous point, and the direction of the hyperplane which defines the
 * invariant.
 * 
 * -modes MODE1 MODE2 ... -points POINT1 POINT2 ... -dirs DIR1 DIR2 ...
 * 
 * where PT and DIR are comma-separated lists of doubles, like 1,-2,3
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class PseudoInvariantPass extends TransformationPass
{
	@Option(name = "-modes", handler = StringArrayOptionHandler.class, usage = "mode names (optional)", metaVar = "MODE1 MODE2 ...")
	private List<String> modes;

	@Option(name = "-points", required = true, handler = HyperPointArrayOptionHandler.class, usage = "points where to create pseudo-invariant hyperplane", metaVar = "POINT1 POINT2 ...")
	private List<HyperPoint> points;

	@Option(name = "-dirs", required = true, handler = HyperPointArrayOptionHandler.class, usage = "directions from each point in the guard-enabling direction", metaVar = "DIR1 DIR2 ...")
	private List<HyperPoint> dirs;

	@Option(name = "-skip_urgent_init", usage = "optimization which can be set if we can assume all initial states go to the first mode")
	private boolean skipUrgentInit = false;

	private BaseComponent ha;
	private String lastModeName = null; // the name of the last mode that was
										// created

	@Override
	public String getCommandLineFlag()
	{
		return "pi";
	}

	@Override
	public String getParamHelp()
	{
		String s = super.getParamHelp();
		s += "\nwhere POINT# and DIR# are each comma-separated (no spaces) lists of numbers, like 1,-2,3";

		return s;
	}

	@Override
	public String getName()
	{
		return "Pseudo-Invariant at Point Pass";
	}

	private void checkParams()
	{
		if (modes == null)
		{
			if (ha.modes.size() == 1)
			{
				modes = new ArrayList<String>();
				modes.add(ha.modes.values().iterator().next().name);
			}
			else
				throw new AutomatonExportException(
						"Multi-mode automaton requires the mode name in params.");
		}

		int len = modes.size();

		if (len == 0)
			throw new AutomatonExportException("Expected at least one mode in params.");

		if (points.size() != len)
			throw new AutomatonExportException(
					"Expected " + len + " points in params, got " + points.size());
		else if (dirs.size() != len)
			throw new AutomatonExportException(
					"Expected " + len + " dirs in params, got " + points.size());

		int dims = ha.variables.size();

		if (points.get(0).dims.length != dims)
			throw new AutomatonExportException(
					"Expected points with " + dims + " dimensions: " + ha.variables);

		if (dirs.get(0).dims.length != dims)
			throw new AutomatonExportException(
					"Expected dirs with " + dims + " dimensions: " + ha.variables);
	}

	/**
	 * Make the param string for calling this programatically
	 * 
	 * @param modes
	 *            (may be null for single-mode automata)
	 * @param points
	 * @param dirs
	 * @param skipUrgentInit
	 *            should we omit constructing an urgent start state and assume all initial states go
	 *            to the first mode?
	 * @return a string you can call runPass() with
	 */
	public static String makeParamString(List<String> modes, List<HyperPoint> points,
			List<HyperPoint> dirs, boolean skipUrgentInit)
	{
		StringBuilder rv = new StringBuilder();

		if (modes != null)
		{
			rv.append("-modes ");

			for (String m : modes)
				rv.append(m + " ");
		}

		rv.append("-points");

		for (HyperPoint p : points)
			rv.append(" " + StringOperations.join(",", p.dims));

		rv.append(" -dirs");

		for (HyperPoint p : dirs)
			rv.append(" " + StringOperations.join(",", p.dims));

		if (skipUrgentInit)
			rv.append(" -skip_urgent_init");

		return rv.toString();
	}

	@Override
	protected void runPass()
	{
		ha = (BaseComponent) config.root;
		checkParams();

		Expression savedInitialCondition = null;

		if (skipUrgentInit)
		{
			if (config.init.size() != 1)
				throw new AutomatonExportException(
						"skip urgent init option requires a single initial state");

			savedInitialCondition = config.init.values().iterator().next();
		}

		ConvertToStandardForm.run(config);

		// since we split modes, some of them become synonyms
		for (int i = 0; i < modes.size(); ++i)
		{
			AutomatonMode am = ha.modes.get(modes.get(i));
			HyperPoint point = points.get(i);
			HyperPoint dir = dirs.get(i);

			createPseudoInvariant(am, point, dir);
		}

		ConvertFromStandardForm.run(config);

		// if we should revert initial states to what they were before
		if (skipUrgentInit)
		{
			AutomatonMode init = ConvertToStandardForm.getInitMode(ha);

			if (ha.modes.containsValue(init))
			{
				ha.modes.remove(init.name);

				ArrayList<AutomatonTransition> toRemove = new ArrayList<AutomatonTransition>();

				for (AutomatonTransition at : ha.transitions)
				{
					if (at.from == init || at.to == init)
						toRemove.add(at);
				}

				ha.transitions.removeAll(toRemove);
			}

			config.init.clear();

			config.init.put(lastModeName, savedInitialCondition);
		}
	}

	private void createPseudoInvariant(AutomatonMode afterMode, HyperPoint point, HyperPoint dir)
	{
		String beforeName = makeModeName(afterMode);
		this.lastModeName = beforeName;
		Hyst.log("Creating PI mode " + beforeName + " from point " + point + " and direction "
				+ dir);

		AutomatonMode beforeMode = afterMode.copyWithTransitions(beforeName);

		Expression piInv = createInvariantExpression(ha.variables, point, dir);
		Expression piGuard = createGuardExpression(ha.variables, point, dir);

		// incoming transitions to afterMode should have an extra condition (the
		// pi guard)
		for (AutomatonTransition at : ha.transitions)
		{
			if (at.to == afterMode)
				at.guard = Expression.and(at.guard, piGuard);
		}

		// beforeMode should have an extra invariant (the pseudo-invariant)
		beforeMode.invariant = Expression.and(beforeMode.invariant, piInv);

		// there should be a transition between the two modes
		AutomatonTransition at = ha.createTransition(beforeMode, afterMode);
		at.guard = piGuard;

		Hyst.log("Created PI mode " + beforeName + " with invariant: " + piInv.toDefaultString());
	}

	/**
	 * Create the expression for the invariant, at the given point with the given gradient. The
	 * invariant is that we are BEFORE the time at which a trajectory would reach the given point.
	 * 
	 * @param vars
	 *            the variable names, in order
	 * @param point
	 * @param dir
	 * @return
	 */
	public static Expression createInvariantExpression(List<String> vars, HyperPoint point,
			HyperPoint gradient)
	{
		double rhs = dot(point.dims, gradient.dims);

		return makeExpressionFromLinearInequality(vars, gradient.dims, Operator.LESSEQUAL, rhs);
	}

	/**
	 * Create the expression for the invariant
	 * 
	 * @param vars
	 *            the variable names, in order
	 * @param point
	 * @param dir
	 * @return
	 */
	public static Expression createGuardExpression(List<String> vars, HyperPoint point,
			HyperPoint dir)
	{
		double rhs = dot(point.dims, dir.dims);

		return makeExpressionFromLinearInequality(vars, dir.dims, Operator.GREATEREQUAL, rhs);
	}

	/**
	 * Create the name for the predecessor am on a pseudo-invariant split
	 * 
	 * @param am
	 *            the original mode
	 * @return a unique name, for after the pi
	 */
	private String makeModeName(AutomatonMode am)
	{
		String baseName = am.name;
		int id = 2;
		String name = baseName + "_" + id;

		while (ha.modes.containsKey(name))
			name = baseName + "_" + (++id);

		return name;
	}

	/**
	 * Make an expression from a linear inequality (dot product of vars and coeffs) <=
	 * right-hand-side value
	 * 
	 * @param vars
	 *            the list of variables
	 * @param coeff
	 *            the list of coefficients
	 * @param op
	 *            the operator
	 * @param rhs
	 *            the right hand side
	 * @return the Expression for the desired linear inequality
	 */
	public static Expression makeExpressionFromLinearInequality(List<String> vars, double[] coeff,
			Operator op, double rhs)
	{
		Operation sum = null;

		new Operation(new Constant(coeff[0]), Operator.MULTIPLY, new Variable(vars.get(0)));

		for (int d = 0; d < coeff.length; ++d)
		{
			double c = coeff[d];

			if (c == 0)
				continue;

			Operation term = new Operation(c, Operator.MULTIPLY, vars.get(d));

			if (sum == null)
				sum = term;
			else
				sum = new Operation(sum, Operator.ADD, term);
		}

		if (sum == null)
			throw new AutomatonExportException(
					"all coefficients were zero when constructing linear inequality");

		return new Operation(sum, op, new Constant(rhs));
	}

	private static double dot(double[] a, double[] b)
	{
		double rv = 0;

		for (int i = 0; i < a.length; ++i)
			rv += a[i] * b[i];

		return rv;
	}
}
