
package spreadsheet.gui;

import java.awt.Color;

/**
 *
 * @author Madhawa
 */
/*
    CellHighlight of cell represented by current expressions selected token
*/
class ActiveCellHighlight extends CellHighlight {

    public ActiveCellHighlight(int startColumnId, int endColumnId, int startRowId, int endRowId) {
        super(startColumnId, endColumnId, startRowId, endRowId);
    }
    @Override
    public Color getColour()
    {
        return Color.ORANGE;
    }
}
