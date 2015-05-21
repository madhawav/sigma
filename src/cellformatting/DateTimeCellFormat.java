/*
 *  Spreadsheet by Madhawa

 
 */
package cellformatting;


import datatypes.DataType;
import datatypes.DateTimeDataType;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/**
 *
 * @author Madhawa
 */
/*
    Used to represent DateTime cell format
*/
public class DateTimeCellFormat extends AbstractCellFormat {
    private String dateFormat = "MM/dd/yyyy";

    //the term used to identify this type when spreadsheet is saved to disk
    public static final String serializedName = "DateTime";
    
    public String getDateFormat()
    {
        return dateFormat;
    }
    public void setDateFormat(String newFormat)
    {
        dateFormat= newFormat;
    }
    
    public DateTimeCellFormat()
    {
    }
    //creates a datetime format with given format in parameter
    public DateTimeCellFormat(String format)
    {
        dateFormat = format;
    }
    @Override
    public String getFormattedText(DataType value) {
        if(value instanceof DateTimeDataType)
        {
            DateTimeDataType dateTime = (DateTimeDataType)value;
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            String formattedDate = sdf.format(dateTime.getValue().getTime());
            return formattedDate;
        }
        else
        {
           return value.getCellText(null);
        }
    }

    
    @Override
    public void applyArguements(HashMap<String, Serializable> arguements) {
        this.dateFormat = (String) arguements.get("DateFormat");
    }

    @Override
    public String getSerializedCellFormatName() {
        return serializedName;
    }

    @Override
    public void appendSerializedArguements(HashMap<String,Serializable> target)  {
       
        target.put("DateFormat", dateFormat);
      
    }
    
}
