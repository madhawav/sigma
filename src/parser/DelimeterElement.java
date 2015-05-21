/*
 *  Spreadsheet by Madhawa

 
 */
package parser;

import solver.MathSymbol;

/**
 *
 * @author 130614N
 */
/*
    An expression consists of tokens and delemeters. 
        Delemeters are the seperators which appear in an expression. These include operators, brackets, commas and other symbols
    Represents a delimeter used by the tokenizer.
    These mostly includes math symbols
*/
public final class DelimeterElement implements ExpressionElement
{
    private String delimeter;
    public DelimeterElement(String delimeter)
    {
        this.delimeter = delimeter;
    }
    @Override
    public String getStringValue()
    {
        return this.delimeter;
    }
    @Override
    public void setStringValue(String newDelimeter)
    {
        this.delimeter = newDelimeter;
    }
    
    public MathSymbol getSymbol() //create and return the associated MathSymbol for this delimeter
    {
        return MathSymbol.parse(delimeter);
    }
}