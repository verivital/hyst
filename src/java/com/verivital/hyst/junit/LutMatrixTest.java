package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.LutExpression;
import com.verivital.hyst.grammar.formula.MatrixExpression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.complex.ConvertLutFlowsPass;
import com.verivital.hyst.printers.PySimPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.util.AutomatonUtil;

@RunWith(Parameterized.class)
public class LutMatrixTest
{
	@Before
	public void setUpClass()
	{
		ConvertLutFlowsPass.MAX_CONVERSIONS = 5;
		Expression.expressionPrinter = null;
		ConvertLutFlowsPass.simplifyMode = ConvertLutFlowsPass.SIMPLIFY_NONE;
	}

	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public LutMatrixTest(boolean block)
	{
		PythonBridge.setBlockPython(block);
	}

	@Test
	/**
	 * Test matrix size detection
	 */
	public void testMatrixSize()
	{
		String str = "[1, 2]";
		MatrixExpression m = (MatrixExpression) FormulaParser.parseValue(str);

		Assert.assertEquals("small matrix num dims is 1", 1, m.getNumDims());

		str = "[1, 2, 3, 11, 12, 13, 101, 102, 103, 111, 112, 113]";
		m = (MatrixExpression) FormulaParser.parseValue(str);

		Assert.assertEquals("larger matrix num dims is 1", 1, m.getNumDims());
	}

	@Test
	public void testMatrixEnumerateValues()
	{
		String str = "[1, 2; 3, 4; 5, 6]";
		MatrixExpression m = (MatrixExpression) FormulaParser.parseValue(str);
		double total = 0;

		for (Entry<int[], Expression> e : m)
			total += ((Constant) e.getValue()).getVal();

		double TOL = 1e-9;
		Assert.assertEquals("matrix total is correct", 21, total, TOL);
	}

	@Test
	/**
	 * Test a matrix expression with negatives that uses spaces as a delimiter
	 */
	public void testNegativeMatrix()
	{
		// this is not supported, as it makes the underlying grammar whitespace
		// sensitive
		// matlab also permits expressions like "+50" rather than 50

		/*
		 * String[] strings = {"[1 1]", "[1-1]", "[1 - 1]", "[1 -1]", "[-1 -1]", "[-1 -(1 -1)]" };
		 * int widths[] = {2, 1, 1, 2, 2, 2 };
		 * 
		 * for (int i = 0; i < strings.length; ++i) { String s = strings[i]; int expected =
		 * widths[i];
		 * 
		 * MatrixExpression m = (MatrixExpression)FormulaParser.parseValue(s);
		 * 
		 * Assert.assertEquals("matrix width is incorrect for " + s, expected, m.getDimWidth(0)); }
		 */
	}

	@Test
	/**
	 * Test a reshape expression
	 */
	public void testReshape()
	{
		// this uses
		String str = "reshape([-2.0000, -1.5000, -1.0000, -0.5000, 0, -1.5000, -1.0000, -0.5000, 0, 0.5000, "
				+ "-1.0000, -0.5000, 0, 0.5000, 1.0000, -0.5000, 0, 0.5000, 1.0000, 1.5000],10,2)";

		MatrixExpression m = (MatrixExpression) FormulaParser.parseValue(str);

		Assert.assertEquals(10, m.getDimWidth(0));
		Assert.assertEquals(2, m.getDimWidth(1));
	}

	/**
	 * Test printing 1-d, 2-d and 3-d matrices
	 */
	@Test
	public void testPrintMatrix()
	{
		String[] strs = { "[1.0, 2.0, -5.0, 10.0]", "[1.0, 2.0 ; 10.0, 20.0 ; 100.0, 200.0]",
				"reshape([1.0, 2.0, 3.0, 11.0, 12.0, 13.0, 101.0, 102.0, 103.0, 111.0, 112.0, 113.0], 3, 2, 2)" };

		for (String str : strs)
		{
			MatrixExpression m = (MatrixExpression) FormulaParser.parseValue(str);

			Assert.assertEquals("MatrixExpression.toString() was incorrect", str,
					m.toDefaultString());
		}
	}

