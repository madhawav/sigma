/*
 *  Spreadsheet by Madhawa

 
 */

package functions;

import exceptions.SpreadsheetException;
import solver.DataTypeElement;
import java.util.Stack;

/**
 *
 * @author 130614N
 */
/*
    Functions are used to undertake complex mathematical operations.
    Eg - max(a,b) can be used to select the max value from a and b.
    This class defines the abstract class used by such functions
*/
public abstract class AbstractFunction {
    //returns function string as it should appear in expression
    public abstract String getFunctionName();
    //applies this function to the input given in RPN. 
    public abstract void solveReversePolishStack(Stack<DataTypeElement> stack) throws SpreadsheetException;
    
}
