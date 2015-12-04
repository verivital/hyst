/**
 * 
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.Operation;
import com.verivital.hyst.grammar.formula.Operator;
import com.verivital.hyst.grammar.formula.Variable;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.Configuration;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.AutomatonTransition;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.geometry.Interval;
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;

public class XspeedPrinter extends ToolPrinter {
	private BaseComponent ha;
	double [] invX ;
	double [][] invY;
	double []ConUX;//matrix that takes the Uncontrol variable ranges from invariant 
	double [][]ConUY; // matrix that takes the Uncontrol variable ranges from invariant 
	double [] guardX ;
	double [][] guardY;
	double [][] Rmatrix;
	double [] Wmatrix;
	int invSize = 0;//size of total invariants
	int Uinvsize =0;//size of uncontrol variable invariants 
	int invindex=0;//Used for indexing of invariant matrix
	int Uinvindex=0; // used for indexing Uncontrol variable from invariants 
	public ArrayList <String> ControlVar = new ArrayList <String>();
	public ArrayList <String> UncontrolVar = new ArrayList <String>();
	
	
	public ArrayList <String> ControlVarI = new ArrayList <String>();//control and uncontrol variable matrix for inital state
	public ArrayList <String> UncontrolVarI = new ArrayList <String>();
	int Inid;
	String initname;//name of initial location
	
	public XspeedPrinter() {
		preconditions.skip[PreconditionsFlag.NO_URGENT.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS
				.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS
				.ordinal()] = true;
	}
	String org;
	@Override
	protected String getCommentCharacter() {
		return "//";
	}

	/**
	 * This method starts the actual printing! Prepares variables etc. and calls
	 * printProcedure() to print the BPL code
	 */
	private void printDocument(String originalFilename) {
		//System.out.println("we are in Xspeed ");
		this.printCommentHeader();
		org = new String(originalFilename);
		org = org.substring(org.lastIndexOf("/")+1);
		// begin printing the actual program
		
		printNewline();
		printProcedure();
	}

	/**
	 * Simplify an expression by substituting constants and then doing math
	 * simplification
	 * 
	 * @param e
	 *            the original expression
	 * @return the modified expression
	 */
	private Expression simplifyExpression(Expression ex) {
		Expression subbed = new SubstituteConstantsPass()
				.substituteConstantsIntoExpression(ha.constants, ex);

		return AutomatonUtil.simplifyExpression(subbed);
	}

	/**
	 * Print the actual Flow* code
	 */
	private void printProcedure() {
		printInitial();
		printModes();
		printInitialStatesExspeed();
		//System.out.println("End init of printing");
		printJumpsXspeed();
		//System.out.println("End of printing");
	}
	private void printInitial()
	{	printLine("#include \"user_model.h\"");
		printLine("void user_model(hybrid_automata& Hybrid_Automata, symbolic_states& initial_symbolic_state, ReachabilityParameters& reach_parameters, int& transition_iterations) {");
		printNewline();
		printNewline();
		printLine("typedef typename boost::numeric::ublas::matrix<double>::size_type size_type;");
		printNewline();
		printNewline();
		printLine("unsigned int Directions_Type = 1;");
		printLine("unsigned int iter_max = "+config.settings.spaceExConfig.maxIterations+";");
		printLine("double time_horizon = "+config.settings.spaceExConfig.timeHorizon+"; ");
		printLine("double sampling_time = "+config.settings.spaceExConfig.samplingTime+";");
		printLine("unsigned int dim;");
		printLine("size_type row, col;");
		printNewline();
		printNewline();
		printNewline();
		printNewline();
		printLine("polytope::ptr initial_polytope_I;");
		printNewline();
		printNewline();
		print("polytope::ptr invariant0");
		for(int h=1; h<ha.modes.size();h++)
		print(", invariant"+h+"");
		printLine(";");
		printNewline();
		printNewline();
		print("polytope::ptr gaurd_polytope0");
		for(int h=1; h<ha.transitions.size();h++)
		print(", gaurd_polytope"+h+"");
		printLine(";");
		printNewline();
		printNewline();
		print("Dynamics system_dynamics0");
		for(int h=1; h<ha.modes.size();h++)
		print(", system_dynamics"+h+"");
		printLine(";");
		printNewline();
		printNewline();
		print("math::matrix<double> ConstraintsMatrixI , ");
		print("ConstraintsMatrixV0, ");
		for(int h=1; h<ha.modes.size();h++)
			print("ConstraintsMatrixV"+h+" , ");
		print("invariantConstraintsMatrix0 , ");
		for(int h=1; h<ha.modes.size();h++)
			print("invariantConstraintsMatrix"+h+" , ");
		print("gaurdConstraintsMatrix0 , ");
		for(int h=1; h<ha.transitions.size();h++)
			print("gaurdConstraintsMatrix"+h+" , ");
		print("A0matrix , ");
		for(int h=1; h<ha.modes.size();h++)
			print("A"+h+"matrix , ");
		print("B0matrix");
		for(int h=1; h<ha.modes.size();h++)
			print(" , B"+h+"matrix");
		printLine(";");
		printNewline();
		printNewline();
		print("std::vector<double> boundValueI,");
		print("boundValueV0 , ");
		for(int h=1; h<ha.modes.size();h++)
			print("boundValueV"+h+" , ");
		print("C0 , ");
		for(int h=1; h<ha.modes.size();h++)
			print("C"+h+" , ");
		print("invariantBoundValue0 , ");
		for(int h=1; h<ha.modes.size();h++)
			print("invariantBoundValue"+h+" , ");
		print("gaurdBoundValue0");
		for(int h=1; h<ha.transitions.size();h++)
		print(" , gaurdBoundValue"+h+"");
		printLine(";");
		printNewline();
		printNewline();
		printLine("int boundSignI, invariantBoundSign, gaurdBoundSign, boundSignV;");
		printNewline();
		printNewline();
	}
	
	

	private void printInitialStatesExspeed() {
		printNewline();
		//int init =1;
		//printLine("init");
		//printLine("{");
		
			//System.out.println(config.init.values().toString());
		/*for (Entry<String, Expression> e : config.init.entrySet()) {
			//System.out.println("Hwfjwchocjewoj");	
			init=init+1 ;
		}*/
		printLine("row = "+ (2*(ControlVar.size()))+";");
		printLine("col = "+ ControlVar.size()+";");
		printLine("ConstraintsMatrixI.resize(row, col);");
			
		//System.out.println("Thezhckuhsviuhvih"+config.init.toString());
	
		
		for (Entry<String, Expression> e : config.init.entrySet()) {
		
		printFlowRangeConditionsExspeed(
				removeConstants(e.getValue(), ha.constants.keySet()), true, 2*ControlVar.size());
		}
		//printLine("}"); // end all initial modes
	}


	private static Expression removeConstants(Expression e,
			Collection<String> constants) {
		Operation o = e.asOperation();
		Expression rv = e;

		if (o != null) {
			if (o.op == Operator.AND) {
				Operation rvO = new Operation(Operator.AND);

				for (Expression c : o.children)
					rvO.children.add(removeConstants(c, constants));

				rv = rvO;
			} else if (o.op == Operator.EQUAL) {
				Expression left = o.getLeft();

				if (left instanceof Variable
						&& constants.contains(((Variable) left).name))
					rv = Constant.TRUE;
			}
		}

		return rv;
	}

	/**
	 * Prints the locations with their labels and everything that happens in
	 * them (invariant, flow...)
	 */
	private void printModes() {
		//System.out.println("printing modes");
		
		
		printNewline();
		//printLine("modes");
		//printLine("{");
		int inc =0;
		// modename
		boolean first = true;
///////////////////////////////////////////inital location selected///////////////////////////////////////////
		for (Entry<String, Expression> e : config.init.entrySet()) {
			int id=1;
			for(Entry<String,AutomatonMode>m: ha.modes.entrySet())
			{
				AutomatonMode m1 = m.getValue();
				if(m1.name.equals(e.getKey()))
					{  
				    Inid = id;
				    initname = e.getKey();
				    break;
					}
			id++;
			}
			id=0;
		}
		
		
		
		//printLine("Max iterations are"+config.settings.spaceExConfig.maxIterations);
		//printLine("dir are "+ config.settings.spaceExConfig.timeHorizon);
		
		
		//System.out.printn("modes size :  "+ ha.es.size());
		for (Entry<String, AutomatonMode> e : ha.modes.entrySet()) {
			ControlVar.clear();/////////////clear Control and uncontrol matrix for next location
			UncontrolVar.clear();
			AutomatonMode mode = e.getValue();
			printCommentblock("The mode name is  "+ mode.name);
			String[] str = new String[ha.variables.size()];
			String[] str1 = new String[ha.constants.keySet().size()];
			str = ha.variables.toArray(str);
			str1 = 	ha.constants.keySet().toArray(str1);
			int j, k;
			
			for(Entry<String, ExpressionInterval> entry : mode.flowDynamics///////////Add control matrix contents
					.entrySet())
			{
				entry.toString();
				if(!entry.getValue().getExpression().toString().equals("0"))
				{
				ControlVar.add(entry.getKey());
				}
			}
			
			/*for(String s: ControlVar)
			{
				System.out.println("The Control is "+s);
			}*/
			for(Entry<String, ExpressionInterval> entry : mode.flowDynamics//add Uncontrol variable entries to the matrix
					.entrySet())
			{
				if(!ControlVar.contains(entry.getKey().toString()))
					UncontrolVar.add(entry.getKey().toString());
			}
			/*if(UncontrolVar.isEmpty())
				System.out.println("No uncontrolled Var");
			else
			{
			for(String s: UncontrolVar)
			{
				System.out.println("The UnControl is "+s);
				//UncontrolVar.remove(ControlVar.indexOf(s));
			}
			}*/
		
			
			
			XspeedExpressionPrinter dep = new XspeedExpressionPrinter(str, str1, ControlVar.size(), UncontrolVar.size());
			if (first)
				first = false;
			else
				printNewline();

			
			
			
			if(mode.name.equals(initname))//check if mode is matching with the initial mode.....if matched then save control and Uncontrol variable matrix
			{
				ControlVarI = ControlVar;
				UncontrolVarI = UncontrolVar;
			}
			
			
			
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics
					.entrySet()) {
				if(!UncontrolVar.contains(entry.getKey()))
						{
			//	System.out.println("calling checking");
				ExpressionInterval ei = entry.getValue();
				//System.out.println("sdsdfcf"+ei.toString());
			//	System.out.println("Arup "+entry.toString());
				//XSpeed:: edited
				String st = entry.getKey();
				for (j = 0; j <=ControlVar.size() ; j++) {
					String st1 = ControlVar.get(j);
					if (st1.equals(st)) {
						break;
					}
				}
				
			//	System.out.println("Constants  "+ha.constants.keySet());
			//	System.out.println("calling BEFORE");
				//System.out.println(j+""+ ei.getExpression().toString());
				
				/*************************************************************
				printExspeedFirst(simplifyExpression(ei.getExpression()), j, ControlVar, UncontrolVar) will output 
				A matrix Bmatrix and Cmatrix content from flow equations...
				
				
				********************************************/////////////////////////
				
				
				dep.printExspeedFirst(simplifyExpression(ei.getExpression()), j, ControlVar, UncontrolVar);
			//	System.out.println("calling AFTER");
			//	printLine("\n");
			//	printLine("" + entry.getValue());
				
				// edited
				ei.setExpression(simplifyExpression(ei.getExpression()));
				// be explicit (even though x' == 0 is implied by Flow*)
			//	printLine(entry.getKey() + "' = " + ei);
						}
			}
			//printLine("}");
			if(ControlVar.size() != 0)
			{
			printLine("row = "+ ControlVar.size()+";");
			printLine("col = "+ ControlVar.size()+";");
			printLine("A"+inc+"matrix.resize(row, col);");
			
			
			for (j = 0; j < ControlVar.size(); j++) {
				for (k = 0; k < ControlVar.size(); k++) 
					printLine("A"+inc+"matrix("+j+" , "+k+") = "+ dep.cons(j, k)+";");
			}
			printLine("system_dynamics"+inc+".isEmptyMatrixA = false;");
			printLine("system_dynamics"+inc+".MatrixA = A"+inc+"matrix;");
			}
			else
			{
				printLine("system_dynamics"+inc+".isEmptyMatrixA = true;");
			}

///////////////////////////////A matrix print complete////////////////////////////////////////
			printNewline();
			
			if(ControlVar.size() != 0 && UncontrolVar.size() != 0)
			{
			printLine("col = "+ UncontrolVar.size()+";");
			printLine("B"+inc+"matrix.resize(row, col);");
			
			for (j = 0; j < ControlVar.size(); j++) {
				for (k = 0; k < UncontrolVar.size(); k++) 
					printLine("B"+inc+"matrix("+j+" , "+k+") = "+ dep.Bmatrix(j, k)+";");
			}
			
			printLine("system_dynamics"+inc+".isEmptyMatrixB = false;");
			printLine("system_dynamics"+inc+".MatrixB = B"+inc+"matrix;");
			}
			else
			{
				printLine("system_dynamics"+inc+".isEmptyMatrixB = true;");
			}
			
////////////////////////////////B matrix print complete///////////////////////////////////////////////				
				
			printNewline();
			
			if(ControlVar.size() != 0)
			{
			printLine("C"+inc+".resize(row );");
			for (j = 0; j < ControlVar.size(); j++)
			{ 
				
					printLine("C"+inc+"["+j+"] = "+ dep.Cmatrix(j)+";");
			}
			
			printLine("system_dynamics"+inc+".isEmptyC = false;");
			printLine("system_dynamics"+inc+".C = C"+inc+";");
			}
			else
			{
				printLine("system_dynamics"+inc+".isEmptyC = true;");
			}
			
			printNewline();
			printNewline();
			printNewline();
///////////////////////////////////C vector print complete//////////////////////////////////////////			
			// invariant
			invSize = 0;
			Uinvsize =0;
			invindex =0;
			Uinvindex=0;
			Expression inv = simplifyExpression(mode.invariant);
			//printLine("   "+ inv.toString());
			if (!inv.equals(Constant.TRUE)) {
				int xl = getsizeofInvX(inv)+1;
				//printCommentblock("Original invariant: " + inv);
				invX = new double[xl-Uinvsize];
				invY = new double[xl-Uinvsize][ControlVar.size()];
				ConUX = new double[Uinvsize];
				ConUY = new double[Uinvsize][UncontrolVar.size()];
				
				for (j = 0; j < xl-Uinvsize; j++) {
					for (k = 0; k < ControlVar.size(); k++) 
						invY[j][k] = 0;
					invX[j]=0;
				}
				for (j = 0; j <Uinvsize; j++) {
					for (k = 0; k < UncontrolVar.size(); k++) 
						ConUY[j][k] = 0;
					ConUX[j]=0;
				}
				
				printNewline();
				printNewline();
				/////////making of invaiant and constraint U polytope/////////////////////////
				getFlowConditionExpressionXspeed(inv);
				//System.out.println("hello");
				//printing invariant matrix/////////////////////////////////////
				if(xl-Uinvsize != 0)
				{
				printLine("row = "+ (xl-Uinvsize)+";");
				printLine("col = "+ ControlVar.size()+";");
				printLine("invariantConstraintsMatrix"+inc+".resize(row, col);");
				for (j = 0; j < xl-Uinvsize; j++) {
					for (k = 0; k < ControlVar.size(); k++) 
						printLine("invariantConstraintsMatrix"+inc+"("+j+","+k+")= "+invY[j][k]+";");
				}
				printNewline();
				printNewline();
				printLine("invariantBoundValue"+inc+".resize(row);");
				for (j = 0; j < xl-Uinvsize; j++) { 
						printLine("invariantBoundValue"+inc+"["+j+"] = "+invX[j]+";");
				}
				
				printLine("invariantBoundSign = 1;");
				printLine("invariant"+inc+" = polytope::ptr(new polytope(invariantConstraintsMatrix"+inc+", invariantBoundValue"+inc+",invariantBoundSign));");
				
				}
				
				else
				{
					//printLine("invariantBoundSign = 1;");
					printLine("invariant"+inc+" = polytope::ptr(new polytope());");
					//printLine("invariant"+inc+" = polytope::ptr(new polytope(invariantConstraintsMatrix"+inc+", invariantBoundValue"+inc+",invariantBoundSign));");
					printLine("invariant"+inc+" -> setIsUniverse(true);");
				}
				
				
				printNewline();
				printNewline();
				printNewline();
				
				//printing ConstraintU matrix//////////////////
				if(UncontrolVar.size() != 0)
				{
				printLine("row = "+ Uinvsize+";");
				printLine("col = "+ UncontrolVar.size()+";");
				printLine("ConstraintsMatrixV"+inc+".resize(row, col);");
				//System.out.println("Hii"+Uinvsize);
				for (j = 0; j < Uinvsize; j++) {
					for (k = 0; k < UncontrolVar.size(); k++) 
						printLine("ConstraintsMatrixV"+inc+"("+j+","+k+")= "+ConUY[j][k]+";");
				}
				
				
				printNewline();
				printNewline();
				printLine("boundValueV"+inc+".resize(row);");
				for (j = 0; j < Uinvsize; j++) { 
						printLine("boundValueV"+inc+"["+j+"] = "+ConUX[j]+";");
				}
				printLine("boundSignV = 1;");
				
				printLine("system_dynamics"+inc+".U = polytope::ptr(new polytope(ConstraintsMatrixV"+inc+", boundValueV"+inc+", boundSignV));");
				}
				else
				{
				printLine("system_dynamics"+inc+".U = polytope::ptr(new polytope(true));");	
				}
				
				
				invSize = 0;
				Uinvsize =0;
				invindex =0;
				Uinvindex=0;
				
				printNewline();
				printNewline();
				printNewline();
				printNewline();
				
				//System.out.println("Hello");
				

			}
			inc = inc+1;
			//printLine("}"); // end invariant

			//printLine("}"); // end individual mode
		}
		

	//	printLine("}"); // end all modes
	}
