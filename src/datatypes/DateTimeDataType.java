/*
 *  Spreadsheet by Madhawa

 
 */
package datatypes;

import exceptions.InvalidOperationException;
import exceptions.SpreadsheetException;
import exceptions.TypeErrorException;
import java.math.BigDecimal;

import java.math.RoundingMode;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 *
 * @author 130614N
 */
/*
    Represents a date-time combination (Absolute Date).


    Internally uses Javas GregotianCalendar to handle date-time related operations
    Addition and Subtraction can be done with decimal numbers on the basis one unit equals one day
        Fractions of one unit will represents fractions of a day
           Eg - 0.5 = 12 hours
    Multiplication and division not allowed
    Power, Absolute Value and negation operations not supported
    Comparison operators are supported on the basis, 
        a<b means a comes before b
    
*/
public class DateTimeDataType extends ValueDataType {

    //DateTime may represent a exact date. Decimal numbers can be added or substracted on the basis that 1 represents one day.
    private GregorianCalendar value;
   
    public DateTimeDataType() {
        value = new GregorianCalendar(0, 0, 0, 0, 0, 0);
    }

    public DateTimeDataType(GregorianCalendar value) {
        this.value = value;
    }

    //Returns internal gregorian calendar object
    public GregorianCalendar getValue() {
        return value;
    }


    //Adds two date time objects or a date time object to a decimal number
    @Override
    public DataType add(ValueDataType value2) throws TypeErrorException {
        //Date + Date
        if (value2 instanceof DateTimeDataType) {
            DateTimeDataType v2 = (DateTimeDataType) value2;
            GregorianCalendar cal2 = v2.getValue(); //2nd value as calendar
            
            GregorianCalendar cal1 = (GregorianCalendar) value.clone(); //first value as calendar

            //do additions
            cal1.add(Calendar.SECOND, cal2.get(Calendar.SECOND));
            cal1.add(Calendar.MINUTE, cal2.get(Calendar.MINUTE));
            cal1.add(Calendar.HOUR, cal2.get(Calendar.HOUR));

            cal1.add(Calendar.DAY_OF_MONTH, cal2.get(Calendar.DAY_OF_MONTH));
            cal1.add(Calendar.MONTH, cal2.get(Calendar.MONTH));
            cal1.add(Calendar.YEAR, cal2.get(Calendar.YEAR));

            DateTimeDataType result = new DateTimeDataType(cal1);
            return result;

        }
        //Date + Decimal number
        else if (value2 instanceof DecimalNumberDataType) //behaves similar to ms excel.whole number = day
        {
            //V2 is a decimal number here
            DecimalNumberDataType v2 = (DecimalNumberDataType) value2;
            BigDecimal days = v2.getValue().setScale(0, RoundingMode.FLOOR); //retrieves number of days represented by v2
            BigDecimal hours = v2.getValue().subtract(days); //retrieves number of hours (in a day) represented by v2

            GregorianCalendar cal1 = (GregorianCalendar) value.clone(); //gregorian calendar of v1

            //Do additions
            cal1.add(Calendar.DAY_OF_YEAR, days.intValue());
            cal1.add(Calendar.SECOND, (int) (hours.floatValue() * 24.0 * 3600.0));

            //returns the result
            DateTimeDataType result = new DateTimeDataType(cal1);
            return result;

        }
        throw new TypeErrorException();

    }

    /*Returns the difference between two date-time objects as a DateTime object
            or
    Subtract a 'value2' number of days from value1*/
    @Override
    public DataType subtract(ValueDataType value2) throws TypeErrorException {
        if (value2 instanceof DateTimeDataType) {
            //Difference between two date-time objects
            
            DateTimeDataType v2 = (DateTimeDataType) value2;
            GregorianCalendar cal2 = v2.getValue(); //v2 as a gregorian calendar
            GregorianCalendar cal1 = (GregorianCalendar) value.clone(); //v1 as a gregorianCalendar

            //Do subtractions
            cal1.add(Calendar.SECOND, -cal2.get(Calendar.SECOND));
            cal1.add(Calendar.MINUTE, -cal2.get(Calendar.MINUTE));
            cal1.add(Calendar.HOUR, -cal2.get(Calendar.HOUR));

            cal1.add(Calendar.DAY_OF_MONTH, -cal2.get(Calendar.DAY_OF_MONTH));
            cal1.add(Calendar.MONTH, -cal2.get(Calendar.MONTH));
            cal1.add(Calendar.YEAR, -cal2.get(Calendar.YEAR));

            //returns the result
            DateTimeDataType result = new DateTimeDataType(cal1);
            return result;

        } else if (value2 instanceof DecimalNumberDataType) //behaves similar to ms excel.whole number = day
        {
            //Subtract a v2 number of days from v1
            DecimalNumberDataType v2 = (DecimalNumberDataType) value2;
            //Identify number of days represented by v2
            BigDecimal days = v2.getValue().setScale(0, RoundingMode.FLOOR);
            //Identify hours per day represented by v2
            BigDecimal hours = v2.getValue().subtract(days);

            GregorianCalendar cal1 = (GregorianCalendar) value.clone(); //v1 as a gergorian calendar

            //Do subtractions
            cal1.add(Calendar.DAY_OF_YEAR, -days.intValue());
            cal1.add(Calendar.SECOND, -(int) (hours.floatValue() * 24.0 * 3600.0));

            //return the result
            DateTimeDataType result = new DateTimeDataType(cal1);
            return result;

        }
        throw new TypeErrorException();
    }

