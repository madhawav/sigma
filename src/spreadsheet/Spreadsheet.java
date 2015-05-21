/*
 *  Spreadsheet by Madhawa

 
 */
package spreadsheet;


import cellformatting.GeneralCellFormat;
import exceptions.TranslateException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import serializables.SerializableCell;
import serializables.SerializableCellStyle;
import serializables.SerializableTable;

/**
 *
 * @author Madhawa
 */
/*
    Uses a hashmap to represent the cell-states of spreadsheet
    The cells which are not present in hashmap are assumed to have the default cell value. (blank)
    
    HashMap is updated as and when a cell value is changed from default value to a non-default value.
*/
public class Spreadsheet implements parser.CellContentProvider {
    
    //active spreadsheet is the spreadsheet currently been evaluated. Cell references will be searched in this spreadsheet.
    
    
    //the set of listners listning to changes done in this spreadsheet
    private ArrayList<ChangeListener> changeListners = new ArrayList<>();
    //the set of listners to be notified in an event user interface of spreadsheet is to be refreshed. This occurs when the results of an expression evaluation becomes available
    private ArrayList<ChangeListener> uiChangedListners = new ArrayList<>();
    private int rowCount = 100;
    private int columnCount = 17;
    
    public synchronized void setRowCount(int value)
    {
        rowCount = value;
        notifyChanged();
    }
    public synchronized int getRowCount()
    {
        return rowCount;
    }
    public synchronized void setColumnCount(int value)
    {
        columnCount = value;
        notifyChanged();
    }
    public synchronized int getColumnCount()
    {
        return columnCount;
    }
    
    public synchronized void addChangedListner(ChangeListener listner)
    {
        changeListners.add(listner);
    }
    public synchronized void removeChangedListner(ChangeListener listner)
    {
        changeListners.remove(listner);
    }
    
    public synchronized  void addUIChangedListner(ChangeListener listener)
    {
        uiChangedListners.add(listener);
    }
    public synchronized  void removeUIChangedListner(ChangeListener listner)
    {
        uiChangedListners.remove(listner);
    }
    private synchronized void notifyUIChangedListners()
    {
        for(ChangeListener listner: uiChangedListners)
        {
            listner.stateChanged(new ChangeEvent(this));
        }
    }
    private synchronized void notifyChanged()
    {
        for(ChangeListener listner: changeListners)
        {
            listner.stateChanged(new ChangeEvent(this));
        }
    }
    
    //adds given number of rows at given position. shifting rows below as required
    public synchronized void addRows(int at, int count)
    {
        shiftCells(at, 0, count,0);
        rowCount+= count;
        notifyChanged();
    }
    //removes number of rows at given position. shifting rows below upwards
    public synchronized void deleteRows(int at, int count)
    {
        //clear the row which is being removed
        ArrayList<String> qualifiedCellList = new ArrayList<>();
        for(String key: cells.keySet())
        {
            //loop through individual cell id
            //check whether the cell lies in row being deleted
            
            int rowIndex = Integer.parseInt(key.trim().split("[A-Z]+")[1]) - 1;
            
            //check requirement to delete
            if(rowIndex>=at && rowIndex < at + count)
            {
                //add to qualified list to delete
                
                qualifiedCellList.add(key);
                
                
            }
        }
        
        for(String key: qualifiedCellList)
        {
            //delete
            clearCell(key);
        }
        //shift remaining cells
        shiftCells(at+count, 0, -count,0);
        rowCount-= count;
        
        notifyChanged();
    }
    //removes number of columns at given position. shifting columns right leftwards
     public synchronized void deleteColumns(int at, int count)
    {
        //clear the row which is being removed
        ArrayList<String> qualifiedCellList = new ArrayList<>();
        for(String key: cells.keySet())
        {
            //loop through individual cell id
            //check whether the cell lies in column being deleted
            
            int rowIndex = Integer.parseInt(key.trim().split("[A-Z]+")[1]) - 1;
            int columnIndex = Spreadsheet.columnNameToIndex(key.trim().split("[0-9]+")[0]) + 1;
            //check requirement to delete
            if(columnIndex>=at && columnIndex < at + count)
            {
                //add to qualified list to delete
                
                qualifiedCellList.add(key);
                
                
            }
        }
        
        for(String key: qualifiedCellList)
        {
            //delete
            clearCell(key);
        }
        
        shiftCells(0, at+count, 0,-count);
        columnCount-= count;
        notifyChanged();
    }
    //adds given number of columns at given position. shifting rows below as required
    public synchronized void addColumns(int at, int count)
    {
        shiftCells(0, at, 0,count);
        columnCount+= count;
        notifyChanged();
    }
    
   
    //Retrieves the CellIdSet of spreadsheet
     synchronized Set<String> getCellIdSet()
    {
        return cells.keySet();
    }
    private volatile HashMap<String,Cell> cells = null; //holds the cells of spreadsheet. Assuming spreadsheet is a sparse matrix
    public Spreadsheet()
    {
        cells = new HashMap<>();
        
    }
    
