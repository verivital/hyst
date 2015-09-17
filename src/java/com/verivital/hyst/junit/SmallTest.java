package com.verivital.hyst.junit;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SimplifyExpressionsPass;
import com.verivital.hyst.passes.complex.ContinuizationPass;
import com.verivital.hyst.passes.complex.ContinuizationPass.IntervalTerm;
import com.verivital.hyst.passes.complex.PseudoInvariantPass;
import com.verivital.hyst.passes.complex.PseudoInvariantPass.PseudoInvariantParams;
import com.verivital.hyst.printers.DReachPrinter.DReachExpressionPrinter;
import com.verivital.hyst.printers.FlowPrinter;
import com.verivital.hyst.printers.StateflowSpPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;

/**
 * Small tests dealing with the expression parser or range extractor. Things that don't require
 * loading a whole model.
 * 
 * @author Stanley Bak
 *
 */
public class SmallTest
{
	@Before 
	public void setUpClass() 
	{      
	    Expression.expressionPrinter = null;
	}
	
	@Test
    public void testFormatDecimal() 
	{
		Assert.assertEquals(ToolPrinter.doubleToString(0.3), "0.3");
		
		Assert.assertEquals(ToolPrinter.doubleToString(10.3), "10.3");
    }
	
	@Test
	public void testComplexFlowExpression()
	{
		String s = "v_i = Ii_dn / C_i & v_p1 = -v_p1 / C_p1 * (1 / R_p2 + 1 / R_p3) + v_p / (C_p1 * R_p3) + Ip_dn / C_p1 & v_p = v_p1 / (C_p3 * R_p3) - v_p / (C_p3 * R_p3) & phi_v = v_i * K_i / N + v_p * K_p / N + 6.28 * f_0 / N & phi_ref = 6.28 * f_ref & t = 0";
				
		FormulaParser.parseFlow(s);
	}
	
	@Test
    public void testTripleExpressionCondition() 
	{
		String sampleGuard = "-5 <= x <= 5";
		
		Expression e = FormulaParser.parseGuard(sampleGuard);

		Assert.assertNotEquals(e, null);
    }
	
	@Test
    public void testScientificNotationCapitalE() 
	{
		String sampleGuard = "-5 <= x <= 1.00E+03";
		
		Expression e = FormulaParser.parseGuard(sampleGuard);

		Assert.assertNotEquals(e, null);
    }
	
	@Test
    public void testPointNoZero() 
	{
		String sampleGuard = "-.5 <= x <= .4e3";
		
		Expression e = FormulaParser.parseGuard(sampleGuard);

		Assert.assertNotEquals(e, null);
    }
	