	@Test
	public void testMatrixExplicitParsing()
	{
		Expression[][] expArray = { { new Constant(1), new Constant(2) },
				{ new Constant(10), new Constant(20) }, { new Constant(100), new Constant(200) } };
		MatrixExpression m = new MatrixExpression(expArray);

		String expectedReshape = "reshape([1.0, 10.0, 100.0, 2.0, 20.0, 200.0], 3, 2)";
		String expected2d = "[1.0, 2.0 ; 10.0, 20.0 ; 100.0, 200.0]";

		StringBuilder rv = new StringBuilder();
		m.makeStringReshape(rv, DefaultExpressionPrinter.instance);

		Assert.assertEquals("Matrix internally created incorrectly", expectedReshape,
				rv.toString());

		Assert.assertEquals("2-d matrix prints incorrectly", expected2d, m.toDefaultString());
	}

	@Test
	public void testMatrixDereferenceOrder()
	{
		String str2d = "[1, 2 ; 10, 20 ; 100, 200]";
		MatrixExpression m2d = (MatrixExpression) FormulaParser.parseValue(str2d);

		// make sure the internal representation matches what matlab uses (from
		// reshape)
		String correct = "reshape([1.0, 10.0, 100.0, 2.0, 20.0, 200.0], 3, 2)";
		StringBuilder sb = new StringBuilder();
		m2d.makeStringReshape(sb, DefaultExpressionPrinter.instance);
		Assert.assertEquals("matrix internals should be same as matlab's reshape", correct,
				sb.toString());

		Assert.assertEquals("2d matrix size doesn't match matlab", 3, m2d.getDimWidth(0));
		Assert.assertEquals("2d matrix size doesn't match matlab", 2, m2d.getDimWidth(1));

		Assert.assertEquals("matrix dereference order doesn't match matlab in 2d matrix", "100.0",
				m2d.get(2, 0).toDefaultString());

		// try 3d matrix
		String str3d = "reshape([1, 2, 3, 11, 12, 13, 101, 102, 103, 111, 112, 113], 3, 2, 2)";
		MatrixExpression m3d = (MatrixExpression) FormulaParser.parseValue(str3d);
		Assert.assertEquals("3d matrix size doesn't match matlab", 3, m3d.getDimWidth(0));
		Assert.assertEquals("3d matrix size doesn't match matlab", 2, m3d.getDimWidth(1));
		Assert.assertEquals("3d matrix size doesn't match matlab", 2, m3d.getDimWidth(2));

		Assert.assertEquals("matrix dereference order doesn't match matlab in 3d matrix", "113.0",
				m3d.get(2, 1, 1).toDefaultString());
		Assert.assertEquals("matrix dereference order doesn't match matlab in 3d matrix", "13.0",
				m3d.get(2, 1, 0).toDefaultString());
	}

	/**
	 * Test matrix expressions (general n-dimensional arrays)
	 */
	@Test
	public void testMatrixExpression2d()
	{
		double TOL = 1e-9;
		double[][] dblArray = { { 1, 2 }, { 10, 20 }, { 100, 200 } };
		// 1 2 ; 10 20 ; 100 200

		Expression[][] expArray = { { new Constant(1), new Constant(2) },
				{ new Constant(10), new Constant(20) }, { new Constant(100), new Constant(200) } };
		MatrixExpression m1 = new MatrixExpression(expArray);

		// the internal representation for
		String str2 = "reshape([1, 10, 100, 2, 20, 200],3,2)";
		MatrixExpression m2 = (MatrixExpression) FormulaParser.parseValue(str2);

		String str3 = "[1, 2 ; 10, 20 ; 100, 200]";
		MatrixExpression m3 = (MatrixExpression) FormulaParser.parseValue(str3);

		Expression[] expArray1d = { new Constant(1), new Constant(10), new Constant(100),
				new Constant(2), new Constant(20), new Constant(200) };
		MatrixExpression m4 = new MatrixExpression(expArray1d, new int[] { 3, 2 });

		for (int y = 0; y < 2; ++y)
		{
			for (int x = 0; x < 3; ++x)
			{
				double val = dblArray[x][y];

				Assert.assertEquals("value in m1.get(" + x + ", " + y + ") was wrong", val,
						((Constant) m1.get(x, y)).getVal(), TOL);
				Assert.assertEquals("value in m2.get(" + x + ", " + y + ") was wrong", val,
						((Constant) m2.get(x, y)).getVal(), TOL);
				Assert.assertEquals("value in m3.get(" + x + ", " + y + ") was wrong", val,
						((Constant) m3.get(x, y)).getVal(), TOL);
				Assert.assertEquals("value in m4.get(" + x + ", " + y + ") was wrong", val,
						((Constant) m4.get(x, y)).getVal(), TOL);
			}
		}
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
		String lutStr = "lut([t], [1.0, 2.0, 1.0, 2.0], [0.0, 10.0, 30.0, 40.0])";

		Expression e = FormulaParser.parseValue(lutStr);

		Assert.assertEquals(lutStr, e.toDefaultString());
	}