    @Override
    public synchronized Cell getCell(String cellId) //returns the cell with requested cell id. if it doesnt exist, return null
    {
        if(cells.containsKey(cellId))
        {
            return cells.get(cellId);
        }
        return null;
        
    }
    
    //clears a cell and returns true if succesful. This removes the relavent cell entry from hash map
    public synchronized boolean clearCell(String cellId)
    {
        if(cells.containsKey(cellId))
        {
            Cell cell = cells.get(cellId);
            
            cell.notifyObservers();
            cells.remove(cellId);
            notifyChanged();
            return true;
        }
        return false;
    }
    
    //retrieves cell style assigned to a particular cell
    public synchronized serializables.SerializableCellStyle getCellStyle(String cellId)
    {
        Cell cell = getCell(cellId);
        if(cell == null)
        {
            //the cell requested is blank and hence its records are not held. but it should have the default configuration
            return SerializableCellStyle.getDefaultCellStyle();
        }
        return cell.getCellStyle();
    }
    
    //retrieves cell formatting assigned to a particular cell
    public synchronized cellformatting.AbstractCellFormat getCellFormatting(String cellId)
    {
        Cell cell = getCell(cellId);
        if(cell == null)
        {
            //the cell requested is blank and hence its records are not held. but it should have the default configuration
            return new cellformatting.GeneralCellFormat();
        }
        return cell.getCellFormat();
    }
    //sets the cell style assigned to a particular cell
    
    public synchronized boolean setCellStyle(String cellId, SerializableCellStyle newCellStyle)
    {
        Cell cell = getCell(cellId);
        if(cell == null)
        {
            //Such a cell doesnt exist. Is our cell id a valid cell id?
            if(Cell.validateCellId(cellId))
            {
                //Valid Cell ID. Therefore, make a reference to this cell
                if(!(SerializableCellStyle.isDefault(newCellStyle)))
                {
                    cell = new Cell(this);
                    setCell(cellId, cell);
                }
                
                
            }
            else return false;
        }
        if(cell != null)
        {
            cell.setCellStyle(newCellStyle);
            if(cell.isEmpty())
                clearCell(cellId);
        }
        notifyChanged();
       
        return true;
    }
    
    //sets the cell formatting assigned to a particular cell
    public synchronized boolean setCellFormatting(String cellId, cellformatting.AbstractCellFormat newCellFormat)
    {
        Cell cell = getCell(cellId);
        if(cell == null)
        {
            //Such a cell doesnt exist. Is our cell id a valid cell id?
            if(Cell.validateCellId(cellId))
            {
                //Valid Cell ID. Therefore, make a reference to this cell
                if(!(newCellFormat instanceof GeneralCellFormat))
                {
                    cell = new Cell(this);
                    setCell(cellId, cell);
                }
                
                
            }
            else return false;
        }
        if(cell != null)
        {
            cell.setCellFormat(newCellFormat);
            if(cell.isEmpty())
                clearCell(cellId);
        }
       notifyChanged();
        return true;
    }
   //retrieve the cellExpression associated to a cellId
    public synchronized String getCellExpression(String cellId)
    {
        Cell c = getCell(cellId);
        if(c == null)
            return "";
        return c.getCellExpression();
    }
    
    
    //Set the expression associated with cell cellId
    public synchronized boolean setCellExpression(String cellId, String newExpression)
    {
       
        //retrieve the associated cell
        Cell cell = getCell(cellId);
        if(cell == null)
        {
            //Such a cell doesnt exist. Is our cell id a valid cell id?
            if(Cell.validateCellId(cellId))
            {
                //Valid Cell ID. Therefore, make a reference to this cell
                if(!newExpression.trim().equals(""))
                {
                    cell = new Cell(this);
                    setCell(cellId, cell);
                }
                
                
            }
            else return false;
        }
        if(cell != null)
        {
            cell.setCellExpression(newExpression);
            if(cell.isEmpty())
                clearCell(cellId);
        }
        
        notifyChanged();
        return true;
    }
    
