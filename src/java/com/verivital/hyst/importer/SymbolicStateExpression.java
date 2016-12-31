package com.verivital.hyst.importer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Location;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExBaseComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;

/**
 * This is a container class used when importing. Basically, it stores a set of discrete modes and
 * an equation for the continuous constraints
 * 
 * The discrete modes are divided up into the modes of the sub-automata in the networked system.
 * 
 * Initially the symbolic state is unconstrainted (all discrete states and all continuous states)
 * 
 * @author Stanley Bak
 *
 */
public class SymbolicStateExpression
{
	// example: [ ["on", "off"], ["blocked", "trickle", "flowing"] ]
	private List<Collection<String>> discStates;

	// example: x <= 5 && x >= 4
	private Expression contStates;

	/**
	 * Create a new symbolic state
	 * 
	 * @param entireSpace
	 *            should this state be initialized to the entire state space?
	 */
	public SymbolicStateExpression(boolean entireSpace)
	{
		if (doc == null)
			throw new RuntimeException(
					"static SymbolicState.setComponent() method must be called before constructor");

		if (entireSpace)
		{
			discStates = getAllDiscStates();
			contStates = Constant.TRUE;
		}
		else
		{
			discStates = new ArrayList<Collection<String>>();

			for (int i = 0; i < instanceNames.size(); ++i)
				discStates.add(new ArrayList<String>());

			contStates = Constant.FALSE;
		}
	}

	/**
	 * Create a deep copy of the symbolic state
	 */
	public SymbolicStateExpression copy()
	{
		SymbolicStateExpression rv = new SymbolicStateExpression(false);

		rv.contStates = contStates.copy();

		for (int i = 0; i < discStates.size(); ++i)
		{
			Collection<String> possiblities = discStates.get(i);
			Collection<String> rvPoss = rv.discStates.get(i);

			rvPoss.addAll(possiblities);
		}

		return rv;
	}

	private List<Collection<String>> getAllDiscStates()
	{
		List<Collection<String>> rv = new ArrayList<Collection<String>>(instanceTypes.size());

		for (String id : instanceTypes)
		{
			SpaceExBaseComponent c = (SpaceExBaseComponent) doc.getComponent(id);

			ArrayList<String> modes = new ArrayList<String>(c.getLocationCount());

			for (int i = 0; i < c.getLocationCount(); ++i)
			{
				Location l = c.getLocation(i);
				String name = l.getName();

				modes.add(name);
			}

			rv.add(modes);
		}

		return rv;
	}

	public void addContinuousConstraint(Expression e)
	{
		assert Operator.isComparison(e.asOperation().op);

		if (contStates == Constant.TRUE)
			contStates = e;
		else if (contStates instanceof Operation && contStates.asOperation().op == Operator.AND)
			contStates.asOperation().children.add(e); // big and expression
		else
			contStates = new Operation(Operator.AND, contStates, e);
	}

	public void addDiscreteConstraint(String instance, String state)
	{
		int index = instanceNames.indexOf(instance);

		if (index == -1)
			throw new AutomatonExportException(
					"Instance named " + instance + " not found in automaton.");

		Collection<String> states = discStates.get(index);

		String id = instanceTypes.get(index);

		if (!componentContainsDiscreteState(id, state))
			throw new AutomatonExportException(
					"Automaton doesn't contain a state with the given name: loc(" + instance
							+ ") = " + state);

		if (!states.contains(state))
			throw new AutomatonExportException(
					"Unrealizable symbolic state due to contradictory discrete location assignment: "
							+ "loc(" + instance + ") = " + state);

		// set it to the (single) discrete state
		states.clear();
		states.add(state);
		// states.add(instance + "." + state);
	}

	public String discreteStateString()
	{
		String rv = "";

		for (Collection<String> list : discStates)
		{
			String listStr = "";

			for (String s : list)
			{
				if (listStr.length() > 0)
					listStr += ", ";

				listStr += s;
			}

			listStr = "[" + listStr + "]";

			if (rv.length() > 0)
				rv += ", ";

			rv += listStr;
		}

		return "[" + rv + "]";
	}

