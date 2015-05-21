/*
 *  Spreadsheet by Madhawa

 
 */
package parser;

/**
 *
 * @author 130614N
 */
/*
    Parent interface of Token Element and delimeter element
    Represents a token in an expression
*/
public interface ExpressionElement
{
    public abstract String getStringValue();
    public abstract void setStringValue(String newValue);
}