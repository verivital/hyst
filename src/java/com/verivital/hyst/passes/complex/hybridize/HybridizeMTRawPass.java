package com.verivital.hyst.passes.complex.hybridize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.HyperRectangle;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.internalpasses.ConvertFromStandardForm;
import com.verivital.hyst.internalpasses.ConvertToStandardForm;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.basic.RemoveDiscreteUnreachablePass;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationModeParams;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationParams;
import com.verivital.hyst.passes.complex.pi.PseudoInvariantPass;
import com.verivital.hyst.util.HyperRectangleArrayOptionHandler;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;
import com.verivital.hyst.util.StringOperations;

/**
 * This is the hybridize mixed-triggered pass from the HSCC 2016 paper,
 * "Scalable Static Hybridization Methods for Analysis of Nonlinear Systems" by Bak et. al
 * 
 * This is the 'raw' version of the transformation, which in the paper is described in Section 5.
 * The paremeters in the paper are:
 * 
 * a list of splitting elements E_1, ... E_{n-1}, where each element E_i is either a real number to
 * be used for time-triggered splitting, or a PI function to be used for space-triggered splitting
 * (list 1),
 *
 * D_1, ... D_n are the contraction domains (sets) for each new location (list 2), and
 *
 * g_1, ... g_n$ are the dynamics abstraction functions for each location (list 3).
 *
 * In this pass, the g's are automatically computed based on global optimization within the
 * contraction domains and a linear approximation (based on sampling near the center of each
 * contraction domain). Thus, only two lists are paramters of this pass.
 * 
 * Elements of list 1 are single numbers (time-triggered), or two comma-separated lists, separated
 * by a semicolon (space-triggered). Both should be surrounded by parenthesis. The PI-function is
 * taken by considering the hyperplane at the given-point (first comma- separated list), in the
 * direction of the gradient (second comma-separated list). For example: '(0.1) (0.1) (0.25,0;1,1)
 * (0.2)'
 * 
 * Elements of list 2 are hyperrectangles, which use commas to separate min and max, and semicolons
 * to separate dimensions, surrounded by parenthesis. For example: '(0.1,0.2;2.1,2.2)
 * (-0.2,-0.1;2.2,2.3)'
 * 
 * In addition to these parameters from the paper, the optimization method can be chosen:
 * 
 * opt the optimization method, one of {basinhopping, interval, intervalXYZ} where XYZ is the
 * maximum overapproximation error (low values in high dimensions may take longer)
 * 
 * Additionally, the user can (optionally) specify a trigger mode which indicates the transformation
 * should begin; otherwise it begins at time 0.
 * 
 * If there are N domains given and N-1 splitting elements, the final (abstracted dynamics) mode
 * will be the last one where the trajectories will remain for all time.
 * 
 * If there are N domains and N splitting elements, after the last splitting element, the state will
 * transition back to the original automaton (to every possible state, only restricted by the
 * invariant).
 *
 * @author Stanley Bak
 *
 */
public class HybridizeMTRawPass extends TransformationPass
{
	@Option(name = "-E", aliases = {
			"-splitelements" }, required = true, handler = SplittingElementArrayOptionHandler.class, usage = "A list of splitting elements, where each element is either a real "
					+ "number to be used for time-triggered splitting, or a PI function to be used for "
					+ "space-triggered splitting. Elements are single numbers (time-triggered), or two "
					+ "comma-separated lists, separated by a semicolon (space-triggered). Both should be "
					+ "surrounded by parenthesis. The PI-function is taken by considering the hyperplane "
					+ "at the given-point (first comma-separated list), in the direction of the gradient "
					+ "(second comma-separated list).\n"
					+ "For example: '(0.1) (0.1) (0.25,0;1,1) (0.2)'", metaVar = "E_1 E_2 ...")
	List<SplittingElement> splitElements;

