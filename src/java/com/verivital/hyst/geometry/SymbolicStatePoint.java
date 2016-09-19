package com.verivital.hyst.geometry;

public class SymbolicStatePoint
{
	public SymbolicStatePoint()
	{
	}

	public SymbolicStatePoint(String mode, HyperPoint pt)
	{
		this.modeName = mode;
		this.hp = pt;
	}

	public String modeName;
	public HyperPoint hp;

	public String toString()
	{
		return "[SSP: " + modeName + " - " + hp + "]";
	}

}
