/*
 *  Spreadsheet by Madhawa

 
 */

package exporters;

import java.awt.Component;

/**
 *
 * @author 130614N
 * An abstract exporter of spreadsheet
 */
public abstract class AbstractExporter {
    public abstract spreadsheet.Spreadsheet getSpreadsheet();
    public abstract void setSpreadsheet(spreadsheet.Spreadsheet spreadsheet);
    public abstract boolean inquirePreferences(Component parent); //inqure preferences from user about export. return true if user accepted preferences. if the user canceled the export, return false
    public abstract void prepareExport(); //called in the gui thread before export now is called
    public abstract void exportNow(ExportGUIFeedback uiFeedback); //called in a background thread to do the export
    
}
