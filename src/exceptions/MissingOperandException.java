/*
 *  Spreadsheet by Madhawa

 
 */
package exceptions;

/**
 *
 * @author Madhawa
 */
/*
    This exception is raised when a given expression has some operands missing. Maybe the expression is incomplete. 
    The raise condition is checked before each and every pop from the RPN stack
*/
public class MissingOperandException extends SpreadsheetException {
    @Override
    public String getMessage()
    {
        return "%Missing Operand";
    }
}
