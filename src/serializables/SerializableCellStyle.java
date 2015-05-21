/*
 *  Spreadsheet by Madhawa

 
 */
package serializables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.io.Serializable;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

/**
 *
 * @author Madhawa
 */
/*
    Represents the styling aspects of a cell such as font, font size, font style, allignment
*/
public class SerializableCellStyle implements Serializable {
    //use the keyword default to denote system default font
    private String fontName = "Default";
    private boolean isBold = false;
    private boolean isItalic  = false;
    private boolean isUnderlined =false;
    private int fontSize = 12;
    private boolean isDefaultFontSize = true;
    private Alignment cellAlignment = Alignment.Auto;
    private Color fontColor = Color.BLACK; 
    private Color cellColor = Color.WHITE;
    
    //retrieves the default cell styling
    public static SerializableCellStyle getDefaultCellStyle()
    {
        SerializableCellStyle style = new SerializableCellStyle(); //default constructor assigns default values
        return style;
    }
    public static boolean isDefault(SerializableCellStyle cellStyle)
    {
        return isIdentical(cellStyle, getDefaultCellStyle());
    }
    //check whether twp cell styles are identical
    //needed for cleanup operations of hashmap to check whether a cell has become default again
    public static boolean isIdentical(SerializableCellStyle cellStyle1, SerializableCellStyle cellStyle2)
    {
        if(cellStyle1.getBold() != cellStyle2.getBold())
            return false;
        if(cellStyle1.getItalic() != cellStyle2.getItalic())
            return false;
        if(cellStyle1.getIsDefaultFontSize() != cellStyle2.getIsDefaultFontSize())
            return false;
        if(!cellStyle1.getIsDefaultFontSize())
        {
            if(cellStyle1.getFontSize() != cellStyle2.getFontSize())
                return false;
        }
        if(cellStyle1.getUnderlined() != cellStyle2.getUnderlined())
            return false;
        if(cellStyle1.getAlignment() != cellStyle2.getAlignment())
            return false;
        if(!cellStyle1.getCellColor().equals(cellStyle2.getCellColor()))
            return false;
        if(!cellStyle1.getFontColor().equals(cellStyle2.getFontColor()))
            return false;
         if(!cellStyle1.getFontFamilyName().equals(cellStyle2.getFontFamilyName()))
            return false;
         return true;
    }
    
    public String getFontFamilyName()
    {
        return fontName;
    }
    public void setFontFamilyName(String newName)
    {
        fontName = newName;
    }
    
    public boolean getBold()
    {
        return isBold;
    }
    public void setBold(boolean value)
    {
        isBold = value;
    }
    
    public boolean getItalic()
    {
        return isItalic;
    }
    public void setItalic(boolean value)
    {
        isItalic = value;
    }
    
    public boolean getUnderlined()
    {
        return isUnderlined;
    }
    public void setUnderlined(boolean value)
    {
        isUnderlined = value;
    }
    
    public boolean getIsDefaultFontSize()
    {
        return isDefaultFontSize;
    }
    
    public void setIsDefaultFontSize(boolean newValue)
    {
        isDefaultFontSize = newValue;
    }
    public int getFontSize()
    {
        return fontSize;
    }
    public void setFontSize(int newSize)
    {
        fontSize = newSize;
    }
    
    public Alignment getAlignment()
    {
        return cellAlignment;
    }
    public void setAlignment(Alignment newAlignment)
    {
        cellAlignment = newAlignment;
    }
    public Color getCellColor()
    {
        return cellColor;
    }
    public void setCellColor(Color newValue)
    {
        cellColor = newValue;
    }
    public Color getFontColor()
    {
        return fontColor;
    }
    public void setFontColor(Color newColor)
    {
        fontColor = newColor;
    }
    public enum Alignment
    {
        Auto, Left, Center, Right
    }
    
    public void applyStyle(Component component, int defaultAlignment)
    {
        //apply the styling to given componenet
        //determine Style
        int fontStyle = 0;
        if(isBold)
        {
            fontStyle = fontStyle | Font.BOLD;
        }
        if(isItalic)
        {
            fontStyle = fontStyle | Font.ITALIC;
        }
        //determine size
        int fontSz = this.fontSize;
        if(isDefaultFontSize)
        {
            Font defaultFont = UIManager.getDefaults().getFont("Label.font");
            fontSz = defaultFont.getSize();
        }
        //make base font
        Font fnt = new Font(fontName, fontStyle, fontSz);
        //apply underlined
        Map attribs = fnt.getAttributes();
        if(isUnderlined)
        {
            attribs.put(TextAttribute.UNDERLINE,TextAttribute.UNDERLINE_ON );    
        }
        fnt = fnt.deriveFont(attribs);
        //apply font
        component.setFont(fnt);
        if(component instanceof JLabel)
        {
            JLabel label = (JLabel)component;
            switch(cellAlignment)
            {
                case Auto:
                    label.setHorizontalAlignment(defaultAlignment);
                    break;
                case Left:
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                    break;
                case Center:
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    break;
                case Right:
                    label.setHorizontalAlignment(SwingConstants.RIGHT);
                    break;
                    
            }
           
        }
        component.setForeground(fontColor);
        component.setBackground(cellColor);
        
        
              
    }
}
