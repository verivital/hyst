package com.verivital.hyst.junit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.verivital.hyst.generators.IntegralChainGenerator;
import com.verivital.hyst.generators.NavigationGenerator;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.printers.FlowPrinter;
import com.verivital.hyst.printers.ToolPrinter;
import com.verivital.hyst.python.PythonBridge;

/**
 * JUnit tests for model generators
 * 
 * @author sbak
 *
 */
@RunWith(Parameterized.class)
public class GeneratorTests 
{
	@Before
	public void setUpClass() 
	{
		Expression.expressionPrinter = null;
	}

	@Parameters
	public static Collection<Object[]> data() 
	{
		return Arrays.asList(new Object[][] { { false }, { true } });
	}

	public GeneratorTests(boolean block) 
	{
		PythonBridge.setBlockPython(block);
	}

	@Test
	public void testIntegralChain()
	{
		IntegralChainGenerator gen = new IntegralChainGenerator();
		
		Configuration c = gen.generate("-dims 6");
		
		Assert.assertEquals("six variables", 6, c.root.variables.size());
		
		ToolPrinter printer = new FlowPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");
		
		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}
	
	@Test
	public void testNav()
	{
		NavigationGenerator gen = new NavigationGenerator();
		
		String param = "-matrix -1.2 0.1 0.1 -1.2 -i_list 2 2 A 4 3 4 B 2 4 -width 3 "
				+ "-startx 0.5 -starty 1.5 -time 3.0 -noise 0.25";
		Configuration c = gen.generate(param);
		
		Assert.assertEquals("four variables", 4, c.root.variables.size());
		
		Entry<String, Expression> entry = c.init.entrySet().iterator().next();
		
		Assert.assertEquals("mode_0_1", entry.getKey());
		Assert.assertEquals("x = 0.5 & y = 1.5 & -1 <= xvel & xvel <= 1 & -1 <= yvel & yvel <= 1", 
				entry.getValue().toDefaultString());
		
		
		ToolPrinter printer = new FlowPrinter();
		printer.setOutputString();
		printer.print(c, "", "model.xml");
		
		String out = printer.outputString.toString();

		Assert.assertTrue("some output exists", out.length() > 10);
	}
}
