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
public final class SerializationHelper {
    
    //disable constructor
    private SerializationHelper()
    {
        
    }
    public static AbstractCellFormat deserializeCellFormat(String formatName, HashMap<String,Serializable> arguements)
    {
        AbstractCellFormat result = null;
        if(formatName.equals(DecimalNumberCellFormat.serializedName))
        {
             result = new DecimalNumberCellFormat();
        }
        else if(formatName.equals(GeneralCellFormat.serializedName))
        {
            result = new GeneralCellFormat();
        }
        else if(formatName.equals(DateTimeCellFormat.serializedName))
        {
            result = new DateTimeCellFormat();
        }
        else if(formatName.equals(CurrencyCellFormat.serializedName))
        {
            result = new CurrencyCellFormat();
        }
        else if(formatName.equals(PercentageCellFormat.serializedName))
        {
            result = new PercentageCellFormat();
        }
        else
            result = new GeneralCellFormat(); //assume general for unsupported cell formats
        
        result.applyArguements(arguements);
        return result;
    }
}
