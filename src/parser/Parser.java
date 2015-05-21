/*
 *  Spreadsheet by Madhawa

 
 */
package parser;

import datatypes.ArrayDataType;

import datatypes.BooleanDataType;
import datatypes.CurrencyDataType;
import datatypes.DataType;
import datatypes.DateTimeDataType;
import datatypes.DecimalNumberDataType;
import datatypes.ExpressionDataType;
import datatypes.LabelDataType;
import exceptions.CircularReferenceException;

import exceptions.SpreadsheetException;
import exceptions.ParseException;
import exceptions.TranslateException;
import functions.Abs;
import functions.AbstractFunction;
import functions.Avg;
import functions.If;
import functions.Max;
import functions.Min;

import functions.Sum;

import spreadsheet.Cell;
import spreadsheet.Spreadsheet;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;


/**
 *
 * @author Madhawa
 */
/*
 Holds set of parsing functions to parse data types and expressions
 */
public final class Parser {

    //supported DateTime parse formats
    private static final String[] supporteDateTimeFormats = {"MM/dd/yyyy hh:mm:ss aa", "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy hh:mm aa", "MM/dd/yyyy HH:mm", "MM/dd/yyyy", "yyyy-MM-dd hh:mm:ss aa", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd hh:mm aa", "yyyy-MM-dd HH:mm", "yyyy-MM-dd", "hh:mm:ss aa", "HH:mm:ss", "hh:mm aa", "HH:mm"};

    //cell provider is used to solve cell references (eg =A1, =A1:A5)
    private CellContentProvider cellProvider = null;

    public CellContentProvider getCellProvider() {
        return cellProvider;
    }

    public void setCellProvider(CellContentProvider cellProvider) {
        this.cellProvider = cellProvider;
    }

    private static final String[] supportedCurrencyPrefixes = {"Rs", "$"}; //Set of supported currency prefixes. 

    //try to parse a cell range to an ArrayDataType
    public ArrayDataType parseCellRange(String expressionText) throws SpreadsheetException {

        String matchText = "[A-Z]+[0-9]+\\:[A-Z]+[0-9]+";
        if (expressionText.matches(matchText)) {
            //expression text is a cell range
            //separate two cell references
            String[] parts = expressionText.split("\\:");
            //start reference
            String startReference = parts[0];
            //sperate start row index and start column index
            int startRowIndex = Integer.parseInt(startReference.split("[A-Z]+")[1]) - 1;
            int startColumnIndex = Spreadsheet.columnNameToIndex(startReference.split("[0-9]+")[0]);

            //end reference
            String endReference = parts[1];
            //sperate end row index and end column index
            int endRowIndex = Integer.parseInt(endReference.split("[A-Z]+")[1]) - 1;
            int endColumnIndex = Spreadsheet.columnNameToIndex(endReference.split("[0-9]+")[0]);

            DataType[][] resultArray = new DataType[endRowIndex - startRowIndex + 1][endColumnIndex - startColumnIndex + 1];

            //fill the array
            for (int rowIndex = startRowIndex; rowIndex <= endRowIndex; rowIndex++) {
                for (int columnIndex = startColumnIndex; columnIndex <= endColumnIndex; columnIndex++) {
                    
                    
                        String cellId = Spreadsheet.indexToColumnName(columnIndex) + String.valueOf(rowIndex + 1);
                        Cell referredCell = cellProvider.getCell(cellId);
                        //Think on what to do if accessed cell is empty
                        //it seems excel see them as zeros
                        //lets try the same logic here
                        DataType result = null;
                        if (referredCell == null) {
                            result = new LabelDataType("");
                        } else {
                            try {
                                result = referredCell.getSolvedCellData();
                            } catch (StackOverflowError stackOf) //incurs due to circular reference
                            {
                                throw new CircularReferenceException();
                            }

                        }
                        resultArray[rowIndex - startRowIndex][columnIndex - startColumnIndex] = result;
                    
                    
                }
            }
            return new ArrayDataType(resultArray);

        }
        throw new ParseException("Unable to parse cell-range");
    }

    //parse a cell reference to a binded datattype object. 
    //the binded data-type will contain the pair of unsolved cell data-type and cell id
    public DataType parseCellReference(String expressionText) throws SpreadsheetException {
        String trimmedText = expressionText.trim();
        Cell referredCell = cellProvider.getCell(trimmedText);
        //Think on what to do if accessed cell is empty
        //it seems excel see them as zeros
        //lets try the same logic here
        if (referredCell == null) {
            if (Cell.validateCellId(trimmedText)) {
                return new LabelDataType("");
            }
            throw new ParseException("Unable to parse cell reference");
        }

        try {
            DataType data = referredCell.getSolvedCellData();
            //reportCellReferenceParsed(trimmedText, data);
            return data;
        } catch (StackOverflowError stackOF) //incurs during a circular reference
        {
            throw new CircularReferenceException();
        }

    }

    /*
     Parser for tokens which appear in expressions
     Eg:
     =2+"Hello"
     in this case, 2 and "Hello" are recognized as DecimalNumber and String by this parser
     Note: This parser should be used to parse tokens of expressions.
     This will be used to recognize the tokens returned from the tokenizer when tokenizing an expression.
     It should not be used to parse cell values (or cell expressions)
     Recognizable DataTypes:
     Decimal Numbers
     Label (strings should be within quotation marks)
     Boolean Figures
     Cell References - By extendedParser
     Other DataTypes are not recognized due to possible confusions when placed in a mathematical expression
     Eg: 1993/05/01 - Is it a date or does that mean 1993 divide by 05 and divide by 01?
     */
    public DataType parseTokenString(String parseText) throws SpreadsheetException {
        String trimmedParseText = parseText.trim();
        if (trimmedParseText.length() == 0) {
            throw new ParseException("Empty Expression");
        }
        if (trimmedParseText.charAt(0) == (char) 34 && trimmedParseText.charAt(trimmedParseText.length() - 1) == (char) 34) {
            return new LabelDataType(trimmedParseText.substring(1, trimmedParseText.length() - 1));
        }
        DataType tryType;
        try {
            return parseDecimalNumber(trimmedParseText);
        } catch (ParseException ex) {
            /*No, it isn't */
        }
        try {
            return parseBoolean(trimmedParseText);
        } catch (ParseException ex) {
            /*No, it isn't */
        }
        try {
            return parseCellRange(parseText);
        } catch (ParseException ex) {
            /*No, it isn't */
        }
        try {
            return parseCellReference(parseText);
        } catch (ParseException ex) {
            /*No, it isn't */
        }
        //Currency DataTypes: consider giving as a value in a new cell. Expressions can be complex and confusing with currency units
        //DataTimes cannot be recognized within expressions. They are confused with division. Therefore we need to use cell reference there.
        throw new ParseException(parseText);
    }

    /*
     This would parse any expression which could appear as a cell expression.
     Tokenizing will be done as and when required.
     Would return an object of DataType according to given parseText
     Recognizable DataTypes
     ExpressionDataType - if begun with = sign.
     DecimalNumber
     DateTime
     Boolean
     Currency
     Strings - without quotation marks
     Parse the labelDataType last. It would be positive to any input. If all other parsing attempts fail, input will be recognized as a label.
     */
    public DataType parseCellExpression(String parseText) {
        String trimmedParseText = parseText.trim();
        //Expression
        try {
            return parseExpression(trimmedParseText);
        } catch (ParseException ex) {
        }
        //Decimal Number
        try {
            return parseDecimalNumber(trimmedParseText);
        } catch (ParseException ex) {
        }
        //Boolean
        try {
            return parseBoolean(trimmedParseText);
        } catch (ParseException ex) {
        }
        //Currency
        try {
            return parseCurrency(trimmedParseText);
        } catch (ParseException ex) {
        }
        //Date-Times
        try {
            return parseDateTime(trimmedParseText);
        } catch (ParseException ex) {
        }
        //Label
        return parseLabel(trimmedParseText);
    }

    //parses a function name to a function. returns null if no function is found.
    public static AbstractFunction tryParseFunctionName(String funcName) {
        String functionName = funcName.toLowerCase();
        //Sum
        if (functionName.equals(Sum.functionName)) {
            return new Sum();
        }

        //Max
        if (functionName.equals(Max.functionName)) {
            return new Max();
        }
        //Min
        if (functionName.equals(Min.functionName)) {
            return new Min();
        }
        //Abs
        if (functionName.equals(Abs.functionName)) {
            return new Abs();
        }
        //If
        if (functionName.equals(If.functionName)) {
            return new If();
        }
        //Avg
        if (funcName.equals(Avg.functionName)) {
            return new Avg();
        }
        return null;

    }

    //attempts to parse parseText to boolean data type
    public static DataType parseBoolean(String parseText) throws ParseException {
        String trimmedParseText = parseText.trim();
        if (trimmedParseText.equals("TRUE")) {
            return new BooleanDataType(true);
        } else if (trimmedParseText.equals("FALSE")) {
            return new BooleanDataType(false);
        }
        throw new ParseException(parseText);
    }

    //attempts to parse the parseText to a currency data type
    public static DataType parseCurrency(String parseText) throws ParseException {
        for (String prefix : supportedCurrencyPrefixes) {
            if (parseText.startsWith(prefix)) {
                String numPart = parseText.substring(prefix.length());
                DataType numPartParsed = parseDecimalNumber(numPart.trim());
                if (!(numPartParsed instanceof DecimalNumberDataType)) {
                    continue; //oops, cant recognize a decimal part. try next type.
                }
                DecimalNumberDataType numPartDecimal = (DecimalNumberDataType) numPartParsed;
                CurrencyDataType result = new CurrencyDataType(numPartDecimal.getValue());
                result.setCurrencyPrefix(prefix);
                return result;
            }
        }
        throw new ParseException(parseText);
    }

    //Parse the given parseText into a DateTime Data Type. Otherwise throw ParseException.
    public static DataType parseDateTime(String parseText) throws exceptions.ParseException {
        for (String format : supporteDateTimeFormats) {
            SimpleDateFormat sdformat = new SimpleDateFormat(format, Locale.US);
            sdformat.setLenient(true); //be strict with rules
            try {
                //try java date parser
                Date date = sdformat.parse(parseText);
                //if everythings ok, convert to gregorian calendar. 
                GregorianCalendar gregCal = new GregorianCalendar();
                gregCal.setTime(date);
                return new DateTimeDataType(gregCal);
            } catch (java.text.ParseException ex) {
                //maybe the next format could be valie. So dont throw yet
            }
        }
        //all the supported date-time formats failed
        throw new ParseException(parseText);
    }

    //parse the parseText to a DecimalNumber data type. Otherwise, throw parse exception
    public static DataType parseDecimalNumber(String parseText) throws ParseException {
        //Check for the standard decimal number format. (Eg 12.53, -43.45)
        if (parseText.matches("-?\\+?\\d+(\\.\\d+)?")) {
            return new DecimalNumberDataType(new BigDecimal(parseText));
        } //Check for in-bracket negative numbers (Eg -43.45)
        else if (parseText.matches("\\(\\d+(\\.\\d+)?\\)")) {
            String valueText = parseText.trim();
            return new DecimalNumberDataType(new BigDecimal(valueText.substring(1, valueText.length() - 1)).negate());
        } else {
            //All failed. Therefore cannot be a decimal number
            throw new ParseException(parseText);
        }
    }

    //Parse the parseText into a Expression data type. Otherwise throw ParseException
    public DataType parseExpression(String parseText) throws ParseException {
        //too short parseText to be an expression
        if (parseText.length() <= 1) {
            throw new ParseException(parseText);
        }
        //Check for preceeding =
        if (parseText.charAt(0) == "=".charAt(0)) {

            return new ExpressionDataType(parseText, this);
        } else {
            //Not an expresion.
            throw new ParseException(parseText);
        }
    }

    //Parse parseText to a Label data type. Any string can be parsed to a label.
    public static DataType parseLabel(String parseText) {
        LabelDataType result = new LabelDataType(parseText);
        return result;
    }

    //Translates a cell range by a given offset
    public static String translateCellRange(String cellRange, int rowOffset, int columnOffset, int thresholdRowIndex, int thresholdColumnIndex) throws TranslateException {
        //separate two cell references
        String[] parts = cellRange.trim().split("\\:");
        //start seoerate two references
        String startReference = parts[0];
        String endReference = parts[1];
        //translate individual reference
        startReference = translateCellId(startReference, rowOffset, columnOffset, thresholdRowIndex, thresholdColumnIndex);
        endReference = translateCellId(endReference, rowOffset, columnOffset, thresholdRowIndex, thresholdColumnIndex);

        return startReference + ":" + endReference;
    }

    //Translates a cell id by a given offset

    public static String translateCellId(String cellId, int rowOffset, int columnOffset, int thresholdRowIndex, int thresholdColumnIndex) throws exceptions.TranslateException {
        //find the row-index and columnindex
        int rowIndex = Integer.parseInt(cellId.trim().split("[A-Z]+")[1]) - 1;
        int columnIndex = Spreadsheet.columnNameToIndex(cellId.trim().split("[0-9]+")[0]) + 1;

        if (rowIndex >= thresholdRowIndex && columnIndex >= thresholdColumnIndex) {
            //do translations
            rowIndex += rowOffset;
            columnIndex += columnOffset;

        }

        if (rowIndex < 0 || columnIndex < 1) //leaving the first column for row ids
        {
            throw new TranslateException();
        }

        //find the new columnName and cellName
        String columnName = Spreadsheet.indexToColumnName(columnIndex - 1); //first column reserved for row ids
        String cellName = columnName + String.valueOf(rowIndex + 1);

        return cellName;
    }

    //Translates a given expression by the offset provided. This is used to keep the cell references of  expressions valid during copy operations.
    public static String translateExpression(String sourceExpression, int rowOffset, int columnOffset) throws TranslateException {
        return translateExpression(sourceExpression, rowOffset, columnOffset, 0, 0);
    }

    //Translates a given expression by the offset provided given that it qualifies threshold. This is used to keep the cell references of  expressions valid during copy operations.
    public static String translateExpression(String sourceExpression, int rowOffset, int columnOffset, int thresholdRowIndex, int thresholdColumnIndex) throws TranslateException {
        //First check whether given expression is an expression, otherwise no need of doing translations
        String trimmedExpression = sourceExpression.trim();
        if (!trimmedExpression.startsWith("=")) {
            //Given expression is a non expressional cell value. Hence, we dont have to do any translation.
            //This expression is probably a decimal number or a string label. Only cell references need to be translated.
            return sourceExpression;
        }
        //The result to be returned
        String result = "=";
        List<ExpressionElement> elements = ExpressionTokenizer.tokenizeExpressionElements(trimmedExpression, null); //dont need the parser since we are not planning to evaluate any expressions here
        //now develop the new translated expression by analysing the tokens
        for (ExpressionElement element : elements) {
            if (element instanceof DelimeterElement) {
                //delimeters are never translated
                result += element.getStringValue();
            } else if (element instanceof TokenElement) {
                //a token element could be a cell reference or a cell range. hence we have to analyse it and modify as required.

                if (Cell.validateCellId(element.getStringValue())) {
                    String newCellId = translateCellId(element.getStringValue(), rowOffset, columnOffset, thresholdRowIndex, thresholdColumnIndex);
                    result += newCellId;
                } //or it could be a cell range
                else if (element.getStringValue().matches("[A-Z]+[0-9]+\\:[A-Z]+[0-9]+")) {
                    String newCellRange = translateCellRange(element.getStringValue(), rowOffset, columnOffset, thresholdRowIndex, thresholdColumnIndex);
                    result += newCellRange;
                } else {
                    //its not a sort of element to be translated. 
                    result += element.getStringValue();
                }
            }
        }

        return result;
    }

    //return the expression_element at given position of expression. Also return information regarding its position in expression.

    public TokenIdentificationResult identifyTokenAt(String expression, int position) {
        String preExpression = expression.substring(0, position + 1);
        //tokenize preElements
        ArrayList<ExpressionElement> preElements = ExpressionTokenizer.tokenizeExpressionElements(preExpression, this);
        //find the number of elements
        int i = preElements.size() - 1;
        if (i < 0) {
            return null;
        }

        //The element at ith index of tokenized expression is the element at given position
        ArrayList<ExpressionElement> elements = ExpressionTokenizer.tokenizeExpressionElements(expression, this);
        TokenIdentificationResult result = new TokenIdentificationResult(elements.get(i), position - preElements.get(i).getStringValue().length() + 1, position - preElements.get(i).getStringValue().length() + 1 + elements.get(i).getStringValue().length());

        return result;
    }
}
