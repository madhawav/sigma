/*
 *  Spreadsheet by Madhawa

 
 */
package parser;

import spreadsheet.Cell;

/**
 *
 * @author Madhawa
 */
//Represents an object which can provide information regarding a cell when a cell id is given.
// Eg: Spreadsheet
public interface CellContentProvider {
    public Cell getCell(String cellId); //returns the cell with requested cell id. if it doesnt exist, return null
   
}
