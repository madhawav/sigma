
package spreadsheet.gui;

import java.awt.Color;

/**
 *
 * @author Madhawa
 */
/*
    Cell highlights are used to highlight cells in spreadsheet gui. This is useful to contrast relavent cells to a given expression
    
*/
class CellHighlight {
    //for range selection, start and end ids are used. 
    //the boundry cells are also considered selected
    //only continues selections are supported by one class
    private int startColumnId;
    private int endColumnId;
    private int startRowId;
    private int endRowId;
    
    //getters and setters
    public int getStartColumnId() {return startColumnId;}
    public int getEndColumnId() { return endColumnId; }
    public int getStartRowId() {return startRowId;}
    public int getEndRowId() { return endRowId; }
    
    public void setStartColumnId(int value) { startColumnId = value; }
    public void setEndColumnId(int value) { endColumnId = value; }
    public void setStartRowId(int value) { startRowId = value; }
    public void setEndRowId(int value) { endRowId = value; }
    
    public CellHighlight(int startColumnId, int endColumnId, int startRowId, int endRowId)
    {
        this.startColumnId = startColumnId;
        this.startRowId = startRowId;
        this.endRowId = endRowId;
        this.endColumnId = endColumnId;
    }
    
    //check whether a cell is within the range of this CellSelection
    public boolean isSelected(int rowId, int columnId)
    {
        if(rowId >= startRowId && rowId <= endRowId && columnId >= startColumnId && columnId <= endColumnId)
            return true;
        return false;
    }
    //default selection colour is gray. 
    public Color getColour()
    {
        return Color.LIGHT_GRAY;
    }
}
