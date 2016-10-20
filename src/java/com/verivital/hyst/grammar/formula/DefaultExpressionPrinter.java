package com.verivital.hyst.grammar.formula;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DefaultExpressionPrinter extends ExpressionPrinter
{
	public static DefaultExpressionPrinter instance = new DefaultExpressionPrinter();

	public DecimalFormat constFormatter;
	protected Map<Operator, String> opNames = new TreeMap<Operator, String>();

	public DefaultExpressionPrinter()
	{
		initializeData();
	}

	private void initializeData()
	{
		constFormatter = new DecimalFormat("", new DecimalFormatSymbols(Locale.ENGLISH));
		constFormatter.setGroupingUsed(false);
		constFormatter.setMinimumFractionDigits(1);
		constFormatter.setMinimumIntegerDigits(1);

		for (Operator o : Operator.values())
		{
			switch (o)
			{
			case ADD:
				opNames.put(Operator.ADD, "+");
				break;
			case AND:
				opNames.put(Operator.AND, "&");
				break;
			case COS:
				opNames.put(Operator.COS, "cos");
				break;
			case DIVIDE:
				opNames.put(Operator.DIVIDE, "/");
				break;
			case EQUAL:
				opNames.put(Operator.EQUAL, "=");
				break;
			case EXP:
				opNames.put(Operator.EXP, "exp");
				break;
			case GREATER:
				opNames.put(Operator.GREATER, ">");
				break;
			case GREATEREQUAL:
				opNames.put(Operator.GREATEREQUAL, ">=");
				break;
			case LESS:
				opNames.put(Operator.LESS, "<");
				break;
			case LESSEQUAL:
				opNames.put(Operator.LESSEQUAL, "<=");
				break;
			case LN:
				opNames.put(Operator.LN, "ln");
				break;
			case LOC:
				opNames.put(Operator.LOC, "loc");
				break;
			case MULTIPLY:
				opNames.put(Operator.MULTIPLY, "*");
				break;
			case NEGATIVE:
				opNames.put(Operator.NEGATIVE, "-");
				break;
			case NOTEQUAL:
				opNames.put(Operator.NOTEQUAL, "!=");
				break;
			case OR:
				opNames.put(Operator.OR, "|");
				break;
			case POW:
				opNames.put(Operator.POW, "^");
				break;
			case SIN:
				opNames.put(Operator.SIN, "sin");
				break;
			case SQRT:
				opNames.put(Operator.SQRT, "sqrt");
				break;
			case SUBTRACT:
				opNames.put(Operator.SUBTRACT, "-");
				break;
			case TAN:
				opNames.put(Operator.TAN, "tan");
				break;
			case LOGICAL_NOT:
				opNames.put(Operator.LOGICAL_NOT, "!");
				break;
			default:
				throw new RuntimeException("unsupported case: " + o.name());
			}
		}
	}

	@Override
	protected String printConstantValue(double d)
	{
		return constFormatter.format(d);
	}

	@Override
	public String printOperator(Operator op)
	{
		return opNames.get(op);
	}

	/**
	 * Usually inline printing
	 */
	@Override
	protected String printOperation(Operation o)
	{
		String rv;
		List<Expression> children = o.children;
		Operator op = o.op;

		if (children.size() == 0)
			rv = printOperator(o.op);
		else if (children.size() == 1)
		{
			Expression child = children.get(0);

			if (op.equals(Operator.NEGATIVE) || op.equals(Operator.LOGICAL_NOT))
			{
				if (child instanceof Operation && child.asOperation().children.size() > 1)
					rv = printOperator(o.op) + "(" + print(child) + ")";
				else
					rv = printOperator(o.op) + "" + print(child);
			}
			else
				rv = printOperator(o.op) + "(" + print(child) + ")";
		}
		else if (children.size() == 2)
		{
			Expression leftExp = children.get(0);
			Operation left = leftExp.asOperation();

			Expression rightExp = children.get(1);
			Operation right = rightExp.asOperation();

			boolean needParenLeft = false;
			boolean needParenRight = false;

			// use parentheses if they are needed
			int myP = Operator.getPriority(op);

			if (left != null && left.children.size() > 1)
			{
				int leftP = Operator.getPriority(left.op);

				if (leftP < myP)
					needParenLeft = true;
			}

			if (right != null && right.children.size() > 1)
			{
				int rightP = Operator.getPriority(right.op);

				if (myP > rightP || (myP == rightP && !Operator.isCommutative(op))) // commutative
					needParenRight = true;
			}

			rv = "";

			if (needParenLeft)
				rv += "(" + print(leftExp) + ")"; // maybe not strictly
													// necessary as the
													// expression.toString is
													// overriden to call this
													// print, but was having
													// problems with this
			else
				rv += print(leftExp);

			rv += " " + printOperator(o.op) + " ";

			if (needParenRight)
				rv += "(" + print(rightExp) + ")";
			else
				rv += print(rightExp);
		}
		else
		{
			rv = super.printOperation(o);
		}

		return rv;
	}
}