/**
 * 
 */
package com.verivital.hyst.grammar.formula;

/**
 * A variable references a Param of a Component. It should start with a character from a to z or an
 * underscore. Both upper- and lowercase characters can be used. Following characters can include
 * digits, too.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class Variable extends Expression
{
	public String name;

	public Variable(String name)
	{
		this.name = name;
	}

	@Override
	public Expression copy()
	{
		return new Variable(name);
	}
}
