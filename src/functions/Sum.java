/*
 *  Spreadsheet by Madhawa

 
 */
package functions;

import datatypes.ArrayDataType;
import datatypes.BasicMath;
import datatypes.DecimalNumberDataType;
import exceptions.InvalidOperationException;
import exceptions.MissingParameterException;
import exceptions.SpreadsheetException;
import solver.DataTypeElement;
import solver.ParsedElement;
import java.math.BigDecimal;
import java.util.Stack;

/**
 *
 * @author Madhawa
 */
/*
    The sum function which can be used to add set of values in a cell-range
    The cells should be decimal number compatible in addition.
        Incompatible cells will be ignored

*/
public class Sum extends AbstractFunction {

    //The name of the function undertaken by this class
    public static final String functionName = "sum";
    
    @Override
    public String getFunctionName() {
        return "sum";
    }

    @Override
    public void solveReversePolishStack(Stack<DataTypeElement> stack) throws SpreadsheetException {
        //Pop the function arguement used by sum
        if(stack.isEmpty())
            throw new MissingParameterException();
        
        DataTypeElement token1 = stack.pop();

        //retrieve there values
        datatypes.DataType n1 = token1.getDataType();
        datatypes.DataType n3 = n1; //pass through if following if condition fails
        if(n1 instanceof ArrayDataType)
        {
            //we have an array of data-types as an arguement. so function can work properly.
            ArrayDataType dataRange = (ArrayDataType)n1;
            //retrieve the vector of data given by cell-range
            datatypes.DataType[] dataVector = dataRange.asVector();
            if(dataVector.length == 0)
            {
                //empty data vector as input
                throw new InvalidOperationException();
            }
            //take n3 as 0
            n3 = new DecimalNumberDataType(new BigDecimal(0));
            //now add  values to this n3
            for(int i = 0;i <dataVector.length;i++)
            {
                try{
                    n3 = BasicMath.add(n3, dataVector[i]);
                }
                catch(SpreadsheetException ex)
                {
                    //errors occur because we tried to add non-decimal value. lets skip this element. (similar to ms excep)
                }
                
            }
            
        }
        
        //push the results back to the stack
        stack.push(new ParsedElement(n3));
    }
    
}