///////////////////////////////////////////Locations work completed ///////////////////////////////////
	
	public static boolean isLinearExpression(Expression e) {
		boolean rv = true;

		Operation o = e.asOperation();

		if (o != null) {
			if (o.op == Operator.MULTIPLY) {
				int numVars = 0;

				for (Expression c : o.children) {
					int count = countVariablesMultNeg(c);

					if (count != Integer.MAX_VALUE)
						numVars += count;
					else {
						rv = false;
						break;
					}
				}

				if (numVars > 1)
					rv = false;
			} else if (o.op == Operator.ADD || o.op == Operator.SUBTRACT) {
				for (Expression c : o.children) {
					if (!isLinearExpression(c)) {
						rv = false;
						break;
					}
				}
			} else if (o.op == Operator.NEGATIVE)
				rv = isLinearExpression(o.children.get(0));
			else
				rv = false;
		}

		return rv;
	}

	/**
	 * Recursively count the number of variables. only recurse if we have
	 * multiplication, or negation, otherwise return Integer.MAX_VALUE
	 * 
	 * @param e
	 *            the expression
	 * @return the number of variables
	 */
	private static int countVariablesMultNeg(Expression e) {
		int rv = 0;
		Operation o = e.asOperation();

		if (o != null) {
			if (o.op == Operator.MULTIPLY || o.op == Operator.NEGATIVE) {
				for (Expression c : o.children) {
					int count = countVariablesMultNeg(c);

					if (count == Integer.MAX_VALUE)
						rv = Integer.MAX_VALUE;
					else
						rv += count;
				}
			} else
				rv = Integer.MAX_VALUE;
		} else if (e instanceof Variable)
			rv = 1;

		return rv;
	}

	public static String getFlowConditionExpression(Expression e) {
		String rv = null;

		try {
			rv = getFlowConditionExpressionRec(e);
			//System.out.println(rv);
		} catch (AutomatonExportException ex) {
			
			throw new AutomatonExportException("Error with expression:" + e, ex);
		}

		return rv;
	}

	private static String getFlowConditionExpressionRec(Expression e) {
		String rv = "";
		// replace && with ' ' and then print as normal

		if (e instanceof Operation) {
			Operation o = (Operation) e;

			if (o.op == Operator.AND) {
				rv += getFlowConditionExpressionRec(o.getLeft());
				rv += "   ";
				rv += getFlowConditionExpressionRec(o.getRight());
			} else if (o.op == Operator.EQUAL) {
				rv += getFlowConditionExpressionRec(o.getLeft());
				rv += " = ";
				rv += getFlowConditionExpressionRec(o.getRight());
			} else if (o.op == Operator.OR) {
				throw new AutomatonExportException(
						"XSpeed printer doesn't support OR operator. "
								+ "Consider using a Hyst pass to eliminate disjunctions)");
			} else if (Operator.isComparison(o.op)) {
				Operator op = o.op;

				// Flow doesn't like < or >... needs <= or >=
				if (op.equals(Operator.GREATER) || op.equals(Operator.LESS)
						|| op.equals(Operator.NOTEQUAL))
					throw new AutomatonExportException(
							"XSpeed printer doesn't support operator " + op);

				// make sure it's of the form p ~ c
				if (o.children.size() == 2 && o.getRight() instanceof Constant)
					rv = e.toString();
				else {
					// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'

					rv += getFlowConditionExpression(o.getLeft());
					rv += " - (" + getFlowConditionExpression(o.getRight());
					rv += ")" + Expression.expressionPrinter.printOperator(op)
							+ "0";
				}
			} else
				rv = e.toString();
		} else
			rv = e.toString();

		return rv;
	}

	/***************************************
	getFlowConditionExpressionXspeed(Expression e) calls getFlowConditionExpressionRecXspeed(e) which creates invariant marix and Upolytope...
	from the set of invariants....
	
	
	*///////////////////////////////////////////////////////////////////
	
	
	public String getFlowConditionExpressionXspeed(Expression e) {
		String rv = null;

		try {
			rv = getFlowConditionExpressionRecXspeed(e);
			//System.out.println(rv);
		} catch (AutomatonExportException ex) {
			System.out.println("XSpeed Can not able to handle those models");
			throw new AutomatonExportException("Error with expression:" + e, ex);
		}
			return rv;
	}

	private  String getFlowConditionExpressionRecXspeed(Expression e) {
		String rv = "";
		// replace && with ' ' and then print as normal
		//System.out.println(e.toString());
		if (e instanceof Operation) {
			Operation o = (Operation) e;
			if (o.op == Operator.AND) {
				rv += getFlowConditionExpressionRecXspeed(o.getLeft());
				rv += "   ";
				rv += getFlowConditionExpressionRecXspeed(o.getRight());
			} else if (o.op == Operator.EQUAL) {
				rv += getFlowConditionExpressionRecXspeed(o.getLeft());
				rv += " = ";
				rv += getFlowConditionExpressionRecXspeed(o.getRight());
			} else if (o.op == Operator.OR) {
				throw new AutomatonExportException(
						"Xspeed printer doesn't support OR operator. "
								+ "Consider using a Hyst pass to eliminate disjunctions)");
			} else if (Operator.isComparison(o.op)) {
				Operator op = o.op;

				// Flow doesn't like < or >... needs <= or >=
				if (op.equals(Operator.GREATER) || op.equals(Operator.LESS)
						|| op.equals(Operator.NOTEQUAL))
					throw new AutomatonExportException(
							"Xspeed printer doesn't support operator " + op);

				// make sure it's of the form p ~ c
				if ((o.children.size() == 2 && o.getRight() instanceof Constant && op.equals(Operator.GREATEREQUAL)))
					{
					//System.out.println("Hii"+o.getLeft().toString());
					//System.out.println(" I'm in block 1");
						rv = e.toString();
					//System.out.println(rv);
						if(ControlVar.contains(o.getLeft().toString()))
						{
						int l= CVarcontainX(o.getLeft().toString());
					//System.out.println(" The index is "+l);
						invY[invindex][l] = -1;
						invX[invindex] = -(Double.parseDouble(o.getRight().toString()));
						invindex = invindex+1;
						}
						else
						{
							int l= UVarcontainX(o.getLeft().toString());
							//System.out.println(" The index is "+l);
								ConUY[Uinvindex][l] = -1;
								ConUX[Uinvindex] = -(Double.parseDouble(o.getRight().toString()));
								Uinvindex = Uinvindex+1;
						}
					}
				else {
					if(o.children.size() == 2 && o.getRight() instanceof Constant && op.equals(Operator.LESSEQUAL))
					{
						//System.out.println("Hii"+o.getLeft().toString());
						//System.out.println(" I'm in block 2");
						rv = e.toString();
						//System.out.println(rv);
						if(ControlVar.contains(o.getLeft().toString()))
						{
						int l= CVarcontainX(o.getLeft().toString());
						//System.out.println("The index is "+l);
						invY[invindex][l] = 1;
						invX[invindex] = (Double.parseDouble(o.getRight().toString()));
						invindex = invindex+1;
						}
						else
						{
							int l= UVarcontainX(o.getLeft().toString());
							//System.out.println("The index is "+l);
							ConUY[Uinvindex][l] = 1;
							ConUX[Uinvindex] = (Double.parseDouble(o.getRight().toString()));
							Uinvindex = Uinvindex+1;
						}
					}
					else
					{
						if((o.children.size() == 2 && o.getLeft() instanceof Constant && op.equals(Operator.LESSEQUAL)))
						{
							//System.out.println("Hii"+o.getRight().toString());
							//System.out.println(" I'm in block 3");
							rv = e.toString();
							//System.out.println(rv);
							if(ControlVar.contains(o.getRight().toString()))
							{
							int l= CVarcontainX(o.getRight().toString());
							//System.out.println("The index is "+l);
							//System.out.println("The index is "+invindex);
							invY[invindex][l] = -1;
							invX[invindex] = -(Double.parseDouble(o.getLeft().toString()));
							invindex = invindex+1;
							}
							else
							{
								int l= UVarcontainX(o.getRight().toString());
								//System.out.println("The index is "+l);
								//System.out.println("The index is "+invindex);
								ConUY[Uinvindex][l] = -1;
								ConUX[Uinvindex] = -(Double.parseDouble(o.getLeft().toString()));
								Uinvindex = Uinvindex+1;
								
							}
							//System.out.println(" I'm out block 3");
						}
						
					// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
						else
						{
							if((o.children.size() == 2 && o.getLeft() instanceof Constant && op.equals(Operator.GREATEREQUAL)))
							{
								//System.out.println("Hii"+o.getRight().toString());
								//System.out.println(" I'm in block 4");
								rv = e.toString();
								if(ControlVar.contains(o.getRight().toString()))
								{
								//System.out.println(rv);
								int l= CVarcontainX(o.getRight().toString());
								//System.out.println("The index is "+l);
								invY[invindex][l] = 1;
								invX[invindex] = (Double.parseDouble(o.getRight().toString()));
								invindex = invindex+1;
								}
								else
								{
									int l= UVarcontainX(o.getRight().toString());
									//System.out.println("The index is "+l);
									ConUY[Uinvindex][l] = 1;
									ConUX[Uinvindex] = (Double.parseDouble(o.getRight().toString()));
									Uinvindex = Uinvindex+1;
									
								}
							}
							else
							{
								rv += getFlowConditionExpressionXspeed(o.getLeft());
								rv += " - (" + getFlowConditionExpressionXspeed(o.getRight());
								rv += ")" + Expression.expressionPrinter.printOperator(op)
								+ "0";
							}
						}
					}	
				}
			} else
				rv = e.toString();
		} else
			rv = e.toString();
		//System.out.println("hello");
		return rv;
		
	}

	/*****************************************************************
	getguardConditionExpressionXspeed(Expression e) function calls  getguardConditionExpressionRecXspeed(Expression e) which will create guard matrix
	from the set of assignments..............
	*///////////////////////////////////////////////////////////////////////
	public String getguardConditionExpressionXspeed(Expression e) {
		String rv = null;
		try {
			invindex =0;
			rv = getguardConditionExpressionRecXspeed(e);
		} catch (AutomatonExportException ex) {
			throw new RuntimeException("XSpeed does not work on these models\n");
		}
			return rv;
	}

	private  String getguardConditionExpressionRecXspeed(Expression e) {
		String rv = "";
		try
		{
		// replace && with ' ' and then print as normal
		if (e instanceof Operation) {
			Operation o = (Operation) e;
			if (o.op == Operator.AND) {
				//System.out.println("I'm here");
				rv += getguardConditionExpressionRecXspeed(o.getLeft());
				rv += "   ";
				rv += getguardConditionExpressionRecXspeed(o.getRight());
			} else if (o.op == Operator.EQUAL) {
				rv = e.toString();
				int l= CVarcontainI(o.getLeft().toString());
				//System.out.println("The index is "+l);
				guardY[invindex][l] = -1;
				guardY[invindex+1][l] = 1;
				if(ConstCheck(o.getRight().toString()))
				{
					guardX[invindex] = -1;
					guardX[invindex+1] = 1;
				}
				else
				{
					guardX[invindex] = -(Double.parseDouble(o.getRight().toString()));
					guardX[invindex+1] = (Double.parseDouble(o.getRight().toString()));
				}
				
				invindex = invindex+2;
			} else if (o.op == Operator.OR) {
				throw new AutomatonExportException(
						"Xspeed printer doesn't support OR operator. "
								+ "Consider using a Hyst pass to eliminate disjunctions)");
			} else if (Operator.isComparison(o.op)) {
				
				Operator op = o.op;
				// Flow doesn't like < or >... needs <= or >=
				if (op.equals(Operator.NOTEQUAL))
					
					System.out.println("need to be modified");
				
				// make sure it's of the form p ~ c
				//System.out.println("********"+o.getRight().toString());
				
				if ((o.children.size() == 2 &&( (o.getRight() instanceof Constant) || ConstCheck(o.getRight().toString()))&& (op.equals(Operator.GREATEREQUAL) || op.equals(Operator.GREATER) )))
					{
					rv = e.toString();
					int l= CVarcontainI(o.getLeft().toString());
					//System.out.println("The index is "+l);
					guardY[invindex][l] = -1;
					if(ConstCheck(o.getRight().toString()))
					{
						guardX[invindex] = -1;
				
					}
					else
					guardX[invindex] = -(Double.parseDouble(o.getRight().toString()));
				
					
					invindex = invindex+1;
					}
				else {
					if(o.children.size() == 2 && ((o.getRight() instanceof Constant)||ConstCheck(o.getRight().toString())) && (op.equals(Operator.LESSEQUAL)|| ( op.equals(Operator.LESS) )))
					{
						//System.out.println("haksfha");
						rv = e.toString();
						int l= CVarcontainI(o.getLeft().toString());
						//System.out.println("The index is "+l);
						guardY[invindex][l] = 1;
						//System.out.println("Desferg4ergr"+ConstCheck(o.getRight().toString()));
						if(ConstCheck(o.getRight().toString()))
						{
							guardX[invindex] = 1;
						//	System.out.println("The index is cxsdgwvesgeshe");
						}
						else
							guardX[invindex] = (Double.parseDouble(o.getRight().toString()));	
						
						invindex = invindex+1;
					}
					else
					{
						if((o.children.size() == 2 && ((o.getLeft() instanceof Constant)||ConstCheck(o.getLeft().toString())) && (op.equals(Operator.LESSEQUAL) || ( op.equals(Operator.LESS) ))))
						{	
							rv = e.toString();
							int l= CVarcontainI(o.getRight().toString());
							//System.out.println("The index is "+l);
							guardY[invindex][l] = -1;
							
							if(ConstCheck(o.getRight().toString()))
									{
								guardX[invindex] = -1;
								
									}
							else
							guardX[invindex] = -(Double.parseDouble(o.getLeft().toString()));
							
							
							invindex = invindex+1;
							
						}
						
					// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
						else
						{
							if((o.children.size() == 2 && ((o.getLeft() instanceof Constant)||ConstCheck(o.getLeft().toString())) && (op.equals(Operator.GREATEREQUAL) ||( op.equals(Operator.GREATER) ))))
							{
								
								rv = e.toString();
								int l= CVarcontainI(o.getLeft().toString());
								//System.out.println("The index is "+l);
								guardY[invindex][l] = 1;
								if(ConstCheck(o.getRight().toString()))
								{
									guardX[invindex] = 1;
							
								}
								else
								guardX[invindex] = (Double.parseDouble(o.getRight().toString()));
								
								
								invindex = invindex+1;
							}
							else
							{
								rv += getguardConditionExpressionXspeed(o.getLeft());
								rv += " - (" + getguardConditionExpressionXspeed(o.getRight());
								rv += ")" + Expression.expressionPrinter.printOperator(op)
								+ "0";
							}
						}
					}	
				}
			} else
				rv = e.toString();
		} else
			rv = e.toString();
		}
		catch(Exception ex)
		{
			throw new RuntimeException("XSpeed does not work on Disjunctiveguard models\n");
			
		}

		return rv;
	}
	
	
	
	
	
	
	
	
	


	
