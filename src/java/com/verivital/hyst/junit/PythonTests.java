package com.verivital.hyst.junit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.complex.HybridizeGridPass;
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
			System.out.println("Couldn't open python bridge; skipping tests.");
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
		}
	}
	
	public void testAffineHybridized(PythonBridge pb)
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
		
		LinkedHashMap<String, ExpressionInterval> newDy = HybridizeGridPass.createAffineDynamics(pb, dy, bounds);
		ExpressionInterval yEi = newDy.get("y");
		
		double TOL = 10e-6;
		Assert.assertTrue("interval min bound is -12", Math.abs((-12) - yEi.getInterval().min) < TOL);
		Assert.assertTrue("interval max bound is -10.5", Math.abs((-10.5) - yEi.getInterval().max) < TOL);
	}

	public void testIntervalOptBranchAndBound(PythonBridge pb)
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
	
	public void testIntervalOpt(PythonBridge pb)
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
	
	public void testIntervalOptMulti(PythonBridge pb)
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
	
	public void testVanderpolOptimize(PythonBridge pb)
	{
		// (1-x*x)*y-x has critical points at (1, 0.5) and (-1, -0.5)
		Expression e = FormulaParser.parseNumber("(1-x*x)*y-x");
		HashMap<String, Interval> bounds = new HashMap<String, Interval>();
		bounds.put("x", new Interval(-1.1, -0.9));
		bounds.put("y", new Interval(0.49, 0.51));
		
		Interval sciPi = PythonUtil.scipyOptimize(pb, e, bounds);
		
		Assert.assertTrue("optimization included 1.0", sciPi.contains(1.0));
	}
}