    //Not supported by DateTime
    @Override
    public DataType negate() throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    //Not supported by DateTime
    @Override
    public DataType multiply(ValueDataType value2) throws InvalidOperationException {

        throw new InvalidOperationException();
    }

    //Not supported by DateTime
    @Override
    public DataType divide(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    //Retrieves the text version of date-time, as it should appear in cell
    @Override
    public String getCellText(cellformatting.AbstractCellFormat cellFormat) {
        if(cellFormat == null)
            return String.valueOf(value.get(Calendar.MONTH) + 1) + "/"
                    + String.valueOf(value.get(Calendar.DAY_OF_MONTH))
                    + "/" + String.valueOf(value.get(Calendar.YEAR))
                    + " " + String.valueOf(value.get(Calendar.HOUR_OF_DAY))
                    + ":" + String.valueOf(value.get(Calendar.MINUTE))
                    + ":" + String.valueOf(value.get(Calendar.SECOND));

        return cellFormat.getFormattedText(this);
    }
    

    //
    @Override
    public DataType isGreaterThan(ValueDataType value2) throws TypeErrorException {
        if (value2 instanceof DateTimeDataType) {
            DateTimeDataType v2 = (DateTimeDataType) value2; //identify v2 as Date-Time
            //Do comparison
            int compResult = value.compareTo(v2.getValue());
            
            if (compResult > 0) {
                //Comparison passed
                return new BooleanDataType(true);
            }
            //Comparison failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isGreaterThanOrEqual(ValueDataType value2) throws TypeErrorException {
        if (value2 instanceof DateTimeDataType) {
            DateTimeDataType v2 = (DateTimeDataType) value2; //identify v2 as a Date-Time
            //Do comparison
            int compResult = value.compareTo(v2.getValue());
            
            if (compResult > 0 || compResult == 0) {
                //Passed
                return new BooleanDataType(true);
            }
            //Comparison failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    //Compares two dates to be equal. Therefore, the types should match. Otherwise throw TypeError exception.
    @Override
    public DataType isEqual(ValueDataType value2) throws TypeErrorException {
        //Do the types match?
        if (value2 instanceof DateTimeDataType) {
            //Identify v2 as dateTime
            DateTimeDataType v2 = (DateTimeDataType) value2;
           //Do comparison
            int compResult = value.compareTo(v2.getValue());
            if (compResult == 0) {
                //comparison passed
                return new BooleanDataType(true);
            }
            //comparision failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isNotEqual(ValueDataType value2) throws TypeErrorException {
        if (value2 instanceof DateTimeDataType) {
            //Identify v2 as date time
            DateTimeDataType v2 = (DateTimeDataType) value2;
            //do comparision
            int compResult = value.compareTo(v2.getValue());
            if (compResult != 0) {
                //comparison passed
                return new BooleanDataType(true);
            }
            //comparison failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isLessThan(ValueDataType value2) throws TypeErrorException {
        if (value2 instanceof DateTimeDataType) {
            //identify v2 as date time
            DateTimeDataType v2 = (DateTimeDataType) value2;
            
            //do comparison
            int compResult = value.compareTo(v2.getValue());
            if (compResult < 0) {
                //comparison passed
                return new BooleanDataType(true);
            }
            //comparison failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    @Override
    public DataType isLessThanOrEqual(ValueDataType value2) throws TypeErrorException {
        if (value2 instanceof DateTimeDataType) {
            //identify v2 as date time
            DateTimeDataType v2 = (DateTimeDataType) value2;
            //do comparison
            int compResult = value.compareTo(v2.getValue());
            if (compResult < 0 || compResult == 0) {
                return new BooleanDataType(true);
                //comparison passed
            }
            //comparison failed
            return new BooleanDataType(false);
        }
        throw new TypeErrorException();
    }

    //Not Supported
    @Override
    public DataType pow(ValueDataType value2) throws InvalidOperationException {
        throw new InvalidOperationException();
    }

    //Not Supported
    @Override
    public DataType absoluteValue() throws SpreadsheetException {
        return new DateTimeDataType((GregorianCalendar) this.getValue().clone());
    }

}