	// D or domains
	@Option(name = "-D", aliases = {
			"-domains" }, required = true, handler = HyperRectangleArrayOptionHandler.class, usage = "The contraction domains (sets) for each new location. Domains are given as "
					+ "hyperrectangles, which use commas to separate min and max, and semicolons "
					+ "to separate dimensions, surrounded by parenthesis.\n"
					+ "For example: '(-0.2,-0.1;2.1,2.2) (-0.2,-0.1;2.2,2.3)'", metaVar = "D_1 D_2 ...")
	List<HyperRectangle> domains;

	// O or optimization
	@Option(name = "-O", aliases = {
			"-opt" }, usage = "the optimization method, one of {basinhopping, kodiak, interval, interval#, "
					+ "where # is the max error, like 0.1}", metaVar = "METHOD")
	String opt = "basinhopping";

	@Option(name = "-T", aliases = {
			"-triggermode" }, usage = "the name of the mode which triggers entering the MTH-chain", metaVar = "NAME")
	String triggerMode = null;

	// no error
	@Option(name = "-noerror", usage = "do not insert the forbidden DCEM mode (useful for plotting)")
	boolean noError = false;

	private BaseComponent ha;
	private static final String TT_VARIABLE = "_tt";
	private int ttVarIndex = -1;
	private static final Expression ttGreaterThanZero = new Operation(Operator.GREATEREQUAL,
			TT_VARIABLE, 0);

	public ArrayList<AutomatonMode> modeChain = new ArrayList<AutomatonMode>();
	public ArrayList<HyperRectangle> modeChainInvariants = new ArrayList<HyperRectangle>();
	private int chainModeCount = 0;
	private AutomatonMode errorMode;
	private AutomatonMode initMode;

	/**
	 * Construct the parameter string
	 * 
	 * @param splitElements
	 * @param domains
	 * @param opt
	 *            (can be null)
	 * @param triggerMode
	 *            (can be null)
	 * @return
	 */
	public static String makeParamString(List<SplittingElement> splitElements,
			List<HyperRectangle> domains, String opt, String triggerMode, boolean noError)
	{
		StringBuffer rv = new StringBuffer();

		if (opt != null)
			rv.append("-opt " + opt + " ");

		if (triggerMode != null)
			rv.append("-triggermode " + triggerMode + " ");

		if (splitElements.size() == 0)
			throw new AutomatonExportException("No split elements were given");

		rv.append("-splitelements ");

		for (SplittingElement se : splitElements)
		{
			if (se instanceof TimeSplittingElement)
				rv.append("(" + ((TimeSplittingElement) se).time + ") ");
			else
			{
				SpaceSplittingElement sse = (SpaceSplittingElement) se;

				rv.append("(");

				for (int d = 0; d < sse.pt.dims.length; ++d)
					rv.append(sse.pt.dims[d] + ",");

				rv.append(";");

				for (int d = 0; d < sse.gradient.length; ++d)
					rv.append(sse.gradient[d] + ",");

				rv.append(") ");
			}
		}

		rv.append("-domains ");

		for (HyperRectangle hr : domains)
		{
			rv.append("(");

			for (int d = 0; d < hr.dims.length; ++d)
				rv.append(hr.dims[d].min + "," + hr.dims[d].max + ";");

			rv.append(") ");
		}

		if (noError)
			rv.append(" -noerror");

		return rv.toString().trim();
	}

	@Override
	public String getCommandLineFlag()
	{
		return "hybridizemtraw";
	}

	@Override
	public String getName()
	{
		return "Raw Hybridization Mixed-triggered pass";
	}

	@Override
	public String getLongHelp()
	{
		return "This is the raw version of the hybridize mixed-triggered pass from "
				+ "the HSCC 2016 paper, "
				+ "'Scalable Static Hybridization Methods for Analysis of Nonlinear Systems'"
				+ " by Bak et. al.";
	}

