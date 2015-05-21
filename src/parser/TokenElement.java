/*
 *  Spreadsheet by Madhawa

 
 */
package parser;

import datatypes.DataType;
import functions.AbstractFunction;

/**
 *
 * @author 130614N
 */
//Represents an unparsed token in an parsed expression. call getDataType to parse this token.
public final class TokenElement implements ExpressionElement, solver.DataTypeElement
{
    private String token;
    
    //This reference is required for evaluating cell references
    private parser.Parser associatedParser = null;
    
    public TokenElement(String token, parser.Parser parser)
    {
        this.associatedParser = parser;
        this.token = token;
    }
    
    
    @Override
    public String getStringValue()
    {
        return this.token;
    }
    @Override
    public void setStringValue(String newToken)
    {
        this.token = newToken;
    }
    // creates and instance of the function associated with the token. returns null if no such function can be associated.
    public AbstractFunction getTokenFunction()
    {
       return Parser.tryParseFunctionName(token);
    }
    //ensure that the token represents a data type before calling this function
    @Override
    public DataType getDataType() throws exceptions.SpreadsheetException{
       
        return associatedParser.parseTokenString(token);
    }

}