/*****************************************	
	printFlowRangeConditionsExspeed(,...........) function used for making Initial polytope I from the initial input.
	
*///////////////////////////////////////////////	
	private void printFlowRangeConditionsExspeed(Expression ex, boolean isAssignment, int row) {
		TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();
		
		
		
		try {
			RangeExtractor.getVariableRanges(ex, ranges);
		} catch (EmptyRangeException e) {
			System.out.println("XSpeed Can not able to handle those models");
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} catch (ConstantMismatchException e) {
			
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		int init = ControlVarI.size();
		int ini = 0;
		int inj = 0;
		//System.out.println("sjdcdc"+init);
		double [] ConstraintsMatrixI = new double[ControlVarI.size()*2];
		
		for (Entry<String, Interval> e : ranges.entrySet()) {
			String varName = e.getKey();
			//Interval inter = e.getValue();

			if (isAssignment && ControlVarI.contains(varName))
			{
				for(int vari=ini; vari<ini+2; vari++)
				{
					for(int varj=0; varj<init; varj++)
					{
						if(inj/2 == varj && vari%2==0)
						printLine("ConstraintsMatrixI("+vari+" , "+varj+") = 1;");
						else
						if(inj/2 == varj && vari%2==1)
							printLine("ConstraintsMatrixI("+vari+" , "+varj+") = -1;");
						else
							printLine("ConstraintsMatrixI("+vari+" , "+varj+") = 0;");
					}
					inj = inj+1;
				}
				ini =ini+2;
			}
				
			else {
				// it's a comparison
				/*
				if (inter.min == inter.max)
					printLine(varName + " = " + doubleToString(inter.min));
				else {
					if (inter.min != -Double.MAX_VALUE)
						printLine(varName + " >= " + doubleToString(inter.min));

					if (inter.max != Double.MAX_VALUE)
						printLine(varName + " <= " + doubleToString(inter.max));
				}*/
			}
		}
		//int vari = 0;
		
		
		printLine("boundValueI.resize(row );");
		for (Entry<String, Interval> e : ranges.entrySet())
		{
			if(ControlVarI.contains(e.getKey().toString()))
			{	
			//System.out.println(VarcontainX(e.getKey()));
				//System.out.println("e.getkey "+e.getKey());
			int max = 2*(CVarcontainX(e.getKey()));
			int min = 2*(CVarcontainX(e.getKey()))+1;
			//System.out.println("The boundI is"+e.getKey());
			if (isAssignment)
			{
				ConstraintsMatrixI[max]= e.getValue().max;
				//System.out.println("boundValueI["+max+"]="+doubleToString(ConstraintsMatrixI[max])+";");
				if(e.getValue().min==0)
				ConstraintsMatrixI[min]= e.getValue().min;
				else
				ConstraintsMatrixI[min]= -e.getValue().min;
				//System.out.println("boundValueI["+min+"]="+doubleToString(ConstraintsMatrixI[min])+";");
				//System.out.println("max"+max+" "+e.getValue().max);
				//System.out.println("min"+min+" "+e.getValue().min);
			}
			}
		}
		for(int h=0;h<ControlVarI.size()*2;h++)
		{
			printLine("boundValueI["+h+"]="+doubleToString(ConstraintsMatrixI[h])+";");
			
		}
		printLine("boundSignI = 1;");
		printNewline();
		printNewline();
		printNewline();

		
	}

	
	/**************************************
	printJumpsXspeed() function is used for making guard matrix and assignment matrix W[] and R matrix of transition .
	
	*///////////////////////////////////
	
	private void printJumpsXspeed() {
		printNewline();
		boolean first = true;
		int inc =0;
		for (AutomatonTransition t : ha.transitions) {
			printCommentblock("The transition label is"+t.label);
			
			//String stw[] = new String[t.reset.size()];
			Rmatrix = new double[ControlVarI.size()][ControlVarI.size()];
			Wmatrix = new double[ControlVarI.size()];
			//Expression ee  = t.reset.keySet();
			//System.out.println("Assignments may be  ....."+t.reset.get(ha.modes.toString()));
			Expression guard = simplifyExpression(t.guard);
			//System.out.println("guard"+guard.toString());
			//System.out.println("guard size"+(getsizeofInvX(guard)));
			//System.out.println("guards are  "+(getsizeofInvX(guard)+1));
			int xl = getsizeofInvX(guard)+1;
			guardX = new double[xl];
			guardY = new double[xl][ControlVarI.size()];
			if (guard == Constant.FALSE)
				continue;

			if (first)
				first = false;
			else
				printNewline();
			
			for(int i=0;i<xl;i++)
			for(int k=0;k<ControlVarI.size();k++)
			 guardY[i][k] = 0;	
			for(int i=0;i<xl;i++)
				guardX[i] = 0;

			if (!guard.equals(Constant.TRUE)) {
				printCommentblock("Original guard: " + t.guard);
			   // System.out.println("The guards are"+t.guard);
				getguardConditionExpressionXspeed(guard);
			}
			if(xl!=0)
			{
			printLine("row = "+xl+";");
			printLine("col = "+ControlVarI.size()+";");
			printNewline();
			
			printLine("gaurdConstraintsMatrix"+inc+".resize(row, col);");
			for(int i=0;i<xl;i++)
				for(int k=0;k<ControlVarI.size();k++)
				 printLine("gaurdConstraintsMatrix"+inc+"("+i+","+k+") = "+guardY[i][k]+";"); 
			
				printNewline();
				printLine("gaurdBoundValue"+inc+".resize(row);");
			
				for(int i=0;i<xl;i++)
					printLine("gaurdBoundValue"+inc+"["+i+"] = "+guardX[i]+";");
				
				printLine("gaurdBoundSign = 1;");
				
				printLine("gaurd_polytope"+inc+" = polytope::ptr(new polytope(gaurdConstraintsMatrix"+inc+", gaurdBoundValue"+inc+", gaurdBoundSign));");
			}
			
			else
			{
				printLine("gaurdBoundSign = 1;");
				printLine("gaurd_polytope"+inc+" = polytope::ptr(new polytope(gaurdConstraintsMatrix"+inc+", gaurdBoundValue"+inc+", gaurdBoundSign));");
				printLine("gaurd_polytope"+inc+" -> setIsUniverse(true);");
			}
			
			printNewline();
			printNewline();
			printNewline();
			printNewline();
			printNewline();
			printNewline();
			
			inc = inc+1;
			invSize = 0;
			
		}
			
	
			inc =0;
			for (AutomatonTransition t1 : ha.transitions) {
				printCommentblock("The transition label is   "+t1.label);
				//System.out.println("guards are  "+(getsizeofInvX(guard)+1));
				
				
				for(int i=0;i<ControlVar.size();i++)
					for(int k=0;k<ControlVar.size();k++)
					{
						if(i==k)
						Rmatrix[i][k] = 1;
						else
							Rmatrix[i][k] = 0;
					}
				
				for (String key : t1.reset.keySet()) {
					//String rv = "";
					int re = CVarcontainX(key);
				    Expression eee = t1.reset.get(key).getExpression();
				 
				    for(int i=0;i<ControlVarI.size();i++)
						Wmatrix[i] = 0;
				
				    
				    
				    if(eee instanceof Operation)
				    {
				    	
				    	eee = simplifyExpression(eee);
				    	Operation o = (Operation) eee;
				    	if(o.getRight() instanceof Variable)
				    	{
				    		Rmatrix[re][CVarcontainX(o.getRight().toString())] = Double.parseDouble(o.getLeft().toString());
				    	}
				    	
				    	else
				    		resetExtract(re,o);
				    		
				    	//System.out.println("The Rmatrix["+VarcontainX(o.getRight())+"]["+VarcontainX(o.getRight())+"] is ....  "+ Rmatrix[VarcontainX(o.getRight())][VarcontainX(o.getRight())]);
				    }	
				    else if(eee instanceof Variable)
				    {
				    	Rmatrix[re][CVarcontainX(eee.toString())] = 1;
				    	//System.out.println("The Rmatrix["+VarcontainX(eee)+"]["+VarcontainX(eee)+"] is ....  "+ Rmatrix[VarcontainX(eee)][VarcontainX(eee)]);
				    	
				    }
				    
				}
				
				printLine("math::matrix<double> R"+inc+";");
				printLine("row = "+ControlVar.size()+";");
				printLine("col = "+ControlVar.size()+";");
				printLine("R"+inc+".resize(row, col);");
				
				
				for(int i=0;i<ControlVarI.size();i++)
					for(int k=0;k<ControlVarI.size();k++)
						printLine("R"+inc+"("+i+","+k+") =  "+Rmatrix[i][k]+";");
				printLine("std::vector<double> w"+inc+"(row);");
				
				for(int i=0;i<ControlVarI.size();i++)
					printLine("w"+inc+"["+i+"] = "+Wmatrix[i]+";");
				
				printNewline();
				printNewline();
				printNewline();
				printNewline();
				printNewline();
				
				printLine("Assign assignment"+inc+";");
				printLine("assignment"+inc+".Map = R"+inc+";");
				printLine("assignment"+inc+".b = w"+inc+";");
				
				printNewline();
				printNewline();
				printNewline();
				
				
				
				inc = inc+1;
			}
			
			
			

			/*for (int h=0;h<ha.modes.size();h++) {
			
				
			printNewline();
			printNewline();
			printNewline();
				
			printLine("system_dynamics"+h+".MatrixA = A"+h+"matrix;");
			printLine("system_dynamics"+h+".MatrixB = B"+h+"matrix;");
			printLine("system_dynamics"+h+".MatrixC = C"+h+"matrix;");
			
			printNewline();
			printNewline();
			printNewline();
			
			printLine("system_dynamics"+h+".U = polytope::ptr(new polytope(ConstraintsMatrixV"+h+", boundValueV"+h+", boundSignV"+h+"));");
			}*/
			printNewline();
			printNewline();
			printNewline();
			
			
			printLine("initial_polytope_I = polytope::ptr(new polytope(ConstraintsMatrixI, boundValueI, boundSignI));");

			printNewline();
			printNewline();
			printNewline();
			
			
			
			
			
			
			
			
			
			
			
			
			for (AutomatonTransition t1 : ha.transitions) {

				printLine("transitions t"+TransIdXspeed(t1.label)+"("+TransIdXspeed(t1.label)+",\""+t1.label+"\","+ModeIdXspeed(t1.from)+","+ModeIdXspeed(t1.to)+",gaurd_polytope"+(TransIdXspeed(t1.label)-1)+",assignment"+(TransIdXspeed(t1.label)-1)+");");
				}
			printNewline();
			printNewline();
			printNewline();
			int h=0;
			for(Entry<String,AutomatonMode>m: ha.modes.entrySet())
			{
				AutomatonMode mode = m.getValue();
				printLine("std::list<transitions> Out_Going_Trans_from"+mode.name+";");
				printNewline();
				int from =0;
				for(AutomatonTransition t:ha.transitions)
				{
					if(t.from == mode)
					{
						from = from+1;
						printLine("Out_Going_Trans_from"+mode.name+".push_back(t"+TransIdXspeed(t.label)+");");
					}
					
				}
				
				if(from ==0)
					//printLine("Out_Going_Trans_from"+mode.name+".push_back(Null);");
				printNewline();
				Expression inv = simplifyExpression(mode.invariant);
				//printLine("   "+ inv.toString());
					int xl = getsizeofInvX(inv)+1;
					if(xl-Uinvsize!=0)
					{
						printLine("location l"+ModeIdXspeed(mode)+"("+ModeIdXspeed(mode)+", \""+mode.name+"\", system_dynamics"+h+", invariant"+h+", true, Out_Going_Trans_from"+mode.name+");");
					}
					else
						printLine("location l"+ModeIdXspeed(mode)+"("+ModeIdXspeed(mode)+", \""+mode.name+"\", system_dynamics"+h+", invariant"+h+", false, Out_Going_Trans_from"+mode.name+");");
				h++;
				printNewline();
				printNewline();
				printNewline();
			}
			
			printNewline();
			printNewline();
			
			printLine("dim = initial_polytope_I->getSystemDimension();");
			printLine("Hybrid_Automata.addInitial_Location(l1);");
			for(Entry<String,AutomatonMode>m: ha.modes.entrySet())
			{
				AutomatonMode mode = m.getValue();
				printLine("Hybrid_Automata.addLocation(l"+ModeIdXspeed(mode)+");");
			}
			
			printLine("Hybrid_Automata.setDimension(dim);");
			printNewline();
			printNewline();
			
			printLine("discrete_set d_set;");
			printLine("d_set.insert_element("+Inid+");");
			printLine("initial_symbolic_state.setDiscreteSet(d_set);");
			printLine("initial_symbolic_state.setContinuousSet(initial_polytope_I);");
			printNewline();
			printNewline();
			
			printLine("dim = initial_symbolic_state.getContinuousSet()->getSystemDimension();");
				printLine("std::vector<std::vector<double> > newDirections;");
				printLine("math::matrix<double> Real_Directions;");
				printLine("unsigned int dir_nums;");
				printLine("if (Directions_Type == BOX) {");
				printLine("dir_nums = 2 * dim;");
				printLine("newDirections = generate_axis_directions(dim);");
				printLine("}");
				printLine("if (Directions_Type == OCT) {");
				printLine("dir_nums = 2 * dim * dim;");
				printLine("newDirections = get_octagonal_directions(dim);");
				printLine("}");
				printLine("if (Directions_Type > 2) {");
				printLine("dir_nums = Directions_Type;");
				printLine("newDirections = math::uni_sphere(dir_nums, dim, 100, 0.0005);");
				printLine("}");
				
				printLine("get_ublas_matrix(newDirections, Real_Directions); ");
				printLine("row = dir_nums;");
				printLine("col = dim;");
				printLine("reach_parameters.Directions.resize(row, col);");
				printLine("reach_parameters.Directions = Real_Directions;");
				printLine("transition_iterations = iter_max;");
				printLine("reach_parameters.Iterations =  time_horizon / sampling_time; ");
				printLine("reach_parameters.TimeBound = time_horizon; ");
				printLine("reach_parameters.time_step = reach_parameters.TimeBound / reach_parameters.Iterations;");
			
			
			
			
			
			printLine("}");
			
	}
	
	
	
	///////**************************************************************
	
	public static class XspeedExpressionPrinter extends DefaultExpressionPrinter
	{
		
		public ArrayList <String> ControlVar = new ArrayList <String>();
		public ArrayList <String> UncontrolVar = new ArrayList <String>();
		int index,i,index0,indexU, indexU0;
		String [] st;
		String [] st1;
		double[][] cons ;
		double[][] Bmatrix;
		double [] constraint;
		public XspeedExpressionPrinter(String [] vars, String [] cos, int con, int uncon)
		{
			super();
			cons = new double[con][con];
			Bmatrix = new double [con][uncon];
			for(int k=0; k<con;k++)
				for(int j=0;j<con;j++)
					cons[k][j]=0;
			for(int k=0; k<con;k++)
				for(int j=0;j<uncon;j++)
					Bmatrix[k][j]=0;
			
			st = new String[vars.length];
			st = vars;
			st1 = new String[cos.length];
			st1 = cos;
			constraint = new double[con];
			for(int k=0; k<con;k++)
					constraint[k]=0;
		}
		
		public String printOperationExspeed(Operation o, int j)
		{
			
			String rv;
			List <Expression> children = o.children;
			Operator op = o.op;
			
			if(op.equals(Operator.EQUAL))
			{
				if(children.get(1) instanceof Constant)
				{
					constraint[j] = Double.parseDouble(children.get(1).toString());
				}
			}
			
			if (children.size() == 0)
				{
				rv = printOperator(o.op);
				}
			else if (children.size() == 1)
			{
				
				//System.out.println("I'm in 1 child block");
				
				Expression child = children.get(0);
			    
				
				if (op.equals(Operator.NEGATIVE) || op.equals(Operator.LOGICAL_NOT))
				{
					if (child instanceof Operation && child.asOperation().children.size() > 1)
						rv = opNames.get(o.op) + "(" + printExspeed(child,j) + ")";
					else
						rv = opNames.get(o.op) + "" + printExspeed(child,j);
				//	System.out.println("negative : "+rv);
					
				}
				else
					{
					
					rv = opNames.get(o.op) + "(" + printExspeed(child,j) + ")";
				    }
				for(int l =0 ;l<st1.length;l++)
				{
					if(child.toString().equals(st1[l]))
					{
						//System.out.println(j+" () "+l+"  "+st1[l]);
						constraint[j] = Double.parseDouble(rv);
						break;
					}
				}
				
			}
			else if (children.size() == 2)
			{
				//System.out.println(children.toString());
				
				//System.out.println("vvvv"+children.toString());
				
				Expression leftExp = children.get(0);
				Operation left = leftExp.asOperation();
				
				Expression rightExp = children.get(1);
				Operation right = rightExp.asOperation();
				
			
				boolean needParenLeft = false;
				boolean needParenRight = false;
				
				rv = "";
				// use parentheses if they are needed
	
				if(children.get(1) instanceof Variable && children.get(0) instanceof Variable && o.op.equals(Operator.MULTIPLY))
				{
					throw new RuntimeException("XSpeed does not work on non-linear models\n");
					
				}
				
				
				
				if(children.get(1) instanceof Variable && children.get(0) instanceof Constant && o.op.equals(Operator.MULTIPLY))
				{				
					for (int k = 0; k < ControlVar.size(); k++)
					{
						if(ControlVar.get(k).equals((children.get(1).toString())))
							{
							//System.out.println("hiii");
							cons[j][k] =Double.parseDouble(children.get(0).toString());
							//System.out.println("hiii    cons["+j+"]["+k+"]"+cons[j][k]);
							   break;
							}
					}
					for (int k = 0; k < UncontrolVar.size(); k++)
					{
					if(UncontrolVar.get(k).equals((children.get(1).toString())))
						{
						//System.out.println("hiii");
							Bmatrix[j][k] =Double.parseDouble(children.get(0).toString());
							//System.out.println("hiii"+Bmatrix[j][k]);
							break;
						}
					}
				}
				
				else if(( rightExp instanceof Constant) && (opNames.get(o.op)=="+" || opNames.get(o.op)=="-"))
				{
					//System.out.println("hiii");
					if(opNames.get(o.op)=="-")
					{
					constraint[j] = -Double.parseDouble(rightExp.toString());
					}
					else
					{
					constraint[j] = Double.parseDouble(rightExp.toString());
					}
					
					rv += printExspeed(leftExp,j);
				}
			
				else if(left instanceof Operation && rightExp instanceof Variable && opNames.get(o.op)=="-")
					{
						for (int k = 0; k < ControlVar.size(); k++)
						{
							if(ControlVar.get(k).equals(rightExp))
								{
								//System.out.println("hiii");
									cons[j][k]= -1;
									break;
								}
						
						}
					
						for (int k = 0; k < UncontrolVar.size(); k++)
						{
							if(UncontrolVar.get(k).equals(rightExp))
							{
								//System.out.println("hiii");
								cons[j][k]= -1;
								break;
							}
						}
						
						rv += printExspeed(leftExp,j);
					//   System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
					}
				
				
				else
					if(left instanceof Operation && rightExp instanceof Variable && opNames.get(o.op)=="+")
				{
						for (int k = 0; k < ControlVar.size(); k++)
						{
							if(ControlVar.get(k).equals(rightExp))
								{
									//System.out.println("hiii");
									cons[j][k]= 1;
									break;
								}
						
						}
					
						for (int k = 0; k < UncontrolVar.size(); k++)
						{
							if(UncontrolVar.get(k).equals(rightExp))
							{
								//System.out.println("hiii");
								cons[j][k]= 1;
								break;
							}
						}
						
						
						rv += printExspeed(leftExp,j);
				//   System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
				}
					
					else
					{
						
						int myP = Operator.getPriority(op);
						if (left != null && left.children.size() > 1)
						{
							int leftP = Operator.getPriority(left.op);
							
							if (leftP < myP)
								needParenLeft = true;
						//	System.out.println(needParenLeft);
						}
						
						if (right != null && right.children.size() > 1)
						{
							int rightP = Operator.getPriority(right.op);
							
							if (myP > rightP || (myP == rightP && !Operator.isCommutative(op))) // commutative
								needParenRight = true;
							//System.out.println(needParenRight);
						}
						
						
						if (needParenLeft)
						{
							//System.out.println("Unexpected");
						   rv += "(" + printExspeed(leftExp,j) + ")"; 
						// maybe not strictly necessary as the expression.toString is overriden to call this print, but was having problems with this
						}
						else
							{
							rv += printExspeed(leftExp,j);
							//System.out.println("the left expression is:"+ print(leftExp));
							}
						
						rv += " " + opNames.get(o.op) + " ";
					//	System.out.println(opNames.get(o.op));
						if (needParenRight)
						{
							//System.out.println("Unexpected");
							rv += "(" + printExspeed(rightExp,j) + ")";
						}
						else
						{
							rv += printExspeed(rightExp,j);
							//System.out.println("the right expression is:"+ print(rightExp));
						}
						
					}
				
			}
			else
			{
				throw new AutomatonExportException("No default way to in-line print expression with " + children.size() 
						+ " children (" + opNames.get(o.op) + ".");
			}
			
			
			//System.out.println("The result is :"+ rv);
			return rv;
			
		}
		
		public double cons(int cons1, int cons2)
		{
			return cons[cons1][cons2];
				
		}
		
		public double Bmatrix(int cons1, int cons2)
		{
			return Bmatrix[cons1][cons2];
				
		}
		
		
		public double Cmatrix(int cons1)
		{
			return constraint[cons1];
				
		}
		
		
		
		
		
		
		public String printExspeedFirst(Expression e, int j, ArrayList<String>Control, ArrayList<String>Uncontrol)
		{
			String rv = null;
			ControlVar = Control;
			UncontrolVar = Uncontrol;
			//System.out.println("Inside "+j);
				if (e == null)
					rv = "null";
				else if (e instanceof Constant && Integer.parseInt(e.toString())!=0)
				{
				rv = e.toString();
				//System.out.println("constant :"+rv);
				saveCmatrix(rv,j);
				}
				else if (e instanceof Operation)
				{
			 	rv = printOperationExspeed((Operation) e,j);
				//System.out.println("operation "+rv);
				}
				else if (e instanceof Variable)
				{
					if(ControlVar.contains(e.toString()))
					{
					rv = printVariable((Variable) e);
					//System.out.println("variable "+rv);
					saveAmatrix(rv,j);
					}
					if(UncontrolVar.contains(e.toString()))
					{
						rv = printVariable((Variable) e);
						//System.out.println("unvariable "+rv);
						saveBmatrix(rv,j);
					}
				}
				else
				{
					rv = printOther(e);
					//System.out.println("other"+rv);
				}
			return rv;
			
				
		}
		
		
		
		
		public String printExspeed(Expression e, int j)
		{

				String rv = null;
				if (e == null)
					rv = "null";
				else if (e instanceof Constant)
				{
				rv = printConstant((Constant) e);
				//System.out.println("constant :"+rv);
				}
				else if (e instanceof Operation)
				{
			 	rv = printOperationExspeed((Operation) e,j);
				//System.out.println("operation "+rv);
				}
				else if (e instanceof Variable)
				{
					rv = printVariable((Variable) e);
				//	System.out.println("variable "+rv);
				}
				else
				{
					rv = printOther(e);
				//	System.out.println("other"+rv);
				}
			return rv;
				
		}
		public String printOther(Expression e)
		{
			return e.toString();
		}
		
		public String printVariable(Variable v)
		{
			return v.name;
		}
		
		public String printConstant(Constant c)
		{
			String rv = null;
			
			if (c == Constant.TRUE)
				rv = printTrue();
			else if (c == Constant.FALSE)
				rv = printFalse();
			else
				rv = printConstantValue(c.getVal());

			return rv;
		}
		
		public String printConstantValue(double d)
		{
			return "" + d;
		}
		
		public String printTrue()
		{
			return "true";
		}
		
		public String printFalse()
		{
			return "false";
		}
		
		// although Operators are technically not expressions, it's better to define this here to keep the printing all in once place

		
		/**
		 * Prefix printing for everything
		 * @param o
		 * @return
		 */
		public String printOperation(Operation o)
		{
			String childrenStr = "";
			
			for (Expression e : o.children)
			{
				//System.out.println("");
				//System.out.println("");
				childrenStr += " " + print(e);
			}
			return "(" + printOperator(o.op) + childrenStr + ")";
		}
		
		public void saveAmatrix(String stx,int j)
		{
			
			for (int k = 0; k < ControlVar.size(); k++)
			{
				if(ControlVar.get(k).equals(stx.toString()))
					{
					   index = k;
					   cons[j][index]= 1;
					   //System.out.println("cons["+j+"]["+index+"]"+cons[j][index]);
					   break;
					}
			}
			
			  // System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
			
		}
		
		public void saveCmatrix(String stl,int j)
		{
			
					   constraint[j]= Integer.parseInt(stl);
			
			
			  // System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
			
		}
		
		public void saveBmatrix(String stl,int j)
		{
			
			for (int k = 0; k < UncontrolVar.size(); k++)
			{
				if(UncontrolVar.get(k).equals(stl.toString()))
					{
					   index = k;
					   Bmatrix[j][index]= 1;
					   break;
					}
			}
			
			
			  // System.out.println("The constant in  cons["+j+"]["+index+"] is: "+ cons[j][index]);
			
		}
		
		
	
		
	}

	
	
	
	
	
	

	
//////////////////////////***********************************************	

	@Override
	protected void printAutomaton() {
		// First Entry to this Class from Hyst tool
		//System.out.println("I'm in xspeedprinter ---printautomation");
		this.ha = (BaseComponent) config.root;
		//System.out.println("print automaton 2");
		Expression.expressionPrinter = DefaultExpressionPrinter.instance;
		// Expression expressionPrinter1 = DefaultExpressionPrinter.instance;
		if (ha.modes.containsKey("init"))
			throw new AutomatonExportException(
					"mode named 'init' is not allowed in Flow* printer");

		if (config.init.size() > 1) {
			Hyst.log("Multiple initial modes detected (not supported by Flow*). Converting to single urgent one.");
			convertInitialModes(config);
		}
		
		AutomatonUtil.convertUrgentTransitions(ha, config);
		printDocument(originalFilename);
	}

	/**
	 * Use urgent modes to convert a configuration with multiple initial modes
	 * to one with a single initial mode
	 * 
	 * @param c
	 *            the configuration to convert
	 */
	public static void convertInitialModes(Configuration c) {
		final String INIT_NAME = "_init";
		BaseComponent ha = (BaseComponent) c.root;
		Collection<String> constants = ha.constants.keySet();

		AutomatonMode init = ha.createMode(INIT_NAME);
		init.invariant = Constant.TRUE;
		init.urgent = true;
		init.flowDynamics = null;

		for (Entry<String, Expression> e : c.init.entrySet()) {
			String modeName = e.getKey();
			AutomatonTransition at = ha.createTransition(init,
					ha.modes.get(modeName));
			at.guard = Constant.TRUE;

			Expression resetExp = removeConstants(e.getValue(), constants);

			TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();
			try {
				RangeExtractor.getVariableRanges(e.getValue(), ranges);
			} catch (EmptyRangeException e1) {
				System.out.println("XSpeed Can not able to handle those models");
				throw new AutomatonExportException(
						"Empty range in initial mode: " + modeName, e1);
			} catch (ConstantMismatchException e2) {
				System.out.println("XSpeed Can not able to handle those models");
				throw new AutomatonExportException(
						"Constant mismatch in initial mode: " + modeName, e2);
			}

			Collection<String> vars = AutomatonUtil
					.getVariablesInExpression(resetExp);

			for (String var : vars) {
				Interval i = ranges.get(var);

				if (i == null)
					throw new AutomatonExportException("Variable " + var
							+ " not defined in initial mode " + modeName);

				at.reset.put(var, new ExpressionInterval(new Constant(0), i));
			}
		}

		Expression firstReachableState = c.init.values().iterator().next();
		c.init.clear();
		c.init.put(INIT_NAME, firstReachableState);

		c.validate();
	}

	@Override
	public String getToolName() {
		return "Xspeed";
	}

	@Override
	public String getCommandLineFlag() {
		return "-xspeed";
	}

	@Override
	public boolean isInRelease() {
		return true;
	}

	public Map<String, String> getDefaultParams() {
		LinkedHashMap<String, String> toolParams = new LinkedHashMap<String, String>();

		toolParams.put("time", "auto");
		toolParams.put("step", "auto-auto");
		toolParams.put("remainder", "1e-4");
		toolParams.put("precondition", "auto");
		toolParams.put("plot", "auto");
		toolParams.put("orders", "3-8");
		toolParams.put("cutoff", "1e-15");
		toolParams.put("precision", "53");
		toolParams.put("jumps", "99999999");
		toolParams.put("print", "on");
		toolParams.put("aggregation", "parallelotope");

		return toolParams;
	}

	@Override
	public String getExtension() {
		return ".cpp";
	}
	
	
	
	//getsizeofInvX(Expression e) return the size of the invariant expression with number of variables available
	public int getsizeofInvX(Expression e)
	{
		//int p;	
		if (e instanceof Operation)
		{
			Operation o = (Operation) e;
			if (o.op == Operator.AND)
			{
				
				invSize = getsizeofInvX(o.getLeft())+ getsizeofInvX(o.getRight()) + 1;
				return invSize;
			}
			else
			{
				if (o.op == Operator.EQUAL)
				{	
					invSize = getsizeofInvX(o.getLeft())+ getsizeofInvX(o.getRight()) + 1;
					return invSize;
				}
				else
				{
				invSize= getsizeofInvX(o.getLeft())+ getsizeofInvX(o.getRight());
					return invSize;
				}
			}
		}
		else
		{
			if(UncontrolVar.contains(e.toString()))
				Uinvsize = Uinvsize+1;
			return 0;
		}
	}
	public int getsizeofInit(Expression e)
	{	
		if (e instanceof Operation)
		{
			Operation o = (Operation) e;
			if (o.op == Operator.AND)
			{
				
				invSize = getsizeofInvX(o.getLeft())+ getsizeofInvX(o.getRight()) + 1;
				return invSize;
			}
			else
			{
				invSize= getsizeofInvX(o.getLeft())+ getsizeofInvX(o.getRight());
					return invSize;
			}
		}
		else
			return 0;
	}
	
	public int CVarcontainX(String e)// it returns the index of the control variable list
	{
		int i;
		String[] st = new String[ControlVar.size()];
		st= ControlVar.toArray(st);
		for(i=0; i<ControlVar.size(); i++)
		{
			if(st[i].equals(e))
			{
				break;
			}
		}
		
		return i;
	}
	
	
	public int CVarcontainI(String e)// needed for guard polytope.
	{
		int i;
		String[] st = new String[ControlVarI.size()];
		st= ControlVarI.toArray(st);
		if(ControlVarI.size()==0)
			return -1;
		for(i=0; i<ControlVarI.size(); i++)
		{
			if(st[i].equals(e))
			{
				break;
			}
		}
		
		return i;
	}
	
	
	
	
	public int UVarcontainX(String e)//it returns the index of the Uncontrol variable list
	{
		int i;
		String[] st = new String[UncontrolVar.size()];
		st= UncontrolVar.toArray(st);
		for(i=0; i<UncontrolVar.size(); i++)
		{
			if(st[i].equals(e))
			{
				break;
			}
		}
		
		return i;
	}
	
	
	//It returns the ID of the location.
	public int ModeIdXspeed(AutomatonMode m)
	{
		int i=1;
		for(Entry<String,AutomatonMode> t: ha.modes.entrySet())
		{
			AutomatonMode mode = t.getValue();
			if(mode.hashCode() == m.hashCode())
				{
				break;
				}
			else
			{
				i++;
			}
		}
		return i;
	}
	
	
	//it returns the Id of the transition by taking the Label of the transition
	public int TransIdXspeed(String m)
	{
		int i=1;
		for(AutomatonTransition t: ha.transitions)
		{
			if(t.label == null)
			{
				throw new RuntimeException("XSpeed does not work on non-deterministic models\n");
			}
			String l = t.label;
			if(l.equals(m))
				{
				break;
				}
			else
			{
				i++;
			}
		}
		return i;
	}
	
	
	
	public void resetExtract(int ind,Operation op)
	{
			Operator o = op.op;
		
		if(o.equals(Operator.MULTIPLY))
		{
		int i = CVarcontainX(op.getRight().toString());
		Rmatrix[ind][i] = Double.parseDouble(op.getLeft().toString());
		}
		else
			if(o.equals(Operator.ADD) || o.equals(Operator.NEGATIVE))
			{
				if(op.getRight() instanceof Constant && o.equals(Operator.ADD))
					{
						Wmatrix[ind] = Double.parseDouble(op.getRight().toString());
						resetExtract(ind, (Operation)op.getLeft());
					}
				else
					if(op.getRight() instanceof Constant && o.equals(Operator.NEGATIVE))
						{
							Wmatrix[ind] = -Double.parseDouble(op.getRight().toString());
							resetExtract(ind, (Operation)op.getLeft());
						}
					else
					{
						resetExtract(ind, (Operation)op.getRight());
						resetExtract(ind, (Operation)op.getLeft());
					}
						
			}
			
	}
	
	public boolean ConstCheck(String e)
	{
		
		String [] st= new String[ha.constants.size()];
		st = ha.constants.keySet().toArray(st);
		for(int i=0;i<st.length;i++)
		{
			if(e.equals(st[i]))
			{
				return true;
			}
		}
		
		return false;
		
	}
	
}