	/**
	 * Add these symbolic states to the passed-in map
	 * 
	 * @param map
	 *            where to store the results
	 */
	public void exportStates(LinkedHashMap<String, Expression> map)
	{
		ArrayList<String> discFlat = getFlatDiscreteStatesRec(0, new StringBuffer(""));

		contStates = splitLargeConjunctions(contStates);

		for (String s : discFlat)
		{
			Expression e = map.get(s);

			if (e == null)
				map.put(s, contStates);
			else
				map.put(s, new Operation(Operator.OR, e, contStates));
		}
	}

	Expression makeBalancedAnd(List<Expression> children)
	{
		Expression rv;

		if (children.size() == 1)
			rv = children.get(0);
		else
		{
			int middleIndex = children.size() / 2;

			rv = new Operation(Operator.AND, makeBalancedAnd(children.subList(0, middleIndex)),
					makeBalancedAnd(children.subList(middleIndex, children.size())));
		}

		return rv;
	}

	Expression splitLargeConjunctions(Expression e)
	{
		Expression rv = e;

		if (e instanceof Operation)
		{
			Operation o = e.asOperation();

			if (o.op == Operator.AND && o.children.size() > 2)
			{
				rv = makeBalancedAnd(o.children);
			}

		}

		return rv;
	}

	/**
	 * get the (flattened) set of discrete states corresponding to this symbolic state
	 * 
	 * @param componentIndex
	 *            the index of the component
	 * @param accumulatedState
	 *            the current name of the state
	 * @return the list of corresponding flattened discrete states
	 */
	private ArrayList<String> getFlatDiscreteStatesRec(int componentIndex,
			StringBuffer accumulatedState)
	{
		ArrayList<String> rv = null;

		if (componentIndex < instanceTypes.size())
		{
			Collection<String> states = discStates.get(componentIndex);

			StringBuffer base = accumulatedState;

			if (base.length() > 0)
				base.append(".");

			for (String s : states)
			{
				StringBuffer newBase = new StringBuffer(base);
				newBase.append(s);

				ArrayList<String> a = getFlatDiscreteStatesRec(componentIndex + 1, newBase);

				if (rv == null)
					rv = a;
				else
					rv.addAll(a);
			}
		}
		else
		{
			rv = new ArrayList<String>(1);
			rv.add(accumulatedState.toString());
		}

		return rv;
	}

	///////////////////
	/// static below //
	///////////////////

	private static SpaceExDocument doc;

	// example: ["net.controller_i", "net.plant_i"]
	private static List<String> instanceNames;

	// example: ["controller", "plant"]
	private static List<String> instanceTypes;

	/**
	 * Initializes the instanceNames and instanceTypes (statically stored)
	 * 
	 * @param root
	 *            the root component
	 * @param doc
	 *            the spaceExDoc it came from
	 */
	public static void setComponent(SpaceExComponent root, SpaceExDocument doc)
	{
		if (doc == null)
			throw new RuntimeException("doc cannot be null");

		SymbolicStateExpression.doc = doc;

		instanceNames = getInstanceNames("", root);
		instanceTypes = getInstanceTypes(root);
	}

	private static boolean componentContainsDiscreteState(String id, String state)
	{
		boolean rv = false;

		SpaceExBaseComponent c = (SpaceExBaseComponent) doc.getComponent(id);

		for (int i = 0; i < c.getLocationCount(); ++i)
		{
			Location l = c.getLocation(i);

			if (l.getName().equals(state))
			{
				rv = true;
				break;
			}
		}

		return rv;
	}

