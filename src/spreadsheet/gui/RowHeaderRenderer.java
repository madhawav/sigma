/*
 *  Spreadsheet by Madhawa

 
 */
package spreadsheet.gui;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Madhawa
 */
/*
    The left-most column is used to represent row headers
    Its renderer is defined here.

*/
class RowHeaderRenderer extends JLabel implements TableCellRenderer {

    public RowHeaderRenderer()
    {
        this.setOpaque(true);
    }
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if(table != null)
        {
            //copy from row header
            JTableHeader header = table.getTableHeader();
            setForeground(header.getForeground());
            setBackground(header.getBackground());
            setFont(header.getFont());
        }
        if(isSelected)
            setFont(getFont().deriveFont(Font.BOLD));
        this.setText(value.toString());
        
        return this;
    }
    
}
