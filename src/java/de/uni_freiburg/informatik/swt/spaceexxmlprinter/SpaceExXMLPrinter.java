/**
 * 
 */
package de.uni_freiburg.informatik.swt.spaceexxmlprinter;

import java.io.File;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.ir.AutomatonExportException;

import de.uni_freiburg.informatik.swt.sxhybridautomaton.Bind;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.BindMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Location;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Param;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ParamType;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExBaseComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExConfigValues;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExDocument;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.SpaceExNetworkComponent;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.Transition;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.UIDimensions;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.UIPosition;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.UIWaypoints;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.ValueMap;
import de.uni_freiburg.informatik.swt.sxhybridautomaton.VariableParam;

/**
 * Saves a SpaceEx hybrid automaton to a spaceex XML file.
 * 
 * @author Christopher Dillo (dilloc@informatik.uni-freiburg.de)
 *
 */
public class SpaceExXMLPrinter
{
	private SpaceExExpressionPrinter printer = new SpaceExExpressionPrinter();

	public enum FormulaType
	{
		DEFAULT, ASSIGNMENT, DIFFERENTIAL
	}

	private SpaceExDocument mSXDocument;
	private Document mXMLDocument;

	/**
	 * Initialize a new SpaceExXMLPrinter with a document, which will be translated into a
	 * SpaceEx-compatible XML DOM
	 * 
	 * @param document
	 */
	public SpaceExXMLPrinter(SpaceExDocument document)
	{
		mSXDocument = document;

		buildDOM();
	}

	/**
	 * Get the DOM of the converted XML document
	 * 
	 * @return
	 */
	public Document getDOM()
	{
		return mXMLDocument;
	}

