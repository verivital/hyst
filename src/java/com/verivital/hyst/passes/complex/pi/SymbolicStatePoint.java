package com.verivital.hyst.passes.complex.pi;

import com.verivital.hyst.geometry.HyperPoint;

public class SymbolicStatePoint
{
	public SymbolicStatePoint() {}
	
	public SymbolicStatePoint(String mode, HyperPoint pt)
	{
		this.modeName = mode;
		this.hp = pt;
	}
	
	public String modeName;
	public HyperPoint hp;
}
