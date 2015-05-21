/*
 *  Spreadsheet by Madhawa

 
 */

package exceptions;

/**
 *
 * @author 130614N
 */
/*
    Raised when the parser fails to parse a particular data type
*/
public class ParseException extends exceptions.SpreadsheetException {
    private String parseText;
    public ParseException(String parseText)
    {
        this.parseText = parseText;
    }
    public String getParseText()
    {
        return this.parseText;
    }
    public String getMessage()
    {
        return "%Parse Error";
    }
}
