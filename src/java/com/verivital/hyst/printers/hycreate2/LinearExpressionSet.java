package com.verivital.hyst.printers.hycreate2;

import java.util.LinkedList;
import java.util.List;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;

/**
 * A conjunction of linear expressions
 * 
 * This assumes the top level is either a linear expression or a conjunction
 * 
 * @author Stanley Bak
 *
 */
public class LinearExpressionSet
{
	public LinkedList<LinearExpression> expressions = new LinkedList<LinearExpression>();

	public LinearExpressionSet(Expression e, List<String> variables)
	{
		if (e != null)
			accumulateExpressions(e, e, variables);
	}

	private void accumulateExpressions(Expression original, Expression e, List<String> variables)
	{
		if (e instanceof Operation)
		{
			Operation o = (Operation) e;

			if (o.op == Operator.AND)
			{
				accumulateExpressions(original, o.getLeft(), variables);
				accumulateExpressions(original, o.getRight(), variables);
			}
			else
			{
				try
				{
					expressions.add(new LinearExpression(o, variables));
				}
				catch (AutomatonExportException aee)
				{
					String text = aee.getMessage() + "; original whole expression: "
							+ original.toDefaultString();
					throw new AutomatonExportException(text);
				}
			}
		}
		else
			throw new AutomatonExportException(
					"Could not parse linear expression set from: " + original.toDefaultString()
							+ " ('" + e.toDefaultString() + "' is not an operation)");
	}

	@Override
	public String toString()
	{
		String rv = "";

		for (LinearExpression le : expressions)
		{
			if (rv.length() > 0)
				rv += " and ";

			rv += le;
		}

		return rv;
	}
}
