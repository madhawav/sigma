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
    Represents the percentage (%) cell format
*/
public class PercentageCellFormat extends AbstractCellFormat {

    private int decimalCount  = 2;
    public static final String serializedName = "Percentage";
    
    public int getDecimalPlacesCount()
    {
        return decimalCount;
    }
    public void setDecimalPlacesCount(int decimalCount)
    {
        this.decimalCount = decimalCount;
    }
    
    @Override
    public String getFormattedText(DataType value) {
        if(value instanceof DecimalNumberDataType)
        {
            DecimalNumberDataType number  =(DecimalNumberDataType)value;
            BigDecimal val = (BigDecimal)number.getValue();
            val = val.multiply(new BigDecimal(100)); //to get into 100% format we multiply by 100%
            
            BigDecimal rounded = val.setScale(decimalCount, RoundingMode.HALF_UP);
            //add the percentage mark to the end
            return rounded.toPlainString() + "%";
            
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
