/*
 *  Spreadsheet by Madhawa

 
 */
package spreadsheet.gui;

import javax.swing.event.CaretEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;

/**
 *
 * @author Madhawa
 */
/*
    Adapter for SpreadsheetCellEditorListener
*/
public class SpreadsheetCellEditorAdapter implements ISpreadsheetCellEditorListener {

    @Override
    public void cellEditingBegun() {
        
    }

    @Override
    public void cellEditorFocusGained() {
        
    }

    @Override
    public void cellEditorFocusLost() {
        
    }

    @Override
    public void cellTextChanged(String newText, DocumentEvent documentEvent) {
       
    }

    @Override
    public void cellCaretUpdated(CaretEvent caretEvent) {
        
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        
    }
    
}
