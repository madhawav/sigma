/*
 *  Spreadsheet by Madhawa

 
 */
package serializables;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author Madhawa
 */
/*
    A lightweight cell which can be serialized for saving purposes
*/
public class SerializableCell implements Serializable {
    private String expression = ""; //the expression of cell
    private String formatName = "General"; //the cell format
    private HashMap<String,Serializable> formatArguements = new HashMap<>();
    private SerializableCellStyle cellStyle = SerializableCellStyle.getDefaultCellStyle();
    
    public SerializableCell(){}
    public SerializableCell(String expression)
    {
        this.expression = expression;
    }
    
    public String getExpression()
    {
        return expression;
    }
    
    public void setExpression(String newExpression)
    {
        this.expression = newExpression;
    }
    
    public String getFormatName()
    {
        return formatName;
    }
    
    public void setFormatName(String newName)
    {
        formatName = newName;
    }
    public HashMap<String,Serializable> getFormatArguements()
    {
        return formatArguements;
    }
    public SerializableCellStyle getSerializableCellStyle()
    {
        return cellStyle;
    }
    public void setSerializableCellStyle(SerializableCellStyle newStyle)
    {
        cellStyle =newStyle;
    }
}
