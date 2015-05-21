/*
 *  Spreadsheet by Madhawa

 
 */

package exceptions;

/**
 *
 * @author 130614N
 */
//raised when the function name is not available in known set of functions
public class InvalidFunctionNameException extends SpreadsheetException{
    private String functionName;
    public InvalidFunctionNameException(String functionName)
    {
        this.functionName = functionName;
    }
    public String getFunctionName()
    {
        return functionName;
    }
}
