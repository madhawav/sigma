/*
 *  Spreadsheet by Madhawa

 
 */

package exporters;

/**
 *
 * @author 130614N
 */
//functions in this interface are called by the exportNow function of Exporter
public interface ExportGUIFeedback {
    void reportCompletedSuccesfully(String statusMessage); //export has completed succesfully
    void reportStatus(String statusMessage); //report a message to be shown on status bar
    void reportFailed(String errorMessage); //exporter reporting an error to user
    void reportStopedByError(); //exporter reports that it has stopped due to an error
    
}
