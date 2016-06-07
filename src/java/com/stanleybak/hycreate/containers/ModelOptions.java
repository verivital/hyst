package com.stanleybak.hycreate.containers;

/**
 * Saveable options specific to each model file
 * 
 * @author Stanley Bak
 *
 */
public class ModelOptions implements GenericOptions
{
	private String reachabilityTime = "0";
	private String timeStep = "0.01";

	private boolean saveReachableFile = false;
	private boolean saveInitialStates = false;

	private ModelPlotOptions plotOptions = new ModelPlotOptions();

	private boolean detectMinMaxErrors = false;

	public enum AggregationMethod
	{
		NONE, CONVEX_HULL, DISCRETIZED_HULL, CONDITIONAL_DISCRETIZED_HULL,
	};

	public enum ReachabilityMethod
	{
		MFL_RECONSTRUCT,
	};

	private AggregationMethod successorAggregationMethod = AggregationMethod.CONDITIONAL_DISCRETIZED_HULL;

	private ReachabilityMethod reachabilityMethod = ReachabilityMethod.MFL_RECONSTRUCT;

	private boolean splitLargeRectangles = true;

	private int maxRectangles = 500;

	private ModelSimulationOptions simulationOptions = new ModelSimulationOptions();

	private boolean usePseudoInvariants = true;

	private static final String REACHABILITY_TIME_LABEL = "Reachability Time (0 = no limit)";

	private static final String DETECT_MIN_MAX_ERRORS_LABEL = "Attempt to Detect Min/Max Errors (Reduces Performance)";

	private static final String PARTIAL_OUTPUT_MS_LABEL = "Partial Output Time (ms)";

	private static final String MAX_RECTANGLES_HELP = "The maximum number of rectangles to track. This is only enforced before deciding\n"
			+ "whether or not to split tracked rectangles, so that the count can go slightly above\n"
			+ "this value. Larger values will be more accurate, but may be slower. 0 means no limit.";

	private static final String TIME_STEP_HELP = "The desired time step per face-lifting operation. This is an estimate, and\n"
			+ "the actual time step will usually be near this value. This can be used to\n"
			+ "tune the run time and accuracy of the computation.";

	private static final String DETECT_MIN_MAX_ERRORS_HELP = "Each model needs to have a function which can provide the min/max points\n"
			+ "for the dynamics in each dimension. This option will, at runtime, oversample the\n"
			+ "derivative function to double-check if the min/max function was entered correctly.\n"
			+ "Although it can detect errors, it is not a proof of the absence of errors.";

	private static final String SUCCESSORS_AGGREGATION_HELP = "The method to use when computing discrete successors. Doing aggregation may \n"
			+ "increase error, but will likely significantly reduce computation time and\n"
			+ "memory usage for systems with multiple discrete modes. Conditional discretized hull\n"
			+ "will take minimum number of boxes of no aggregation or discretized hull aggregation.";

	private static final String REACHABILITY_METHOD_HELP = "The core reachability method to use. \n"
			+ "Reconstruct MLF - Mixed face lifting, but will reconstruct neighborhoods based on the\n"
			+ "reach time estimate at each step, until the neighborhood-crossing-time is close to the\n"
			+ "desired step size.";

	public ModelOptions()
	{
	}

	public String guiTitle()
	{
		return "Model Options";
	}

	public String getReachabilityTime()
	{
		return reachabilityTime;
	}

	public String getReachabilityTimeLabel()
	{
		return REACHABILITY_TIME_LABEL;
	}

	public void setReachabilityTime(String reachabilityTime)
	{
		this.reachabilityTime = reachabilityTime;
	}

	public String getTimeStep()
	{
		return timeStep;
	}

	public void setTimeStep(String timeStep)
	{
		this.timeStep = timeStep;
	}

	public String getTimeStepHelp()
	{
		return TIME_STEP_HELP;
	}

