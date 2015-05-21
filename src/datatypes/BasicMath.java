/*
 *  Spreadsheet by Madhawa

 
 */
package datatypes;

import exceptions.SpreadsheetException;
import exceptions.TypeErrorException;


/**
 *
 * @author Madhawa
 */

 /*
    This class holds the basic maths operations the data-types can undergo. 
    The implementations of these functions are held at individual classes of relavent data types. (class corresponding to first parameter)
    This is done to reduce the complexity of catering many combinations of operands in a single code file.
    First, each of these functions would validate its corresponding input values. Then it will return resulting data type of corresponding operation.
    if types mismatch, then TypeError will be returned.
    
    The actual implementations of these functions are found at the corresponding data types.
    More narrower validations will be undertaken there to determine what kind of operation is requested. 
        Eg: Are we talking about 2+3 or TRUE+FALSE
            Date + Decimal
    
    */
    


public final class BasicMath {
  
    //disable constructor
   private BasicMath()
   {
       
   }
    
    /*
    Power operation
        returns v1 to the power v2
    */
    public static datatypes.DataType pow(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //Validation
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //Pass
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.pow((ValueDataType)v2);
        }
        else
        {
            //Fail
            throw new TypeErrorException();
        }
    }
    
    /*
    Addition operation
        returns v1 + v2 
    */
    public static datatypes.DataType add(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //Validation
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //Pass
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.add((ValueDataType)v2);
        }
        else
        {
            //Fail
            throw new TypeErrorException();
        }
    }
    
    /*
    Subtraction operation
        returns v1 - v2
    */
    public static datatypes.DataType subtract(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //Validation
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //Pass
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.subtract((ValueDataType)v2);
        }
        else
        {
            //Fail
            throw new TypeErrorException();
        }
    }
    /*
    Multiplication operation
        returns v1 * v2
    */
    public static datatypes.DataType multiply(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //Validation
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //Pass
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.multiply((ValueDataType)v2);
        }
        else
        {
            //Fail
            throw new TypeErrorException();

        }
    }
    /*
    Division operation
        returns v1 / v2
    */
    public static datatypes.DataType divide(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //Validation
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //Pass
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.divide((ValueDataType)v2);
        }
        else
        {
            //Fail
            throw new TypeErrorException();
        }
    }
    
    //negation operation
    public static datatypes.DataType negate(datatypes.DataType v1) throws exceptions.SpreadsheetException
    {
        if(v1 instanceof datatypes.ValueDataType )
        {
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.negate();
        }
        else
        {
            //fail
            throw new TypeErrorException();
        }
    }
    
    //logical operations
    public static datatypes.DataType isEqual(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //check for type compatibility
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //do comparison
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.isEqual((ValueDataType)v2);
        }
        else
        {
            //type mismatch
            throw new TypeErrorException();
        }
    }
    
    public static datatypes.DataType isNotEqual(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //check for types compatibility
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //do comparison
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.isNotEqual((ValueDataType)v2);
        }
        else
        {
            //types mismatch
            throw new TypeErrorException();
        }
    }
    
    public static datatypes.DataType isGreaterThan(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //check for types compatibility
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //do comparison
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.isGreaterThan((ValueDataType)v2);
        }
        else
        {
            //types mismatch
            throw new TypeErrorException();
        }
    }
    public static datatypes.DataType isGreaterThanOrEqual(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //check for types compatibility
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //do comparison
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.isGreaterThanOrEqual((ValueDataType)v2);
        }
        else
        {
            //types mismatch
            throw new TypeErrorException();
        }
    }
    public static datatypes.DataType isLessThan(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //check for types compatibility
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //do comparison
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.isLessThan((ValueDataType)v2);
        }
        else
        {
            //types mismatch
            throw new TypeErrorException();
        }
    }
    public static datatypes.DataType isLessThanOrEqual(datatypes.DataType v1, datatypes.DataType v2) throws exceptions.SpreadsheetException
    {
        //check for types compatibility
        if(v1 instanceof datatypes.ValueDataType && v2 instanceof datatypes.ValueDataType)
        {
            //do comparison
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.isLessThanOrEqual((ValueDataType)v2);
        }
        else
        {
            //types mismatch
            throw new TypeErrorException();
        }
    }
    //retrieves the absolute value of v1
    public static datatypes.DataType AbsoluteValue(datatypes.DataType v1) throws SpreadsheetException
    {
        //check for compatibility
        if(v1 instanceof datatypes.ValueDataType)
        {
            //do opeation
            datatypes.ValueDataType val1 = (datatypes.ValueDataType)v1;
            return val1.absoluteValue();
        }
        else
            throw new TypeErrorException();
    }
    
    
    
}
