package com.verivital.hyst.util;

import java.util.Map;
import java.util.Map.Entry;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.LutExpression;
import com.verivital.hyst.grammar.formula.MatrixExpression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;

public class ValueSubstituter
{
	private Map<String, Expression> valMap;

	public ValueSubstituter(Map<String, Expression> substitutionMap)
	{
		this.valMap = substitutionMap;
	}

	/**
	 * Do the Substitution return the new expression
	 * 
	 * @param e
	 *            the expression to substitute into
	 * @return the modified expression
	 */
	public Expression substitute(Expression e)
	{
		Expression rv = null;

		if (e != null)
			rv = substituteCopy(e.copy());

		return rv;
	}

	private Expression substituteCopy(Expression e)
	{
		Expression rv = e;

		if (e instanceof Variable)
		{
			Variable v = (Variable) e;
			Expression subIn = valMap.get(v.name);

			if (subIn != null)
				rv = subIn.copy();
		}
		else if (e instanceof Operation)
		{
			Operation o = (Operation) e;

			for (int i = 0; i < o.children.size(); ++i)
			{
				Expression child = o.children.get(i);

				Expression newChild = substitute(child);

				if (newChild instanceof Constant && o.op == Operator.NEGATIVE)
				{
					rv = new Constant(-((Constant) newChild).getVal());
					break;
				}

				o.children.set(i, newChild);
			}
		}
		else if (e instanceof LutExpression)
		{
			LutExpression lut = (LutExpression) e;

			for (int i = 0; i < lut.inputs.length; ++i)
				lut.inputs[i] = substitute(lut.inputs[i]);

			substitute(lut.table);
		}
		else if (e instanceof MatrixExpression)
		{
			MatrixExpression m = (MatrixExpression) e;

			for (Entry<int[], Expression> entry : m)
			{
				Expression simpler = substitute(entry.getValue());

				m.setExpressionAtIndex(entry.getKey(), simpler);
			}
		}

		return rv;
	}
};
