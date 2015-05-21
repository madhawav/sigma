/*
 *  Spreadsheet by Madhawa

 
 */
package serializables;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author Madhawa
 */
/*
    A spreadsheet can be converted to a much lightweight serializable table. This table would contain very basic information of a spreadsheet such as
        cell expression of each cell
*/
public class SerializableTable implements Serializable {
    //map of cell expressions
    private HashMap<String,SerializableCell> cellMap = new HashMap<>();
    //time-stamp saved with the file
    private Date timeStamp = null;
    
    private int columnCount = 1;
    private int rowCount = 1;
    
    public int getColumnCount()
    {
        return columnCount;
    }
    public int getRowCount()
    {
        return rowCount;
    }
    public void setColumnCount(int value)
    {
        columnCount = value;
    }
    public void setRowCount(int value)
    {
        rowCount = value;
    }
    
    public Date getTimeStamp()
    {
        return timeStamp;
    }
    public void setTimeStamp(Date value)
    {
        timeStamp = value;
    }
    public HashMap<String,SerializableCell> getCellMap()
    {
        return cellMap;
    }
    
}
