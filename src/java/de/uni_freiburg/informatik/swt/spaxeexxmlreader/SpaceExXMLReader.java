/**
 * 
 */
package de.uni_freiburg.informatik.swt.spaxeexxmlreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.BindMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.LabelParam;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Location;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Param;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamDynamics;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamType;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExBaseComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Transition;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.UIDimensions;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.UIPosition;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.UIWaypoints;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ValueMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.VariableParam;

/**
 * Load a SpaceExDocument from an XML file.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class SpaceExXMLReader
{

	private Document mXMLDocument;
	private FileReader mCFGFileReader;
	private SpaceExDocument mTarget;

	private boolean mPrintWarnings, mPrintErrors = true;

	/**
	 * Create a new XML Reader for the given XML Document
	 * 
	 * @param xmlDocument
	 */
	public SpaceExXMLReader(Document xmlDocument)
	{
		mXMLDocument = xmlDocument;
	}

	/**
	 * Create a new XML Reader for the given XML Document and config file reader
	 * 
	 * @param xmlDocument
	 * @param cfgReader
	 */
	public SpaceExXMLReader(Document xmlDocument, FileReader cfgReader)
	{
		mXMLDocument = xmlDocument;
		mCFGFileReader = cfgReader;
	}

	/**
	 * Create a new XML Reader for the given XML file
	 * 
	 * @param xmlFileName
	 * @param cfgFileName
	 *            Config file to go with the XML automaton
	 */
	public SpaceExXMLReader(String xmlFileName, String cfgFileName)
	{
		File xmlFile = new File(xmlFileName);
		if (xmlFile.isFile() && xmlFile.canRead())
		{
			try
			{
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				mXMLDocument = builder.parse(xmlFile);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
			throw new RuntimeException("xml file doesn't exist or cannot be read: " + xmlFileName);

		if (cfgFileName != null)
		{
			File cfgFile = new File(cfgFileName);
			if (cfgFile.isFile() && cfgFile.canRead())
			{
				try
				{
					mCFGFileReader = new FileReader(cfgFileName);
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Read the XML document
	 * 
	 * @return The SpaceEx document as described by the XML
	 */
	public SpaceExDocument read()
	{
		mTarget = new SpaceExDocument();
		if (mXMLDocument != null)
		{
			mXMLDocument.getDocumentElement().normalize();
			parseSSpaceEx(mXMLDocument.getDocumentElement());
		}
		else
		{
			printError("No XML source set.");
		}
		parseCFG();
		return mTarget;
	}

	public boolean getPrintWarnings()
	{
		return mPrintWarnings;
	}

	public void setPrintWarnings(Boolean printWarnings)
	{
		mPrintWarnings = printWarnings;
	}

	public boolean getPrintErrors()
	{
		return mPrintErrors;
	}

	public void setPrintErrors(boolean printErrors)
	{
		mPrintErrors = printErrors;
	}

	/**
	 * Parse the &lt;sspaceex&gt;-Element of the DOM
	 * 
	 * @param element
	 */
	private void parseSSpaceEx(Element element)
	{
		if (element.getTagName().equalsIgnoreCase("sspaceex"))
		{

			String math = element.getAttribute("math");
			mTarget.setMathFormat(math);

			String version = element.getAttribute("version");
			mTarget.setVersion(version);

			if (!math.equalsIgnoreCase("SpaceEx"))
			{
				printWarning("Unexpected MathFormat: " + math);
			}
			if (!version.equalsIgnoreCase("0.2"))
			{
				printWarning("Unexpected Version: " + version);
			}

			NodeList childNodes = element.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++)
			{
				Node child = childNodes.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE)
				{
					parseComponent((Element) child);
				}
				else if (child.getNodeType() != Node.TEXT_NODE)
				{
					printWarning("Unexpected node type of node " + child.getNodeName() + "; "
							+ child.getNodeValue());
				}
			}
		}
		else
		{
			printError("Invalid main element: " + element.getTagName());
		}
	}

	/**
	 * Parse a &lt;component&gt;-Element of the DOM
	 * 
	 * @param element
	 */
	private void parseComponent(Element element)
	{
		if (element.getTagName().equalsIgnoreCase("component"))
		{
			NodeList params = element.getElementsByTagName("param");
			NodeList binds = element.getElementsByTagName("bind");

			SpaceExComponent component;
			if (binds.getLength() > 0)
			{
				// Binds -> Network Component
				SpaceExNetworkComponent netComponent = new SpaceExNetworkComponent(mTarget);
				component = netComponent;

				parseBinds(binds, netComponent);
			}
			else
			{
				// No Binds -> Base Component with Locations & Transitions
				NodeList locations = element.getElementsByTagName("location");
				NodeList transitions = element.getElementsByTagName("transition");

				SpaceExBaseComponent baseComponent = new SpaceExBaseComponent(mTarget);
				component = baseComponent;

				parseLocations(locations, baseComponent);
				parseTransitions(transitions, baseComponent);
			}
			parseParams(params, component);

			component.setID(element.getAttribute("id"));
			component.setNote(parseNote(element));
		}
		else
		{
			printError("Invalid Component element: " + element.getTagName());
		}
	}

	/**
	 * Parse all &lt;param&gt;-Elements of a Component
	 * 
	 * @param paramList
	 * @param parentComponent
	 */
	private void parseParams(NodeList paramList, SpaceExComponent parentComponent)
	{
		for (int i = 0; i < paramList.getLength(); i++)
		{
			Node paramNode = paramList.item(i);

			if (paramNode.getNodeName().equalsIgnoreCase("param")
					&& paramNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element paramElement = (Element) paramNode;

				String typeStr = paramElement.getAttribute("type");
				String typeStrLower = typeStr.toLowerCase().trim();
				ParamType type;
				if (typeStrLower.equals("label"))
					type = ParamType.LABEL;
				else if (typeStrLower.equals("int"))
					type = ParamType.INT;
				else if (typeStrLower.equals("real"))
					type = ParamType.REAL;
				else
				{
					type = ParamType.LABEL;
					printError("Invalid Param type: " + typeStr);
				}

				Param param;

				if (type == ParamType.LABEL)
				{
					param = new LabelParam(parentComponent);
				}
				else
				{
					VariableParam varParam = new VariableParam(parentComponent);
					param = varParam;
					varParam.setType(type);

					int dim = parseInt(paramElement, "d1", -1);
					if (dim > 0)
						varParam.setDimensionSize(1, dim);
					else
						varParam.setDimensionSize(1, paramElement.getAttribute("d1"));
					dim = parseInt(paramElement, "d2", -1);
					if (dim > 0)
						varParam.setDimensionSize(2, dim);
					else
						varParam.setDimensionSize(2, paramElement.getAttribute("d2"));

					String dynamicsStr = paramElement.getAttribute("dynamics");
					String dynamicsStrLower = dynamicsStr.toLowerCase().trim();
					ParamDynamics dynamics;
					if (dynamicsStrLower.equals("any"))
						dynamics = ParamDynamics.ANY;
					else if (dynamicsStrLower.equals("const"))
						dynamics = ParamDynamics.CONST;
					else if (dynamicsStrLower.equals("explicit"))
						dynamics = ParamDynamics.EXPLICIT;
					else
					{
						dynamics = ParamDynamics.ANY;
						printError("Invalid Param dynamics: " + dynamicsStr);
					}
					varParam.setDynamics(dynamics);

					if (paramElement.hasAttribute("controlled"))
					{
						varParam.setControlled(
								paramElement.getAttribute("controlled").equalsIgnoreCase("true"));
					}
				}
				param.setName(paramElement.getAttribute("name"));

				param.setLocal(paramElement.getAttribute("local").equalsIgnoreCase("true"));

				param.setNote(parseNote(paramElement));
			}
			else
			{
				printError("Invalid Param node: " + paramNode.getNodeName());
			}
		}
	}

	/**
	 * Parse all &lt;bind&gt;-Elements of a Component
	 * 
	 * @param bindList
	 * @param parentComponent
	 */
	private void parseBinds(NodeList bindList, SpaceExNetworkComponent parentComponent)
	{
		for (int i = 0; i < bindList.getLength(); i++)
		{
			Node bindNode = bindList.item(i);

			if (bindNode.getNodeName().equalsIgnoreCase("bind")
					&& bindNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element bindElement = (Element) bindNode;

				Bind bind = new Bind(parentComponent);

				bind.setAs(bindElement.getAttribute("as"));
				bind.setComponent(bindElement.getAttribute("component"));

				bind.setNote(parseNote(bindElement));

				UIPosition position = parsePosition(bindElement);
				if (position != null)
					bind.setPosition(position);
				UIDimensions dimensions = parseDimensions(bindElement);
				if (dimensions != null)
					bind.setDimensions(dimensions);

				NodeList mapList = bindElement.getElementsByTagName("map");
				parseMaps(mapList, bind);
			}
			else
			{
				printError("Invalid Bind node: " + bindNode.getNodeName());
			}
		}
	}

	/**
	 * Parse all &lt;map&gt;-Elements of a Component
	 * 
	 * @param mapList
	 * @param parentComponent
	 */
	private void parseMaps(NodeList mapList, Bind parentBind)
	{
		for (int i = 0; i < mapList.getLength(); i++)
		{
			Node mapNode = mapList.item(i);

			if (mapNode.getNodeName().equalsIgnoreCase("map")
					&& mapNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element mapElement = (Element) mapNode;

				// old version doesn't work with <link> attributes produced in
				// the maps of the new space ex model editor
				// String content = mapElement.getTextContent().trim();

				// this new version works with the <link> attributes produced in
				// the maps of the new space ex model editor
				String content = "";
				for (Node n = mapElement.getFirstChild(); n != null; n = n.getNextSibling())
				{
					if (n.getNodeType() == Node.TEXT_NODE)
						content += n.getTextContent();
				}

				content = content.trim();

				String contentStart = content.substring(0, 1);

				/*
				 * We need to check if we have a Param reference or a list of concrete values.
				 */
				Pattern regex = Pattern.compile("^[a-zA-Z_]");

				BindMap map;
				if (regex.matcher(contentStart).find())
				{
					// content is a Param name
					ParamMap paramMap = new ParamMap(parentBind);
					map = paramMap;
					paramMap.setParamReference(content);

				}
				else
				{
					// content is a sequence of values
					ValueMap valueMap = new ValueMap(parentBind);
					map = valueMap;

					String[] values = content.split(" ");
					double currentValue;
					boolean gotValue;
					for (int j = 0; j < values.length; j++)
					{
						if (values[j] != "")
						{
							try
							{
								currentValue = Double.parseDouble(values[j]);
								gotValue = true;
							}
							catch (Exception e)
							{
								currentValue = 0.0;
								gotValue = false;
							}
							if (gotValue)
								valueMap.addValue(currentValue);
						}
					}
				}
				map.setKey(mapElement.getAttribute("key"));
			}
			else
			{
				printError("Invalid Map node: " + mapNode.getNodeName());
			}
		}
	}

	/**
	 * Parse all &lt;location&gt;-Elements of a Component
	 * 
	 * @param locationList
	 * @param parentComponent
	 */
	private void parseLocations(NodeList locationList, SpaceExBaseComponent parentComponent)
	{
		for (int i = 0; i < locationList.getLength(); i++)
		{
			Node locationNode = locationList.item(i);

			if (locationNode.getNodeName().equalsIgnoreCase("location")
					&& locationNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element locationElement = (Element) locationNode;

				Location location = new Location(parentComponent);

				int id = parseInt(locationElement, "id", -1);
				if (id < 0)
					printError("Unable to parse Location ID");
				location.setId(id);
				location.setName(locationElement.getAttribute("name"));

				Expression invariant = parseFormula(locationElement, "invariant");
				if (invariant != null)
					location.setInvariant(invariant);

				Expression flow = parseFormula(locationElement, "flow");
				if (flow != null)
					location.setFlow(flow);

				location.setNote(parseNote(locationElement));

				UIPosition position = parsePosition(locationElement);
				if (position != null)
					location.setPosition(position);
				UIDimensions dimensions = parseDimensions(locationElement);
				if (dimensions != null)
					location.setDimensions(dimensions);
			}
			else
			{
				printError("Invalid Location node: " + locationNode.getNodeName());
			}
		}
	}

	/**
	 * Parse all &lt;transition&gt;-Elements of a Component
	 * 
	 * @param transitionList
	 * @param parentComponent
	 */
	private void parseTransitions(NodeList transitionList, SpaceExBaseComponent parentComponent)
	{
		for (int i = 0; i < transitionList.getLength(); i++)
		{
			Node transitionNode = transitionList.item(i);

			if (transitionNode.getNodeName().equalsIgnoreCase("transition")
					&& transitionNode.getNodeType() == Node.ELEMENT_NODE)
			{
				Element transitionElement = (Element) transitionNode;

				Transition transition = new Transition(parentComponent);

				Node labelNode = transitionElement.getElementsByTagName("label").item(0);
				if (labelNode != null)
					transition.setLabel(labelNode.getTextContent());

				int sourceId = parseInt(transitionElement, "source", -1);
				if (sourceId < 0)
					printError("Unable to parse Transition source ID");
				transition.setSource(sourceId);
				int targetId = parseInt(transitionElement, "target", -1);
				if (targetId < 0)
					printError("Unable to parse Transition target ID");
				transition.setTarget(targetId);

				if (transitionElement.hasAttribute("asap"))
				{
					transition.setAsap(
							transitionElement.getAttribute("asap").equalsIgnoreCase("true"));
				}
				if (transitionElement.hasAttribute("timedriven"))
				{
					transition.setTimeDriven(
							transitionElement.getAttribute("timedriven").equalsIgnoreCase("true"));
				}
				if (transitionElement.hasAttribute("bezier"))
				{
					transition.setBezier(
							transitionElement.getAttribute("bezier").equalsIgnoreCase("true"));
				}

				if (transitionElement.hasAttribute("priority"))
				{
					int priority = parseInt(transitionElement, "priority", -1);
					if (priority < 0)
						printError("Unable to parse Transition priority");
					transition.setPriority(priority);
				}

				Expression guard = parseFormula(transitionElement, "guard");
				if (guard != null)
					transition.setGuard(guard);

				Expression assignment = parseFormula(transitionElement, "assignment");
				if (assignment != null)
					transition.setAssignment(assignment);

				transition.setNote(parseNote(transitionElement));

				Node labelPositionNode = transitionElement.getElementsByTagName("labelposition")
						.item(0);
				if (labelPositionNode != null)
				{
					UIPosition position = parsePosition((Element) labelPositionNode);
					if (position != null)
						transition.setLabelPosition(position);
					UIDimensions dimensions = parseDimensions((Element) labelPositionNode);
					if (dimensions != null)
						transition.setLabelDimensions(dimensions);
				}

				Node middlePointNode = transitionElement.getElementsByTagName("middlepoint")
						.item(0);
				if (middlePointNode != null)
				{
					UIPosition position = parsePosition((Element) middlePointNode);
					if (position != null)
						transition.setMiddlepointPosition(position);
				}

				Node waypointsNode = transitionElement.getElementsByTagName("waypoints").item(0);
				if (waypointsNode != null)
					parseWaypoints(waypointsNode, transition);
			}
			else
			{
				printError("Invalid Transition node: " + transitionNode.getNodeName());
			}
		}
	}

	/**
	 * Checks if a DOM Element has a &lt;note&gt; child Element and returns its text content if any.
	 * 
	 * @param element
	 *            The Element to examine (PARENT of the &lt;note&gt; node!)
	 * @return The note's text or "" if no &lt;note&gt; is present
	 */
	private String parseNote(Element element)
	{
		if (element.hasChildNodes())
		{
			NodeList noteNodes = element.getElementsByTagName("note");
			if (noteNodes.getLength() > 0)
			{
				return noteNodes.item(0).getTextContent();
			}
		}
		return "";
	}

	/**
	 * Checks if a DOM Element has "x" and "y" attributes and transforms them into a UIPosition if
	 * they're present.
	 * 
	 * @param element
	 *            The element which's position to check
	 * @return A new UIPosition of (x, y) or null if "x" and "y" are not given
	 */
	private UIPosition parsePosition(Element element)
	{
		if (element.hasAttribute("x") && element.hasAttribute("y"))
		{
			double x;
			try
			{
				x = Double.parseDouble(element.getAttribute("x"));
			}
			catch (Exception e)
			{
				x = 0.0;
			}
			double y;
			try
			{
				y = Double.parseDouble(element.getAttribute("y"));
			}
			catch (Exception e)
			{
				y = 0.0;
			}
			return new UIPosition(x, y);
		}
		return null;
	}

	/**
	 * Checks if a DOM Element has "x" and "y" attributes and transforms them into a UIPosition if
	 * they're present.
	 * 
	 * @param element
	 *            The element which's position to check
	 * @return A new UIPosition of (x, y) or null if "x" and "y" are not given
	 */
	private UIDimensions parseDimensions(Element element)
	{
		if (element.hasAttribute("width") && element.hasAttribute("height"))
		{
			double width;
			try
			{
				width = Double.parseDouble(element.getAttribute("width"));
			}
			catch (Exception e)
			{
				width = 0.0;
			}
			double height;
			try
			{
				height = Double.parseDouble(element.getAttribute("height"));
			}
			catch (Exception e)
			{
				height = 0.0;
			}
			return new UIDimensions(width, height);
		}
		return null;
	}

	/**
	 * Parses the Waypoints of a Transition
	 * 
	 * @param waypointsNode
	 * @param parentTransition
	 */
	private void parseWaypoints(Node waypointsNode, Transition parentTransition)
	{
		if (waypointsNode.getNodeName().equalsIgnoreCase("waypoints")
				&& waypointsNode.getNodeType() == Node.ELEMENT_NODE)
		{
			Element waypointsElement = (Element) waypointsNode;

			UIWaypoints waypoints = new UIWaypoints();

			Node beforeMiddleNode = waypointsElement.getElementsByTagName("beforemiddle").item(0);
			if ((beforeMiddleNode != null) && (beforeMiddleNode.getNodeType() == Node.ELEMENT_NODE))
			{
				parseWaypointsList((Element) beforeMiddleNode, waypoints, true);
			}
			Node afterMiddleNode = waypointsElement.getElementsByTagName("aftermiddle").item(0);
			if ((afterMiddleNode != null) && (afterMiddleNode.getNodeType() == Node.ELEMENT_NODE))
			{
				parseWaypointsList((Element) afterMiddleNode, waypoints, false);
			}

			parentTransition.setWaypoints(waypoints);
		}
		else
		{
			printError("Invalid Waypoints node: " + waypointsNode.getNodeName());
		}
	}

	/**
	 * Parse a list of waypoints from a &lt;beforemiddle&gt; or &lt;aftermiddle&gt; Node
	 * 
	 * @param source
	 * @param waypoints
	 * @param insertBefore
	 */
	private void parseWaypointsList(Element source, UIWaypoints waypoints, boolean insertBefore)
	{
		/*
		 * waypoints are given as a comma-separated list of REAL values, which use a dot as the
		 * decimal separator.
		 */
		String content = source.getTextContent().trim();
		String[] values = content.split(",");
		double currentValue, previousValue = 0.0;
		int valueCounter = 0;
		boolean gotValue;
		for (int i = 0; i < values.length; i++)
		{
			if (values[i] != "")
			{
				try
				{
					currentValue = Double.parseDouble(values[i].trim());
					gotValue = true;
				}
				catch (Exception e)
				{
					currentValue = 0.0;
					gotValue = false;
				}
				if (gotValue)
				{
					valueCounter++;
					if (valueCounter >= 2)
					{
						// we have two new values for a new waypoint
						UIPosition position = new UIPosition(previousValue, currentValue);
						waypoints.addWaypoint(position, insertBefore);
						// reset counter as the values have been used!
						valueCounter = 0;
					}
					else
					{
						// we only have a single new value: get the next one
						previousValue = currentValue;
					}
				}
			}
		}
	}

	private int parseInt(Element source, String attributeName, int defaultValue)
	{
		int result;
		try
		{
			result = Integer.parseInt(source.getAttribute(attributeName));
		}
		catch (Exception E)
		{
			result = defaultValue;
		}
		return result;
	}

	private Expression parseFormula(Element parentElement, String nodeName)
	{
		Expression rv = null;

		NodeList nodes = parentElement.getElementsByTagName(nodeName);
		if ((nodes != null) && (nodes.getLength() > 0))
		{
			String text = nodes.item(0).getTextContent();

			if (nodeName.equals("invariant"))
				rv = FormulaParser.parseInvariant(text);
			else if (nodeName.equals("assignment"))
				rv = FormulaParser.parseReset(text);
			else if (nodeName.equals("guard"))
				rv = FormulaParser.parseGuard(text);
			else if (nodeName.equals("flow"))
				rv = FormulaParser.parseFlow(text);
			else
				throw new AutomatonExportException("unknown node type: " + nodeName);
		}

		return rv;
	}

	/**
	 * Parse the config file and read all supported properties
	 */
	private void parseCFG()
	{
		if (mCFGFileReader != null)
		{
			String line;
			BufferedReader br = new BufferedReader(mCFGFileReader);
			try
			{
				while ((line = br.readLine()) != null)
				{
					// remove comments from string
					int commentPos = line.indexOf("#");
					if (commentPos != -1)
						line = line.substring(0, commentPos);

					int eqPos = line.indexOf("=");
					if (eqPos > 0)
					{
						String property = line.substring(0, eqPos).trim().toLowerCase();

						String value = line.substring(eqPos + 1);

						int quoteIndex = value.indexOf("\"");

						// there was an open quote... but no end quote
						if (quoteIndex != -1 && value.indexOf("\"", quoteIndex + 1) == -1)
						{
							// keep reading lines until the end quote
							while ((line = br.readLine()) != null)
							{
								commentPos = line.indexOf("#"); // trim comments
								if (commentPos > 0)
									line = line.substring(0, commentPos - 1);

								value += " " + line;

								quoteIndex = line.indexOf("\"");

								if (quoteIndex != -1)
									break;
							}

							if (quoteIndex == -1)
								throw new AutomatonExportException(
										"Quoted multi-line property in .cfg file did not have "
												+ "end quote: " + property);
						}

						value = value.trim().replace("\"", "");

						if (property.equals("system"))
							mTarget.setSystemID(value);
						else if (property.equals("time-horizon"))
						{
							double th = Double.parseDouble(value);

							mTarget.setTimeHorizon(th);
						}
						else if (property.equals("sampling-time"))
						{
							double st = Double.parseDouble(value);

							mTarget.setSamplingTime(st);
						}
						else if (property.equals("flowpipe-tolerance"))
						{
							double tol = Double.parseDouble(value);

							mTarget.setFlowpipeTolerance(tol);
						}
						else if (property.equals("iter-max"))
						{
							int im = Integer.parseInt(value);

							mTarget.setMaxIterations(im);
						}
						else if (property.equals("map-zero-duration-jump-sets"))
						{
							mTarget.setTimeTriggered(value.equals("true"));
						}
						else if (property.equals("initially"))
						{
							Expression initialStates = FormulaParser.parseInitialForbidden(value);

							mTarget.setInitialStateConditions(initialStates);
						}
						else if (property.equals("forbidden") && value.trim().length() > 0)
						{
							Expression forbiddenStates = FormulaParser.parseInitialForbidden(value);

							mTarget.setForbiddenStateConditions(forbiddenStates);
						}
						else if (property.equals("output-variables"))
						{
							String[] varNames = value.split(",");
							for (int i = 0; i < varNames.length; i++)
								mTarget.addOutputVar(varNames[i].trim());
						}
						else if (property.equals("output-format"))
						{
							mTarget.setOutputFormat(value);
						}
						else if (property.equals("scenario"))
						{
							mTarget.setScenario(value);
						}
						else if (property.equals("directions"))
						{
							mTarget.setDirections(value);
						}
						else if (property.equals("set-aggregation"))
						{
							mTarget.setAggregation(value);
						}
					}
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void printError(String message)
	{
		if (mPrintErrors)
			System.err.println("[SX2B] Error: " + message);

		throw new AutomatonExportException("Error while reading model: " + message);
	}

	private void printWarning(String message)
	{
		if (mPrintWarnings)
			System.out.println("[SX2B] Warning: " + message);

		throw new AutomatonExportException("Warning while reading model: " + message);
	}

}
