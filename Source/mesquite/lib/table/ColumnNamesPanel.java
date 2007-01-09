/* Mesquite source code.  Copyright 1997-2006 W. Maddison and D. Maddison. Version 1.11, June 2006.Disclaimer:  The Mesquite source code is lengthy and we are few.  There are no doubt inefficiencies and goofs in this code. The commenting leaves much to be desired. Please approach this source code with the spirit of helping out.Perhaps with your help we can be more than a few, and make Mesquite better.Mesquite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY.Mesquite's web site is http://mesquiteproject.orgThis source code and its compiled class files are free and modifiable under the terms of GNU Lesser General Public License.  (http://www.gnu.org/copyleft/lesser.html)*/package mesquite.lib.table;import java.awt.*;import java.awt.event.*;import mesquite.lib.*;import java.io.*;/* ======================================================================== *//** A panel for column headings for use in MesquiteTable*/public class ColumnNamesPanel extends EditorPanel  {	MesquiteTable table;	public int width,  height;	int touchX = -1;	int touchColumn = -1;	int lastX=-1;	int numRows = 1;	int numInfoStrips = 0;	int rowH=20;//	int nameRowAdjust = -5;		public ColumnNamesPanel (MesquiteTable table , int w, int h) {		super(table);		this.table=table;		this.width=w;		this.height=h;		//setBackground(ColorDistribution.medium[table.colorScheme]);			setBackground(Color.white);		setCursor(table.getHandCursor());		setSize(w, h);	}	public void setTableUnitSize (int w, int h) {		this.width=w;		this.height=h;		setSize(width, h);	}	/*...............................................................................................................*/	/** Gets the height of the columnNames Panel.*/	public int calcColumnNamesHeight() {		return rowHeight(-1)*getNumRows() + table.getColumnGrabberWidth();	}	public void setHeight () {		this.height=rowHeight(-1)*numRows + table.getColumnGrabberWidth();		setSize(width, height);	}	public int getNumRows () {		return numRows;	}	public int getNumInfoStrips () {		return numInfoStrips;	}	public void setNumInfoStrips (int num) {		numInfoStrips = num;		numRows = numInfoStrips+1;	}	public void appendInfoStrip () {		numInfoStrips++;		numRows++;	}	public void decrementInfoStrips () {		numInfoStrips--;		numRows--;	}	/*@@@...............................................................................................................*/	/** returns in which column x lies, -1 if to left, -2 if to right.*/	public int findRegionInCellH(int x) {		if (x<=0)			return 50;		int cx = 0;		for (int column=table.firstColumnVisible; (column<table.numColumnsTotal) && (cx<x); column++) {			cx += table.columnWidths[column];			if (column>= table.numColumnsTotal)				return 50;			else if (cx>=x) {				int dXR = cx-x; //distance from right edge to 				int dXL = x - (cx-table.columnWidths[column]); //distance from left edge to 				return dXL*100/(dXR+dXL);			}		}		return 50;			}	/*@@@...............................................................................................................*/	/** returns in which column x lies, -1 if to left, -2 if to right.*/	public int findRegionInCellV(int y) {		if (y<=0)			return 50;		return (y-startOfRow(-1))*100/(rowHeight(-1));	}	public int startOfRow(int row){		if (table.showColumnGrabbers)			return (table.getColumnGrabberWidth());		else			return 0;	}	public int firstRowVisible(){		return -1;	}	public int numRowsVisible(){		return 1;	}	public int calcRowHeight(int row) {		if (table.showColumnGrabbers)			return (height -table.getColumnGrabberWidth())/getNumRows();		else			return (height)/getNumRows();	}	public int rowHeight(int num) {		return rowH;	}	public void setRowHeight(int h) {		rowH = h;	}	public int nameRowTop() {		return (startOfRow(-1));	}	public int nameRowBottom() {		return (startOfRow(-1) + nameRowHeight());	}	public int extraRowTop(int extraRow) {		return (nameRowBottom() + rowHeight(-1)*(extraRow));	}	public int nameRowHeight() {		return (rowHeight(-1));	}	public int lastRowBottom() {		return (startOfRow(-1) + rowHeight(-1)*getNumRows());	}	public void textReturned(int column, int row, String text, CommandRecord commandRec){		table.returnedColumnNameText(column, text, commandRec);	}	public String getText(int column, int row){		return table.getColumnNameText(column);	}	public void deselectCell(int column,int row){		table.deselectColumnName(column);	}	public void redrawCell(int column, int row){		Graphics g = getGraphics();		if (g!=null) {			redrawName(g, column);			g.dispose();		}	}	/*...............................................................................................................*/	public void redrawName(Graphics g, int column) {		int left = table.getFirstColumnVisible();				if (column<left) //TODO: should also fail to draw if to big			return;  		if (column == returningColumn){			return; //don't draw if text about to be returned to cell, and will soon be redrawn anyway		}		int leftSide = startOfColumn(column);		if (leftSide>getBounds().width || leftSide+columnWidth(column)<0)			return;		int topSide = nameRowTop();		int botSide = nameRowBottom();		Shape clip = g.getClip();		g.setClip(leftSide,0,columnWidth(column), botSide);				prepareCell(g, leftSide+1, 1,columnWidth(column), botSide, table.focusColumn == column, table.isColumnNameSelected(column) || table.isColumnSelected(column), table.getCellDimmed(column, -1), table.isColumnNameEditable(column));				g.setClip(0,0, getBounds().width, botSide);//		g.setClip(0,0, getBounds().width, getBounds().height);		if (table.frameColumnNames) {			Color cg = g.getColor();			g.setColor(Color.gray); 			g.drawLine(leftSide+columnWidth(column), 0, leftSide+columnWidth(column), botSide);			g.setColor(cg);		}		Font fnt= null;		boolean doFocus = table.focusColumn == column && table.boldFont !=null;		if (doFocus){			fnt = g.getFont();			g.setFont(table.boldFont);		}		if (table.showColumnGrabbers) {			if (table.showColumnNumbers) {								table.drawRowColumnNumber(g,column,false,leftSide+1,0, columnWidth(column)-2, table.getColumnGrabberWidth());			}			else				table.drawRowColumnNumberBox(g,column, false,leftSide+1,0, columnWidth(column)-2, table.getColumnGrabberWidth());			g.setClip( leftSide,nameRowTop(), columnWidth(column), botSide);			table.drawColumnNameCell(g, leftSide,nameRowTop(), columnWidth(column), nameRowHeight(), column);						}		else {			g.setClip( leftSide,nameRowTop(), columnWidth(column), botSide-1);			table.drawColumnNameCell(g, leftSide,nameRowTop(), columnWidth(column), nameRowHeight(), column);		}		if (doFocus && fnt !=null){			g.setFont(fnt);		}		g.setClip(0,0, getBounds().width, botSide);				g.setColor(Color.black);		if (table.getDropDown(column, -1)) {			int offset = 0;			if (table.showColumnGrabbers)				offset = table.getColumnGrabberWidth();						dropDownTriangle.translate(leftSide - 8 + columnWidth(column),1 + offset);			g.setColor(Color.white);			g.drawPolygon(dropDownTriangle);			g.setColor(Color.black);			g.fillPolygon(dropDownTriangle);			dropDownTriangle.translate(-(leftSide - 8 + columnWidth(column)),-(1 + offset));		}		g.setClip(clip);		g.drawLine(0, botSide-1, width, botSide-1);	}	/*...............................................................................................................*/	public void repaint(){		checkEditFieldLocation();		super.repaint();	}		/*...............................................................................................................*/	public void paint(Graphics g) {	   	if (MesquiteWindow.checkDoomed(this))	   		return;		try {		int lineX = 0;		int oldLineX=lineX;		int resetWidth = getBounds().width;		int resetHeight = getBounds().height;		width = resetWidth;//this is here to test if width/height should be reset here		height = resetHeight;		Shape clip = g.getClip();		for (int c=table.firstColumnVisible; (c<table.numColumnsTotal) && (lineX<width); c++) { // or lineX+table.columnWidths[c]			redrawName(g, c);		}		g.setClip(0,0, getBounds().width, getBounds().height);		if ((endOfLastColumn()>=0) && (endOfLastColumn()<table.matrixWidth)) {			g.setColor(ColorDistribution.medium[table.colorScheme]);			g.fillRect(endOfLastColumn(), 0, getBounds().width, getBounds().height);		}		table.drawColumnNamesPanelExtras(g, 0,nameRowBottom(),endOfLastColumn(), getBounds().height);		if (lastRowBottom()<height) {			g.setColor(ColorDistribution.medium[table.colorScheme]);			g.fillRect(0, lastRowBottom(), endOfLastColumn(), getBounds().height);		}		g.setColor(Color.black);		if (table.frameColumnNames)			g.drawRect(0, 0, width-1, height);		g.drawLine(0, height-1, width, height-1);		g.setClip(clip);		width = resetWidth;		}		catch (Throwable e){				MesquiteMessage.warnProgrammer("Exception or Error in drawing table (CNP); details in Mesquite log file");				PrintWriter pw = MesquiteFile.getLogWriter();				if (pw!=null)					e.printStackTrace(pw);		}				MesquiteWindow.uncheckDoomed(this);	}	/*...............................................................................................................*/	public void print(Graphics g) {		int lineX = 0;		int oldLineX=lineX;		Shape clip = g.getClip();		for (int c=0; (c<table.numColumnsTotal); c++) { 			lineX += table.columnWidths[c];			g.setColor(Color.black);			//g.setClip( oldLineX,0, table.columnWidths[c], height);			table.drawColumnNameCell(g, oldLineX,0, table.columnWidths[c], height, c);			g.setColor(Color.black);							oldLineX = lineX;		}		g.setColor(Color.black);		g.setClip(0,0, table.getTotalColumnWidth(), height);		g.drawLine(0, height-1,table.getTotalColumnWidth(), height-1);		g.setClip(clip);	}	/*...............................................................................................................*/	public void enterPressed(KeyEvent e){		if (!getEditing())			return;		if (table.getCellsEditable())			table.editMatrixCell(editField.getColumn(), 0);	}	/*...............................................................................................................*/	public void downArrowPressed(KeyEvent e){		if (getEditing())			enterPressed(e);		//here should move selection into matrix	}	  	  	  	/*...............................................................................................................*/	/*@@@...............................................................................................................*/	/** Returns in which column x lies, -1 if to left, -2 if to right. 	Differs from findColumn in that*/	public int findHalfColumn(int x) {		if (x<=0)			return -1;		int cx = 0;		for (int column=table.firstColumnVisible; (column<table.numColumnsTotal) && (cx<x); column++) {			cx += table.columnWidths[column];			if (column>= table.numColumnsTotal)				return -1;			else if (cx>=x)				if (x+(table.columnWidths[column]/2) > cx) // then we are in the right half of the column					return column;				else					return column - 1;		}		return -2; //past the last column	}	/*@@@...............................................................................................................*/	/** returns in which column x lies, -1 if to left, -2 if to right.*/	public int findColumn(int x) {		if (x<=0)			return -1;		int cx = 0;		for (int column=table.firstColumnVisible; (column<table.numColumnsTotal) && (cx<x); column++) {			cx += table.columnWidths[column];			if (column>= table.numColumnsTotal)				return -1;			else if (cx>=x)				return column;		}		return -2;//past the last column	}	/*@@@...............................................................................................................*/	/** returns in which row y lies, -1 if above, -2 if below.*/	public int findRow(int y) {			return -1;	}	/*@@@...............................................................................................................*/	/** returns in which row y lies, -1 if above, -2 if below.*/	public int findSubRow(int y) {		if (y<=nameRowBottom())			return -1;		else {			return (int)(y-nameRowBottom())/rowHeight(-1);		}	}	/*...............................................................................................................*/	   public void mouseDown(int modifiers, int clickCount, long when, int x, int y, MesquiteTool tool) {		if (!(tool instanceof TableTool))			return;		 boolean isArrowEquivalent = ((TableTool)tool).isArrowKeyOnColumn(y,table);		table.adjustingColumnWidth = false;		touchX=-1;		touchColumn=-1;			/*@@@*/		int possibleTouch = findColumn(x);		int regionInCellH = findRegionInCellH(x);		int regionInCellV = findRegionInCellV(y);		int subRow = findSubRow(y);		if (possibleTouch<table.numColumnsTotal && possibleTouch>=0) {			if (subRow>=0) {  // touch on subrow				table.subRowTouched(subRow, possibleTouch,regionInCellH, regionInCellV, x, y, modifiers);			}			else if (tool != null && isArrowEquivalent && table.getUserAdjustColumn()==MesquiteTable.RESIZE && table.nearColumnBoundary(x)  && !MesquiteEvent.shiftKeyDown(modifiers) && !MesquiteEvent.commandOrControlKeyDown(modifiers)) {				touchX=x;				lastX = x;				touchColumn=possibleTouch;				table.shimmerVerticalOn(touchX);				table.adjustingColumnWidth = true;			}			/*@@@*/			else if (tool != null && isArrowEquivalent && table.getUserMoveColumn() && table.isColumnSelected(possibleTouch) && !MesquiteEvent.shiftKeyDown(modifiers) && !MesquiteEvent.commandOrControlKeyDown(modifiers)) {				touchX=x;				lastX = x;				touchColumn=possibleTouch;				table.shimmerVerticalOn(touchX);			}			else if ((table.showColumnGrabbers) && (y<table.getColumnGrabberWidth())) {				if (((TableTool)tool).getIsBetweenRowColumnTool() && !isArrowEquivalent)					possibleTouch = table.findColumnBeforeBetween(x);				table.columnTouched(isArrowEquivalent, possibleTouch,regionInCellH, regionInCellV, modifiers);				if (tool != null && isArrowEquivalent && table.getUserMoveColumn() && table.isColumnSelected(possibleTouch) && !MesquiteEvent.shiftKeyDown(modifiers) && !MesquiteEvent.commandOrControlKeyDown(modifiers)) {					touchX=x;					lastX = MesquiteInteger.unassigned;;					touchColumn=possibleTouch;				}			}			else {				if (((TableTool)tool).getIsBetweenRowColumnTool())					possibleTouch = table.findColumnBeforeBetween(x);				table.columnNameTouched(possibleTouch,regionInCellH, regionInCellV, modifiers, clickCount);			}		}		else if (possibleTouch==-2 && ((TableTool)tool).getWorksBeyondLastColumn())			table.columnTouched(isArrowEquivalent, possibleTouch,regionInCellH, regionInCellV, modifiers);		else if (((TableTool)tool).getDeselectIfOutsideOfCells()) {	   		table.offAllEdits();	   		if (table.anythingSelected()) {	   			table.deselectAllNotify();	   			table.repaintAll();	   		}	   	}	 }	/*...............................................................................................................*/   	public void mouseDrag(int modifiers, int x, int y, MesquiteTool tool) {		if (touchColumn>=0 && tool != null && ((TableTool)tool).isArrowKeyOnColumn(y,table)) {						if (table.getUserAdjustColumn()==MesquiteTable.RESIZE) {				table.shimmerVerticalOff(lastX);				table.shimmerVerticalOn(x);				lastX=x;			}			else if (table.getUserMoveColumn()) {				table.shimmerVerticalOff(lastX);				table.shimmerVerticalOn(x);				lastX=x;			}		}   	 }	/*...............................................................................................................*/   	public void mouseUp(int modifiers, int x, int y, MesquiteTool tool) {		if (touchColumn>=0 && tool != null && ((TableTool)tool).isArrowKeyOnColumn(y,table)) {			if (table.getUserAdjustColumn()==MesquiteTable.RESIZE && table.adjustingColumnWidth) {				table.shimmerVerticalOff(lastX);				int newColumnWidth = table.columnWidths[touchColumn] + x-touchX;				if ((newColumnWidth >= table.getMinColumnWidth()) && (touchX>=0)) {					table.setColumnWidth(touchColumn, newColumnWidth);					table.columnWidthsAdjusted.setBit(touchColumn);					table.repaintAll();					//touchX=-1;				}							}			else if (table.getUserMoveColumn()) {				table.shimmerVerticalOff(lastX);				int dropColumn = findHalfColumn(x);   //cursor; regionH; clickCount; colour by selected				if (dropColumn == -2)					dropColumn = table.getNumColumns();				if (dropColumn != touchColumn && (dropColumn!=touchColumn-1) && !table.isColumnSelected(dropColumn)) //don't move dropped on column included in selection					table.selectedColumnsDropped(dropColumn);			}		table.adjustingColumnWidth = false;		}   	 }   	/*...............................................................................................................*/   	public void mouseExited(int modifiers, int x, int y, MesquiteTool tool) {		if (!table.editingAnything() && !table.singleTableCellSelected()) 				setWindowAnnotation("", null);		setCursor(Cursor.getDefaultCursor());		int column = findColumn(x);		table.mouseExitedCell(modifiers, column, -1, -1, -1, tool);  	}	/*...............................................................................................................*/	public void setCurrentCursor(int modifiers, int column, int x, int y, MesquiteTool tool) {		if (tool == null || !(tool instanceof TableTool))				setCursor(getDisabledCursor());		else if (column<table.numColumnsTotal && column>=0) {   //within bounds of normal columns			if (((TableTool)tool).isArrowKeyOnColumn(y,table)) {				if (table.getUserAdjustColumn()==MesquiteTable.RESIZE && table.nearColumnBoundary(x)  && !MesquiteEvent.shiftKeyDown(modifiers) && !MesquiteEvent.controlKeyDown(modifiers)) 					setCursor(table.getEResizeCursor());				else {					setCursor(table.getHandCursor());					if (!(table.editingAnything() || table.singleTableCellSelected())) {						String s = table.getColumnComment(column);						if (s!=null)   							setWindowAnnotation(s, "Footnote above refers to " + table.getColumnNameText(column));						else							setWindowAnnotation("", null);					}				}			}			else if (((TableTool)tool).getWorksOnColumnNames())				setCursor(tool.getCursor());			else				setCursor(getDisabledCursor());		}		else if (((TableTool)tool).getWorksBeyondLastColumn() && (column==-2))			setCursor(tool.getCursor());		else			setCursor(getDisabledCursor());	}	/*...............................................................................................................*/	public void mouseEntered(int modifiers, int x, int y, MesquiteTool tool) {		int column = findColumn(x);		setCurrentCursor(modifiers, column ,  x, y, tool);		table.mouseInCell(modifiers, column,-1, -1, findSubRow(y),tool);	}	/*...............................................................................................................*/	public void mouseMoved(int modifiers, int x, int y, MesquiteTool tool) {		int column = findColumn(x);		setCurrentCursor(modifiers, column ,  x, y, tool);		table.mouseInCell(modifiers, column, -1,-1,  findSubRow(y), tool);	}}