/*
 *  Spreadsheet by Madhawa

 
 */
package solver;


import exceptions.MissingOperandException;
import exceptions.SpreadsheetException;
import java.util.Stack;

/**
 *
 * @author 130614N
 */
/*
    Represents mathematical operators found in expressions.
    Eg - + - * /
    Holds the precedence values of each operator and its associativity.
    Instantiated to associate a particular operator found in an expression
*/
public class Operator extends MathSymbol {
    
    public static int getPrecedence(OperatorType operator) //Returns the precendece value of the given operator 
    {
        switch(operator)
        {
            case Add:
                return 3;
            case Subtract:
                return 3;
            case Multiply:
                return 4;
            case Divide:
                return 4;
            case Power:
                return 5;
            case Negate:
                return 6;
            case Equals:
                return 0;
            case NotEquals:
                return 0;
            case LessThan:
                return 2;
            case LessThanOrEqual:
                return 2;
            case GreaterThan:
                return 1;
            case GreaterThanOrEqual:
                return 1;
        }
        return 0;
    }
    
    public Operator(OperatorType opType)
    {
        this.operatorType = opType;
    }
    
    public static Operator tryParse(String parseText) //attempts to parse the parseText to an operator object. returns null in failure
    {
        String trimmed = parseText.trim();
        if(trimmed.equals("+"))
            return new Operator(OperatorType.Add);
        if(trimmed.equals("neg"))
            return new Operator(OperatorType.Negate);
        if(trimmed.equals("-"))
            return new Operator(OperatorType.Subtract);
        if(trimmed.equals("*"))
            return new Operator(OperatorType.Multiply);
        if(trimmed.equals("/") || trimmed.equals("\\"))
            return new Operator(OperatorType.Divide);
        if(trimmed.equals("^"))
            return new Operator(OperatorType.Power);
        if(trimmed.equals("="))
            return new Operator(OperatorType.Equals);
        if(trimmed.equals(">"))
            return new Operator(OperatorType.GreaterThan);
        if(trimmed.equals(">="))
            return new Operator(OperatorType.GreaterThanOrEqual);
        if(trimmed.equals("<"))
            return new Operator(OperatorType.LessThan);
        if(trimmed.equals("<="))
            return new Operator(OperatorType.LessThanOrEqual);
        if(trimmed.equals("<>"))
            return new Operator(OperatorType.NotEquals);
        
        return null;
    }
    
    public static boolean isLeftAssociative(OperatorType operator)//is the given operator a left associative
    {
        if(operator == OperatorType.Power)
            return false;
        if(operator == OperatorType.Negate)
            return false;
        if(operator == OperatorType.Equals)
            return false;
        return true;
    }
    
    private OperatorType operatorType; //Type of mathematical operation. Addition, subtraction....
    
    public OperatorType getOperatorType()
    {
        return this.operatorType;
    }
    public void setOperatorType(OperatorType newOperatorType)
    {
        this.operatorType = newOperatorType;
    }
    
    public int getPrecedence()
    {
        return getPrecedence(this.operatorType);
    }
    public boolean isLeftAssociative()
    {
        return isLeftAssociative(this.operatorType);
    }
    //Solves a stack of inputs generated using RPN using the current operator
    
    public void solveReversePolishStack(Stack<DataTypeElement> stack) throws SpreadsheetException
    {
       
        if(operatorType== OperatorType.Negate)
        {
            //Special case
            //Negation uses only one input
            if(stack.isEmpty())
                throw new MissingOperandException();
            
            DataTypeElement token1 = stack.pop();

            datatypes.DataType n1 = token1.getDataType();
            datatypes.DataType n3 = datatypes.BasicMath.negate(n1);
            stack.push(new ParsedElement(n3));    
            
        }
        else
        {
            //pop the last two numbers in the stack (n1 and n2). Do n3 = n1 * n2, where * is this operator. pop n3 to stack.
            
             //retrieve n1 and n2
             if(stack.isEmpty())
                throw new MissingOperandException();
            DataTypeElement token2 = stack.pop();
            
             if(stack.isEmpty())
                throw new MissingOperandException();
            DataTypeElement token1 = stack.pop();
             
            datatypes.DataType n1 = token1.getDataType();
            datatypes.DataType n2 = token2.getDataType();
            
            datatypes.DataType n3 = null;
            //Solve
            switch(operatorType)
            {
                case Add:
                    n3 = datatypes.BasicMath.add(n1, n2);
                    break;
                case Subtract:
                    n3 = datatypes.BasicMath.subtract(n1, n2);
                    break;
                case Multiply:
                    n3 = datatypes.BasicMath.multiply(n1, n2);
                    break;
                case Divide:
                    n3 = datatypes.BasicMath.divide(n1, n2);
                    break;
                case Power:
                    n3 = datatypes.BasicMath.pow(n1, n2);
                    break;
                case Equals:
                    n3 = datatypes.BasicMath.isEqual(n1, n2);
                    break;
                case NotEquals:
                    n3 = datatypes.BasicMath.isNotEqual(n1, n2);
                    break;
                case LessThan:
                    n3 = datatypes.BasicMath.isLessThan(n1, n2);
                    break;
                case LessThanOrEqual:
                    n3 = datatypes.BasicMath.isLessThanOrEqual(n1, n2);
                    break;
                case GreaterThan:
                    n3 = datatypes.BasicMath.isGreaterThan(n1, n2);
                    break;
                case GreaterThanOrEqual:
                    n3 = datatypes.BasicMath.isGreaterThanOrEqual(n1, n2);
                    break;
               
            }
            //put the result back in stack
            stack.push(new ParsedElement(n3));    
        }
        
    }
    @Override
    public String asString() {
          switch(operatorType)
        {
            case Add:
                return "+";
            case Subtract:
                return "-";
            case Multiply:
                return "*";
            case Divide:
                return "/";
            case Power:
                return "^";
            case Negate:
                return "-";
            case Equals:
                return "=";
            case GreaterThan:
                return ">";
            case GreaterThanOrEqual:
                return ">=";
            case LessThan:
                return "<";
            case LessThanOrEqual:
                return "<=";
            case NotEquals:
                return "<>";
        }
        return "";
    }
    
}
