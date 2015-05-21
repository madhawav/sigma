/*
 *  Spreadsheet by Madhawa

 
 */

package exporters;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import spreadsheet.Spreadsheet;

/**
 *
 * @author 130614N
 * 
 * The exporter used to export a spreadsheet as a .csv file
 */
public class CSVExporter extends AbstractExporter {

    private serializables.SerializableTable preparedValues = null;
    
    //file choser for saving
    private JFileChooser saveFileDg = null;
    private Spreadsheet targetSpreadsheet = null;
    //selected file from inquire preferences
    private File selectedFile = null;
    @Override
    public Spreadsheet getSpreadsheet() {
        return targetSpreadsheet;
    }

    @Override
    public void setSpreadsheet(Spreadsheet spreadsheet) {
        targetSpreadsheet = spreadsheet;
        preparedValues = null;
    }

    @Override
    public boolean inquirePreferences(Component parent) {
        //inquire file location from user
        saveFileDg = new JFileChooser();
        
        FileNameExtensionFilter filter =  new FileNameExtensionFilter("Comma Seperated Values (*.csv)", "csv");
        saveFileDg.addChoosableFileFilter(filter);
        saveFileDg.setFileFilter(filter);
        
        if(saveFileDg.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION)
        {
            selectedFile = saveFileDg.getSelectedFile();
            if(!saveFileDg.getSelectedFile().getAbsoluteFile().toString().toLowerCase().endsWith(".csv"))
            {
                selectedFile = new File(saveFileDg.getSelectedFile() + ".csv");

            }

            
            return true;
        }
        return false;
        
    }

   
   
    //load prepared values
    @Override
    public void prepareExport() {
        if(targetSpreadsheet == null)
            throw new IllegalArgumentException("A spreadsheet is not assigned to exporter.");
        
        preparedValues = targetSpreadsheet.valuesToSerializableTable();
    }

    @Override
    public void exportNow(ExportGUIFeedback uiFeedback) {
        PrintStream writer = null;
     
            if(preparedValues == null)
                throw new IllegalArgumentException("Call prepareExport before calling export now");
            if(selectedFile == null)
                throw new IllegalArgumentException("Call inquirePreferences return false or was not called before");
            //sort by keys so exporting can be done fast
            Set<Entry<String,serializables.SerializableCell>> values =  preparedValues.getCellMap().entrySet();
            //well, java doesnt support generic arrays
            Object[] cellValues = values.toArray();
            if(uiFeedback != null)
                uiFeedback.reportStatus("Arranging cells for export...");
            java.util.Arrays.sort(cellValues,new Comparator<Object>() {
                
                @Override
                public int compare(Object o1, Object o2) {
                    Entry<String,serializables.SerializableCell> obj1 = (Entry<String, serializables.SerializableCell>)o1;
                    Entry<String,serializables.SerializableCell> obj2 = (Entry<String, serializables.SerializableCell>)o2;
                    String id1 = obj1.getKey();
                    String id2 = obj2.getKey();
                    //sperate start row index and start column index
                    int RowIndex1 = Integer.parseInt(id1.split("[A-Z]+")[1]) - 1;
                    int columnIndex1 = Spreadsheet.columnNameToIndex(id1.split("[0-9]+")[0])+1;
                    
                    int RowIndex2 = Integer.parseInt(id2.split("[A-Z]+")[1]) - 1;
                    int columnIndex2 = Spreadsheet.columnNameToIndex(id2.split("[0-9]+")[0])+1;
                    
                    //left up columns come before
                    //do comparison
                    if(RowIndex1 < RowIndex2)
                        return -1;
                    else if(RowIndex1 > RowIndex2)
                        return 1;
                    else if(columnIndex1 < columnIndex2)
                        return -1;
                    else if(columnIndex1 > columnIndex2)
                        return 1;
                    return 0;
                    
                    
                }
            }); 
        try {   
            if(uiFeedback != null)
                uiFeedback.reportStatus("Accessing '" + selectedFile.getAbsolutePath() + "'");
            writer = new PrintStream(selectedFile);
            
                //read through sorted array and prepare the output
                int rowIndex = 0;
                int columnIndex = 0;
                if(uiFeedback != null)
                  uiFeedback.reportStatus("Writing to '" + selectedFile.getAbsolutePath() + "'");
                for(int readIndex = 0; readIndex <cellValues.length;readIndex++)
                {
                    Entry<String,serializables.SerializableCell> cellEntry = (Entry<String,serializables.SerializableCell>)cellValues[readIndex];
                    String cellId = cellEntry.getKey();
                    //read through cells adding each one of them to output
                    
                    int newRowIndex = Integer.parseInt(cellId.split("[A-Z]+")[1]) - 1;
                    int newColumnIndex = Spreadsheet.columnNameToIndex(cellId.split("[0-9]+")[0]);
                    while(rowIndex < newRowIndex)
                    {
                        //add the newlines until correct line is reached
                        rowIndex++;
                        columnIndex = 0;
                        writer.println();
                    }
                    while(columnIndex <newColumnIndex)
                    {
                        //add commas until correct offset is reached
                        columnIndex++;
                        writer.print(",");
                    }
                    //process the value
                    String value = cellEntry.getValue().getExpression();
                    boolean addStringQuote = false;
                    //for commas in value, ad quotation marks on either side of value
                    if(value.contains(","))
                    {
                        addStringQuote = true;
                    }
                    //for inverted commas in values, add two more inverted commas
                    value = value.replace("\"", "\"\"");
                    //and also mark for the inverted commas on either side
                    if(value.contains("\""))
                    {
                        addStringQuote = true;
                                
                    }
                    if(addStringQuote)
                        value = "\"" + value + "\"";
                    writer.print(value);
                }
                //all has completed succefully
                writer.flush();
                writer.close();
                writer = null;
                if(uiFeedback != null)
                    uiFeedback.reportCompletedSuccesfully("CSV Export has completed succesfully");
               
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CSVExporter.class.getName()).log(Level.SEVERE, null, ex);
            if(uiFeedback!= null){
                uiFeedback.reportFailed("Export Error: " + ex.getLocalizedMessage());
                uiFeedback.reportStopedByError();
            }
        } finally {
           if(writer!= null)
                writer.close();
            
        }
        
    }
    
}
