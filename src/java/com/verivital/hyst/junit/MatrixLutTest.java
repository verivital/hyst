package com.verivital.hyst.junit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Lut;
import com.verivital.hyst.grammar.formula.MatrixExpression;

public class MatrixLutTest
{
	@Before 
	public void setUpClass() 
	{      
	    Expression.expressionPrinter = null;
	}
	
	/**
	 * Test a 1-d LUT in flow
	 */
	@Test
	public void testLut1d()
	{
		Expression e = FormulaParser.parseFlow("y' = lut([t], [0,0,1,1,0], [1,2,3,4,8])");
		
		if (!e.asOperation().getRight().getClass().equals(Lut.class))
		{
			System.out.println("LUT flow not parsed correctly: " + e);
			Assert.fail("LUT flow not parsed correctly");
		}
	}
	
	/**
	 * Test a 2-d LUT in flow
	 */
	@Test
	public void testLut2d()
	{
		String data = "reshape([0.8,0.6,0.4,0.3,0.2,0.4,0.3,0.2,0.2,0.2,0.3,0.25,0.2,0.2,0.2,0.25,0.2,0.2,0.2,0.2],5,4)";
		String breakPoints1 = "[800,1000,1500,2000,3000]";
		String breakPoints2 = "[0.05,0.15,0.2,0.25]";
		String lutExpStr = "lut([x,y], " + data + ", " + breakPoints1 + ", " + breakPoints2 + ")";
		Expression e = FormulaParser.parseFlow("y' = " + lutExpStr);
		
		if (!e.asOperation().getRight().getClass().equals(Lut.class))
		{
			System.out.println("LUT flow not parsed correctly: " + e);
			Assert.fail("LUT flow not parsed correctly");
		}
	}
	
	@Test
	/**
	 * Test matrix size detection
	 */
	public void testMatrixSize()
	{
		String str = "[1, 2]";
		MatrixExpression m = (MatrixExpression)FormulaParser.parseNumber(str);
		
		Assert.assertEquals("small matrix num dims is 1", 1, m.getNumDims());
		
		str = "[1, 2, 3, 11, 12, 13, 101, 102, 103, 111, 112, 113]";
		m = (MatrixExpression)FormulaParser.parseNumber(str);
		
		Assert.assertEquals("larger matrix num dims is 1", 1, m.getNumDims());
	}
	
	
	/**
	 * Test printing 1-d, 2-d and 3-d matrices
	 */
	@Test
	public void testPrintMatrix()
	{
		String[] strs = 
		{	
			"[1, 2, -5, 10]",
			"[1, 2 ; 10, 20 ; 100, 200]",
			"reshape([1, 2, 3, 11, 12, 13, 101, 102, 103, 111, 112, 113], 3, 2, 2)"
		};
	
		for (String str : strs)
		{
			MatrixExpression m = (MatrixExpression)FormulaParser.parseNumber(str);
			
			Assert.assertEquals("MatrixExpression.toString() was incorrect", str, m.toDefaultString());
		}
	}
	
	@Test
	public void testMatrixExplicitParsing()
	{
		Expression[][] expArray = {{new Constant(1), new Constant(2)},
				{new Constant(10), new Constant(20)},
				{new Constant(100), new Constant(200)}};
		MatrixExpression m = new MatrixExpression(expArray);
		
		String expectedReshape = "reshape([1, 10, 100, 2, 20, 200], 3, 2)";
		String expected2d = "[1, 2 ; 10, 20 ; 100, 200]";
		
		StringBuilder rv = new StringBuilder();
		m.makeStringReshape(rv, DefaultExpressionPrinter.instance);
		
		Assert.assertEquals("Matrix internally created incorrectly", expectedReshape, rv.toString());
		
		Assert.assertEquals("2-d matrix prints incorrectly", expected2d, m.toDefaultString());
	}
	
	/**
	 * Test matrix expressions (general n-dimensional arrays) 
	 */
	@Test
	public void testMatrixExpression2d()
	{
		double TOL = 1e-9;
		double[][] dblArray = {{1, 2}, {10, 20}, {100, 200} };
		// 1 2 ; 10 20 ; 100 200
		
		Expression[][] expArray = {{new Constant(1), new Constant(2)},
				{new Constant(10), new Constant(20)},
				{new Constant(100), new Constant(200)}};
		MatrixExpression m1 = new MatrixExpression(expArray);
		
		// the internal representation for 
		String str2 = "reshape([1, 10, 100, 2, 20, 200],3,2)";
		MatrixExpression m2 = (MatrixExpression)FormulaParser.parseNumber(str2);
		
		String str3 = "[1 2 ; 10 20 ; 100 200]";
		MatrixExpression m3 = (MatrixExpression)FormulaParser.parseNumber(str3);
		
		Expression[] expArray1d = {new Constant(1), new Constant(10), new Constant(100), 
				new Constant(2), new Constant(20), new Constant(200)};
		MatrixExpression m4 = new MatrixExpression(expArray1d, new int[]{3, 2});

		for (int y = 0; y < 3; ++y)
		{
			for (int x = 0; x < 2; ++x)
			{
				double val = dblArray[y][x]; // array indices and matrix indices are reversed
				
				Assert.assertEquals("value in m1.get(" + x + ", " + y + ") was wrong", val, ((Constant)m1.get(x,y)).getVal(), TOL);
				Assert.assertEquals("value in m2.get(" + x + ", " + y + ") was wrong", val, ((Constant)m2.get(x,y)).getVal(), TOL);
				Assert.assertEquals("value in m3.get(" + x + ", " + y + ") was wrong", val, ((Constant)m3.get(x,y)).getVal(), TOL);
				Assert.assertEquals("value in m4.get(" + x + ", " + y + ") was wrong", val, ((Constant)m4.get(x,y)).getVal(), TOL);
			}
		}
	}
}
