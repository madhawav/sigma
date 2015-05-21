/*
 *  Spreadsheet by Madhawa

 
 */
package solver;

import datatypes.DataType;

/**
 *
 * @author Madhawa
 */

//an expression element which holds a datatype object. parsed or unparsed.
public interface DataTypeElement {
    //Retrieves the internal DataType object
    public DataType getDataType() throws exceptions.SpreadsheetException;
    
}
