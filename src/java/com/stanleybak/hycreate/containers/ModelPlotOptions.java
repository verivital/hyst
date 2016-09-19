package com.stanleybak.hycreate.containers;

public class ModelPlotOptions implements GenericOptions
{
	public static final int PLOT_SIZE_MIN = 10;
	public static final int PLOT_SIZE_MAX = 5000;

	private String plotTitle = "";
	private String xAxisLabel = "";
	private String yAxisLabel = "";

	private int plotXDimensionIndex = 0;
	private int plotYDimensionIndex = 1;
	private String plotRange = "";
	private double plotBloatFactor = 1.25;

	private int plotWidth = 1000;
	private int plotHeight = 1000;

	private String plotModeColors = "green, blue, orange, light-grey, magenta";

	private boolean visualizeAfterComputation = true;
	private boolean visualizeDuringComputation = true;

	public boolean isVisualizeDuringComputation()
	{
		return visualizeDuringComputation;
	}

	public void setVisualizeDuringComputation(boolean visualizeDuringComputation)
	{
		this.visualizeDuringComputation = visualizeDuringComputation;
	}

	public boolean isVisualizeAfterComputation()
	{
		return visualizeAfterComputation;
	}

	public void setVisualizeAfterComputation(boolean visualizeAfterComputation)
	{
		this.visualizeAfterComputation = visualizeAfterComputation;
	}

	public String getPlotTitle()
	{
		return plotTitle;
	}

	public void setPlotTitle(String plotTitle)
	{
		this.plotTitle = plotTitle;
	}

	public String getxAxisLabel()
	{
		return xAxisLabel;
	}

	public void setxAxisLabel(String xAxisLabel)
	{
		this.xAxisLabel = xAxisLabel;
	}

	public String getyAxisLabel()
	{
		return yAxisLabel;
	}

	public void setyAxisLabel(String yAxisLabel)
	{
		this.yAxisLabel = yAxisLabel;
	}

	public int getPlotXDimensionIndex()
	{
		return plotXDimensionIndex;
	}

	public int[] getPlotXDimensionIndexRange()
	{
		return new int[] { 0, Integer.MAX_VALUE };
	}

	public void setPlotXDimensionIndex(int plotXDimensionIndex)
	{
		this.plotXDimensionIndex = plotXDimensionIndex;
	}

	public int getPlotYDimensionIndex()
	{
		return plotYDimensionIndex;
	}

	public int[] getPlotYDimensionIndexRange()
	{
		return new int[] { 0, Integer.MAX_VALUE };
	}

	public void setPlotYDimensionIndex(int plotYDimensionIndex)
	{
		this.plotYDimensionIndex = plotYDimensionIndex;
	}

	public int getPlotWidth()
	{
		return plotWidth;
	}

	public int[] getPlotWidthRange()
	{
		return new int[] { PLOT_SIZE_MIN, PLOT_SIZE_MAX };
	}

	public void setPlotWidth(int plotWidth)
	{
		this.plotWidth = plotWidth;
	}

	public int getPlotHeight()
	{
		return plotHeight;
	}

	public void setPlotHeight(int plotHeight)
	{
		this.plotHeight = plotHeight;
	}

	public int[] getPlotHeightRange()
	{
		return new int[] { PLOT_SIZE_MIN, PLOT_SIZE_MAX };
	}

	public String getPlotRange()
	{
		return plotRange;
	}

	public void setPlotRange(String plotRange)
	{
		this.plotRange = plotRange;
	}

	public String getPlotRangeHelp()
	{
		return "The range recorded in the plot for the two dimensions you indicated. For example, \n"
				+ "if you have two dimensions, time and temperature, and you want to plot time from \n"
				+ "0 to 100, and temperature from 50 to 90, you would enter '0, 100 ; 50, 90', and \n"
				+ "be sure to select the corresponding values for 'Plot X/Y Dimension Index'. If you\n"
				+ "leave this blank or 'auto', a simulation will be performed to automatically determine\n"
				+ "the bounds for the plot.";
	}

	public double getPlotBloatFactor()
	{
		return plotBloatFactor;
	}

	public void setPlotBloatFactor(double plotBloatFactor)
	{
		this.plotBloatFactor = plotBloatFactor;
	}

	public String getPlotBloatFactorHelp()
	{
		return "When 'auto' plot range is used, a simulation is performed to determine the bounds. These\n"
				+ "bounds are then expanded by a bloat factor (1.0 exactly the bounds bound, larger numbers\n"
				+ "will zoomed out more).";
	}

	public String getPlotModeColors()
	{
		return plotModeColors;
	}

	public void setPlotModeColors(String plotModeColors)
	{
		this.plotModeColors = plotModeColors;
	}

	@Override
	public String guiTitle()
	{
		return "Plot Options";
	}
}
