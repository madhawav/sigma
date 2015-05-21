/*
 *  Spreadsheet by Madhawa

 
 */
package cellformatting;

import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author Madhawa
 */
/*
    Represents a cell format which can be used to customize the data shown by a cell
*/
public abstract class AbstractCellFormat {
    public abstract String getFormattedText(datatypes.DataType value);
    public void applyArguements(HashMap<String,Serializable> arguements) //applies the arguements given in hashmap to this cell format
    {
        
    } 
    public abstract String getSerializedCellFormatName(); //return the name to be placed to represent this cell format during serialization
    public void appendSerializedArguements(HashMap<String,Serializable> target) //returns the argeuments of the formatting serialized
    {
       
    }
}
