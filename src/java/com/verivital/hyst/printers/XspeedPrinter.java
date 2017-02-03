/**
 * @Amit :: New Source Code with added Modification to support large Navigation Models
 */
package com.verivital.hyst.printers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matheclipse.core.eval.ExprEvaluator;//Symja
import org.matheclipse.core.interfaces.IExpr;//Symja

import com.verivital.hyst.geometry.Interval;
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
import com.verivital.hyst.main.Hyst;
import com.verivital.hyst.passes.basic.SubstituteConstantsPass;
import com.verivital.hyst.util.AutomatonUtil;
import com.verivital.hyst.util.PreconditionsFlag;
import com.verivital.hyst.util.RangeExtractor;
import com.verivital.hyst.util.RangeExtractor.ConstantMismatchException;
import com.verivital.hyst.util.RangeExtractor.EmptyRangeException;
import com.verivital.hyst.util.RangeExtractor.UnsupportedConditionException;

public class XspeedPrinter extends ToolPrinter {
	ExprEvaluator util = new ExprEvaluator(false, 100); // Hazel Added for symja library
	private BaseComponent ha;
	double[] invX;
	double[][] invY;
	double[] ConUX;// matrix that takes the Uncontrol variable ranges from
					// invariant
	double[][] ConUY; // matrix that takes the Uncontrol variable ranges from
						// invariant
	double[] guardX;
	double[][] guardY;
	double[][] Rmatrix;
	double[] Wmatrix;

	int invSize = 0;// size of total invariants
	int Uinvsize = 0;// size of uncontrol variable invariants
	int invindex = 0;// Used for indexing of invariant matrix
	int Uinvindex = 0; // used for indexing Uncontrol variable from invariants
	/***** Variables for module splitting ****/
	int module = 0; // to hold value of current module printing
	int[] index = new int[5]; // to hold the value of last location of each
								// module
	int div; // to hold size of each module

	/***** Done variables for module *****/
	public ArrayList<String> ControlVar = new ArrayList<String>();
	public ArrayList<String> UncontrolVar = new ArrayList<String>();

	public ArrayList<String> ControlVarI = new ArrayList<String>();// control
																	// and
																	// uncontrol
																	// variable
																	// matrix
																	// for
																	// inital
																	// state
	public ArrayList<String> UncontrolVarI = new ArrayList<String>();
	int Inid, forbid;
	String initname;// name of initial location

	public XspeedPrinter() {
		preconditions.skip[PreconditionsFlag.NO_URGENT.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.NO_NONDETERMINISTIC_DYNAMICS.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_NONDETERMINISTIC_RESETS.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_ALL_FLOWS_ASSIGNED.ordinal()] = true;
		preconditions.skip[PreconditionsFlag.CONVERT_DISJUNCTIVE_INIT_FORBIDDEN.ordinal()] = true;
	}

	String org;

	/*
	 * @Override protected String getCommentCharacter() { return "//"; }
	 */

	@Override
	protected String getCommentPrefix() {
		return "//";
	}

	/**
	 * This method starts the actual printing! Prepares variables etc. and calls
	 * printProcedure() to print the BPL code
	 */
	private void printDocument(String originalFilename) {
		this.printCommentHeader();
		org = new String(originalFilename);
		org = org.substring(org.lastIndexOf("/") + 1);
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
		Expression subbed = new SubstituteConstantsPass().substituteConstantsIntoExpression(ha.constants, ex);
		return AutomatonUtil.simplifyExpression(subbed);
	}

	/**
	 * ********************** ********************** Print the actual Flow* code
	 */
	private void printProcedure() {

		ControlVarI = ControlVar;
		UncontrolVarI = UncontrolVar;
		printInitial();
		if (ha.modes.size() > 5) {		//To divide output into modules
			div = ha.modes.size() / 5;
			index[0] = ha.modes.size() - (4 * div);
			index[1] = div + index[0];
			index[2] = div + index[1];
			index[3] = div + index[2];
			index[4] = div + index[3];
		} else {
			index[module] = -1;
		}
		int localLoc = 0;
		int count = 1, k, locCount = 1, tno1 = 1, invv = 0, pol = 0, trans = 0, loc = 1;
		for (Entry<String, AutomatonMode> e : ha.modes.entrySet()) {
			printNewline();
			int inc = 0;
			// modename
			boolean first = true;
			//////////////////inital location selected////////////////
			for (Entry<String, Expression> ex : config.init.entrySet()) {
				int id = 1;
				for (Entry<String, AutomatonMode> m : ha.modes.entrySet()) {
					AutomatonMode m1 = m.getValue();
					if (m1.name.equals(ex.getKey())) {
						Inid = id;
						initname = ex.getKey();
						break;
					}
					id++;
				}
				id = 0;
			}
			ControlVar.clear();//clear Control and uncontrol matrix for next location
			UncontrolVar.clear();
			AutomatonMode mode = e.getValue();
			printCommentBlock("The mode name is  " + mode.name); // Amit Edited
			String[] str = new String[ha.variables.size()];
			String[] str1 = new String[ha.constants.keySet().size()];
			str = ha.variables.toArray(str);
			str1 = ha.constants.keySet().toArray(str1);
			int j;
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet()) {//Add control matrix contents
				entry.toString();
				if (!entry.getValue().getExpression().toString().equals("0")
						|| !entry.getValue().getExpression().toString().equals("1")) {
					ControlVar.add(entry.getKey());
				}
			}


			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet()) {// add uncontrolled var entries to matrix
				UncontrolVar.clear();
				for (j = 0; j < ha.variables.size(); j++) {
					if (!(ControlVar.contains(str[j]))) {
						UncontrolVar.add(str[j]);
					}
				}
			}
			XspeedExpressionPrinter dep = new XspeedExpressionPrinter(str, str1, ControlVar.size(),
					UncontrolVar.size());
			if (first)
				first = false;
			else
				printNewline();

