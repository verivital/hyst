package com.verivital.hyst.passes.flat;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeSet;

import com.verivital.hyst.grammar.formula.Constant;
import com.verivital.hyst.grammar.formula.DefaultExpressionPrinter;
import com.verivital.hyst.grammar.formula.Expression;
import com.verivital.hyst.grammar.formula.FormulaParser;
import com.verivital.hyst.ir.AutomatonExportException;
import com.verivital.hyst.ir.AutomatonValidationException;
import com.verivital.hyst.ir.base.AutomatonMode;
import com.verivital.hyst.ir.base.BaseComponent;
import com.verivital.hyst.ir.base.ExpressionInterval;
import com.verivital.hyst.ir.base.Interval;
import com.verivital.hyst.passes.TransformationPass;
import com.verivital.hyst.passes.complex.HybridizeGridPass;
import com.verivital.hyst.simulation.Simulator;
import com.verivital.hyst.util.AutomatonUtil;

public class SimulatorPass extends TransformationPass {

    class Pair<L, R> {

        public final L left;
        public final R right;

        public Pair(L left, R right) {
            this.left = left;
            this.right = right;
        }
    }

    private BaseComponent ha;
    private AutomatonMode initialMode;
    private TreeSet<Double> simTimes;
    private double[] initialPoint;
    private double delta = 0.01;
    private HashSet<Pair<AutomatonMode, AutomatonMode>> noIntersectionModes;
    private String timeVariable;
    private int noOfSimulations = 1;
    private boolean someBooleanVariableIsTrue = true;
    private double lastTime = 0.0;
    private AutomatonMode lastTimeMode;

    @Override
    public String getName()
    {
        return "Simulator Pass";
    }

    @Override
    public String getCommandLineFlag()
    {
        return "-simulate";
    }

    @Override
    public String getParamHelp()
    {
        return "[no. of simulations,time based guards,time1,time2,...]";
    }

    @Override
    protected void runPass(String params)
    {
        /*Expression.expressionPrinter = DefaultExpressionPrinter.instance;

        this.ha = (BaseComponent)config.root;

        if(recursion()){
            epsilonIncrementRoutine();
        }
        else {
            validate();
            setup(params);

            if (someBooleanVariableIsTrue)
                doEverythingDifferently();
            else
                doEverything();

            writeToFile("temp_isRecursionNeeded.txt", "yes");
        }*/
    }

