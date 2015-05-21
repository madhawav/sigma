/*
 *  Spreadsheet by Madhawa

 
 */
package spreadsheet.gui;

import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.CellEditorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import spreadsheet.Cell;
import spreadsheet.Spreadsheet;

/**
 *
 * @author Madhawa
 */

    /*Used to edit the values in cells
     This class does the role of swapping the field value with expression when a cell value is edited(inplace editing).
     During editing, text and the selection in addressbar is adjusted to match the selection and text in cell being edited.
     Therefore, the events triggered in the addressbar would update the ui cell highlights as required.
        Cell highlights - highlight relavent cells in special colours when an expression is being edited.

*/  

class SpreadsheetCellEditor  extends DefaultCellEditor implements TableCellEditor{
    
    //the textField representing the cell in ui
    private JTextField editField ;
    
    //keep reference to the spreadsheet
    private spreadsheet.Spreadsheet spreadsheet;
    
    //notifies the registered cellEditListners that focus has been gained
    private void notifyCellEditFocusGained()
    {
   
        CellEditorListener[] listners = this.getCellEditorListeners();
        for(CellEditorListener listener:listners)
        {
            if(listener instanceof ISpreadsheetCellEditorListener)
            {
                ISpreadsheetCellEditorListener spreadsheetListener = (ISpreadsheetCellEditorListener)listener;
                spreadsheetListener.cellEditorFocusGained();
            }
        }
    }
    
    //notifies the registered cellEditListners that focus has been lost
    private void notifyCellEditFocusLost()
    {
   
        CellEditorListener[] listners = this.getCellEditorListeners();
        for(CellEditorListener listener:listners)
        {
            if(listener instanceof ISpreadsheetCellEditorListener)
            {
                ISpreadsheetCellEditorListener spreadsheetListener = (ISpreadsheetCellEditorListener)listener;
                spreadsheetListener.cellEditorFocusLost();
            }
        }
    }
    
    //notifies the registerd cellEditListners that cell editing has begun
    private void notifyCellEditingBegun()
    {
       
        CellEditorListener[] listners = this.getCellEditorListeners();
        for(CellEditorListener listener:listners)
        {
            if(listener instanceof ISpreadsheetCellEditorListener)
            {
                ISpreadsheetCellEditorListener spreadsheetListener = (ISpreadsheetCellEditorListener)listener;
                spreadsheetListener.cellEditingBegun();
            }
        }
       
    }
    
    //notified the registered cellEditorListners that caret has been updated
    private void notifyCaretUpdated(CaretEvent e)
    {
         CellEditorListener[] listners = this.getCellEditorListeners();
        for(CellEditorListener listener:listners)
        {
            if(listener instanceof ISpreadsheetCellEditorListener)
            {
                ISpreadsheetCellEditorListener spreadsheetListener = (ISpreadsheetCellEditorListener)listener;
                spreadsheetListener.cellCaretUpdated(e);
            }
        }
    }
   

    //notify all registered cellEditorListeners that text has changed in cell being edited
    private void notifyDocumentListenerTextChanged(DocumentEvent e)
    {
        CellEditorListener[] listners = this.getCellEditorListeners();
        for(CellEditorListener listener:listners)
        {
            if(listener instanceof ISpreadsheetCellEditorListener)
            {
                ISpreadsheetCellEditorListener spreadsheetListener = (ISpreadsheetCellEditorListener)listener;
                spreadsheetListener.cellTextChanged(editField.getText(),e);
            }
        }
       
    }
    
    public spreadsheet.Spreadsheet getSpreadsheet()
    {
        return spreadsheet;
    }
    
    public SpreadsheetCellEditor(spreadsheet.Spreadsheet currentSheet)
    {
        super(new JTextField());       
        this.spreadsheet = currentSheet;
    }
    
    @Override
    public Object getCellEditorValue() {
        //pass through the new cell text as it is. It will be evaluated when the setCellValue function of CellTableModel is called
        return editField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, int row, int column) {
        
        //retrieves the ui editor for the cell. 
        editField = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
        if(table==null)
        {
            editField.setText("");
            return editField;
        }
        //find cellId
        String columnName = Spreadsheet.indexToColumnName(column - 1);
        String cellName = columnName + String.valueOf(row+1);
        
        Cell cell = spreadsheet.getCell(cellName);
        if(cell != null)
        {
            //Set the cell expression instead of cell value. Swapping occurs here. (cell value shown in cell now becomes the cell expression for editing purpose)
            editField.setText(cell.getCellExpression());
        }
        else{
            editField.setText("");
        }
        //to identify when editing begins in cell, a listner is used.       
        
         
        editField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                
               notifyCellEditFocusGained();
                
            }

            @Override
            public void focusLost(FocusEvent e) {
                
                notifyCellEditFocusLost();
            }

        });
        
        //reflect the cursor position of currentCell in the addressBar.
        //Therefore, both the addressbar and the cell selects the same token
        editField.addCaretListener(new CaretListener() {

            @Override
            public void caretUpdate(CaretEvent e) {
                notifyCaretUpdated(e);
               
                
            }
        });
        //we need a listner to update the ui textfield
        editField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyDocumentListenerTextChanged(e);
                
                
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyDocumentListenerTextChanged(e);
                
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyDocumentListenerTextChanged(e);
           
            }
        });
        //pass the message to listners.
         notifyCellEditingBegun();
      
        
        return editField;
    }

   
    
    
}