			if (mode.name.equals(initname))//if matched then save control and Uncontrol variable matrix
			{
				ControlVarI = ControlVar;
				UncontrolVarI = UncontrolVar;
			}
			for (Entry<String, ExpressionInterval> entry : mode.flowDynamics.entrySet()) {
				if (!UncontrolVar.contains(entry.getKey())) {
					ExpressionInterval ei = entry.getValue();
					String st = entry.getKey();
					for (j = 0; j <= ControlVar.size(); j++) {
						String st1 = ControlVar.get(j);
						if (st1.equals(st)) {
							break;
						}
					}

					/*************************************************************
					 * printExspeedFirst(simplifyExpression(ei.getExpression()),
					 * j, ControlVar, UncontrolVar) will output A matrix Bmatrix
					 * and Cmatrix content from flow equations...
					 ********************************************/
					String test = simplifyExpression(ei.getExpression()).toString(); 
					test=test.replaceAll("\\s","");
					int tr,phlag=0,phlag2=0;
					for(tr=0;tr<=test.length()-1;tr++){
						if(test.indexOf('(')==(-1) && test.charAt(tr)=='-' && ((test.charAt(tr+1)>='a' && test.charAt(tr+1)<='z') || (test.charAt(tr+1)>='A' && test.charAt(tr+1)<='Z'))){
							phlag=1;
							break;
						}
					}
					int flag = ei.getExpression().toString().indexOf('('); //To check for brackets
					int flagg = 0;
					if (flag != (-1) || phlag==1) {
						String exp = simplifyExpression(ei.getExpression()).toString();
						exp = exp.replaceAll("\\s", "");
						String exp2 = "";
						String h;
						if (test.indexOf('(') != (-1)) {
							phlag=0;
							for (int r = 0; r < (exp.length()); r++) {
								if (r == exp.length() - 1)
									exp2 += exp.charAt(r);
								else {
									if (exp.charAt(r) == '+' && exp.charAt(r + 1) == '-') {
										exp2 += '-';
										r++;
									} else if (exp.charAt(r) == '-' && exp.charAt(r + 1) == '-') {
										exp2 += '+';
										r++;
									} else
										exp2 += exp.charAt(r);
								}
							}
							String exp3 = "";
							IExpr result = util.evaluate("Expand(" + exp2 + ")");
							h = result.toString();
						} else if(phlag==1)
							h=test;
						else
							h = exp;
						String number = "";
						for (int i = 0; i < h.length(); i++) {

							if (i == 0 && h.charAt(0) == '-') {
								number += '-';
								i++;
							}
							if (h.charAt(i) == '-' && i != 0) {
								number += '-';
								i++;
							}
							if (h.charAt(i) == '+' && i != 0) {
								i++;
							}
							while ((h.charAt(i) >= '0' && h.charAt(i) <= '9') || h.charAt(i) == '.') {
								number += h.charAt(i);
								i++;
								if (i == h.length()) {
									flagg = 1;
									break;
								}
							}
							if (flagg == 1)
								break;
							String var = "";
							int flag2=0;
							if (i > 0 && h.charAt(i) != '.' && !(h.charAt(i) >= '0' && h.charAt(i) <= '9')) {
								if (h.charAt(i) == '*' && ((h.charAt(i + 1) >= 'a' && h.charAt(i + 1) <= 'z')
												|| (h.charAt(i + 1) >= 'A' && h.charAt(i + 1) <= 'Z'))) {
									i++;
									while(i!=h.length() && (h.charAt(i)!='+' && h.charAt(i)!='-'))
									{
										var+=h.charAt(i);
										i++;
									}
									for (k = 0; k < ControlVar.size(); k++) {
										if (ControlVar.get(k).equals(var)) {
											dep.saveAmatrix2(var, j, k, Double.parseDouble(number));
										}
									}
									for (k = 0; k < UncontrolVar.size(); k++) {
										if (UncontrolVar.get(k).equals(var)) {
											dep.saveBmatrix2(var, j, k, Double.parseDouble(number));
											break;
										}
									}
									number = "";
									var = null;
								} else if (h.charAt(i) == '-' || h.charAt(i) == '+') {
									dep.saveCmatrix2(j, Double.parseDouble(number));
									number = "";
									i--;
								}
								if(i==h.length())
									break;
							}
							while((h.charAt(i)>='a' && h.charAt(i)<='z') || (h.charAt(i)>='A' && h.charAt(i)<='Z') || (h.charAt(i)=='-' || h.charAt(i)=='+')){
								String va="";String num="",num2="";
								if(h.charAt(i)=='-' || h.charAt(i)=='+'){
									if((h.charAt(i+1)>='a' && h.charAt(i+1)<='z')|| (h.charAt(i+1)>='A' && h.charAt(i+1)<='Z')){
										if(h.charAt(i)=='-')
											num+='-';
										i++;
									}	
									else if((h.charAt(i+1)>='0' && h.charAt(i+1)<='9') || h.charAt(i+1)=='.'){
										i--;
										break;
									}
								}
								else if(i!=0 && h.charAt(i-1)=='-'){
									va+=h.charAt(i);
									num+='-';
									i++;
								}
								int ko=0;
								do{
									flag2=0;
									va+=h.charAt(i);
									if(i==h.length()-1){
										if(num.length()==1){
										if(num.charAt(0)=='-'){
											for (k = 0; k < ControlVar.size(); k++) {
												if (ControlVar.get(k).equals(va)) {
													dep.saveAmatrix2(va, j, k, -1);
													break;
												}
											}
											for (k = 0; k < UncontrolVar.size(); k++) {
												if (UncontrolVar.get(k).equals(va)) {
													dep.saveBmatrix2(va, j, k, -1);
													break;
												}
											}
										}
									}
									else if(num.length()==0){
										for (k = 0; k < ControlVar.size(); k++) {
											if (ControlVar.get(k).equals(va)) {
												dep.saveAmatrix2(va, j, k, 1);
												break;
											}
										}
										for (k = 0; k < UncontrolVar.size(); k++) {
											if (UncontrolVar.get(k).equals(va)) {
												dep.saveBmatrix2(va, j, k, 1);
												break;
											}
										}
									}
									flag2=0;
									phlag2=1;
									flagg=1;
									break;
								}
								if(num.length()==1){
								if(num.charAt(0)=='-'){
									for (k = 0; k < ControlVar.size(); k++) {
										if (ControlVar.get(k).equals(va)) {
											dep.saveAmatrix2(va, j, k, -1);
											break;
										}
									}
									for (k = 0; k < UncontrolVar.size(); k++) {
										if (UncontrolVar.get(k).equals(va)) {
											dep.saveBmatrix2(va, j, k, -1);
											break;
										}
									}
								}
							}
							if(!(h.charAt(i+1)=='+' || h.charAt(i+1)=='-' || h.charAt(i+1)=='/' || h.charAt(i+1)=='*'))
							{
								flag2=1;
								i++;
							}
							else{
								i++;
								if(i>0 && h.charAt(0)=='-' && num.length()==0)
									num+='-';
								if(h.charAt(i)=='/')
								{
									i++;
									num="";
									num2="";
									if(i>0 && h.charAt(0)=='-' && num.length()==0)
										num+='-';
									while((h.charAt(i)>='0' && h.charAt(i)<='9')|| h.charAt(i)=='.'){
										num+=h.charAt(i);
										i++;
										if(i==h.length())
										{
											phlag2=1;
											break;
										}
										if(h.charAt(i)=='*'){
											i++;
										while((h.charAt(i)>='0' && h.charAt(i)<='9')|| h.charAt(i)=='.'){
											num2+=h.charAt(i);
											i++;
											if(i==h.length())
											{
												phlag2=1;
												flag2=0;
												break;
											}
										}
										double d;
										if(num2!=null)
										{
											d = Double.parseDouble(num2)/Double.parseDouble(num);
										}
										else
											d = Double.parseDouble(num);
										for (k = 0; k < ControlVar.size(); k++) {
											if (ControlVar.get(k).equals(va)) {
												dep.saveAmatrix2(va, j, k, d);
											}
										}
										for (k = 0; k < UncontrolVar.size(); k++) {
											if (UncontrolVar.get(k).equals(va)) {
												dep.saveBmatrix2(va, j, k, d);
												break;
											}
										}
									}
									else if(h.charAt(i)=='-' || h.charAt(i)=='+'){
													for (k = 0; k < ControlVar.size(); k++) {
														if (ControlVar.get(k).equals(va)) {
															ko=1;
															dep.saveAmatrix2(va, j, k, 1.0/Double.parseDouble(num));
															break;
														}
													}
													for (k = 0; k < UncontrolVar.size(); k++) {
														if (UncontrolVar.get(k).equals(va)) {
															ko=1;
															dep.saveBmatrix2(va, j, k, 1.0/Double.parseDouble(num));
															break;
														}
													}
												}
												if(phlag2==1){
													flag2=0;
													break;
												}
											}
										}
										if(i!=h.length())
										if(h.charAt(i)=='*' ){
											i++;
											while(h.charAt(i)=='.' || (h.charAt(i)>='0' && h.charAt(i)<='9')){
												num+=h.charAt(i);
												i++;
												if((i+1)==h.length()){
													num+=h.charAt(i);
													break;
												}
											}
											for (k = 0; k < ControlVar.size(); k++) {
												if (ControlVar.get(k).equals(va)) {
													dep.saveAmatrix2(va, j, k, Double.parseDouble(num));
												}
											}
											for (k = 0; k < UncontrolVar.size(); k++) {
												if (UncontrolVar.get(k).equals(va)) {
													dep.saveBmatrix2(va, j, k, Double.parseDouble(num));
													break;
												}
											}
										}
										else if(h.charAt(i)=='-' && ko==0){
											for (k = 0; k < ControlVar.size(); k++) {
												if (ControlVar.get(k).equals(va)) {
													dep.saveAmatrix2(va, j, k, 1);
												}
											}
											for (k = 0; k < UncontrolVar.size(); k++) {
												if (UncontrolVar.get(k).equals(va)) {
													dep.saveBmatrix2(va, j, k, 1);
													break;
												}
											}
										}
										//here you can do else if for all others like + - (Might not be required)
									}
								}
								while(flag2==1);
								if(phlag2==1)
									break;
							}
							if (flagg == 1)
								break;
						}
					}
					else {
						dep.printExspeedFirst(simplifyExpression(ei.getExpression()), j, ControlVar, UncontrolVar);
						ei.setExpression(simplifyExpression(ei.getExpression()));
					}
				}
			}
			int flg = 0;
			if (ha.variables.size() != 0) {
				printLine("row = " + ControlVar.size() + ";");
				printLine("col = " + ControlVar.size() + ";");
				for (j = 0; j < ControlVar.size(); j++) {
					for (k = 0; k < ControlVar.size(); k++) {
						if (dep.cons(j, k) != 0.0)
							flg = 1;
					}
				}
				if (flg == 1) {
					printLine("Amatrix.resize(row, col);");
					printLine("Amatrix.clear();");
					for (j = 0; j < ControlVar.size(); j++) {
						for (k = 0; k < ControlVar.size(); k++) {
							if (dep.cons(j, k) != 0)
								printLine("Amatrix(" + j + " , " + k + ") = " + dep.cons(j, k) + ";");
						}
					}
					printLine("system_dynamics.isEmptyMatrixA = false;");
					printLine("system_dynamics.MatrixA = Amatrix;");
				} else if (flg == 0)
					printLine("system_dynamics.isEmptyMatrixA = true;");

			} else {
				printLine("system_dynamics.isEmptyMatrixA = true;");
			}
			// ////////////// A matrix print complete /////////////////
			printNewline();
			flg = 0;
			if (ControlVar.size() != 0 && UncontrolVar.size() != 0) {
				printLine("col = " + UncontrolVar.size() + ";");
				printLine("Bmatrix.resize(row, col);");
				printLine("Bmatrix.clear();");
				for (j = 0; j < ControlVar.size(); j++) {
					for (k = 0; k < UncontrolVar.size(); k++) {
						if (dep.Bmatrix(j, k) != 0.0)
							flg = 1;
					}
				}
				if (flg == 1) {
					for (j = 0; j < ControlVar.size(); j++) {
						for (k = 0; k < UncontrolVar.size(); k++) {
							if (dep.Bmatrix(j, k) != 0)
								printLine("Bmatrix(" + j + " , " + k + ") = " + dep.Bmatrix(j, k) + ";");
						}
					}
					printLine("system_dynamics.isEmptyMatrixB = false;");
					printLine("system_dynamics.MatrixB = Bmatrix;");
				} else if (flg == 0)
					printLine("system_dynamics.isEmptyMatrixB = true;");
			} else {
				printLine("system_dynamics.isEmptyMatrixB = true;");
			}
			// ///////////// B matrix print complete /////////////////
			printNewline();
			flg=0;
			if (ControlVar.size() != 0) {
				for (j = 0; j < ControlVar.size(); j++) {
					if (dep.Cmatrix(j) != 0){
						flg=1;
						break;
					}
				}
				if(flg==1){
					printLine("C.resize(row );");
					printLine("C.assign(row,0);");
					for (j = 0; j < ControlVar.size(); j++) {
						if (dep.Cmatrix(j) != 0){
							printLine("C[" + j + "] = " + dep.Cmatrix(j) + ";");
						}
					}
					printLine("system_dynamics.isEmptyC = false;");
					printLine("system_dynamics.C = C;");
				}
			}if(flg==0){
				printLine("system_dynamics.isEmptyC = true;");
			}
			printNewline();
			// C vector print complete invariant //
			invSize = 0;
			Uinvsize = 0;
			invindex = 0;
			Uinvindex = 0;
			Expression inv = simplifyExpression(mode.invariant);
			if (!inv.equals(Constant.TRUE)) {
				int xl = getsizeofInvX(inv) + 1;
				invX = new double[xl];
				invY = new double[xl][ControlVar.size()];
				ConUX = new double[xl];
				ConUY = new double[xl][UncontrolVar.size()];
				for (j = 0; j < xl; j++) {
					for (k = 0; k < ControlVar.size(); k++)
						invY[j][k] = 0;
					invX[j] = 0;
				}
				for (j = 0; j < xl; j++) {
					for (k = 0; k < UncontrolVar.size(); k++)
						ConUY[j][k] = 0;
					ConUX[j] = 0;
				}
				printNewline();
				getFlowConditionExpressionXspeed(inv);
				if (xl - Uinvsize != 0) {
					printLine("row = " + (xl-Uinvsize) + ";");
					printLine("col = " + ControlVar.size() + ";");
					printLine("invariantConstraintsMatrix.resize(row, col);");
					printLine("invariantConstraintsMatrix.clear();");
					for (j = 0; j < xl; j++) {
						for (k = 0; k < ControlVar.size(); k++)
							if (invY[j][k] != 0)
								printLine("invariantConstraintsMatrix(" + j + "," + k + ")= " + invY[j][k] + ";");
					}
					printNewline();
					printLine("invariantBoundValue.resize(row);");
					printLine("invariantBoundValue.assign(row,0);");
					for (j = 0; j < xl; j++) {
						if (invX[j] != 0)
							printLine("invariantBoundValue[" + j + "] = " + invX[j] + ";");
					}
					printLine(
							"invariant = polytope::ptr(new polytope(invariantConstraintsMatrix, invariantBoundValue,invariantBoundSign));");
					invv++;
				} else {
					printLine("invariant = polytope::ptr(new polytope());");
					printLine("invariant -> setIsUniverse(true);");
				}
				printNewline();
				// printing ConstraintU matrix////
				if (UncontrolVar.size() != 0) {
					printLine("row = " + Uinvsize + ";");
					printLine("col = " + UncontrolVar.size() + ";");
					printLine("ConstraintsMatrixV.resize(row, col);");
					for (j = 0; j < Uinvsize; j++) {
						for (k = 0; k < UncontrolVar.size(); k++)
							if (ConUY[j][k] != 0)
								printLine("ConstraintsMatrixV(" + j + "," + k + ")= " + ConUY[j][k] + ";");
					}
					printNewline();
					printLine("boundValueV.resize(row);");
					for (j = 0; j < Uinvsize; j++) {
						if (ConUX[j] != 0)
							printLine("boundValueV[" + j + "] = " + ConUX[j] + ";");
					}
					printLine(
							"system_dynamics.U = polytope::ptr(new polytope(ConstraintsMatrixV, boundValueV, boundSignV));");
				} else {
					printLine("system_dynamics.U = polytope::ptr(new polytope(true));");
				}

				invSize = 0;
				Uinvsize = 0;
				invindex = 0;
				Uinvindex = 0;
				printNewline();
			}
			printNewline();
			mode = e.getValue();
			printLine("std::list<transition::ptr> Out_Going_Trans_from" + mode.name + ";");
			printNewline();
			int from = 0;
			for (AutomatonTransition t : ha.transitions) {
				if (t.from == mode) {
					tno1++;
					//print the transitions
					first = true;
					inc = 0;
					printCommentBlock("The transition label is " + t.label); // Amit Edited
					Rmatrix = new double[ControlVar.size()][ControlVar.size()];
					Wmatrix = new double[ControlVar.size()];
					Expression guard = simplifyExpression(t.guard);
					int xl = getsizeofInvX(guard) + 1;
					guardX = new double[xl];
					guardY = new double[xl][ControlVar.size()];
					if (guard == Constant.FALSE)
						continue;
					if (first)
						first = false;
					else
						printNewline();
					for (int i = 0; i < xl; i++)
						for (k = 0; k < ControlVar.size(); k++)
							guardY[i][k] = 0;
					for (int i = 0; i < xl; i++)
						guardX[i] = 0;
					if (!guard.equals(Constant.TRUE)) {
						printCommentBlock("Original guard: " + t.guard); // Amit Edited
						getguardConditionExpressionXspeed(guard);
					}
					if (guard.toString().equalsIgnoreCase("true")) {
						printLine("guard_polytope = polytope::ptr(new polytope());");
						printLine("guard_polytope -> setIsUniverse(true);");
						pol++;
					} else if (guard.toString().equalsIgnoreCase("false")) {
						printLine("guard_polytope = polytope::ptr(new polytope(true));");
						pol++;
					} else {
						if (xl != 0) {
							printLine("row = " + xl + ";");
							printLine("col = " + ControlVar.size() + ";");
							printNewline();
							printLine("guardConstraintsMatrix.resize(row, col);");
							printLine("guardConstraintsMatrix.clear();");
							for (int i = 0; i < xl; i++)
								for (k = 0; k < ControlVar.size(); k++)
									if (guardY[i][k] != 0)
										printLine(
												"guardConstraintsMatrix(" + i + "," + k + ") = " + guardY[i][k] + ";");
							printNewline();
							printLine("guardBoundValue.resize(row);");
							printLine("guardBoundValue.assign(row,0);");
							for (int i = 0; i < xl; i++)
								if (guardX[i] != 0)
									printLine("guardBoundValue[" + i + "] = " + guardX[i] + ";");
							printLine(
									"guard_polytope = polytope::ptr(new polytope(guardConstraintsMatrix, guardBoundValue, guardBoundSign));");
							pol++;
						} else {
							printLine(
									"guard_polytope = polytope::ptr(new polytope(guardConstraintsMatrix, guardBoundValue, guardBoundSign));");
							printLine("guard_polytope -> setIsUniverse(true);");
							pol++;
						}
					}
					printNewline();
					printNewline();
					invSize = 0;
					for (int i = 0; i < ControlVar.size(); i++)
						for (k = 0; k < ControlVar.size(); k++) {
							if (i == k) {
								Rmatrix[i][k] = 1;
							} else
								Rmatrix[i][k] = 0;
						}
					for (String key : t.reset.keySet()) {
						int re = CVarcontainX(key);
						Expression eee = t.reset.get(key).getExpression();
						if (eee.toString().indexOf(key.toString()) == (-1) && !(eee.toString().equals("0"))) {
							Rmatrix[re][re] = 0;
						}
						for (int i = 0; i < ControlVar.size(); i++)
							Wmatrix[i] = 0;
						if(eee.toString().indexOf('(')!=(-1)){
							IExpr result = util.evaluate("Expand(" + eee + ")");
							String p=result.toString();
							for(int i=0;i<p.length();i++){
								String num="",var="";
								if(p.charAt(i)=='-')
								{
									num+='-';
									i++;
								}
								if(p.charAt(i)=='+')
									i++;
								if(p.charAt(i)>='0' && p.charAt(i)<='9'){
									while((p.charAt(i)>='0' && p.charAt(i)<='9') || p.charAt(i)=='.'){
										num+=p.charAt(i);
										//i++;
										if(i+1 <p.length())
											i++;
										else
											break;
									}
								}
								if(p.charAt(i)=='*'){
									i++;
									while(p.charAt(i)!='-' && p.charAt(i)!='+'){
										var+=p.charAt(i);
										i++;
										if(i==p.length()){
											if(var.equals("vx"))
												var="Vx";
											if(var.equals("vy"))
												var="Vy";
											Rmatrix[re][CVarcontainX(var)] = Double.parseDouble(num);
											break;
										}
									}
								}
								if(var.equals("vx"))
									var="Vx";
								if(var.equals("vy"))
									var="Vy";
								if(var.length()!=0)
								{
									Rmatrix[re][CVarcontainX(var)] = Double.parseDouble(num);
									i--;
								}
							}
						}
						if (eee instanceof Operation) {
							eee = simplifyExpression(eee);
							Operation o = (Operation) eee;
							;
							// System.out.println(" Operator is
							// "+Expression.expressionPrinter.printOperator(o.op));
							if (o.getRight() instanceof Variable) { 
								if (o.op == Operator.MULTIPLY)
								{
									if (o.getLeft() instanceof Constant)
										Rmatrix[re][CVarcontainX(o.getRight().toString())] = Double
												.parseDouble(o.getLeft().toString());
								} else if (o.op == Operator.ADD) {
									if (o.getLeft() instanceof Variable) {
										Rmatrix[re][CVarcontainX(o.getRight().toString())] = 1;
										Rmatrix[re][CVarcontainX(o.getLeft().toString())] = 1;
									}
								} else if (o.op == Operator.SUBTRACT) {
									if (o.getLeft() instanceof Variable) {
										Rmatrix[re][CVarcontainX(o.getRight().toString())] = -1;
										Rmatrix[re][CVarcontainX(o.getLeft().toString())] = 1;
									}
								}
							} else if (o.op == Operator.DIVIDE) {
								if (o.getRight() instanceof Constant) {
									Operation oo = (Operation) o.getLeft();
									if (oo.op == Operator.ADD) {
										Rmatrix[re][CVarcontainX(oo.getLeft().toString())] = 1
												/ (Double.parseDouble(o.getRight().toString()));
										Rmatrix[re][CVarcontainX(oo.getRight().toString())] = 1
												/ (Double.parseDouble(o.getRight().toString()));
									}
								} else if (o.getRight() instanceof Variable) {
									// Do the necessary
								}
							}
							else if(o.getLeft() instanceof Variable && o.getRight() instanceof Constant){
								if (o.op == Operator.ADD) {
									Rmatrix[re][CVarcontainX(o.getLeft().toString())] = 1;
											
									Wmatrix[re]=Double.parseDouble(o.getRight().toString());
								}
								if(o.op == Operator.SUBTRACT){
									Rmatrix[re][CVarcontainX(o.getLeft().toString())] = 1;
									
									Wmatrix[re]=-Double.parseDouble(o.getRight().toString());
								}
							}
							else
								resetExtract(re, o);
						} else if (eee instanceof Variable) {
							Rmatrix[re][CVarcontainX(eee.toString())] = 1;
						} else if (eee instanceof Constant) {
							Rmatrix[re][re] = 0.0;
							Wmatrix[re] = Double.parseDouble(eee.toString());
						}
					}
					printLine("row = " + ControlVar.size() + ";");
					printLine("col = " + ControlVar.size() + ";");
					printLine("R.resize(row, col);");
					printLine("R.clear();");
					for (int i = 0; i < ControlVar.size(); i++)
						for (k = 0; k < ControlVar.size(); k++)
							if (Rmatrix[i][k] != 0)
								printLine("R(" + i + "," + k + ") =  " + Rmatrix[i][k] + ";");
					if (localLoc == 0) {
						printLine("std::vector<double> w(row);");
					}
					printLine("w.assign(row,0);");
					for (int i = 0; i < ControlVar.size(); i++) {
						if (Wmatrix[i] != 0)
							printLine("w[" + i + "] = " + Wmatrix[i] + ";");
					}
					printNewline();
					printNewline();
					printLine("assignment.Map = R;");
					printLine("assignment.b = w;");
					printNewline();
					printLine("t = transition::ptr(new" + " transition(" + (trans + 1) + ",\"" + t.label + "\","
							+ ModeIdXspeed(t.from) + "," + ModeIdXspeed(t.to) + ",guard_polytope,assignment));");
					trans++;
					localLoc++;
					printNewline();

					if (config.forbidden.entrySet().size() > 0) {
						System.out.println("Forbidden entry = " + config.forbidden.entrySet().size());
						printforbidden();
					}

					//end print transitions
					from = from + 1;
					printLine("Out_Going_Trans_from" + mode.name + ".push_back(t);");
				}
			}
			if (from == 0)
				printNewline();
			inv = simplifyExpression(mode.invariant);
			int xl = getsizeofInvX(inv) + 1;
			if (xl - Uinvsize != 0 || (mode.name.equals("GOOD") || mode.name.equals("BAD"))) {
				printLine("l = location::ptr(new location" + "(" + locCount + ", \"" + mode.name
						+ "\", system_dynamics, invariant, true, Out_Going_Trans_from" + mode.name + "));");
			} else
				printLine("l =location::ptr(new location(" + locCount + ", \"" + mode.name
						+ "\", system_dynamics, invariant, false, Out_Going_Trans_from" + mode.name + "));");
			mode = e.getValue();

