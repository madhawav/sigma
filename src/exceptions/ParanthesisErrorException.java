/*
 *  Spreadsheet by Madhawa

 
 */

package exceptions;

/**
 *
 * @author 130614N
 */
//Represents bracket-mismatch exceptions
public class ParanthesisErrorException extends exceptions.SpreadsheetException {
    @Override
    public String getMessage()
    {
        return "%Bracket Error";
    }
}