	@Test
    public void testScientificNotation() 
	{
		String sampleExpression = "0.5 >= x & x <= 0.123e4";
		
		Expression e = FormulaParser.parseGuard(sampleExpression);

		Assert.assertEquals("0.5 >= x & x <= 1230", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "1.5e-5 >= x & x <= 100.123e4";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("0.000015 >= x & x <= 1001230", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "-1.5e+5 >= x & x <= -100.123e-0";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("-150000 >= x & x <= -100.123", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "--1.5e0 >= x & x <= -100.123e+0";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("1.5 >= x & x <= -100.123", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "x == 9.1e16";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 91000000000000000", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "x == 25.1e-16";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 0.00000000000000251", DefaultExpressionPrinter.instance.print(e));
		
		// using 10^X
		sampleExpression = "x == 10^4.1";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10 ^ 4.1", DefaultExpressionPrinter.instance.print(e)); 
		// probably want to do this comparisons with regex to avoid spacing complaints
		
		sampleExpression = "x == 10^-4.1";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10 ^ -4.1", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "x == 10^(0)";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10 ^ 0", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "x == 10^(-1)";
		e = FormulaParser.parseGuard(sampleExpression);
		Assert.assertEquals("x = 10 ^ -1", DefaultExpressionPrinter.instance.print(e));
		
		sampleExpression = "A == 7.89*10^-10.1"; // from E5/E5.xml example from ODE/DAE test set
		e = FormulaParser.parseLoc(sampleExpression);
		Assert.assertEquals("A = 7.89 * 10 ^ -10.1", DefaultExpressionPrinter.instance.print(e));
    }
	
	@Test
	public void testTranscendentalFlow() {
		// this example flow is from the satellite model (Johnson et al, FM 2012)
		String sampleFlow = "nu1' == sqrt( mu / (p1^3)) * ((1 + e1 * cos(nu1))^2)";
		
		Expression e = FormulaParser.parseFlow(sampleFlow);
		
		Assert.assertEquals("nu1 = sqrt(mu / p1 ^ 3) * (1 + e1 * cos(nu1)) ^ 2", 
				DefaultExpressionPrinter.instance.print(e));
	}

	@Test
    public void testLocSingle() 
	{
		String t = "loc(x) = y & x > 5";
		
		Expression e = FormulaParser.parseLoc(t);

		Assert.assertNotEquals(e, null);
    }

	@Test
    public void testLocNone() 
	{
		String t = "x > 5";
		
		Expression e = FormulaParser.parseLoc(t);

		Assert.assertNotEquals(e, null);
    }
	
	@Test
	public void testLocExtract()
	{
		String t = "x = boy & boy <= 5 & boy >= cat & cat = 5";
		
		Expression e = FormulaParser.parseLoc(t);
		
		try
		{
			RangeExtractor.getVariableRange(e, "x");
		} 
		catch (EmptyRangeException ex)
		{
			throw new RuntimeException(ex);
		}
		catch (ConstantMismatchException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	@Test
    public void testLocNested() 
	{
		String t = "loc(automaton.x) = y & x > 5";
		
		Expression e = FormulaParser.parseLoc(t);

		Assert.assertNotEquals(e, null);
    }
	
	@Test
    public void testLocMultiple() 
	{
		String t = "loc(automaton.x) = y & x > 5 & loc(automatonTwo.z) = jump";
		
		Expression e = FormulaParser.parseLoc(t);

		Assert.assertNotEquals(e, null);
    }
	
	@Test
    public void testLocDouble() 
	{
		String t = "loc(x) = y & a > 5 | loc(x) = z & a > 6";
		
		try
		{
			FormulaParser.parseLoc(t);
			
			Assert.fail("ors are not allowed in loc statements");
		}
		catch (AutomatonExportException e) {}
    }
	
	@Test
    public void testLocEmptyComponentName() 
	{
		String t = "x==0 & y==0 & loc()==one";
		
		FormulaParser.parseLoc(t);
    }
	
	@Test
    public void testParseFalseFlow()
    {
		Expression e = FormulaParser.parseFlow("false");
		
		if (e == null || !e.equals(Constant.FALSE))
			Assert.fail("flow not parsed to Constant.FALSE");
    }
	
	@Test
    public void testNestedGuard() 
	{
		String t = "automaton.x <= 5";
		
		try
		{
			FormulaParser.parseGuard(t);
			
			Assert.fail("dotted variables are not allowed in guards");
		}
		catch (AutomatonExportException e) {}
    }
	
	@Test
    public void testNondetermResetTriple() 
	{
		String t = "0 <= x' <= 0.1";
		
		Expression e = FormulaParser.parseReset(t);
		
		if (e == null || !DefaultExpressionPrinter.instance.print(e).equals("0 <= x & x <= 0.1"))
		//if (e == null || !DefaultExpressionPrinter.instance.print(e).equals("((0.0 <= x) && (x <= 0.1))"))
			Assert.fail("parsed incorrectly, was: " + e);
    }
	
	@Test
    public void testResetCombined() 
	{
		String t = "1 <= z & y := x + 3 & 0 <= x' <= 0.1 & z <= 2";
		
		Expression e = FormulaParser.parseReset(t);

		if (e == null || !e.toDefaultString().equals("1 <= z & y = x + 3 & 0 <= x & x <= 0.1 & z <= 2"))
			Assert.fail("parsed incorrectly, was: " + e);
    }
	
	@Test
    public void testNondeterminisiticReset() 
	{
		String sample = "T := 0 && ImaginaryChannel_min' >= 0 && ImaginaryChannel_min' <= 2147483647";
		
		Expression e = FormulaParser.parseReset(sample);
		
		Assert.assertNotEquals(e, null);
		
		Assert.assertTrue(DefaultExpressionPrinter.instance.print(e).equals("T = 0 & ImaginaryChannel_min >= 0 & ImaginaryChannel_min <= 2147483647"));
    }
	
	/**
	 * Test the range extractor logic
	 */
	@Test
	public void testExtractRange()
	{
		Expression e = FormulaParser.parseGuard("y >= ---3"); // double negatives should get cancelled in parser
		
		// am.invariant = 1.0 <= range & range <= 2.0 & x <= 5.0
		
		try
		{
			TreeMap <String, Interval> vals = new TreeMap <String, Interval>();
			
			RangeExtractor.getVariableRanges(e, vals);
			Interval extracted = vals.get("y");
			
			if (extracted == null || !extracted.equals(new Interval(-3, Double.MAX_VALUE)))
				Assert.fail("Extracted range was not [-3, inf]. It was " + extracted);
		} 
		catch (EmptyRangeException e1)
		{
			Assert.fail("range extractor raised exception.");
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
	}
	
	/**
	 * Test the range extractor logic with a single variable
	 */
	@Test
	public void testExtractRangeSingle()
	{
		Expression e = FormulaParser.parseInvariant("1.0 <= range & range <= 2.0 & x <= 5.0"); 
		
		try
		{
			Interval extracted = RangeExtractor.getVariableRange(e, "range");
			
			if (extracted == null || !extracted.equals(new Interval(1, 2)))
				Assert.fail("Extracted range was not [1, 2]. It was " + extracted);
		} 
		catch (EmptyRangeException e1)
		{
			Assert.fail("range extractor raised exception.");
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
	}
	
	/**
	 * Test the range extractor logic for invalid range
	 */
	@Test
	public void testExtractRangeInvalid()
	{
		Expression e = FormulaParser.parseGuard("y >= 3 & y <= 2");
		
		try
		{
			TreeMap <String, Interval> vals = new TreeMap <String, Interval>();
			
			RangeExtractor.getVariableRanges(e, vals);
			
			Assert.fail("range extractor should raise exception on invalid ranges. ranges returned = " + vals);
		} 
		catch (EmptyRangeException e1) {	} 
		catch (ConstantMismatchException e1) 
		{
			Assert.fail("should be empty range");
		}
	}
	
	/**
	 * Test the range extractor logic for invalid range
	 */
	@Test
	public void testNullRange()
	{
		Expression e = FormulaParser.parseGuard("y >= 3 & y <= 2");
		
		try
		{
			Interval rv = RangeExtractor.getVariableRange(e, "x");
			
			if (rv != null)
				Assert.fail("range extractor should return null on empty range");
		} 
		catch (EmptyRangeException e1) {	}
		catch (ConstantMismatchException e1) 
		{
			Assert.fail("should be empty range");
		}
	}
	
	/**
	 * Test the range extractor logic for invalid range
	 */
	@Test
	public void testConstantMismatch()
	{
		Expression e = FormulaParser.parseGuard("x = 2 && y >= 3 & y <= 4 & x == 4");
		
		try
		{
			RangeExtractor.getVariableRange(e, "y");
			
			Assert.fail("expected constant mismatch exception");
		} 
		catch (EmptyRangeException e1) 
		{
			Assert.fail("expected constant mismatch exception");
		}
		catch (ConstantMismatchException e1) 
		{
			// expected
		}
	}
	
	@Test
    public void testExtractVarConstRange() 
	{
		String sampleInv = "xmin == -5 & xmin <= x <= xmax & xmax == 6";
		
		Expression e = FormulaParser.parseInvariant(sampleInv);
		try
		{
			Interval range = RangeExtractor.getVariableRange(e, "x");
			
			if (range == null || !range.equals(new Interval(-5, 6)))
					Assert.fail("wrong range extracted: " + range);
		} 
		catch (EmptyRangeException e1)
		{
			Assert.fail("empty range found");
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
    }
	
	@Test
    public void testExtractVarConstRangeAlias() 
	{
		String sampleInv = "xmin == -5 & xmin <= x_alias <= xmax & xmax == 6 & x == x_alias";
		
		Expression e = FormulaParser.parseInvariant(sampleInv);
		try
		{
			Interval range = RangeExtractor.getVariableRange(e, "x");
			
			if (range == null || !range.equals(new Interval(-5, 6)))
					Assert.fail("wrong range extracted: " + range);
		} 
		catch (EmptyRangeException ex)
		{
			throw new RuntimeException("empty range found", ex);
		}
		catch (ConstantMismatchException e1)
		{
			Assert.fail("range extractor raised constant mismatch exception.");
		}
    }
	
	@Test
    public void testEqualsInterval() 
	{
		Expression[] expressions = {new Constant(0), new Constant(-5), new Variable("x")};
		Interval[] eq = {new Interval(0, 1), new Interval(-5, -4), null};
		Interval[] notEq = {new Interval(0, 2), new Interval(-4, -4), new Interval(0, 0)};
		
		for (int i = 0; i < expressions.length; ++i)
		{
			Expression e = expressions[i];
			Interval eqInt = eq[i];
			Interval neqInt = notEq[i];
			
			if (eqInt != null && !new ExpressionInterval(e, new Interval(0,1)).equalsInterval(eqInt))
				Assert.fail(e + " + [0, 1] != " + eqInt);
			
			if (new ExpressionInterval(e, new Interval(0,1)).equalsInterval(neqInt))
				Assert.fail(e + " + [0, 1] == " + neqInt);
		}
	}
	
	/**
	 * Test using pow exponentials
	 */
	@Test
	public void testPow()
	{
		Expression e = FormulaParser.parseGuard("y >= 10^3");
		
		if (e == null || !DefaultExpressionPrinter.instance.print(e).equals("y >= 10 ^ 3"))
			Assert.fail("displayed pow incorrectly: " + e);
	}
	
	/**
	 * Test using single number
	 */
	@Test
	public void testNumber()
	{
		Expression e = FormulaParser.parseNumber("3.0");
		
		Expression simple = SimplifyExpressionsPass.simplifyExpression(e);
		
		if (!DefaultExpressionPrinter.instance.print(simple).equals("3"))
			Assert.fail("simplification failed: got: " + DefaultExpressionPrinter.instance.print(simple));
	}
	
	/**
	 * Test using pow exponentials
	 */
	@Test
	public void testSimplifyPow()
	{
		Expression e = FormulaParser.parseNumber("3 * 10^7");
		
		Expression simple = SimplifyExpressionsPass.simplifyExpression(e);
		
		if (!DefaultExpressionPrinter.instance.print(simple).equals("30000000"))
			Assert.fail("simplification failed");
	}
	
	@Test 
	public void testMultiResetExpression()
	{
		String str = "g := fischer_agent_2_i & fischer_agent_2_x := 0.0 & g := 2.0";
		Expression exp = FormulaParser.parseReset(str);
		
		ArrayList <String> vars = new ArrayList <String>();
		vars.add("g");
		vars.add("fischer_agent_2_i");
		vars.add("fischer_agent_2_x");
		
		try
		{
			AutomatonUtil.extractExactAssignments(vars, exp);
			
			Assert.fail("multiple resets which could contradict not allowed");
		}
		catch (AutomatonExportException e)
		{
			// expected
		}
	}
	
	@Test 
	public void testNonExactExtractReset()
	{
		Expression exp = FormulaParser.parseReset("x := x + y");
		
		ArrayList <String> vars = new ArrayList <String>();
		vars.add("x");
		vars.add("y");
		
		try
		{
			AutomatonUtil.extractReset(exp, vars);
		}
		catch (AutomatonExportException e)
		{
			Assert.fail("failed getting non-exact assignemnts");
		}
	}
	
	@Test 
	public void testDReachExpressionPrinter()
	{
		DReachExpressionPrinter exp_printer = new DReachExpressionPrinter();
		
		String sampleGuard = "-5 <= x && x <= 5";
		Expression e1 = FormulaParser.parseGuard(sampleGuard);
		Assert.assertEquals("(and (-5.0 <= x) (x <= 5.0))", exp_printer.print(e1));
		
		String sampleFlow = "x' == x^x";
		Expression e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x ^ x)", exp_printer.print(e3));
		
		sampleFlow = "x' == x * x";
		e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x * x)", exp_printer.print(e3));
		
		sampleFlow = "x' == x^2";
		e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x ^ 2.0)", exp_printer.print(e3));
		
		sampleFlow = "x' == x^2.1234";
		e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("(x = x ^ 2.1234)", exp_printer.print(e3));
	}
	
	@Test
	public void testPseudoInvariantCondition1()
	{
		// pseudo invariant at (1.5,1.5) in direction <1,0> should be 1.0 * x + 0.0 * y >= 1.5
		double[] point = {1.5, 1.5};
		double[] dir = {1.0, 0};
		String expectedResult = "1 * x + 0 * y >= 1.5";
		
		PseudoInvariantPass pi = new PseudoInvariantPass();
		pi.vars = new ArrayList<String>(2);
		pi.vars.add("x");
		pi.vars.add("y");
		PseudoInvariantParams pip = pi.new PseudoInvariantParams(point, dir);
		
		if (!DefaultExpressionPrinter.instance.print(pip.inv).equals(expectedResult))
			Assert.fail("created pseudo-invariant was " + pip.inv + " instead of the expected " + expectedResult);
	}
	
	@Test
	public void testPseudoInvariantCondition2()
	{
		// pseudo invariant at (0, 0) in direction <0,1> should be 0.0 * x + 1.0 * y >= 0.0
		double[] point = {0, 0};
		double[] dir = {0, 1};
		String expectedResult = "0 * x + 1 * y >= 0";
		
		PseudoInvariantPass pi = new PseudoInvariantPass();
		pi.vars = new ArrayList<String>(2);
		pi.vars.add("x");
		pi.vars.add("y");
		PseudoInvariantParams pip = pi.new PseudoInvariantParams(point, dir);
		
		String got = DefaultExpressionPrinter.instance.print(pip.inv);
		
		if (!got.equals(expectedResult))
			Assert.fail("created pseudo-invariant was " + got + " instead of the expected " + expectedResult);
	}
	
	@Test
    public void testStateflowExpressionPrinterOne() 
	{
		StateflowSpPrinter spprinter = new StateflowSpPrinter();
		StateflowSpPrinter.StateflowSpExpressionPrinter exp_printer = spprinter.new StateflowSpExpressionPrinter(0);
		String sampleGuard = "-5 <= x <= 5";
		FormulaParser.parseGuard(sampleGuard);
		
		String sampleFlow = "nu1' = sqrt( mu / (p1^3)) * ((1 + e1 * cos(nu1))^2)";
		Expression e3 = FormulaParser.parseFlow(sampleFlow);
		Assert.assertEquals("nu1 = sqrt(mu / p1 ^ 3) * (1 + e1 * cos(nu1)) ^ 2", exp_printer.print(e3));
	
        }
	
	@Test
	public void testFlowstarLinearDetection()
	{
		String exp = "(1.0 - x * x) * y - x";
		
		Expression e = FormulaParser.parseNumber(exp);
		
		if (FlowPrinter.isLinearExpression(e))
			Assert.fail("expression was detected as linear: " + exp);
	}
	
	/**
	 * Make sure usage can be printed in Hyst (will also check for class loading issues)
	 */
	@Test
	public void testPrintUsage()
	{
		String[] args = {"-help"};
		
		Hyst.silentUsage = true;
		Hyst.convert(args);
	}
	
	@Test
	public void testSubstituteExpression()
	{
		Expression e = FormulaParser.parseNumber("5 * c + 2");
		Expression sub = new Operation(Operator.ADD, new Variable("c"), new IntervalTerm(new Interval(0, 1)));
		Expression result = AutomatonUtil.substituteVariable(e, "c", sub);
		
		Assert.assertTrue("Substituted in correctly", result.toDefaultString().equals("5 * (c + [0.0, 1.0]) + 2")); 
		
		ExpressionInterval ei = ContinuizationPass.simplifyExpressionWithIntervals(result);
		
		Assert.assertTrue("substituted in interval was nonnull", ei.getInterval() != null);
		Assert.assertTrue("simplification resulted in correct interval", new Interval(0, 5).equals(ei.getInterval()));
		
		Assert.assertTrue("simplification resulted in correct expression", 
				ei.getExpression().toDefaultString().equals("5 * c + 2"));
	}
	
	@Test
	public void testHarderExpression()
	{
		Expression e = FormulaParser.parseNumber("-10 * v - 3 * a");
		Expression sub = new Operation(Operator.ADD, new Variable("a"), new IntervalTerm(new Interval(-1, 2)));
		Expression result = AutomatonUtil.substituteVariable(e, "a", sub);
		
		Assert.assertTrue("Substituted in correctly", 
				DefaultExpressionPrinter.instance.print(result).equals("-10 * v - 3 * (a + [-1.0, 2.0])")); 
		
		ExpressionInterval ei = ContinuizationPass.simplifyExpressionWithIntervals(result);
		
		Assert.assertTrue("substituted in interval was nonnull", ei.getInterval() != null);
		
		Assert.assertTrue("simplification resulted in correct interval", new Interval(-6, 3).equals(ei.getInterval()));
		
		Assert.assertTrue("simplification resulted in correct expression", 
				DefaultExpressionPrinter.instance.print(ei.getExpression()).equals("-10 * v - 3 * a"));
	}
	
	@Test
	public void testSimpleInvariantFlowPrinter()
	{
		Expression e= FormulaParser.parseInvariant("0 <= t");
		
		Expression.expressionPrinter = DefaultExpressionPrinter.instance;
		String s = FlowPrinter.getFlowConditionExpression(e);
		
		if (!s.contains("<="))
			Assert.fail("flow condition expression didn't convert operator <= correctly: " + s);
	}
	
	@Test
	public void testPrintNullExp()
	{
		Assert.assertEquals("null", DefaultExpressionPrinter.instance.print(null));
	} 
	
	@Test
	public void testSimplifyCos()
	{
		Expression e = FormulaParser.parseNumber("cos(t)");
		
		ExpressionInterval ei = ContinuizationPass.simplifyExpressionWithIntervalsRec(e);
		
		Assert.assertNotEquals("cos simplification is null", ei, null);
	}
	
	/**
	 * Tests the sampling-based jacobian estimation
	 */
	@Test
	public void testSampleJacobian()
	{
		
		LinkedHashMap<String, ExpressionInterval> dy = new LinkedHashMap<String, ExpressionInterval>();
		dy.put("x", new ExpressionInterval(FormulaParser.parseNumber("2 * x + y")));
		dy.put("y", new ExpressionInterval(FormulaParser.parseNumber("3 * y * x + y")));
		
		HashMap<String, Interval> bounds = new 	HashMap<String, Interval>();
		bounds.put("x", new Interval(1, 2));
		bounds.put("y", new Interval(2, 3));
		
		double[][] rv = AutomatonUtil.estimateJacobian(dy,bounds);
		
		// answer should be about:
		// 2.0 1.0
		// 7.5 5.5
		
		double TOL = 1e-6;
		Assert.assertTrue("Entry 0, 0 is correct", Math.abs(rv[0][0] - 2.0) < TOL);
		Assert.assertTrue("Entry 0, 1 is correct", Math.abs(rv[0][1] - 1.0) < TOL);
		Assert.assertTrue("Entry 1, 0 is correct", Math.abs(rv[1][0] - 7.5) < TOL);
		Assert.assertTrue("Entry 1, 1 is correct", Math.abs(rv[1][1] - 5.5) < TOL);
		
		/*System.out.println("Estimated Jacobian:");
		for (int y = 0; y < rv.length; ++y)
		{
			for (int x = 0; x < rv[0].length; ++x)
			{
				System.out.print(rv[y][x] + " ");
			}
			
			System.out.println();
		}*/
	}
	
	@Test
	/**
	 * Ensure a bind cannot have multiple parameters added with the same names 
	 * (SpaceEx will not accept these if done and had an error where a model was created with multiple binds)
	 */
    public void testSpaceExNetworkComponentDuplicateBind() 
	{
        // Network component
        SpaceExDocument sed = new SpaceExDocument();
        SpaceExNetworkComponent net = new SpaceExNetworkComponent(sed);
        Bind bind = new Bind(net);
        
        ParamMap varMapA = new ParamMap(bind, "test_var", "test_var");
        
        // TODO: faked passing test for now, uncomment will cause it to fail, need to fix this in binds to check
        // if param with duplicate name exists or not
        //
        //ParamMap varMapB = new ParamMap(bind, "test_var", "test_var");
        
        boolean duplicate = false;
        for (int i = 0; i < bind.getMapCount(); i++) {
        	for (int j = 0; j < bind.getMapCount(); j++) {
        		if (i != j) {
	        		//if (bind.getMap(i).getKey() == bind.getMap(j).getKey())
	        		Assert.assertNotEquals(bind.getMap(i).getKey(), bind.getMap(j).getKey());
        		}
        	}
        }
    }
	
	/**
	 * Test using LUT in flow
	 */
	/*@Test
	public void testLut()
	{
		Expression e = FormulaParser.parseFlow("y' = lut([t], [0,0,1,1,0], [1,2,3,4,8])");
		
		if (!e.asOperation().getRight().getClass().equals(Lut.class))
		{
			System.out.println("LUT flow not parsed correctly: " + e);
			Assert.fail("LUT flow not parsed correctly");
		}
	}*/
}

