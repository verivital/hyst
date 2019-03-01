package com.verivital.hyst.passes.basic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Component;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.network.ComponentInstance;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.DynamicsUtil;

/**
 * Adds an new constant affine variable which is equal to 1 always, and changes constant terms from
 * the differential equations to refer to the new variable
 *
 */
public class AffineTransformationPass extends TransformationPass
{
	@Option(name = "-var_name", aliases = {
			"-name" }, required = false, usage = "new affine variable name", metaVar = "NAME")
	private String varName = "affine";

	@Override
	protected void runPass()
	{
		BaseComponent ha = (BaseComponent) config.root;

		AutomatonMode anyMode = ha.modes.values().iterator().next();
		ArrayList<String> nonInputVars = DynamicsUtil.getNonInputVariables(anyMode, ha.variables);
		ArrayList<String> inputVars = new ArrayList<String>();

		for (String var : ha.variables)
		{
			if (!nonInputVars.contains(var))
				inputVars.add(var);
		}

		if (ha.variables.contains(varName))
			throw new AutomatonExportException(
					"Affine Transformation variable '" + varName + "' already exists in automaton");

		ha.variables.add(varName);

		LinkedHashMap<String, Expression> newInit = new LinkedHashMap<String, Expression>();

		for (Entry<String, Expression> e : config.init.entrySet())
		{
			Operation eq = new Operation(Operator.EQUAL, new Variable(varName), new Constant(1));
			Operation o = new Operation(Operator.AND, e.getValue(), eq);
			newInit.put(e.getKey(), o);
		}

		config.init = newInit;

		/////// convert flows ////////

		for (AutomatonMode m : ha.modes.values())
		{
			if (m.flowDynamics == null)
				continue;

			m.flowDynamics.put(varName, new ExpressionInterval("0"));

			// rename all dynamics with an affine term to refer to the new variable
			ArrayList<ArrayList<Double>> bMat = DynamicsUtil.extractDynamicsMatrixB(m);
			ArrayList<Double> cVec = DynamicsUtil.extractDynamicsVectorC(m);

			for (int index = 0; index < nonInputVars.size(); ++index)
			{
				double c = cVec.get(index);

				if (c != 0)
				{
					ArrayList<Double> aRow = DynamicsUtil.extractDynamicsMatrixARow(m, index);
					ArrayList<Double> bRow = bMat.get(index);

					// create new dynamics for this variable
					StringBuilder expStr = new StringBuilder("");

					for (int aIndex = 0; aIndex < nonInputVars.size(); ++aIndex)
					{
						double val = aRow.get(aIndex);

						if (val != 0)
						{
							if (expStr.length() > 0)
								expStr.append("+");

							expStr.append(val + "*" + nonInputVars.get(aIndex));
						}
					}

					// add affine term
					if (expStr.length() > 0)
						expStr.append("+");

					expStr.append(cVec.get(index) + "*" + varName);

					for (int bIndex = 0; bIndex < inputVars.size(); ++bIndex)
					{
						double val = bRow.get(bIndex);

						if (val != 0)
						{
							if (expStr.length() > 0)
								expStr.append("+");

							expStr.append(val + "*" + inputVars.get(bIndex));
						}
					}

					Expression exp = FormulaParser.parseValue(expStr.toString());
					String var = nonInputVars.get(index);
					m.flowDynamics.put(var, new ExpressionInterval(exp));
				}
			}
		}
	}

	/**
	 * Does the passed-in component have affine terms
	 * 
	 * @param c
	 *            a component to check
	 * @return are there affine terms used in c?
	 */
	public static boolean hasAffineTerms(Component c)
	{
		boolean rv = false;

		if (c instanceof NetworkComponent)
		{
			for (ComponentInstance ci : ((NetworkComponent) c).children.values())
			{
				if (hasAffineTerms(ci.child))
				{
					rv = true;
					break;
				}
			}
		}
		else
		{
			BaseComponent ha = (BaseComponent) c;

			for (AutomatonMode am : ha.modes.values())
			{
				ArrayList<Double> vals = DynamicsUtil.extractDynamicsVectorC(am);

				for (double v : vals)
				{
					if (v != 0)
					{
						rv = true;
						break;
					}
				}
			}
		}
		return rv;
	}

	@Override
	public String getCommandLineFlag()
	{
		return "pass_affine_transformation";
	}

	@Override
	public String getName()
	{
		return "Affine Transformation Pass";
	}
}