    //start observing a cell by some other cell
    //this function is implemented in spreadsheet (and not in cell) because not all cells are held as objects in memory. (Empty cells are not made into objects)
    //hence, we need to make cell objects to those empty cells and add observer to them
    synchronized void  addCellObserver(String observableId, Observer observer)
    {
        Cell observable = getCell(observableId);
        if(observable == null)
        {
            //the cell doesnt exist in hashmap. hence make one for it
            observable = new Cell(this);
            cells.put(observableId, observable);
            
        }
        observable.addObserver(observer); //by doing so, we have also made sure this cell is no longer considered empty (isEmpty would return false)
        
        
    }
    private boolean closed = false; 
    public synchronized void close()
    {
        //since stopping is unsafe, we will send an interupt asking them to stop as soon as possible 
        getDefaultWorkerThreadGroup().interrupt();
        waitingThreads.clear();
        uiChangedListners.clear();
        closed = true;
    }
    public synchronized boolean isClosed()
    {
        return this.closed;
    }
    //adds a new cell to the hash-map of spreadsheet
    public synchronized boolean setCell(String cellName, Cell newCell)
    {
        if(Cell.validateCellId(cellName))
        {
            cells.put(cellName, newCell);
            notifyChanged();
            return true;
            
        }
        else
        {
            return false; //invalid cell references
        }
        
    }
    //converts a column index to column name
    public static String indexToColumnName(int i)
    {
        char output = (char) (65 + i);
        return String.valueOf(output);
    }
    
 
    //convert columnName to index
    public static int columnNameToIndex(String columnName)
    {
        //convert to uppercase
        String trimmedColumnName = columnName.trim().toUpperCase();
        
        //this is very similar to doing a base conversion with 26 as base
        
        int columnIndex = 0; //output
        //LSC = least significant charachter
        int placeValue = 1; //place value initially set at unity. we are reading from back to front. so LSC is read first.
        for(int i =trimmedColumnName.length()-1;i>=0;i--)
        {
            //Get the index of char(i) in alphabet. A = 1.
            int unitValue = (int)trimmedColumnName.charAt(i) - 64;
            //update output
            columnIndex += placeValue * unitValue;
            //update place value
            placeValue *= 26;
        }
        
        return columnIndex - 1;
    }
    
    //create a spreadsheet from a serializable table
    public static Spreadsheet fromSerializableTable(SerializableTable serializableTable)
    {
        Spreadsheet sheet = new Spreadsheet();
        for(String key: serializableTable.getCellMap().keySet())
        {
            serializables.SerializableCell serializedCell = serializableTable.getCellMap().get(key);
            sheet.setCellExpression(key, serializedCell.getExpression());
            sheet.setCellFormatting(key, cellformatting.SerializationHelper.deserializeCellFormat(serializedCell.getFormatName(),serializedCell.getFormatArguements()));
            sheet.setCellStyle(key, serializedCell.getSerializableCellStyle());
        }
        sheet.setRowCount(serializableTable.getRowCount());
        sheet.setColumnCount(serializableTable.getColumnCount());
        return sheet;
    }
    
    //create a serializable table from this spreadsheet
    public synchronized serializables.SerializableTable toSerializableTable()
    {
        serializables.SerializableTable output = new SerializableTable();
        for(String key : cells.keySet())
        {
            Cell currentCell = cells.get(key);
            serializables.SerializableCell serializableCell = new SerializableCell(currentCell.getCellExpression());
            serializableCell.setFormatName(currentCell.getCellFormat().getSerializedCellFormatName());
            currentCell.getCellFormat().appendSerializedArguements(serializableCell.getFormatArguements());
            
            //store styling
            serializableCell.setSerializableCellStyle(currentCell.getCellStyle());
            output.getCellMap().put(key, serializableCell);
        }
        output.setTimeStamp(new Date()); //new Date is set to current time stamp
        output.setColumnCount(columnCount);
        output.setRowCount(rowCount);
        return output;
    }
    //create a serializable table of values instead of expressions
    public synchronized serializables.SerializableTable valuesToSerializableTable()
    {
        serializables.SerializableTable output = new SerializableTable();
        for(String key : cells.keySet())
        {
            Cell currentCell = cells.get(key);
            serializables.SerializableCell serializableCell = new SerializableCell(currentCell.getCellText());
            serializableCell.setFormatName(currentCell.getCellFormat().getSerializedCellFormatName());
            currentCell.getCellFormat().appendSerializedArguements(serializableCell.getFormatArguements());
            
            //store styling
            serializableCell.setSerializableCellStyle(currentCell.getCellStyle());
            
            output.getCellMap().put(key, serializableCell);
        }
        output.setTimeStamp(new Date()); //new Date is set to current time stamp
        output.setColumnCount(columnCount);
        output.setRowCount(rowCount);
        return output;
    }

    //the set of curently running cell solving threads
    private int MaxThreadCount = 10;
    
    public synchronized int getMaxThreadCount()
    {
        return MaxThreadCount;
    }
    public synchronized void setMaxThreadCount(int maxCount)
    {
        MaxThreadCount = maxCount;
    }
    
