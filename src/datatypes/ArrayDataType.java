/*
 *  Spreadsheet by Madhawa

 
 */
package datatypes;

/**
 *
 * @author Madhawa
 */
/*
    DataArray is used to hold data in cell ranges.
    An ArrayDataType is generated when a range cell reference is parsed

    Array indices are held similar to matrix notation. 
        i.e. - rows come first in index.

*/
public class ArrayDataType extends DataType{

    
    DataType[][] dataArray = null;
    //default contructor
    public ArrayDataType()
    {
        dataArray = new DataType[0][0];
    }
    public ArrayDataType(DataType[][] data)
    {
        this.dataArray = data;
    }
    
    public DataType[][] getDataArray()
    {
        return dataArray;
    }
    
    public void setDataArray(DataType[][] value)
    {
        this.dataArray = value;
    }
    
    public boolean isVector()
    {
        if(dataArray.length == 0)
            return true;
        if(dataArray.length == 1)
            return true;
        if(dataArray[0].length == 1)
            return true;
        return false;
    }
    
    //return the internal 1D/2D array as a single dimension vector
    public DataType[] asVector()
    {
        
        if(dataArray.length == 0) //empty array is an empty vector
            return new DataType[0];
        
        DataType[] results = new DataType[dataArray.length * dataArray[0].length];
        
        int index = 0; //current Index updated in output array
        for(int x = 0;x < dataArray.length;x++)
        {
            for(int y = 0; y < dataArray[x].length;y++)
            {
                results[index] = dataArray[x][y];
                index++;
            }
        }
        return results;
    }
    
    @Override
    public String getCellText(cellformatting.AbstractCellFormat cellFormat) {
        
        
        //if vector, print as a 1D array with only one curly bracket
        
        if(isVector())
        {
            DataType[] dataList = asVector();
            String result = "{";
            for(int i = 0;i < dataList.length;i++)
            {
                result += dataList[i].getCellText(cellFormat);
                if(i < dataList.length - 1)
                    result += ", ";
            }
            result += "}";
            return result;
            
        }
        else
        {
            //if a 2D array, we use nested brackets notation
            //NOTE: if length is 0, then its a vector. Therefore, dataArray[0] exists for this case
            String result = "{";
            for(int x = 0;x < dataArray.length;x++)
            {
                //row wise
                result += "{";
                for(int y = 0;y < dataArray[x].length;y++)
                {
                    //column wise
                    result += dataArray[x][y].getCellText(cellFormat);
                    if(y < dataArray[x].length - 1)
                        result += ", ";
                }
                
                result += "}";
                if(x < dataArray.length - 1)
                            result += ", ";

            }
            result += "}";
            return result;
            
            
        }
        
       
    }

   
    
}
