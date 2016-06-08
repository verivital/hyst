package com.verivital.hyst.main;

import com.verivital.hyst.passes.TransformationPass;

/**
 * Container class for a transformation pass to run, along with the params to use
 * 
 * @author Stanley Bak
 *
 */
public class RequestedTransformationPass
{
	public TransformationPass tp;
	public String params;

	public RequestedTransformationPass(TransformationPass tp, String params)
	{
		this.tp = tp;
		this.params = params;
	}
}
