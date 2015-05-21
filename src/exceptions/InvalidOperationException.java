/*
 *  Spreadsheet by Madhawa

 
 */
package exceptions;

/**
 *
 * @author Madhawa
 */

//Exception thrown when an invalid operation is attempted. Probably due to an error in input expression.
public class InvalidOperationException extends exceptions.SpreadsheetException{

    @Override
    public String getMessage()
    {
        return "%Unsupported Operation";
    }
    
    
}
