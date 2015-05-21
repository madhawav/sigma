/*
 *  Spreadsheet by Madhawa

 
 */

package datatypes;




/**
 *
 * @author 130614N
 */
/*
    BaseType of all data types. 
    The concrete data types would posses static functions to parse. (tryParse)
    CellText is the string which appear in the cell, representing this data type.
        This text may get modified based on cell format

    The derived classes hold the suffix "DataType" to avoid confusions with JAVA SE Data Types
    
*/
public abstract class DataType {
    public abstract String getCellText(cellformatting.AbstractCellFormat cellFormat); //How it should appear in the cell of spreadsheet. pass null as cellFormat to use the default formating defined by datatype
   // public abstract String getExpression(); //Gets or rebuilds the underlying expression
}
