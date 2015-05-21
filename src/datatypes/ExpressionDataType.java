/*
 *  Spreadsheet by Madhawa

 
 */
package datatypes;


import parser.ExpressionElement;
import exceptions.SpreadsheetException;

import java.util.ArrayList;

import java.util.Queue;


/**
 *
 * @author Madhawa
 */

/*
    Used to represent mathematical expressions. 
        eg: = 2 +(3 * 2)
    Uses the packages parser and expression evaluator to solve the given expression

*/

public class ExpressionDataType extends DataType {

    private String expression;
    
    private ArrayList<ExpressionElement> expressionElements; //represents the individual elements of the expression
    
    
    public ExpressionDataType()
    {
        expression = "";
        expressionElements = new ArrayList<>();
        
        
    }
    //returns the expression elements in reverse polish notation
    public ArrayList<ExpressionElement> getExpressionElements()
    {
        return this.expressionElements;
    }
    
    
    
    public ExpressionDataType(String expression,parser.Parser associatedParser)
    {
        
        this();
        this.expression = expression;
        //Parse the expression and store tokens
        this.expressionElements = parser.ExpressionTokenizer.tokenizeExpressionElements(expression,associatedParser);
        
        
    }
    
    //Evaluates the expression and return the answer
    public DataType getSolvedDataType() throws SpreadsheetException
    {
               
        //Convert to RPN
        Queue<ExpressionElement> reversePolishExpressionElements= solver.ExpressionEvaluator.getReversedPolishedNotation(expressionElements);
       
        //Solve
        DataType result = solver.ExpressionEvaluator.solveReversePolishNotation(reversePolishExpressionElements);
        
        return result;
    }
    
    @Override
    public String getCellText(cellformatting.AbstractCellFormat cellFormat) {
        //Returns the expression to be appeared on the cell
        try
        {
            return getSolvedDataType().getCellText(cellFormat);
        }
        catch(SpreadsheetException ex)
        {
            return ex.getMessage();
        }
    }

    
    public String getExpression() {
        return expression;
    }
    
}
