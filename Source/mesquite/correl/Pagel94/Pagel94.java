/* Mesquite source code.  Copyright 1997-2006 W. Maddison and D. Maddison. 
 This module copyright 2006 P. Midford and W. Maddison
 
Version 1.06+, January 2006.
Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. 
The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.
Perhaps with your help we can be more than a few, and make Mesquite better.

Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.
Mesquite's web site is http://mesquiteproject.org

This source code and its compiled class files are free and modifiable under the terms of 
GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)
*/

package mesquite.correl.Pagel94;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import Jama.Matrix;
import mesquite.categ.lib.CategoricalAdjustable;
import mesquite.categ.lib.CategoricalDistribution;
import mesquite.categ.lib.CategoricalHistory;
import mesquite.categ.lib.CategoricalState;
import mesquite.cont.lib.EigenAnalysis;
import mesquite.correl.lib.*;
import mesquite.lib.*;
import mesquite.lib.characters.AdjustableDistribution;
import mesquite.lib.characters.CLikelihoodCalculator;
import mesquite.lib.characters.CModelEstimator;
import mesquite.lib.characters.CharacterDistribution;
import mesquite.lib.characters.CharacterStates;
import mesquite.lib.characters.ProbabilityModel;
import mesquite.lib.duties.*;
import mesquite.stochchar.lib.MkModel;
import mesquite.stochchar.lib.ProbabilityCategCharModel;
import mesquite.stochchar.lib.TreeDataModelBundle;

public class Pagel94 extends Pagel94Calculator {
    
    private CategoricalDistribution observedStates1;
    private CategoricalDistribution observedStates2;
    MesquiteLong seed;
    long originalSeed=System.currentTimeMillis(); //0L;
    private CategoricalHistory[] evolvingStates;
    
    private ProgressIndicator progress = null;

    private PagelMatrixModel model4;
    private PagelMatrixModel model8;
    
