/*
 *  Spreadsheet by Madhawa

 
 */
package solver;


import exceptions.ParanthesisErrorException;
import datatypes.DecimalNumberDataType;
import exceptions.InvalidOperationException;
import exceptions.ParseException;
import parser.DelimeterElement;
import parser.ExpressionElement;
import parser.TokenElement;
import exceptions.SpreadsheetException;
import functions.AbstractFunction;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Stack;

/**
 *
 * @author 130614N
 */

//Used to solve mathematical expressions
//Requires the expression to be initially parsed using parser.
public class ExpressionEvaluator {
    
    /*
    Genaral Usage
        Parse the input expression using parser and obtain parsedOutput.
        Call getReversedPolishNotation to convert the parsed output into RPN.
        solve the RPN using solveReversePolishNotation function and obtain value of expression
    
    */
    
 
    
    //solve a RPN input recieved as output from getReversePolishNotation function
    public static datatypes.DataType solveReversePolishNotation(Queue<parser.ExpressionElement> input) throws SpreadsheetException
    {
        Stack<DataTypeElement> solveStack = new Stack<>(); //the stack of numbers used for solving
        
        
        while(!input.isEmpty())
        {
            parser.ExpressionElement element = input.poll();
            if(element instanceof parser.TokenElement)
            {
                //element is a value or a function name
                parser.TokenElement currentToken = (parser.TokenElement)element;
                AbstractFunction associatedFunction = currentToken.getTokenFunction();
                
                if(associatedFunction == null)
                {
                    //element is a number
                    //push the numbers to solveStack
                    solveStack.push((parser.TokenElement)element);
                }
                else
                {
                    //element is a functionToken. Hence, solve it
                    associatedFunction.solveReversePolishStack(solveStack);
                }
                
            }
            else if(element instanceof parser.DelimeterElement)
            {
                DelimeterElement delElement = (DelimeterElement)element;
                //follow operator solving protocol if delimeter is an operator
                MathSymbol symbol = delElement.getSymbol();
                if(symbol instanceof Operator)
                {
                    Operator opSymbol = (Operator)symbol;
                    opSymbol.solveReversePolishStack(solveStack);
                }
                
            }
        }
        
        //retrieve the result from stack
        if(solveStack.isEmpty()) //happens if a blank expression is evaluvated
        {
            return new DecimalNumberDataType();
        }
        else if(solveStack.size() > 1)
        {
            //occurs when the input has missing operators
                throw new exceptions.InvalidOperationException(); //we cannot have more than one item left in output array
        }
        else
        {
            DataTypeElement result = (DataTypeElement) solveStack.pop();
            return result.getDataType();
        }
        
    }
    
