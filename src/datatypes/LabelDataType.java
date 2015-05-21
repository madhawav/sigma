/*
 *  Spreadsheet by Madhawa

 
 */

package datatypes;

/**
 *
 * @author 130614N
 */
public class LabelDataType extends DataType {
    private String cellText = "";
    
    public LabelDataType(String expression)
    {
        cellText = expression;
    }
    @Override
    public String getCellText(cellformatting.AbstractCellFormat cellFormat) {
        if(cellFormat == null)
            return cellText; //To change body of generated methods, choose Tools | Templates.
        return cellFormat.getFormattedText(this);
    }

   
    
  
    
    
   
}