	@Override
	protected void runPass()
	{
		ha = (BaseComponent) config.root;

		checkParams();
		ConvertToStandardForm.run(config);

		errorMode = ConvertToStandardForm.getErrorMode(ha);
		initMode = ConvertToStandardForm.getInitMode(ha);

		makeTimeTriggerVariable();

		// this populates modeChain
		constructChain();
		runOptimization(opt, getNonChainModes(), modeChain, modeChainInvariants);

		redirectStart();

		new RemoveDiscreteUnreachablePass().runVanillaPass(config, "");

		ConvertFromStandardForm.convertInit(config);

		config.settings.spaceExConfig.timeTriggered = true;

		if (noError)
			removeErrorModes();
	}

	private void removeErrorModes()
	{
		errorMode = ConvertToStandardForm.getErrorMode(ha);

		ha.modes.remove(errorMode.name);

		ArrayList<AutomatonTransition> toRemove = new ArrayList<AutomatonTransition>();

		for (AutomatonTransition at : ha.transitions)
		{
			if (at.to == errorMode)
				toRemove.add(at);
		}

		ha.transitions.removeAll(toRemove);

		config.forbidden.remove(errorMode.name);
		config.init.remove(errorMode.name);

		config.validate();
	}

	/**
	 * Populate modeChain
	 */
	private void constructChain()
	{
		Expression previousGuard = null;
		AutomatonMode previousMode = null;

		for (int i = 0; i < domains.size(); ++i)
		{
			HyperRectangle domain = domains.get(i);

			String type = "final";

			if (i < splitElements.size() && splitElements.get(i) instanceof TimeSplittingElement)
				type = "time_trig";
			else if (i < splitElements.size()
					&& splitElements.get(i) instanceof SpaceSplittingElement)
				type = "space_trig";

			AutomatonMode curMode = makeModeInChain(domain, type);

			// add default tt flow
			curMode.flowDynamics.put(TT_VARIABLE, new ExpressionInterval(0));

			// add a transition based on previousGuard
			if (previousGuard != null)
			{
				addChainTransition(previousMode, curMode, previousGuard, domain);
				previousGuard = null;
			}

			// setup previousGuard for the next mode
			if (i < splitElements.size())
			{
				SplittingElement e = splitElements.get(i);

				if (e instanceof TimeSplittingElement)
				{
					TimeSplittingElement tse = (TimeSplittingElement) e;
					previousGuard = makeTimeTriggeredGuard(curMode);
					addTimeResetToIncomingTransitions(curMode, tse.time);

					// also update the flow
					curMode.flowDynamics.put(TT_VARIABLE, new ExpressionInterval(-1));
				}
				else
				{
					SpaceSplittingElement sse = (SpaceSplittingElement) e;
					previousGuard = makeSpaceTriggeredGuard(curMode, sse.pt, sse.gradient);
					addTimeResetToIncomingTransitions(curMode, 0);
				}
			}

			previousMode = curMode;
		}

		if (previousGuard != null && triggerMode != null)
		{
			// previousMode is the last mode in the chain
			redirectEnd(previousMode, previousGuard);
		}
	}

	/**
	 * Add the time-trigger reset condition for all incoming states.
	 * 
	 * @param am
	 *            the mode
	 * @param val
	 *            the dwell time in the mode (0 = space-triggered)
	 */
	private void addTimeResetToIncomingTransitions(AutomatonMode am, double val)
	{
		for (AutomatonTransition at : ha.transitions)
		{
			if (at.to == am)
				at.reset.put(TT_VARIABLE, new ExpressionInterval(val));
		}
	}

	private List<AutomatonMode> getNonChainModes()
	{
		ArrayList<AutomatonMode> rv = new ArrayList<AutomatonMode>();

		for (AutomatonMode am : ha.modes.values())
		{
			if (am == errorMode || am == initMode)
				continue;

			if (modeChain.contains(am))
				continue;

			rv.add(am);
		}

		return rv;
	}

