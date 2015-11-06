package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.geometry.HyperPoint;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.LutExpression;
import com.verivital.hyst.grammar.formula.MatrixExpression;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.passes.complex.ConvertLutFlowsPass;
import com.verivital.hyst.simulation.RungeKutta;
import com.verivital.hyst.util.AutomatonUtil;

public class LutMatrixTest
{
	@Before 
	public void setUpClass() 
	{      
	    Expression.expressionPrinter = null;
	}
	
	@Test
	/**
	 * Test matrix size detection
	 */
	public void testMatrixSize()
	{
		String str = "[1, 2]";
		MatrixExpression m = (MatrixExpression)FormulaParser.parseValue(str);
		
		Assert.assertEquals("small matrix num dims is 1", 1, m.getNumDims());
		
		str = "[1, 2, 3, 11, 12, 13, 101, 102, 103, 111, 112, 113]";
		m = (MatrixExpression)FormulaParser.parseValue(str);
		
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
			MatrixExpression m = (MatrixExpression)FormulaParser.parseValue(str);
			
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
		MatrixExpression m2 = (MatrixExpression)FormulaParser.parseValue(str2);
		
		String str3 = "[1 2 ; 10 20 ; 100 200]";
		MatrixExpression m3 = (MatrixExpression)FormulaParser.parseValue(str3);
		
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
	
	/**
	 * Creates a new configuration with an initial urgent mode
	 * @return the constructed Configuration
	 */
	private Configuration makeLutConfiguration(String[][] dynamics)
	{
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		
		try
		{
			new ConvertLutFlowsPass().runTransformationPass(c, null);
			Assert.fail("Luts in initial modes should be rejected");
		}
		catch (AutomatonExportException e)
		{
			// expected, conversions of luts in initial modes are not allowed
		}
		
		// create a new urgent initial mode, and change the initial states to point to it
		BaseComponent ha = (BaseComponent)c.root;
		AutomatonMode am = ha.modes.values().iterator().next();
		AutomatonMode start = ha.createMode("start");
		start.urgent = true;
		start.flowDynamics = null;
		start.invariant = Constant.TRUE;
		
		AutomatonTransition at = ha.createTransition(start, am);
		at.guard = Constant.TRUE;
		
		Expression initExpression = c.init.values().iterator().next();
		c.init.clear();
		c.init.put("start", initExpression);
		
		c.validate();
		
		return c;
	}
	
	/**
	 * Test a 2-d LUT in flow
	 */
	@Test
	public void testParseBigLut2d()
	{
		String data = "reshape([0.8,0.6,0.4,0.3,0.2,0.4,0.3,0.2,0.2,0.2,0.3,0.25,0.2,0.2,0.2,0.25,0.2,0.2,0.2,0.2],5,4)";
		String breakPoints1 = "[800,1000,1500,2000,3000]";
		String breakPoints2 = "[0.05,0.15,0.2,0.25]";
		String lutExpStr = "lut([x,y], " + data + ", " + breakPoints1 + ", " + breakPoints2 + ")";
		Expression e = FormulaParser.parseFlow("y' = " + lutExpStr);
		
		if (!e.asOperation().getRight().getClass().equals(LutExpression.class))
		{
			System.out.println("LUT flow not parsed correctly: " + e);
			Assert.fail("LUT flow not parsed correctly");
		}
	}
	
	@Test
	public void testLutPrinting()
	{
		String lutStr = "lut([t], [1, 2, 1, 2], [0.0, 10.0, 30.0, 40.0])";
		
		Expression e = FormulaParser.parseValue(lutStr);
		
		Assert.assertEquals(lutStr, e.toDefaultString());
	}
	
	/**
	 * Test a 1-d LUT in flow
	 */
	@Test
	public void testLut1d()
	{
		String lutStr = "lut([t], [1, 2, 1, 2], [0, 10, 30, 40])";
		String[][] dynamics = {{"t", "1", "0"}, {"y", lutStr, "15"}};
		Configuration c = makeLutConfiguration(dynamics);
		BaseComponent ha = (BaseComponent)c.root;
		
		new ConvertLutFlowsPass().runTransformationPass(c, null);
		
		// lut([t], [1, 2, 1, 2], [0, 10, 30, 40])
		// there should be 3 modes + init		
		Assert.assertEquals("4 modes after conversion", 4, ha.modes.size());
		
		// transition from start should point to each of the three (3 transitions)
		// and there should be bi-directional transitions between each of them (2 * 3 = 6 transitions)
		Assert.assertEquals("9 transitions after conversion", 9, ha.transitions.size());
		
		// 15 -> 1.75
		String[] names = {"on_0", "on_1", "on_2"};
		String[] invariants = {"t <= 10", "t >= 10 & t <= 30", "t >= 30"};
		String[] flows = {"1 + 1 / 10 * (t - 0)", "2 + -1 / -20 * (t - 10)", "1 + 1 / 10 * (t - 30)"};
		
		for (int i = 0; i < names.length; ++i)
		{
			String name = names[i];
			String invariant = invariants[i];
			String flow = flows[i];
			
			AutomatonMode am = ha.modes.get(name);
			Assert.assertNotEquals("mode " + name + " is not supposed to be null", null, am);
			
			Assert.assertEquals("invariant in " + name + " is incorrect", invariant, am.invariant.toDefaultString());
			Assert.assertEquals("flow in " + name + " is incorrect", flow, am.flowDynamics.get("y").asExpression().toDefaultString());
		}
		
		// test the guard from mode 0 to mode 1 (should be t >= 10)
		AutomatonTransition at = ha.findTransition(names[0], names[1]);
		Assert.assertEquals("guard for transition from mode 0 to mode 1 is incorrect", "t >= 10", at.guard.toDefaultString());
		
		// test the guard from mode 2 to mode 1 (should be t <= 30)
		at = ha.findTransition(names[2], names[1]);
		Assert.assertEquals("guard for transition from mode 2 to mode 1 is incorrect", "t <= 30", at.guard.toDefaultString());
	}
	
	/**
	 * Test a 2-d LUT in flow
	 */
	@Test
	public void testLut2d()
	{
		String lutStr = "lut([a, b], [1 2 4 ; 2 3 5 ; 3 5 10], [0, 1, 3], [0, 10, 30])";
		String[][] dynamics = {{"a", "1", "0"}, {"b", "1", "0"}, {"y", lutStr, "15"}};
		Configuration c = makeLutConfiguration(dynamics);
		BaseComponent ha = (BaseComponent)c.root;
		
		// should have four modes created, with the split at (a,b)=(1,10)
		// ok, so dynamics in mode '1_1' (lower right) are based on the quadrant:
		// 3 5
		// 5 10
		// where a ranges from 1 to 3
		// and b ranges from 10 to 30
		// 1. interpolate a at the top of the range: atop = 3+(a-1)*1          (1 comes from (5-3)/(3-1))
		// 2. interpolate a at the bottom of the range: abottom = 5+(a-1)*2.5    (2.5 comes from (10-5)/(3-1))
		// 3. use b to interpolate between the a's: output = atop + (b-10)/20 * (abottom - atop)     (20 comes from 30-10)
		// = 3+(a-1)*1 + (b-10)/20 * (5+(a-1)*2.5 - (3+(a-1)*1))
		
		new ConvertLutFlowsPass().runTransformationPass(c, null);
		
		// there should be 4 modes + init		
		Assert.assertEquals("5 modes after conversion", 5, ha.modes.size());
		
		// transition from start should point to each of the four (4 transitions)
		// and there should be bi-directional transitions between each of them (2 * 4 = 8 transitions)
		Assert.assertEquals("12 transitions after conversion", 12, ha.transitions.size());
		
		// check the invariant at 1_1
		String name = "on_1_1";
		String invariant = "a >= 1 & b >= 10";
		
		AutomatonMode am = ha.modes.get(name);
		Assert.assertNotEquals("mode " + name + " is not supposed to be null", null, am);		
		Assert.assertEquals("invariant in " + name + " is incorrect", invariant, am.invariant.toDefaultString());
		
		// check the flow dynamics for a 'random' point
		Expression expected = FormulaParser.parseValue("3+(a-1)*1 + (b-10)/20 * (5+(a-1)*2.5 - (3+(a-1)*1))");
		Expression got = am.flowDynamics.get("y").asExpression();
		ArrayList <String> vars = new ArrayList <String>();
		vars.add("a");
		vars.add("b");
		
		// the test points
		double[][] tests = new double[][]
		{
			{1, 10},
			{3, 10},
			{1, 30},
			{3, 30},
			{2, 20},
			{32.15, 15.351},
			{-3.15, -1.351},
			{0, 0},
			{Math.PI * 3.44 + 12, Math.E * 7.1 + 834}
		};
		
		for (double[] test : tests)
		{
			double a = test[0];
			double b = test[1];
			HyperPoint p = new HyperPoint(a, b);
			
			double expectedVal = RungeKutta.evaluateExpression(expected, p, vars);
			double gotVal = RungeKutta.evaluateExpression(got, p, vars);
			double TOL = 1e-9;
			
			Assert.assertEquals("flow in " + name + " is incorrect at point (" + a + ", " + b + ")", expectedVal, gotVal, TOL);
		}
	}
	
	@Test
	public void testLinearInterpolation1d()
	{
		String lutStr = "lut([t], [1, 2, 1, 2], [0, 10, 30, 40])";
		LutExpression lut = (LutExpression)FormulaParser.parseValue(lutStr);
		String[] vars = new String[]{"t"};
		int[] indexList = new int[]{0};
		Interval[] rangeList = new Interval[]{new Interval(0,10)};
		
		Expression expected = FormulaParser.parseValue("1 + 1 / 10 * (t - 0)");
		Expression got = ConvertLutFlowsPass.nLinearInterpolation(vars, lut.table, indexList, rangeList);

		List <String> varList = Arrays.asList(vars);
		
		// the values to test
		double[] tests = {0, 1, 10, 2, 5, 1.5, Math.PI * Math.E + 1.3515};
		
		for (double t : tests)
		{
			HyperPoint p = new HyperPoint(t);
			
			double expectedVal = RungeKutta.evaluateExpression(expected, p, varList);
			double gotVal = RungeKutta.evaluateExpression(got, p, varList);
			double TOL = 1e-9;
			
			Assert.assertEquals("1-d lut interpolation was wrong for t = " + t, expectedVal, gotVal, TOL);
		}
	}
	
	@Test
	public void testLinearInterpolation2d()
	{
		String lutStr = "lut([a, b], [1 2 4 ; 2 3 5 ; 3 5 10], [0, 1, 3], [0, 10, 30])";
		LutExpression lut = (LutExpression)FormulaParser.parseValue(lutStr);
		String[] vars = new String[]{"a", "b"};
		int[] indexList = new int[]{1,1};
		Interval[] rangeList = new Interval[]{new Interval(1,3), new Interval(10, 30)};
		
		Expression expected = FormulaParser.parseValue("3+(a-1)*1 + (b-10)/20 * (5+(a-1)*2.5 - (3+(a-1)*1))");
		Expression got = ConvertLutFlowsPass.nLinearInterpolation(vars, lut.table, indexList, rangeList);
		
		System.out.println("got: " + got.toDefaultString());

		List <String> varList = Arrays.asList(vars);
		
		// the test points
		double[][] tests = new double[][]
		{
			{1, 10},
			{3, 10},
			{1, 30},
			{3, 30},
			{2, 20},
			{32.15, 15.351},
			{-3.15, -1.351},
			{0, 0},
			{Math.PI * 3.44 + 12, Math.E * 7.1 + 834}
		};
		
		for (double[] test : tests)
		{
			double a = test[0];
			double b = test[1];
			HyperPoint p = new HyperPoint(a, b);
			
			double expectedVal = RungeKutta.evaluateExpression(expected, p, varList);
			double gotVal = RungeKutta.evaluateExpression(got, p, varList);
			double TOL = 1e-9;
			
			Assert.assertEquals("2-d lut interpolation was wrong for (a,b) = (" + a + "," + b + ")", expectedVal, gotVal, TOL);
		}
	}
}
