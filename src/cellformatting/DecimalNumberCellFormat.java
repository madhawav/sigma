/*
 *  Spreadsheet by Madhawa

 
 */
package cellformatting;

import datatypes.DataType;
import datatypes.DecimalNumberDataType;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

/**
 *
 * @author Madhawa
 */
/*

    used to format decimal number data type. The number of decimal places to be displayed can be set here.
*/
public class DecimalNumberCellFormat extends AbstractCellFormat {

    //the term used to identify this type when spreadsheet is saved to disk
    public static final String serializedName = "Decimal";
    private int decimalCount = 2;
    
    
    public int getDecimalPlacesCount()
    {
        return decimalCount;
        
    }
    public void setDecimalPlacesCount(int newCount)
    {
        decimalCount = newCount;
    }
    @Override
    public String getFormattedText(DataType value) {
        if(value instanceof DecimalNumberDataType)
        {
            DecimalNumberDataType number  =(DecimalNumberDataType)value;
            BigDecimal rounded = number.getValue().setScale(decimalCount, RoundingMode.HALF_UP);
            return rounded.toPlainString();
            
        }
        return value.getCellText(null);
    }

    @Override
    public void applyArguements(HashMap<String, Serializable> arguements) {
        this.decimalCount = (int) arguements.get("DecimalCount");
    }

    @Override
    public String getSerializedCellFormatName() {
        return serializedName;
    }

    @Override
    public void appendSerializedArguements(HashMap<String,Serializable> target)  {
       
        target.put("DecimalCount", decimalCount);
      
    }
    
}