	/**
	 * Do linear approximation over all the modes in the chain. This modifies the flow dynamics for
	 * each mode in the chain.
	 * 
	 * @param optimizationType
	 *            the optimization engine to use, one of "basinhopping", "kodiak", "interval",
	 *            "intervalXX"
	 * @param oldModes
	 *            The list of all modes in the original automaton
	 * @param modeChain
	 *            the list of modes in the chain
	 * @param rects
	 *            the box invariant sets corresponding to each mode in the chain
	 */
	public static void runOptimization(String optimizationType, Collection<AutomatonMode> oldModes,
			List<AutomatonMode> modeChain, List<HyperRectangle> rects)
	{
		if (modeChain.size() == 0)
			throw new AutomatonExportException("runOptimization was called an empty modeChain");

		Hyst.logDebug("runOptimization called with oldModes = " + oldModes);

		List<String> vars = getNonTTVaraibles(modeChain.get(0).automaton);

		List<OptimizationParams> params = new ArrayList<OptimizationParams>();

		// parallel lists for every mode in the mode chain
		int chainLen = modeChain.size();
		List<Integer> boxIntersectsCount = new ArrayList<Integer>(chainLen);

		for (int i = 0; i < chainLen; ++i)
		{
			HyperRectangle box = rects.get(i);

			List<AutomatonMode> boxIntersects = getIntersectingModes(oldModes, box);
			boxIntersectsCount.add(boxIntersects.size());

			LinkedHashMap<String, ExpressionInterval> avgFlow = getAverageFlow(boxIntersects);
			OptimizationParams op = new OptimizationParams();

			HashMap<String, Interval> bounds = toVariableBounds(box, vars);
			op.newDynamics = AffineOptimize.affineApprox(avgFlow, bounds);
			params.add(op);

			Hyst.logDebug("Processing " + modeChain.get(i).name + ", avgDynamics = "
					+ StringOperations.makeDefaultEiMapString(avgFlow));

			// update the invariant if they're all equal
			Expression invariant = boxIntersects.get(0).invariant;

			for (int m = 1; m < boxIntersects.size(); ++m)
			{
				if (!boxIntersects.get(m).invariant.toDefaultString()
						.equals(invariant.toDefaultString()))
				{
					// invariant SHOULD be disjunction of all modes, but we'll
					// be pessimistic
					invariant = Constant.TRUE;
					break;
				}
			}

			if (invariant != Constant.TRUE)
				modeChain.get(i).invariant = Expression.and(invariant, modeChain.get(i).invariant);

			for (int imIndex = 0; imIndex < boxIntersects.size(); ++imIndex)
			{
				AutomatonMode mode = boxIntersects.get(imIndex);
				OptimizationModeParams modeParams = new OptimizationModeParams();
				op.origModes.add(modeParams);

				modeParams.bounds = invIntersection(mode, box);
				modeParams.origDynamics = removeTTFlow(mode.flowDynamics);
			}
		}

		AffineOptimize.optimizeDynamics(optimizationType, params);

		for (int i = 0; i < chainLen; ++i)
		{
			AutomatonMode chainMode = modeChain.get(i);
			OptimizationParams op = params.get(i);

			chainMode.flowDynamics.putAll(op.result);

			Hyst.logDebug("Final Flow for " + chainMode.name + " was "
					+ StringOperations.makeDefaultEiMapString(chainMode.flowDynamics));
		}
	}

	private static LinkedHashMap<String, ExpressionInterval> removeTTFlow(
			LinkedHashMap<String, ExpressionInterval> flowDynamics)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();

		rv.putAll(flowDynamics);
		rv.remove(TT_VARIABLE);

