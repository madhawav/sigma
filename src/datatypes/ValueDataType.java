/*
 *  Spreadsheet by Madhawa

 
 */

package datatypes;

/**
 *
 * @author 130614N
 */
public abstract class ValueDataType extends DataType{
    
    /*The base class of all Value Data-Types*/
    
    /*For static version of these functions, please refer BasicMaths class.
    I've defined the functions here to improve the clarity of implementation. 
    If these functions are to be solely implemented as in procedural programming, the code length of them will be massive 
    and hence unclear.
    */
    
    public abstract DataType add(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType subtract(ValueDataType value2) throws exceptions.SpreadsheetException;
    
    public abstract DataType negate() throws exceptions.SpreadsheetException; //-x
    
    public abstract DataType multiply(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType divide(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType pow(ValueDataType value2) throws exceptions.SpreadsheetException; //to Power
    
    public abstract DataType isGreaterThan(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType isGreaterThanOrEqual(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType isEqual(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType isNotEqual(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType isLessThan(ValueDataType value2) throws exceptions.SpreadsheetException;
    public abstract DataType isLessThanOrEqual(ValueDataType value2) throws exceptions.SpreadsheetException;
    
    public abstract DataType absoluteValue() throws exceptions.SpreadsheetException; //abs function
}
