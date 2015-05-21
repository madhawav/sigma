/*
 *  Spreadsheet by Madhawa

 
 */

package exceptions;

/**
 *
 * @author 130614N
 */
/*
    raised when a data type is divided by zero
*/
public class DivZeroException extends exceptions.SpreadsheetException {

    @Override
    public String getMessage()
    {
        return "%Division By Zero";
    }
}
