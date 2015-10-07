package com.verivital.hyst.passes.complex;


import java.util.LinkedList;
import java.util.List;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.TransformationPass;


/**
 * This pass splits the initial mode into two using the technique of pseudo-invariants:
 * "Reducing the Wrapping Effect in Flowpipe Construction Using Pseudo-invariants", CyPhy 2014, Bak 2014
 * 
 * The parameter is a time. The center of the initial set is simulated for this time before splitting orthogonal to the gradient.
 * 
 * @author Stanley Bak (October 2014)
 *
 */
public class PseudoInvariantPass extends TransformationPass
{
	private static int UNIQUE_ID = 0;
	BaseComponent ha;
	public List <String> vars;
	
	AutomatonMode applyMode;
	String applyModeName;
	LinkedList <PseudoInvariantParams> params = new LinkedList <PseudoInvariantParams>();
	String initModeName;
	
	int numVars;
	
	@Override
	public String getCommandLineFlag()
	{
		return "-pass_pi";
	}
	
	@Override
	public String getName()
	{
		return "Pseudo-Invariant at Point Pass";
	}
	
	@Override
	public String getParamHelp()
	{
		return "[(modename|)pt1;inv_dir1|pt2;inv_dir2|...] (point/invariant direction is a comma-separated list of reals)";
	}
	
	@Override
	protected void runPass(String stringParams)
	{
		BaseComponent ha = (BaseComponent)config.root;
		initialize(ha, stringParams);
		
		// first make an incoming mode for incoming transitions
		// This mode has zero dynamics (x'=0) and outgoing guards based on the current state
		
		AutomatonMode initMode = makeInitMode();
		AutomatonMode modeToSplit = applyMode;
		
		// make modes for each step in the automaton
		AutomatonMode modes[] = new AutomatonMode[params.size()];
		
		for (int i = 0; i < params.size(); ++i)
			modes[i] = modeToSplit.copy(ha, params.get(i).modeName);
		
		for (int i = 0; i < params.size(); ++i)
		{
			PseudoInvariantParams pip = params.get(i);
			
			// apply the method of pseudo-invariants
			// copy the initial mode
			AutomatonMode secondMode = modes[i];
			
			// copy outgoing transitions (automatic)
			for (AutomatonTransition t : ha.transitions)
			{
				if (t.from == modeToSplit)
					t.copy(ha).from = secondMode;
			}
			
			// copy create incoming transition, adding the guard to determine which one to go to
			secondMode.invariant = pip.inv;
			
			// create the transition from the incoming mode
			AutomatonTransition atFromIncoming = ha.createTransition(initMode, secondMode);
			atFromIncoming.guard = createPiGuard(pip.inv, i);
			
			// create the transition to the next pseudo-invariant modes
			for (int nextI = i+1; nextI < params.size(); ++nextI)
			{
				AutomatonMode nextMode = modes[nextI];
				AutomatonTransition atToNext = ha.createTransition(secondMode, nextMode);
				
				atToNext.guard = negateEqualCondition(pip.inv);
				
				for (int otherModeI = i+1; otherModeI <= nextI; ++otherModeI)
				{
					PseudoInvariantParams otherModePip = params.get(otherModeI);
					
					// it should be in this set
					if (otherModeI == nextI)
						atToNext.guard = new Operation(Operator.AND, atToNext.guard, otherModePip.inv);
					else
						atToNext.guard = new Operation(Operator.AND, atToNext.guard, negateEqualCondition(otherModePip.inv));
				}
			}
			
			// create the last transition which goes to applymode
			AutomatonMode nextMode = applyMode;

			AutomatonTransition atToNext = ha.createTransition(secondMode, nextMode);
			atToNext.guard = negateEqualCondition(pip.inv);
			
			for (int otherModeI = i+1; otherModeI < params.size(); ++otherModeI)
			{
				PseudoInvariantParams otherModePip = params.get(otherModeI);
				
				// it should be in this set
				atToNext.guard = new Operation(Operator.AND, atToNext.guard, negateEqualCondition(otherModePip.inv));
			}
			
			Hyst.log("Created pi mode #" + i + " with invariant: " + pip.inv.toDefaultString());
		}
		
		// add the transition from initial mode to applyMode
		AutomatonTransition atFromIncoming = ha.createTransition(initMode, applyMode);
		atFromIncoming.guard = createPiGuard(null, params.size());
		
		// rename applyMode to applyMode_final
		ha.modes.remove(applyModeName);
		applyMode.name = checkNewModeName(applyMode.name + "_final");
		ha.modes.put(applyMode.name, applyMode);
	}

	/**
	 * Create a pseudoInvariant initial state guard. This consists of the negation of a set of previous mode's guards, as well
	 * as an extra guard for this mode
	 * @param g the extra guard (can be null)
	 * @param i the maximum index of the previous modes in params list to negate
	 * @return the guard expression
	 */
	private Expression createPiGuard(Expression g, int i)
	{
		Expression guard = g;
		
		for (int j = 0; j < i; ++j)
		{
			Expression cond = negateEqualCondition(params.get(j).inv);
			
			if (guard == null)
				guard = cond;
			else
				guard = new Operation(guard, Operator.AND, cond);
		}
		
		return guard;
	}

