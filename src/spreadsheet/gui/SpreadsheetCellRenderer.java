
package spreadsheet.gui;


import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import serializables.SerializableCellStyle;
import spreadsheet.Cell;
import spreadsheet.Spreadsheet;

/**
 *
 * @author Madhawa
 */
public class SpreadsheetCellRenderer extends DefaultTableCellRenderer {
    
    //the highlighted cells in spreadsheet
    private ArrayList<CellHighlight> cellHighlights = new ArrayList<>();
    
    private Spreadsheet spreadsheet = null;
    public Spreadsheet getSpreadsheet()
    {
        return spreadsheet;
    }
    public void setSpreadsheet(Spreadsheet sheet)
    {
        spreadsheet = sheet;
    }
    public ArrayList<CellHighlight> getCellHighlights()
    {
        return cellHighlights;
    }
    //get the component associated with cell renderer
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,int row,int col)
    {
        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                
        Color prevColor = comp.getBackground(); //backup of selection background colour
       
        SerializableCellStyle style = null;
        int autoAlignment = SwingConstants.LEFT;
         // if the spreadsheet is assigned, styling can be applied
        if(spreadsheet != null)
        {
            //find selected cell
             String columnName = Spreadsheet.indexToColumnName(col - 1); //-1 to leave the row_headers
             String cellName = columnName + String.valueOf(row+1);
         
             //retrieve style
             style = spreadsheet.getCellStyle(cellName);
             //Determine auto alignment
             
             Cell cell = spreadsheet.getCell(cellName);
             if(cell != null)
             {
                 if(cell.getRawDataType() instanceof datatypes.ValueDataType || cell.getRawDataType() instanceof datatypes.ExpressionDataType)
                 {
                     autoAlignment = SwingConstants.RIGHT;
                 }
             }
             //apply style
             style.applyStyle(comp, autoAlignment);
        }
          
        
        if(isSelected)
        {
            comp.setBackground(prevColor);
            return comp; //cellHighlights are ignored for selected cells. We will use the standard system highlight colour for these cells.
        }
            
        //apply cell highlights
        for(CellHighlight highlight : cellHighlights)
        {
            if(highlight.isSelected(row, col))
            {
                JLabel newComp = new JLabel(String.valueOf(value));
                if(style != null)
                    style.applyStyle(comp,autoAlignment );
                newComp.setOpaque(true);
                
                newComp.setBackground(highlight.getColour());
                return newComp;
            }
            
           
        }
        
        return comp;
    }
}