    private int simCount = 0;
    private MesquiteBoolean presentPValue;
    private Logger logger;
    private MesquiteBoolean resimulateConstantCharacters;
    private int numIterations = 10;
    
      
   public boolean startJob(String arguments, Object condition, CommandRecord commandRec, boolean hiredByName) {
	   model4 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL4PARAM);
	   model8 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL8PARAM);
	   seed = new MesquiteLong(originalSeed);
	   // And here
		presentPValue = new MesquiteBoolean(false);
		resimulateConstantCharacters = new MesquiteBoolean(false);  // turned off for now
		addCheckMenuItem(null, "Present P Value", makeCommand("togglePresentPValue", this), presentPValue);
		addMenuItem("Likelihood Iterations (Pagel 94)...", makeCommand("setNumIterations", this));
		addMenuItem("Set Seed (Pagel 94)...", makeCommand("setSeed", this));
	   addMenuItem("Set Simulation Replicates...", makeCommand("setSimCount", this));
		if (!commandRec.scripting()){
			int newNum = MesquiteInteger.queryInteger(containerOfModule(), "Likelihood Iterations (Pagel 94)", "Intensity of likelihood search for 8 parameter model for Pagel 94 (Number of extra iterations):", numIterations, 0, MesquiteInteger.infinite);
			if (MesquiteInteger.isCombinable(newNum))
					numIterations = newNum;
		}
		return true;
   }
   
   public void setLogger(Logger logger){
	   this.logger = logger;
   }
 	/*.................................................................................................................*/
  	 public Snapshot getSnapshot(MesquiteFile file) { 
   	 	Snapshot temp = new Snapshot();
  	  	 temp.addLine("setSeed " + originalSeed); 
	  	 temp.addLine("setSimCount " + getSimCount()); 
 	 	temp.addLine("togglePresentPValue " + presentPValue.toOffOnString());
  	 	temp.addLine("setNumIterations " + numIterations);
 	 	return temp;
  	 }
  public Object doCommand(String commandName, String arguments, CommandRecord commandRec, CommandChecker checker) {
       if (checker.compare(this.getClass(), "Sets the random number seed to that passed", "[long integer seed]", commandName, "setSeed")) {
            long s = MesquiteLong.fromString(parser.getFirstToken(arguments));
            if (!MesquiteLong.isCombinable(s) && !commandRec.scripting()){
                s = MesquiteLong.queryLong(containerOfModule(), "Random number seed", "Enter an integer value for the random number seed for character evolution simulation", originalSeed);
            }
            if (MesquiteLong.isCombinable(s)){
                originalSeed = s;
                seed.setValue(originalSeed);
               if (!commandRec.scripting()) parametersChanged(null, commandRec); //?
            }
            return null;
        }
    	 	else if (checker.compare(this.getClass(), "Sets the number of iterations for likelihood search", "[number]", commandName, "setNumIterations")) {
			MesquiteInteger pos = new MesquiteInteger();
			int newNum= MesquiteInteger.fromFirstToken(arguments, pos);
			if (!MesquiteInteger.isCombinable(newNum))
				newNum = MesquiteInteger.queryInteger(containerOfModule(), "Likelihood iterations", "Intensity of likelihood search for 8 parameter model for Pagel 94 (Number of extra iterations):", numIterations, 0, MesquiteInteger.infinite);
    	 		if (newNum>0  && newNum!=numIterations) {
    	 			numIterations = newNum;
  				if (!commandRec.scripting()){
					parametersChanged(null, commandRec);
				}
    	 		}
    	 	}
  	 	else if (checker.compare(this.getClass(), "Sets the number of simulations to run to estimate p-value of likelihood difference", "[nonnegative integer]", commandName, "setSimCount")){
    	 		int sCount = MesquiteInteger.fromFirstToken(arguments,stringPos);
  			if (!MesquiteInteger.isCombinable(sCount))
    	 			sCount = MesquiteInteger.queryInteger(containerOfModule(),"Number of simulations", "Number of simulations to estimate p-value for Pagel 94 analysis", getSimCount(),0,1000,true);
    	 		if (MesquiteInteger.isCombinable(sCount)){
    	 			setSimCount(sCount);
    	 			if (!commandRec.scripting())
    	 				parametersChanged(null, commandRec);
  	 		}
    	 		else 
    	 			setSimCount(0);
    	 		return null;
    	 		
    	 	}
  	 	else if (checker.compare(this.getClass(), "Sets whether or not P values are to be presented", "[on; off]", commandName, "togglePresentPValue")) {
    	 		presentPValue.toggleValue(parser.getFirstToken(arguments));
    	 		
			if (commandRec != null && !commandRec.scripting()) {
			if (!MesquiteInteger.isCombinable(getSimCount())){
					int sCount = MesquiteInteger.queryInteger(containerOfModule(),"Number of simulations", "Number of simulations to estimate p-value for Pagel 94 analysis", 100,0,10000,true);
		    	 		if (MesquiteInteger.isCombinable(sCount))
		    	 			setSimCount(sCount);
		    	 		else
		    	 			return null;
				}
				parametersChanged(null, commandRec);
			}
    	 	}
//        else if (checker.compare(this.getClass(),"(Currently) saves surface plot of 2 parameters as text file","",commandName,"showSurface")){
//           return null;
//        }
        else
            return super.doCommand(commandName, arguments, commandRec, checker);
       return null;
     }


    public void initialize(Tree tree, CharacterDistribution charStates1, CharacterDistribution charStates2, CommandRecord commandRec) {
        if (!(charStates1 instanceof CategoricalDistribution ||
             charStates2 instanceof CategoricalDistribution)) {
            if (!(charStates1 instanceof CategoricalDistribution))
                Debugg.println("Quitting because the first character is not Categorical");
            else
                Debugg.println("Quitting because the second character is not Categorical");
            iQuit();
        }
        observedStates1 = (CategoricalDistribution)charStates1;
        observedStates2 = (CategoricalDistribution)charStates2;
        if (observedStates1.getMaxState() > 1 ||
            observedStates2.getMaxState() > 1) {
            if (observedStates1.getMaxState() > 1)
                Debugg.println("Quitting because the first character doesn't seem to be binary -- getMaxState() returned " + observedStates1.getMaxState());
            else
                Debugg.println("Quitting because the second character doesn't seem to be binary -- getMaxState() returned " + observedStates2.getMaxState());
            iQuit();
        }
        if (model8 == null){
        		model4 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL4PARAM);
        		model8 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL8PARAM);
        }
    }
    
    public void setSeed(long newSeed){
    		originalSeed = newSeed;
    		seed.setValue(newSeed);
    }
    
    public long getSeed(){
    		return seed.getValue();
    }
    
    public int getSimCount(){
         return simCount;
    }
    
    public void setSimCount(int newCount){
    		simCount = newCount;
    }
    


    public void calculateNumber(Tree tree, CharacterDistribution charStates1, CharacterDistribution charStates2, MesquiteNumber result, MesquiteString resultString, CommandRecord commandRec) {
    		double result4;
    		double result8;
    		double pvalue = MesquiteDouble.unassigned;
    	
        if (!(charStates1 instanceof CategoricalDistribution ||
                charStates2 instanceof CategoricalDistribution)) {
        		result.setValue(MesquiteDouble.unassigned);     
        }
        
        observedStates1 = (CategoricalDistribution)charStates1;
        observedStates2 = (CategoricalDistribution)charStates2;
        
        if (model8 == null) {
    			model4 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL4PARAM);
    			model8 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL8PARAM);
        }
        MesquiteInteger numIt = model8.getExtraSearch();
        numIt.setValue(numIterations);
        logln("Pagel 94 analysis: Calculating likelihood for 4 parameter model");
       
        //model4.showSurface(tree,observedStates1,observedStates2,10);
        //Debugg.println("Estimating model4; start time is " + System.currentTimeMillis()); 
        model4.estimateParameters(tree,observedStates1,observedStates2,commandRec);
        //Debugg.println("Final params are " + DoubleArray.toString(model4.getParams()));
        result4 = model4.evaluate(model4.getParams(),null);

       logln("Pagel 94 analysis: Calculating likelihood for 8 parameter model (" + numIterations + " iterations)");
       //Debugg.println("Model4 -loglikelihood is " + result4.getValue());
        model8.setParametersFromSimplerModel(model4.getParams(),PagelMatrixModel.MODEL4PARAM);
        model8.estimateParameters(tree,observedStates1,observedStates2,commandRec);
        //Debugg.println("Final params are " + DoubleArray.toString(model8.getParams()));
        result8 = model8.evaluate(model8.getParams(),null);

       double score = result4-result8;
       if (logger!= null){ logger.log("\n\nFor four parameter model : \n");
	        logger.log(model4.getParameters());
	        logger.log("\n\nLikelihood is " + result4);
	        logger.log("\n\n\nFor eight parameter model : \n");
	        logger.log(model8.getParameters());
	        logger.log("\n\nLikelihood is " + result8);
	        
	        logger.log("\n\nDifference is " + score);
       }
       
       if (presentPValue.getValue() && simCount>0){
    	   		logln("Pagel 94 analysis: Estimating p value using simulations (Generating " + simCount + " simulated data sets)");
	        // Begin Simulation
	        CategoricalAdjustable[] SimData = new CategoricalAdjustable[2];
	        double independentScore = 0;  // keep compiler happy
	        double dependentScore;
	        evolvingStates = new CategoricalHistory[2];
	        double[][] rootPriors = model4.getRootPriors();
	        PagelMatrixModel savedModel4 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL4PARAM);
	        model4.copyToClone(savedModel4);
	        PagelMatrixModel savedModel8 = new PagelMatrixModel("",CategoricalState.class,PagelMatrixModel.MODEL8PARAM);
	        model8.copyToClone(savedModel8);
	        double diffs[] = new double[simCount];
	        DoubleArray.deassignArray(diffs);
	        boolean hasAborted = false;
	        progress = new ProgressIndicator(getProject(),"Running simulations", "Running", simCount, true);
	        progress.start();
	        for (int simNumber = 0;(simNumber<simCount)&&!hasAborted;simNumber++){
	            progress.setCurrentValue(simNumber+1);  // so user sees 1-based count
	            progress.setText("Running simulation " + (simNumber+1) + " of " + simCount, false);
	            SimData[0] = new CategoricalAdjustable(observedStates1.getTaxa(),observedStates1.getNumNodes());
	            SimData[1] = new CategoricalAdjustable(observedStates2.getTaxa(),observedStates2.getNumNodes());
	            evolvingStates[0] = new CategoricalHistory(tree.getTaxa(), tree.getNumNodeSpaces());
	            evolvingStates[1] = new CategoricalHistory(tree.getTaxa(), tree.getNumNodeSpaces());
	            getSimulatedCharacters(SimData,tree, savedModel4, rootPriors, seed, commandRec);
	            if (!eitherCharacterConstant(tree,SimData[0],SimData[1])) {
	            	   if (progress.isAborted()){
                        hasAborted = true;
	            	   }
	            	   if (!hasAborted){
	            		   model4.estimateParameters(tree,SimData[0],SimData[1],commandRec);
	            		   independentScore = model4.evaluate(model4.getParams(),null);
	            	   }
	            	   if (progress.isAborted()){
	            		   hasAborted = true;
	            	   }
	            	   if (!hasAborted){
	            		   model8.setParametersFromSimplerModel(model4.getParams(),PagelMatrixModel.MODEL4PARAM);
	            		   //Debugg.println("; Analyzing 8p model");
	            		   model8.estimateParameters(tree,SimData[0],SimData[1],commandRec);
	            		   dependentScore= model8.evaluate(model8.getParams(),null);
	            		   //Debugg.println("Scores (indep,dep): " + independentScore + ", " + dependentScore);           	 
	            		   diffs[simNumber] = independentScore-dependentScore;
	            	   }
	            	   if (progress.isAborted()){
	            		   hasAborted = true;
	            	   }
	            }
	            else if(resimulateConstantCharacters.getValue()){
	                while(eitherCharacterConstant(tree,SimData[0],SimData[1])){
	                	   Debugg.println("Resimulating Characters");
	                    SimData[0] = new CategoricalAdjustable(observedStates1.getTaxa(),observedStates1.getNumNodes());
	                    SimData[1] = new CategoricalAdjustable(observedStates2.getTaxa(),observedStates2.getNumNodes());
	                    evolvingStates[0] = new CategoricalHistory(tree.getTaxa(), tree.getNumNodeSpaces());
	                    evolvingStates[1] = new CategoricalHistory(tree.getTaxa(), tree.getNumNodeSpaces());
	                    getSimulatedCharacters(SimData,tree, savedModel4, rootPriors, seed, commandRec);
	                }
	            	   if (progress.isAborted()){
	            		   hasAborted = true;
	            	   }
	            	   if (!hasAborted){
	            		   model4.estimateParameters(tree,SimData[0],SimData[1],commandRec);
	            		   independentScore = model4.evaluate(model4.getParams(),null);
	            	   }
	            	   if (progress.isAborted()){
	            		   hasAborted = true;
	            	   }
	            	   if (!hasAborted){
	            		   model8.setParametersFromSimplerModel(model4.getParams(),PagelMatrixModel.MODEL4PARAM);
	            		   //Debugg.println("; Analyzing 8p model");
	            		   model8.estimateParameters(tree,SimData[0],SimData[1],commandRec);
	            		   dependentScore= model8.evaluate(model8.getParams(),null);
	            		   Debugg.println("Scores (indep,dep): " + independentScore + ", " + dependentScore);           	 
	            		   diffs[simNumber] = independentScore-dependentScore;
	            	   }
	            	   if (progress.isAborted()){
	            		   hasAborted = true;
	            	   }
	            }
	            else diffs[simNumber] = 0;   //if character is constant the 4-parameter and 8-parameter models ought to be =
	         }
	         progress.goAway();
	         DoubleArray.sort(diffs);
	         int position;
	         for (position = 0;position<simCount;position++)
	             if (score<diffs[position])
	            	 	break;
	         if (hasAborted){
	        	 	int completedDiffs = countAssigned(diffs);
	        	 	if (completedDiffs>0){
	        	 		pvalue = 1-(1.0*position)/(1.0*completedDiffs);
	        	 		logger.log("\nP-values from " + completedDiffs +"simulations is " + pvalue + "\n");
	        	 	}
	        	 	else{
	        	 		pvalue = MesquiteDouble.unassigned;
	        	 		logger.log("\nNo simulations completed");
	        	 	}
	         }
	         else {
	        	 	pvalue = 1-(1.0*position)/(1.0*simCount);
	        	 	logger.log ("\nP-value from simulations is " + pvalue + "\n");
	         }
  	         if (result != null)
	     	    result.setValue(pvalue);
	         if (result.isUnassigned())
	            resultString.setValue("No pvalue was calculated, see MesquiteLog for details");
	        else
	            resultString.setValue("p-value = " + result.getDoubleValue());
     }
       else {
	         if (result != null)
	     	    result.setValue(score);
	         if (result.isUnassigned())
	            resultString.setValue("No likelihood was calculated, see MesquiteLog for details");
	        else
	            resultString.setValue("Difference in log likelihoods = " + result.getDoubleValue());
       }
    }
    
    private boolean eitherCharacterConstant(Tree tree,CategoricalAdjustable char1, CategoricalAdjustable char2) {
        return (char1.isConstant(tree,tree.getRoot()) || char2.isConstant(tree,tree.getRoot()));
    }
    

    
    // Wayne, this might be a useful addition to DoubleArray??
    private int countAssigned(double[] data){
    		int count = 0;
    		for(int i=0;i<data.length;i++)
    			if (!MesquiteDouble.isUnassigned(data[i]))
    				count++;
    		return count;
    }
    
   	private CharacterDistribution[] getSimulatedCharacters(CategoricalAdjustable[] statesAtTips, Tree tree, PagelMatrixModel model, double[][] rootPriors, MesquiteLong seed, CommandRecord commandRec){
   		if (tree == null) {
   			MesquiteMessage.warnProgrammer("tree null in Pagel94 getSimulatedCharacters ");
   			return null;
   		}
   		if (statesAtTips == null){
   			MesquiteMessage.warnProgrammer("statesAtTips is null in Pagel94 getSimulatedCharacters");
   			return null;
   		}
   		for (int i=0;i<statesAtTips.length;i++) {
   			if (!(statesAtTips[i] instanceof CategoricalAdjustable))
   				statesAtTips[i] = new CategoricalAdjustable(tree.getTaxa(), tree.getTaxa().getNumTaxa());
   			else
   				statesAtTips[i] = (CategoricalAdjustable)((AdjustableDistribution)statesAtTips[i]).adjustSize(tree.getTaxa());
   		}
   		for (int i=0;i<statesAtTips.length;i++) {
   	   		if (statesAtTips[i] instanceof CategoricalAdjustable)
   	   			((CategoricalAdjustable)statesAtTips[i]).deassignStates();
   		}
		//probabilityModel.setMaxStateSimulatable(probabilityModel.getMaxStateDefined()); //should allow user to set # states
   		if (model == null) {
   			MesquiteMessage.warnProgrammer("Model is null in Pagel94 getSimulatedCharacters");
   			return null;
   		}
   		if (!model.isFullySpecified()) {
   			MesquiteMessage.warnProgrammer("Model was not fully specified during estimate or wrong model used ");
   			return null;
   		}	
   		//failed = false;
   		for (int i=0;i<statesAtTips.length;i++){
   			evolvingStates[i] =(CategoricalHistory) statesAtTips[i].adjustHistorySize(tree, evolvingStates[i]);
   		}
   		if (seed!=null)
   			model.setSeed(seed.getValue());
   		long[] rootstate = model.getRootStates(tree,rootPriors);
   		for (int c=0;c<rootstate.length;c++)
   			evolvingStates[c].setState(tree.getRoot(), rootstate[c]);  //starting rootCategoricalState.makeSet(0)); //
   		//Debugg.println("States at root[0] are " + evolvingStates[0].states[tree.getRoot()]);
   		//Debugg.println("States at root[1] are " + evolvingStates[1].states[tree.getRoot()]);
   		evolve(tree, (CategoricalAdjustable[])statesAtTips, model, tree.getRoot());
   		//Debugg.println("Final States are " + LongArray.toString(evolvingStates[0].states));
   		//Debugg.println("Final States are " + LongArray.toString(evolvingStates[1].states));
  		if (seed!=null)
   			seed.setValue(model.getSeed());
  		return statesAtTips;
   	}
 	/*.................................................................................................................*/

	private void evolve(Tree tree, CategoricalAdjustable[] statesAtTips, PagelMatrixModel model, int node) {
   		if (node!=tree.getRoot()) {
   			int size = statesAtTips.length;
   			int[] stateAtAncestor = new int[size];
   			int[] stateAtNode;
    			for (int c=0;c<size;c++){
    				long states = evolvingStates[c].getState(tree.motherOfNode(node));
  				stateAtAncestor[c] = CategoricalState.minimum(states);
   			}
   			stateAtNode = model.evolveState(stateAtAncestor,tree,node);
   			for (int c=0;c<size;c++){
   				long states = CategoricalState.makeSet(stateAtNode[c]);
   				evolvingStates[c].setState(node,states);
   				if (tree.nodeIsTerminal(node)) {   				
   					statesAtTips[c].setState(tree.taxonNumberOfNode(node), states);
   				}
   			}
   		}
		for (int daughter = tree.firstDaughterOfNode(node); tree.nodeExists(daughter); daughter = tree.nextSisterOfNode(daughter))
				evolve(tree, statesAtTips, model, daughter);
	}
 	/*.................................................................................................................*/

    
    public String getVeryShortName() {
        return "Test from Pagel1994";
    }
    
    public String getAuthors() {
    		return "Peter E. Midford & Wayne P. Maddison";
    }
    
    public String getVersion() {
    		return "0.1";
    }

    public String getName() {
        return "Pagel's 1994 test of correlated (discrete) character evolution";
    }

    public String getExplanation(){
    		return "A statistical test, described in Pagel(1994), for nonindependent evolution of two discrete, binary characters";
    }
    
    // This isPrerelease also proxies for PagelMatrixModel in mesquite.correl.lib  
    public boolean isPrerelease(){
    		return true;
    }

}

    