package com.stanleybak.hycreate.containers;

public class ModelSimulationOptions implements GenericOptions
{
	private String simulationStep = "auto";

	public enum SimulationType
	{
		SIMULATION_AND_REACHABILITY, REACHABILITY_ONLY, SIMULATION_ONLY,
	}

	private SimulationType simulationType = SimulationType.SIMULATION_AND_REACHABILITY;

	public enum StartingPositionsType
	{
		MIDPOINTS, CORNERS, CORNERS_AND_MIDPOINTS,
	};

	private StartingPositionsType startingPositions = StartingPositionsType.MIDPOINTS;

	public enum EnumDerivativesType
	{
		AVERAGE_DERIVATIVE, AUTO_ENUMERATE, ENUMERATE_ALL,
	};

	private EnumDerivativesType enumerateDerivatives = EnumDerivativesType.AUTO_ENUMERATE;

	private boolean enumerateTransitions = false;

	private int maxDiscreteTransitions = 10000;

	private static final String ENUM_DERS_HELP = "Each simulation start point can have multiple derivatives (due to nondeterminism). This setting\n"
			+ "determines how this is handled. 'Auto Enumerate' attempts to detect which dimensions are\n"
			+ "nondeterministic and will enumerate all combinations of only those dimensions at each point.";

	private static final String ENUM_TRANSITIONS_HELP = "If set, each simulation will be run twice: once with greedy discrete transitions and once with\n"
			+ "lazy transitions. If not set, only greedy (as soon as possible) jumps will be simulated.\n"
			+ "Automata with only a single mode and no discrete transitions will never be enumerated.";

	@Override
	public String guiTitle()
	{
		return "Simulation Options";
	}

	public SimulationType getSimulationType()
	{
		return simulationType;
	}

	public void setSimulationType(SimulationType simulationType)
	{
		this.simulationType = simulationType;
	}

	public String getSimulationStep()
	{
		return simulationStep;
	}

	public void setSimulationStep(String simulationStep)
	{
		this.simulationStep = simulationStep;
	}

	public EnumDerivativesType getEnumerateDerivatives()
	{
		return enumerateDerivatives;
	}

	public void setEnumerateDerivatives(EnumDerivativesType enumerateDerivatives)
	{
		this.enumerateDerivatives = enumerateDerivatives;
	}

	public String getEnumerateDerivativesHelp()
	{
		return ENUM_DERS_HELP;
	}

	public boolean isEnumerateTransitions()
	{
		return enumerateTransitions;
	}

	public void setEnumerateTransitions(boolean enumerateTransitions)
	{
		this.enumerateTransitions = enumerateTransitions;
	}

	public String getEnumerateTransitionsHelp()
	{
		return ENUM_TRANSITIONS_HELP;
	}

	public StartingPositionsType getStartingPositions()
	{
		return startingPositions;
	}

	public void setStartingPositions(StartingPositionsType startingPositions)
	{
		this.startingPositions = startingPositions;
	}

	public int getMaxDiscreteTransitions()
	{
		return maxDiscreteTransitions;
	}

	public int[] getMaxDiscreteTransitionsRange()
	{
		return new int[] { 0, Integer.MAX_VALUE };
	}

	public void setMaxDiscreteTransitions(int maxDiscreteTransitions)
	{
		this.maxDiscreteTransitions = maxDiscreteTransitions;
	}

}
