package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationModeParams;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationParams;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.python.PythonUtil;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.KodiakUtil;

/**
 * All these tests require python, so if it fails to load, they will be skipped
 * 
 * @author sbak
 *
 */
@RunWith(Parameterized.class)
public class PythonTests
{
	@Parameters
	public static Collection<Object[]> data()
	{
		return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public PythonTests(boolean block)
	{
		PythonBridge.setBlockPython(block);
	}

	@Test
	public void testAffineApprox()
	{
		// this function tests that the produced affine approximation of (2 +
		// 1)/2 is exactly 1.5
		LinkedHashMap<String, ExpressionInterval> flow = new LinkedHashMap<String, ExpressionInterval>();
		flow.put("x", new ExpressionInterval("(2 + 1)/2"));
		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		bounds.put("x", new Interval(0, 1));

		Expression e = AffineOptimize.affineApprox(flow, bounds).get("x").asExpression();
		Assert.assertNull(AutomatonUtil.areExpressionsEqual("1.5", e));
	}

	@Test
	public void testAffineHybridized()
	{
		if (!PythonBridge.hasPython())
			return;

		LinkedHashMap<String, ExpressionInterval> dy = new LinkedHashMap<String, ExpressionInterval>();
		dy.put("x", new ExpressionInterval(FormulaParser.parseValue("2 * x + y")));
		dy.put("y", new ExpressionInterval(FormulaParser.parseValue("3 * y * x + y")));

		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		bounds.put("x", new Interval(1, 2));
		bounds.put("y", new Interval(2, 3));

		LinkedHashMap<String, ExpressionInterval> avg = AffineOptimize.affineApprox(dy, bounds);

		// linear estimate is
		// y' == 7.5*x + 5.5*y
		// interval for y should be [10.5, 12]

		List<OptimizationParams> params = new ArrayList<OptimizationParams>();
		OptimizationParams op = new OptimizationParams();
		op.newDynamics = avg;
		OptimizationModeParams modeParams = new OptimizationModeParams();
		op.origModes.add(modeParams);

		modeParams.origDynamics = dy;
		modeParams.bounds = bounds;
		params.add(op);

		AffineOptimize.optimizeDynamics("basinhopping", params);
		ExpressionInterval yEi = params.get(0).result.get("y");

		Assert.assertNull(
				AutomatonUtil.areExpressionIntervalsEqual("7.5 * x + 5.5 * y - 12", 0, 1.5, yEi));
	}

	@Test
	public void testIntervalOptBranchAndBound()
	{
		if (!PythonBridge.hasPython())
			return;

		Expression e = FormulaParser.parseFlow("var' == x^2 - 2*x").asOperation().getRight();
		List<Expression> eList = Arrays.asList(e);

		HashMap<String, Interval> range1 = new HashMap<String, Interval>();
		range1.put("x", new Interval(0, 2));

		ArrayList<HashMap<String, Interval>> ranges = new ArrayList<HashMap<String, Interval>>(1);
		ranges.add(range1);

		Interval rv = PythonUtil.intervalOptimizeBounded(eList, ranges, 0.1).get(0);

		if (rv.max > 0.1 || rv.min < -1.1)
			Assert.fail("bounds was too pessimistic (not inside [-1.1,0.1]): " + rv);
	}

	@Test
	public void testIntervalOpt()
	{
		if (!PythonBridge.hasPython())
			return;

		Expression e = FormulaParser.parseFlow("var' == 2*x + y - x").asOperation().children.get(1);
		List<Expression> eList = Arrays.asList(e);

		HashMap<String, Interval> ranges = new HashMap<String, Interval>();
		ranges.put("x", new Interval(0, 1));
		ranges.put("y", new Interval(-0.2, -0.1));
		List<HashMap<String, Interval>> rList = Arrays.asList(ranges);

		Interval rv = PythonUtil.intervalOptimize(eList, rList).get(0);

		final double EPSILON = 1e-9;

		if (Math.abs(rv.min - -0.2) > EPSILON || Math.abs(rv.max - 0.9) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [-0.2, 0.9], got " + rv);
	}

	@Test
	public void testIntervalOptMulti()
	{
		if (!PythonBridge.hasPython())
			return;

		Expression e = FormulaParser.parseFlow("var' == 2*x + y - x").asOperation().children.get(1);
		List<Expression> eList = Arrays.asList(e, e);

		HashMap<String, Interval> range1 = new HashMap<String, Interval>();
		range1.put("x", new Interval(0, 1));
		range1.put("y", new Interval(-0.2, -0.1));

		HashMap<String, Interval> range2 = new HashMap<String, Interval>();
		range2.put("x", new Interval(1, 2.5));
		range2.put("y", new Interval(-1.2, -1.1));

		ArrayList<HashMap<String, Interval>> ranges = new ArrayList<HashMap<String, Interval>>(2);
		ranges.add(range1);
		ranges.add(range2);

		List<Interval> rv = PythonUtil.intervalOptimize(eList, ranges);

		final double EPSILON = 1e-9;

		Interval i0 = rv.get(0);
		Interval i1 = rv.get(1);

		if (Math.abs(i0.min - -0.2) > EPSILON || Math.abs(i0.max - 0.9) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [-0.2, 0.9], got " + rv);

		if (Math.abs(i1.min - -0.2) > EPSILON || Math.abs(i1.max - 1.4) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [-0.2, 1.4], got " + rv);
	}

	@Test
	public void testVanderpolOptimize()
	{
		if (!PythonBridge.hasPython())
			return;

		// (1-x*x)*y-x has critical points at (1, 0.5) and (-1, -0.5)
		Expression e = FormulaParser.parseValue("(1-x*x)*y-x");
		List<Expression> eList = Arrays.asList(e);
		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		bounds.put("x", new Interval(-1.1, -0.9));
		bounds.put("y", new Interval(0.49, 0.51));

		List<HashMap<String, Interval>> bList = Arrays.asList(bounds);

		Interval sciPi = PythonUtil.scipyOptimize(eList, bList).get(0);

		Assert.assertTrue("optimization included 1.0", sciPi.contains(1.0));
	}

	@Test
	public void testOptimizeSqrt()
	{
		if (!PythonBridge.hasPython())
			return;

		ArrayList<Expression> expList = new ArrayList<Expression>();
		expList.add(FormulaParser.parseValue("sqrt(x)"));

		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		bounds.put("x", new Interval(1, 4));

		ArrayList<HashMap<String, Interval>> boundsList = new ArrayList<HashMap<String, Interval>>();
		boundsList.add(bounds);

		List<Interval> result = PythonUtil.scipyOptimize(expList, boundsList);

		Interval i0 = result.get(0);

		double TOL = 1e-6;
		Assert.assertEquals("lower bound of sqrt([1, 4]) = 1", 1, i0.min, TOL);
		Assert.assertEquals("upper bound of sqrt([1, 4]) = 2", 2, i0.max, TOL);
	}

	@Test
	public void testMultiOptimizeSciPy()
	{
		if (!PythonBridge.hasPython())
			return;

		ArrayList<Expression> expList = new ArrayList<Expression>();
		expList.add(FormulaParser.parseValue("x * x"));
		expList.add(FormulaParser.parseValue("x * y"));

		HashMap<String, Interval> bounds1 = new HashMap<String, Interval>();
		bounds1.put("x", new Interval(-1, 1));
		bounds1.put("y", new Interval(-1, 1));

		HashMap<String, Interval> bounds2 = new HashMap<String, Interval>();
		bounds2.put("x", new Interval(1, 2));
		bounds2.put("y", new Interval(1, 2));

		ArrayList<HashMap<String, Interval>> boundsList = new ArrayList<HashMap<String, Interval>>();
		boundsList.add(bounds1);
		boundsList.add(bounds2);

		List<Interval> result = PythonUtil.scipyOptimize(expList, boundsList);

		Interval i0 = result.get(0);
		Interval i1 = result.get(1);
		final double EPSILON = 1e-9;

		if (Math.abs(i0.min - 0) > EPSILON || Math.abs(i0.max - 1) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [0, 1], got " + i0);

		if (Math.abs(i1.min - 1) > EPSILON || Math.abs(i1.max - 4) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [1, 4], got " + i1);
	}

	@Test
	public void testSimplify()
	{
		if (!PythonBridge.hasPython())
			return;

		String str = "sin(x)^2 + cos(x)^2";
		Expression e = FormulaParser.parseValue(str);
		Expression result = PythonUtil.pythonSimplifyExpression(e);
		Assert.assertEquals("python failed at trig simplification", 1,
				Float.parseFloat(result.toDefaultString()), 1e-9);

		// try another one which doesn't simplify
		str = "-2.0 ^ time / sqrt(y) + sin(2.0 * x) + 5.0";
		e = FormulaParser.parseValue(str);
		result = PythonUtil.pythonSimplifyExpression(e);
		String expected = "1.0 * -2.0 ^ time / sqrt(y) + 1.0 * sin(2.0 * x) + 5.0";

		Assert.assertEquals("python simplification incorrect", result.toDefaultString(), expected);
	}

	@Test
	public void testBoundedIntervalVersusKodiak()
	{
		if (!PythonBridge.hasPython())
			return;

		Expression e = FormulaParser
				.parseValue("(1 - x * x) * y - x - (-7.2 * x + -1.03 * y + 8.97)");
		List<Expression> eList = Arrays.asList(e, e);

		HashMap<String, Interval> range1 = new HashMap<String, Interval>();
		range1.put("x", new Interval(0, 2));
		range1.put("y", new Interval(1, 2.2));

		HashMap<String, Interval> range2 = new HashMap<String, Interval>();
		range2.put("x", new Interval(0.6, 0.8));
		range2.put("y", new Interval(1.8, 2.1));

		List<HashMap<String, Interval>> ranges = Arrays.asList(range1, range2);
		List<Interval> kodList = KodiakUtil.kodiakOptimize(eList, ranges);

		if (kodList == null) // kodiak doesn't exist
			return;

		double TOL = 0.1;
		List<Interval> intList = PythonUtil.intervalOptimizeBounded(eList, ranges, TOL);

		for (int i = 0; i < kodList.size(); ++i)
		{
			Interval kod = kodList.get(i);
			Interval bb = intList.get(i);

			Interval.COMPARE_TOL = TOL;

			Assert.assertEquals("Result for optimiziation " + i + " wasn't equal", kod, bb);
		}
	}
}
