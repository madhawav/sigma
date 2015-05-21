/*
 *  Spreadsheet by Madhawa

 
 */

package datatypes;

import exceptions.InvalidOperationException;
import exceptions.TypeErrorException;
import exceptions.DivZeroException;
import exceptions.SpreadsheetException;
import java.math.BigDecimal;
import java.math.RoundingMode;


/**
 *
 * @author 130614N
 */
/*
    Represents currency figures.
    The supported currency prefixes are Rs and $
    Inter-conversion between currencies are not supported.
        Hence, operations between two different types of currencies are not supported
    Addition and subtraction can be done between two currencies of same type
    Multiplication can be done between currency and Decimal number type
    Division can be done between currency-decimal and currency-currency
    Powers not supported
    
    When parsing, currency type is recognized by the prefix.

    BigDecimal is used to hold the value internal to ensure maximum precision

    The suffix "DataType" to avoid confusions with JAVA SE Data Types
*/

public class CurrencyDataType extends ValueDataType {
    private BigDecimal value;
    private String currencyPrefix = "Rs";
    
    public CurrencyDataType()
    {
        value = new BigDecimal(0);
        
    }
    //get the unit of currency
    public String getCurrencyPrefix()
    {
        return this.currencyPrefix;
    }
    //set the unit of currency
    public void setCurrencyPrefix(String newPrefix)
    {
        this.currencyPrefix = newPrefix;
    }
    
    public BigDecimal getValue()
    {
        return value;
    }
    public CurrencyDataType(BigDecimal value)
    {
        //We need precision upto two decimal places only. 
        //Conventional round-off strategy
        this.value = value.setScale(2,RoundingMode.HALF_UP);        
    }
     public CurrencyDataType(BigDecimal value, String currencyPrefix)
    {
        this(value);     
        this.currencyPrefix = currencyPrefix;
    }
    @Override
    public DataType add(ValueDataType value2) throws TypeErrorException {
        //validation
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2 = (CurrencyDataType)value2;
            
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            
            //create the resulting object
            CurrencyDataType result = new CurrencyDataType(this.value.add(v2.getValue()));
            result.setCurrencyPrefix(currencyPrefix);
            return result;
            
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType subtract(ValueDataType value2) throws TypeErrorException {
        //validation
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2 = (CurrencyDataType)value2;
            
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            
            //create the resulting object
            CurrencyDataType result = new CurrencyDataType(this.value.subtract(v2.getValue()));
            result.setCurrencyPrefix(currencyPrefix);
            
            return result;
            
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType negate() {
        
         return new CurrencyDataType(value.negate(),this.currencyPrefix);
    }

    //only between currency and decimal numbers
    @Override
    public DataType multiply(ValueDataType value2) throws TypeErrorException{
        //validation
        if(value2 instanceof DecimalNumberDataType)
        {
            DecimalNumberDataType v2 = (DecimalNumberDataType)value2;
            
            //create the resulting object
            CurrencyDataType result = new CurrencyDataType(this.value.multiply(v2.getValue()));
            return result;
            
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType divide(ValueDataType value2) throws exceptions.DivZeroException, TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //Currency/Decimal gives a currency answer
            DecimalNumberDataType v2 = (DecimalNumberDataType)value2;
            if(v2.getValue().compareTo(BigDecimal.ZERO) == 0)
                throw new DivZeroException(); //DivisionByZero
            
            //create the resulting object
            CurrencyDataType result = new CurrencyDataType(this.value.divide(v2.getValue(),2,RoundingMode.HALF_UP));
            return result;
            
        } 
        else if(value2 instanceof CurrencyDataType)
        {
            //Currency/Currency gives a decimal number result
            CurrencyDataType v2 = (CurrencyDataType)value2;
            if(v2.getValue().compareTo(BigDecimal.ZERO) == 0)
                throw new DivZeroException(); //DivisionByZero
            
            //create the resulting object
            DecimalNumberDataType result = new DecimalNumberDataType(this.value.divide(v2.getValue(),2,RoundingMode.HALF_UP));
            return result;
            
        }   
        throw new TypeErrorException();
    }

    //Value to be appeared on cell
    @Override
    public String getCellText(cellformatting.AbstractCellFormat cellFormat) {
        if(cellFormat == null)
            return currencyPrefix + value.toPlainString(); 
        return cellFormat.getFormattedText(this);
    }

    
     @Override
    public DataType isGreaterThan(ValueDataType value2) throws TypeErrorException {
        //Validation
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2=(CurrencyDataType)value2;
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            
            //Validations passed. now Compare.
            if(value.compareTo(v2.getValue()) > 0)
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isGreaterThanOrEqual(ValueDataType value2) throws TypeErrorException {
        //validations
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2=(CurrencyDataType)value2;
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            
            //Validations passed. now compare.
            int compResult = value.compareTo(v2.getValue());
            if(compResult > 0 || compResult == 0 )
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isEqual(ValueDataType value2) throws TypeErrorException {
        //Validation
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2=(CurrencyDataType)value2;
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            
            //Validations passed. now compare.
            if(value.compareTo(v2.getValue()) == 0)
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isNotEqual(ValueDataType value2) throws TypeErrorException {
        //Validation
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2=(CurrencyDataType)value2;
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            //Validations passed. now compare.
            if(value.compareTo(v2.getValue()) != 0)
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isLessThan(ValueDataType value2) throws TypeErrorException {
        //Validations
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2=(CurrencyDataType)value2;
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            
            //Validations passed. now compare.
            if(value.compareTo(v2.getValue()) < 0)
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isLessThanOrEqual(ValueDataType value2) throws TypeErrorException {
        //Validations
        if(value2 instanceof CurrencyDataType)
        {
            CurrencyDataType v2=(CurrencyDataType)value2;
            //If the two currencies are different, we dont know the exchange rates to calculate
            if(!(v2.getCurrencyPrefix().equals(getCurrencyPrefix())))
                throw new TypeErrorException();
            
            //Validations passed. now compare.
            int compResult = value.compareTo(v2.getValue());
            if(compResult < 0 || compResult == 0 )
            {
                return new BooleanDataType(true);
            }
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    //Not supported
    @Override
    public DataType pow(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    @Override
    public DataType absoluteValue() throws SpreadsheetException {
        CurrencyDataType result = new CurrencyDataType(value.abs());
        result.setCurrencyPrefix(getCurrencyPrefix());
        return result;
        
    }
    
}
