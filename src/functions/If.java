/*
 *  Spreadsheet by Madhawa

 
 */
package functions;

import datatypes.BooleanDataType;
import datatypes.DecimalNumberDataType;
import exceptions.MissingParameterException;
import exceptions.SpreadsheetException;
import exceptions.TypeErrorException;
import solver.DataTypeElement;
import solver.ParsedElement;
import java.math.BigDecimal;
import java.util.Stack;

/**
 *
 * @author Madhawa
 * 
 */
/*
    if formula
    if(condition,true_value,false_value)
    

*/
public class If extends AbstractFunction {

    public static final String functionName = "if";
    
    @Override
    public String getFunctionName() {
        return "if";
    }

    
    @Override
    public void solveReversePolishStack(Stack<DataTypeElement> stack) throws SpreadsheetException {
        //pop 3 function parameters
        if(stack.isEmpty())
            throw new MissingParameterException();
        DataTypeElement token1 = stack.pop();
        
         if(stack.isEmpty())
            throw new MissingParameterException();
        DataTypeElement token2 = stack.pop();
        
         if(stack.isEmpty())
            throw new MissingParameterException();
        DataTypeElement token3 = stack.pop();
        
        //retrieve there values
        datatypes.DataType falseValue = token1.getDataType();
        datatypes.DataType trueValue = token2.getDataType();
        datatypes.DataType condition = token3.getDataType();
        
        //evaluation
        if(condition instanceof BooleanDataType)
        {
            BooleanDataType boolCondition = (BooleanDataType)condition;
            if(boolCondition.getValue() == true)
            {
                //expression evaluated as true. push the param2
                stack.push(new ParsedElement(trueValue));
            }
            else
            {
                //otherwise, push param3
                stack.push(new ParsedElement(falseValue));
            }
        }
        else if(condition instanceof DecimalNumberDataType)
        {
            DecimalNumberDataType decimalCondition = (DecimalNumberDataType)condition;
            if(!decimalCondition.getValue().equals(BigDecimal.ZERO))
            {
                //expression evaluated as non zero. push the param2
                stack.push(new ParsedElement(trueValue));
            }
            else
            {
                //otherwise, push param3
                stack.push(new ParsedElement(falseValue));
            }
        }
        else
        {
            //parameter one doesnt give a boolean or a decimal number result
            throw new TypeErrorException();
        }
        
    }
    
}
