package com.verivital.hyst.passes.basic;

import org.kohsuke.args4j.Option;

import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.network.NetworkComponent;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.util.Preconditions;
import com.verivital.hyst.util.Preconditions.PreconditionsFailedException;

/**
 * A model transformation pass which copies a base-component instance in a
 * network model
 * 
 * @author Stanley Bak (September 2015)
 *
 */
public class CopyInstancePass extends TransformationPass
{
	@Option(name = "-instance", aliases = {
			"-name" }, required = false, usage = "name of instance to copy", metaVar = "NAME")
	private String ignoreVar;

	@Option(name = "-number", aliases = {
			"-n" }, required = false, usage = "the number of copies at the end (1 = no copy)", metaVar = "NUM")
	private int num = 2;

	public CopyInstancePass()
	{
		// skip all checks and conversions
		this.preconditions = new Preconditions(true);
	}

	@Override
	public String getName()
	{
		return "Copy Base Component Instance Pass";
	}

	@Override
	public String getCommandLineFlag()
	{
		return "copy";
	}

	@Override
	protected void checkPreconditons(Configuration c, String name)
	{
		super.checkPreconditons(c, name);

		if (!(c.root instanceof NetworkComponent))
			throw new PreconditionsFailedException("Root must be a network component");

	}

	@Override
	protected void runPass()
	{
		NetworkComponent root = (NetworkComponent) config.root;

		// TODO WORKING HERE
	}
}