   /* private void doEverythingDifferently(){
        findTimeVariable();
        createBoxes();
        createTransitions();
        addInitialConditions();
        addForbiddenConditions();
        affineOptimize();
        addTimeVariable();
        addSinkStates();
    }

    private void doEverything(){
        findTimeVariable();
        createBoxes();
        union();
        createTransitions();
        addInitialConditions();
        addForbiddenConditions();
        affineOptimize();
        addSinkStates();
    }

    private void findTimeVariable(){
        boolean timeExists = false;

        for(String var : ha.variables){
            if(var.toLowerCase().equals("t") || var.toLowerCase().equals("time")) {
                timeExists = true;
                timeVariable = var;
            }
        }

        if(!timeExists) {
            timeVariable = "t";
        }
    }

    private void addTimeVariable(){
        if(!ha.variables.contains(timeVariable)) {
            ha.variables.add(timeVariable);
        }

        for(AutomatonMode am : ha.modes.values()){
            am.flowDynamics.put(timeVariable, new ExpressionInterval(new Constant(1)));
        }
    }

    private void affineOptimize(){

        HybridizeGridPass.affineOptimize(ha, 0.01);
    }

    private double[] getRandomInitialPoint(){
        double[] rv = new double[ha.variables.size()];

        HashMap<String, Interval> bounds = AutomatonUtil.getIntervals(config.init.values().iterator().next());
        for(int i = 0;i<ha.variables.size();++i){
            rv[i] = bounds.get(ha.variables.get(i)).randomPoint();
        }

        return rv;
    }

    private void createBoxes(){

        ha.modes.clear();
        ArrayList<double[]> rv = simulationPoints(initialPoint);

        for(int y=0;y<noOfSimulations;++y) {

            if(someBooleanVariableIsTrue){

                TreeSet<Double> simTimesCopy = new TreeSet<Double>(simTimes);
                simTimesCopy.remove(0.0);

                AutomatonMode am = null;

                for(int i=0;i<rv.size()-1;++i){
                    String invariant = lastTime + " <= " + timeVariable + " <= " + simTimesCopy.first() + " && " + getInvariants(rv.get(i), rv.get(i + 1));

                    am = ha.createMode("Mode" + y + "_" + i, invariant, "");
                    am.flowDynamics = new LinkedHashMap<String, ExpressionInterval>(initialMode.flowDynamics);

                    lastTime = simTimesCopy.first();
                    simTimesCopy.remove(simTimesCopy.first());
                }

                lastTimeMode = am;
            }

            else {
                noIntersectionModes = new HashSet<Pair<AutomatonMode, AutomatonMode>>();

                double[] firstPoint = rv.get(0);
                double[] secondPoint = rv.get(1);
                double[] lastPoint = new double[firstPoint.length];
                double[] nextPoint = new double[firstPoint.length];


                for(int i=0;i<firstPoint.length;++i){
                    lastPoint[i] = (3*firstPoint[i] - secondPoint[i])/2;
                }

                for(int i=0;i<rv.size()-1;++i){
                    firstPoint = rv.get(i);
                    secondPoint = rv.get(i+1);

                    for(int j=0;j<firstPoint.length;++j) {
                        nextPoint[j] = (firstPoint[j] + secondPoint[j]) / 2;
                    }

                    AutomatonMode am = ha.createMode("Mode" + y + "_" + i, getInvariants(lastPoint, nextPoint), "");
                    am.flowDynamics = new LinkedHashMap<String, ExpressionInterval>(initialMode.flowDynamics);

                    lastPoint = Arrays.copyOf(nextPoint, nextPoint.length);
                }

                for(int j=0;j<lastPoint.length;++j)
                    nextPoint[j] = 2*secondPoint[j] - lastPoint[j];

                AutomatonMode am = ha.createMode("Mode" + y + "_" + (rv.size() - 1), getInvariants(lastPoint, nextPoint), "");
                am.flowDynamics = new LinkedHashMap<String, ExpressionInterval>(initialMode.flowDynamics);

                ArrayList<AutomatonMode> l = new ArrayList<AutomatonMode>(ha.modes.values());
                for(int i=0;i<l.size()-1;++i){
                    noIntersectionModes.add(new Pair<AutomatonMode, AutomatonMode>(l.get(i), l.get(i+1)));
                }
            }

            rv = simulationPoints(getRandomInitialPoint());
        }
    }

    private void union(){
        boolean added;
        ArrayList<ArrayList<AutomatonMode>> temp = new ArrayList<ArrayList<AutomatonMode>>();

        ArrayList<AutomatonMode> modes = new ArrayList<AutomatonMode>(ha.modes.values());

        for(int i=0;i<modes.size();++i){
            AutomatonMode am = modes.get(i);
            added = false;

            for(int j=0;j<temp.size();++j){
                for(int k=0;k<temp.get(j).size();++k){
                    if(shouldITakeUnionIntersectionOrNone(am, temp.get(j).get(k)).equals("u")){
                        temp.get(j).add(am);
                        added = true;
                        break;
                    }
                }
                if(added)
                    break;
            }

            if(!added)
                temp.add(new ArrayList<AutomatonMode>(Collections.singletonList(am)));
        }

        for(int i=0;i<temp.size();++i) {
            AutomatonMode am = temp.get(i).get(0);
            for (int j = 1; j < temp.get(i).size(); ++j) {
                unionOfModes(am, temp.get(i).get(j));

                System.out.println(am.name + " :: " + temp.get(i).get(j).name);
                String oldName = am.name;
                am.name = am.name + "_" + temp.get(i).get(j).name.replaceAll("Mode", "");
                ha.modes.put(am.name, ha.modes.remove(oldName));
                ha.modes.remove(temp.get(i).get(j).name);
            }
        }

        for(int i=0;i<temp.size();++i){
            if(temp.get(i).size() != 1) {
                union();
                break;
            }
        }
    }

    private String shouldITakeUnionIntersectionOrNone(AutomatonMode a, AutomatonMode b){
        double K = 0.6;

        if(noIntersectionModes.contains(new Pair<AutomatonMode, AutomatonMode>(a, b))){
            return "n";
        }

        HashMap<String, Interval> hm_a = AutomatonUtil.getIntervals(a);

        HashMap<String, Interval> hm_b = AutomatonUtil.getIntervals(b);

        double intersectionVolume = volumeOfIntervalHashMap(AutomatonUtil.intersectionOfRegions(hm_a, hm_b));

        double unionVolume = volumeOfIntervalHashMap(unionOfRegions(hm_a, hm_b));

        double percentVolume = intersectionVolume/unionVolume;

        if(percentVolume > K)
            return "u";
        else if(percentVolume > 0 && percentVolume <= K)
            return "i";
        else return "n";
    }

    private void unionOfModes(AutomatonMode a, AutomatonMode b){

        HashMap<String, Interval> boundsOfA = AutomatonUtil.getIntervals(a);

        HashMap<String, Interval> boundsOfB = AutomatonUtil.getIntervals(b);

        HashMap<String, Interval> temp = new HashMap<String, Interval>();

        for(String var : boundsOfA.keySet()){
            temp.put(var, Interval.union(boundsOfA.get(var), boundsOfB.get(var)));
        }

        a.invariant = FormulaParser.parseInvariant(intervalMapToInvariant(temp));
    }

    private HashSet<HashMap<String, Interval>> splittingOfModes(AutomatonMode a, AutomatonMode b){
        HashMap<String, Interval> hm_a = AutomatonUtil.getIntervals(a);

        HashMap<String, Interval> hm_b = AutomatonUtil.getIntervals(b);

        HashMap<String, Interval> intersection = AutomatonUtil.intersectionOfRegions(hm_a, hm_b);

        HashSet<HashMap<String, Interval>> newBoxes = splittingOfModesHelper(hm_a, intersection);
        newBoxes.addAll(splittingOfModesHelper(hm_b, intersection));
        newBoxes.remove(intersection);

        return newBoxes;
    }

    private HashSet<HashMap<String, Interval>> splittingOfModesHelper(HashMap<String, Interval> originalBox, HashMap<String, Interval> intersection){

        HashMap<String, ArrayList<Interval>> partitions = generatePartitions(originalBox, intersection);

        HashSet<HashMap<String, Interval>> newBoxes = new HashSet<HashMap<String, Interval>>();
        HashSet<HashMap<String, Interval>> temp = new HashSet<HashMap<String, Interval>>();

        ArrayList<String> variables = new ArrayList<String>(partitions.keySet());
        String var = variables.get(0);

        for(Interval i : partitions.get(var)) {
            HashMap<String, Interval> rv = new HashMap<String, Interval>();
            rv.put(var, i);
            newBoxes.add(rv);
        }

        for(int l = 1;l<variables.size();++l){
            var = variables.get(l);
            temp.clear();
            for(HashMap<String, Interval> rv : newBoxes){
                for(Interval i : partitions.get(var)){
                    HashMap<String, Interval> u = new HashMap<String, Interval>(rv);
                    u.put(var, i);
                    temp.add(u);
                }
            }
            newBoxes = new HashSet<HashMap<String, Interval>>(temp);
        }

        return newBoxes;
    }

    private HashMap<String, ArrayList<Interval>> generatePartitions(HashMap<String, Interval> originalBox, HashMap<String, Interval> intersection){

        HashMap<String, ArrayList<Interval>> partitions = new HashMap<String, ArrayList<Interval>>();

        for(String var : originalBox.keySet()){
            partitions.put(var, new ArrayList<Interval>());
            Interval intersectionInterval = intersection.get(var);
            Interval originalInterval = originalBox.get(var);

            partitions.get(var).add(new Interval(intersectionInterval));

            if(originalInterval.min < intersectionInterval.min) {
                partitions.get(var).add(new Interval(originalInterval.min, intersectionInterval.min));
            }

            if(originalInterval.max > intersectionInterval.max) {
                partitions.get(var).add(new Interval(intersectionInterval.max, originalInterval.max));
            }

        }

        return partitions;
    }

    private String intervalMapToInvariant(HashMap<String, Interval> hm){

        StringBuilder ret = new StringBuilder();
        String temp = "";

        for(String var : hm.keySet()){
            ret.append(temp + hm.get(var).min + " <= " + var + " <= " + hm.get(var).max);
            temp = " && ";
        }

        return ret.toString();
    }

    private void createTransitions(){

        if(someBooleanVariableIsTrue){
            ArrayList<AutomatonMode> modes = new ArrayList<AutomatonMode>(ha.modes.values());

            TreeSet<Double> simTimesCopy = new TreeSet<Double>(simTimes);
            simTimesCopy.remove(0.0);

            for(int i=0;i<modes.size()-1;++i){
                ha.createTransition(modes.get(i), modes.get(i + 1), "t <= " + simTimesCopy.first());
                simTimesCopy.remove(simTimesCopy.first());
            }
        }

        else {

            ArrayList<String> temp = new ArrayList<String>(ha.modes.keySet());
            for(int i=0;i<temp.size()-1;++i){
                for(int j=i+1;j<temp.size();++j)
                ha.createTransition(ha.modes.get(temp.get(i)), ha.modes.get(temp.get(j)), "true");
            }
        }

    }

    private void addInitialConditions(){
        ArrayList<Expression> l = new ArrayList<Expression>(config.init.values());
        config.init.clear();

        String name = ha.modes.keySet().iterator().next();

        for(int i=0;i<l.size();++i){
            config.init.put(name, l.get(i));
        }

        if(someBooleanVariableIsTrue){
            String inv = config.init.get(name).toString() + " && " + timeVariable + " == 0";
            config.init.put(name, FormulaParser.parseInvariant(inv));
        }
    }

    private void addForbiddenConditions(){

        ArrayList<Expression> forbidden = new ArrayList<Expression>(config.forbidden.values());
        config.forbidden.clear();

        
        //for(String name : ha.modes.keySet()){
        //    for(int i=0;i<forbidden.size();++i) {
        //        ha.forbidden.put(name, forbidden.get(i));
        //    }
        //}
        
    }

    private String getInvariants(double[] lastPoint, double[] nextPoint){
        StringBuilder s = new StringBuilder();
        String temp = "";

        for(int i=0;i<lastPoint.length;++i){
            double min = Math.min(lastPoint[i], nextPoint[i]);
            double max = Math.max(lastPoint[i], nextPoint[i]);
            s.append(temp);
            s.append(min - (Math.abs(min) + delta)*delta);
            s.append(" <= ");
            s.append(ha.variables.get(i));
            s.append(" <= ");
            s.append(max + (Math.abs(max) + delta)*delta);
            temp = " && ";
        }

        return s.toString();
    }

    private String sinkFlowString(){
        String s = "";

        for(String var : ha.variables){
            if(!timeVariable.equals(var))
                s += var + "' == 0 && ";
        }

        if(someBooleanVariableIsTrue){
            s += timeVariable + "' == 0 && ";
        }

        return s.substring(0, s.length()-4);
    }

    private void addSinkStates(){

        String auxiliaryFlow = sinkFlowString();

        for(AutomatonMode am : new ArrayList<AutomatonMode>(ha.modes.values())){
            HashMap<String, Interval> bounds = AutomatonUtil.getIntervals(am);

            HashMap<String, Interval> temp = new HashMap<String, Interval>(bounds);
            for(String var : temp.keySet()){

                if(!timeVariable.equals(var)){
                    Interval i = bounds.get(var);

                    Interval left = new Interval(i.min - (Math.abs(i.min) + i.min == 0 ? delta:0)*delta, i.min);
                    Interval right = new Interval(i.max, i.max + (Math.abs(i.max) + i.max == 0 ? delta:0)*delta);

                    temp.put(var, left);
                    if(isSinkNeeded(temp)){
                        AutomatonMode rv = ha.createMode("SinkFor_" + am.name + "_alongNegative_" + var, intervalMapToInvariant(temp), auxiliaryFlow);
                        ha.createTransition(am, rv, "true");
                        config.forbidden.put(rv.name, rv.invariant);
                    }

                    temp.put(var, right);
                    if(isSinkNeeded(temp)){
                        AutomatonMode rv = ha.createMode("SinkFor_" + am.name + "_alongPositive_" + var, intervalMapToInvariant(temp), auxiliaryFlow);
                        ha.createTransition(am, rv, "true");
                        config.forbidden.put(rv.name, rv.invariant);
                    }

                    temp.put(var, i);
                }
            }
        }

        if(someBooleanVariableIsTrue) {
            AutomatonMode sink = ha.createMode("SinkFor_" + lastTimeMode.name + "_alongPositive_" + timeVariable, "true", auxiliaryFlow);
            ha.createTransition(lastTimeMode, sink, timeVariable + " >= " + lastTime);
            config.forbidden.put(sink.name, sink.invariant);
        }
    }

    private boolean isSinkNeeded(HashMap<String, Interval> hm){

        for(AutomatonMode am : ha.modes.values()){
            HashMap<String, Interval> temp = AutomatonUtil.getIntervals(am);
            HashMap<String, Interval> intersection = AutomatonUtil.intersectionOfRegions(hm, temp);

            if(AutomatonUtil.intervalMapIsNotNull(intersection))
                return false;
        }
        return true;
    }

    private ArrayList<double[]> simulationPoints(double[] initialPoint){
        int NUM_STEPS = 500;

        ArrayList<double[]> rv = new ArrayList<double[]>();

        for(double time : simTimes) {
            rv.add(Simulator.simulateFor(time, initialPoint, NUM_STEPS, initialMode.flowDynamics, ha.variables, null));
        }

        return rv;
    }

    private HashMap<String, Interval> unionOfRegions(HashMap<String, Interval> a, HashMap<String, Interval> b){
        HashMap<String, Interval> intersectionRegion = new HashMap<String, Interval>();

        for(String s : a.keySet()){
            intersectionRegion.put(s, Interval.union(a.get(s), b.get(s)));
        }

        return intersectionRegion;
    }

    private void validate(){
        if(ha.modes.size() != 1)
            throw new AutomatonValidationException("Number of modes != 1");

        initialMode = ha.modes.get(ha.modes.keySet().iterator().next());
    }

    private void setup(String params){
        processParams(params);

        initialMode = ha.modes.get(config.init.keySet().iterator().next());

        initialPoint = AutomatonUtil.getInitialPoint(ha, config);
    }

    private void processParams(String params){
        String[] parts = params.split(",");

        if (parts.length < 3)
            throw new AutomatonExportException("Expected param with 'no. of simulations,time based guards,time1,time2,...'");

        try{
            noOfSimulations = Integer.parseInt(parts[0]);

            if(noOfSimulations < 1)
                throw new AutomatonValidationException("Number of simulations was < 1 " + noOfSimulations);
        }
        catch(NumberFormatException e){
            throw new AutomatonValidationException("Cannot parse Number of simulations:" + e);
        }

        if(parts[1].equals("t"))
            someBooleanVariableIsTrue = true;
        else if(parts[1].equals("m"))
            someBooleanVariableIsTrue = false;
        else throw new AutomatonValidationException("Time based guard flag wrong!");

        simTimes = new TreeSet<Double>();

        for (int p = 2; p < parts.length; ++p)
        {
            try
            {
                double time = Double.parseDouble(parts[p]);

                if (time < 0)
                    throw new AutomatonExportException("Time was negative: " + time);

                simTimes.add(time);
            }
            catch (NumberFormatException e)
            {
                throw new AutomatonExportException("Error parsing time: " + e);
            }
        }

        if(!simTimes.contains(0.0))
            simTimes.add(0.0);
    }

    private double volumeOfHyperBox(AutomatonMode am){

        return volumeOfIntervalHashMap(AutomatonUtil.getIntervals(am));
    }

    private double volumeOfIntervalHashMap(HashMap<String, Interval> hm){

        double volume = 1.0;

        ArrayList<Interval> inter = new ArrayList<Interval>(hm.values());
        for(int i=0;i<inter.size();++i){

            if(inter.get(i) == null)
                return 0.0;

            volume *= inter.get(i).width();
        }

        return volume;
    }

    private boolean recursion(){
        boolean rv = false;

        try {
            String line;
            BufferedReader inp = new BufferedReader(new FileReader("temp_isRecursionNeeded.txt"));
            while ((line = inp.readLine()) != null) {
                if(line.equals("yes")) {
                    rv = true;
                }
            }
        } catch(Exception e){}

        return rv;
    }

    private static boolean writeToFile(String fileName, String text){
        try{
            PrintWriter writer = new PrintWriter(fileName);
            writer.println(text);
            writer.close();

            return true;

        } catch(Exception e){
            e.printStackTrace();

            return false;
        }
    }

    private void epsilonIncrementRoutine(){
        Process p;

        System.out.println("Trying to run SpaceEx");

        LinkedHashMap<String, ArrayList<String>> errorStates = new LinkedHashMap<String, ArrayList<String>>();

        try {
            p = Runtime.getRuntime().exec("../tools/spaceex/spaceex --model-file sim.xml --config sim.cfg --output-file temp.intv");
            p.waitFor();

            BufferedReader reader =	new BufferedReader(new FileReader("temp.intv"));

            String line = "";
            while ((line = reader.readLine()) != null) {
                if(line.startsWith("Location:")){
                    int index = line.indexOf('_') + 1;
                    line = line.substring(index, line.length());
                    index = line.lastIndexOf('_');

                    String direction = line.substring(index+1, line.length());
                    String sign = line.substring(index-8,index).equals("Positive") ? "+" : "-";
                    String mode = line.substring(0, index-14);

                    if(!errorStates.containsKey(mode)){
                        errorStates.put(mode, new ArrayList<String>());
                        errorStates.get(mode).add(sign+direction);
                    }
                    else{
                        errorStates.get(mode).add(sign+direction);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Trying to hybridize");
        }

        System.out.println(errorStates);

        if(errorStates.isEmpty()){
            writeToFile("temp_isRecursionNeeded.txt", "no");
        }
        else{
            //using error states, expand the system
        }
        //writeToFile("temp_isRecursionNeeded.txt", "no");
    }
    
    */
}