/**
 * 
 */
package com.verivital.hyst.grammar.formula;

import java.util.ArrayList;
import java.util.List;

import com.verivital.hyst.ir.AutomatonExportException;

/**
 * Combines expressions with an operator, like + or * or ==, or sin()
 */
public class Operation extends Expression
{
	public Operator op;
	public List<Expression> children = null;

	// shallow copy exps
	public Operation(Operator operator, Expression... exps)
	{
		this.op = operator;
		children = new ArrayList<Expression>();

		for (Expression e : exps)
		{
			if (e == null)
				throw new AutomatonExportException("subexpressions cannot be null");

			children.add(e);
		}
	}

	public Operation(Operation parent, Expression left, Operator operator, Expression right)
	{
		super(parent);
		children.add(left);
		setOperator(operator);
		children.add(right);
	}

	/**
	 * Binary operation, shallows copies exps
	 * 
	 * @param left
	 *            the right-hand-side expression
	 * @param operator
	 *            the operator of the binary operation
	 * @param right
	 *            the right-hand-side expression
	 */
	public Operation(Expression left, Operator operator, Expression right)
	{
		this.op = operator;
		children = new ArrayList<Expression>();
		children.add(left);
		children.add(right);

		if (left == null || right == null)
			throw new AutomatonExportException("subexpressions cannot be null");
	}

	// shallow copy list
	public Operation(Operator operator, List<Expression> children)
	{
		this.op = operator;
		this.children = children;

		for (Expression e : children)
		{
			if (e == null)
				throw new AutomatonExportException("subexpressions cannot be null");
		}
	}

	/**
	 * Create an operation of the form 'var <op> constant'
	 * 
	 * @param op
	 *            the operator
	 * @param var
	 *            the variable name
	 * @param c
	 *            the constant value
	 */
	public Operation(Operator op, String var, double c)
	{
		this(op, new Variable(var), new Constant(c));
	}

	/**
	 * Create an operation of the form 'var <op> constant'
	 * 
	 * @param op
	 *            the operator
	 * @param c
	 *            the constant value
	 * @param var
	 *            the variable name
	 */
	public Operation(Operator op, double c, String var)
	{
		this(op, new Constant(c), new Variable(var));
	}

	/**
	 * Create an operation of the form 'var -op- constant'
	 * 
	 * @param var
	 *            the variable name
	 * @param op
	 *            the operator
	 * @param c
	 *            the constant value
	 */
	public Operation(String var, Operator op, double c)
	{
		this(op, new Variable(var), new Constant(c));
	}

	/**
	 * Create an operation of the form 'var <op> constant'
	 * 
	 * @param c
	 *            the constant value
	 * @param op
	 *            the operator
	 * @param var
	 *            the variable name
	 */
	public Operation(double c, Operator op, String var)
	{
		this(op, new Constant(c), new Variable(var));
	}

	public Operation copy()
	{
		ArrayList<Expression> c = new ArrayList<Expression>();

		for (Expression e : children)
			c.add(e.copy());

		Operation rv = new Operation(op, c);

		return rv;
	}

	public String toStringInline()
	{
		String rv;

		if (children.size() == 0)
			rv = op.toString();
		else if (children.size() == 1)
		{
			Expression child = children.get(0);

			if (op.equals(Operator.NEGATIVE) || op.equals(Operator.LOGICAL_NOT))
			{
				if (child instanceof Operation)
					rv = op + "(" + child + ")";
				else
					rv = op.toString() + child;
			}
			else
				rv = op + "(" + child + ")";
		}
		else if (children.size() == 2)
		{
			Operation left = children.get(0).asOperation();
			Operation right = children.get(1).asOperation();

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

			if (right != null && left.children.size() > 1)
			{
				int rightP = Operator.getPriority(left.op);

				if (myP <= rightP
						&& !(myP == rightP && op == Operator.ADD || op == Operator.MULTIPLY)) // commutative
					needParenRight = true;
			}

			rv = "";

			if (needParenLeft)
				rv += "(" + left + ")";
			else
				rv += left;

			rv += " " + op + " ";

			if (needParenRight)
				rv += "(" + right + ")";
			else
				rv += right;
		}
		else
		{
			throw new AutomatonExportException(
					"No way defined to print expression with " + children.size() + " children.");
		}

		return rv;
	}

	public String toStringPrefix()
	{
		String childrenStr = "";

		for (Expression e : children)
			childrenStr += " " + e;

		return "(" + op + childrenStr + ")";
	}

	public Expression getLeft()
	{
		if (children.size() != 2)
			throw new AutomatonExportException(
					"getLeft() is only valid when there are two children: " + this);
		if (children.get(0) != null)
			children.get(0).setParent(this);
		return children.get(0);
	}

	public Expression getRight()
	{
		if (children.size() != 2)
			throw new AutomatonExportException(
					"getRight() is only valid when there are two children: " + this);
		if (children.get(1) != null)
			children.get(1).setParent(this);
		return children.get(1);
	}

	public Operator getOperator()
	{
		return op;
	}

	public void setOperator(Operator operator)
	{
		op = operator;
	}
}
