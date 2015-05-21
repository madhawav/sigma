/*
 *  Spreadsheet by Madhawa

 
 */
package exceptions;

/**
 *
 * @author Madhawa
 */
/*
    This error is raised when a translation function defined in parser is used inappropriately. This usually occurs if a translation results a cell which doesnt exist.
*/
public class TranslateException extends SpreadsheetException  {
    @Override
    public String getMessage()
    {
        return "%!REF";
    }
}
