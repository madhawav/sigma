/*
 *  Spreadsheet by Madhawa

 
 */

package datatypes;

import exceptions.TypeErrorException;
import exceptions.DivZeroException;
import exceptions.SpreadsheetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *
 * @author 130614N
 */
/*
    Represents a decimal number.
    This includes integers and fractions both. Including negative numbers.
    BigDecimal is used internally to hold the numbers as accurate as possible.

    Addition and subtraction compatible with DateTime on the basis
        one unit = one day
    Multiplication compatible with currency
    
    All comparison operations supported with decimal numbers
    Absolute, negate and pow supported
*/
public class DecimalNumberDataType extends ValueDataType{
    private BigDecimal value;
    public DecimalNumberDataType()
    {
        value = new BigDecimal(0);
    }
    
    //returns internal BigDecimal value
    public BigDecimal getValue()
    {
        return value;
    }
    
    public DecimalNumberDataType(BigDecimal value)
    {
        this.value = value;
    }
    
    //Add two numbers or add a number to a DateTime
    
    @Override
    public DataType add(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //Number, Number addition
            DecimalNumberDataType v2 = (DecimalNumberDataType)value2;
            
            DecimalNumberDataType result = new DecimalNumberDataType(this.value.add(v2.getValue()));
            return result;
            
        }
        else if(value2 instanceof DateTimeDataType) //behaves similar to ms excel.whole number = day
        {
            //Number-DateTime addition
            //Identifies v2 as a date-time
            DateTimeDataType v2 = (DateTimeDataType)value2;
            //Number of days represented by v2
            BigDecimal days = getValue().setScale(0,RoundingMode.FLOOR);
            //Hours per day represented by v2
            BigDecimal hours = getValue().subtract(days);
            //convert v2 to a gregorian calendar
            GregorianCalendar cal2=(GregorianCalendar)v2.getValue().clone();
            
            //do addition
            cal2.add(Calendar.DAY_OF_YEAR, days.intValue());
            cal2.add(Calendar.SECOND, (int)(hours.floatValue() * 24.0 * 3600.0));
            
                       
            DateTimeDataType result = new DateTimeDataType(cal2);
            return result;
            
        }
        throw new TypeErrorException();
        
    }

    //Subtracts two numbers or subtract a date-time from a number
    @Override
    public DataType subtract(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //Number-Number subtraction
            DecimalNumberDataType v2 = (DecimalNumberDataType)value2;
            
            DecimalNumberDataType result = new DecimalNumberDataType(this.value.subtract(v2.getValue()));
            return result;
            
        }
        else if(value2 instanceof DateTimeDataType) //behaves similar to ms excel.whole number = day
        {
            //Number-Date subtraction
            //Identify v2 as a date
            DateTimeDataType v2 = (DateTimeDataType)value2;
            //Obtain number of days in v2
            BigDecimal days = getValue().setScale(0,RoundingMode.FLOOR);
            //Obtain hours per day in v2
            BigDecimal hours = getValue().subtract(days);
            
            //recognize v2 as gregorian calendar
            GregorianCalendar cal2 = (GregorianCalendar)v2.getValue().clone();
            
            //Do subtractions
            cal2.add(Calendar.DAY_OF_YEAR, -days.intValue());
            cal2.add(Calendar.SECOND, -(int)(hours.floatValue() * 24.0 * 3600.0));
            
            //Returns results
            DateTimeDataType result = new DateTimeDataType(cal2);
            return result;
            
        }
        throw new TypeErrorException();
    }

    @Override
    //Negates decimal number
    public DataType negate() {
        return new DecimalNumberDataType(value.negate()); 
    }
    
    //Multiplies two decimal numbers or multiply a decimal number and currency
    @Override
    public DataType multiply(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //Number * Number
            //Identifies v2 as a decimal number
            DecimalNumberDataType v2 = (DecimalNumberDataType)value2;
            //Generate results
            DecimalNumberDataType result = new DecimalNumberDataType(this.value.multiply(v2.getValue()));
            return result;
            
        }
        else if(value2 instanceof CurrencyDataType)
        {
            //Recognize v2 as currency data type
            CurrencyDataType v2 = (CurrencyDataType)value2;
            
            //Do multiplication and generate results
            CurrencyDataType result = new CurrencyDataType(this.value.multiply(v2.getValue()),v2.getCurrencyPrefix());
            
            return result;
            
        }
        //Incompatible types
        throw new TypeErrorException();
    }

    //Divides a decimal number by a decimal number
    @Override
    public DataType divide(ValueDataType value2) throws exceptions.DivZeroException,TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //Identifies v2 as a decimal number
            DecimalNumberDataType v2 = (DecimalNumberDataType)value2;
            
            //Check for division by 0
            if(v2.getValue().compareTo(BigDecimal.ZERO) == 0)
                throw new DivZeroException(); //Throw devision by zero exception
            
            //Generate results
            DecimalNumberDataType result = new DecimalNumberDataType(this.value.divide(v2.getValue(),10,RoundingMode.HALF_UP));
            return result;
            
        }
        //Unsupported types
        throw new TypeErrorException();
    }

    @Override
    public String getCellText(cellformatting.AbstractCellFormat format) {
        if(format == null)
            return value.toPlainString(); 
        else return format.getFormattedText(this);
    }

   
    
    //comparison methods
    @Override
    public DataType isGreaterThan(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //recognizes v2 as decimal number
            DecimalNumberDataType v2=(DecimalNumberDataType)value2;
            //do comparison
            if(value.compareTo(v2.getValue()) > 0)
            {
                //passed
                return new BooleanDataType(true);
            }
            //failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isGreaterThanOrEqual(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //recoznizes v2 as decimal number
            DecimalNumberDataType v2=(DecimalNumberDataType)value2;
            //do comparison
            int compResult = value.compareTo(v2.getValue());
            if(compResult > 0 || compResult == 0 )
            {
                //passed
                return new BooleanDataType(true);
            }
            //failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isEqual(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //recognizes v2 as decimal number
            DecimalNumberDataType v2=(DecimalNumberDataType)value2;
            
            //do comparison
            if(value.compareTo(v2.getValue()) == 0)
            {
                //passed
                return new BooleanDataType(true);
            }
            //failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isNotEqual(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //recognizes v2 as a decimal number
            DecimalNumberDataType v2=(DecimalNumberDataType)value2;
            //do comparison
            if(value.compareTo(v2.getValue()) != 0)
            {
                //passed
                return new BooleanDataType(true);
            }
            //failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isLessThan(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //recongizes v2 as a decimal number
            DecimalNumberDataType v2=(DecimalNumberDataType)value2;
            
            //do comparison
            if(value.compareTo(v2.getValue()) < 0)
            {
                //passed
                return new BooleanDataType(true);
            }
            //failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isLessThanOrEqual(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            //recognizes v2 as a decimal number
            DecimalNumberDataType v2=(DecimalNumberDataType)value2;
            //do comparison
            int compResult = value.compareTo(v2.getValue());
            
            if(compResult < 0 || compResult == 0 )
            {
                //passed
                return new BooleanDataType(true);
            }
            //failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType pow(ValueDataType value2) throws TypeErrorException {
        if(value2 instanceof DecimalNumberDataType)
        {
            DecimalNumberDataType v2=(DecimalNumberDataType)value2;
            //for integer powers, the pow function of bigdecimal will be used
            if(v2.getValue().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0)
            {
                //its an integer power
                DecimalNumberDataType result = new DecimalNumberDataType(this.value.pow(v2.getValue().intValueExact()));
                return result;
            }
            else
            {
                //we have no choice but to convert the figures into double preciison
                double base = this.value.doubleValue();
                double power = v2.getValue().doubleValue();
                return new DecimalNumberDataType(new BigDecimal(Math.pow(base, power))); 
                
            }
            
            
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType absoluteValue() throws SpreadsheetException {
        return new DecimalNumberDataType(this.getValue().abs());
    }
}
