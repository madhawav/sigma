/*
 *  Spreadsheet by Madhawa

 
 */
package spreadsheet;

import cellformatting.AbstractCellFormat;
import cellformatting.GeneralCellFormat;
import exceptions.SpreadsheetException;
import datatypes.DataType;
import datatypes.ExpressionDataType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import parser.ExpressionElement;
import parser.Parser;
import parser.TokenElement;
import serializables.SerializableCellStyle;

/**
 *
 * @author Madhawa
 */
public class Cell extends Observable implements Observer {
    /*
    Represents a cell in spreadsheet.
    Things to be added
        Cell dependancy 
        Circular reference check
    
    Observable because other cells would observe changes of this cell if they depend on it by cell references
    Observer because this might depend on other cells as cell references
    */
    //Is cellId a valid cell id?
    public static boolean validateCellId(String cellId)
    {
        String cellIdPattern = "[A-Z]+[0-9]+";
        return cellId.matches(cellIdPattern);
    }
    
    private ArrayList<String> ObservingCellIds = new ArrayList<>();
    
    private final Spreadsheet cellContainer;
    
    private AbstractCellFormat cellFormat = new GeneralCellFormat();
    
    //stores the styling information related to a cell
    private serializables.SerializableCellStyle cellStyle = SerializableCellStyle.getDefaultCellStyle();
    
    public SerializableCellStyle getCellStyle()
    {
        return cellStyle;
    }
    
    //use spreadsheet class to assign cell styles to cells
    //this is required because no cell objects are held for blank cells (to save memory)
    void setCellStyle(SerializableCellStyle newStyle)
    {
        cellStyle = newStyle;
    }
    
    public AbstractCellFormat getCellFormat()
    {
        return cellFormat;
    }
    
    //please use spreadsheet to assign cell formats of cells
    void setCellFormat(AbstractCellFormat newFormat)
    {
        cellFormat = newFormat;
        cellText = null;
    }
    
    
    //this back reference is required in expression evaluation to solve cell references.
    public Spreadsheet getContainer()
    {
        return cellContainer;
    }
    
    private String expression;
    //private DataType cellData; //the internal datatype inside the cell
    public Cell(Spreadsheet container)
    {
        this.expression = "";
        this.cellContainer = container;
        //cellData = new LabelDataType(""); //default cell value;
    }
    public Cell(String expression, Spreadsheet container)
    {
        this.expression = expression;
        this.cellContainer = container;
    }
    
    /*public DataType getCellData()
    {
        return cellData;
    }*/
    /*public void setCellData(DataType value)
    {
        this.cellData = value;
    }*/
    
    private synchronized void updateObservers(DataType cellData)
    {
        //remove prevailing observables being observed by this
        String[] deleteCheckList = new String[ObservingCellIds.size()];
        ObservingCellIds.toArray(deleteCheckList);
        
        for(String obsrvedCellId: deleteCheckList)
        {
            Cell observedCell = cellContainer.getCell(obsrvedCellId);
            //remove this from observing
            observedCell.deleteObserver(this);
            
        }
        ObservingCellIds.clear();
        
        if(cellData instanceof datatypes.ExpressionDataType)
        {
            //find new cells to be observed
            ExpressionDataType expaDataType =(ExpressionDataType)cellData;
            //retrieve the elements in expression
            List<ExpressionElement> elements = expaDataType.getExpressionElements();
            for(ExpressionElement element: elements)
            {
                if(element instanceof TokenElement)
                {
                    //it could be a cell reference. so check
                    TokenElement token = (TokenElement)element;
                    //is it a cell reference?
                    String elementValue = element.getStringValue();
                    if(Cell.validateCellId(elementValue))
                    {
                        cellContainer.addCellObserver(elementValue, this);
                        ObservingCellIds.add(elementValue);
                    }
                    //or it could be a cell range
                    else if(element.getStringValue().matches("[A-Z]+[0-9]+\\:[A-Z]+[0-9]+"))
                    {
                        //its a cell-range
                        //separate two cell references
                        String[] parts = element.getStringValue().split("\\:");
                        //start reference
                        String startReference = parts[0];
                        //sperate start row index and start column index
                        int startRowIndex = Integer.parseInt(startReference.split("[A-Z]+")[1]) - 1;
                        int startColumnIndex = Spreadsheet.columnNameToIndex(startReference.split("[0-9]+")[0])+1;

                        //end reference
                        String endReference = parts[1];
                        //sperate end row index and end column index
                        int endRowIndex = Integer.parseInt(endReference.split("[A-Z]+")[1]) - 1;
                        int endColumnIndex = Spreadsheet.columnNameToIndex(endReference.split("[0-9]+")[0])+1;

                        //go through individual cell in range
                        for(int y = startRowIndex;y <= endRowIndex;y++)
                            for(int x = startColumnIndex;x <= endColumnIndex; x++)
                            {
                                //find the appropriate cell id of source to read the expression from for currently processed cell
                                String columnName = Spreadsheet.indexToColumnName(x - 1); //first column reserved for row ids
                                String cellName = columnName + String.valueOf(y+1);
                                
                                //setup observer
                                cellContainer.addCellObserver(cellName, this);
                                ObservingCellIds.add(cellName);
                            }

                    }
                    
                }
            }
            
            
        }
        
        for(String deleteCheckId: deleteCheckList)
        {
            Cell checkCell = cellContainer.getCell(deleteCheckId);
            
            if(checkCell.isEmpty())
            {
                //since it has become empty, remve it from hashmap
                cellContainer.clearCell(deleteCheckId);
            }
        }
    }
    
    
    
