/*
 *  Spreadsheet by Madhawa

 
 */

package functions;

import datatypes.BasicMath;
import exceptions.MissingParameterException;
import exceptions.SpreadsheetException;
import solver.DataTypeElement;
import solver.ParsedElement;
import java.util.Stack;

/**
 *
 * @author 130614N
 */
/*
    Represents absolute function
*/
public class Abs extends AbstractFunction {
    public static final String functionName = "abs";
    @Override
    public String getFunctionName() {
        return "abs";
    }

    @Override
    public void solveReversePolishStack(Stack<DataTypeElement> stack) throws SpreadsheetException {
         //Pop the two function arguements used by sum
        if(stack.isEmpty())
            throw new MissingParameterException();
        DataTypeElement token1 = stack.pop();
        
        //retrieve there values
        
        datatypes.DataType n1 = token1.getDataType();
        datatypes.DataType n3 = BasicMath.AbsoluteValue(n1);
        stack.push(new ParsedElement(n3));
        
    }
    
}