	private static List<String> getInstanceTypes(SpaceExComponent c)
	{
		List<String> rv = new ArrayList<String>();

		if (c instanceof SpaceExBaseComponent)
			rv.add(c.getID());
		else
		{
			SpaceExNetworkComponent nc = (SpaceExNetworkComponent) c;

			for (int i = 0; i < nc.getBindCount(); ++i)
			{
				Bind b = nc.getBind(i);

				String compName = b.getComponent();
				SpaceExComponent comp = doc.getComponent(compName);

				rv.addAll(getInstanceTypes(comp));
			}
		}

		return rv;
	}

	private static List<String> getInstanceNames(String instanceName, SpaceExComponent c)
	{
		List<String> rv = new ArrayList<String>();

		if (c instanceof SpaceExBaseComponent)
			rv.add(instanceName);
		else
		{
			SpaceExNetworkComponent nc = (SpaceExNetworkComponent) c;

			for (int i = 0; i < nc.getBindCount(); ++i)
			{
				Bind b = nc.getBind(i);

				String compName = b.getComponent();
				SpaceExComponent comp = doc.getComponent(compName);

				String newInstName = (instanceName.length() > 0 ? instanceName + "." : "")
						+ b.getAs();

				rv.addAll(getInstanceNames(newInstName, comp));
			}
		}

		return rv;
	}

	/**
	 * Recursively extract the symbolic (start) states from an expression
	 * 
	 * @param e
	 *            the expression to parse
	 * @param description
	 *            the text description of the states being parsed, like "initial states"
	 */
	public static List<SymbolicStateExpression> extractSymbolicStates(Expression e,
			String description)
	{
		List<SymbolicStateExpression> rv = new ArrayList<SymbolicStateExpression>();
		rv.add(new SymbolicStateExpression(true));

		try
		{
			extractSymbolicStatesRec(rv, e);
		}
		catch (AutomatonExportException ex)
		{
			throw new AutomatonExportException(
					"Error while processing " + description + "  with expression: "
							+ e.toDefaultString() + " and return value: " + rv + ".",
					ex);
		}

		return rv;
	}

	private static void extractSymbolicStatesRec(List<SymbolicStateExpression> rv, Expression e)
	{
		Operation o = e.asOperation();

		if (o.op == Operator.OR)
		{
			List<SymbolicStateExpression> rvRight = new ArrayList<SymbolicStateExpression>();

			for (SymbolicStateExpression ss : rv)
				rvRight.add(ss.copy());

			extractSymbolicStatesRec(rv, o.getLeft());
			extractSymbolicStatesRec(rvRight, o.getRight());

			rv.addAll(rvRight);
		}
		else if (o.op == Operator.AND)
		{
			extractSymbolicStatesRec(rv, o.getLeft());
			extractSymbolicStatesRec(rv, o.getRight());
		}
		else if (o.op == Operator.EQUAL && o.getLeft() instanceof Operation
				&& o.getLeft().asOperation().op == Operator.LOC)
		{
			Operation locFunction = o.getLeft().asOperation();
			// base case: discrete (loc) constraint
			String instance = locFunction.children.size() == 0 ? ""
					: ((Variable) locFunction.children.get(0)).name;
			String state = ((Variable) o.getRight()).name;

			for (SymbolicStateExpression ss : rv)
				ss.addDiscreteConstraint(instance, state);
		}
		else if (Operator.isComparison(o.op))
		{
			// base case: continuous constraint

			for (SymbolicStateExpression ss : rv)
				ss.addContinuousConstraint(e);
		}
		else
			throw new AutomatonExportException(
					"Error extracting symbolic states from expression: " + e);
	}

	public String toString()
	{
		StringBuilder discStr = new StringBuilder();
		// disc states example: [ ["on", "off"], ["blocked", "trickle",
		// "flowing"] ]

		discStr.append("[");
		for (Collection<String> modes : discStates)
		{
			discStr.append("[");
			String prefix = "";

			for (String m : modes)
			{
				discStr.append(prefix + m);
				prefix = ", ";
			}

			discStr.append("] ");
		}
		discStr.append("]");

		return discStates.toString() + " with equation " + this.contStates.toDefaultString();
	}
}
