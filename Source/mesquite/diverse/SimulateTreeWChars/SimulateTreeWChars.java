/* Mesquite module ~~ Copyright 1997-2001 W. & D. Maddison*/package mesquite.diverse.SimulateTreeWChars;/*~~  */import java.applet.*;import java.util.*;import java.awt.*;import mesquite.lib.*;import mesquite.lib.characters.*;import mesquite.lib.duties.*;/** ======================================================================== */public class SimulateTreeWChars extends TreeSource {	public void getEmployeeNeeds(){  //This gets called on startup to harvest information; override this and inside, call registerEmployeeNeed		EmployeeNeed e = registerEmployeeNeed(TreeSimulate.class, getName() + "  needs a method to simulate trees.",		"The method to simulate trees can be selected initially or in the Tree Simulator submenu");		EmployeeNeed e2 = registerEmployeeNeed(CharacterSimulator.class, getName() + "  needs a method to simulate characters.",		"The method to simulate characters can be selected initially or in the Character Simulator submenu");	}	/*.................................................................................................................*/	int currentTree=0;	long originalSeed=System.currentTimeMillis(); //0L;	Tree tree;	TreeSimulate treeSimulatorTask;	MesquiteLong seed;	static int numTrees = MesquiteInteger.infinite;	Random rng;	MesquiteString treeSimulatorName;	int notifyEvery = 0;	long count = 0;	CharacterSimulator simulatorTask;	int numChars = 100;	MesquiteString simulatorName;	Object dataCondition;	MesquiteCommand tstC, stC;  	 	public boolean startJob(String arguments, Object condition, CommandRecord commandRec, boolean hiredByName) {		dataCondition = condition;	 	treeSimulatorTask= (TreeSimulate)hireEmployee(commandRec, TreeSimulate.class, "Tree simulator");    	 	if (treeSimulatorTask == null) {    	 		return sorry(commandRec, "Simulated Tree With Characters could not start because no appropriate tree simulator module was obtained");    	 	}    	 	if (condition!=null)    	 		simulatorTask= (CharacterSimulator)hireCompatibleEmployee(commandRec, CharacterSimulator.class, dataCondition, "Character simulator");    	 	else    	 		simulatorTask= (CharacterSimulator)hireEmployee(commandRec, CharacterSimulator.class, "Character simulator");    	 	    	 	if (simulatorTask == null) {    	 		return sorry(commandRec, "Simulated Tree With Characters could not start because no appropriate character simulator module was obtained");    	 	}    	 	stC = makeCommand("setCharacterSimulator",  (Commandable)this);    	 	tstC = makeCommand("setTreeSimulator",  (Commandable)this);    	 	simulatorTask.setHiringCommand(stC);    	 	treeSimulatorTask.setHiringCommand(tstC); 		treeSimulatorName = new MesquiteString();	    	treeSimulatorName.setValue(treeSimulatorTask.getName());    	 	seed = new MesquiteLong(1);    	 	seed.setValue(originalSeed);    	 	rng = new Random(originalSeed);		if (numModulesAvailable(TreeSimulate.class)>1){			MesquiteSubmenuSpec mss = addSubmenu(null, "Tree Simulator",tstC, TreeSimulate.class); 			mss.setSelected(treeSimulatorName);  		}  		addMenuItem("Reset Seed", makeCommand("resetSeed",  (Commandable)this));  		checkSimulator(commandRec); 		simulatorName = new MesquiteString(simulatorTask.getName());		if (numCompatibleModulesAvailable(CharacterSimulator.class, dataCondition, this)>1){			MesquiteSubmenuSpec mss = addSubmenu(null, "Character Simulator", stC, CharacterSimulator.class); 			mss.setSelected(simulatorName); 			mss.setCompatibilityCheck(dataCondition); 		} 		if (!commandRec.scripting()){			numChars = MesquiteInteger.queryInteger(containerOfModule(), "Number of characters in matrix", "Number of characters:", 100, 1, 10000);	 		if (MesquiteInteger.isUnassigned(numChars))	 			return false;	 		if (!MesquiteInteger.isCombinable(numChars))	 			numChars = 100;  		} 		addMenuItem( "Number of characters...",makeCommand("setNumChars",  (Commandable)this));  		return true;  	 }  	public boolean isPrerelease(){   		return true;   	}	/*.................................................................................................................*/  	 public Snapshot getSnapshot(MesquiteFile file) {   	 	Snapshot temp = new Snapshot();  	 	temp.addLine("setTreeSimulator ", treeSimulatorTask);   	 	temp.addLine("setNumChars " + numChars); 	 	temp.addLine("setCharacterSimulator ", simulatorTask);  	 	return temp;  	 }  	 MesquiteInteger pos = new MesquiteInteger(0);	/*.................................................................................................................*/    	 public Object doCommand(String commandName, String arguments, CommandRecord commandRec, CommandChecker checker) {    	 	if (checker.compare(this.getClass(), "Sets the module simulating trees", "[name of module]", commandName, "setTreeSimulator")) {    	 		TreeSimulate temp=  (TreeSimulate)replaceEmployee(commandRec, TreeSimulate.class, arguments, "Tree simulator", treeSimulatorTask);	    	 	if (temp!=null) {	    	 		treeSimulatorTask = temp;		    	 	treeSimulatorTask.setHiringCommand(tstC);	    	 		treeSimulatorName.setValue(treeSimulatorTask.getName());	    	 		seed.setValue(originalSeed); 				parametersChanged(null, commandRec); //? 			} 			return temp;    	 	}     	 	else if (checker.compare(this.getClass(), "Resets the random number seed to the current system time", null, commandName, "resetSeed")) {    	 		originalSeed = System.currentTimeMillis(); 			parametersChanged(null, commandRec); //?    	 	}     	 	else if (checker.compare(this.getClass(), "Notifies the user periodically how many trees have been simulated", "[how many trees between notifications]", commandName, "notifyEvery")) {    	 		int notify = MesquiteInteger.fromFirstToken(arguments, pos);   	 		if (MesquiteInteger.isCombinable(notify) && notify>0)    	 			notifyEvery = notify;    	 		else    	 			notifyEvery = -1;    	 	}    	 	else if (checker.compare(this.getClass(), "Sets the module used to simulate character evolution", "[name of module]", commandName, "setCharacterSimulator")) {    	 		CharacterSimulator temp;    	 		if (dataCondition==null)    	 			temp =  (CharacterSimulator)replaceEmployee(commandRec, CharacterSimulator.class, arguments, "Character simulator", simulatorTask);    	 		else    	 			temp =  (CharacterSimulator)replaceCompatibleEmployee(commandRec, CharacterSimulator.class, arguments, simulatorTask, dataCondition);	    	 	if (temp!=null) {	    	 		simulatorTask=  temp;		    	 	simulatorTask.setHiringCommand(stC);		    	 	treeSimulatorTask.setHiringCommand(tstC);	    	 		simulatorName.setValue(simulatorTask.getName());	    	 		seed.setValue(originalSeed); 				parametersChanged(null, commandRec); //? 				return simulatorTask; 			}    	 	}    	 	else if (checker.compare(this.getClass(), "Sets the number of characters simulated", "[number of characters]", commandName, "setNumChars")) {			int newNum= MesquiteInteger.fromFirstToken(arguments, pos);			if (!MesquiteInteger.isCombinable(newNum) )				newNum = MesquiteInteger.queryInteger(containerOfModule(), "Set number of characters", "Number of characters to simulate:", numChars);    	 		if (MesquiteInteger.isCombinable(newNum) && newNum>0 && newNum<1000000 && newNum!=numChars) {    	 			numChars=newNum;    	 			parametersChanged(null, commandRec);    	 		}    	 	}   	 	else {    	 		try {return super.doCommand(commandName, arguments, commandRec, checker);}    	 		catch (NoSuchMethodError e){}    	 	}		return null;   	 }   	 	/*.................................................................................................................*/  	public void setPreferredTaxa(Taxa taxa){  	}	/*.................................................................................................................*/  	private void checkSimulator(CommandRecord commandRec){  		if (treeSimulatorTask==null) {			treeSimulatorTask = (TreeSimulate)hireEmployee(commandRec, TreeSimulate.class, "Tree simulator");	    	 	if (treeSimulatorTask == null)	    	 		MesquiteMessage.warnUser("Unable to hire tree simulator");	    	 	if (treeSimulatorTask!=null)	    	 		treeSimulatorName.setValue(treeSimulatorTask.getName());		}  	}   	/** Called to provoke any necessary initialization.  This helps prevent the module's intialization queries to the user from   	happening at inopportune times (e.g., while a long chart calculation is in mid-progress)*/   	public void initialize(Taxa taxa, CommandRecord commandRec){   	}	/*.................................................................................................................*/   	public Tree getFirstTree(Taxa taxa, CommandRecord commandRec) {   		currentTree=0;   		return getTree(taxa, currentTree, commandRec);   	}	/*.................................................................................................................*/   	public Tree getTree(Taxa taxa, int itree, CommandRecord commandRec) {  //TODO: should this reuse the same Tree object? forces user to use immediately or clone for storages   		currentTree=itree;   		if (taxa==null) {   			MesquiteMessage.warnProgrammer("taxa null in getTree of SimulateTreeWChars");   			return null;   		}   		if (notifyEvery>0 && count++ % notifyEvery==0)   			System.out.println("   tree " + currentTree);		 tree = new MesquiteTree(taxa);		rng.setSeed(originalSeed);		long rnd = 0;		for (int it = 0; it<=currentTree; it++)			rnd =  rng.nextInt();		seed.setValue(rnd);		tree =  treeSimulatorTask.getSimulatedTree(taxa, tree, currentTree, null, seed, commandRec); Debugg.println("getTree to " + tree.getID()); 		if (tree instanceof MesquiteTree) {			((MesquiteTree)tree).setName(getTreeNameString(taxa, currentTree, commandRec));			attachCharacters(((MesquiteTree)tree), commandRec);   		}   		else   			MesquiteMessage.warnProgrammer("Error: tree in SimulateTreeWChars is not a MesquiteTree");/*   Debugg.println("sensitives  \n" + ((MesquiteTree)tree).getListAttachedSensitives());   MCharactersStates matrix = (MCharactersStates)((MesquiteTree)tree).getAttachedSensitive(MCharactersStates.class, 0);   Debugg.println("matrix \n" + matrix.matrixToString());*/   		return tree;   	}   	private void attachCharacters(MesquiteTree tree, CommandRecord commandRec){		MAdjustableDistribution matrix = null;		Class c = simulatorTask.getStateClass();//getDataClass from simulator and get it to make matrix		if (c==null)			return;		try {			CharacterState s = (CharacterState)c.newInstance();			if (s!=null) {				matrix = s.makeMCharactersDistribution(tree.getTaxa(), numChars, tree.getTaxa().getNumTaxa());				if (matrix == null)					return;			}		}		catch (IllegalAccessException e){alert("iae getM"); return; }		catch (InstantiationException e){alert("ie getM");  return;}				CharacterDistribution states = null;				for (int ic = 0; ic<numChars; ic++) {			states = simulatorTask.getSimulatedCharacter(states, tree, seed, commandRec);  	 		matrix.transferFrom(ic, states); 	 	}  		tree.attachToSensitives(matrix);  Debugg.println("attached to " + tree.getID() + " matrix  " + matrix);   	}	/*.................................................................................................................*/   	public Tree getNextTree(Taxa taxa, CommandRecord commandRec) {   		currentTree++;   		return getTree(taxa, currentTree, commandRec);   	}	/*.................................................................................................................*/   	public int getNumberOfTrees(Taxa taxa, CommandRecord commandRec) {   		return numTrees;   	}   	/*.................................................................................................................*/   	public String getTreeNameString(Taxa taxa, int itree, CommandRecord commandRec) {   		if (treeSimulatorTask==null) return "";		return "Tree # " + MesquiteTree.toExternal(itree)  + " simulated by " + treeSimulatorTask.getName();   	}	/*.................................................................................................................*/   	public String getCurrentTreeNameString() {   		if (treeSimulatorTask==null) return "";		return "Tree # " + MesquiteTree.toExternal(currentTree)  + " simulated by " + treeSimulatorTask.getName();   	}	/*.................................................................................................................*/   	public String getParameters() {   		if (treeSimulatorTask==null) return "";		return "Tree simulator: " + treeSimulatorTask.getName() + "; Character simulator: " + simulatorTask.getName();   	}	/*.................................................................................................................*/    	 public String getName() {		return "Simulate Tree with Characters";   	 }	/*.................................................................................................................*/  	 public String getVersion() {		return null;   	 }   	 	/*.................................................................................................................*/  	 public String getExplanation() {		return "Supplies trees from simulations, but at the same time evolves characters and attaches them to the trees.";   	 }}