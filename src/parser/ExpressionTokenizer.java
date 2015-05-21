/*
 *  Spreadsheet by Madhawa

 
 */
package parser;


import solver.RightParanthesis;
import java.util.ArrayList;


/**
 *
 * @author 130614N
 */
/*
    Tokenizes a given expression and retrieves a list of expression elements which can be parsed individually.
    Token elements and delimeter elements are recognized separately
*/
public final class ExpressionTokenizer {
    
    //disables constructor (similar to math class in java)
    private ExpressionTokenizer()
    {
        
    }

    //Tokenizes an expression and returns the used delemeters and tokens in the order they appeard in input
    public static ArrayList<ExpressionElement> tokenizeExpressionElements(String input, Parser associatedParser)
    {        
        
        ArrayList<ExpressionElement> expressionElements = new ArrayList();
        final String delimeterString = "*\\/(),=<>^"; //The set of delimeters used to split the expression. <,>,= are used in logical expressions
        final String confusingDelimeterString = "+-"; //Similar to delimeterString. But these charachters can confuse with positive, negative indicators
        final String[] twoCharDelimeters = {"<=",">=","<>"};
        String trimmedInput = input.trim();
        String tokenizeString = trimmedInput.substring(1); //We need to omit the = sign at the begining
        
                
        //These two pointers will be used to track the reading block
        int point1 = 0;
        int point2 = 0;
        
        boolean isStringInput = false; //True when we are inside double quotation marks
        
        while(point2 < tokenizeString.length())
        {
            char currentChar = tokenizeString.charAt(point2);
            String prePhrase = tokenizeString.substring(point1,point2).trim();
            if( !isStringInput && matchTwoCharDelimeter(tokenizeString, point2, twoCharDelimeters)) //checking for <=...
            {
                //we have come accross <=...
                DelimeterElement newDelim = new DelimeterElement(tokenizeString.substring(point2,point2 +2));
                if(prePhrase.length() > 0)
                {
                    //We might have a operator here due to a side effect from recognizing confusing delimeters
                    //so need to check
                    if(confusingDelimeterString.contains(prePhrase.trim()))
                    {
                        DelimeterElement newDlm = new DelimeterElement(prePhrase);
                        expressionElements.add(newDlm);
                    }
                    else
                    {
                         TokenElement newToken = new TokenElement(prePhrase,associatedParser);
                         expressionElements.add(newToken);
                    }
                    
                    point2+=2;
                    point1 = point2;
                    
                 
                    
                }
                else
                {
                    point2+=2;
                }
                   //why are we here? is it an operator?
                    
                expressionElements.add(newDelim);
            }
           
            else if(delimeterString.contains(String.valueOf(currentChar)) && !isStringInput) 
            {
                
                //We might have a data type before this delemeter. Its time to identify it.
            
                if(prePhrase.length() > 0)
                {
                    //We might have a operator here due to a side effect from recognizing confusing delimeters
                    //so need to check
                    if(confusingDelimeterString.contains(prePhrase.trim()))
                    {
                        DelimeterElement newDlm = new DelimeterElement(prePhrase);
                        expressionElements.add(newDlm);
                    }
                    else
                    {
                         TokenElement newToken = new TokenElement(prePhrase,associatedParser);
                         expressionElements.add(newToken);
                    }
                   
                    
                   
                    
                    
                    
                }
               
                point2++;
                point1 = point2;
                    
                //Ideitify the operator
                DelimeterElement newDelim = new DelimeterElement(String.valueOf(currentChar));
                expressionElements.add(newDelim);
                
                
            }
            else if(confusingDelimeterString.contains(String.valueOf(currentChar)) && !isStringInput) 
            {
                //We reached a confusing delimeter
                //We might have a data type before this delemeter. Its time to identify it.
            
                if(prePhrase.length() > 0)
                {
                    TokenElement newToken = new TokenElement(prePhrase,associatedParser);
                    expressionElements.add(newToken);
                    point2++;
                    point1 = point2;
                    
                    //why are we here? is it an operator?
                    DelimeterElement newDelim = new DelimeterElement(String.valueOf(currentChar));
                    expressionElements.add(newDelim);
                    
                }
                else 
                {
                   //for negation or subtraction
                    if(currentChar == "-".charAt(0))
                    {
                        if(expressionElements.isEmpty()) //token at start
                        {
                            DelimeterElement newDelim = new DelimeterElement(String.valueOf("neg"));
                            expressionElements.add(newDelim);
                        }    
                        else if(expressionElements.get(expressionElements.size()-1) instanceof DelimeterElement)
                        {
                            //lets check what the last parsed item is

                            DelimeterElement delimLast = (DelimeterElement)expressionElements.get(expressionElements.size()-1);
                            //is it a bracket
                            if(delimLast.getSymbol() instanceof RightParanthesis)
                            {
                                DelimeterElement newDelim = new DelimeterElement(String.valueOf("-"));
                                expressionElements.add(newDelim);
                            }
                            else
                            {
                                DelimeterElement newDelim = new DelimeterElement(String.valueOf("neg"));
                                expressionElements.add(newDelim);
                            }

                        }
                        else
                        {
                            DelimeterElement newDelim = new DelimeterElement(String.valueOf("neg"));
                            expressionElements.add(newDelim);
                        }
                        
                    }
                    //for positive or addition
                    //case of positive has no associated reverse polish notation
                    else if(currentChar == "+".charAt(0))
                    {
                       
                       
                        if(expressionElements.size()> 0 && expressionElements.get(expressionElements.size()-1) instanceof DelimeterElement)
                        {
                            //lets check what the last parsed item is

                            DelimeterElement delimLast = (DelimeterElement)expressionElements.get(expressionElements.size()-1);
                            //is it a bracket
                            if(delimLast.getSymbol() instanceof RightParanthesis)
                            {
                                DelimeterElement newDelim = new DelimeterElement(String.valueOf("+"));
                                expressionElements.add(newDelim);
                            }
                           

                        }
                        
                        
                    }
                   
                    point2++;
                    point1 = point2;
                }
                
                
            }
            //NOTE -> ASCII Value of " is 34
            else if(currentChar == 34 ) //to recognize "
            {
                if(isStringInput)
                {
                   isStringInput = false;
                   //end of label recognition (closing double quotation)
                   
                   point2++;
                   
                }
                else
                {
                    //start of label recognition (opening double quotation)
                   isStringInput = true;
                   
                   if(prePhrase.length() > 0)
                   {
                        TokenElement newToken = new TokenElement(prePhrase,associatedParser);
                        expressionElements.add(newToken);                        
                   }
                   point1 = point2;
                   point2++;
                   
                }
                
            }
            else
            {
                point2++;
            }
        }
        //Still we have to identify the last block of data
        if(point1 < tokenizeString.length())
        {
            String lastPhrase = tokenizeString.substring(point1).trim();
            TokenElement lastToken = new TokenElement(lastPhrase,associatedParser);
            
            expressionElements.add(lastToken);
        }
        
        return expressionElements;
    }
    
    
    //Used to match two char delimeters
    private static boolean matchTwoCharDelimeter(String expression, int start, String[] delimeters)
    {
        if(start >= expression.length()-1)
            return false;
        for(String delim:delimeters)
        {
            if(expression.substring(start, start+2).equals(delim))
            {
                return true;
            }
        }
        return false;
    }
}
