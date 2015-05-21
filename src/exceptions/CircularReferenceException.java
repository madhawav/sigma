/*
 *  Spreadsheet by Madhawa

 
 */
package exceptions;

/**
 *
 * @author Madhawa
 */
/*
Circular reference occurs when an unstable web of cell references incur. 
    eg 
        A1 = A2
        A2 = A1
    From above data, what is the value of A1 and A2?
    They are being circularly referred
*/
public class CircularReferenceException extends SpreadsheetException {
    @Override
    public String getMessage()
    {
        return "%Circular Reference";
    }
}
