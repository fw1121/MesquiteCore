/* Mesquite source code.  Copyright 1997-2002 W. Maddison & D. Maddison. Version 0.992.  September 2002.Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)*/package mesquite.diverse.BiasByParamAsymSpeciation;import mesquite.diverse.lib.*;import mesquite.lib.*;import mesquite.lib.duties.*;import mesquite.lib.characters.*;import mesquite.categ.lib.*;import mesquite.diverse.ValueField.*;/** ======================================================================== */public class BiasByParamAsymSpeciation extends FileAssistantT {	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed		EmployeeNeed e = registerEmployeeNeed(mesquite.diverse.CategCharSpeciation.CategCharSpeciation.class, getName() + "  needs a simulator.",		"The simulator is arranged automatically");				EmployeeNeed e2 = registerEmployeeNeed(mesquite.stochchar.GainLossRates.GainLossRates.class, getName() + "  needs a method to calculate bias.",		"The calculator is arranged automatically");		EmployeeNeed e3 = registerEmployeeNeed(mesquite.diverse.ValueField.ValueField.class, getName() + "  needs a display module.",		"The display module is arranged automatically");	}	/*.................................................................................................................*/	TreeCharSimulate simulator;	//hire num fo rchar & tree forward/backward gain //feed trees 100 per cell	NumberForCharAndTree biasTask;	ValueField valueField;	/*.................................................................................................................*/	public boolean startJob(String arguments, Object condition, CommandRecord commandRec, boolean hiredByName) { 		simulator = (TreeCharSimulate)hireNamedEmployee(CommandRecord.scriptingRecord, TreeCharSimulate.class, "#mesquite.diverse.CategCharSpeciation.CategCharSpeciation");		if (simulator == null)			return sorry(commandRec, getName() + " couldn't start because no simulating module was obtained."); 		biasTask = (NumberForCharAndTree)hireNamedEmployee(commandRec, NumberForCharAndTree.class, "#mesquite.stochchar.GainLossRates.GainLossRates");		if (biasTask == null)			return sorry(commandRec, getName() + " couldn't start because no bias calculating module was obtained."); 		valueField = (mesquite.diverse.ValueField.ValueField)hireNamedEmployee(commandRec, mesquite.diverse.ValueField.ValueField.class, "mesquite.diverse.ValueField.ValueField");		if (valueField == null)			return sorry(commandRec, getName() + " couldn't start because no display module was obtained.");				incrementMenuResetSuppression();		Taxa taxa = getProject().chooseTaxa(containerOfModule(), "For which block of taxa do you want to evolve trees and characters?",commandRec);		TreeVector trees = new TreeVector(taxa);		boolean saveTrees = false;				int numTaxa = taxa.getNumTaxa();		CategoricalAdjustable character= new CategoricalAdjustable(taxa, taxa.getNumTaxa());		Tree tree = null;		MesquiteLong seed = new MesquiteLong(System.currentTimeMillis());				//====================		int numTrees = 1001;		double initialRate0 = 0.1;		double initialRate1 = 0.1;		double rateIncrement = 0.1;		int numDivisions = 5;		double prior1AtRoot = 0.5;		/*		int numTrees = 21;		double initialRate = 0.1;		double rateIncrement = 0.005;		int numDivisions = 100;	*/		//====================				MesquiteInteger buttonPressed = new MesquiteInteger(1);		ExtensibleDialog queryDialog = new ExtensibleDialog(containerOfModule(), "Bias simulations",  buttonPressed);		SingleLineTextField seedField = queryDialog.addTextField("Seed:", seed.toString(), 20);		SingleLineTextField priorField = queryDialog.addTextField("Prob 1 at root:", "" + prior1AtRoot, 10);		SingleLineTextField numTreesField = queryDialog.addTextField("Num Trees per cell:", "" + numTrees, 10);		SingleLineTextField initRate0Field = queryDialog.addTextField("Init rate 0:", "" + initialRate0, 10);		SingleLineTextField initRate1Field = queryDialog.addTextField("Init rate 1:", "" + initialRate1, 10);		SingleLineTextField rateIncrementField = queryDialog.addTextField("Rate increment:", "" + rateIncrement, 10);		SingleLineTextField numDivisionsField = queryDialog.addTextField("Num Divisions:", "" + numDivisions, 10);				queryDialog.completeAndShowDialog(true);					boolean ok = (queryDialog.query()==0);				if (ok) {			String s = seedField.getText();			long sL = MesquiteLong.fromString(s);			if (MesquiteLong.isCombinable(sL))				seed.setValue(sL);							s = priorField.getText();			double sd = MesquiteDouble.fromString(s);			if (MesquiteDouble.isCombinable(sd))				prior1AtRoot = (sd);							s = numTreesField.getText();			int sI = MesquiteInteger.fromString(s);			if (MesquiteInteger.isCombinable(sI))				numTrees = sI;							s = initRate0Field.getText();			sd = MesquiteDouble.fromString(s);			if (MesquiteDouble.isCombinable(sd))				initialRate0 = (sd);							s = initRate1Field.getText();			sd = MesquiteDouble.fromString(s);			if (MesquiteDouble.isCombinable(sd))				initialRate1 = (sd);							s = rateIncrementField.getText();			sd = MesquiteDouble.fromString(s);			if (MesquiteDouble.isCombinable(sd))				rateIncrement = (sd);							s = numDivisionsField.getText();			sI = MesquiteInteger.fromString(s);			if (MesquiteInteger.isCombinable(sI))				numDivisions = sI;			queryDialog.dispose();		}		else {			queryDialog.dispose();			return false;		}		logln("Seed: " + seed);		ObjectContainer charHistoryContainer = new ObjectContainer();		ObjectContainer treeContainer = new ObjectContainer();		MesquiteNumber result = new MesquiteNumber();		//====================		double[][] values = new double[numDivisions][numDivisions];		for (int rate0 = 0; rate0 < numDivisions; rate0++)			for (int rate1 = 0; rate1<numDivisions; rate1++)				values[rate0][rate1] = MesquiteDouble.unassigned;		double r1 = initialRate1;		double r0 = initialRate0;		simulator.doCommand("setPrior", Double.toString(prior1AtRoot), CommandRecord.scriptingRecord, CommandChecker.defaultChecker);		MesquiteTree clone = null;		CategoricalData data= null;		if (saveTrees){			CharactersManager charManager = (CharactersManager)getFileCoordinator().findImmediateEmployeeWithDuty(CharactersManager.class);			data = (CategoricalData)charManager.newCharacterData(taxa, numTrees, "Standard Categorical Data");		}		for (int rate0 = 0; rate0 < numDivisions; rate0++){						simulator.doCommand("setRate0", Double.toString(r0), CommandRecord.scriptingRecord, CommandChecker.defaultChecker);			for (int rate1 = 0; rate1<numDivisions; rate1++){				simulator.doCommand("setRate1", Double.toString(r1), CommandRecord.scriptingRecord, CommandChecker.defaultChecker);				logln(">-Current seed: " + seed);				double[] results = new double[numTrees];		   		for (int iTree=0; iTree<numTrees; iTree++){		   			commandRec.tick("Simulating tree and character " + iTree);		   			charHistoryContainer.setObject(null);		   			treeContainer.setObject(null);		   			simulator.doSimulation(taxa, iTree, treeContainer, charHistoryContainer, seed, CommandRecord.scriptingRecord);		   			if (saveTrees){		   				clone = tree.cloneTree();		   				clone.setName("tree " + (iTree +1));		   				trees.addElement(clone, false);		   				character= new CategoricalAdjustable(taxa, taxa.getNumTaxa());		   			}		   			Object ch = charHistoryContainer.getObject();		   			if (ch !=null && ch instanceof CategoricalHistory){		   				CategoricalHistory characterStates = (CategoricalHistory)ch;		   				harvestStates(tree, tree.getRoot(), iTree, characterStates, character);		   				if (hasZeroOrNegLengthBranches(tree, tree.getRoot(), false))		   					logln("BAD TREE@@@@@@@@@@@@@*************************" + tree.writeTree());		   				biasTask.calculateNumber(tree, character, result, null, commandRec);		   				if (saveTrees){		   					clone.attachToSensitives(character); 		   					harvestStates(tree, tree.getRoot(), iTree, characterStates, data);		   				}		   				logln("Tree " + iTree + " result " + result);		   				results[iTree] = result.getDoubleValue();		   			}		   		}		   		DoubleArray.sort(results);		   		double sum = 0;		   		for (int i = 0; i<results.length; i++)		   			sum += results[i];		   		int med = numTrees/2;		   		if (med*2 == numTrees) // if numTrees is 4, this yields 1, i.e. second; if numTrees is 3, this yields 1		   			med--;		   		values[rate1][rate0] = results[med];		   		valueField.showField(values, "Just calculated: rate0 " + r0 + " rate1 " + r1);	   			r1 += rateIncrement;	   		}	   		r1 = initialRate1;	   		r0 += rateIncrement;		}		if (saveTrees){					trees.setName("Evolved with Categorical");					trees.addToFile(getProject().getHomeFile(), getProject(), findElementManager(TreeVector.class));  				data.addToFile(getProject().getHomeFile(), getProject(), findElementManager(CharacterData.class));  				data.setName("Evolved on Simulated Trees");		}		decrementMenuResetSuppression(); 		return true;  	 }	/*.................................................................................................................*/  	 private void harvestStates(Tree tree, int node, int i, CategoricalHistory states, CategoricalAdjustable character){  	 	if (tree.nodeIsTerminal(node)) {  	 		character.setState(tree.taxonNumberOfNode(node), states.getState(node));		}		for (int d = tree.firstDaughterOfNode(node); tree.nodeExists(d); d = tree.nextSisterOfNode(d)) {			harvestStates(tree, d, i, states, character);		}  	 }	/*.................................................................................................................*/  	 private void harvestStates(Tree tree, int node, int i, CategoricalHistory states, CategoricalData data){  	 	if (tree.nodeIsTerminal(node)) {  	 		data.setState(i, tree.taxonNumberOfNode(node), states.getState(node));		}		for (int d = tree.firstDaughterOfNode(node); tree.nodeExists(d); d = tree.nextSisterOfNode(d)) {			harvestStates(tree, d, i, states, data);		}  	 }	 	private boolean hasZeroOrNegLengthBranches(Tree tree, int N, boolean countRoot) {		if (tree.getBranchLength(N) <= 0.0 && (countRoot || tree.getRoot() != N))			return true;		if (tree.nodeIsInternal(N)){			for (int d = tree.firstDaughterOfNode(N); tree.nodeExists(d); d = tree.nextSisterOfNode(d))				if (hasZeroOrNegLengthBranches(tree, d, countRoot))					return true;		}		return false;	}	/*.................................................................................................................*/    	 public String getName() {		return "Bias by Asym Speciation";   	 }	/*.................................................................................................................*/  	 public String getVersion() {		return null;   	 }   	 	/*.................................................................................................................*/ 	/** returns an explanation of what the module does.*/ 	public String getExplanation() { 		return "calculates bias inferred in character change from simulations with biased speciation." ;   	 }}	