	/**
	 * Saves the XML DOM to a file of the given name (and path)
	 * 
	 * @param filename
	 */
	public void saveXML(String filename)
	{
		if (mSXDocument == null)
			return;

		// save the DOM contents into an XML file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try
		{
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "iso-8859-1");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			DOMSource source = new DOMSource(mXMLDocument);
			StreamResult result = new StreamResult(new File(filename));

			transformer.transform(source, result);
		}
		catch (TransformerConfigurationException e)
		{
			e.printStackTrace();
		}
		catch (TransformerException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * return string of XML DOM
	 * 
	 */
	public String stringXML()
	{
		if (mSXDocument == null)
			return "";

		String str_result = "";

		// save the DOM contents into an XML file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer;
		try
		{
			transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "iso-8859-1");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			DOMSource source = new DOMSource(mXMLDocument);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);

			transformer.transform(source, result);

			str_result = writer.toString();
		}
		catch (TransformerConfigurationException e)
		{
			e.printStackTrace();
		}
		catch (TransformerException e)
		{
			e.printStackTrace();
		}
		return str_result;
	}

	/**
	 * Builds the DOM
	 */
	public void buildDOM()
	{
		if (mSXDocument != null)
		{
			DocumentBuilderFactory xmlFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder;
			try
			{
				xmlBuilder = xmlFactory.newDocumentBuilder();
				mXMLDocument = xmlBuilder.newDocument();

				buildDocument();
			}
			catch (ParserConfigurationException e)
			{
				e.printStackTrace();
			}

		}
	}

	/**
	 * Fills the DOM with data, including general information and the components
	 */
	public void buildDocument()
	{
		if (mXMLDocument == null)
			return;

		// write the root element: sspaceex, version, ...
		Element sspaceex = mXMLDocument.createElement("sspaceex");
		mXMLDocument.appendChild(sspaceex);

		sspaceex.setAttribute("xmlns", "http://www-verimag.imag.fr/xml-namespaces/sspaceex");
		sspaceex.setAttribute("version", mSXDocument.getVersion());
		sspaceex.setAttribute("math", mSXDocument.getMathFormat());

		// write all components
		for (int i = 0; i < mSXDocument.getComponentCount(); i++)
		{
			SpaceExComponent comp = mSXDocument.getComponent(i);
			buildComponent(comp, sspaceex);
		}
	}

	/**
	 * Add the data for a component to the DOM
	 * 
	 * @param component
	 *            Which component to add
	 * @param parentNode
	 *            Where to add the data
	 */
	public void buildComponent(SpaceExComponent component, Element parentNode)
	{
		if (component == null)
			return;

		// write the component element
		Element compElem = mXMLDocument.createElement("component");
		parentNode.appendChild(compElem);

		compElem.setAttribute("id", component.getID());

		buildNote(component.getNote(), compElem);

		// write all params
		for (int i = 0; i < component.getParamCount(); i++)
		{
			Param param = component.getParam(i);
			buildParam(param, compElem);
		}

		/*
		 * base component: locations & transitions network component: binds
		 */
		if (component instanceof SpaceExBaseComponent)
		{
			SpaceExBaseComponent baseComp = (SpaceExBaseComponent) component;

			// write all locations
			for (int i = 0; i < baseComp.getLocationCount(); i++)
			{
				Location loc = baseComp.getLocation(i);
				buildLocation(loc, compElem);
			}
			// write all transitions
			for (int i = 0; i < baseComp.getTransitionCount(); i++)
			{
				Transition transition = baseComp.getTransition(i);
				buildTransition(transition, compElem);
			}
		}
		else if (component instanceof SpaceExNetworkComponent)
		{
			SpaceExNetworkComponent netComp = (SpaceExNetworkComponent) component;

			// write all binds
			for (int i = 0; i < netComp.getBindCount(); i++)
			{
				Bind bind = netComp.getBind(i);
				buildBind(bind, compElem);
			}
		}
		else
		{
			printWarning("Unknown component type: " + component.getID());
		}
	}

	/**
	 * Add Params to the DOM
	 * 
	 * @param param
	 * @param parentNode
	 *            Node of the Param's Component
	 */
	public void buildParam(Param param, Element parentNode)
	{
		if (param == null)
			return;

		// write the param element
		Element paramElem = mXMLDocument.createElement("param");
		parentNode.appendChild(paramElem);

		paramElem.setAttribute("name", param.getName());

		switch (param.getType())
		{
		case INT:
			paramElem.setAttribute("type", "int");
			break;
		case REAL:
			paramElem.setAttribute("type", "real");
			break;
		case LABEL:
		default:
			paramElem.setAttribute("type", "label");
		}

		paramElem.setAttribute("local", param.getLocal() ? "true" : "false");

		buildNote(param.getNote(), paramElem);

		if (param instanceof VariableParam)
		{
			VariableParam varPar = (VariableParam) param;

			// d1:
			if (varPar.isDimensionSizeReferenced(1))
				paramElem.setAttribute("d1", varPar.getDimensionSizeReference(1));
			else
				paramElem.setAttribute("d1", intToString(varPar.getDimensionSize(1)));

			// d2:
			if (varPar.isDimensionSizeReferenced(2))
				paramElem.setAttribute("d2", varPar.getDimensionSizeReference(2));
			else
				paramElem.setAttribute("d2", intToString(varPar.getDimensionSize(2)));

			// dynamics
			switch (varPar.getDynamics())
			{
			case CONST:
				paramElem.setAttribute("dynamics", "const");
				break;
			case EXPLICIT:
				paramElem.setAttribute("dynamics", "explicit");
				break;
			case ANY:
			default:
				paramElem.setAttribute("dynamics", "any");
			}

			if (varPar.getControlled())
				paramElem.setAttribute("controlled", "true");
			else
				paramElem.setAttribute("controlled", "false");
		}
	}

	/**
	 * Add Locations to the DOM
	 * 
	 * @param loc
	 * @param parentNode
	 *            Node of the Location's Component
	 */
	public void buildLocation(Location loc, Element parentNode)
	{
		if (loc == null)
			return;

		// write the location element
		Element locElem = mXMLDocument.createElement("location");
		parentNode.appendChild(locElem);

		locElem.setAttribute("id", intToString(loc.getId()));
		locElem.setAttribute("name", loc.getName());

		buildPosition(loc.getPosition(), locElem);
		buildDimensions(loc.getDimensions(), locElem);

		try
		{
			buildExpression(loc.getInvariant(), locElem, "invariant");
		}
		catch (AutomatonExportException e)
		{
			throw new AutomatonExportException("Error exporting invariant: " + loc.getInvariant(),
					e);
		}

		buildExpression(loc.getFlow(), locElem, "flow", FormulaType.DIFFERENTIAL);

		buildNote(loc.getNote(), locElem);
	}

	/**
	 * Add Transitions to the DOM
	 * 
	 * @param transition
	 * @param parentNode
	 *            Node of the Transition's Component
	 */
	public void buildTransition(Transition transition, Element parentNode)
	{
		if (transition == null)
			return;

		// write the transition element
		Element transElem = mXMLDocument.createElement("transition");
		parentNode.appendChild(transElem);

		transElem.setAttribute("source", intToString(transition.getSource()));
		transElem.setAttribute("target", intToString(transition.getTarget()));

		transElem.setAttribute("bezier", transition.isBezier() ? "true" : "false");
		transElem.setAttribute("timedriven", transition.isTimeDriven() ? "true" : "false");
		transElem.setAttribute("asap", transition.isAsap() ? "true" : "false");

		buildText(transition.getLabel(), transElem, "label");

		buildExpression(transition.getGuard(), transElem, "guard");
		buildExpression(transition.getAssignment(), transElem, "assignment",
				FormulaType.ASSIGNMENT);

		// label position:
		Element labPosElem = mXMLDocument.createElement("labelposition");
		transElem.appendChild(labPosElem);
		buildPosition(transition.getLabelPosition(), labPosElem);
		buildDimensions(transition.getLabelDimensions(), labPosElem);

		// middle point
		Element midPosElem = mXMLDocument.createElement("middlepoint");
		transElem.appendChild(midPosElem);
		buildPosition(transition.getMiddlepointPosition(), midPosElem);

		buildWaypoints(transition.getWaypoints(), transElem);

		buildNote(transition.getNote(), transElem);
	}

	/**
	 * Add Waypoints to the DOM
	 * 
	 * @param waypoints
	 * @param parentNode
	 *            Node of the UIWaypoints' Transition
	 */
	public void buildWaypoints(UIWaypoints waypoints, Element parentNode)
	{
		if (waypoints == null)
			return;

		// write the waypoints element
		Element waypointsElem = mXMLDocument.createElement("waypoints");
		parentNode.appendChild(waypointsElem);

		buildWaypointsNode(waypoints, waypointsElem, true, "beforemiddle");
		buildWaypointsNode(waypoints, waypointsElem, false, "aftermiddle");
	}

	/**
	 * 
	 * @param waypoints
	 * @param parentNode
	 * @param beforeMiddle
	 * @param tagName
	 */
	public void buildWaypointsNode(UIWaypoints waypoints, Element parentNode, boolean beforeMiddle,
			String tagName)
	{
		String pointsStr = "";
		for (int i = 0; i < waypoints.getCount(beforeMiddle); i++)
		{
			UIPosition point = waypoints.getWaypoint(i, beforeMiddle);
			if (point != null)
			{
				pointsStr += ", " + doubleToString(point.getX()) + ", "
						+ doubleToString(point.getY());
			}
		}
		if (pointsStr.length() > 2)
		{
			pointsStr.substring(2); // strip first ", "
			buildText(pointsStr, parentNode, tagName);
		}
	}

	/**
	 * Add Binds to the DOM
	 * 
	 * @param bind
	 * @param parentNode
	 *            Node of the Bind's (Network) Component
	 */
	public void buildBind(Bind bind, Element parentNode)
	{
		if (bind == null)
			return;

		// write the bind element
		Element bindElem = mXMLDocument.createElement("bind");
		parentNode.appendChild(bindElem);

		bindElem.setAttribute("component", bind.getComponent());
		bindElem.setAttribute("as", bind.getAs());

		buildNote(bind.getNote(), bindElem);

		buildPosition(bind.getPosition(), bindElem);
		buildDimensions(bind.getDimensions(), bindElem);

		// write all maps
		for (int i = 0; i < bind.getMapCount(); i++)
		{
			BindMap map = bind.getMap(i);
			buildMap(map, bindElem);
		}
	}

	/**
	 * Add Maps to the DOM
	 * 
	 * @param map
	 * @param parentNode
	 *            Node of the Map's Bind
	 */
	public void buildMap(BindMap map, Element parentNode)
	{
		if (map == null)
			return;

		// write the bind element
		Element mapElem = mXMLDocument.createElement("map");
		parentNode.appendChild(mapElem);

		mapElem.setAttribute("key", map.getKey());

		// param or value bind?
		if (map instanceof ParamMap)
		{
			ParamMap parMap = (ParamMap) map;
			mapElem.setTextContent(parMap.getParamReference());
		}
		else if (map instanceof ValueMap)
		{
			ValueMap valMap = (ValueMap) map;
			String values = "";

			// determine corresponding variable type
			ParamType varType = ParamType.REAL;

			Bind bind = map.getParent();
			if (bind != null)
			{
				SpaceExComponent comp = mSXDocument.getComponent(bind.getComponent());
				if (comp instanceof SpaceExBaseComponent)
				{
					Param par = comp.getParam(map.getKey());
					if (par != null)
						varType = par.getType();
				}
			}

			// create value list
			for (int i = 0; i < valMap.getValueCount(); i++)
			{
				switch (varType)
				{
				case INT:
					values += ", " + intToString(valMap.getIntValue(i));
					break;
				case REAL:
				default:
					values += ", " + doubleToString(valMap.getRealValue(i));
				}
			}

			mapElem.setTextContent(values.substring(2)); // strip heading ", "
		}
		else
		{
			printWarning("Unknown map type for key: " + map.getKey());
		}
	}

	/**
	 * Build a text node with the given content
	 * 
	 * @param text
	 * @param parentNode
	 *            Where to add it in the DOM
	 * @param tagName
	 *            Name of the Text Node's tag
	 */
	public void buildText(String text, Element parentNode, String tagName)
	{
		if (text == null)
			return;

		if (text.length() <= 0)
			return;

		Element textElem = mXMLDocument.createElement(tagName);
		parentNode.appendChild(textElem);
		textElem.setTextContent(text);
	}

	/**
	 * Add a Node for a Note in the DOM
	 * 
	 * @param note
	 * @param parentNode
	 *            Node of the Note's Component/Location/...
	 */
	public void buildNote(String note, Element parentNode)
	{
		buildText(note, parentNode, "note");
	}

	public void buildPosition(UIPosition pos, Element parentNode)
	{
		if (pos == null)
			return;

		parentNode.setAttribute("x", doubleToString(pos.getX()));
		parentNode.setAttribute("y", doubleToString(pos.getY()));
	}

	public void buildDimensions(UIDimensions dim, Element parentNode)
	{
		if (dim == null)
			return;

		parentNode.setAttribute("width", doubleToString(dim.getWidth()));
		parentNode.setAttribute("height", doubleToString(dim.getHeight()));
	}

	public void buildExpression(Expression expression, Element parentNode, String tagName,
			FormulaType type)
	{
		if (expression == null)
			return;

		buildText(expressionToString(expression, type, tagName).replace("&", "&\n"), parentNode,
				tagName);
	}

	public void buildExpression(Expression expression, Element parentNode, String tagName)
	{
		buildExpression(expression, parentNode, tagName, FormulaType.DEFAULT);
	}

	/**
	 * Save a SpaceEx-compatible config file to go with the XML file
	 * 
	 * @param filename
	 */
	public String getCFGString(boolean skipTol)
	{
		StringBuffer rv = new StringBuffer();

		SpaceExConfigValues config = mSXDocument.getConfig();

		appendCfgString(rv, "system", config.systemID);
		appendCfgString(rv, "scenario", config.scenario); // was supp
		appendCfgString(rv, "directions", config.directions);
		appendCfgString(rv, "set-aggregation", config.aggregation);

		if (config.flowpipeTol > 0)
			appendCfgString(rv, "flowpipe-tolerance", Double.toString(config.flowpipeTol));

		appendCfgString(rv, "sampling-time", Double.toString(config.samplingTime).toString());
		appendCfgString(rv, "time-horizon", Double.toString(config.timeHorizon));
		appendCfgString(rv, "iter-max", Integer.toString(config.maxIterations));
		appendCfgString(rv, "output-format", config.outputFormat);

		if (!skipTol)
		{
			appendCfgString(rv, "rel-err", "1.0e-12");
			appendCfgString(rv, "abs-err", "1.0e-13");
		}

		if (config.timeTriggered)
			appendCfgString(rv, "map-zero-duration-jump-sets", "true");

		Expression e = mSXDocument.getInitialStateConditions();
		if (e != null)
			appendCfgString(rv, "initially",
					"\"" + expressionToString(e, FormulaType.DEFAULT, "initial states") + "\"");
		e = mSXDocument.getForbiddenStateConditions();
		if (e != null)
			appendCfgString(rv, "forbidden",
					"\"" + expressionToString(e, FormulaType.DEFAULT, "forbidden states") + "\"");

		// output-variables

		if (config.outputVars.size() > 0)
		{
			String outputVars = "";

			for (int i = 0; i < config.outputVars.size(); i++)
			{
				if (i > 0)
					outputVars += ", ";

				outputVars += config.outputVars.get(i);
			}

			appendCfgString(rv, "output-variables", "\"" + outputVars + "\"");
		}

		return rv.toString();
	}

	private void appendCfgString(StringBuffer rv, String key, String value)
	{
		String s = key + " = " + value;

		rv.append(s + "\n");
	}

	class SpaceExExpressionPrinter extends DefaultExpressionPrinter
	{
		FormulaType type = FormulaType.DEFAULT;

		@Override
		public String printOperation(Operation o)
		{
			String rv = null;

			if (o.op == Operator.EQUAL)
			{
				rv = print(o.getLeft());

				switch (type)
				{
				case ASSIGNMENT:
					// op = " := ";
					rv += "' == ";
					break;
				case DIFFERENTIAL:
					rv += "' == ";
					break;
				case DEFAULT:
				default:
					rv += " == ";
					break;
				}

				rv += print(o.getRight());
			}
			else if (o.op == Operator.LESSEQUAL || o.op == Operator.GREATEREQUAL
					|| o.op == Operator.LESS || o.op == Operator.GREATER)
			{
				rv = print(o.getLeft());

				switch (type)
				{
				case ASSIGNMENT:
					// op = " := ";
					rv += "' ";
					break;
				default:
					rv += " ";
					break;
				}

				rv += this.opNames.get(o.op);
				rv += " " + print(o.getRight());
			}
			else
				rv = super.printOperation(o);

			return rv;
		}
	}

	public String expressionToString(Expression expression, FormulaType type, String tagName)
	{
		printer.type = type;

		return printer.print(expression);
	}

	public void printWarning(String message)
	{
		System.out.println("[SX XML Printer] Warning: " + message);
	}

	/**
	 * Compatible format: No grouping, dot as decimal seperator, at least one digit before and after
	 * the dot
	 * 
	 * @param value
	 * @return
	 */
	public String doubleToString(double value)
	{
		DecimalFormat formatter = new DecimalFormat("", new DecimalFormatSymbols(Locale.ENGLISH));
		formatter.setGroupingUsed(false);
		formatter.setMinimumFractionDigits(1);
		formatter.setMinimumIntegerDigits(1);
		return formatter.format(value);
	}

	public String intToString(int value)
	{
		return Integer.toString(value);
	}

	/**
	 * Same as doubleToString(), except that no floating point part is enforced (1.0 will be
	 * returned as "1").
	 * 
	 * @param value
	 * @return
	 */
	public String valueToString(double value)
	{
		DecimalFormat formatter = new DecimalFormat("", new DecimalFormatSymbols(Locale.ENGLISH));
		formatter.setGroupingUsed(false);
		formatter.setMinimumFractionDigits(0);
		formatter.setMinimumIntegerDigits(1);
		return formatter.format(value);
	}
}