    private ThreadGroup workerThreads = new ThreadGroup("Spreadsheet Worker Threads");
    private LinkedList<Thread> waitingThreads = new LinkedList<>();
    
    ThreadGroup getDefaultWorkerThreadGroup()
    {
        
        return workerThreads;
    }
    
    void enqueueWorkerThread(Thread nextThread) {
        if(workerThreads.activeCount() < getMaxThreadCount())
        {
            //we can start it immediately
            nextThread.start();
        }
        else
        {
            //lets leave it in the queue
            waitingThreads.addLast(nextThread);
        }
    }

    //called when a thread has been completed
    synchronized void reportThreadCompleted() {
       //ask the spreadsheet gui to update
       notifyUIChangedListners();
        
       if(waitingThreads.isEmpty())
           return;
       Thread nextThread = waitingThreads.removeFirst();
       nextThread.start();
       
       
    }
    
    
    private class cellInfo //used to store cell expression and cell format bundled together. Used by shiftCells
    {
        private String cellExpression = "";
        private cellformatting.AbstractCellFormat cellFormat;
        private SerializableCellStyle cellStyle;
        public String getCellExpression()
        {
            return cellExpression;
        }
        public SerializableCellStyle getCellStyle()
        {
            return cellStyle;
        }
        public void setCellStyle(SerializableCellStyle style)
        {
            this.cellStyle = style;
        }
        public void setCellExpression(String newExpression)
        {
            cellExpression = newExpression;
        }
        public cellformatting.AbstractCellFormat getCellFormat()
        {
            return cellFormat;
        }
        public void setCellFormat(cellformatting.AbstractCellFormat newCellFormat)
        {
            cellFormat = newCellFormat;
        }
        public cellInfo(String cellExpression, cellformatting.AbstractCellFormat cellFormat, SerializableCellStyle style)
        {
            this.cellExpression = cellExpression;
            this.cellFormat = cellFormat;
            this.cellStyle = style;
        }
    }
    //shift cells which pass a given threshold (beyond minimum row and column ids) by the given offset
    public synchronized void shiftCells(int thresholdRowIndex, int thresholdColumnIndex, int rowOffset, int columnOffset)
    {
        
        HashMap<String,cellInfo> qualifiedCellList = new HashMap<>();
        //make a hash-maps of cells to be shifted
        for(String key: cells.keySet())
        {
            //loop through individual cell id
            //check whether it qualifies threshold
            //find the row-index and columnindex
            int rowIndex = Integer.parseInt(key.trim().split("[A-Z]+")[1]) - 1;
            int columnIndex = Spreadsheet.columnNameToIndex(key.trim().split("[0-9]+")[0]) + 1;
            //check threshold requirement
            if(rowIndex>=thresholdRowIndex && columnIndex >=thresholdColumnIndex)
            {
                //add to qualified list
                qualifiedCellList.put(key, new cellInfo(cells.get(key).getCellExpression(), cells.get(key).getCellFormat(),cells.get(key).getCellStyle()) );
                
                
            }
        }
        for(String key: qualifiedCellList.keySet())
        {
            //clear previous cells
            clearCell(key);
            setCellFormatting(key, new GeneralCellFormat());
            setCellStyle(key, SerializableCellStyle.getDefaultCellStyle());
        }
        for(String key: qualifiedCellList.keySet())
        {
            //find the row-index and columnindex
            int sourceRowIndex = Integer.parseInt(key.trim().split("[A-Z]+")[1]) - 1;
            int sourceColumnIndex = Spreadsheet.columnNameToIndex(key.trim().split("[0-9]+")[0]) + 1;
            
            //find targets
            int targetRowIndex = sourceRowIndex + rowOffset;
            int targetColumnIndex = sourceColumnIndex + columnOffset;
            
            String translatedExpression  = "";
            try {
                //translate expression
                translatedExpression = parser.Parser.translateExpression(qualifiedCellList.get(key).cellExpression, rowOffset, columnOffset,thresholdRowIndex,thresholdColumnIndex);
            } catch (TranslateException ex) {
                Logger.getLogger(Spreadsheet.class.getName()).log(Level.SEVERE, null, ex);
                translatedExpression = ex.getMessage();
            }
            
            
            //find the target cell_id
             String columnName = Spreadsheet.indexToColumnName(targetColumnIndex - 1); //first column reserved for row ids
            String destinationId = columnName + String.valueOf(targetRowIndex+1);
         
            //set the translated expression at destinaion
            setCellExpression(destinationId, translatedExpression);
            setCellFormatting(destinationId, qualifiedCellList.get(key).getCellFormat());
            setCellStyle(destinationId, qualifiedCellList.get(key).getCellStyle());
        }
        
        notifyChanged();
        
    }
}
