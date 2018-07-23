package com.verivital.hyst.passes.basic;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.TransformationPass;

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

		if (ha.variables.contains(varName))
			throw new AutomatonExportException(
					"Affine Transformation variable '" + varName + "' already exists in automaton");

		ha.variables.add(varName);

		LinkedHashMap<String, Expression> newInit = new LinkedHashMap<String, Expression>();

		for (Entry<String, Expression> e : config.init.entrySet())
		{
			Operation eq = new Operation(Operator.EQUAL, new Variable(varName), 1);
			Operation o = new Operation(Operator.AND, e.getValue(), eq);
			newInit.put(e.getKey(), o);
		}

		for (AutomatonMode m : ha.modes.values())
		{

		}
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
