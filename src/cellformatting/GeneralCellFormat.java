/*
 *  Spreadsheet by Madhawa

 
 */
package cellformatting;

import datatypes.DataType;


/**
 *
 * @author Madhawa
 */
/*
    No special formatting. use the data-types default formatting
*/
public class GeneralCellFormat extends AbstractCellFormat {

    public static final String serializedName = "General";
    @Override
    public String getFormattedText(DataType value) {
        return value.getCellText(null);
    }

    @Override
    public String getSerializedCellFormatName() {
        return serializedName;
    }
    
    
    
}