    //solve the cell expression and return its data type
    public DataType getSolvedCellData() throws SpreadsheetException
    {
        Parser parser = new Parser();
        parser.setCellProvider(cellContainer);
        //parse the expression. 
        //synchronization required to ensure that setCellExpression of spreadsheet is not called concurrenctly to reading the expression in following line of code
        Object lockObject = cellContainer;
        DataType cellData;
        synchronized(lockObject)
        {
            cellData = parser.parseCellExpression(expression);
        }
        
        
        if(cellData instanceof datatypes.ExpressionDataType)
        {
            //Solve if cellData is an expression
            datatypes.ExpressionDataType expressionDT = (datatypes.ExpressionDataType)cellData;
            
            
            
            return expressionDT.getSolvedDataType();
        }
        //Otherwise return cell data as it is
        return cellData;
    }
    //returns true if this cell is empty, with no formatting or styling remaining and if its not being observed by anyone
    public boolean isEmpty()
    {
        if(countObservers() != 0)
            return false;
        if(!SerializableCellStyle.isDefault(cellStyle))
            return false;
        return getCellExpression().trim().equals("") && getCellFormat() instanceof GeneralCellFormat;
    }
    
    //the cell text to be returned when getCellText is called. This field should be cleared whenever cellText is to be updated.
    private volatile String cellText = null;
    //The data type before solving
    private DataType rawDataType = null;
    public DataType getRawDataType()
    {
        if(rawDataType == null) //raw data type is not available. that means the expression has not been parsed to retrieve this rawDataType
        {
            //so retrieve the raw data type
            Parser parser = new Parser();
            parser.setCellProvider(cellContainer);

            rawDataType = parser.parseCellExpression(expression);
        }
        return rawDataType;
    }
    
    private Thread queuedThreadInSpreadsheet = null;
    private java.util.ArrayDeque<Thread> localThreadQueue = new ArrayDeque<>();
    
    private synchronized boolean isThreadQueuedInSpreadsheet()
    {
        return queuedThreadInSpreadsheet != null;
    }
    private boolean enqueueWorkerThread(Thread thread)
    {
        //dont start any threads if the spreadsheet is marked closed
        if(cellContainer.isClosed())
            return false;
        if(!isThreadQueuedInSpreadsheet())
        {
            //there are no threads running for this cell currently. Hence we can send this to the spreadsheet straightaway
            cellContainer.enqueueWorkerThread(thread);
            queuedThreadInSpreadsheet = thread;
            return true;
        }
        else
        {
            localThreadQueue.addLast(thread);
            return true;
        }
    }
    //called by a worker thread when its task is done
    private synchronized void onWorkerThreadCompleted()
    {
        queuedThreadInSpreadsheet = null;
        if(cellContainer.isClosed())
            //dont restart any more threads if the spreadsheet is closed
            return;
        if(!localThreadQueue.isEmpty())
        {
            //queue the next entry from local queue to spreadsheets queue
            Thread nextThread = localThreadQueue.removeFirst();
            cellContainer.enqueueWorkerThread(nextThread);
            queuedThreadInSpreadsheet = nextThread;
        }
        cellContainer.reportThreadCompleted();
        
    }
    
    
    private synchronized void setCellText(String cellText)
    {
        this.cellText = cellText;
    }
    private synchronized String readCellText()
    {
        return cellText;
    }
    
    private synchronized void clearObsoleteThreads()
    {
        //clear the queue of threads and interupt the running thread if any
        localThreadQueue.clear();
        if(queuedThreadInSpreadsheet != null && queuedThreadInSpreadsheet.isAlive())
        {
            queuedThreadInSpreadsheet.interrupt();
        }
    }
    
    public String getCellText()
    {
        //The text which should appear in the cell
        if(readCellText() != null)
            return readCellText();
        
        setCellText("Evaluating...");
        
        //stop all queued worker threads and interupt the running worker thread if any
        
        
        Parser parser = new Parser();
        parser.setCellProvider(cellContainer);

        final DataType cellData = parser.parseCellExpression(expression);
        rawDataType = cellData;
        updateObservers(cellData);
                
        Thread solveThread = new Thread(cellContainer.getDefaultWorkerThreadGroup(), new Runnable() {

            @Override
            public void run() {
                //Following block must happen in the thread
              
                if(Thread.interrupted())
                {
                    //interupted by a never thread evaluating expression on this cell. Hence, this thread is now obsolete
                    return;
                }
                setCellText(cellData.getCellText(cellFormat));  
                onWorkerThreadCompleted();
            }
        },"Worker Thread");
        
        enqueueWorkerThread(solveThread);
        
        return readCellText();
        //return cellData.getCellText();
    }
    public String getCellExpression()
    {
        
        //get the expression associated with this cell
        return this.expression;
    }
    
    //set changed is coupled with notifyObservers
    @Override
    public void notifyObservers()
    {
        setChanged();
        super.notifyObservers();
    }
    
    //external packages should use spreadsheet class to assign expressions
    void setCellExpression(String value)
    {
        this.expression = value;
        setCellText(null);
        rawDataType = null;
        //tell all observers that cell value has been modified
        //setChanged();
        notifyObservers();
        //cellData = Parser.parseCellExpression(value);
    }

    @Override
    public void update(Observable o, Object arg) {
        //some of the depending cells has changed. therefore, the cellText stored in local variable is now obsolete. make it null so it will be generated next time getCellText is called
        setCellText(null);
        
        try
        {
            notifyObservers();    
        }
        catch(StackOverflowError e)
        {
            //Circular reference
            //the error message is displayed when stack-overflow occurs at cell evaluation
           
        }
        
    }
}
