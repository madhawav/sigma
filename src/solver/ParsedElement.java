/*
 *  Spreadsheet by Madhawa

 
 */
package solver;

import datatypes.DataType;

/**
 *
 * @author Madhawa
 */


//holds a parsed DataType object. Generated as result of evaluating expressions.
public class ParsedElement implements DataTypeElement{

    DataType dataType = null;
    public ParsedElement(DataType value)
    {
        this.dataType = value;
    }
    @Override
    public DataType getDataType() {
        return dataType;
    }
    public void setDataType(DataType value)
    {
        dataType = value;
    }
    
}