    //Converts parameter 'tokens' into RPN. Uses Shunting yard algorithm.
    public static Queue<parser.ExpressionElement> getReversedPolishedNotation(ArrayList<parser.ExpressionElement> tokens) throws ParseException,ParanthesisErrorException, InvalidOperationException
    {
        
        Queue<parser.ExpressionElement> output = new java.util.LinkedList<>();
        Stack<ExpressionElement> operatorStack = new Stack<>();
        for (ExpressionElement token : tokens) {
            if(token instanceof parser.TokenElement)
            {
                //token sould be a data-type or a function name. Lets find out!
                parser.TokenElement currentToken = (parser.TokenElement)token;
                AbstractFunction associatedFunction = currentToken.getTokenFunction();
                if(associatedFunction == null)
                {
                    //This should be a number, data range or any other data type
                    output.add(token);
                }
                else
                {
                    //This is a function token
                    operatorStack.push(currentToken);
                }
                
            }
            
            
            else if(token instanceof parser.DelimeterElement)
            {
                DelimeterElement delimToken = (DelimeterElement)token;
                MathSymbol symbol = delimToken.getSymbol();
                //is it the comma used as parameter seperator
                if(symbol != null && symbol instanceof ParameterSeperator)
                {
                    /*
                        pop operator stack into the output queue until a left paranthesis is found.
                    
                    */
                    while(true) //the condition is complex
                    {
                        if(operatorStack.size() == 0)
                        {
                            //no left paranthesis was found
                            throw new ParanthesisErrorException();
                        }
                        
                        ExpressionElement opPeek = operatorStack.peek();
                        //check whether opPeek is a left paranthesis
                        if(opPeek instanceof DelimeterElement)
                        {
                            DelimeterElement opPeekDelim = (DelimeterElement)opPeek;
                            if (opPeekDelim.getSymbol() instanceof LeftParanthesis)
                            {
                                //opPeek is a left paranthesis. stop here.
                                break;
                            }
                        }
                        //opPeek is not a left paranthesis
                        output.add(operatorStack.pop());
                    }
                }
                //is this an operator?
                else if(symbol != null && symbol instanceof Operator)
                {
                    //Yes, its an operator
                    //Now check the stack
                    
                    DelimeterElement delimStackLast;// = (DelimeterElement) operatorStack.peek();
                    MathSymbol lastSym;// = delimStackLast.getSymbol();*/
                    while(!operatorStack.empty())  //will break when the operator stack is empty
                    {
                        if(!(operatorStack.peek() instanceof DelimeterElement))
                            break;
                        delimStackLast = (DelimeterElement)operatorStack.peek();
                        lastSym = delimStackLast.getSymbol();
                        if(lastSym instanceof Operator)
                        {
                            Operator op2 = (Operator)lastSym;//last operator in stack
                            Operator op1 = (Operator)symbol; //current operator
                        
                            //place the operator at the proper place in output
                            if(op1.isLeftAssociative() && op1.getPrecedence() <= op2.getPrecedence())
                            {
                                output.add(operatorStack.pop());
                            }
                            else if(!op1.isLeftAssociative() && op1.getPrecedence() < op2.getPrecedence())
                            {
                                output.add(operatorStack.pop());
                            }
                            else
                                break;
                        }
                        else
                            break; //its not an operator.
                        
                        
                       
                    }
                    operatorStack.push(delimToken); //pushing current operator to stack
                }
                else if(symbol!= null && symbol instanceof LeftParanthesis) //For left paranthesis
                {
                    operatorStack.push(delimToken);
                }
                else if(symbol !=null && symbol instanceof RightParanthesis)
                {
                    boolean leftParaFound = false; //will be used to check whether paranthesis match
                    while(!operatorStack.empty())
                    {
                        //pop all items from operator stack to que, until left paranthesis is found
                        DelimeterElement stackPeek = (DelimeterElement)operatorStack.peek(); 
                        if(stackPeek.getSymbol() instanceof LeftParanthesis)
                        {
                            //when a left paranthesis is found
                            operatorStack.pop();
                            //function label will be added to the output here. 
                            if(operatorStack.size()>0 && operatorStack.peek() instanceof TokenElement)
                            {
                                TokenElement functionCandidate = (TokenElement)operatorStack.peek();
                                //can it be a function header?
                                if(functionCandidate.getTokenFunction() != null)
                                {
                                    //yes it is. just remove it
                                    output.add(operatorStack.pop());
                                }
                            }
                            leftParaFound=true;
                            break;
                        }
                        
                        output.add(operatorStack.pop());
                        
                        
                    }
                    if(!leftParaFound)
                    {
                        throw new ParanthesisErrorException();
                        //ERROR: We have a bracketmismatch
                        
                    }
                    
                    
                }
            }
        }
        
        while(!operatorStack.empty())
        {
            //if paranthese are found, then there is a bracket mismatch
            if(operatorStack.peek() instanceof Paranthesis)
            {
                throw new ParanthesisErrorException();
                //again we have a paranthesis mismatch
            }
            output.add(operatorStack.pop());
            
        }
        
        return output;
    }
}