	/**
	 * Negate a simple operation, retaining equals, like 'x <= 5' -> 'x >= 5'
	 * @param exp
	 * @return
	 */
	private Expression negateEqualCondition(Expression exp)
	{
		Expression rv = null;
		
		if (exp instanceof Operation)
		{
			Operation o = (Operation)exp;
			Expression left = o.getLeft();
			Expression right = o.getRight();
			Operator op = o.op;
			
			Operator[][] opposites = new Operator[][]
			{
				{Operator.GREATEREQUAL, Operator.LESSEQUAL},
				{Operator.NOTEQUAL, Operator.EQUAL},
			};
			
			for (Operator[] opposite : opposites)
			{
				if (op.equals(opposite[0]))
				{
					rv = new Operation(left.copy(), opposite[1], right.copy());
					break;
				}
				else if (op.equals(opposite[1]))
				{
					rv = new Operation(left.copy(), opposite[0], right.copy());
					break;
				}
			}
			
			if (rv == null)
				throw new AutomatonExportException("Expression was not simple comparison in negateCondition: " + exp);
		}
		else
			throw new AutomatonExportException("negateCondititon called on non-operation: " + exp);
		
		
		return rv;
	}

	private String checkNewModeName(String name)
	{
		if (ha != null && ha.modes.get(name) != null)
			throw new AutomatonExportException("Mode with name '" + name + "' already exists in automaton.");
			
		return name;
	}

	private AutomatonMode makeInitMode()
	{
		AutomatonMode rv = ha.createMode(initModeName);
		
		// invariant
		rv.invariant = Constant.TRUE;
		
		// flow dynamics (urgent)
		rv.flowDynamics = null;
		rv.urgent = true;
		
		// redirect initial states to this mode
		if (config.init.containsKey(applyModeName))
		{
			Expression e = config.init.remove(applyModeName);
			config.init.put(initModeName, e);
		}
		
		// redirect incoming transitions to this mode
		for (AutomatonTransition t : ha.transitions)
		{
			if (t.to.equals(applyMode))
				t.to = rv; 
		}
		
		return rv;
	}

	private Expression makeExpressionFromLinearInequality(double[] coeff,
			double rhs, Operator op)
	{
		Operation sum = new Operation(new Constant(coeff[0]), Operator.MULTIPLY, new Variable(vars.get(0)));
		
		for (int d = 1; d < coeff.length; ++d)
		{
			Operation term = new Operation(new Constant(coeff[d]), Operator.MULTIPLY, new Variable(vars.get(d)));
			
			sum = new Operation(sum, Operator.ADD, term);
		}
		
		return new Operation(sum, op, new Constant(rhs));
	}

	private double dot(double[] a, double[] b)
	{
		double rv = 0;
		
		for (int i = 0; i < a.length; ++i)
			rv += a[i] * b[i];
		
		return rv;
	}
	
	private void initialize(BaseComponent ha, String allParams)
	{
		this.ha = ha;
		this.vars = ha.variables;
		numVars = ha.variables.size();
		
		String[] splitParams = allParams.split("\\|");
		int firstIndex = 0;
		
		if (splitParams.length == 0)
			throw new AutomatonExportException("pseudo-invariant params are blank!");
		
		if (ha.modes.get(splitParams[0]) != null)
		{
			firstIndex = 1;
			applyModeName = splitParams[0];
		}
		else
		{
			firstIndex = 0;
			applyModeName = config.init.keySet().iterator().next();
		}
			
		applyMode = ha.modes.get(applyModeName);
		initModeName = checkNewModeName(applyModeName + "_init");
		
		for (int i = firstIndex; i < splitParams.length; ++i)
		{
			String params = splitParams[i];
			
			double[] pt = new double[numVars];
			double[] gradient = new double[numVars];
			
			String[] sides = params.split(";");
			
			if (sides.length != 2)
				throw new AutomatonExportException("Parsing pass parameters, each |-separated entry should have two vectors separated by ';': "
						+ params);
			
			String[] parts1 = sides[0].split(",");
			String[] parts2 = sides[1].split(",");
			
			if (parts1.length != parts2.length || parts1.length != numVars)
				throw new AutomatonExportException("Parsing pass parameters, should have two vectors of length " + 
						numVars + ": " + params);
				
			try
			{
				for (int j = 0; j < numVars; ++j)
				{
					pt[j] = Double.parseDouble(parts1[j]);
					gradient[j] = Double.parseDouble(parts2[j]);
				}
			}
			catch (NumberFormatException e)
			{
				throw new AutomatonExportException("Error parsing pass parameter: " + params + "; " + e);
			}
			
			this.params.add(new PseudoInvariantParams(pt, gradient));
		}
	}
	
	public class PseudoInvariantParams
	{
		public String modeName;
		
		public Expression inv;
		
		public PseudoInvariantParams(double[] pt, double[] dir)
		{
			double rhs = dot(pt, dir);
			
			inv = makeExpressionFromLinearInequality(dir, rhs, Operator.GREATEREQUAL);
			
			Hyst.log("Making invariant from point " + arrayString(pt) + " and dir " + arrayString(dir) + ": " + 
					DefaultExpressionPrinter.instance.print(inv));
			
			if (inv == null)
				throw new AutomatonExportException("Invariant was null from dir: " + arrayString(dir));
			
			modeName = checkNewModeName(applyModeName + "_pi_" + (UNIQUE_ID++));
		}
	}
	
	private String arrayString(double[] array)
	{
		StringBuffer s = new StringBuffer("[");
		boolean first = true;
		
		for (double d : array)
		{
			if (first)
				first = false;
			else
				s.append(", ");
			
			s.append(d);
		}
		
		s.append("]");
		
		return s.toString();
	}

}