	/**
	 * Test a 1-d LUT with expression as input
	 */
	@Test
	public void testLut1dExpressionInput()
	{
		String lutStr = "lut([t + 1], [1, 2, 1, 2], [0, 10, 30, 40])";
		String[][] dynamics = { { "t", "t", "0" }, { "y", lutStr, "15" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		new ConvertLutFlowsPass().runTransformationPass(c, null);

		// lut([t+1], [1, 2, 1, 2], [0, 10, 30, 40])
		String[] names = { "on_0", "on_1", "on_2" };
		String[] invariants = { "t + 1.0 <= 10.0", "t + 1.0 >= 10.0 & t + 1.0 <= 30.0",
				"t + 1.0 >= 30.0" };
		String[] flows = { "1 + 1 / 10 * ((t+1) - 0)", "2 + -1 * ((t+1) - 10) / 20",
				"1 + 1 * ((t+1) - 30) / 10" };

		for (int i = 0; i < names.length; ++i)
		{
			String name = names[i];
			String invariant = invariants[i];
			String flow = flows[i];

			AutomatonMode am = ha.modes.get(name);
			Assert.assertNotEquals("mode " + name + " is not supposed to be null", null, am);

			Assert.assertEquals("invariant in " + name + " is incorrect", invariant,
					am.invariant.toDefaultString());

			Expression gotFlow = am.flowDynamics.get("y").asExpression();
			Expression expectedFlow = FormulaParser.parseValue(flow);

			String errorMsg = AutomatonUtil.areExpressionsEqual(expectedFlow, gotFlow);

			if (errorMsg != null)
				Assert.fail("In mode '" + name + "', " + errorMsg);
		}

		// test the guard from mode 0 to mode 1 (should be t >= 10)
		AutomatonTransition at = ha.findTransition(names[0], names[1]);
		Assert.assertNotNull("transition exists between " + names[0] + " and " + names[1], at);
		Assert.assertEquals("guard for transition from mode 0 to mode 1 is incorrect",
				"t + 1.0 >= 10.0 & t >= 0.0", at.guard.toDefaultString());

		// test the guard from mode 2 to mode 1 (should be t <= 30)
		at = ha.findTransition(names[2], names[1]);
		Assert.assertEquals("guard for transition from mode 2 to mode 1 is incorrect",
				"t + 1.0 <= 30.0 & t <= 0.0", at.guard.toDefaultString());
	}

	/**
	 * Test a 1-d LUT in flow
	 */
	@Test
	public void testLut1d()
	{
		String lutStr = "lut([t], [1, 2, 1, 2], [0, 10, 30, 40])";
		String[][] dynamics = { { "t", "1", "0" }, { "y", lutStr, "15" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		new ConvertLutFlowsPass().runTransformationPass(c, null);

		// lut([t], [1, 2, 1, 2], [0, 10, 30, 40])
		// there should be 3 modes + init
		Assert.assertEquals("4 modes after conversion", 4, ha.modes.size());

		// transition from start should point to each of the three (3
		// transitions)
		// and there should be bi-directional transitions between each of them
		// (2 * 2 = 4 transitions)
		Assert.assertEquals("7 transitions after conversion", 7, ha.transitions.size());

		// 15 -> 1.75
		String[] names = { "on_0", "on_1", "on_2" };
		String[] invariants = { "t <= 10.0", "t >= 10.0 & t <= 30.0", "t >= 30.0" };
		String[] flows = { "1 + 1 / 10 * (t - 0)", "2 + -1 * (t - 10) / 20",
				"1 + 1 * (t - 30) / 10" };

		for (int i = 0; i < names.length; ++i)
		{
			String name = names[i];
			String invariant = invariants[i];
			String flow = flows[i];

			AutomatonMode am = ha.modes.get(name);
			Assert.assertNotEquals("mode " + name + " is not supposed to be null", null, am);

			Assert.assertEquals("invariant in " + name + " is incorrect", invariant,
					am.invariant.toDefaultString());

			Expression gotFlow = am.flowDynamics.get("y").asExpression();
			Expression expectedFlow = FormulaParser.parseValue(flow);

			String errorMsg = AutomatonUtil.areExpressionsEqual(expectedFlow, gotFlow);

			if (errorMsg != null)
				Assert.fail("In mode '" + name + "', " + errorMsg);
		}

		// test the guard from mode 0 to mode 1 (should be t >= 10)
		AutomatonTransition at = ha.findTransition(names[0], names[1]);
		Assert.assertNotNull("transition exists between " + names[0] + " and " + names[1], at);
		Assert.assertEquals("guard for transition from mode 0 to mode 1 is incorrect",
				"t >= 10.0 & 1.0 >= 0.0", at.guard.toDefaultString());

		// test the guard from mode 2 to mode 1 (should be t <= 30)
		at = ha.findTransition(names[2], names[1]);
		Assert.assertEquals("guard for transition from mode 2 to mode 1 is incorrect",
				"t <= 30.0 & 1.0 <= 0.0", at.guard.toDefaultString());
	}

	/**
	 * Test a 2-d LUT in flow
	 */
	@Test
	public void testLut2d()
	{
		String lutStr = "lut([a, b], [1, 2, 4 ; 2, 3, 5 ; 3, 5, 10], [0, 1, 3], [0, 10, 30])";
		String[][] dynamics = { { "a", "1", "0" }, { "b", "1", "0" }, { "y", lutStr, "15" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		// should have four modes created, with the split at (a,b)=(1,10)
		// ok, so dynamics in mode '1_1' (lower right) are based on the
		// quadrant:
		// 3 5
		// 5 10
		// where a ranges from 1 to 3
		// and b ranges from 10 to 30
		// 1. interpolate a at the top of the range: atop = 3+(a-1)*1 (1 comes
		// from (5-3)/(3-1))
		// 2. interpolate a at the bottom of the range: abottom = 5+(a-1)*2.5
		// (2.5 comes from (10-5)/(3-1))
		// 3. use b to interpolate between the a's: output = atop + (b-10)/20 *
		// (abottom - atop) (20 comes from 30-10)
		// = 3+(a-1)*1 + (b-10)/20 * (5+(a-1)*2.5 - (3+(a-1)*1))

		new ConvertLutFlowsPass().runTransformationPass(c, null);

		// there should be 4 modes + init
		Assert.assertEquals("5 modes after conversion", 5, ha.modes.size());

		// transition from start should point to each of the four (4
		// transitions)
		// and there should be bi-directional transitions between each of them
		// (2 * 4 = 8 transitions)
		Assert.assertEquals("12 transitions after conversion", 12, ha.transitions.size());

		// check the invariant at 1_1
		String name = "on_1_1";
		String invariant = "a >= 1.0 & b >= 10.0";

		AutomatonMode am = ha.modes.get(name);
		Assert.assertNotEquals("mode " + name + " is not supposed to be null", null, am);
		Assert.assertEquals("invariant in " + name + " is incorrect", invariant,
				am.invariant.toDefaultString());

		// check the flow dynamics for a 'random' point
		Expression expected = FormulaParser
				.parseValue("3+(a-1)*1 + (b-10)/20 * (5+(a-1)*2.5 - (3+(a-1)*1))");
		Expression got = am.flowDynamics.get("y").asExpression();

		String errorMsg = AutomatonUtil.areExpressionsEqual(expected, got);

		if (errorMsg != null)
			Assert.fail("In mode '" + name + "', " + errorMsg);
	}

	@Test
	public void testLinearInterpolation1d()
	{
		String lutStr = "lut([t], [1, 2, 1, 2], [0, 10, 30, 40])";
		LutExpression lut = (LutExpression) FormulaParser.parseValue(lutStr);
		int[] indexList = new int[] { 0 };
		Interval[] rangeList = new Interval[] { new Interval(0, 10) };

		Expression expected = FormulaParser.parseValue("1.0 + 1.0 / 10.0 * (t - 0)");
		Expression got = ConvertLutFlowsPass.nLinearInterpolation(lut, indexList, rangeList);

		String msg = AutomatonUtil.areExpressionsEqual(expected, got);

		if (msg != null)
			Assert.fail("1-d lut interpolation was wrong: " + msg);
	}

	@Test
	public void testLinearInterpolation2d()
	{
		String lutStr = "lut([a, b], [1, 2, 4 ; 2, 3, 5 ; 3, 5, 10], [0, 1, 3], [0, 10, 30])";
		LutExpression lut = (LutExpression) FormulaParser.parseValue(lutStr);
		int[] indexList = new int[] { 1, 1 };
		Interval[] rangeList = new Interval[] { new Interval(1, 3), new Interval(10, 30) };

		Expression expected = FormulaParser
				.parseValue("3+(a-1)*1 + (b-10)/20 * (5+(a-1)*2.5 - (3+(a-1)*1))");
		Expression got = ConvertLutFlowsPass.nLinearInterpolation(lut, indexList, rangeList);

		String msg = AutomatonUtil.areExpressionsEqual(expected, got);

		if (msg != null)
			Assert.fail("2-d lut interpolation was wrong: " + msg);
	}

	@Test
	public void testClassifyLut()
	{
		String val = "lut([(input - x) * 5, input - x - v], "
				+ "[-2, -1.5, -1, -0.5, 0 ; -1.5, -1, -0.5, 0, 0.5 ; -1, -0.5, 0, 0.5, 1 "
				+ "; -0.5, 0, 0.5, 1, 1.5 ; 0, 0.5, 1, 1.5, 2], [-1.0, -0.5, 0.0, 0.5, 1.0], "
				+ "[-1.0, -0.5, 0.0, 0.5, 1.0])";
		Expression e = FormulaParser.parseValue(val);

		byte b = AutomatonUtil.classifyExpressionOps(e);

		Assert.assertNotEquals("Classification of lut expression should be nonzero", 0, b);
	}

	@Test
	public void testLutLinearReset()
	{
		String lutStr = "lut([(5-x)*5,(5-x-v)], reshape([-2, -1.5, -1, -0.5, 0, "
				+ "-1.5, -1, -0.5, 0, 0.5, -1, -0.5, 0, 0.5, 1, -0.5, 0, 0.5, 1, 1.5, 0, "
				+ "0.5, 1.0, 1.5, 2],5,5) , [-1, -0.5, 0, 0.5, 1], [-1, -0.5, 0, 0.5, 1])";
		String[][] dynamics = { { "t", "1", "0" }, { "x", "v", "0" }, { "v", lutStr, "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		new ConvertLutFlowsPass().runTransformationPass(c, null);

		// we are interested in the guard from on_3_3 to on_3_2

		AutomatonTransition at = null;
		AutomatonMode am = ha.modes.get("on_3_3");

		for (AutomatonTransition t : ha.transitions)
		{
			if (t.from.name.equals("on_3_3") && t.to.name.equals("on_3_2"))
				at = t;
		}

		Assert.assertNotNull("Didn't find mode 3_3", am);
		Assert.assertNotNull("Didn't find transition 3_3 -> 3_2", at);

		// derivative of x should be v
		Expression expectedX = FormulaParser.parseValue("v");
		Expression xDer = am.flowDynamics.get("x").asExpression();
		Assert.assertEquals(null, AutomatonUtil.areExpressionsEqual(expectedX, xDer));

		// derivative of v should be 6 * 5.0 - 1 * v - 6 * x
		Expression expectedV = FormulaParser.parseValue("6 * 5.0 - 1 * v - 6 * x");
		Expression vDer = am.flowDynamics.get("v").asExpression();
		Assert.assertEquals(null, AutomatonUtil.areExpressionsEqual(expectedV, vDer));

		// in on_3_3 -> on_3_2, there is a change along the second input, which
		// is (5-x-v)
		// the condition should be that (5-x-v) <= 0.5 and the derivative is
		// negative
		// derivative is negative if (5-x-v)' <= 0, which is the same as
		// -x' - v' = -v - (6 * 5.0 - 1 * v - 6 * x)
		Expression guard = at.guard;

		Assert.assertTrue(guard instanceof Operation && guard.asOperation().op == Operator.AND);
		Expression left = guard.asOperation().getLeft();
		Expression right = guard.asOperation().getRight();

		// left is the value condition (I guess the left/right part is
		// implementation specific)
		Expression expectedLeft = FormulaParser.parseValue("5-x-v"); // <= 0.5
		Assert.assertEquals(null,
				AutomatonUtil.areExpressionsEqual(expectedLeft, left.asOperation().getLeft()));

		// right is the derivative condition
		Expression expectedRight = FormulaParser.parseValue("-v - (6 * 5.0 - v - 6*x)"); // <=
																							// 0
		String str = AutomatonUtil.areExpressionsEqual(expectedRight,
				right.asOperation().getLeft());

		if (str != null)
			Assert.fail(str);
	}

	@Test
	public void testDerivativeOf()
	{
		// test extracted from testLutLinearReset
		// (5-x-v)', with x' == v and v' == 6 * 5.0 - 1 * v - 6 * x

		HashMap<String, Expression> map = new HashMap<String, Expression>();
		map.put("x", FormulaParser.parseValue("v"));
		map.put("v", FormulaParser.parseValue("6 * 5.0 - 1 * v - 6 * x"));

		Expression input = FormulaParser.parseValue("(5-x-v)");
		Expression der = AutomatonUtil.derivativeOf(input, map);

		Expression expected = FormulaParser.parseValue("-v - (6 * 5.0 - v - 6*x)");

		String str = AutomatonUtil.areExpressionsEqual(expected, der);

		if (str != null)
			Assert.fail(str);
	}

	@Test
	public void testLutLinearNoSmall()
	{
		if (!PythonBridge.hasPython())
			return;

		String lutStr = "lut([x, v],  " + "[-2, -1.9; " + "-1.9, -1.8]," + "[-1, 0], " + "[-1, 0])";
		String[][] dynamics = { { "t", "1", "0" }, { "x", "v", "0" }, { "v", lutStr, "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		new ConvertLutFlowsPass().runTransformationPass(c, null);

		// we are interested in on_0_0
		// we want to make sure there are no small coefficients there
		// due to floating-point roundoff issues

		AutomatonMode am = ha.modes.get("on_0_0");

		String der = am.flowDynamics.get("v").toDefaultString();

		Assert.assertFalse(
				"Small numbers found (from floating-point roundoff) in converted dynamics"
						+ " for v:\n" + der,
				der.contains("0.0000000000"));
	}

	@Test
	public void testLutLinearNoSmallFullTable()
	{
		if (!PythonBridge.hasPython())
			return;

		String lutStr = "lut([(input-x)*5,(input-x-v)], " + "[-2.0000, -1.9000; "
				+ "-1.9000, -1.8000]" + ", [-1.0000, -0.0800]" + ", [-1.0000, -0.0800])";
		String[][] dynamics = { { "t", "1", "0" }, { "x", "v", "0" }, { "v", lutStr, "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);
		BaseComponent ha = (BaseComponent) c.root;

		ha.variables.add("input");

		for (AutomatonMode am : ha.modes.values())
			am.flowDynamics.put("input", new ExpressionInterval("0"));

		new ConvertLutFlowsPass().runTransformationPass(c, null);

		// we are interested in on_0_0
		// we want to make sure there are no small coefficients there
		// due to floating-point roundoff issues

		AutomatonMode am = ha.modes.get("on_0_0");

		String der = am.flowDynamics.get("v").toDefaultString();

		Assert.assertFalse(
				"Small numbers found (from floating-point roundoff) in converted dynamics"
						+ " for v:\n" + der,
				der.contains("0.0000000000"));
	}

	@Test
	public void testLutPrinter()
	{
		// Test the auto-conversion of a lut
		String lutStr = "lut([u1, u2], "
				+ "reshape([.4,.3,.35,.3,.2,.22,.22,.4,.35,.5,.20,.22,.5,.4,.35,.35,.3,.45,.5,.4],5,4),"
				+ "[1000,1500,2000,2500,3000]," + "[0.1,0.2,0.3,0.4])";

		String[][] dynamics = { { "t", "1", "0" }, { "u1", "2000", "1000" }, { "u2", "0.3", "0.1" },
				{ "out", "out + " + lutStr, "0" } };
		Configuration c = AutomatonUtil.makeDebugConfiguration(dynamics);

		ToolPrinter printer = new PySimPrinter();
		printer.setOutputString();
		printer.print(c, "", "fakeinput.xml");

		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);

		Assert.assertTrue("output doesn't contain 'lut('", !out.contains("lut("));
	}

	@Test
	public void testLutGenerated()
	{
		String lutStr = "[-1:0.5:1]";

		Expression e = FormulaParser.parseValue(lutStr);

		Assert.assertEquals("[-1.0, -0.5, 0.0, 0.5, 1.0]", e.toDefaultString());
	}
}
