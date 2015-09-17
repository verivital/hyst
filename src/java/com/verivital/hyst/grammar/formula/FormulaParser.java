package com.verivital.hyst.grammar.formula;


import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import com.verivital.hyst.grammar.antlr.HystExpressionLexer;
import com.verivital.hyst.grammar.antlr.HystExpressionParser;
import com.verivital.hyst.ir.AutomatonExportException;


public class FormulaParser
{
	public static Expression getExpression(String text, String part)
	{
		Expression rv = null;
		HystExpressionParser par = null;
		
		try
		{
			HystExpressionLexer lex = new HystExpressionLexer(new ANTLRInputStream(text));
			
			par = new HystExpressionParser(new CommonTokenStream(lex));
			par.setErrorHandler(new BailErrorStrategy());
			
			ParseTree tree = getParseTree(par, part);
			
			rv = new ExpVisitor().visit(tree);
			
			// dotted variables are only allowed in loc expressions
			if (!part.equals("initial/forbidden"))
				checkNoDottedVariables(rv);
		}
		catch (AutomatonExportException e)
		{
			throw new AutomatonExportException("Could not parse " + part + ": '" + text + "'", e);
		}
		catch (ParseCancellationException e)
		{
			if (par != null)
			{
				// reparse with error reporting
				par.setErrorHandler(new DefaultErrorStrategy());
			
				try
				{
					getParseTree(par, part);
				}
				catch (ParseCancellationException e2) {}
			}
			
			throw new AutomatonExportException("Could not parse " + part + ": '" + text + "'", e);
		}
		
		return rv;
	}
	
	private static void checkNoDottedVariables(Expression e)
	{
		if (e != null)
		{
			Operation o = e.asOperation();
			
			if (o != null)
			{
				for (int i = 0; i < o.children.size(); ++i)
					checkNoDottedVariables(o.children.get(i));
			}
			else if (e instanceof Variable)
			{
				Variable v = (Variable)e;
				
				if (v.name.contains("."))
					throw new AutomatonExportException("Variable is not allowed to contains dots: " + v.name);
			}
		}
	}

	private static ParseTree getParseTree(HystExpressionParser par, String part)
	{
		ParseTree rv = null;
		
		if (part.equals("invariant"))
			rv = par.invariantExpression();
		else if (part.equals("reset"))
			rv = par.resetExpression();
		else if (part.equals("guard"))
			rv = par.guardExpression();
		else if (part.equals("flow"))
			rv = par.flowExpression();
		else if (part.equals("initial/forbidden"))
			rv = par.locExpression();
		else if (part.equals("number (addsub)"))
			rv = par.addSub();
		else
			throw new AutomatonExportException("Unknown expression type: " + part);
		
		return rv;	
	}
	
	/**
	 * Parse a number like 2 * x - 5
	 * @param text
	 * @return
	 */
	public static Expression parseNumber(String text)
	{
		Expression rv = null;
		
		try
		{
			rv = getExpression(text, "number (addsub)");
			
			checkNoLut(rv);
		}
		catch (AutomatonExportException e)
		{
			String msg = e.getMessage();
			
			throw new AutomatonExportException("Parser Error; " + msg + "; sample expected syntax: x' := x + y & y' := 0", e);
		}
		
		return rv;
	}

	public static Expression parseInvariant(String text)
	{
		Expression rv = null;
		
		try
		{
			rv = getExpression(text, "invariant");
			checkNoLut(rv);
		}
		catch (AutomatonExportException e)
		{
			String msg = e.getMessage();
			
			throw new AutomatonExportException("Parser Error; " + msg + "; sample expected syntax: x >= 0 & x <= 1 | y >= x & y <= x + 1", e);
		}
		
		return rv;
	}
	
	public static Expression parseReset(String text)
	{
		Expression rv = null;
		
		try
		{
			rv = getExpression(text, "reset");
			checkNoLut(rv);
		}
		catch (AutomatonExportException e)
		{
			String msg = e.getMessage();
			
			throw new AutomatonExportException("Parser Error; " + msg + "; sample expected syntax: x' := x + y & y' := 0", e);
		}
		
		return rv;
	}
	
	public static Expression parseGuard(String text)
	{
		Expression rv = null;
		
		try
		{
			rv = getExpression(text, "guard");
			checkNoLut(rv);
		}
		catch (AutomatonExportException e)
		{
			String msg = e.getMessage();
			
			throw new AutomatonExportException("Parser Error; " + msg + "; sample expected syntax: x >= 0 & x <= 1 | y >= x & y <= x + 1", e);
		}
		
		return rv;
	}
	
	public static Expression parseFlow(String text)
	{
		Expression rv = null;
		
		try
		{
			rv = getExpression(text, "flow");
		}
		catch (AutomatonExportException e)
		{
			String msg = e.getMessage();
			
			throw new AutomatonExportException("Parser Error; " + msg + "; sample expected syntax: x' == 2 * x + y & y' == y", e);
		}
		
		return rv;
	}
	
	/**
	 * Parses locations with expressions over state variables
	 * 
	 * Used to Parse: initially and forbidden expressions from SpaceEx XML config file
	 * 
	 * TODO: TJ: change name to parseInitial or parseForbidden? or maybe parseState? this name is a little confusing and I had to look through SpaceExXMLReader.java to find it over here
	 * 
	 * @param text
	 * @return
	 */
	public static Expression parseLoc(String text)
	{
		Expression rv = null;
		
		try
		{
			rv = getExpression(text, "initial/forbidden");
			checkNoLut(rv);
		}
		catch (AutomatonExportException e)
		{
			String msg = e.getMessage();
			
			throw new AutomatonExportException("Parser Error; " + msg + "; sample syntax: loc(automaton) == start & x == 5 & y >= 0 & y <= x", e);
		}
		
		return rv;
	}

	/**
	 * Check to make sure the expression below does not contain a look up table (which are only allowed in flows)
	 * @param e the expression to check
	 */
	private static void checkNoLut(Expression e)
	{
		
	}
}
