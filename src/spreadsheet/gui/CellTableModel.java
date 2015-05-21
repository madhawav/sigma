/*
 *  Spreadsheet by Madhawa

 
 */

package spreadsheet.gui;

import javax.swing.JTable;
import spreadsheet.Cell;
import spreadsheet.Spreadsheet;

/*

    A custom table model
    This table model is used to bind the spreadsheet data to jTable used to preview.


*/

/**
 *
 * @author 130614N
 */
class CellTableModel extends javax.swing.table.AbstractTableModel {
   
    //references to the spreadsheet and the jTable in the ui
    private spreadsheet.Spreadsheet spreadsheet = null;
    private JTable uiCellTable = null;
    
    //all cells readonly is set to true when a user is editing a cell in the address-bar
    private boolean allCellsReadonly = false;
    
    public boolean getAllCellsReadonly()
    {
        return allCellsReadonly;
    }
    
    public void setAllCellsReadonly(boolean value)
    {
        allCellsReadonly = value;
    }
    
    public Spreadsheet getSpreadsheet()
    {
        return spreadsheet;
    }
    
    public void setSpreadsheet(Spreadsheet value)
    {
        this.spreadsheet = value;
    }
    
    public void setUICellTable(JTable value)
    {
        this.uiCellTable = value;
    }
    
    public JTable getUICellTable()
    {
        return this.uiCellTable;
    }
    
    //is a cell editable? row headers return false
    @Override
    public boolean isCellEditable(int row, int column)
    {
        //0th column has row headers. Hence not editable.
        if(allCellsReadonly)
            return false;
        if(column == 0)
            return false;
        return true;
    }
    
    
    @Override
    public int getRowCount() {
        return spreadsheet.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return spreadsheet.getColumnCount() + 1;
    }

    @Override
    public String getColumnName(int column)
    {
        
        if(column == 0) //0th column has row headers
            return "";
        
        return super.getColumnName(column-1); //follow usual naming convention from second column onwards. (first column is reserved for row headers)
        
    }
    @Override
    public void setValueAt(Object val, int rowIndex, int columnIndex)
    {
        //Updates the value in spreadsheet to match the new cell expression
        String expression = val.toString();
        String columnName = Spreadsheet.indexToColumnName(columnIndex - 1);
        String cellName = columnName + String.valueOf(rowIndex+1);
        
        spreadsheet.setCellExpression(cellName, expression);
        if(this.uiCellTable != null)
            this.uiCellTable.repaint(); //update the table to reflect changes
        
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        //retrieves the appropriate cell value from spreadsheet
        
        if(spreadsheet == null) return "N/A";
        if(columnIndex == 0)
        {
            //its the row header
            return String.valueOf(rowIndex+1);
        }
           
        String columnName = Spreadsheet.indexToColumnName(columnIndex - 1); //we need to have a column reserved for headers. Therefore -1 is used
        String cellName = columnName + String.valueOf(rowIndex+1); //selected cells id
        
        Cell cell = spreadsheet.getCell(cellName);
        
        if(cell == null)
        {
            //the cell has no assigned value. 
            //i.e. there are no records about this cell in the spreadsheet. 
            //such cells are blank cells.
            //(we dont keep records about blank cells since it will consume a lot of memory)
            return "";
        }
        return cell.getCellText();
        
        
    }
    
}
