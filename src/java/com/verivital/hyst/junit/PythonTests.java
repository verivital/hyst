package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize;
import com.verivital.hyst.passes.complex.hybridize.AffineOptimize.OptimizationParams;
import com.verivital.hyst.python.PythonBridge;
import com.verivital.hyst.python.PythonUtil;

public class PythonTests
{
	@Test
	public void testPythonFuncs()
	{
		PythonBridge pb = new PythonBridge();
		boolean runTests = true;

		try
		{
			pb.open();
		}
		catch (AutomatonExportException e) 
		{
			// skip these tests if python doesn't open correctly
			runTests = false; 
			System.out.println("Couldn't open python bridge; skipping tests " + e);
		}
		
		if (runTests)
		{
			sympyTests(pb);
			scipyTests(pb);
		
			pb.close();
		}
	}

	private void sympyTests(PythonBridge pb)
	{
		boolean skip = false;
		
		try
		{
			pb.send("import sympy");
		}
		catch (AutomatonExportException e)
		{
			System.out.println("Sympy import failed; skipping tests: " + e);
			skip = true;
		}
		
		if (!skip)
		{
			testIntervalOptBranchAndBound(pb);
			testIntervalOpt(pb);
			testIntervalOptMulti(pb);
		}
	}

	private void scipyTests(PythonBridge pb)
	{
		boolean skip = false;
		
		try
		{
			pb.send("import scipy");
		}
		catch (AutomatonExportException e)
		{
			System.out.println("Scipy import failed; skipping tests: " + e);
			skip = true;
		}
		
		if (!skip)
		{
			testVanderpolOptimize(pb);
			testAffineHybridized(pb);
			testMultiOptimizeSciPy(pb);
		}
	}
	
	private void testAffineHybridized(PythonBridge pb)
	{
		LinkedHashMap<String, ExpressionInterval> dy = new LinkedHashMap<String, ExpressionInterval>();
		dy.put("x", new ExpressionInterval(FormulaParser.parseNumber("2 * x + y")));
		dy.put("y", new ExpressionInterval(FormulaParser.parseNumber("3 * y * x + y")));
		
		HashMap<String, Interval> bounds = new 	HashMap<String, Interval>();
		bounds.put("x", new Interval(1, 2));
		bounds.put("y", new Interval(2, 3));
		
		// linear estimate is 
		// y' == 7.5*x + 5.5*y
		// interval for y should be [10.5, 12]
		
		List<OptimizationParams> params = new ArrayList<OptimizationParams>();
		OptimizationParams op = new OptimizationParams();
		op.original = dy;
		op.bounds = bounds;
		params.add(op);
		
		AffineOptimize.createAffineDynamics(pb, params);
		ExpressionInterval yEi = params.get(0).result.get("y");
		
		// the interval is offset to start at 0
		Assert.assertEquals("hybridized dynamics are correct", "7.5 * x + 5.5 * y + -12 + [0, 1.5]", yEi.toDefaultString());
	}

	private void testIntervalOptBranchAndBound(PythonBridge pb)
	{
		Expression e = FormulaParser.parseFlow("var' == x^2 - 2*x").asOperation().getRight();
		double maxWidth = 0.1;
		
		Map <String, Interval> range1 = new HashMap <String, Interval>();
		range1.put("x", new Interval(0, 2));
		
		ArrayList<Map <String, Interval>> ranges = new ArrayList<Map <String, Interval>>(2); 
		ranges.add(range1);
		
		Interval rv = PythonUtil.intervalOptimizeMulti_bb(pb, e, ranges, maxWidth).get(0);

		if (rv.max > 0 || rv.min < -1.1)
			Assert.fail("bounds was too pessimistic (not inside [-1.1,0]): " + rv);
	}
	
	private void testIntervalOpt(PythonBridge pb)
	{
		Expression e = FormulaParser.parseFlow("var' == 2*x + y - x").asOperation().children.get(1);
		
		Map <String, Interval> ranges = new HashMap <String, Interval>();
		ranges.put("x", new Interval(0, 1));
		ranges.put("y", new Interval(-0.2, -0.1));
		
		Interval rv = PythonUtil.intervalOptimize(pb, e, ranges);
		
		final double EPSILON = 1e-9;
		
		if (Math.abs(rv.min - -0.2) > EPSILON || Math.abs(rv.max - 0.9) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [-0.2, 0.9], got " + rv);
	}
	
	private void testIntervalOptMulti(PythonBridge pb)
	{
		Expression e = FormulaParser.parseFlow("var' == 2*x + y - x").asOperation().children.get(1);
		
		Map <String, Interval> range1 = new HashMap <String, Interval>();
		range1.put("x", new Interval(0, 1));
		range1.put("y", new Interval(-0.2, -0.1));
		
		Map <String, Interval> range2 = new HashMap <String, Interval>();
		range2.put("x", new Interval(1, 2.5));
		range2.put("y", new Interval(-1.2, -1.1));
		
		ArrayList<Map <String, Interval>> ranges = new ArrayList<Map <String, Interval>>(2); 
		ranges.add(range1);
		ranges.add(range2);
		
		List<Interval> rv = PythonUtil.intervalOptimizeMulti(pb, e, ranges);
		
		final double EPSILON = 1e-9;
		
		Interval i0 = rv.get(0);
		Interval i1 = rv.get(1);
		
		if (Math.abs(i0.min - -0.2) > EPSILON || Math.abs(i0.max - 0.9) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [-0.2, 0.9], got " + rv);
		
		if (Math.abs(i1.min - -0.2) > EPSILON || Math.abs(i1.max - 1.4) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [-0.2, 1.4], got " + rv);
	}
	
	private void testVanderpolOptimize(PythonBridge pb)
	{
		// (1-x*x)*y-x has critical points at (1, 0.5) and (-1, -0.5)
		Expression e = FormulaParser.parseNumber("(1-x*x)*y-x");
		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		bounds.put("x", new Interval(-1.1, -0.9));
		bounds.put("y", new Interval(0.49, 0.51));
		
		Interval sciPi = PythonUtil.scipyOptimize(pb, e, bounds);
		
		Assert.assertTrue("optimization included 1.0", sciPi.contains(1.0));
	}
	
	private void testMultiOptimizeSciPy(PythonBridge pb)
	{
		ArrayList <Expression> expList = new ArrayList <Expression>(); 
		expList.add(FormulaParser.parseNumber("x * x"));
		expList.add(FormulaParser.parseNumber("x * y"));
		
		HashMap<String, Interval> bounds1 = new HashMap<String, Interval>();
		bounds1.put("x", new Interval(-1, 1));
		bounds1.put("y", new Interval(-1, 1));

		HashMap<String, Interval> bounds2 = new HashMap<String, Interval>();
		bounds2.put("x", new Interval(1, 2));
		bounds2.put("y", new Interval(1, 2));

		ArrayList<HashMap<String, Interval>> boundsList = new ArrayList<HashMap<String, Interval>>();
		boundsList.add(bounds1);
		boundsList.add(bounds2);
		
		List<Interval> result = PythonUtil.scipyOptimize(pb, expList, boundsList);
		
		Interval i0 = result.get(0);
		Interval i1 = result.get(1);
		final double EPSILON = 1e-9;
		
		if (Math.abs(i0.min - 0) > EPSILON || Math.abs(i0.max - 1) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [0, 1], got " + i0);
		
		if (Math.abs(i1.min - 1) > EPSILON || Math.abs(i1.max - 4) > EPSILON)
			Assert.fail("Computed bounds were wrong. Expected [1, 4], got " + i1);
	}
}
