/*
 *  Spreadsheet by Madhawa

 
 */

package exceptions;

/**
 *
 * @author 130614N
 */
/*
    Represents an exception which is generated in the code of this program
    To be inherited from the packages in this project
*/
public class SpreadsheetException extends Exception {
    //In sub classes, getMessage will be overridden. This determines the value which should appear in the cell
    @Override
    public String getMessage()
    {
        return "%Error";
    }
}