	public boolean isSplitLargeRectangles()
	{
		return splitLargeRectangles;
	}

	public void setSplitLargeRectangles(boolean splitLargeRectangles)
	{
		this.splitLargeRectangles = splitLargeRectangles;
	}

	public int getMaxRectangles()
	{
		return maxRectangles;
	}

	public int[] getMaxRectanglesRange()
	{
		return new int[] { 0, Integer.MAX_VALUE };
	}

	public String getMaxRectanglesHelp()
	{
		return MAX_RECTANGLES_HELP;
	}

	public void setMaxRectangles(int maxRectangles)
	{
		this.maxRectangles = maxRectangles;
	}

	public boolean isUsePseudoInvariants()
	{
		return usePseudoInvariants;
	}

	public void setUsePseudoInvariants(boolean usePseudoInvariants)
	{
		this.usePseudoInvariants = usePseudoInvariants;
	}

	public String getUsePseudoInvariantsHelp()
	{
		return "If set, pseudo-invariants will be constructed based on the simulation of the dynamics,\n"
				+ "at orthogonal directions. They will be used if the number of tracked rectangles exceeds\n"
				+ "the Max Rectangles bound setting.";
	}

	public boolean isDetectMinMaxErrors()
	{
		return detectMinMaxErrors;
	}

	public void setDetectMinMaxErrors(boolean detectMinMaxErrors)
	{
		this.detectMinMaxErrors = detectMinMaxErrors;
	}

	public boolean isSaveReachableFile()
	{
		return saveReachableFile;
	}

	public void setSaveReachableFile(boolean saveReachableFile)
	{
		this.saveReachableFile = saveReachableFile;
	}

	public String getPartialOutputMsLabel()
	{
		return PARTIAL_OUTPUT_MS_LABEL;
	}

	public int[] getPartialOutputMsRange()
	{
		return new int[] { 0, 10000 };
	}

	public String getDetectMinMaxErrorsLabel()
	{
		return DETECT_MIN_MAX_ERRORS_LABEL;
	}

	public String getDetectMinMaxErrorsHelp()
	{
		return DETECT_MIN_MAX_ERRORS_HELP;
	}

	public boolean isSaveInitialStates()
	{
		return saveInitialStates;
	}

	public void setSaveInitialStates(boolean saveInitialStates)
	{
		this.saveInitialStates = saveInitialStates;
	}

	public String getSaveInitialStatesLabel()
	{
		return "Save Initial States Before Each Continuous Successor Computation";
	}

	public String getSaveInitialStatesHelp()
	{
		return "If set, this will save the initial states before computing continuous\n"
				+ "successors in each discrete mode. This is useful if you want to isolate\n"
				+ "one specific continuous successors operation.";
	}

	public AggregationMethod getSuccessorAggregationMethod()
	{
		return successorAggregationMethod;
	}

	public String getSuccessorAggregationMethodHelp()
	{
		return SUCCESSORS_AGGREGATION_HELP;
	}

	public void setSuccessorAggregationMethod(AggregationMethod successorAggregationMethod)
	{
		this.successorAggregationMethod = successorAggregationMethod;
	}

	public ReachabilityMethod getReachabilityMethod()
	{
		return reachabilityMethod;
	}

	public void setReachabilityMethod(ReachabilityMethod reachabilityMethod)
	{
		this.reachabilityMethod = reachabilityMethod;
	}

	public String getReachabilityMethodHelp()
	{
		return REACHABILITY_METHOD_HELP;
	}

	public ModelPlotOptions getPlotOptions()
	{
		return plotOptions;
	}

	public void setPlotOptions(ModelPlotOptions plotOptions)
	{
		this.plotOptions = plotOptions;
	}

	public ModelSimulationOptions getSimulationOptions()
	{
		return simulationOptions;
	}

	public void setSimulationOptions(ModelSimulationOptions simulationOptions)
	{
		this.simulationOptions = simulationOptions;
	}
}
