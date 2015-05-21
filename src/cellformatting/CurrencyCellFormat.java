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
    Denotes the cell format for currency. This could be $ or Rs.
*/
public class CurrencyCellFormat extends AbstractCellFormat {
    private String currencyPrefix = "Rs. ";
    //the term used to identify this type when spreadsheet is saved to disk
    public static final String serializedName = "Currency";
    
    public CurrencyCellFormat()
    {
        
    }
    
    public void setCurrencyPrefix(String newPrefix)
    {
        currencyPrefix = newPrefix;
    }
    public String getCurrencyPrefix()
    {
        return currencyPrefix;
    }

    public CurrencyCellFormat(String prefix)
    {
        currencyPrefix = prefix;
    }
    @Override
    public String getFormattedText(DataType value) {
        if(value instanceof datatypes.CurrencyDataType)
        {
            //if the data-type is also currency, we will use the prefix given by it
            return value.getCellText(null);
        }
        else if(value instanceof datatypes.DecimalNumberDataType)
        {
            //if not if the data-type is decimal, we will use the formatting given here
            DecimalNumberDataType number = (DecimalNumberDataType)value;
            BigDecimal rounded = number.getValue().setScale(2, RoundingMode.HALF_UP);
            return currencyPrefix + rounded.toPlainString() ;
            
        }
        else
        {
            return value.getCellText(null);
        }
    }

    
    @Override
    public void applyArguements(HashMap<String, Serializable> arguements) {
        this.currencyPrefix = (String) arguements.get("Prefix");
    }

    @Override
    public String getSerializedCellFormatName() {
        return serializedName;
    }

    @Override
    public void appendSerializedArguements(HashMap<String,Serializable> target)  {
       
        target.put("Prefix", currencyPrefix);
      
    }
    
    
}
