/*
 *  Spreadsheet by Madhawa

 
 */
package parser;

/**
 *
 * @author Madhawa
 */
/*
    Return type of parser.identifyTokenAt()
*/
public final class TokenIdentificationResult {
    private ExpressionElement observedElement;
    private int startIndex;
    private int endIndex;
    
    public TokenIdentificationResult(ExpressionElement observedElement, int startIndex, int endIndex)
    {
        this.observedElement = observedElement;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
    
    public ExpressionElement getObservedElement()
    {
        return observedElement;
    }
    //index at which element start to appear in expression
    public int getStartIndex()
    {
        return startIndex;
    }
    //index at which element end in expression. if expression is incomplete, this would be the string.size()-1
    public int getEndIndex()
    {
        return endIndex;
    }
}
