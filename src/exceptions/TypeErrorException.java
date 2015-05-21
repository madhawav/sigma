/*
 *  Spreadsheet by Madhawa

 
 */

package exceptions;

/**
 *
 * @author 130614N
 */
//Raised when data types required for a operation doesn't match
public class TypeErrorException extends exceptions.SpreadsheetException {
    @Override
    public String getMessage()
    {
        return "%Type Error";
    }
}
