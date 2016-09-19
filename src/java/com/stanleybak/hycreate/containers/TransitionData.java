package com.stanleybak.hycreate.containers;

public class TransitionData
{
	private String from = "";
	private String to = "";

	public static String DEFAULT_GUARD = "return false; // transition is disabled";

	public static String DEFAULT_RESET = "/* no reset action assignments */";

	private String guard = DEFAULT_GUARD;
	private String reset = DEFAULT_RESET;

	public TransitionData()
	{

	}

	public String getFrom()
	{
		return from;

	}

	public void setFrom(String from)
	{
		this.from = from;
	}

	public String getTo()
	{
		return to;
	}

	public void setTo(String to)
	{
		this.to = to;
	}

	public String getGuard()
	{
		return guard;
	}

	public void setGuard(String guard)
	{
		this.guard = guard;
	}

	public String getReset()
	{
		return reset;
	}

	public void setReset(String reset)
	{
		this.reset = reset;
	}

	public TransitionData(String from, String to)
	{
		this.from = from;
		this.to = to;
	}

	public TransitionData(TransitionData other)
	{
		from = other.from;
		to = other.to;
		guard = other.guard;
		reset = other.reset;
	}

	public String toString()
	{
		return from + " -> " + to;
	}
}
