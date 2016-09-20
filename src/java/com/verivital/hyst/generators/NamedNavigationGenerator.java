package com.verivital.hyst.generators;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;

/**
 * Creates the NAV benchmark, from "Benchmarks for Hybrid Systems Verification", Fehnker et. al,
 * HSCC 2004
 * 
 * This one creates benchmark from names (the standard instances). Valid names are "nav01", "nav02",
 * ... as well as "fig1" and "fig2" (corresponding to the figures in the paper)
 * 
 * @author Stanley Bak (Sept 2016)
 *
 */
public class NamedNavigationGenerator extends ModelGenerator
{
	@Option(name = "-name", required = true, usage = "named instance, like 'nav01'", metaVar = "NAME")
	String name;

	// set with loadBenchmarkParams()
	private String[] map;
	private double[] matA;
	private Interval[] x0;
	private Interval[] v0;

	@Override
	public String getCommandLineFlag()
	{
		return "named_nav";
	}

	@Override
	public String getName()
	{
		return "Standard-Instance Navigation [Fehnker06]";
	}

	@Override
	protected Configuration generateModel()
	{
		loadBenchmarkParams();

		NavigationGenerator navGen = new NavigationGenerator();

		String param = NavigationGenerator.makeParamString(map, matA, x0, v0);

		return navGen.generate(param);
	}

	private void loadBenchmarkParams()
	{
		if (name.equals("nav01"))
		{
			map = new String[] { "B", "2", "4", "4", "3", "4", "2", "2", "A" };
			matA = new double[] { -1.2, 0.1, 0.1, -1.2 };
			x0 = new Interval[] { new Interval(2, 3), new Interval(1, 2) };
			v0 = new Interval[] { new Interval(-0.3, 0.3), new Interval(-0.3, 0) };
		}
		else if (name.equals("nav02"))
		{
			map = new String[] { "B", "2", "4", "2", "3", "4", "2", "2", "A" };
			matA = new double[] { -1.2, 0.1, 0.1, -1.2 };
			x0 = new Interval[] { new Interval(2, 3), new Interval(1, 2) };
			v0 = new Interval[] { new Interval(-0.3, 0.3), new Interval(-0.3, 0.3) };
		}
		else if (name.equals("nav03"))
		{
			map = new String[] { "B", "2", "4", "2", "3", "4", "2", "2", "A" };
			matA = new double[] { -1.2, 0.1, 0.1, -1.2 };
			x0 = new Interval[] { new Interval(2, 3), new Interval(1, 2) };
			v0 = new Interval[] { new Interval(-0.4, 0.4), new Interval(-0.4, 0.4) };
		}
		else
			throw new AutomatonExportException(
					"Unrecognized standard-instance benchmark name: '" + name + "'");
	}
}