		return rv;
	}

	private static HashMap<String, Interval> toVariableBounds(HyperRectangle box, List<String> vars)
	{
		HashMap<String, Interval> rv = new HashMap<String, Interval>();
		int i = 0;

		for (String var : vars)
			rv.put(var, box.dims[i++]);

		return rv;
	}

	private static List<String> getNonTTVaraibles(Component c)
	{
		List<String> vars = new ArrayList<String>();
		vars.addAll(c.variables);
		vars.remove(TT_VARIABLE);

		return vars;
	}

	/**
	 * Get the intersection (as a hashmap of var->intervals) of a mode's invariant and a box
	 * 
	 * @param am
	 *            the mode
	 * @param box
	 * @return
	 */
	private static HashMap<String, Interval> invIntersection(AutomatonMode am, HyperRectangle box)
	{
		HashMap<String, Interval> rv = new HashMap<String, Interval>();
		List<String> vars = getNonTTVaraibles(am.automaton);

		HyperRectangle invBox = expressionToBox(am.invariant, vars);
		HyperRectangle inter = HyperRectangle.intersection(invBox, box);
		int i = 0;

		for (String var : vars)
			rv.put(var, inter.dims[i++]);

		return rv;
	}

	private static LinkedHashMap<String, ExpressionInterval> getAverageFlow(
			List<AutomatonMode> modeList)
	{
		LinkedHashMap<String, ExpressionInterval> rv = new LinkedHashMap<String, ExpressionInterval>();
		List<String> vars = getNonTTVaraibles(modeList.get(0).automaton);

		Hyst.logDebug("getting avg dynamics of modes: " + modeList);

		for (AutomatonMode am : modeList)
		{
			for (String var : vars)
			{
				Expression e = am.flowDynamics.get(var).asExpression(); // intervals
																		// not
																		// allowed
																		// so
																		// far
				ExpressionInterval cur = rv.get(var);

				if (cur != null)
					rv.put(var, new ExpressionInterval(
							new Operation(Operator.ADD, e, cur.asExpression())));
				else
					rv.put(var, new ExpressionInterval(e));
			}
		}

		// divide by num expression
		int numExpressions = modeList.size();

		for (String var : vars)
		{
			Expression cur = rv.get(var).asExpression();

			rv.put(var, new ExpressionInterval(
					new Operation(Operator.DIVIDE, cur, new Constant(numExpressions))));
		}

		Hyst.logDebug("computed avg dynamics: " + StringOperations.makeDefaultEiMapString(rv));

		return rv;
	}

	/**
	 * Convert an invariant expression to a hyper rectangle
	 * 
	 * @param inv
	 *            an inveriant expression
	 * @param vars
	 *            a list of variables, in order
	 * @return the invariant box
	 */
	private static HyperRectangle expressionToBox(Expression inv, List<String> vars)
	{
		HashMap<String, Interval> ranges = new HashMap<String, Interval>();

		try
		{
			RangeExtractor.getVariableRanges(inv, ranges);
		}
		catch (EmptyRangeException e)
		{
			throw new AutomatonExportException(
					"Error converting mode invariant to box: " + inv.toDefaultString(), e);
		}
		catch (ConstantMismatchException e)
		{
			throw new AutomatonExportException(
					"Error converting mode invariant to box: " + inv.toDefaultString(), e);
		}
		catch (UnsupportedConditionException e)
		{
			throw new AutomatonExportException(
					"Error converting mode invariant to box: " + inv.toDefaultString(), e);
		}

		HyperRectangle rv = new HyperRectangle(vars.size());
		int i = 0;

		for (String v : vars)
		{
			Interval range = ranges.get(v);

			if (range == null)
				range = new Interval(-Double.MAX_VALUE, Double.MAX_VALUE);

			rv.dims[i++] = range;
		}

		return rv;
	}

	private static List<AutomatonMode> getIntersectingModes(Collection<AutomatonMode> oldModes,
			HyperRectangle box)
	{
		assert oldModes.size() > 0;

		ArrayList<AutomatonMode> rv = new ArrayList<AutomatonMode>();

		// remove time-triggered variable
		List<String> vars = getNonTTVaraibles(oldModes.iterator().next().automaton);

		for (AutomatonMode am : oldModes)
		{
			HyperRectangle modeBox = expressionToBox(am.invariant, vars);

			if (HyperRectangle.intersection(modeBox, box) != null)
				rv.add(am);
		}

		return rv;
	}

	private void redirectStart()
	{
		AutomatonMode firstMode = modeChain.get(0);
		HyperRectangle firstBox = modeChainInvariants.get(0);
		ArrayList<AutomatonTransition> originalTransitions = new ArrayList<AutomatonTransition>();
		originalTransitions.addAll(ha.transitions);

		if (triggerMode == null)
		{
			// redirect outgoing transitions from _init to firstMode
			for (AutomatonTransition at : originalTransitions)
			{
				if (at.from == initMode)
				{
					at.to = firstMode;
					addErrorTransitionsAtGuard(at.from, at.guard, firstBox);

					// add the initial reset
					SplittingElement e = splitElements.get(0);

					if (e instanceof TimeSplittingElement)
					{
						TimeSplittingElement tse = (TimeSplittingElement) e;
						Operation op = new Operation(TT_VARIABLE, Operator.EQUAL, tse.time);
						at.guard = Expression.and(at.guard, op);
					}
					else
					{
						// space triggered
						Operation op = new Operation(TT_VARIABLE, Operator.EQUAL, 0);
						at.guard = Expression.and(at.guard, op);
					}
				}
			}
		}
		else
		{
			// there was a trigger mode. We should consider all INCOMING
			// transitions and
			// redirect them to firstMode.

			for (AutomatonTransition at : originalTransitions)
			{
				if (at.to.name == triggerMode)
				{
					at.to = firstMode;

					addErrorTransitionsAtGuard(at.from, at.guard, firstBox);
				}
			}
		}
	}

	/**
	 * Redirect the end of the chain back to the original automaton
	 * 
	 * @param lastMode
	 *            the last mode in the chain
	 * @param guard
	 *            the guard at which to transition out of it
	 */
	private void redirectEnd(AutomatonMode lastMode, Expression guard)
	{
		for (AutomatonMode am : ha.modes.values())
		{
			// skip init and error
			if (am == errorMode || am == initMode)
				continue;

			// skip the constructed mode chain
			if (modeChain.contains(am))
				continue;

			// we might be able to optimize here by checking for satisfiability
			// of
			// guard and am.invariant
			AutomatonTransition at = ha.createTransition(lastMode, am);
			at.guard = guard.copy();
		}
	}

	/**
	 * Create a transition within the chain
	 * 
	 * @param previousMode
	 * @param curMode
	 * @param previousGuard
	 * @param domain
	 */
	private void addChainTransition(AutomatonMode previousMode, AutomatonMode curMode,
			Expression previousGuard, HyperRectangle domain)
	{
		AutomatonTransition at = ha.createTransition(previousMode, curMode);
		at.guard = previousGuard;

		// add error transitions at guard
		addErrorTransitionsAtGuard(previousMode, previousGuard, domain);

		// premode adding (for plotting) used to be done here.
		// we probably want a more generic way to do this (for example a pass
		// which takes
		// a regexp of mode names to match against to create pre-modes)
	}

	/**
	 * Add transitions at the guard (for example time trigger) to the error mode (negation of
	 * invariant)
	 * 
	 * @param am
	 *            the mode to add to
	 * @param tt
	 *            the time-trigger time
	 * @param hr
	 *            the rectangle bounds
	 */
	private void addErrorTransitionsAtGuard(AutomatonMode am, Expression guard,
			HyperRectangle nextModeInvariant)
	{
		for (int d = 0; d < nextModeInvariant.dims.length; ++d)
		{
			if (d == ttVarIndex)
				continue;

			Interval i = nextModeInvariant.dims[d];

			Variable v = new Variable(ha.variables.get(d));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.min));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.max));

			ha.createTransition(am, errorMode).guard = Expression.and(guard.copy(), le);
			ha.createTransition(am, errorMode).guard = Expression.and(guard.copy(), ge);
		}
	}

	/**
	 * Make the outgoing guard for this state (which is time-triggered). Also, update the invariant
	 * to reflect the guard.
	 * 
	 * @param am
	 * @param time
	 * @return
	 */
	private Expression makeTimeTriggeredGuard(AutomatonMode am)
	{
		am.flowDynamics.put(TT_VARIABLE, new ExpressionInterval(new Constant(-1)));

		// transition condition for next state
		return new Operation(Operator.LESSEQUAL, TT_VARIABLE, 0);
	}

	/**
	 * Make the outgoing guard for this state (which is time-triggered). Also, update the invariant
	 * to reflect the guard.
	 * 
	 * @param am
	 * @param pt
	 * @param gradient
	 * @return
	 */
	private Expression makeSpaceTriggeredGuard(AutomatonMode am, HyperPoint pt, double[] gradient)
	{
		am.flowDynamics.put(TT_VARIABLE, new ExpressionInterval(new Constant(0)));

		am.invariant = Expression.and(am.invariant, makePiInvariant(pt, gradient));

		return makePiGuard(pt, gradient);
	}

	/**
	 * Make an expression which represents the pi invariant at the given point with the given
	 * gradient
	 * 
	 * @param p
	 *            the point where the pi is being constructed
	 * @param gradient
	 *            the gradient of the point
	 * @return the expression representing the invariant
	 */
	private Expression makePiInvariant(HyperPoint p, double[] gradient)
	{
		double value = dotProduct(gradient, p);
		List<String> vars = getNonTTVaraibles(ha);

		return PseudoInvariantPass.makeExpressionFromLinearInequality(vars, gradient,
				Operator.LESSEQUAL, value);
	}

	/**
	 * Make an expression which represents the pi guard at the given point with the given gradient
	 * 
	 * @param p
	 *            the point where the pi is being constructed
	 * @param gradient
	 *            the gradient of the point
	 * @return the expression representing the guard
	 */
	private Expression makePiGuard(HyperPoint p, double[] gradient)
	{
		double value = dotProduct(gradient, p);
		List<String> vars = getNonTTVaraibles(ha);

		return PseudoInvariantPass.makeExpressionFromLinearInequality(vars, gradient,
				Operator.GREATEREQUAL, value);
	}

	/**
	 * Evaluate a dot product of a gradient and a hyperpoint
	 * 
	 * @param gradient
	 * @param p
	 * @return the cross product
	 */
	private static double dotProduct(double[] gradient, HyperPoint p)
	{
		if (gradient.length != p.dims.length)
			throw new RuntimeException(
					"eval gradient requires gradient and point have same number of dimensions");

		double rv = 0;

		for (int d = 0; d < gradient.length; ++d)
			rv += gradient[d] * p.dims[d];

		return rv;
	}

	/**
	 * Create a mode in the currently-constructed chain.
	 * 
	 * @param domain
	 *            the domain to use as the invariant
	 * @param typeName
	 *            the type of mode, "final", "time_trig", or "space_trig"
	 * @return the constructed mode
	 */
	private AutomatonMode makeModeInChain(HyperRectangle domain, String typeName)
	{
		AutomatonMode am = ha.createMode(nextModeName(typeName));

		// store mode and rectangle for optimization
		modeChain.add(am);
		modeChainInvariants.add(domain);

		// dynamics are set during optimization

		// set invariant (probably not the best way to check if it's a
		// time-triggered transition)
		if (typeName.equals("time_trig"))
			am.invariant = ttGreaterThanZero;
		else
			am.invariant = Constant.TRUE;

		addRectangleInvariant(am, domain);

		// add transitions to error mode during the computation in this box
		addModeErrorTransitions(am, domain, "_error_tt_inv" + am.name);

		return am;
	}

	/**
	 * Add transitions to the error mode on each side of the box for a given mode
	 * 
	 * @param am
	 *            the mode
	 * @param hr
	 *            the invariant box of the mode
	 * @param errorModeName
	 *            the name of the error mode to transition to
	 */
	private void addModeErrorTransitions(AutomatonMode am, HyperRectangle hr, String errorModeName)
	{
		for (int d = 0; d < hr.dims.length; ++d)
		{
			if (d == ttVarIndex)
				continue;

			Interval i = hr.dims[d];

			Variable v = new Variable(ha.variables.get(d));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.min));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.max));

			ha.createTransition(am, errorMode).guard = le;
			ha.createTransition(am, errorMode).guard = ge;
		}
	}

	/**
	 * Set the invariant for a newly-constructed mode
	 * 
	 * @param am
	 *            the mode
	 * @param hr
	 *            the rectangle invariant
	 */
	private void addRectangleInvariant(AutomatonMode am, HyperRectangle hr)
	{
		for (int d = 0; d < hr.dims.length; ++d)
		{
			if (d == ttVarIndex)
				continue;

			Interval i = hr.dims[d];
			Variable v = new Variable(ha.variables.get(d));
			Expression ge = new Operation(Operator.GREATEREQUAL, v, new Constant(i.min));
			Expression le = new Operation(Operator.LESSEQUAL, v, new Constant(i.max));
			Expression constraint = Expression.and(ge, le);

			am.invariant = Expression.and(am.invariant, constraint);
		}
	}

	private String nextModeName(String typeName)
	{
		++chainModeCount;
		String name = null;

		while (true)
		{
			String base = "";

			if (triggerMode != null)
				base = "_" + triggerMode;

			name = base + "_" + chainModeCount + "_" + typeName;

			if (!ha.modes.containsKey(name))
				break;

			++chainModeCount;
		}

		return name;
	}

	private void checkParams()
	{
		if (splitElements.size() != domains.size() && splitElements.size() != domains.size() - 1)
			throw new AutomatonExportException("Expected the either the same number of "
					+ "split elements and domains, or one extra domain. Got " + splitElements.size()
					+ " split elements and " + domains.size() + " domains.");

		if (domains.size() == 0)
			throw new AutomatonExportException("expected at least one domain");

		if (!opt.equals("basinhopping") && !opt.equals("kodiak") && !opt.startsWith("interval"))
			throw new AutomatonExportException("unknown optimization method: " + opt);

		int numDims = config.root.variables.size();

		for (SplittingElement se : splitElements)
		{
			if (se instanceof SpaceSplittingElement
					&& ((SpaceSplittingElement) se).pt.dims.length != numDims)
				throw new AutomatonExportException(
						"space-triggered splitting element expected to have " + numDims
								+ " dimensions: " + ((SpaceSplittingElement) se).pt.dims);
		}

		for (HyperRectangle hr : domains)
		{
			if (hr.dims.length != numDims)
				throw new AutomatonExportException(
						"domain should have " + numDims + " dimensions: " + hr);
		}

		if (triggerMode != null && ha.modes.get(triggerMode) == null)
			throw new AutomatonExportException(
					"trigger mode '" + triggerMode + "' not found" + " in automaton");
	}

	/**
	 * makes a time-triggered variable
	 */
	private void makeTimeTriggerVariable()
	{
		// this assumes a single-mode automaton with a single initial state
		// expression
		if (config.root.variables.contains(TT_VARIABLE))
			throw new AutomatonExportException(
					"time-triggered variable already exists: " + TT_VARIABLE);

		config.root.variables.add(TT_VARIABLE);
		ttVarIndex = ha.variables.size() - 1;

		for (AutomatonMode am : ha.modes.values())
		{
			if (am.flowDynamics != null)
				am.flowDynamics.put(TT_VARIABLE, new ExpressionInterval(0));
		}
	}

	public static class SplittingElement
	{

	}

	public static class TimeSplittingElement extends SplittingElement
	{
		public TimeSplittingElement(double d)
		{
			time = d;
		}

		double time;
	}

	public static class SpaceSplittingElement extends SplittingElement
	{
		public SpaceSplittingElement()
		{

		}

		public SpaceSplittingElement(HyperPoint pt, double[] grad)
		{
			this.pt = pt;
			this.gradient = grad;
		}

		HyperPoint pt;
		double[] gradient;
	}
}
