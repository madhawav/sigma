/*
 *  Spreadsheet by Madhawa

 
 */
package spreadsheet.gui;

import javax.swing.event.CaretEvent;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;

/**
 *
 * @author Madhawa
 */
public interface ISpreadsheetCellEditorListener extends CellEditorListener {
    //When the user has started editing the value in a cell, this event is triggered
    public void cellEditingBegun();
    //When the cell editor has gained focus, this event is trigered
    public void cellEditorFocusGained();
    //When the cell editor has lost focus, this event is trigered
    public void cellEditorFocusLost();
    //Called when the text in cell being edited is modified
    public void cellTextChanged(String newText,DocumentEvent documentEvent);
    //Called when the caret of cell editor is updated
    public void cellCaretUpdated(CaretEvent caretEvent);
}
