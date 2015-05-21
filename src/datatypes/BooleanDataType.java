/*
 *  Spreadsheet by Madhawa

 
 */
package datatypes;

import exceptions.InvalidOperationException;
import exceptions.TypeErrorException;
import exceptions.SpreadsheetException;



/**
 *
 * @author Madhawa
 */

/*
    Represents boolean data types. 
    The add function behaves as 'or' operation.
    The multiply function behaves as 'and' operation
    The negate function behaves as 'not' operation
    The power function behaves as 'xor' operation

    String representation - "TRUE" for true, "FALSE" for false

    The suffix "DataType" to avoid confusions with JAVA SE Data Types
*/
public class BooleanDataType extends ValueDataType {

    private  boolean value; //the internal value
    //Constructors
    public BooleanDataType()
    {
        value = false;
    }
    public BooleanDataType(boolean value)
    {
        this.value=value;
    }
    public boolean getValue()
    {
        return value;
    }
    
    
    @Override
    public DataType add(ValueDataType value2) throws TypeErrorException { //OR GATE
        //Validation
        if(value2 instanceof BooleanDataType)
        {
            //Passed
            BooleanDataType v2 = (BooleanDataType)value2;
            
            BooleanDataType result = new BooleanDataType(this.value | v2.getValue());
            return result;
            
        }
        //Failed
        throw new TypeErrorException();
    }

    //Subtraction is not supported
    @Override
    public DataType subtract(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    @Override
    public DataType negate() { //NOT GATE
        return new BooleanDataType(!value); 
    }

    @Override
    public DataType multiply(ValueDataType value2) throws TypeErrorException { //AND GATE
        //Validation
        if(value2 instanceof BooleanDataType)
        {
            //Pass
            BooleanDataType v2 = (BooleanDataType)value2;
            
            BooleanDataType result = new BooleanDataType(this.value & v2.getValue());
            return result;
            
        }
        //Fail
        throw new TypeErrorException();
    }

    //Division is not supported
    @Override
    public DataType divide(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    //How it should appear in the cell of spreadsheet
    @Override
    public String getCellText(cellformatting.AbstractCellFormat cellformat) {
        if(cellformat == null)
        {
            if(value == false)
            {
                return "FALSE";
            }
            return "TRUE";
        }
        return cellformat.getFormattedText(this);
    }
    
    

    //Not Supported
    @Override
    public DataType isGreaterThan(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    //Not Supported
    @Override
    public DataType isGreaterThanOrEqual(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    /*
    Both data types should also match. 1 is not equal to TRUE.
    */
    @Override
    public DataType isEqual(ValueDataType value2) throws TypeErrorException {
        //Validation
        if(value2 instanceof BooleanDataType)
        {
            //Pass
            BooleanDataType v2=(BooleanDataType)value2;
            if(v2.getValue() == value)
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    /*
    Both values should be boolean. 
    TRUE if the boolean values are unequal.
    FALSE if both values are equal
    */
    @Override
    public DataType isNotEqual(ValueDataType value2) throws TypeErrorException{
        //Validation
        if(value2 instanceof BooleanDataType)
        {
            //Pass
            BooleanDataType v2=(BooleanDataType)value2;
            if(v2.getValue() != value)
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    //Unsupported
    @Override
    public DataType isLessThan(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    //Unsupported
    @Override
    public DataType isLessThanOrEqual(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    //pow function (^) is used to represent xor operation
    @Override
    public DataType pow(ValueDataType value2) throws TypeErrorException{
        //Validation
        if(value2 instanceof BooleanDataType)
        {
            //Pass
            BooleanDataType v2 = (BooleanDataType)value2;
            
            BooleanDataType result = new BooleanDataType(this.value ^ v2.getValue());
            return result;
            
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType absoluteValue() throws SpreadsheetException {
        return new BooleanDataType(this.value);
    }
    
}