			if (locCount == 1) {
				printNewline();
				printLine("Hybrid_Automata.addInitial_Location(l);");
			}

			printLine("Hybrid_Automata.addLocation(l);");
			printNewline();
			loc++;
			if (locCount == index[module]) {
				if (module == 0) {
					printLine("module1(Hybrid_Automata,init_state_list,reach_parameters,op);");
					printLine("module2(Hybrid_Automata,init_state_list,reach_parameters,op);");
					printLine("module3(Hybrid_Automata,init_state_list,reach_parameters,op);");
					printLine("module4(Hybrid_Automata,init_state_list,reach_parameters,op);");
					printInitialStatesExspeed();
					printForbiddenSection();
				}
				module++;
				localLoc = 0;
				printLine("}");
				printNewline();
				if (module < 5)
					printInitial();
			}
			locCount++;
		}
		if (module == 0) {
			printInitialStatesExspeed();
			printForbiddenSection();
			printLine("}");
		}
	}

	private void printInitial() {
		if (module == 0) {
			printLine("#include \"Hybrid_Model_Parameters_Design/user_model/user_model.h\"");
			printLine(
					"void user_model(hybrid_automata& Hybrid_Automata, std::list<initial_state::ptr>& init_state_list, ReachabilityParameters& reach_parameters, userOptions& op) {");
		}
		else
			printLine("void module" + module
					+ "(hybrid_automata& Hybrid_Automata, std::list<initial_state::ptr>& init_state_list, ReachabilityParameters& reach_parameters, userOptions& op) {");
		printNewline();
		printNewline();
		printLine("typedef typename boost::numeric::ublas::matrix<double>::size_type size_type;");
		printNewline();
		printNewline();
		printLine("unsigned int dim;");
		printLine("size_type row, col;");
		printNewline();
		print("polytope::ptr ");
		int i = 0;
		for (Entry<String, Expression> e : config.init.entrySet()) {
			Operation o = e.getValue().asOperation();
			while (o.op == Operator.OR) {
				print("initial_polytope_I" + i + ",");
				i++;
				Expression left = o.getLeft();
				o = left.asOperation();
			}
			print("initial_polytope_I" + i + ",");
			i++;
		}
		print(" forbid_polytope;");
		printNewline();
		printLine("location::ptr l;");
		printLine("transition::ptr t;");
		print("polytope::ptr invariant");
		printLine(";");
		printNewline();
		print("polytope::ptr guard_polytope");
		printLine(";");
		printNewline();
		print("Dynamics system_dynamics");
		printLine(";");
		printNewline();
		print("math::matrix<double> ConstraintsMatrixI , ");
		print("ConstraintsMatrixV, ");
		print("invariantConstraintsMatrix , ");
		print("guardConstraintsMatrix , ");
		print("Amatrix , ");
		print("Bmatrix");
		print(",forbiddenMatrixI");
		printLine(";");
		printNewline();
		print("std::vector<double> boundValueI,");
		print("boundValueV , ");
		print("C , ");
		print("invariantBoundValue , ");
		print("guardBoundValue");
		print(", boundValueF");
		printLine(";");
		printNewline();
		printLine("int boundSignI=1, invariantBoundSign=1, guardBoundSign=1, boundSignV=1;");
		printNewline();
		printLine("Assign assignment;");
		printLine("math::matrix<double> R;");
	}

	public void printForbiddenSection() {
		// printLine("// ************* Section for handling Input from cfg *************");
		printLine("// ************* Section required for setting Reach Parameters & User Options *************");
		String user_dirs;
		user_dirs = config.settings.spaceExConfig.directions;
		if (user_dirs.equalsIgnoreCase("box"))
			printLine("unsigned int Directions_Type = 1;");
		if (user_dirs.equalsIgnoreCase("oct"))
			printLine("unsigned int Directions_Type = 2;");

		if (user_dirs.startsWith("uni"))
		{
			int num_dirs;
			String str_num = user_dirs.substring(3);
			num_dirs = Integer.parseInt(str_num);
			printLine("unsigned int Directions_Type = " + num_dirs + ";");
		}
		printLine("unsigned int iter_max = " + config.settings.spaceExConfig.maxIterations + ";");
		printLine("double time_horizon = " + config.settings.spaceExConfig.timeHorizon + "; ");
		printLine("double sampling_time = " + config.settings.spaceExConfig.samplingTime + ";");
		printLine("std::vector<std::string> output_variables;");
		printLine("output_variables.push_back(\"" + config.settings.spaceExConfig.outputVars.get(0) + "\");");
		printLine("output_variables.push_back(\"" + config.settings.spaceExConfig.outputVars.get(1) + "\");");
		if (config.settings.spaceExConfig.outputVars.size() == 3)
			printLine("output_variables.push_back(\"" + config.settings.spaceExConfig.outputVars.get(2) + "\");");
		printNewline();
		printLine("op.set_timeStep(sampling_time)" + ";");
		printLine("op.set_timeHorizon(time_horizon)" + ";");
		printLine("op.set_bfs_level(iter_max)" + ";");
		printLine("op.set_directionTemplate(Directions_Type)" + ";");
		printNewline();
		printLine("int x1 = Hybrid_Automata.get_index(output_variables[0]);");
		printLine("int x2 = Hybrid_Automata.get_index(output_variables[1]);");
		printLine("op.set_first_plot_dimension(x1);");
		printLine("op.set_second_plot_dimension(x2);");
		if (config.settings.spaceExConfig.outputVars.size() == 3) {
			printLine("int x3 = Hybrid_Automata.get_index(output_variables[2]);");
			printLine("op.set_third_plot_dimension(x3);");
		}
		printLine("reach_parameters.TimeBound = op.get_timeHorizon();");
		printLine("reach_parameters.Iterations = (unsigned int) op.get_timeHorizon()/ op.get_timeStep();");
		printLine("reach_parameters.time_step = op.get_timeStep();");
		printNewline();
		printLine("std::vector<std::vector<double> > newDirections;");
		printLine("math::matrix<double> Real_Directions;");
		printLine("unsigned int dir_nums;");
		printLine("if (Directions_Type == BOX) {");
		printLine("\t dir_nums = 2 * dim;");
		printLine("\t newDirections = generate_axis_directions(dim);");
		printLine("}");
		printLine("if (Directions_Type == OCT) {");
		printLine("\t dir_nums = 2 * dim * dim;");
		printLine("\t newDirections = get_octagonal_directions(dim);");
		printLine("}");
		printLine("if (Directions_Type > 2) {");
		printLine("\t dir_nums = Directions_Type;");
		printLine("\t newDirections = math::uni_sphere(dir_nums, dim, 100, 0.0005);");
		printLine("}");

		printLine("get_ublas_matrix(newDirections, Real_Directions); ");
		printLine("row = dir_nums;");
		printLine("col = dim;");
		printLine("reach_parameters.Directions.resize(row, col);");
		printLine("reach_parameters.Directions = Real_Directions;");
	}

	// ////////////////////////////////////////////************************************
	// printing forbidden states and polytopes
	//
	// ////////////////////////////////////////////***************************************
	private void printforbidden() {

		printNewline();
		printLine("std::pair<int, polytope::ptr> forbid_pair;");
		printLine("row = " + (2 * (ControlVar.size())) + ";");
		printLine("col = " + ControlVar.size() + ";");
		printLine("forbiddenMatrixI.resize(row, col);");

		for (Entry<String, Expression> e : config.forbidden.entrySet()) {

			printforbiddenXspeed(removeConstants(e.getValue(), ha.constants.keySet()), true, 2 * ControlVar.size());
		}
	}

	private void printInitialStatesExspeed() {
		printNewline();
		printLine("row = " + (2 * (ControlVar.size())) + ";");
		printLine("col = " + ControlVar.size() + ";");
		printLine("ConstraintsMatrixI.resize(row, col);");
		printLine("ConstraintsMatrixI.clear();");
		int i = 0;
		int noOfPolytopesOfLoc = 1;
		Expression ee;
		for (Entry<String, Expression> e : config.init.entrySet()) {
			Operation o = e.getValue().asOperation();
			ee = e.getValue();
			while (o != null && o.children.size() == 2 && o.op == Operator.OR) {
				Expression leftExp = o.children.get(0);
				Expression rightExp = o.children.get(1);
				printFlowRangeConditionsExspeed(removeConstants(rightExp, ha.constants.keySet()), true,
						2 * ControlVar.size());
				printNewline();
				printLine("initial_polytope_I" + i
						+ " = polytope::ptr(new polytope(ConstraintsMatrixI, boundValueI, boundSignI));");
				printNewline();
				printLine("dim = initial_polytope_I" + i + "->getSystemDimension();");
				if (i == 0) {
					printLine("int transition_id = 0;");
					printLine("unsigned int initial_location_id =" + Inid + ";");
				}
				printNewline();
				printLine("symbolic_states::ptr S" + i + ";");
				printNewline();
				printLine("initial_state::ptr I" + i
						+ " = initial_state::ptr(new initial_state(initial_location_id, initial_polytope_I" + i + ", S"
						+ i + ", transition_id));");
				printNewline();
				printLine("init_state_list.push_back(I" + i + ");");
				o = leftExp.asOperation();
				if (o.op != Operator.OR)
					ee = leftExp;
				i++;
			}
			printFlowRangeConditionsExspeed(removeConstants(ee, ha.constants.keySet()), true, 2 * ControlVar.size());
			printNewline();
			printLine("initial_polytope_I" + i
					+ " = polytope::ptr(new polytope(ConstraintsMatrixI, boundValueI, boundSignI));");
			printNewline();
			printLine("dim = initial_polytope_I" + i + "->getSystemDimension();");
			if (i == 0) {
				printLine("int transition_id = 0;");
				printLine("unsigned int initial_location_id =" + Inid + ";");
			}
			printNewline();
			printLine("symbolic_states::ptr S" + i + ";");
			printNewline();
			printLine("initial_state::ptr I" + i
					+ " = initial_state::ptr(new initial_state(initial_location_id, initial_polytope_I" + i + ", S" + i
					+ ", transition_id));");
			printNewline();
			printLine("init_state_list.push_back(I" + i + ");");
			i++;
		}
		printLine("Hybrid_Automata.setDimension(dim);");
		printNewline();
		printNewline();
		int var_id = 0;
		printNewline();
		for (String s : ControlVar) {
			printLine("Hybrid_Automata.insert_to_map(\"" + s + "\"," + var_id + ");");
			var_id++;
		}
		printNewline();
	}

	private static Expression removeConstants(Expression e, Collection<String> constants) {
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
				if (left instanceof Variable && constants.contains(((Variable) left).name))
					rv = Constant.TRUE;
			}
		}
		return rv;
	}

	/**
	 * Prints the locations with their labels and everything that happens in
	 * them (invariant, flow...)
	 */

	// /////////////////////////////////////////Locations work completed
	// ///////////////////////////////////

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
		} catch (AutomatonExportException ex) {
			throw new AutomatonExportException("Error with expression:" + e, ex);
		}
		return rv;
	}

	private static String getFlowConditionExpressionRec(Expression e) {
		String rv = "";
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
				throw new AutomatonExportException("XSpeed printer doesn't support OR operator. "
						+ "Consider using a Hyst pass to eliminate disjunctions)");

			} else if (Operator.isComparison(o.op)) {
				Operator op = o.op;
				// Flow doesn't like < or >... needs <= or >=
				if (op.equals(Operator.GREATER) || op.equals(Operator.LESS) || op.equals(Operator.NOTEQUAL))
					throw new AutomatonExportException("XSpeed printer doesn't support operator " + op);
				// make sure it's of the form p ~ c
				if (o.children.size() == 2 && o.getRight() instanceof Constant)
					rv = e.toString();
				else {
					// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
					rv += getFlowConditionExpression(o.getLeft());
					rv += " - (" + getFlowConditionExpression(o.getRight());
					rv += ")" + Expression.expressionPrinter.printOperator(op) + "0";
				}
			} else
				rv = e.toString();
		} else
			rv = e.toString();
		return rv;
	}

	/***************************************
	 * getFlowConditionExpressionXspeed(Expression e) calls
	 * getFlowConditionExpressionRecXspeed(e) which creates invariant marix and
	 * Upolytope... from the set of invariants....
	 */
	// ////////////////////////////////////////////////////////////////

	public String getFlowConditionExpressionXspeed(Expression e) {
		String rv = null;
		try {
			rv = getFlowConditionExpressionRecXspeed(e);
		} catch (AutomatonExportException ex) {
			System.out.println("XSpeed Will NOT be able to handle this model!!!");
			throw new AutomatonExportException("Error with expression:" + e, ex);
		}
		return rv;
	}

	private String getFlowConditionExpressionRecXspeed(Expression e) {
		String rv = "";		
		if (e instanceof Operation) {
			Operation o = (Operation) e;
			if (o.op == Operator.AND) {
				rv += getFlowConditionExpressionRecXspeed(o.getLeft());
				rv += "   ";
				rv += getFlowConditionExpressionRecXspeed(o.getRight());
			}
			else if(o.op==Operator.SUBTRACT || o.op ==Operator.ADD){
				while(o.getLeft() instanceof Operation){
			if(o.op==Operator.SUBTRACT){
				Operation oo=o.getRight().asOperation();
				if(oo.getRight() instanceof Variable && oo.getLeft() instanceof Constant){
					int l = CVarcontainX(oo.getRight().toString());
					invY[invindex][l] = Double.parseDouble(oo.getLeft().toString());
					invY[invindex+1][l] = -Double.parseDouble(oo.getLeft().toString());
					o=o.getLeft().asOperation();
				}
			}
			else if(o.op==Operator.ADD){
				Operation oo=o.getRight().asOperation();
				if(oo.getRight() instanceof Variable && oo.getLeft() instanceof Constant){
					int l = CVarcontainX(oo.getRight().toString());
					invY[invindex][l] = -Double.parseDouble(oo.getLeft().toString());
					invY[invindex+1][l] = Double.parseDouble(oo.getLeft().toString());
					o=o.getLeft().asOperation();
				}
			}}
			}
			else if (o.op == Operator.OR) {
				throw new AutomatonExportException("Xspeed printer doesn't support OR operator. "
						+ "Consider using a Hyst pass to eliminate disjunctions)");
			} else if (Operator.isComparison(o.op) || o.op == Operator.EQUAL) {
				Operator op = o.op;
				// Flow doesn't like < or >... needs <= or >=
				if (op.equals(Operator.GREATER) || op.equals(Operator.LESS) || op.equals(Operator.NOTEQUAL))
					throw new AutomatonExportException("Xspeed printer doesn't support operator " + op);

				// make sure it's of the form p ~ c
				if (o.children.size() == 2 && o.getRight() instanceof Constant
						&& (op.equals(Operator.GREATEREQUAL) || (op.equals(Operator.EQUAL)))) {
					rv = e.toString();
					if (ControlVar.contains(o.getLeft().toString())) {//System.out.println("in here greater");
						int l = CVarcontainX(o.getLeft().toString());
						invY[invindex][l] = -1;
						invX[invindex] = -(Double.parseDouble(o.getRight().toString()));
						invindex = invindex + 1;
					} else if (UncontrolVar.contains(o.getLeft().toString())) {
						int l = UVarcontainX(o.getLeft().toString());
						ConUY[Uinvindex][l] = -1;
						ConUX[Uinvindex] = -(Double.parseDouble(o.getRight().toString()));
						Uinvindex = Uinvindex + 1;
					} else if ((o.getLeft().toString()).indexOf('-') != (-1)
							|| (o.getLeft().toString()).indexOf('+') != (-1)) { 
						Operation oper = o.getLeft().asOperation();
						if (oper != null) {
							if (oper.op == Operator.ADD)
							{
								if (ControlVar.contains(oper.getLeft().toString())) {
									int l = CVarcontainX(oper.getLeft().toString());
									invY[invindex][l] = -1;
								}
								Operation or = oper.getRight().asOperation();
								if (or != null) {
									if (or.op == Operator.MULTIPLY) {
										if (or.getRight() instanceof Constant) {
											int l = CVarcontainX(or.getLeft().toString());
											invY[invindex][l] = -Double.parseDouble(or.getRight().toString());
										} else if (or.getLeft() instanceof Constant) {
											int l = CVarcontainX(or.getRight().toString());
											invY[invindex][l] = -Double.parseDouble(or.getLeft().toString());
										}
									}
								}
								if (ControlVar.contains(oper.getRight().toString())) {
									int l = CVarcontainX(oper.getRight().toString());
									invY[invindex][l] = -1;
								}
								or = oper.getLeft().asOperation();
								if (or != null) {
									if (or.op == Operator.MULTIPLY) {
										if (or.getRight() instanceof Constant) {
											int l = CVarcontainX(or.getLeft().toString());
											invY[invindex][l] = -Double.parseDouble(or.getRight().toString());
										} else if (or.getLeft() instanceof Constant) {
											int l = CVarcontainX(or.getRight().toString());
											invY[invindex][l] = -Double.parseDouble(or.getLeft().toString());
										}
									}
								}
								invX[invindex] = -(Double.parseDouble(o.getRight().toString()));
								invindex = invindex + 1;
							}
							else if(oper.op==Operator.SUBTRACT){
								if(oper.getRight() instanceof Operation && oper.getRight().asOperation().op==Operator.MULTIPLY){
									if(ControlVar.contains(oper.getRight().asOperation().getRight().toString())){
										int l = CVarcontainX(oper.getRight().asOperation().getRight().toString());
										invY[invindex][l] = Double.parseDouble(oper.getRight().asOperation().getLeft().toString());	
									}
									else{
										int l = UVarcontainX(oper.getRight().asOperation().getRight().toString());
										ConUY[Uinvindex][l] = Double.parseDouble(oper.getRight().asOperation().getLeft().toString());
									}
								}
								Expression ll=oper.getLeft();
								Operation ol=ll.asOperation();
								if(ol.op==Operator.ADD){
									if(ol.getRight() instanceof Variable){
										if(ControlVar.contains(ol.getRight().toString())){
											int l = CVarcontainX(ol.getRight().toString());
											invY[invindex][l] = -1;
										}
										else{
											int l = UVarcontainX(ol.getRight().toString());
											ConUY[Uinvindex][l] = -1;
										}
									}
									if(ol.getLeft() instanceof Operation && ol.getLeft().asOperation().op == Operator.MULTIPLY){
										Expression lll=ol.getLeft();
										Operation oll=lll.asOperation();
										if(oll.getLeft() instanceof Constant && oll.getRight() instanceof Variable){
											if(ControlVar.contains(oll.getRight().toString())){
												int l = CVarcontainX(oll.getRight().toString());
												invY[invindex][l] = -Double.parseDouble(oll.getLeft().toString());
											}
											else{
												int l = UVarcontainX(oll.getRight().toString());
												ConUY[Uinvindex][l] =  -Double.parseDouble(oll.getLeft().toString());
											}
										}
									}
								}
								invindex=invindex+1;
								Uinvindex++;							
								}
						}
					}
					else if(o.getLeft() instanceof Operation && o.getRight() instanceof Constant){
						Operation oper = o.getLeft().asOperation();
						if(oper.op == Operator.MULTIPLY){
							if(oper.getLeft() instanceof Constant && oper.getRight() instanceof Variable){
								if(oper.getLeft().toString().equals("1")){
									if (ControlVar.contains(oper.getRight().toString())) {
										int l = CVarcontainX(oper.getRight().toString());
										invY[invindex][l] = -1;
										invX[invindex] = -(Double.parseDouble(o.getRight().toString()));
										invindex = invindex + 1;
									} else if (UncontrolVar.contains(oper.getRight().toString())) {
										int l = UVarcontainX(oper.getRight().toString());
										ConUY[Uinvindex][l] = -1;
										ConUX[Uinvindex] = -(Double.parseDouble(o.getRight().toString()));
										Uinvindex = Uinvindex + 1;
									} 
								}
							}
						}
					}
				}
				if (o.children.size() == 2 && o.getRight() instanceof Constant
						&& (op.equals(Operator.LESSEQUAL) || (op.equals(Operator.EQUAL)))) {
					rv = e.toString();
					if (ControlVar.contains(o.getLeft().toString())) {
						int l = CVarcontainX(o.getLeft().toString());
						invY[invindex][l] = 1;
						invX[invindex] = (Double.parseDouble(o.getRight().toString()));
						invindex = invindex + 1;
					} else if (UncontrolVar.contains(o.getLeft().toString())) {
						int l = UVarcontainX(o.getLeft().toString());
						ConUY[Uinvindex][l] = 1;
						ConUX[Uinvindex] = (Double.parseDouble(o.getRight().toString()));
						Uinvindex = Uinvindex + 1;
					}
					else if (((o.getLeft().toString()).indexOf('-') != (-1)
							|| (o.getLeft().toString()).indexOf('+') != (-1))) {
						Operation oper = o.getLeft().asOperation();
						if (oper != null) {
							if (oper.op == Operator.ADD)
							{
								if (ControlVar.contains(oper.getLeft().toString())) {
									int l = CVarcontainX(oper.getLeft().toString());
									invY[invindex][l] = 1;
								}
								Operation or = oper.getRight().asOperation();
								if (or != null) {
									if (or.op == Operator.MULTIPLY) {
										if (or.getRight() instanceof Constant) {
											int l = CVarcontainX(or.getLeft().toString());
											invY[invindex][l] = Double.parseDouble(or.getRight().toString());
										} else if (or.getLeft() instanceof Constant) {
											int l = CVarcontainX(or.getRight().toString());
											invY[invindex][l] = Double.parseDouble(or.getLeft().toString());
										}
									}
								}
								if (ControlVar.contains(oper.getRight().toString())) {
									int l = CVarcontainX(oper.getRight().toString());
									invY[invindex][l] = 1;
								}
								or = o.getLeft().asOperation();
								if (or != null) {
									if (or.op == Operator.MULTIPLY) {
										if (or.getRight() instanceof Constant) {
											int l = CVarcontainX(or.getLeft().toString());
											invY[invindex][l] = Double.parseDouble(or.getRight().toString());
										} else if (or.getLeft() instanceof Constant) {
											int l = CVarcontainX(or.getRight().toString());
											invY[invindex][l] = Double.parseDouble(or.getLeft().toString());
										}
									}
								}
								invX[invindex] = (Double.parseDouble(o.getRight().toString()));
								if(!op.equals(Operator.EQUAL))
								invindex = invindex + 1;
							}
							else if(oper.op==Operator.SUBTRACT){
								if(oper.getRight() instanceof Operation && oper.getRight().asOperation().op==Operator.MULTIPLY){
									if(ControlVar.contains(oper.getRight().asOperation().getRight().toString())){
										int l = CVarcontainX(oper.getRight().asOperation().getRight().toString());
										invY[invindex][l] = -Double.parseDouble(oper.getRight().asOperation().getLeft().toString());
									}
									else{
										int l = UVarcontainX(oper.getRight().asOperation().getRight().toString());
										ConUY[Uinvindex][l] = -Double.parseDouble(oper.getRight().asOperation().getLeft().toString());
									}
								}
								Expression ll=oper.getLeft();
								Operation ol=ll.asOperation();
								if(ol.op==Operator.ADD){
									if(ol.getRight() instanceof Variable){
										if(ControlVar.contains(ol.getRight().toString())){
											int l = CVarcontainX(ol.getRight().toString());
											invY[invindex][l] = 1;
										}
										else{
											int l = UVarcontainX(ol.getRight().toString());
											ConUY[Uinvindex][l] = 1;
										}
									}
									if(ol.getLeft() instanceof Operation && ol.getLeft().asOperation().op == Operator.MULTIPLY){
										Expression lll=ol.getLeft();
										Operation oll=lll.asOperation();
										if(oll.getLeft() instanceof Constant && oll.getRight() instanceof Variable){
											if(ControlVar.contains(oll.getRight().toString())){
												int l = CVarcontainX(oll.getRight().toString());
												invY[invindex][l] = Double.parseDouble(oll.getLeft().toString());
											}
											else{
												int l = UVarcontainX(oll.getRight().toString());
												ConUY[Uinvindex][l] =  Double.parseDouble(oll.getLeft().toString());
											}
										}
									}
								}
								invindex=invindex+1;
								Uinvindex++;							}
						}

					}
					else if(o.getLeft() instanceof Operation && o.getRight() instanceof Constant){
						Operation oper = o.getLeft().asOperation();
						if(oper.op == Operator.MULTIPLY){
							if(oper.getLeft() instanceof Constant && oper.getRight() instanceof Variable){
								if(oper.getLeft().toString().equals("1")){
									if (ControlVar.contains(oper.getRight().toString())) {
										int l = CVarcontainX(oper.getRight().toString());
										invY[invindex][l] = 1;
										invX[invindex] = (Double.parseDouble(o.getRight().toString()));
										invindex = invindex + 1;
									} else if (UncontrolVar.contains(oper.getRight().toString())) {
										int l = UVarcontainX(oper.getRight().toString());
										ConUY[Uinvindex][l] = 1;
										ConUX[Uinvindex] = (Double.parseDouble(o.getRight().toString()));
										Uinvindex = Uinvindex + 1;
									} 
								}
							}
						}
					}
				} 
					if ((o.children.size() == 2 && o.getLeft() instanceof Constant
							&& (op.equals(Operator.LESSEQUAL) || (op.equals(Operator.EQUAL))))) {
						rv = e.toString();
						if (ControlVar.contains(o.getRight().toString())) {
							int l = CVarcontainX(o.getRight().toString());
							invY[invindex][l] = -1;
							invX[invindex] = -(Double.parseDouble(o.getLeft().toString()));
							if(!op.equals(Operator.EQUAL))
							invindex = invindex + 1;
						} else {
							int l = UVarcontainX(o.getRight().toString());
							ConUY[Uinvindex][l] = -1;
							ConUX[Uinvindex] = -(Double.parseDouble(o.getLeft().toString()));
							if(!op.equals(Operator.EQUAL))
							Uinvindex = Uinvindex + 1;

						}
					} if ((o.children.size() == 2 && ((o.getLeft() instanceof Variable))
							&& ((o.getRight() instanceof Variable))
							&& (op.equals(Operator.LESSEQUAL) || (op.equals(Operator.EQUAL))))) {
						if (ControlVar.contains(o.getRight().toString())) {
							int l = CVarcontainX(o.getRight().toString());
							invY[invindex][l] = -1;
						} else {
							int l = UVarcontainX(o.getRight().toString());
							ConUY[Uinvindex][l] = -1;
						}
						if (ControlVar.contains(o.getLeft().toString())) {
							int l = CVarcontainX(o.getLeft().toString());
							invY[invindex][l] = 1;
						} else {
							int l = UVarcontainX(o.getLeft().toString());
							ConUY[Uinvindex][l] = 1;
						}
							invindex = invindex + 1;
							Uinvindex = Uinvindex + 1;
					}

					// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
					if ((o.children.size() == 2 && o.getLeft() instanceof Constant
							&& (op.equals(Operator.GREATEREQUAL) || (op.equals(Operator.EQUAL))))) {
						rv = e.toString();
						if (ControlVar.contains(o.getRight().toString())) {
							int l = CVarcontainX(o.getRight().toString());
							invY[invindex][l] = 1;
							invX[invindex] = (Double.parseDouble(o.getRight().toString()));
							if(!op.equals(Operator.EQUAL))
							invindex = invindex + 1;
						} else {
							int l = UVarcontainX(o.getRight().toString());
							ConUY[Uinvindex][l] = 1;
							ConUX[Uinvindex] = (Double.parseDouble(o.getRight().toString()));
							if(!op.equals(Operator.EQUAL))
							Uinvindex = Uinvindex + 1;

						}

					} else if ((o.children.size() == 2 && ((o.getLeft() instanceof Variable))
							&& ((o.getRight() instanceof Variable))
							&& (op.equals(Operator.GREATEREQUAL) || (op.equals(Operator.EQUAL))))) {
						if (ControlVar.contains(o.getRight().toString())) {
							int l = CVarcontainX(o.getRight().toString());
							invY[invindex][l] = 1;
						} else {
							int l = UVarcontainX(o.getRight().toString());
							ConUY[Uinvindex][l] = 1;
						}
						if (ControlVar.contains(o.getLeft().toString())) {
							int l = CVarcontainX(o.getLeft().toString());
							invY[invindex][l] = -1;
						} else {
							int l = UVarcontainX(o.getLeft().toString());
							ConUY[Uinvindex][l] = -1;
						}
						if(op.equals(Operator.EQUAL)){
							Uinvindex = Uinvindex + 1;
							invindex = invindex + 1;
						}
						if(!op.equals(Operator.EQUAL))
							Uinvindex = Uinvindex + 1;
						if(!op.equals(Operator.EQUAL))
							invindex = invindex + 1;
					}
					else if ((o.children.size() == 2 && ((o.getLeft() instanceof Operation))
							&& ((o.getRight() instanceof Variable))
							&& (op.equals(Operator.GREATEREQUAL) || (op.equals(Operator.EQUAL))))) {
						if (ControlVar.contains(o.getRight().toString())) {
							int l = CVarcontainX(o.getRight().toString());
							invY[invindex][l] = 1;
							
						} else {
							int l = UVarcontainX(o.getRight().toString());
							ConUY[Uinvindex][l] = 1;
						}
						Operation ol = o.getLeft().asOperation();
						if(ol.children.size()==1 &&  ol.op == Operator.NEGATIVE){
							if (ControlVar.contains(o.getLeft().toString().substring(1))) {
								int l = CVarcontainX(o.getLeft().toString().substring(1));
								invY[invindex][l] = 1;
							} 
							else {
								int l = UVarcontainX(o.getLeft().toString().substring(1));
								ConUY[Uinvindex][l] = 1;
							}
							if(op.equals(Operator.EQUAL)){
								Uinvindex = Uinvindex + 1;
								invindex = invindex + 1;
							}
						}
						if(!op.equals(Operator.EQUAL))
							invindex = invindex + 1;
						if(!op.equals(Operator.EQUAL))
							Uinvindex = Uinvindex + 1;
					}
					else if(o.getLeft() instanceof Variable && o.children.size()==2 && o.getRight() instanceof Operation && o.op==Operator.GREATEREQUAL){
						int f=3;
						if (ControlVar.contains(o.getLeft().toString())) {
							f=0;
							int l = CVarcontainX(o.getLeft().toString());
							invY[invindex][l] = -1;
						} 
						else {
							f=1;
							int l = UVarcontainX(o.getLeft().toString());
							ConUY[Uinvindex][l] = -1;
						}
						
						if(o.getRight().asOperation().op ==Operator.SUBTRACT){
							Expression ll=o.getRight().asOperation().getLeft();
							Expression rr=o.getRight().asOperation().getRight();
							if(rr instanceof Constant){
								if(f==0)
									invX[invindex] = (Double.parseDouble(rr.toString()));
								if(f==1)
									ConUX[Uinvindex] = (Double.parseDouble(rr.toString()));
							}
							if(ll instanceof Operation && ll.asOperation().op==Operator.MULTIPLY){
								if(ll.asOperation().getLeft() instanceof Constant && ll.asOperation().getRight() instanceof Variable)
								{
									if (ControlVar.contains(ll.asOperation().getRight().toString())) {
										int l = CVarcontainX(ll.asOperation().getRight().toString());
										invY[invindex][l] = Double.parseDouble(ll.asOperation().getLeft().toString());
									}
									else{
										int l = UVarcontainX(ll.asOperation().getRight().toString());
										ConUY[Uinvindex][l] = Double.parseDouble(ll.asOperation().getLeft().toString());;
									}
								}
							}
						}
						if(op.equals(Operator.EQUAL)){
							Uinvindex = Uinvindex + 1;
							invindex = invindex + 1;
						}
						if(!op.equals(Operator.EQUAL))
						{
							invindex = invindex + 1;
							Uinvindex = Uinvindex + 1;
						}
					}
					else if(o.getLeft() instanceof Variable && o.children.size()==2 && o.getRight() instanceof Operation && o.op==Operator.LESSEQUAL){
						int f=3;
						if (ControlVar.contains(o.getLeft().toString())) {
							f=0;
							int l = CVarcontainX(o.getLeft().toString());
							invY[invindex][l] = 1;
						} 
						else {
							f=1;
							int l = UVarcontainX(o.getLeft().toString());
							ConUY[Uinvindex][l] = 1;
						}
						
						if(o.getRight().asOperation().op ==Operator.ADD){
							Expression ll=o.getRight().asOperation().getLeft();
							Expression rr=o.getRight().asOperation().getRight();
							if(rr instanceof Constant){
								if(f==0)
									invX[invindex] = (Double.parseDouble(rr.toString()));
								if(f==1)
									ConUX[Uinvindex] = (Double.parseDouble(rr.toString()));
							}
							if(ll instanceof Operation && ll.asOperation().op==Operator.MULTIPLY){
								if(ll.asOperation().getLeft() instanceof Constant && ll.asOperation().getRight() instanceof Variable)
								{
									if (ControlVar.contains(ll.asOperation().getRight().toString())) {
										int l = CVarcontainX(ll.asOperation().getRight().toString());
										invY[invindex][l] = -Double.parseDouble(ll.asOperation().getLeft().toString());
									}
									else{
										int l = UVarcontainX(ll.asOperation().getRight().toString());
										ConUY[Uinvindex][l] = -Double.parseDouble(ll.asOperation().getLeft().toString());;
									}
								}
							}
						}
						if(op.equals(Operator.EQUAL)){
							Uinvindex = Uinvindex + 1;
							invindex = invindex + 1;
						}
						if(!op.equals(Operator.EQUAL))
						{
							invindex = invindex + 1;
							Uinvindex = Uinvindex + 1;
						}
					}
					else {
						rv += getFlowConditionExpressionXspeed(o.getLeft());
						rv += " - (" + getFlowConditionExpressionXspeed(o.getRight());
						rv += ")" + Expression.expressionPrinter.printOperator(op) + "0";
					}
			} else
				rv = e.toString();
		} else {
			rv = e.toString();
		}
		return rv;

	}

	/*****************************************************************
	 * getguardConditionExpressionXspeed(Expression e) function calls
	 * getguardConditionExpressionRecXspeed(Expression e) which will create
	 * guard matrix from the set of assignments..............
	 */
	// ////////////////////////////////////////////////////////////////////
	public String getguardConditionExpressionXspeed(Expression e) {
		String rv = null;
		try {
			invindex = 0;
			rv = getguardConditionExpressionRecXspeed(e);
		} catch (AutomatonExportException ex) {
			throw new RuntimeException("XSpeed does not work on this model\n");
		}
		return rv;
	}

	public static String eval(String exp) {
		String newString = "";
		String temp = "";
		String temp2 = "";
		String newS = "";
		String num = "";
		String num2 = "";
		Double val = 0.0;
		String valu = "";
		String var = "";
		int done = 0, dd = 0;
		int location = 0, j, last = 0;
		int fl = 0;
		char op, op2 = 'g';
		for (int i = 0; i < exp.length(); i++) {

			if (i == 0 && exp.charAt(0) == '-') {
				num += '-';
				last = 1;
			} else if (i == 0 && exp.charAt(0) >= '0' && exp.charAt(0) <= '9') {
				num += exp.charAt(0);
				last = 0;
			}
			if ((exp.charAt(i) == '+' || exp.charAt(i) == '-' || exp.charAt(i) == '*') && i != 0) {
				if (exp.charAt(i + 1) != '(')
					location = i;
			}
			if ((exp.charAt(i) >= 'a' && exp.charAt(i) <= 'z') || (exp.charAt(i) >= 'A' && exp.charAt(i) <= 'Z')) {
				newString += exp.charAt(i);
				if (i + 1 != exp.length()) {
					if (exp.charAt(i + 1) >= '0' && exp.charAt(i + 1) <= '9') {
						newString += exp.charAt(i + 1);
						dd = 1;
					}
					if (i + 1 == exp.length() - 1) {
						newS += newString;
						temp = newS;
						done = 1;
					}
				}
			}
			if (exp.charAt(i) != '(') {
				newString += exp.charAt(i);
			} else {
				op = exp.charAt(i - 1);
				for (j = (location + 1); j < i - 1; j++) {
					if (exp.charAt(j) == '-')
						num += '-';
					num += exp.charAt(j);
				}
				i++;
				if ((exp.charAt(i) > 'a' && exp.charAt(i) < 'z') || (exp.charAt(i) > 'A' && exp.charAt(i) < 'Z')) {
					var += exp.charAt(i);
					i++;
					if ((exp.charAt(i) >= '0' && exp.charAt(i) <= '9')) {
						var += exp.charAt(i);
						i++;
					}

				}
				if (exp.charAt(i) == '-') {
					if (exp.charAt(i + 1) != '-') {
						newString += '-';
						num2 += '-';
						i++;
					} else
						i = i + 2;
				}
				while ((exp.charAt(i) >= '0' && exp.charAt(i) <= '9') || exp.charAt(i) == '.') {
					newString += exp.charAt(i);
					num2 += exp.charAt(i);
					i++;
					if (i == exp.length()) {
						fl = 1;
					}
				}
				if (exp.charAt(i) == ')' || ((exp.charAt(i) == '+' || exp.charAt(i) == '-' || exp.charAt(i) == '*')
						&& (exp.charAt(i + 1) == '-' || (exp.charAt(i + 1) >= '0' && exp.charAt(i + 1) <= '9')))) {
					if (op == '-') {
						val = val + Double.parseDouble(num) - Double.parseDouble(num2);
					}
					if (op == '+') {
						val = val + Double.parseDouble(num) + Double.parseDouble(num2);
					}
					if (op == '*') {
						val = val + Double.parseDouble(num) * Double.parseDouble(num2);
					}
					String t = Double.toString(val);
					if (dd == 1)
						last = last - 1;
					for (int k = 0; k < t.length(); k++) {
						newS += t.charAt(k);
					}
					for (int k = last; k <= location; k++) {
						newS += exp.charAt(k);
					}
					if (Double.parseDouble(num) > 0)
						newS += '+';
					for (int k = 0; k < num.length(); k++) {
						newS += num.charAt(k);
					}
					for (int k = 0; k < temp.length(); k++)
						newS += temp.charAt(k);
					newS += op;
					newS += var.charAt(0);
					newS += var.charAt(1);
				}
			}
			if (exp.charAt(i) == ')') {
				last = i + 2;
				var = "";
				num = "";
				num2 = "";
				newString = "";
			}
		}
		if (done == 0) {
			num = "";
			num2 = "";
			num += newS.charAt(0);
			int index1 = 0, index2 = 0, flag = 0;
			int p = 1;
			while (((newS.charAt(p) >= '0') && (newS.charAt(p) <= '9')) || newS.charAt(p) == '.') {
				p++;
			}
			last = p;
			for (int k = 1; k < newS.length(); k++) {
				while (((newS.charAt(k) >= '0') && (newS.charAt(k) <= '9')) || newS.charAt(k) == '.') {
					num += newS.charAt(k);
					k++;
					flag = 1;
				}
				if (flag == 1 && (newS.charAt(k) == '-' || newS.charAt(k) == '+')) {
					index1 = k;
					k++;
					while ((newS.charAt(k) >= '0' && newS.charAt(k) <= '9') || newS.charAt(k) == '.') {
						k++;
					}
					if (newS.charAt(k) == '-' || newS.charAt(k) == '+') {
						index2 = k;
						break;
					}
					flag = 0;
				}
			}
			for (int k = index1 + 1; k < index2; k++) {
				if (newS.charAt(k - 1) == '-')
					num2 += '-';
				num2 += newS.charAt(k);
			}
			if (!(num.isEmpty() || num2.isEmpty())) {
				val = Double.parseDouble(num) + Double.parseDouble(num2);
				temp = "";
				String t = Double.toString(val);
				for (int k = 0; k < t.length(); k++) {
					temp += t.charAt(k);
				}
				for (int k = last; k < newS.length(); k++) {
					while (k <= index1) {
						temp += newS.charAt(k);
						k++;
					}
					if (k > index2) {
						while (k < newS.length()) {
							temp += newS.charAt(k);
							k++;
						}
					}
				}
			}
		}
		return temp;
	}

	private String getguardConditionExpressionRecXspeed(Expression e) {
		String rv = "";
		try {
			// replace && with ' ' and then print as normal
			if (e instanceof Operation) {
				Operation o = (Operation) e;
				if (o.op == Operator.AND) {
					rv += getguardConditionExpressionRecXspeed(o.getLeft());
					rv += "   ";
					rv += getguardConditionExpressionRecXspeed(o.getRight());
				} else if (o.op == Operator.EQUAL && (o.getLeft() instanceof Variable)) {
					rv = e.toString();
					int l = CVarcontainI(o.getLeft().toString());
					guardY[invindex][l] = -1;
					guardY[invindex + 1][l] = 1;
					if (ConstCheck(o.getRight().toString())) {
						guardX[invindex] = -1;
						guardX[invindex + 1] = 1;
					} else {
						guardX[invindex] = -(Double.parseDouble(o.getRight().toString()));
						guardX[invindex + 1] = (Double.parseDouble(o.getRight().toString()));
					}
					invindex = invindex + 2;
				} else if (o.op == Operator.OR) {
					throw new AutomatonExportException("Xspeed printer doesn't support OR operator. "
							+ "Consider using a Hyst pass to eliminate disjunctions)");
				} else if (Operator.isComparison(o.op)) {
					Operator op = o.op;
					// Flow doesn't like < or >... needs <= or >=
					if (op.equals(Operator.NOTEQUAL))
						System.out.println("need to be modified");
					// make sure it's of the form p ~ c
					if ((o.children.size() == 2
							&& ((o.getRight() instanceof Constant) || ConstCheck(o.getRight().toString()))
							&& (op.equals(Operator.GREATEREQUAL) || op.equals(Operator.GREATER)))) {
						rv = e.toString();int rw=0;
						if (o.getLeft() instanceof Variable) {
							int l = CVarcontainI(o.getLeft().toString());
							guardY[invindex][l] = -1;
							rw=1;
						}
						if(o.getLeft() instanceof Operation && o.getRight() instanceof Constant){
							Operation oper = o.getLeft().asOperation();
							if(oper.op == Operator.MULTIPLY){
								if(oper.getLeft() instanceof Constant && oper.getRight() instanceof Variable){
									if(oper.getLeft().toString().equals("1")){
										int l = CVarcontainI(oper.getRight().toString());
										guardY[invindex][l] = -1;
										rw=1;
									}
								}
							}
						}
						if(o.getRight() instanceof Constant){
							guardX[invindex] = -(Double.parseDouble(o.getRight().toString()));
							if(rw==1)
								invindex++;
						}
						if ((o.getLeft().toString()).indexOf('-') != (-1)
								|| (o.getLeft().toString()).indexOf('+') != (-1)) {
							Operation oper = o.getLeft().asOperation();
							if (oper != null) {
								if (oper.op == Operator.ADD)
								{
									if (ControlVar.contains(oper.getLeft().toString())) {
										int l = CVarcontainX(oper.getLeft().toString());
										guardY[invindex][l] = -1;
									}
									Operation or = oper.getRight().asOperation();
									if (or != null) {
										if (or.op == Operator.MULTIPLY) {
											if (or.getRight() instanceof Constant) {
												int l = CVarcontainX(or.getLeft().toString());
												guardY[invindex][l] = -Double.parseDouble(or.getRight().toString());
											} else if (or.getLeft() instanceof Constant) {
												int l = CVarcontainX(or.getRight().toString());
												guardY[invindex][l] = -Double.parseDouble(or.getLeft().toString());
											}
										}
									}
									if (ControlVar.contains(oper.getRight().toString())) {
										int l = CVarcontainX(oper.getRight().toString());
										guardY[invindex][l] = -1;
									}
									or = oper.getLeft().asOperation();
									if (or != null) {
										if (or.op == Operator.MULTIPLY) {
											if (or.getRight() instanceof Constant) {
												int l = CVarcontainX(or.getLeft().toString());
												guardY[invindex][l] = -Double.parseDouble(or.getRight().toString());
											} else if (or.getLeft() instanceof Constant) {
												int l = CVarcontainX(or.getRight().toString());
												guardY[invindex][l] = -Double.parseDouble(or.getLeft().toString());
											}
										}
									}
								}
								if(oper.op==Operator.SUBTRACT){
									Expression ll=oper.getLeft();
									Expression rr=oper.getRight();
									Operation ol=ll.asOperation();
									Operation or=rr.asOperation();
									if(or.op==Operator.MULTIPLY){
										if(or.getLeft() instanceof Constant && or.getRight() instanceof Variable){
											int l=CVarcontainX(or.getRight().toString());
											guardY[invindex][l]=Double.parseDouble(or.getLeft().toString());
										}
										else if (or.getRight() instanceof Constant) {
											int l = CVarcontainX(or.getLeft().toString());
											guardY[invindex][l] = Double.parseDouble(or.getRight().toString());
										} 
									}
									if(ol.op==Operator.MULTIPLY){
										if(ol.getLeft() instanceof Constant && ol.getRight() instanceof Variable){
											int l=CVarcontainX(ol.getRight().toString());
											guardY[invindex][l]=Double.parseDouble(ol.getLeft().toString());
										}
										else if (ol.getRight() instanceof Constant) {
											int l = CVarcontainX(ol.getLeft().toString());
											guardY[invindex][l] = -Double.parseDouble(ol.getRight().toString());
										} 
									}
									if(ol.op==Operator.ADD){
										Expression lll=ol.getLeft();
										Operation oll=lll.asOperation();
										if(ol.getRight() instanceof Variable){
											int l=CVarcontainX(ol.getRight().toString());
											guardY[invindex][l]=-1;
										}
										if(oll.op==Operator.MULTIPLY){
											if(oll.getRight() instanceof Variable && oll.getLeft() instanceof Constant){
												int l=CVarcontainX(oll.getRight().toString());
												guardY[invindex][l]=-Double.parseDouble(oll.getLeft().toString());
											}
										}
									}
									invindex++;
								}
							}

						} else if(rw==0){
							guardX[invindex] = -(Double.parseDouble(o.getRight().toString()));
						}
					} 
					else{
						if (o.children.size() == 2
								&& ((o.getRight() instanceof Constant) || ConstCheck(o.getRight().toString()))
								&& (op.equals(Operator.LESSEQUAL) || (op.equals(Operator.LESS))
										|| (op.equals(Operator.EQUAL)))) {
							rv = e.toString();int rw=0;
							if (o.getLeft() instanceof Variable) {
								int l = CVarcontainI(o.getLeft().toString());
								guardY[invindex][l] = 1;
								rw=1;
							}
							if(o.getLeft() instanceof Operation && o.getRight() instanceof Constant){
								Operation oper = o.getLeft().asOperation();
								if(oper.op == Operator.MULTIPLY){
									if(oper.getLeft() instanceof Constant && oper.getRight() instanceof Variable){
										if(oper.getLeft().toString().equals("1")){
											int l = CVarcontainI(oper.getRight().toString());
											guardY[invindex][l] = 1;
											rw=1;
										}
									}
								}
							}
							if (o.getRight() instanceof Constant) {
								guardX[invindex] = (Double.parseDouble(o.getRight().toString()));
								if(rw==1)
									invindex++;
							}
							
							if ((o.getLeft().toString()).indexOf('-') != (-1)
									|| (o.getLeft().toString()).indexOf('+') != (-1)) {
								// do remember to add for uncontrolled vars also
								Operation oper = o.getLeft().asOperation();
								if (oper != null) {
									if (oper.op == Operator.ADD)
									{
										if (ControlVar.contains(oper.getLeft().toString())) {
											int l = CVarcontainX(oper.getLeft().toString());
											guardY[invindex][l] = 1;
											if(op.equals(Operator.EQUAL)){
												guardY[invindex+1][l]=-1;
											}
										}
										Operation or = oper.getRight().asOperation();
										if (or != null) {
											if (or.op == Operator.MULTIPLY) {
												if (or.getRight() instanceof Constant) {
													int l = CVarcontainX(or.getLeft().toString());
													guardY[invindex][l] = Double.parseDouble(or.getRight().toString());
													if(op.equals(Operator.EQUAL)){
														guardY[invindex+1][l]=-Double.parseDouble(or.getRight().toString());
													}
												} else if (or.getLeft() instanceof Constant) {
													int l = CVarcontainX(or.getRight().toString());
													guardY[invindex][l] = Double.parseDouble(or.getLeft().toString());
													if(op.equals(Operator.EQUAL)){
														guardY[invindex+1][l]=-Double.parseDouble(or.getLeft().toString());
													}
												}
											}
										}
										if (ControlVar.contains(oper.getRight().toString())) {
											int l = CVarcontainX(oper.getRight().toString());
											guardY[invindex][l] = 1;
											if(op.equals(Operator.EQUAL)){
												guardY[invindex+1][l]=-1;
											}
										}
										or = oper.getLeft().asOperation();
										if (or != null) {
											if (or.op == Operator.MULTIPLY) {
												if (or.getRight() instanceof Constant) {
													int l = CVarcontainX(or.getLeft().toString());
													guardY[invindex][l] = Double.parseDouble(or.getRight().toString());
													if(op.equals(Operator.EQUAL)){
														guardY[invindex+1][l]=-Double.parseDouble(or.getRight().toString());
													}
												} else if (or.getLeft() instanceof Constant) {
													int l = CVarcontainX(or.getRight().toString());
													guardY[invindex][l] = Double.parseDouble(or.getLeft().toString());
													if(op.equals(Operator.EQUAL)){
														guardY[invindex+1][l]=-Double.parseDouble(or.getLeft().toString());
													}
												}
											}

										}
									}
									if(oper.op==Operator.SUBTRACT){
										Expression ll=oper.getLeft();
										Expression rr=oper.getRight();
										Operation ol=ll.asOperation();
										Operation or=rr.asOperation();
										if(or.op==Operator.MULTIPLY){
											if(or.getLeft() instanceof Constant && or.getRight() instanceof Variable){
												int l=CVarcontainX(or.getRight().toString());
												guardY[invindex][l]=-Double.parseDouble(or.getLeft().toString());
											}
										}
										if(ol.op==Operator.ADD){
											Expression lll=ol.getLeft();
											Operation oll=lll.asOperation();
											if(ol.getRight() instanceof Variable){
												int l=CVarcontainX(ol.getRight().toString());
												guardY[invindex][l]=1;
											}
											if(oll.op==Operator.MULTIPLY){
												if(oll.getRight() instanceof Variable && oll.getLeft() instanceof Constant){
													int l=CVarcontainX(oll.getRight().toString());
													guardY[invindex][l]=Double.parseDouble(oll.getLeft().toString());
												}
											}
										}
										invindex++;
										if(op.equals(Operator.EQUAL)){
											invindex++;
										}
									}
								}

							} else if(rw==0){
								guardX[invindex] = (Double.parseDouble(o.getRight().toString()));
							}
						} else {
							if ((o.children.size() == 2
									&& ((o.getLeft() instanceof Constant) || ConstCheck(o.getLeft().toString()))
									&& (op.equals(Operator.LESSEQUAL) || (op.equals(Operator.LESS))))) {
								rv = e.toString();
								int l = CVarcontainI(o.getRight().toString());
								guardY[invindex][l] = -1;
								if (ConstCheck(o.getRight().toString())) {
									guardX[invindex] = -1;
								} else {
									guardX[invindex] = -(Double.parseDouble(o.getLeft().toString()));
								}
								invindex = invindex + 1;
							}
							else if(o.children.size() == 2
									&& (o.getLeft() instanceof Variable) && (o.getRight() instanceof Variable) 
									&& (op.equals(Operator.LESSEQUAL) || (op.equals(Operator.LESS)))){
								int l = CVarcontainI(o.getLeft().toString());
								guardY[invindex][l] = 1;
								int r = CVarcontainI(o.getRight().toString());
								guardY[invindex][r] = -1;
								invindex = invindex + 1;
							}
							else if(o.children.size() ==2 && (o.getLeft() instanceof Operation) && o.getRight() instanceof Variable && (op.equals(Operator.LESSEQUAL) || (op.equals(Operator.LESS)))){
								Operation ol=o.getLeft().asOperation();
								if (ol.op.equals(Operator.NEGATIVE) || ol.op.equals(Operator.LOGICAL_NOT)) {
									String exp=o.getLeft().toString().substring(1);
									int l=CVarcontainI(exp);
									guardY[invindex][l]=-1;
								}
								int r=CVarcontainI(o.getRight().toString());
								guardY[invindex][r] = -1;
								invindex = invindex + 1;
								
							}
							// change 'p1 ~ p2' to 'p1 - (p2) ~ 0'
							else {
								if ((o.children.size() == 2
										&& ((o.getLeft() instanceof Constant) || ConstCheck(o.getLeft().toString()))
										&& (op.equals(Operator.GREATEREQUAL) || (op.equals(Operator.GREATER))))) {
									rv = e.toString();
									int l = CVarcontainI(o.getLeft().toString());
									guardY[invindex][l] = 1;
									if (ConstCheck(o.getRight().toString())) {
										guardX[invindex] = 1;
									} else {
										guardX[invindex] = (Double.parseDouble(o.getRight().toString()));
									}
									invindex = invindex + 1;
								}else if(o.children.size() == 2
										&& (o.getLeft() instanceof Variable) && (o.getRight() instanceof Variable) 
										&& (op.equals(Operator.GREATEREQUAL) || (op.equals(Operator.GREATER)))){
									int l = CVarcontainI(o.getLeft().toString());
									guardY[invindex][l] = -1;
									int r = CVarcontainI(o.getRight().toString());
									guardY[invindex][r] = 1;
									invindex = invindex + 1;
								}
								else if(o.children.size() ==2 && (o.getLeft() instanceof Operation) && o.getRight() instanceof Variable && (op.equals(Operator.GREATEREQUAL) || (op.equals(Operator.GREATER)))){
									Operation ol=o.getLeft().asOperation();
									if (ol.op.equals(Operator.NEGATIVE) || ol.op.equals(Operator.LOGICAL_NOT)) {
										String exp=o.getLeft().toString().substring(1);
										int l=CVarcontainI(exp);
										guardY[invindex][l]=1;
									}
									int r=CVarcontainI(o.getRight().toString());
									guardY[invindex][r] = 1;
									invindex = invindex + 1;
									
								}
								else if(o.children.size()==2 && o.getLeft() instanceof Variable && o.getRight() instanceof Operation){
									if(o.op==Operator.GREATEREQUAL){
										int l=CVarcontainI(o.getLeft().toString());
										guardY[invindex][l]=-1;
										Expression rr=o.getRight();
										Operation or=rr.asOperation();
										if(or.op==Operator.ADD){
											if(or.getRight() instanceof Constant){
												guardX[invindex]=-Double.parseDouble(or.getRight().toString());
											}
											if(or.getLeft() instanceof Operation && or.getLeft().asOperation().op==Operator.MULTIPLY){
												Expression rrr=or.getLeft();
												Operation orr=rrr.asOperation();
												if(orr.getLeft() instanceof Constant && orr.getRight() instanceof Variable){
													int ll=CVarcontainI(orr.getRight().toString());
													guardY[invindex][ll]=Double.parseDouble(orr.getLeft().toString());
												}
											}
										}
										invindex++;
									}
									else if(o.op==Operator.LESSEQUAL){
										int l=CVarcontainI(o.getLeft().toString());
										guardY[invindex][l]=1;
										Expression rr=o.getRight();
										Operation or=rr.asOperation();
										if(or.op==Operator.SUBTRACT){
											if(or.getRight() instanceof Constant){
												guardX[invindex]=-Double.parseDouble(or.getRight().toString());
											}
											if(or.getLeft() instanceof Operation && or.getLeft().asOperation().op==Operator.MULTIPLY){
												Expression rrr=or.getLeft();
												Operation orr=rrr.asOperation();
												if(orr.getLeft() instanceof Constant && orr.getRight() instanceof Variable){
													int ll=CVarcontainI(orr.getRight().toString());
													guardY[invindex][ll]=-Double.parseDouble(orr.getLeft().toString());
												}
											}
										}
										invindex++;
									}
								}
								else {
									rv += getguardConditionExpressionXspeed(o.getLeft());
									rv += " - (" + getguardConditionExpressionXspeed(o.getRight());
									rv += ")" + Expression.expressionPrinter.printOperator(op) + "0";
								}
							}
						}
					}
				} else {
					rv = e.toString();
				}
			} else {
				rv = e.toString();
			}
		} catch (Exception ex) {

			throw new RuntimeException("XSpeed does not work on Disjunctiveguard models: " + ex);
		}
		return rv;
	}

	/*****************************************
	 * printFlowRangeConditionsExspeed(,...........) function used for making
	 * Initial polytope I from the initial input.
	 */
	// ////////////////////////////////////////////
	private void printFlowRangeConditionsExspeed(Expression ex, boolean isAssignment, int row) {
		// TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();
		HashMap<String, Interval> ranges = new HashMap<String, Interval>();
		try {
			RangeExtractor.getVariableRanges(ex, ranges);
		} catch (EmptyRangeException e) {
			System.out.println("XSpeed Cannot handle those models");
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} catch (ConstantMismatchException e) {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} catch (UnsupportedConditionException e) {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		int init = ControlVarI.size();
		int ini = 0;
		int inj = 0;
		int flag = 0;
		double[] ConstraintsMatrixI = new double[ControlVarI.size() * 2];

		for (Entry<String, Interval> f : ranges.entrySet()) {
			String varName = f.getKey();
			if (varName == null) {
				flag = 1;
				break;
			}
		}
		// create double boundvalues and keep doing the values store in array
		// and then print in cpp file
		double[] boundValues = new double[ha.variables.size() * 2];
		double[][] consMatrix = new double[ha.variables.size() * 2][ha.variables.size()];
		int index = ControlVarI.size() * 2 - 1;
		int indexh = ControlVarI.size() * 2 - 1;
		int indexy;
		if (flag == 1) {
			int chil = 2;
			while (chil > 1) {
				Operation o = ex.asOperation();

				if (o != null && o.children.size() == 2) {
					Expression leftExp = o.children.get(0);
					Expression rightExp = o.children.get(1);
					Operator op = o.op;
					if (op.equals(Operator.AND)) {
						Operation oper = rightExp.asOperation();
						Expression lef = oper.children.get(0);
						Expression rig = oper.children.get(1);
						indexy = 0;

						Operator oo = oper.op;
						if (oo.equals(Operator.LESSEQUAL) && rig instanceof Constant) {
							boundValues[index] = ((Constant) rig).getVal();
							index--;
							Operation o1 = lef.asOperation();
							Expression le = o1.children.get(0);
							Expression ri = o1.children.get(1);
							Operator oo1 = o1.op;
							if (oo1.equals(Operator.ADD) || oo1.equals(Operator.SUBTRACT)) {
								if (ControlVar.contains(le.toString())) {
									for (int i = 0; i < ControlVar.size(); i++) {
										if (ControlVar.get(i).equals(le.toString())) {
											consMatrix[indexh][i] = 1;
										}
									}
								} else {
									if (le.toString().charAt(0) == '-') {
										String t = le.toString().substring(1);
										if (ControlVar.contains(t)) {
											for (int i = 0; i < ControlVar.size(); i++) {
												if (ControlVar.get(i).equals(t)) {
													consMatrix[indexh][i] = -1;
												}
											}
										}
									}
								}
								if (ControlVar.contains(ri.toString())) {
									for (int i = 0; i < ControlVar.size(); i++) {
										if (ControlVar.get(i).equals(ri.toString())) {
											if (oo1.equals(Operator.ADD))
												consMatrix[indexh][i] = 1;
											if (oo1.equals(Operator.SUBTRACT))
												consMatrix[indexh][i] = -1;
										}
									}
								} else {
									if (ri.toString().charAt(0) == '-') {
										String t = ri.toString().substring(1);
										if (ControlVar.contains(t)) {
											for (int i = 0; i < ControlVar.size(); i++) {
												if (ControlVar.get(i).equals(t)) {
													consMatrix[indexh][i] = -1;
												}
											}
										}
									}
								}
								indexh--;
							}
						}
						if (index == 0) {
							Operation opera = leftExp.asOperation();
							Expression l = opera.children.get(0);
							Expression r = opera.children.get(1);
							Operator ooo = opera.op;
							if (ooo.equals(Operator.LESSEQUAL) && r instanceof Constant) {
								boundValues[index] = ((Constant) r).getVal();
								Operation o1 = l.asOperation();
								Expression le = o1.children.get(0);
								Expression ri = o1.children.get(1);
								Operator oo1 = o1.op;
								if (oo1.equals(Operator.ADD) || oo1.equals(Operator.SUBTRACT)) {
									if (ControlVar.contains(le.toString())) {
										for (int i = 0; i < ControlVar.size(); i++) {
											if (ControlVar.get(i).equals(le.toString())) {
												consMatrix[indexh][i] = 1;
											}
										}
									} else {
										if (le.toString().charAt(0) == '-') {
											String t = le.toString().substring(1);
											if (ControlVar.contains(t)) {
												for (int i = 0; i < ControlVar.size(); i++) {
													if (ControlVar.get(i).equals(t)) {
														consMatrix[indexh][i] = -1;
													}
												}
											}
										}
									}
									if (ControlVar.contains(ri.toString())) {
										for (int i = 0; i < ControlVar.size(); i++) {
											if (ControlVar.get(i).equals(ri.toString())) {
												if (oo1.equals(Operator.ADD))
													consMatrix[indexh][i] = 1;
												if (oo1.equals(Operator.SUBTRACT))
													consMatrix[indexh][i] = -1;
											}
										}
									} else {
										if (ri.toString().charAt(0) == '-') {
											String t = ri.toString().substring(1);
											if (ControlVar.contains(t)) {
												for (int i = 0; i < ControlVar.size(); i++) {
													if (ControlVar.get(i).equals(t)) {
														consMatrix[indexh][i] = -1;
													}
												}
											}
										}
									}
									indexh--;
								}
							}
						}
					}
				}
				chil = o.children.size();
				ex = o.children.get(0);
			}
			// print constraint matrices
			printLine("ConstraintsMatrixI.clear();");
			for (int h = 0; h < ha.variables.size() * 2; h++) {
				for (int y = 0; y < ha.variables.size(); y++) {
					if (consMatrix[h][y] != 0)
						printLine("ConstraintsMatrixI(" + h + "," + y + ")=" + consMatrix[h][y] + ";");
				}

			}
			// print boudvalues
			printLine("boundValueI.resize(row );");
			printLine("boundValueI.assign(row,0);");
			for (int h = 0; h < ha.variables.size() * 2; h++) {
				if (boundValues[h] != 0)
					printLine("boundValueI[" + h + "]=" + boundValues[h] + ";");
			}
			printNewline();
		}
		if (flag == 0) {
			for (Entry<String, Interval> e : ranges.entrySet()) {
				String varName = e.getKey();
				if (isAssignment && ControlVarI.contains(varName)) {
					for (int vari = ini; vari < ini + 2; vari++) {
						for (int varj = 0; varj < init; varj++) {
							if (inj / 2 == varj && vari % 2 == 0)
								printLine("ConstraintsMatrixI(" + vari + " , " + varj + ") = 1;");
							else if (inj / 2 == varj && vari % 2 == 1)
								printLine("ConstraintsMatrixI(" + vari + " , " + varj + ") = -1;");
						}
						inj = inj + 1;
					}
					ini = ini + 2;
				} else {
					// it's a comparison
					/*
					 * if (inter.min == inter.max) printLine(varName + " = " +
					 * doubleToString(inter.min)); else { if (inter.min !=
					 * -Double.MAX_VALUE) printLine(varName + " >= " +
					 * doubleToString(inter.min));
					 * 
					 * if (inter.max != Double.MAX_VALUE) printLine(varName +
					 * " <= " + doubleToString(inter.max)); }
					 */
				}
			}

			printLine("boundValueI.resize(row );");
			printLine("boundValueI.assign(row,0);");
			for (Entry<String, Interval> e : ranges.entrySet()) {
				if (ControlVarI.contains(e.getKey().toString())) {
					int max = 2 * (CVarcontainX(e.getKey()));
					int min = 2 * (CVarcontainX(e.getKey())) + 1;
					if (isAssignment) {
						ConstraintsMatrixI[max] = e.getValue().max;
						if (e.getValue().min == 0)
							ConstraintsMatrixI[min] = e.getValue().min;
						else
							ConstraintsMatrixI[min] = -e.getValue().min;
					}
				}
			}
			for (int h = 0; h < ControlVarI.size() * 2; h++) {
				if (ConstraintsMatrixI[h] != 0)
					printLine("boundValueI[" + h + "]=" + doubleToString(ConstraintsMatrixI[h]) + ";");
			}
			printNewline();
		}
	}

	// //////////////////////////////***********************************************
	private void printforbiddenXspeed(Expression ex, boolean isAssignment, int row) {
		// TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();
		HashMap<String, Interval> ranges = new HashMap<String, Interval>();

		try {
			RangeExtractor.getVariableRanges(ex, ranges);
		} catch (EmptyRangeException e) {
			System.out.println("XSpeed Cannot handle this model");
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} catch (ConstantMismatchException e) {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		} catch (UnsupportedConditionException e) {
			throw new AutomatonExportException(e.getLocalizedMessage(), e);
		}
		int init = ControlVarI.size();
		int ini = 0;
		int inj = 0;
		double[] ConstraintsMatrixI = new double[ControlVarI.size() * 2];
		for (Entry<String, Interval> e : ranges.entrySet()) {
			String varName = e.getKey();

			if (isAssignment && ControlVarI.contains(varName)) {
				for (int vari = ini; vari < ini + 2; vari++) {
					for (int varj = 0; varj < init; varj++) {
						if (inj / 2 == varj && vari % 2 == 0)
							printLine("forbiddenMatrixI(" + vari + " , " + varj + ") = 1;");
						else if (inj / 2 == varj && vari % 2 == 1)
							printLine("forbiddenMatrixI(" + vari + " , " + varj + ") = -1;");
						else
							printLine("forbiddenMatrixI(" + vari + " , " + varj + ") = 0;");
					}
					inj = inj + 1;
				}
				ini = ini + 2;
			} else {
				// it's a comparison
				/*
				 * if (inter.min == inter.max) printLine(varName + " = " +
				 * doubleToString(inter.min)); else { if (inter.min !=
				 * -Double.MAX_VALUE) printLine(varName + " >= " +
				 * doubleToString(inter.min));
				 * 
				 * if (inter.max != Double.MAX_VALUE) printLine(varName + " <= "
				 * + doubleToString(inter.max)); }
				 */
			}
		}
		printLine("boundValueF.resize(row );");
		for (Entry<String, Interval> e : ranges.entrySet()) {
			if (ControlVarI.contains(e.getKey().toString())) {
				int max = 2 * (CVarcontainX(e.getKey()));
				int min = 2 * (CVarcontainX(e.getKey())) + 1;
				if (isAssignment) {
					ConstraintsMatrixI[max] = e.getValue().max;
					if (e.getValue().min == 0)
						ConstraintsMatrixI[min] = e.getValue().min;
					else
						ConstraintsMatrixI[min] = -e.getValue().min;
				}
			}
		}
		for (int h = 0; h < ControlVarI.size() * 2; h++) {
			printLine("boundValueF[" + h + "]=" + doubleToString(ConstraintsMatrixI[h]) + ";");
		}
		printNewline();
		printLine("forbid_polytope = polytope::ptr(new polytope(forbiddenMatrixI, boundValueF, boundSignI));");
		String[] forstr = new String[config.forbidden.keySet().size()];
		config.forbidden.keySet().toArray(forstr);
		for (Entry<String, Expression> e : config.forbidden.entrySet()) {
			int id = 1;
			for (Entry<String, AutomatonMode> m : ha.modes.entrySet()) {
				AutomatonMode m1 = m.getValue();
				if (m1.name.equals(e.getKey())) {
					forbid = id;
					break;
				}
				id++;
			}
			id = 1;
		}
		printLine("forbid_pair.first = " + forbid + ";");
		printLine("forbid_pair.second = forbid_polytope;");
		printLine("forbidden_set.insert(forbid_pair);");
	}

	// /////**************************************************************

	public static class XspeedExpressionPrinter extends DefaultExpressionPrinter {
		public ArrayList<String> ControlVar = new ArrayList<String>();
		public ArrayList<String> UncontrolVar = new ArrayList<String>();
		int index, i, index0, indexU, indexU0;
		String[] st;
		String[] st1;
		public double[][] cons;
		public double[][] Bmatrix;
		public double[] constraint;

		public XspeedExpressionPrinter(String[] vars, String[] cos, int con, int uncon) {
			super();
			cons = new double[con][con];
			Bmatrix = new double[con][uncon];
			for (int k = 0; k < con; k++)
				for (int j = 0; j < con; j++)
					cons[k][j] = 0;
			for (int k = 0; k < con; k++)
				for (int j = 0; j < uncon; j++)
					Bmatrix[k][j] = 0;

			st = new String[vars.length];
			st = vars;
			st1 = new String[cos.length];
			st1 = cos;
			constraint = new double[con];
			for (int k = 0; k < con; k++)
				constraint[k] = 0;
		}

		public String printOperationExspeed(Operation o, int j) {
			String rv;
			int subtract=0;
			if(o.op == Operator.SUBTRACT && o.getRight() instanceof Operation)
			{
				String left=o.getLeft().toString();
				left = left.replaceAll("\\s","");
					if(left.charAt(left.length()-1)!='-' || left.charAt(left.length()-1)!='+')
					{	subtract=1;}
			}
			List<Expression> children = o.children;
			Operator op = o.op;
			if (op.equals(Operator.EQUAL)) {
				if (children.get(1) instanceof Constant) {
					constraint[j] = Double.parseDouble(children.get(1).toString());
				}
			}

			if (children.size() == 0) {
				rv = printOperator(o.op);
			} 
			else if (children.size() == 1) {
				Expression child = children.get(0);
				
				if (op.equals(Operator.NEGATIVE) || op.equals(Operator.LOGICAL_NOT)) {
					if (child instanceof Operation && child.asOperation().children.size() > 1)
					{	rv = opNames.get(o.op) + "(" + printExspeed(child, j) + ")";
					
					}else
					{
						rv = opNames.get(o.op) + "" + printExspeed(child, j);
					}
				} else {

					rv = opNames.get(o.op) + "(" + printExspeed(child, j) + ")";
				}
				for (int l = 0; l < st1.length; l++) {
					if (child.toString().equals(st1[l])) {
						constraint[j] = Double.parseDouble(rv);
						break;
					}
				}
			} else if (children.size() == 2) {
				Expression leftExp = children.get(0);
				Operation left = leftExp.asOperation();
				Expression rightExp = children.get(1);
				Operation right = rightExp.asOperation();
				if(subtract==1)
				{
					subtract=0;
					if(o.getRight().asOperation().getLeft() instanceof Constant && o.getRight().asOperation().getRight() instanceof Variable){
						double d=-Double.parseDouble(o.getRight().asOperation().getLeft().toString());
						Expression cons=new Constant(d);
						Expression ex=new Operation(cons,o.getRight().asOperation().op,o.getRight().asOperation().getRight());
						rightExp=ex;
					}
				}
				
				boolean needParenLeft = false;
				boolean needParenRight = false;

				rv = "";
				// use parentheses if they are needed
				if(leftExp instanceof Constant && (o.op==Operator.ADD || o.op==Operator.SUBTRACT)){
						constraint[j] = Double.parseDouble(leftExp.toString());
				}
				if (children.get(1) instanceof Variable && children.get(0) instanceof Variable
						&& o.op.equals(Operator.MULTIPLY)) {
					throw new RuntimeException("XSpeed does not work on non-linear models\n");

				}
				if (children.get(1) instanceof Variable && children.get(0) instanceof Variable
						&& opNames.get(o.op) == "+") {
					for (int k = 0; k < ControlVar.size(); k++) {
						if (ControlVar.get(k).equals((children.get(1).toString()))) {
							cons[j][k] = Double.parseDouble("1.0");
						}
						if (ControlVar.get(k).equals((children.get(0).toString()))) {
							cons[j][k] = Double.parseDouble("1.0");
						}
					}
					for (int k = 0; k < UncontrolVar.size(); k++) {
						if (UncontrolVar.get(k).equals((children.get(1).toString()))) {
							Bmatrix[j][k] = Double.parseDouble("1.0");
							break;
						}
						if (UncontrolVar.get(k).equals((children.get(0).toString()))) {
							Bmatrix[j][k] = Double.parseDouble("1.0");
							break;
						}
					}
				}
				
				if (children.get(1) instanceof Variable && children.get(0) instanceof Constant
						&& o.op.equals(Operator.MULTIPLY)) {
					for (int k = 0; k < ControlVar.size(); k++) {
						if (ControlVar.get(k).equals((children.get(1).toString()))) {
								cons[j][k] = Double.parseDouble(children.get(0).toString());
							break;
						}
					}
					for (int k = 0; k < UncontrolVar.size(); k++) {
						if (UncontrolVar.get(k).equals((children.get(1).toString()))) {
								Bmatrix[j][k] = Double.parseDouble(children.get(0).toString());
							break;
						}
					}
				}

				else if ((rightExp instanceof Constant) && (opNames.get(o.op) == "+" || opNames.get(o.op) == "-")) {
					if (opNames.get(o.op) == "-") {
						constraint[j] = -Double.parseDouble(rightExp.toString());
					} else {
						constraint[j] = Double.parseDouble(rightExp.toString());
					}
					rv += printExspeed(leftExp, j);
				}

				else if (left instanceof Operation && rightExp instanceof Variable && opNames.get(o.op) == "-") {
					for (int k = 0; k < ControlVar.size(); k++) {
						if (ControlVar.get(k).equals(rightExp)) {
							cons[j][k] = -1;
							break;
						}
					}

					for (int k = 0; k < UncontrolVar.size(); k++) {
						if (UncontrolVar.get(k).equals(rightExp)) {
							cons[j][k] = -1;
							break;
						}
					}
					rv += printExspeed(leftExp, j);
				}

				else if (left instanceof Operation && rightExp instanceof Variable && opNames.get(o.op) == "+") {
					for (int k = 0; k < ControlVar.size(); k++) {
						if (ControlVar.get(k).toString().equals(rightExp.toString())) {
							cons[j][k] = 1;
							break;
						}
					}

					for (int k = 0; k < UncontrolVar.size(); k++) {
						if (UncontrolVar.get(k).toString().equals(rightExp.toString())) {
							Bmatrix[j][k]=1;
							break;
						}
					}
					rv += printExspeed(leftExp, j);
				}
				/*
				 * else if (left instanceof Variable && rightExp instanceof
				 * Variable && opNames.get(o.op) == "+"){
				 * 
				 * }
				 */
				else {
					int myP = Operator.getPriority(op);
					if (left != null && left.children.size() > 1) {
						int leftP = Operator.getPriority(left.op);

						if (leftP < myP)
							needParenLeft = true;
					}

					if (right != null && right.children.size() > 1) {
						int rightP = Operator.getPriority(right.op);

						if (myP > rightP || (myP == rightP && !Operator.isCommutative(op))) // commutative
							needParenRight = true;
					}

					if (needParenLeft) {
						rv += "(" + printExspeed(leftExp, j) + ")";
						// maybe not strictly necessary as the
						// expression.toString is overriden to call this print,
						// but was having problems with this
					} else {
						rv += printExspeed(leftExp, j);
					}

					rv += " " + opNames.get(o.op) + " ";
					if (needParenRight) {
						rv += "(" + printExspeed(rightExp, j) + ")";
					} else {
						rv += printExspeed(rightExp, j);
					}
				}
			} else {
				throw new AutomatonExportException("No default way to in-line print expression with " + children.size()
						+ " children (" + opNames.get(o.op) + ".");
			}
			return rv;
		}
		public double cons(int cons1, int cons2) {
			return cons[cons1][cons2];
		}

		public double Bmatrix(int cons1, int cons2) {
			return Bmatrix[cons1][cons2];
		}

		public double Cmatrix(int cons1) {
			return constraint[cons1];
		}

		public void saveBmatrix2(String var, int j, int k, double value) {
			Bmatrix[j][k] = value;
		}

		public void saveAmatrix2(String var, int j, int k, double value) {
			cons[j][k] = value;
		}

		public void saveCmatrix2(int j, double value) {
			constraint[j] = value;
		}

		public String printExspeedFirst(Expression e, int j, ArrayList<String> Control, ArrayList<String> Uncontrol) {
			String rv = null;
			ControlVar = Control;
			UncontrolVar = Uncontrol;
			if (e == null)
				rv = "null";
			else if (e instanceof Constant) {
				rv = e.toString();
				saveCmatrix(rv, j);
			} 
			else if (e.toString().charAt(0) == '-' && (e.toString().substring(1).indexOf('-') == (-1)
					&& e.toString().substring(1).indexOf('+') == (-1)) && ControlVar.contains(e.toString().substring(1))) {
				String t = e.toString().substring(1);
				if (ControlVar.contains(t)) {
					rv = t;
					for (int i = 0; i < ControlVar.size(); i++) {
						if (ControlVar.get(i).equals(t))
							cons[j][i] = -1;
					}
				}
			} 
			else if (e instanceof Operation) {
				rv = printOperationExspeed((Operation) e, j);
			} 
			else if (e instanceof Variable) {
				if (ControlVar.contains(e.toString())) {
					rv = printVariable((Variable) e);
					saveAmatrix(rv, j);
				}
				if (UncontrolVar.contains(e.toString())) {
					rv = printVariable((Variable) e);
					saveBmatrix(rv, j);
				}
			} else {
				rv = printOther(e);
			}
			return rv;
		}

		public String printExspeed(Expression e, int j) {
			String rv = null;
			if (e == null)
				rv = "null";
			else if (e instanceof Constant) {
				rv = printConstant((Constant) e);
			} else if (e instanceof Operation) {
				rv = printOperationExspeed((Operation) e, j);
			} else if (e instanceof Variable) {
				rv = printVariable((Variable) e);
			} else {
				rv = printOther(e);
			}
			return rv;

		}

		public String printOther(Expression e) {
			return e.toString();
		}

		public String printVariable(Variable v) {
			return v.name;
		}

		public String printConstant(Constant c) {
			String rv = null;

			if (c == Constant.TRUE)
				rv = printTrue();
			else if (c == Constant.FALSE)
				rv = printFalse();
			else
				rv = printConstantValue(c.getVal());
			return rv;
		}

		public String printConstantValue(double d) {
			return "" + d;
		}

		public String printTrue() {
			return "true";
		}

		public String printFalse() {
			return "false";
		}

		// although Operators are technically not expressions, it's better to
		// define this here to keep the printing all in once place

		/**
		 * Prefix printing for everything
		 * 
		 * @param o
		 * @return
		 */
		public String printOperation(Operation o) {
			String childrenStr = "";
			for (Expression e : o.children) {
				childrenStr += " " + print(e);
			}
			return "(" + printOperator(o.op) + childrenStr + ")";
		}

		public void saveAmatrix(String stx, int j) {
			for (int k = 0; k < ControlVar.size(); k++) {
				if (ControlVar.get(k).equals(stx.toString())) {
					index = k;
					cons[j][index] = 1;
					break;
				}
			}
		}

		public void saveCmatrix(String stl, int j) {
			constraint[j] = Double.parseDouble(stl);
		}

		public void saveBmatrix(String stl, int j) {
			for (int k = 0; k < UncontrolVar.size(); k++) {
				if (UncontrolVar.get(k).equals(stl.toString())) {
					index = k;
					Bmatrix[j][index] = 1;
					break;
				}
			}
		}
	}

	// ////////////////////////***********************************************

	@Override
	protected void printAutomaton() {
		this.ha = (BaseComponent) config.root;
		Expression.expressionPrinter = DefaultExpressionPrinter.instance;
		if (ha.modes.containsKey("init"))
			throw new AutomatonExportException("mode named 'init' is not allowed in Flow* printer");

		if (config.init.size() > 1) {
			Hyst.log(config.init.size() + " initial modes detected");
			// Hyst.log("Multiple initial modes detected (not supported by
			// Flow*). Converting to single urgent one.");
			// convertInitialModes(config);
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
			AutomatonTransition at = ha.createTransition(init, ha.modes.get(modeName));
			at.guard = Constant.TRUE;

			Expression resetExp = removeConstants(e.getValue(), constants);

			TreeMap<String, Interval> ranges = new TreeMap<String, Interval>();
			try {
				RangeExtractor.getVariableRanges(e.getValue(), ranges);
			} catch (EmptyRangeException e1) {
				System.out.println("XSpeed Cannot handle those models");
				throw new AutomatonExportException("Empty range in initial mode: " + modeName, e1);
			} catch (ConstantMismatchException e2) {
				System.out.println("XSpeed Cannot handle those models");
				throw new AutomatonExportException("Constant mismatch in initial mode: " + modeName, e2);
			} catch (UnsupportedConditionException e3) {
				throw new AutomatonExportException(e3.getLocalizedMessage(), e3);
			}

			Collection<String> vars = AutomatonUtil.getVariablesInExpression(resetExp);

			for (String var : vars) {
				Interval i = ranges.get(var);

				if (i == null)
					throw new AutomatonExportException("Variable " + var + " not defined in initial mode " + modeName);

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
		return "XSpeed";
	}

	@Override
	public String getCommandLineFlag() {
		return "xspeed";
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

	// getsizeofInvX(Expression e) return the size of the invariant expression
	// with number of variables available
	public int getsizeofInvX(Expression e) {
		if (e instanceof Operation) {
			Operation o = (Operation) e;
			if (o.op == Operator.AND) {
				invSize = getsizeofInvX(o.getLeft()) + getsizeofInvX(o.getRight()) + 1;
				return invSize;
			} else {
				if (o.op == Operator.EQUAL) {
					invSize = getsizeofInvX(o.getLeft()) + getsizeofInvX(o.getRight()) + 1;
					return invSize;
				} else {

					if ((o.op == Operator.NEGATIVE) || (o.op == Operator.LOGICAL_NOT)) {
						String exp = e.toString().substring(1);
						Expression ee=new Variable(exp);
						invSize = getsizeofInvX(ee);
					}
					else
						invSize = getsizeofInvX(o.getLeft()) + getsizeofInvX(o.getRight());
					return invSize;
				}
			}
		} else {
			if (UncontrolVar.contains(e.toString()))
				Uinvsize = Uinvsize + 1;
			return 0;
		}
	}

	public int getsizeofInit(Expression e) {
		if (e instanceof Operation) {
			Operation o = (Operation) e;
			if (o.op == Operator.AND) {
				invSize = getsizeofInvX(o.getLeft()) + getsizeofInvX(o.getRight()) + 1;
				return invSize;
			} else {
				invSize = getsizeofInvX(o.getLeft()) + getsizeofInvX(o.getRight());
				return invSize;
			}
		} else
			return 0;
	}

	public int CVarcontainX(String e)// it returns the index of the control variable from list
	{
		int i;
		String[] st = new String[ControlVar.size()];
		st = ControlVar.toArray(st);
		for (i = 0; i < ControlVar.size(); i++) {
			if (st[i].equals(e)) {
				break;
			}
		}
		return i;
	}

	public int CVarcontainI(String e)// needed for guard polytope.
	{
		int i;
		String[] st = new String[ControlVarI.size()];
		st = ControlVarI.toArray(st);
		if (ControlVarI.size() == 0)
			return -1;
		for (i = 0; i < ControlVarI.size(); i++) {
			if (st[i].equals(e)) {
				break;
			}
		}
		return i;
	}

	public int UVarcontainX(String e)// it returns the index of the Uncontrol
										// variable list
	{
		int i;
		String[] st = new String[UncontrolVar.size()];
		st = UncontrolVar.toArray(st);
		for (i = 0; i < UncontrolVar.size(); i++) {
			if (st[i].equals(e)) {
				break;
			}
		}
		return i;
	}

	// It returns the ID of the location.
	public int ModeIdXspeed(AutomatonMode m) {
		int i = 1;
		for (Entry<String, AutomatonMode> t : ha.modes.entrySet()) {
			AutomatonMode mode = t.getValue();
			if (mode.hashCode() == m.hashCode()) {
				break;
			} else {
				i++;
			}
		}
		return i;
	}

	// it returns the Id of the transition by taking the Label of the transition
	public int TransIdXspeed(String m) {
		int i = 1;
		for (AutomatonTransition t : ha.transitions) {
			if (t.label == null) {
				throw new RuntimeException("This Model does not have transition ID\n");
			}
			String l = t.label;
			if (l.equals(m)) {
				break;
			} else {
				i++;
			}
		}
		return i;
	}

	public void resetExtract(int ind, Operation op) {
		Operator o = op.op;
		if (o.equals(Operator.MULTIPLY)) {
			int i = CVarcontainX(op.getRight().toString());
			Rmatrix[ind][i] = Double.parseDouble(op.getLeft().toString());
		} else if (o.equals(Operator.ADD) || o.equals(Operator.NEGATIVE)) {
			if (op.getRight() instanceof Constant && o.equals(Operator.ADD)) {
				Wmatrix[ind] = Double.parseDouble(op.getRight().toString());
				resetExtract(ind, (Operation) op.getLeft());
			} else if (op.getRight() instanceof Constant && o.equals(Operator.NEGATIVE)) {
				Wmatrix[ind] = -Double.parseDouble(op.getRight().toString());
				resetExtract(ind, (Operation) op.getLeft());
			} else {
				resetExtract(ind, (Operation) op.getRight());
				resetExtract(ind, (Operation) op.getLeft());
			}
		}
	}

	public boolean ConstCheck(String e) {
		String[] st = new String[ha.constants.size()];
		st = ha.constants.keySet().toArray(st);
		for (int i = 0; i < st.length; i++) {
			if (e.equals(st[i])) {
				return true;
			}
		}
		return false;
	}
}
