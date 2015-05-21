/*
 *  Spreadsheet by Madhawa

 
 */
package solver;

/**
 *
 * @author 130614N
 */
public abstract class MathSymbol {
    //represents mathematical symbols (excluding numbers and .)
    public abstract String asString(); //returns the string representation of the math symbol
    
    public static MathSymbol parse(String parseText)
    {
        //Is it an operator? +-*^/
        MathSymbol output = Operator.tryParse(parseText);
        if(output!=null)
            return output;
        String trimmedParseText = parseText.trim();
        
        //Its not an operator. Lets check for other possibilities
        
        if(trimmedParseText.equals("("))
            return new LeftParanthesis();
        if(trimmedParseText.equals(")"))
            return new RightParanthesis();
        if(trimmedParseText.equals(","))
            return new ParameterSeperator();
        
        //Other types evaluvated here
        
        
        return null;
    }
}
