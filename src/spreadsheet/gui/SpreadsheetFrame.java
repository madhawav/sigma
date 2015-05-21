/*
 *  Spreadsheet by Madhawa

 
 */

package spreadsheet.gui;


import cellformatting.CurrencyCellFormat;
import cellformatting.DateTimeCellFormat;
import cellformatting.DecimalNumberCellFormat;
import cellformatting.GeneralCellFormat;
import cellformatting.PercentageCellFormat;
import exceptions.TranslateException;
import exporters.AbstractExporter;
import exporters.CSVExporter;
import exporters.ExportGUIFeedback;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import parser.DelimeterElement;
import parser.ExpressionElement;
import parser.Parser;
import parser.TokenElement;
import serializables.SerializableCellStyle;
import spreadsheet.Cell;
import spreadsheet.Spreadsheet;

/**
 *
 * @author 130614N
 */
/*
    Main UI of application which holds the jTable to present the Spreadsheet and the AddressBar. This also holds the toolbar, menu bar and the code related to its options.
    Definitions
        Cell Picking - When an expression is being edited in the addressbar, user can select cells in the cellTable to append there cell Id to addressBar.
            This is commonly used in MS Excel to make it easier to develop expressions.
        Cell Highlights - Highlight the cells relevant to current expression being edited in a special colour.
*/
public class SpreadsheetFrame extends javax.swing.JFrame  {

    
    
    private CellTableModel tableModel = null; //the table model of cellTable
    private Spreadsheet currentSheet = null; //currently opened spreadsheet
    private SpreadsheetCellRenderer cellRenderer = null; //cell renderer of cellTable
    
    /*
        Editing Cell - A copy of information regarding currently editing cell is required to make cell picking work
        editingRowId and editingColumnId holds information regarding editing cell
    */
    private int editingRowId = 0;
    private int editingColumnId = 0;
    
    private boolean avoidSelectionFeedback = false; //set true when a selection is changed by code. This is used to filter feedback messages
    private boolean avoidExpressionBarTextFeedback = false; //set true when expressionBar value is changed by code. This is used to filter feedback Messages
    
    //picking mode is enabled when a user starts editing a expression in expression-bar
    private boolean isPickingMode = false;
    
    /*
        Variables regarding saving, opening and exporting of a file
    */
    private boolean isLocationAvailable = false;
    private boolean hasChangesToSave = false; //are there any pending changes to be saved?
    private String fileName = "Spreadsheet";
    
    //only one save operation or open operation may run at once given time. The relavent particular threads references will be held here.
    private Thread openThread = null;
    private Thread saveThread = null;
    private Thread exportThread = null;
    private boolean pendingClose = false; //used to indicate whether any close operation is pending as a result of being blocked by a background thread
   
    
    //Fields regarding copy-paste system
    /*
        When a user copy, the cell value is sent to the clipboard. Hence whereever he may paste, cell value will be pasted.
        But, if he paste again in the spreadsheet, now we have to paste the expressions instead of cell values in clipboard
        Therefore, the following set of fields are required
    */
    private String lastCopiedValue = ""; //Used to check whether the value being pasted is same as the value that was last copied from spreadsheet. 
                                            // if so, user has not copied anything else after copying something from spreadsheet
                                            //therefore, user is pasting something copied from this spreadsheet
                                            //hence, can do the required translations
    private String lastCopiedExpression = ""; 
    private String lastCopiedCellRange = "";
    private serializables.SerializableTable lastCopiedSpreadsheet = null;
    private boolean clipboardFilled = false;
    private ClipboardOwner clipboardOwner = new ClipboardOwner() { // we do not require any special functionalities provided by clipboardOwner

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            //nothing to be done
        }
    };
    
    //Constructors
    
    /**
     * Creates new form SpreadsheetFrame with a new spreadsheet
     */
    public SpreadsheetFrame() {
        this(new Spreadsheet());
        
    }
    
    
    //Creates a new SpreadsheetFrame using currentSheet provided
    public SpreadsheetFrame(Spreadsheet currentSheet)
    {
        this.currentSheet = currentSheet;
        
        try {
            //set the icon            
            this.setIconImage(ImageIO.read(getClass().getResource("/spreadsheet/gui/Images/icon.png")));
        } catch (IOException ex) {
            Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        initComponents(); //init the UI components
        setupCellTable(); //init the jTable used to display the spreadsheet
        setupStyleUI(); //init the UI elements related to cell styling (Bold, Italic, Fonts...)
                
        /*
            Why close operations are managed manually?
                Different spreadsheets can be opened in different windows at same time.
                Therefore, we cant simply terminate the program because one such window is closed.
                Therefore, code is written for formClosing event to determine whether to terminate the program or not when a window is closed.
        */
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        
        //Init Spreadsheets listners
        currentSheet.addChangedListner(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) { //state changed is called whenever the content in the spreadsheet is changed. (Eg - change in cell expression, adding a new row or a column)
                reportChange();
            }
        });
        
        
        currentSheet.addUIChangedListner(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) { //state changed is called whenever new data becomes available to display in a cell which was being evaluated
                //Caution : This method may be called in a non gui thread. Hence, use SwingUtilities to access ui elements
               SwingUtilities.invokeLater(new Runnable() {

                   @Override
                   public void run() {
                       //ensure we are in the gui thread
                       jCellTable.repaint();
                   }
               });
            }
        });
        
         //center the window
        this.setLocationRelativeTo(null);
        
    }
    
    //public fields related to location of opened file. These methods are called when a new file is opened. 
    /*
        Whena a new file is opened, a new spreadsheet frame is displayed with its content. It's necessary to assign the location information of this displayed spreadsheet to the SpreadsheetFrame. 
        The following methods are used for that purpose
    */
    public boolean getLocationAvailability()
    {
        return isLocationAvailable;
    }
    public void setLocationAvailability(boolean value)
    {
        isLocationAvailable = value;
    }
    //return the fileName associated with the spreadsheet being opened
    public String getFileName()
    {
        return fileName;
    }
    //set the fileName of spreadsheet opened
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
        
    }
    
    //update the title bar of window
    private void updateTitle()
    {
        if(hasChangesToSave)
        {
            this.setTitle(fileName + "*");
        }
        else
        {
            this.setTitle(fileName);
        }
    }
    
    //should be called whenever spreadsheet is changed
    private void reportChange()
    {
        hasChangesToSave = true;
        updateTitle();
    }
    
    
    //update the isPickingMode field.
    //is picking mode is to define whether the UI is in a cell picking mode. (check the definition given at start of this file)
    private void setPickingMode(boolean value)
    {
        isPickingMode = value;
        //also set the readonly state
        tableModel.setAllCellsReadonly(value);
    }
    
    
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCellTableContextMenu = new javax.swing.JPopupMenu();
        popupCopy = new javax.swing.JMenuItem();
        popupPaste = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        popupNewColumn = new javax.swing.JMenuItem();
        popupNewRow = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        menuCellFormats = new javax.swing.JMenu();
        menuGeneralFormat = new javax.swing.JRadioButtonMenuItem();
        menuDecimalNumberFormat = new javax.swing.JRadioButtonMenuItem();
        menuPercentage = new javax.swing.JRadioButtonMenuItem();
        menuCurrency = new javax.swing.JMenu();
        menuRs = new javax.swing.JRadioButtonMenuItem();
        menuDollar = new javax.swing.JRadioButtonMenuItem();
        menuDateTime = new javax.swing.JRadioButtonMenuItem();
        jCellFormatMenu = new javax.swing.JPopupMenu();
        menuGeneralFormat2 = new javax.swing.JMenuItem();
        menuDecimal2 = new javax.swing.JMenuItem();
        menuPercentage2 = new javax.swing.JMenuItem();
        menuCurrency2 = new javax.swing.JMenu();
        menuRs2 = new javax.swing.JMenuItem();
        menuDollars2 = new javax.swing.JMenuItem();
        menuDateTime2 = new javax.swing.JMenuItem();
        popupCellAlignment = new javax.swing.JPopupMenu();
        menuAutoCellAlign = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        menuLeftCellAlign = new javax.swing.JMenuItem();
        menuCenterAlign = new javax.swing.JMenuItem();
        menuRightCellAlign = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        jCellTable = new javax.swing.JTable();
        jtxtFormulaBar = new javax.swing.JTextField();
        jbtnEvaluate = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jStatusField = new javax.swing.JLabel();
        jToolBar1 = new javax.swing.JToolBar();
        btnNew = new javax.swing.JButton();
        btnOpen = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jbtnCopy = new javax.swing.JButton();
        jbtnPaste = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        btnNewColumn = new javax.swing.JButton();
        btnNewRow = new javax.swing.JButton();
        btnRemoveRow = new javax.swing.JButton();
        btnRemoveColumn = new javax.swing.JButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        cmbFontFamilies = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        cmbFontSizes = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        btnBold = new javax.swing.JToggleButton();
        btnItalic = new javax.swing.JToggleButton();
        btnUnderline = new javax.swing.JToggleButton();
        jLabel3 = new javax.swing.JLabel();
        btnAlignment = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        btnFontColor = new javax.swing.JButton();
        btnCellColor = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        btnCurrentFormat = new javax.swing.JButton();
        txtSelectedCellId = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu2 = new javax.swing.JMenu();
        jmnNew = new javax.swing.JMenuItem();
        jmnOpen = new javax.swing.JMenuItem();
        jmnSave = new javax.swing.JMenuItem();
        jmnSaveAs = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        jMenu3 = new javax.swing.JMenu();
        menuExportCommaSeperatedValues = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        jmnExit = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        jMenuCopy = new javax.swing.JMenuItem();
        jMenuPaste = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        menuNewColumn = new javax.swing.JMenuItem();
        menuNewRow = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        menuDeleteColumn = new javax.swing.JMenuItem();
        menuDeleteRow = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        menuAbs = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        menuSum = new javax.swing.JMenuItem();
        menuAvg = new javax.swing.JMenuItem();
        menuMax = new javax.swing.JMenuItem();
        menuMin = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        menuIfGreaterThan = new javax.swing.JMenuItem();
        menuIfEquals = new javax.swing.JMenuItem();
        menuIfLessThan = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        menuSupportedFeatures = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        menuAbout = new javax.swing.JMenuItem();

        popupCopy.setText("Copy");
        popupCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popupCopyActionPerformed(evt);
            }
        });
        jCellTableContextMenu.add(popupCopy);

        popupPaste.setText("Paste");
        popupPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popupPasteActionPerformed(evt);
            }
        });
        jCellTableContextMenu.add(popupPaste);
        jCellTableContextMenu.add(jSeparator5);

        popupNewColumn.setText("Insert Column(s)");
        popupNewColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popupNewColumnActionPerformed(evt);
            }
        });
        jCellTableContextMenu.add(popupNewColumn);

        popupNewRow.setText("Insert Row(s)");
        popupNewRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popupNewRowActionPerformed(evt);
            }
        });
        jCellTableContextMenu.add(popupNewRow);
        jCellTableContextMenu.add(jSeparator6);

        menuCellFormats.setText("Cell Format");

        menuGeneralFormat.setSelected(true);
        menuGeneralFormat.setText("General");
        menuGeneralFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuGeneralFormatActionPerformed(evt);
            }
        });
        menuCellFormats.add(menuGeneralFormat);

        menuDecimalNumberFormat.setSelected(true);
        menuDecimalNumberFormat.setText("Decimal Number");
        menuDecimalNumberFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDecimalNumberFormatActionPerformed(evt);
            }
        });
        menuCellFormats.add(menuDecimalNumberFormat);

        menuPercentage.setSelected(true);
        menuPercentage.setText("Percentage");
        menuPercentage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPercentageActionPerformed(evt);
            }
        });
        menuCellFormats.add(menuPercentage);

        menuCurrency.setText("Currency");

        menuRs.setSelected(true);
        menuRs.setText("Rupee (Rs. )");
        menuRs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRsActionPerformed(evt);
            }
        });
        menuCurrency.add(menuRs);

        menuDollar.setSelected(true);
        menuDollar.setText("Dollars ($)");
        menuDollar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDollarActionPerformed(evt);
            }
        });
        menuCurrency.add(menuDollar);

        menuCellFormats.add(menuCurrency);

        menuDateTime.setSelected(true);
        menuDateTime.setText("Date/Time");
        menuDateTime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDateTimeActionPerformed(evt);
            }
        });
        menuCellFormats.add(menuDateTime);

        jCellTableContextMenu.add(menuCellFormats);

        menuGeneralFormat2.setText("General");
        menuGeneralFormat2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuGeneralFormat2ActionPerformed(evt);
            }
        });
        jCellFormatMenu.add(menuGeneralFormat2);

        menuDecimal2.setText("Decimal");
        menuDecimal2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDecimal2ActionPerformed(evt);
            }
        });
        jCellFormatMenu.add(menuDecimal2);

        menuPercentage2.setText("Percentage");
        menuPercentage2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPercentage2ActionPerformed(evt);
            }
        });
        jCellFormatMenu.add(menuPercentage2);

        menuCurrency2.setText("Currency");

        menuRs2.setText("Rupee (Rs. )");
        menuRs2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRs2ActionPerformed(evt);
            }
        });
        menuCurrency2.add(menuRs2);

        menuDollars2.setText("Dollars ($)");
        menuDollars2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDollars2ActionPerformed(evt);
            }
        });
        menuCurrency2.add(menuDollars2);

        jCellFormatMenu.add(menuCurrency2);

        menuDateTime2.setText("DateTime");
        menuDateTime2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDateTime2ActionPerformed(evt);
            }
        });
        jCellFormatMenu.add(menuDateTime2);

        menuAutoCellAlign.setText("Auto");
        menuAutoCellAlign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAutoCellAlignActionPerformed(evt);
            }
        });
        popupCellAlignment.add(menuAutoCellAlign);
        popupCellAlignment.add(jSeparator11);

        menuLeftCellAlign.setText("Left");
        menuLeftCellAlign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuLeftCellAlignActionPerformed(evt);
            }
        });
        popupCellAlignment.add(menuLeftCellAlign);

        menuCenterAlign.setText("Center");
        menuCenterAlign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCenterAlignActionPerformed(evt);
            }
        });
        popupCellAlignment.add(menuCenterAlign);

        menuRightCellAlign.setText("Right");
        menuRightCellAlign.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuRightCellAlignActionPerformed(evt);
            }
        });
        popupCellAlignment.add(menuRightCellAlign);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Spreadsheet");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jCellTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jCellTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        jScrollPane1.setViewportView(jCellTable);

        jtxtFormulaBar.setEditable(false);
        jtxtFormulaBar.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent evt) {
                jtxtFormulaBarCaretUpdate(evt);
            }
        });
        jtxtFormulaBar.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jtxtFormulaBarFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jtxtFormulaBarFocusLost(evt);
            }
        });
        jtxtFormulaBar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jtxtFormulaBarKeyPressed(evt);
            }
        });

        jbtnEvaluate.setText("Evaluate");
        jbtnEvaluate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnEvaluateActionPerformed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        jStatusField.setText("Ready");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jStatusField)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jStatusField)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        btnNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/document_16xLG.png"))); // NOI18N
        btnNew.setToolTipText("New Spreadsheet");
        btnNew.setFocusable(false);
        btnNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewActionPerformed(evt);
            }
        });
        jToolBar1.add(btnNew);

        btnOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/folder_Open_16xLG.png"))); // NOI18N
        btnOpen.setToolTipText("Open");
        btnOpen.setFocusable(false);
        btnOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenActionPerformed(evt);
            }
        });
        jToolBar1.add(btnOpen);

        btnSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/save_16xLG.png"))); // NOI18N
        btnSave.setToolTipText("Save");
        btnSave.setFocusable(false);
        btnSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jToolBar1.add(btnSave);
        jToolBar1.add(jSeparator3);

        jbtnCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Copy_6524.png"))); // NOI18N
        jbtnCopy.setToolTipText("Copy");
        jbtnCopy.setFocusable(false);
        jbtnCopy.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbtnCopy.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jbtnCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnCopyActionPerformed(evt);
            }
        });
        jToolBar1.add(jbtnCopy);

        jbtnPaste.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Paste_6520.png"))); // NOI18N
        jbtnPaste.setToolTipText("Paste");
        jbtnPaste.setFocusable(false);
        jbtnPaste.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jbtnPaste.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jbtnPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jbtnPasteActionPerformed(evt);
            }
        });
        jToolBar1.add(jbtnPaste);
        jToolBar1.add(jSeparator4);

        btnNewColumn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/InsertColumn_5626.png"))); // NOI18N
        btnNewColumn.setToolTipText("Insert Column(s) - Select a number of columns to match the number of new columns required, starting from where new columns should be added");
        btnNewColumn.setFocusable(false);
        btnNewColumn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnNewColumn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnNewColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewColumnActionPerformed(evt);
            }
        });
        jToolBar1.add(btnNewColumn);

        btnNewRow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/TableLayoutPanel_6241.png"))); // NOI18N
        btnNewRow.setToolTipText("New Row(s) - Insert new rows to match the selected number of rows, shifting the set of selected rows down.");
        btnNewRow.setFocusable(false);
        btnNewRow.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnNewRow.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnNewRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewRowActionPerformed(evt);
            }
        });
        jToolBar1.add(btnNewRow);

        btnRemoveRow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Clearwindowcontent_6304.png"))); // NOI18N
        btnRemoveRow.setToolTipText("Remove Row");
        btnRemoveRow.setFocusable(false);
        btnRemoveRow.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnRemoveRow.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnRemoveRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveRowActionPerformed(evt);
            }
        });
        jToolBar1.add(btnRemoveRow);

        btnRemoveColumn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/DeleteColumn_5627.png"))); // NOI18N
        btnRemoveColumn.setToolTipText("Remove Column");
        btnRemoveColumn.setFocusable(false);
        btnRemoveColumn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnRemoveColumn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnRemoveColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveColumnActionPerformed(evt);
            }
        });
        jToolBar1.add(btnRemoveColumn);
        jToolBar1.add(jSeparator10);

        cmbFontFamilies.setEditable(true);
        cmbFontFamilies.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbFontFamilies.setToolTipText("Font Family");
        cmbFontFamilies.setMaximumSize(new java.awt.Dimension(200, 32767));
        cmbFontFamilies.setPreferredSize(new java.awt.Dimension(150, 20));
        cmbFontFamilies.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbFontFamiliesActionPerformed(evt);
            }
        });
        jToolBar1.add(cmbFontFamilies);

        jLabel1.setText(" ");
        jToolBar1.add(jLabel1);

        cmbFontSizes.setEditable(true);
        cmbFontSizes.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Default", "11", "12", "14", "16" }));
        cmbFontSizes.setToolTipText("Font Size");
        cmbFontSizes.setMaximumSize(new java.awt.Dimension(70, 32767));
        cmbFontSizes.setMinimumSize(new java.awt.Dimension(70, 20));
        cmbFontSizes.setPreferredSize(new java.awt.Dimension(70, 20));
        cmbFontSizes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbFontSizesActionPerformed(evt);
            }
        });
        jToolBar1.add(cmbFontSizes);

        jLabel2.setText(" ");
        jToolBar1.add(jLabel2);

        btnBold.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Bold_11689.png"))); // NOI18N
        btnBold.setToolTipText("Bold");
        btnBold.setFocusable(false);
        btnBold.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnBold.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnBold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBoldActionPerformed(evt);
            }
        });
        jToolBar1.add(btnBold);

        btnItalic.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Italic_11693.png"))); // NOI18N
        btnItalic.setToolTipText("Italic");
        btnItalic.setFocusable(false);
        btnItalic.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnItalic.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnItalic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnItalicActionPerformed(evt);
            }
        });
        jToolBar1.add(btnItalic);

        btnUnderline.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Underline_11700.png"))); // NOI18N
        btnUnderline.setToolTipText("Underline");
        btnUnderline.setFocusable(false);
        btnUnderline.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnUnderline.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnUnderline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUnderlineActionPerformed(evt);
            }
        });
        jToolBar1.add(btnUnderline);

        jLabel3.setText(" ");
        jToolBar1.add(jLabel3);

        btnAlignment.setText("Auto");
        btnAlignment.setToolTipText("Text Alignment");
        btnAlignment.setFocusable(false);
        btnAlignment.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnAlignment.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnAlignment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAlignmentActionPerformed(evt);
            }
        });
        jToolBar1.add(btnAlignment);

        jLabel4.setText(" ");
        jToolBar1.add(jLabel4);

        btnFontColor.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Color_fontHS.png"))); // NOI18N
        btnFontColor.setToolTipText("Font Colour");
        btnFontColor.setFocusable(false);
        btnFontColor.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnFontColor.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnFontColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFontColorActionPerformed(evt);
            }
        });
        jToolBar1.add(btnFontColor);

        btnCellColor.setIcon(new javax.swing.ImageIcon(getClass().getResource("/spreadsheet/gui/Images/Color_linecolor.png"))); // NOI18N
        btnCellColor.setToolTipText("Cell Colour");
        btnCellColor.setFocusable(false);
        btnCellColor.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnCellColor.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnCellColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCellColorActionPerformed(evt);
            }
        });
        jToolBar1.add(btnCellColor);
        jToolBar1.add(filler1);

        btnCurrentFormat.setText("General");
        btnCurrentFormat.setFocusable(false);
        btnCurrentFormat.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnCurrentFormat.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnCurrentFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCurrentFormatActionPerformed(evt);
            }
        });
        jToolBar1.add(btnCurrentFormat);

        txtSelectedCellId.setEditable(false);
        txtSelectedCellId.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtSelectedCellId.setText("No Selection");

        jMenu2.setText("File");

        jmnNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        jmnNew.setText("New");
        jmnNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmnNewActionPerformed(evt);
            }
        });
        jMenu2.add(jmnNew);

        jmnOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jmnOpen.setText("Open");
        jmnOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmnOpenActionPerformed(evt);
            }
        });
        jMenu2.add(jmnOpen);

        jmnSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jmnSave.setText("Save");
        jmnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmnSaveActionPerformed(evt);
            }
        });
        jMenu2.add(jmnSave);

        jmnSaveAs.setText("Save As");
        jmnSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmnSaveAsActionPerformed(evt);
            }
        });
        jMenu2.add(jmnSaveAs);
        jMenu2.add(jSeparator1);

        jMenu3.setText("Export");

        menuExportCommaSeperatedValues.setText("Comma Seperated Values (*.csv)");
        menuExportCommaSeperatedValues.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExportCommaSeperatedValuesActionPerformed(evt);
            }
        });
        jMenu3.add(menuExportCommaSeperatedValues);

        jMenu2.add(jMenu3);
        jMenu2.add(jSeparator2);

        jmnExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        jmnExit.setText("Exit");
        jmnExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jmnExitActionPerformed(evt);
            }
        });
        jMenu2.add(jmnExit);

        jMenuBar1.add(jMenu2);

        jMenuEdit.setText("Edit");

        jMenuCopy.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        jMenuCopy.setText("Copy");
        jMenuCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCopyActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuCopy);

        jMenuPaste.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        jMenuPaste.setText("Paste");
        jMenuPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuPasteActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuPaste);

        jMenuBar1.add(jMenuEdit);

        jMenu4.setText("Format");

        menuNewColumn.setText("New Column");
        menuNewColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuNewColumnActionPerformed(evt);
            }
        });
        jMenu4.add(menuNewColumn);

        menuNewRow.setText("New Row");
        menuNewRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuNewRowActionPerformed(evt);
            }
        });
        jMenu4.add(menuNewRow);
        jMenu4.add(jSeparator7);

        menuDeleteColumn.setText("Delete Column");
        menuDeleteColumn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDeleteColumnActionPerformed(evt);
            }
        });
        jMenu4.add(menuDeleteColumn);

        menuDeleteRow.setText("Delete Row");
        menuDeleteRow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuDeleteRowActionPerformed(evt);
            }
        });
        jMenu4.add(menuDeleteRow);

        jMenuBar1.add(jMenu4);

        jMenu5.setText("Formula");

        menuAbs.setText("Abs");
        menuAbs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAbsActionPerformed(evt);
            }
        });
        jMenu5.add(menuAbs);
        jMenu5.add(jSeparator8);

        menuSum.setText("Sum");
        menuSum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSumActionPerformed(evt);
            }
        });
        jMenu5.add(menuSum);

        menuAvg.setText("Avg");
        menuAvg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAvgActionPerformed(evt);
            }
        });
        jMenu5.add(menuAvg);

        menuMax.setText("Max");
        menuMax.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuMaxActionPerformed(evt);
            }
        });
        jMenu5.add(menuMax);

        menuMin.setText("Min");
        menuMin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuMinActionPerformed(evt);
            }
        });
        jMenu5.add(menuMin);
        jMenu5.add(jSeparator9);

        menuIfGreaterThan.setText("If (expression > 0)");
        menuIfGreaterThan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuIfGreaterThanActionPerformed(evt);
            }
        });
        jMenu5.add(menuIfGreaterThan);

        menuIfEquals.setText("If (expression = 0)");
        menuIfEquals.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuIfEqualsActionPerformed(evt);
            }
        });
        jMenu5.add(menuIfEquals);

        menuIfLessThan.setText("if (expression < 0)");
        menuIfLessThan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuIfLessThanActionPerformed(evt);
            }
        });
        jMenu5.add(menuIfLessThan);

        jMenuBar1.add(jMenu5);

        jMenu6.setText("Help");

        menuSupportedFeatures.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        menuSupportedFeatures.setText("Supported Features");
        menuSupportedFeatures.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSupportedFeaturesActionPerformed(evt);
            }
        });
        jMenu6.add(menuSupportedFeatures);
        jMenu6.add(jSeparator12);

        menuAbout.setText("About");
        menuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        jMenu6.add(menuAbout);

        jMenuBar1.add(jMenu6);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 832, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(txtSelectedCellId, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jtxtFormulaBar)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jbtnEvaluate)))
                .addContainerGap())
            .addComponent(jToolBar1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jtxtFormulaBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jbtnEvaluate)
                    .addComponent(txtSelectedCellId, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 330, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
           
        updateTitle();
        jCellTable.requestFocus();
    }//GEN-LAST:event_formWindowOpened

    //setup the jTable cellTable used to preview the spreadsheet
    private void setupCellTable()
    {
        
        //Setup the CellTableModel
        //Link spreadsheet data to jtable via a custom table model
        tableModel = new CellTableModel();        
        tableModel.setSpreadsheet(currentSheet); 
        tableModel.setUICellTable(jCellTable);        
        jCellTable.setModel(tableModel);        
        
        jCellTable.setCellSelectionEnabled(true); //allow individual cell selection
        
        //assign a popup menu for jCellTable
        jCellTable.setComponentPopupMenu(jCellTableContextMenu);
        
        //The first column will serve as the row-header
        //Therefore assign the custom RowHeader renderer
        TableColumn rowHeader = jCellTable.getColumnModel().getColumn(0);
        rowHeader.setCellRenderer(new RowHeaderRenderer());
        //Set row-header dimensions
        rowHeader.setWidth(30);
        rowHeader.setResizable(false);
        rowHeader.setMaxWidth(30);
        rowHeader.setPreferredWidth(30);
        rowHeader.setHeaderValue("");
        
        //Sets up in-place cell editing
        SpreadsheetCellEditor cellEditor = new SpreadsheetCellEditor(currentSheet);
        jCellTable.setDefaultEditor(Object.class, cellEditor);
        
        //add cell editor listner        
        cellEditor.addCellEditorListener(new SpreadsheetCellEditorAdapter(){

              
            @Override
            public void cellEditingBegun() {
                //start cell highlights
                 updateSpreadsheetHighlights();
            }

            @Override
            public void cellTextChanged(String newText, DocumentEvent documentEvent) {
                //update the text in formula bar to match the cell expression being edited
                jtxtFormulaBar.setText(newText);
            }

            @Override
            public void cellCaretUpdated(CaretEvent e) {
                //reflect the changes of caret position of cell editor in the formula bar
                jtxtFormulaBar.setSelectionStart(e.getMark());
                jtxtFormulaBar.setSelectionEnd(e.getDot());
            }

           
        });
        
                
        //a custom cell-renderer is used to provide ui enhancements
        //this renderer does cell highlights (desribed at the begining of this file)
        cellRenderer = new SpreadsheetCellRenderer();
        cellRenderer.setSpreadsheet(currentSheet);
        jCellTable.setDefaultRenderer(Object.class, cellRenderer);
        
        //prevent column re-ordering
        jCellTable.getTableHeader().setReorderingAllowed(false);
        
        
        //Setup selection responses for changes in selected columnIndex and rowIndex
        jCellTable.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                SpreadsheetFrame.this.cellTableSelectionChanged(e);
            }
        });
        
        ListSelectionModel cellSelModel = jCellTable.getSelectionModel();       
        cellSelModel.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                SpreadsheetFrame.this.cellTableSelectionChanged(e);
            }
        });
        
        
        
        //setup ui highlight system
        //a listner is needed to update cellHighlights when the text in addressBar is modified
        
        jtxtFormulaBar.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSpreadsheetHighlights();
                if(avoidExpressionBarTextFeedback) //filter feeedbacks
                    return;
                //if the user has focus in formulaBar, switch to picking mode
                if(jtxtFormulaBar.hasFocus() && jtxtFormulaBar.getText().startsWith("="))
                {
                    setPickingMode(true);                    
                }
                else
                {
                    if(avoidSelectionFeedback) //filter feedbacks
                        return;
                    setPickingMode(false);
                 }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                //same as insertUpdate
                insertUpdate(e);
                            
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                //same as insertUpdate 
                insertUpdate(e);
                
                
            }
        });
    
    }
    
     //retrieve the currently selected cell or cell-range as a text (think about the Cell Name indicated in the left side of addressbar in MS Excel)
    private String getSelectionId()
    {
        //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       if(minCol < 1 || minRow < 0)
           return "";
       
       if(minRow == maxRow && minCol ==maxCol)
       {
           //single cell is selected. derive its cell name
           String columnName = Spreadsheet.indexToColumnName(minCol - 1); //first column reserved for row ids
           String cellName = columnName + String.valueOf(minRow+1);
           
           return cellName;
       }
       else
       {
           //multiple cells selected. identify it as a cell range
           //derive the cell id for particular range
           String columnName1 = Spreadsheet.indexToColumnName(minCol - 1); //first column reserved for row ids
           String cellName1 = columnName1 + String.valueOf(minRow+1);
           
           String columnName2 = Spreadsheet.indexToColumnName(maxCol - 1); //first column reserved for row ids
           String cellName2 = columnName2 + String.valueOf(maxRow+1);
           
           
           return cellName1 + ":" + cellName2;
       }
    }
    
    
    //notify the cellRenderer about changes in cellHighlights   
    void updateSpreadsheetHighlights()
    {
        if(isExpressionEditing())
        {
             //we need to highlight the relavent cells to the expression
             cellRenderer.getCellHighlights().clear(); //clear previous highlights
             String expression = jtxtFormulaBar.getText();
             
             if(expression.trim().startsWith("="))
             {
                 //so its an expression, lets highlight cell references
                 //tokenize
                 ArrayList<parser.ExpressionElement> elements = parser.ExpressionTokenizer.tokenizeExpressionElements(expression, new Parser()); //no need to provide a proper parser since we are not going to evaluate the expression
                 
                 //find the tokens which are cell references
                 for(ExpressionElement element : elements)
                 {
                     if(element instanceof TokenElement)
                     {
                        if(Cell.validateCellId(element.getStringValue()))
                        {
                            //its a cell reference.
                            String cellId = element.getStringValue().trim();
                            //find the row-index and columnindex
                            int rowIndex = Integer.parseInt(cellId.split("[A-Z]+")[1]) - 1;
                            int columnIndex = Spreadsheet.columnNameToIndex(cellId.split("[0-9]+")[0]) + 1;

                            //highlight it
                            cellRenderer.getCellHighlights().add(new CellHighlight(columnIndex, columnIndex, rowIndex, rowIndex));
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
                            
                            //highlight it
                            cellRenderer.getCellHighlights().add(new CellHighlight(startColumnIndex, endColumnIndex, startRowIndex, endRowIndex));

                        }
                     }
                 }
             }
        }
        else
        {
            cellRenderer.getCellHighlights().clear();
        }
       
       //repaint ui
       jCellTable.repaint();
    }
    
     private void highlightCaretElement(int position)
    {
        /*
        IDEA: idea is to highlight the cell relavent to current selection in expression using a special colour. (using ActiveCellHighlight)
            eg - Orange
            
        */        
        //only works when expression is being edited
        if(!isExpressionEditing())
            return;
        
        int cursorPosition = position; //pointOFInterest. we are analysing the element at this position
        
        String expression = jtxtFormulaBar.getText();
        if(expression.trim().startsWith("=") && cursorPosition > 0) //is it an expression?
        {
            //now lets try to identify the element at the location of cursor
            Parser p = new Parser();
            p.setCellProvider(currentSheet);
            parser.TokenIdentificationResult identificationResult;
            //identify element at cursor position
            identificationResult  = p.identifyTokenAt(expression, cursorPosition-1);
            
            //refresh cellHighlights (the gray colour highlights)
            updateSpreadsheetHighlights();
            
            //We will need to highlight appropriate cells with special colour if current element is a cell reference
            
            
            if(identificationResult != null)
            {               
                //is it a cell reference?
                ExpressionElement currentElement = identificationResult.getObservedElement();
                if(currentElement instanceof TokenElement)
                {
                    TokenElement tokenElement = (TokenElement)currentElement;
                    if(Cell.validateCellId(tokenElement.getStringValue()))
                    {
                        //yes, its a cell reference                        
                        
                        String cellId = tokenElement.getStringValue().trim();
                        //find the row-index and columnindex
                        int rowIndex = Integer.parseInt(cellId.split("[A-Z]+")[1]) - 1;
                        int columnIndex = Spreadsheet.columnNameToIndex(cellId.split("[0-9]+")[0]) + 1;

                        //we need to prepend in order to ensure active_cell_highlight gets the priority
                        cellRenderer.getCellHighlights().add(0,new ActiveCellHighlight(columnIndex, columnIndex, rowIndex, rowIndex));
                        
                    }
                     //or it could be a cell range
                    else if(tokenElement.getStringValue().matches("[A-Z]+[0-9]+\\:[A-Z]+[0-9]+"))
                    {
                        //its a cell-range
                        //separate two cell references
                        String[] parts = tokenElement.getStringValue().split("\\:");
                        //start reference
                        String startReference = parts[0];
                        //separate start row index and start column index
                        int startRowIndex = Integer.parseInt(startReference.split("[A-Z]+")[1]) - 1;
                        int startColumnIndex = Spreadsheet.columnNameToIndex(startReference.split("[0-9]+")[0])+1;

                        //end reference
                        String endReference = parts[1];
                        
                        //sperate end row index and end column index
                        int endRowIndex = Integer.parseInt(endReference.split("[A-Z]+")[1]) - 1;
                        int endColumnIndex = Spreadsheet.columnNameToIndex(endReference.split("[0-9]+")[0])+1;
                        
                        //we need to prepend in order to ensure active_cell_highlight gets the priority
                        cellRenderer.getCellHighlights().add(0,new ActiveCellHighlight(startColumnIndex, endColumnIndex, startRowIndex, endRowIndex));

                    }
                }
                else
                {
                    //no selection from cursor
                    
                }
            }
                
        }
    }
    
     
    public void cellTableSelectionChanged(ListSelectionEvent e) {
        //Selection has changed
       jtxtFormulaBar.setEditable(true);
        if(avoidSelectionFeedback)
            return;
        
        
        if(isPickingMode)
        {
            
            //cell picking mode
            if(!e.getValueIsAdjusting()) //dont do anything if selection is still being changed
            {
                
                if(editingColumnId == jCellTable.getSelectedColumn() && editingRowId == jCellTable.getSelectedRow())
                {
                    //its the feedback due to reselecting original cell
                    //ignore
                    return;
                }
                
                
                String expression = jtxtFormulaBar.getText();
                int cursorPosition = jtxtFormulaBar.getCaretPosition();
                
                if(cursorPosition != 0) //if cursor is before the = sign, cant do anything
                {
                
                    //observe the expression
                    Parser p = new Parser();
                    p.setCellProvider(currentSheet);
                    parser.TokenIdentificationResult observationResult;
                    observationResult  = p.identifyTokenAt(expression,  cursorPosition-1);

                    String selectionId = getSelectionId(); //textual representation of the pick user made (eg A1:A5, A3)
                    if(observationResult == null)
                    {
                        //block feedback signals
                        avoidExpressionBarTextFeedback = true;
                        //add to charachter position
                        jtxtFormulaBar.setText(expression.substring(0,cursorPosition) + selectionId + expression.substring(cursorPosition));
                        //move selection to end of append
                        jtxtFormulaBar.setSelectionStart((expression.substring(0,cursorPosition) + selectionId).length() );
                        jtxtFormulaBar.setSelectionEnd((expression.substring(0,cursorPosition) + selectionId).length() );
                        //reset feedback filter
                        avoidExpressionBarTextFeedback = false;
                    }
                    else
                    {

                        if(observationResult.getObservedElement() instanceof DelimeterElement)
                        {
                            //append after the delimeter
                            
                            //avoid feedback signals
                            avoidExpressionBarTextFeedback = true;
                            
                            jtxtFormulaBar.setText(expression.substring(0,observationResult.getEndIndex()) + selectionId + expression.substring(observationResult.getEndIndex()));
                            //move cursor to end of append
                            jtxtFormulaBar.setSelectionStart(observationResult.getEndIndex()+ selectionId.length());
                            jtxtFormulaBar.setSelectionEnd(observationResult.getEndIndex() + selectionId.length());
                            //reset feedback filter
                            avoidExpressionBarTextFeedback = false;
                        }
                        else if(observationResult.getObservedElement() instanceof TokenElement)
                        {
                            //replace token element
                            
                            avoidExpressionBarTextFeedback = true; //avoid feedback signals
                            jtxtFormulaBar.setText(expression.substring(0,observationResult.getStartIndex()) + selectionId + expression.substring(observationResult.getEndIndex()));
                            //move cursor to end of replaced text
                            jtxtFormulaBar.setSelectionStart(observationResult.getStartIndex() + selectionId.length());
                            jtxtFormulaBar.setSelectionEnd(observationResult.getStartIndex() + selectionId.length());

                            avoidExpressionBarTextFeedback = false; //reset feedback filter
                        }
                    }
                }
               
                
                //regain lost ui focus
                avoidSelectionFeedback=true; //block feedbacks
                jCellTable.setRowSelectionInterval(editingRowId, editingRowId);
                jCellTable.setColumnSelectionInterval(editingColumnId,editingColumnId);
                avoidSelectionFeedback = false; //reset filter
                
                jtxtFormulaBar.requestFocusInWindow();
                   
               
            
            }
            //update ui highlights
             highlightCaretElement(jtxtFormulaBar.getCaretPosition());
        }
        else
        {

             //its a normal cell selection. (not a cell pick)

              editingColumnId = jCellTable.getSelectedColumn();
            editingRowId = jCellTable.getSelectedRow();
            
            refreshSelectionUI();
        }
        
             
       
        
       
    }
    
    
    
    private void jbtnEvaluateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnEvaluateActionPerformed
        
        //reset the selection system
        setPickingMode(false);             
        
        
        if(jCellTable.getSelectedColumn() == 0)
            return; //row header is seleced. cant put the expression there.
        
        String columnName = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1); //-1 to leave the row_headers
        String cellName = columnName + String.valueOf(jCellTable.getSelectedRow()+1);
        
        //set the expression
        currentSheet.setCellExpression(cellName, jtxtFormulaBar.getText());        
        
          
        //stop editing at the cell ui since we are editing at the addressbar
        if(jCellTable.getCellEditor() != null)
            jCellTable.getCellEditor().cancelCellEditing();
            
        jCellTable.repaint();
        jCellTable.requestFocus();    
       
    }//GEN-LAST:event_jbtnEvaluateActionPerformed

    private void jtxtFormulaBarKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jtxtFormulaBarKeyPressed
        if(evt.getKeyCode() == KeyEvent.VK_ENTER)
        {
            //user has pressed enter, after typing an expression
            jbtnEvaluate.doClick();
        }
    }//GEN-LAST:event_jtxtFormulaBarKeyPressed

    private void jtxtFormulaBarFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtFormulaBarFocusGained
        if(jtxtFormulaBar.getText().startsWith("=")) //is user typing an expression?
        {
            //yes! switch to picking mode
            setPickingMode(true);
            updateSpreadsheetHighlights();
            //use ActiveCellHighlight to highlight the element represented by current caret position
            highlightCaretElement(jtxtFormulaBar.getCaretPosition());
        }
        
        
        
    }//GEN-LAST:event_jtxtFormulaBarFocusGained
    
    private void jtxtFormulaBarFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jtxtFormulaBarFocusLost
        //remove cell highlights
        cellRenderer.getCellHighlights().clear();
        jCellTable.repaint();
    }//GEN-LAST:event_jtxtFormulaBarFocusLost
    
    //check whether the user is currently editing a cell expression
    private boolean isExpressionEditing()
    {
        if(isPickingMode)
            return true;
        if(jtxtFormulaBar.isFocusOwner())
            return true;
        if(jCellTable.isEditing())
            return true;
        return false;
    }
    
    //update the highlights. highlight the cell referring to cursor position using a special colour
    
 
   
    private void newSpreadsheet()
    {
         //new spreadsheet in a new window
         SpreadsheetFrame frm = new SpreadsheetFrame();
         frm.setVisible(true);
    }
    private void jmnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmnNewActionPerformed
        newSpreadsheet();
    }//GEN-LAST:event_jmnNewActionPerformed

    private void jmnExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmnExitActionPerformed
        //exit
        this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }//GEN-LAST:event_jmnExitActionPerformed
    
    //save the spreadSheet to saveFile
    private void saveTo(final java.io.File saveFile) 
    {
        if(saveThread != null)
        {
            //we already have a save operation pending. wait for it to finish.
            JOptionPane.showMessageDialog(this, "Please wait until pending save operation completes and click Save again.","Error - Pending Save Operation" , JOptionPane.OK_OPTION);
            return;
        }
        
        //prepare serializable
        final serializables.SerializableTable serializableTable = currentSheet.toSerializableTable();
        jStatusField.setText("Saving...");
        
        final boolean previousHasChangesToSave = hasChangesToSave; //remember previous changed state
        hasChangesToSave = false; // all changes done upto now will be saved soon. hence, set hasChangesToSave false
        
        setReadOnly(true);
        
        //make save thread
        saveThread = new Thread(new Runnable() {

            @Override
            public void run() {
                //hold references to connections
                ObjectOutputStream objectOutputStream = null;
                FileOutputStream outputStream = null;
                try {
                    //open output file
                    outputStream = new FileOutputStream(saveFile);
                    objectOutputStream = new ObjectOutputStream(outputStream);
                    //do writing
                    objectOutputStream.writeObject(serializableTable);
                    
                    //files should be closed before ui is updated
                    objectOutputStream.close();
                    outputStream.close();
                    
                    
                    //so that finally block will not try to re-close
                    objectOutputStream = null;
                    outputStream = null;
                    
                    SwingUtilities.invokeLater(new Runnable() {
                        //saving is completed
                        //back to ui thread
                        @Override
                        public void run() {
                            isLocationAvailable = true;
                            fileName = saveFile.getPath();
                                                        
                            updateTitle();
                        }
                    });
                    
                } catch (final FileNotFoundException ex) {
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            //back to ui thread
                            JOptionPane.showMessageDialog(SpreadsheetFrame.this, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
                            hasChangesToSave = previousHasChangesToSave;
                        }
                    });
                    

                    Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                } catch (final IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                         
                        @Override
                        public void run() {
                            //back to uiThread
                            JOptionPane.showMessageDialog(SpreadsheetFrame.this, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
                            hasChangesToSave = previousHasChangesToSave;
                        }
                    });
                    
                    Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally
                {
                    //if the connections are still open, then close.
                    if(objectOutputStream!= null)
                         try {
                             objectOutputStream.close();
                             objectOutputStream = null;
                    } catch (IOException ex) {
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if(outputStream != null)
                        try {
                            outputStream.close();
                            outputStream = null;
                    } catch (IOException ex) {
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        //back to ui thread
                        @Override
                        public void run() {
                            jStatusField.setText("Ready");
                            saveThread = null;
                            
                            setReadOnly(false); //re-enable ui
                            
                            if(pendingClose)
                               performCloseAndCleanup();
                        }
                    });
                    
                }
                
                
            }
        }, "Save Thread");
        
        saveThread.start();
        
       
        
    }
    //show the saveAs dialog if required. do what happens when u click save
    private void saveWithPrompt()
    {
        if(isLocationAvailable)
        {
            saveTo(new File(fileName));
        }
        else
        {
            saveAsWithPrompt();
        }
    }
   
    //do what happens when user clicks saveAs
    private void saveAsWithPrompt()
    {
        //prompt from user
        JFileChooser saveFileDg = new JFileChooser();
        
        FileNameExtensionFilter filter =  new FileNameExtensionFilter("Spreadsheet File (*.spr)", "spr");
        saveFileDg.addChoosableFileFilter(filter);
        saveFileDg.setFileFilter(filter);
        
        if(saveFileDg.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            
            

            File selectedFile = saveFileDg.getSelectedFile();
            if(!saveFileDg.getSelectedFile().getAbsoluteFile().toString().toLowerCase().endsWith(".spr"))
            {
                selectedFile = new File(saveFileDg.getSelectedFile() + ".spr");

            }

            saveTo(selectedFile);
                
            
            
        }
    }
    //Save file
    private void jmnSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmnSaveAsActionPerformed
       saveAsWithPrompt();
    }//GEN-LAST:event_jmnSaveAsActionPerformed
    
    private void openWithPrompt()
    {
        if(openThread != null)
        {
            //already an open operation is pending
            JOptionPane.showMessageDialog(this, "An open operation is pending. Please wait until it finishes.", "Pending Operation", JOptionPane.OK_OPTION);
            return;
        }
        //prompt from user
        final JFileChooser openFileDg = new JFileChooser();
        openFileDg.setMultiSelectionEnabled(false);
        FileNameExtensionFilter filter =  new FileNameExtensionFilter("Spreadsheet File (*.spr)", "spr");
        openFileDg.addChoosableFileFilter(filter);
        openFileDg.setFileFilter(filter);
        
        if(openFileDg.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            final File selectedFile = openFileDg.getSelectedFile();
            jStatusField.setText("Opening " + openFileDg.getSelectedFile().getName() + "...");
            //setup openThread
            openThread = new Thread(new Runnable() {

                @Override
                public void run() {
                    FileInputStream inputStream = null;
                    ObjectInputStream objectStream = null;
                    try {
                        //open input stream
                        inputStream = new FileInputStream(selectedFile);
                        objectStream = new ObjectInputStream(inputStream);
                        //do reading
                        final serializables.SerializableTable serializableTable = (serializables.SerializableTable) objectStream.readObject();
                        
                        //close connections
                        objectStream.close();
                        inputStream.close();
                        
                        //set to null so finally block will no reclose these
                        objectStream = null;
                        inputStream = null;
                        
                        //return to uiThread
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                //convert to a sheet
                                Spreadsheet newSpreadsheet = Spreadsheet.fromSerializableTable(serializableTable);
                                //initialize a new frame to display newly opened spreadsheet
                                SpreadsheetFrame frm = new SpreadsheetFrame(newSpreadsheet);
                                frm.setLocationAvailability(true);
                                frm.setFileName(openFileDg.getSelectedFile().getPath());
                                frm.setVisible(true);
                            }
                        });
                       

                    } catch (final FileNotFoundException ex) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                //UI Thread
                                JOptionPane.showMessageDialog(SpreadsheetFrame.this, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
                            }
                        });
                        
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (final IOException ex) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                //UI Thread
                                JOptionPane.showMessageDialog(SpreadsheetFrame.this, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
                            }
                        });
                        
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (final ClassNotFoundException ex) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(SpreadsheetFrame.this, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
                            }
                        });
                        
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    finally
                    {
                        //close streams if still open
                        //these will remain still open only if an error has occured previously. There is no need to show another error dialog to the user if an error occur at closing
                       
                        if(objectStream != null)
                        {
                            try {
                                objectStream.close();
                            } catch (IOException ex) {
                                Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            objectStream = null;
                        }
                        if(inputStream != null)
                        {
                            try {
                                inputStream.close();
                            } catch (IOException ex) {
                                Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            inputStream = null;
                        }
                        
                        //the saving operation has completed. therefore, reset the reference to open thread.
                        SwingUtilities.invokeLater(new Runnable() {
                            //back to uiThread
                            @Override
                            public void run() {
                                openThread = null;
                                jStatusField.setText("Ready");
                                //check and perform any pending close operations
                                if(pendingClose)
                                    performCloseAndCleanup();
                            }
                        });
                        
                        
                    }
                    
                    
                }
                
                
            },"Open Thread");
            openThread.start();
            
            
        }
    }
//open a saved file
    private void jmnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmnOpenActionPerformed
        openWithPrompt();
        
    }//GEN-LAST:event_jmnOpenActionPerformed

     //St whether the UI is readonly or not?
    private void setReadOnly(boolean value)
    {
        jCellTable.setEnabled(!value);
        jtxtFormulaBar.setEditable(!value);
    }
        
    
    private void jtxtFormulaBarCaretUpdate(javax.swing.event.CaretEvent evt) {//GEN-FIRST:event_jtxtFormulaBarCaretUpdate
        highlightCaretElement(evt.getDot()); 
    }//GEN-LAST:event_jtxtFormulaBarCaretUpdate

    //To be called when the spreadsheet frame is to be closed.
    private void performCloseAndCleanup()
    {
        //do we have any background tasks running?
        if(saveThread != null || openThread != null || exportThread != null ) 
        {
            //we cannot terminate still because some operation is pending
            pendingClose = true;
            return;
        }
        
        //report the spreadsheet that it should close. this would make it clear its queued thread processos
        currentSheet.close();
        this.dispose(); 
        //we cant simply decide to exit just because this window is closed. there could be other spreadsheet windows. we should exit only if this is the last spreadsheet window visible.
        
        
        int visibleCount = 0; //count the number of visible spreadsheet windows
        for(Window frm : SpreadsheetFrame.getOwnerlessWindows())
        {
            if(frm.isVisible() == true)
                visibleCount++;
        }
        
        if(visibleCount == 0)
               System.exit(0); //all windows have closed, hence terminate
        
    }
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        
        if(hasChangesToSave)
        {
            //changes are made. 
            //should prompt to save
            int result = JOptionPane.showConfirmDialog(this, "Do you want to save changes?", "Save", JOptionPane.YES_NO_CANCEL_OPTION);
            switch(result)
            {
                case JOptionPane.YES_OPTION:
                    saveWithPrompt();
                    break;
                case JOptionPane.CANCEL_OPTION:
                    return;
            }
        }
        
        //no changes to save, or user has clicked no in prompt
        performCloseAndCleanup();
    }//GEN-LAST:event_formWindowClosing

    
    
    private void jmnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jmnSaveActionPerformed
        saveWithPrompt();
    }//GEN-LAST:event_jmnSaveActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        saveWithPrompt();
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewActionPerformed
        newSpreadsheet();
    }//GEN-LAST:event_btnNewActionPerformed

    private void btnOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenActionPerformed
        openWithPrompt();
    }//GEN-LAST:event_btnOpenActionPerformed

    private void menuExportCommaSeperatedValuesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExportCommaSeperatedValuesActionPerformed
    if(exportThread != null && exportThread.isAlive())
    {   
        //we already have an export operation pending. wait for it to finish.
        JOptionPane.showMessageDialog(this, "Please wait until pending export operation completes and click Export again.","Error - Pending Export Operation" , JOptionPane.OK_OPTION);
        return;
        
    }
        //create exporter
        final AbstractExporter exporter = new CSVExporter();
        //assign spreadsheet
        exporter.setSpreadsheet(currentSheet);
        //inquire preferences from user
        if(!exporter.inquirePreferences(this))
            return;
        //prepare in ui thread
        exporter.prepareExport();
        
        exportThread = new Thread(new Runnable() {

        @Override
        public void run() {
            exporter.exportNow(new ExportGUIFeedback() {
                //also listen for the feedbacks from the export thread
                @Override
                public void reportCompletedSuccesfully(final String statusMessage) {
                    //update ui
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                           jStatusField.setText(statusMessage);
                           exportThread = null;
                            if(pendingClose)
                               performCloseAndCleanup();
                        }
                    });
                    
                    try {
                        //wait for 5 seconds
                        Thread.currentThread().sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //restore the status bar
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                           jStatusField.setText("Ready");
                           
                        }
                    });
                }

                @Override
                public void reportStatus(final String statusMessage) {
                    SwingUtilities.invokeLater(new Runnable() {
                         //update status bar with new status
                        @Override
                        public void run() {
                            jStatusField.setText(statusMessage);
                        }
                    });
                }

                @Override
                public void reportFailed(final String errorMessage) {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            //update status bar with new status
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(SpreadsheetFrame.this, errorMessage,"Export Error" , JOptionPane.OK_OPTION);
                                
                            }
                        });
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (InvocationTargetException ex) {
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                @Override
                public void reportStopedByError() {
                     SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                           jStatusField.setText("Export Error");
                           exportThread = null;
                            if(pendingClose)
                               performCloseAndCleanup();
                        }
                    });
                    try {
                        //wait for 5 seconds
                        Thread.currentThread().sleep(5000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //restore the status bar
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                           jStatusField.setText("Ready");
                        }
                    });
                }
            });
        }
    },"Export Thread");
        
    exportThread.start();
    
        
    }//GEN-LAST:event_menuExportCommaSeperatedValuesActionPerformed

    private void jbtnCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnCopyActionPerformed
        doCellCopy();
    }//GEN-LAST:event_jbtnCopyActionPerformed

    //returns null if the clipboard doesnt have string. Otherwise returns the string in clipboard
    private String getStringFromClipboard()
    {
        //gain access to clipboard
        Clipboard clipboard  = Toolkit.getDefaultToolkit().getSystemClipboard();
        
        //get clipboard content
        Transferable content = clipboard.getContents(null);
        //is the clipboard content compatible
        if(content != null && content.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            try {
                //yes, so return it as string
                String fromClipboard  = (String) content.getTransferData(DataFlavor.stringFlavor);
                return fromClipboard;

            } catch (UnsupportedFlavorException ex) {
                Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //its not, so return null
        return null;
    }
    
    //copies the selected cell
    private void doCellCopy()
    {
        Clipboard clipboard  = Toolkit.getDefaultToolkit().getSystemClipboard();
        //do we have a selection?
        if(jCellTable.getSelectedColumn() == -1 || jCellTable.getSelectedRow() == -1)
        {
            //no
            JOptionPane.showMessageDialog(this, "Sorry, no cell selection available to copy");
            return;
        }
        //yes
        //take first cell selected. derive its cell name
        String columnName = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1); //first column reserved for row ids
        String cellName = columnName + String.valueOf(jCellTable.getSelectedRow()+1);
        
        //put its value to clipboard and update copy related variables
        Cell cell = currentSheet.getCell(cellName);
        if(cell == null)
        {
            clipboard.setContents(new StringSelection(""), clipboardOwner);
            lastCopiedCellRange = getSelectionId();
            clipboardFilled = true;
            lastCopiedExpression = "";
            lastCopiedValue = "";
            lastCopiedSpreadsheet = currentSheet.toSerializableTable();
        }
        else
        {
            clipboard.setContents(new StringSelection(cell.getCellText()), clipboardOwner); 
            
            lastCopiedCellRange= getSelectionId();
            clipboardFilled = true;
            lastCopiedExpression = cell.getCellExpression();
            lastCopiedValue = cell.getCellText();
            lastCopiedSpreadsheet = currentSheet.toSerializableTable();
        }
        
       
        
    }
    //paste a value from clipboard in the given cell
    private void doCellPasteAt(int rowId, int columnId)
    {
        
        //read a string from clipboard
        String fromClipboard = getStringFromClipboard();
        //check whether any prevailing itesm from this app exist in clipboard
        if(!clipboardFilled || (fromClipboard != null && (!fromClipboard.equals(lastCopiedValue))))
        {
            String columnName = Spreadsheet.indexToColumnName(columnId - 1); //first column reserved for row ids
            String cellName = columnName + String.valueOf(rowId+1);
        
            //nothing copied from spreadsheet or else the content coppied last time from spreadsheet doesnt match whats in clipboard now!
            //just paste whatever in system cliboard as it is
            
            if(fromClipboard == null)
            {
                JOptionPane.showMessageDialog(this, "Incompatible data type to paste!");
                return;
            }
            
            currentSheet.setCellExpression(cellName, fromClipboard);
        }
        else if(clipboardFilled && fromClipboard != null && (fromClipboard.equals(lastCopiedValue)))
        {
            
            
            //cell copied from spreadsheet is still there in clipboard
            
            //is it a cell range or a cell reference?
            if(Cell.validateCellId(lastCopiedCellRange))
            {
                String columnName = Spreadsheet.indexToColumnName(columnId - 1); //first column reserved for row ids
                String cellName = columnName + String.valueOf(rowId+1);
            
                //its a cell reference.

                //find the row-index and columnindex
                int sourceRowIndex = Integer.parseInt(lastCopiedCellRange.trim().split("[A-Z]+")[1]) - 1;
                int sourceColumnIndex = Spreadsheet.columnNameToIndex(lastCopiedCellRange.trim().split("[0-9]+")[0]) + 1;

                //find the row-index and columnindex of paste
                int destinationRowIndex = rowId;
                int destinationColumnIndex = columnId;



                try {
                    //do translations
                    String translatedExpression = parser.Parser.translateExpression(lastCopiedExpression,destinationRowIndex-sourceRowIndex,destinationColumnIndex-sourceColumnIndex);
                    currentSheet.setCellExpression(cellName, translatedExpression);
                } catch (TranslateException ex) {
                    currentSheet.setCellExpression(cellName, ex.getMessage());
                }

                
                
            }
            //or it could be a cell range
            else if(lastCopiedCellRange.matches("[A-Z]+[0-9]+\\:[A-Z]+[0-9]+"))
            {
                //its a cell-range
                //separate two cell references
                String[] parts = lastCopiedCellRange.split("\\:");
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

                //loop through each cell from copied set of cells
                for(int sourceRowIndex = startRowIndex; sourceRowIndex<=endRowIndex;sourceRowIndex++)
                    for(int sourceColumnIndex = startColumnIndex; sourceColumnIndex<=endColumnIndex;sourceColumnIndex++)
                    {
                        
                        //find the row-index and columnindex to paste at for current processed cell
                        int destinationRowIndex = rowId + (sourceRowIndex - startRowIndex);
                        int destinationColumnIndex = columnId + (sourceColumnIndex - startColumnIndex);
                        
                        //find the appropriate cell id of source to read the expression from for currently processed cell
                        String sourceColumnName = Spreadsheet.indexToColumnName(sourceColumnIndex - 1); //first column reserved for row ids
                        String sourceCellName = sourceColumnName + String.valueOf(sourceRowIndex+1);
                        
                        //Cell sourceCell = currentSheet.getCell(sourceCellName);
                        //read the expression

                        
                        String sourceExpression = ""; //we will not copy cell formats.
                        if(lastCopiedSpreadsheet.getCellMap().containsKey(sourceCellName))
                            sourceExpression = lastCopiedSpreadsheet.getCellMap().get(sourceCellName).getExpression();
                        
                        
                        //find the appropriate cell id to store the translated expression
                        String destinationColumnName = Spreadsheet.indexToColumnName(destinationColumnIndex- 1); //first column reserved for row ids
                        String destinationCellName = destinationColumnName + String.valueOf(destinationRowIndex+1);
                        
                        try {

                            //do translations
                            String translatedExpression = parser.Parser.translateExpression(sourceExpression,destinationRowIndex-sourceRowIndex,destinationColumnIndex-sourceColumnIndex);
                            currentSheet.setCellExpression(destinationCellName, translatedExpression);
                        } catch (TranslateException ex) {
                            currentSheet.setCellExpression(destinationCellName, ex.getMessage());
                        }
                        
                    }

            }
            
            
           
        }
        
        
            
    }
    
    //Paste copied value to selected cell
    private void doCellPaste()
    {
         //do we have a selection?
        if(jCellTable.getSelectedColumn() == -1 || jCellTable.getSelectedRow() == -1)
        {
            //no
            JOptionPane.showMessageDialog(this, "Sorry, no cell selection available to paste");
            return;
        }
       
        
       //analyse range of selection
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       for(int y  =minRow;y<= maxRow;y++)
           for(int x = minCol;x<=maxCol;x++)
           {
               //paste to individual cell in cell-range
               doCellPasteAt(y, x);
           }
       
        
        refreshSelectionUI();
        //reportChange();
        jCellTable.repaint();
        
    }
    private void jbtnPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jbtnPasteActionPerformed
        
       doCellPaste();
    }//GEN-LAST:event_jbtnPasteActionPerformed

    private void jMenuPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuPasteActionPerformed
        doCellPaste();
    }//GEN-LAST:event_jMenuPasteActionPerformed

    private void jMenuCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuCopyActionPerformed
        doCellCopy();
    }//GEN-LAST:event_jMenuCopyActionPerformed

    private void addNewColumn()
    {
        
        //check for number of selected columns
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 1)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell in the column to where a new column is needed to be added");
            return;
        }
        
        //add the number of columns
        currentSheet.addColumns(jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex(), jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex()-jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex() + 1);
       
        //update ui
        jCellTable.updateUI();
        for(int x = 0; x< jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex()-jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex() + 1; x++)
        {
            jCellTable.addColumn(new TableColumn(jCellTable.getColumnCount()));
        }
    
        refreshSelectionUI();
    }
    private void btnNewColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewColumnActionPerformed
       addNewColumn();
    }//GEN-LAST:event_btnNewColumnActionPerformed

    private void addNewRow()
    {
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell in the row to where a new row is needed to be added");
            return;
        }
        currentSheet.addRows(jCellTable.getSelectionModel().getMinSelectionIndex(), jCellTable.getSelectionModel().getMaxSelectionIndex() - jCellTable.getSelectionModel().getMinSelectionIndex() + 1);
        //currentSheet.shiftCells(jCellTable.getSelectionModel().getMinSelectionIndex(), 0, jCellTable.getSelectionModel().getMaxSelectionIndex() - jCellTable.getSelectionModel().getMinSelectionIndex() + 1,0);
        //jCellTable.repaint();
        jCellTable.updateUI();
        refreshSelectionUI();   
    }
    private void btnNewRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewRowActionPerformed
        addNewRow();
    }//GEN-LAST:event_btnNewRowActionPerformed

    private void menuNewColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuNewColumnActionPerformed
        // TODO add your handling code here:
        addNewColumn();
    }//GEN-LAST:event_menuNewColumnActionPerformed

    private void menuNewRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuNewRowActionPerformed
        addNewRow();
    }//GEN-LAST:event_menuNewRowActionPerformed

    private void popupCopyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popupCopyActionPerformed
       doCellCopy();
    }//GEN-LAST:event_popupCopyActionPerformed

    private void popupPasteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popupPasteActionPerformed
        doCellPaste();
    }//GEN-LAST:event_popupPasteActionPerformed

    private void popupNewColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popupNewColumnActionPerformed
        addNewColumn();
    }//GEN-LAST:event_popupNewColumnActionPerformed

    private void popupNewRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popupNewRowActionPerformed
        addNewRow();
    }//GEN-LAST:event_popupNewRowActionPerformed

    private void menuDecimalNumberFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDecimalNumberFormatActionPerformed
        //Is a selection available?
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell or cell range");
            refreshSelectionUI();
            return;
        }
       
        //request the number of decimal places from the user
        DecimalCountDialog dialog = new DecimalCountDialog(this, true);
        
        //set the dialog initial value to match the selected cells number of decimal places if its in decimal format
        String columnName1 = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1); //-1 to leave the row_headers
        String cellName1 = columnName1 + String.valueOf(jCellTable.getSelectedRow()+1);
        cellformatting.AbstractCellFormat cellFormat = currentSheet.getCellFormatting(cellName1);
        
        if(cellFormat instanceof DecimalNumberCellFormat)
        {
            //set the initial value of dialog to match the number of decimal places configured in selected cell
            DecimalNumberCellFormat dnFormat = (DecimalNumberCellFormat)cellFormat;
            dialog.setInitialValue(dnFormat.getDecimalPlacesCount());
        }
        dialog.setVisible(true);
        
        if(dialog.getResult())
        {
            //if the user clicked ok
            //find the selection range
            int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
            int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
            int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
            int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();

            //loop through cells in the range

            for(int r = minRow; r <= maxRow;r++)
                for(int c = minCol; c <= maxCol; c++)
                {
                     String columnName = Spreadsheet.indexToColumnName(c - 1); //-1 to leave the row_headers
                     String cellName = columnName + String.valueOf(r+1);

                     //set the formatting
                     DecimalNumberCellFormat decFmt = new DecimalNumberCellFormat();
                     decFmt.setDecimalPlacesCount(dialog.getSelectedNumber());
                     currentSheet.setCellFormatting(cellName, decFmt);
                }


             //update ui
             jCellTable.repaint();
             refreshSelectionUI();   
        }
     
    }//GEN-LAST:event_menuDecimalNumberFormatActionPerformed

    private void menuGeneralFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuGeneralFormatActionPerformed
        //is a selection available?
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell or a cell range");
            refreshSelectionUI();
            return;
        }
        //find the selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       //loop through cells in the range
       
       for(int r = minRow; r <= maxRow;r++)
           for(int c = minCol; c <= maxCol; c++)
           {
                String columnName = Spreadsheet.indexToColumnName(c - 1); //-1 to leave the row_headers
                String cellName = columnName + String.valueOf(r+1);
        
                //set the formatting
                currentSheet.setCellFormatting(cellName, new GeneralCellFormat());
           }
       
       
        //update ui
        jCellTable.repaint();
        refreshSelectionUI();   
    }//GEN-LAST:event_menuGeneralFormatActionPerformed

    private void btnCurrentFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCurrentFormatActionPerformed
        //display popup menu consisting of data formats
        jCellFormatMenu.show(btnCurrentFormat, 0, btnCurrentFormat.getHeight()); 
    }//GEN-LAST:event_btnCurrentFormatActionPerformed

    private void menuGeneralFormat2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuGeneralFormat2ActionPerformed
        menuGeneralFormat.doClick();
    }//GEN-LAST:event_menuGeneralFormat2ActionPerformed

    private void menuDecimal2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDecimal2ActionPerformed
        menuDecimalNumberFormat.doClick();
    }//GEN-LAST:event_menuDecimal2ActionPerformed

    private void menuPercentage2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPercentage2ActionPerformed
        menuPercentage.doClick();
    }//GEN-LAST:event_menuPercentage2ActionPerformed

    private void menuPercentageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPercentageActionPerformed
        //do we have a cell selection?
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell or a cell range");
            refreshSelectionUI();
            return;
        }
        //find the selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       //loop through cells in the range
       
       for(int r = minRow; r <= maxRow;r++)
           for(int c = minCol; c <= maxCol; c++)
           {
                String columnName = Spreadsheet.indexToColumnName(c - 1); //-1 to leave the row_headers
                String cellName = columnName + String.valueOf(r+1);
        
                //set the formatting
                currentSheet.setCellFormatting(cellName, new PercentageCellFormat());
           }
       
       
        //update ui
        jCellTable.repaint();
        refreshSelectionUI();   
    }//GEN-LAST:event_menuPercentageActionPerformed

    private void menuRsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRsActionPerformed
        //do we have a cell selection?
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell or a cell range");
            refreshSelectionUI();
            return;
        }
        //find the selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       //loop through cells in the range
       
       for(int r = minRow; r <= maxRow;r++)
           for(int c = minCol; c <= maxCol; c++)
           {
                String columnName = Spreadsheet.indexToColumnName(c - 1); //-1 to leave the row_headers
                String cellName = columnName + String.valueOf(r+1);
        
                //set the formatting
                currentSheet.setCellFormatting(cellName, new CurrencyCellFormat("Rs "));
           }
       
       
        //update ui
        jCellTable.repaint();
        refreshSelectionUI(); 
    }//GEN-LAST:event_menuRsActionPerformed

    private void menuDollarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDollarActionPerformed
        //do we have a cell selection
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell or a cell range");
            refreshSelectionUI();
            return;
        }
        //find the selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       //loop through cells in the range
       
       for(int r = minRow; r <= maxRow;r++)
           for(int c = minCol; c <= maxCol; c++)
           {
                String columnName = Spreadsheet.indexToColumnName(c - 1); //-1 to leave the row_headers
                String cellName = columnName + String.valueOf(r+1);
        
                //set the formatting
                currentSheet.setCellFormatting(cellName, new CurrencyCellFormat("$"));
           }
       
       
        //update ui
        jCellTable.repaint();
        refreshSelectionUI(); 
    }//GEN-LAST:event_menuDollarActionPerformed

    private void menuRs2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRs2ActionPerformed
        menuRs.doClick();
    }//GEN-LAST:event_menuRs2ActionPerformed

    private void menuDollars2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDollars2ActionPerformed
        menuDollar.doClick();
    }//GEN-LAST:event_menuDollars2ActionPerformed

    private void menuDateTime2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDateTime2ActionPerformed
        menuDateTime.doClick();
    }//GEN-LAST:event_menuDateTime2ActionPerformed

    private void menuDateTimeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDateTimeActionPerformed
        //do we have a cell selection?
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell or a cell range");
            refreshSelectionUI();
            return;
        }
        
        //prompt for DateTime format
        DateFormatChooserDialog dialog = new DateFormatChooserDialog(this, true);
        
        String columnName1 = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1); //-1 to leave the row_headers
        String cellName1 = columnName1 + String.valueOf(jCellTable.getSelectedRow()+1);
        cellformatting.AbstractCellFormat cellFormat = currentSheet.getCellFormatting(cellName1);
        
        if(cellFormat instanceof DateTimeCellFormat)
        {
            //set the initial value of format if the selected cell is also in DateTime format
            DateTimeCellFormat dtFormat = (DateTimeCellFormat)cellFormat;
            dialog.setSelectedDateFormat(dtFormat.getDateFormat());
        }
        //show dialog
        dialog.setVisible(true);
        
        if(dialog.getResult())
        {
           //find the selection range
           int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
           int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
           int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
           int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();

           //loop through cells in the range

           for(int r = minRow; r <= maxRow;r++)
               for(int c = minCol; c <= maxCol; c++)
               {
                    String columnName = Spreadsheet.indexToColumnName(c - 1); //-1 to leave the row_headers
                    String cellName = columnName + String.valueOf(r+1);

                    //set the formatting
                    currentSheet.setCellFormatting(cellName, new DateTimeCellFormat(dialog.getSelectedDateFormat()));
               }


            //update ui
            jCellTable.repaint();
            refreshSelectionUI(); 
        }
    }//GEN-LAST:event_menuDateTimeActionPerformed

    private void removeSelectedRow()
    {
        //check for number of selected rows
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell in the row you want to be deleted");
            return;
        }
         //check whether any rows would remain
        int count = jCellTable.getSelectionModel().getMaxSelectionIndex() - jCellTable.getSelectionModel().getMinSelectionIndex() + 1;
        if(currentSheet.getRowCount() <= count)
        {
            //reached minimum number of rows
            JOptionPane.showMessageDialog(this, "Sorry! You cannot remove all rows of a spreadsheet");
            return;
        }
        //delete row from spreadsheet
        currentSheet.deleteRows(jCellTable.getSelectionModel().getMinSelectionIndex(), count);
        //update UI
        jCellTable.updateUI();
        refreshSelectionUI();   
    }
    private void btnRemoveRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveRowActionPerformed
       removeSelectedRow();
    }//GEN-LAST:event_btnRemoveRowActionPerformed

    private void btnRemoveColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveColumnActionPerformed
      removeSelectedColumn();
    }//GEN-LAST:event_btnRemoveColumnActionPerformed

    private void removeSelectedColumn()
    {
        //check for number of selected columns  
        if(jCellTable.getSelectedRow() < 0 || jCellTable.getSelectedColumn() < 0)
        {
            JOptionPane.showMessageDialog(this, "Please select a cell in the column you want to delete");
            return;
        }
       
        int count =  jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex()-jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex() + 1;

        if(currentSheet.getColumnCount()<= count)
        {
            //reached minimum number of columns
            JOptionPane.showMessageDialog(this, "Sorry! You cannot remove all columns of a spreadsheet");
            return;
        }
        //delete the column
        currentSheet.deleteColumns(jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex(), count);
        //refresh ui
        jCellTable.updateUI();
        for(int x = 0; x < count;x++)
        {
            //remove from last columns from the jTable
            jCellTable.getColumnModel().removeColumn(jCellTable.getColumnModel().getColumn(jCellTable.getColumnCount()-1));
        }
        
        
        refreshSelectionUI(); 
        
        
    }
    private void menuDeleteColumnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDeleteColumnActionPerformed
        removeSelectedColumn();
    }//GEN-LAST:event_menuDeleteColumnActionPerformed

    private void menuDeleteRowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuDeleteRowActionPerformed
        removeSelectedRow();
    }//GEN-LAST:event_menuDeleteRowActionPerformed

    /*
        The implementation of menuitems in formula menu.
    */
    
    //The ABS function
    private void menuAbsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAbsActionPerformed
        //Apply the formula ABS to the selected range of cells
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
        int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
        int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
        int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
        int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       for(int y = minRow; y<=maxRow;y++)
           for(int x = minCol; x <= maxCol;x++)
           {
               //process the selected range of cells
               
               String columnName = Spreadsheet.indexToColumnName(x - 1); //first column reserved for row ids
               String cellName = columnName + String.valueOf(y+1);
               
               Cell cell = currentSheet.getCell(cellName);
               String initialExpression = "";
               if(cell != null)
               {
                   //its not a blank cell. hence read its expression
                   initialExpression = cell.getCellExpression();
               }
               if(initialExpression.trim().startsWith("="))
               {
                   //remove leading equal sign
                   initialExpression = initialExpression.trim().substring(1);
               }
               //apply formula
               String newEquation = "=Abs(" + initialExpression + ")";
               currentSheet.setCellExpression(cellName, newEquation);
               
                refreshSelectionUI();
               
           }
       
        
    }//GEN-LAST:event_menuAbsActionPerformed

    //append a given funtion to the formula bar, replacing the token at caret position, and place the caret at the given offset
    //This function is used by sum, avg, min, max menu items in Formula menu
    private void appendFunctionToFormulaBar(String appendStatement, int caretOffset)
    {
        String expression = jtxtFormulaBar.getText();
        int cursorPosition = jtxtFormulaBar.getCaretPosition();
        
        Parser p = new Parser(); //we will use this parser to analyse the expression in formula bar
        p.setCellProvider(currentSheet);
        parser.TokenIdentificationResult observationResult;
        observationResult  = p.identifyTokenAt(expression,  cursorPosition-1);

        
        if(observationResult == null)
        {
            //block feedback signals
            avoidExpressionBarTextFeedback = true;
            //add to charachter position
            jtxtFormulaBar.setText(expression.substring(0,cursorPosition) + appendStatement + expression.substring(cursorPosition));
            //move selection to given caret
            jtxtFormulaBar.setSelectionStart((expression.substring(0,cursorPosition)).length() + caretOffset );
            jtxtFormulaBar.setSelectionEnd((expression.substring(0,cursorPosition) ).length() + caretOffset );
            //reset feedback filter
            avoidExpressionBarTextFeedback = false;
        }
        else
        {
            //if the caret is at a delimeter element, then append to the end of it
            if(observationResult.getObservedElement() instanceof DelimeterElement)
            {
                //append after the delimeter

                //avoid feedback signals
                avoidExpressionBarTextFeedback = true;

                jtxtFormulaBar.setText(expression.substring(0,observationResult.getEndIndex()) + appendStatement + expression.substring(observationResult.getEndIndex()));
                //move cursor to given offset from append position
                jtxtFormulaBar.setSelectionStart(observationResult.getEndIndex()+ caretOffset);
                jtxtFormulaBar.setSelectionEnd(observationResult.getEndIndex() + caretOffset);
                //reset feedback filter
                avoidExpressionBarTextFeedback = false;
            }
            else if(observationResult.getObservedElement() instanceof TokenElement)
            {
                //replace token element

                avoidExpressionBarTextFeedback = true; //avoid feedback signals
                jtxtFormulaBar.setText(expression.substring(0,observationResult.getStartIndex()) + appendStatement + expression.substring(observationResult.getEndIndex()));
                //move cursor to end of replaced text
                jtxtFormulaBar.setSelectionStart(observationResult.getStartIndex() + caretOffset);
                jtxtFormulaBar.setSelectionEnd(observationResult.getStartIndex() + caretOffset);

                avoidExpressionBarTextFeedback = false; //reset feedback filter
            }
        }
    }
    
    //add sum function to selected cell
    private void menuSumActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSumActionPerformed
        //is a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cell selected. Select the cell you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if(!jtxtFormulaBar.hasFocus())
        {
            //give focus to the formula bar so user can do cell picking to select the cell range
            jtxtFormulaBar.requestFocus();
        }
        //smartly append (or replace the token) the function if the cell contains an expression
        if(jtxtFormulaBar.getText().trim().startsWith("="))
        {
            appendFunctionToFormulaBar("sum()",4);
        }
        else
        {
            //if not, replace the value in formula bar with the function
            jtxtFormulaBar.setText("=sum()");
            jtxtFormulaBar.setCaretPosition(5);
        }
        
        
        
    }//GEN-LAST:event_menuSumActionPerformed

    //add max function to selected cell
    private void menuMaxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuMaxActionPerformed
        //is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cell selected. Select the cell you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        if(!jtxtFormulaBar.hasFocus())
        {
            //give focus to the formula bar so the user can do cell picking to select the cell range
            jtxtFormulaBar.requestFocus();
        }
        //smartly append (or replace the token) the function if the cell contains an expression
        if(jtxtFormulaBar.getText().trim().startsWith("="))
        {
            appendFunctionToFormulaBar("max()",4);
        }
        else
        {
             //if not, replace the value in formula bar with the function
            jtxtFormulaBar.setText("=max()");
            jtxtFormulaBar.setCaretPosition(5);
        }
    }//GEN-LAST:event_menuMaxActionPerformed

    //add min function to selected cell
    private void menuMinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuMinActionPerformed
        //is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cell selected. Select the cell you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if(!jtxtFormulaBar.hasFocus())
        {
            //give focus to the formula bar so the user can do cell picking to select the cell range
            jtxtFormulaBar.requestFocus();
        }
        //smartly append (or replace the token) the function if the cell contains an expression
        if(jtxtFormulaBar.getText().trim().startsWith("="))
        {
            appendFunctionToFormulaBar("min()",4);
        }
        else
        {
             //if not, replace the value in formula bar with the function
            jtxtFormulaBar.setText("=min()");
            jtxtFormulaBar.setCaretPosition(5);
        }
    }//GEN-LAST:event_menuMinActionPerformed

    //add if function to selected cell with greater than 0 condition
    private void menuIfGreaterThanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuIfGreaterThanActionPerformed
        //is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       for(int y = minRow; y<=maxRow;y++)
           for(int x = minCol; x <= maxCol;x++)
           {
               //process the selected range of cells
               
               String columnName = Spreadsheet.indexToColumnName(x - 1); //first column reserved for row ids
               String cellName = columnName + String.valueOf(y+1);
               
               Cell cell = currentSheet.getCell(cellName);
               String initialExpression = "";
               if(cell != null)
               {
                   //the processed cell is not blank, hence read its expression
                   initialExpression = cell.getCellExpression();
               }
               if(initialExpression.trim().startsWith("="))
               {
                   //remove leading equal sign
                   initialExpression = initialExpression.trim().substring(1);
               }
               //append the expression
               String newEquation = "=if(" + initialExpression + " > 0,\"Yes\",\"No\")";
               currentSheet.setCellExpression(cellName, newEquation);
               refreshSelectionUI();
               
           }
    }//GEN-LAST:event_menuIfGreaterThanActionPerformed

    //add if function to selected cell with equals 0 condition
    private void menuIfEqualsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuIfEqualsActionPerformed
        //is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
        int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
        int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
        int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
        int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       for(int y = minRow; y<=maxRow;y++)
           for(int x = minCol; x <= maxCol;x++)
           {
               //process the selected range of cells
               
               String columnName = Spreadsheet.indexToColumnName(x - 1); //first column reserved for row ids
               String cellName = columnName + String.valueOf(y+1);
               
               Cell cell = currentSheet.getCell(cellName);
               String initialExpression = "";
               if(cell != null)
               {
                   initialExpression = cell.getCellExpression();
               }
               if(initialExpression.trim().startsWith("="))
               {
                   //remove leading equal sign
                   initialExpression = initialExpression.trim().substring(1);
               }
               //add the expression
               String newEquation = "=if(" + initialExpression + " = 0,\"Yes\",\"No\")";
               currentSheet.setCellExpression(cellName, newEquation);
               
               refreshSelectionUI();
               
           }
    }//GEN-LAST:event_menuIfEqualsActionPerformed
    
    //add if function to selected cell with less than 0 condition
    private void menuIfLessThanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuIfLessThanActionPerformed
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       
       for(int y = minRow; y<=maxRow;y++)
           for(int x = minCol; x <= maxCol;x++)
           {
               //process the selected range of cells
               
               String columnName = Spreadsheet.indexToColumnName(x - 1); //first column reserved for row ids
               String cellName = columnName + String.valueOf(y+1);
               
               Cell cell = currentSheet.getCell(cellName);
               String initialExpression = "";
               if(cell != null)
               {
                   initialExpression = cell.getCellExpression();
               }
               if(initialExpression.trim().startsWith("="))
               {
                   //remove leading equal sign
                   initialExpression = initialExpression.trim().substring(1);
               }
               //add the expression
               String newEquation = "=if(" + initialExpression + " < 0,\"Yes\",\"No\")";
               currentSheet.setCellExpression(cellName, newEquation);
               
               refreshSelectionUI();
               
           }
    }//GEN-LAST:event_menuIfLessThanActionPerformed

    //add the average function to the selected cell
    private void menuAvgActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAvgActionPerformed
        //is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cell selected. Select the cell you want this function to be applied and then try again.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if(!jtxtFormulaBar.hasFocus())
        {
            //focus the formula bar so that cell picking can be used to select data range
            jtxtFormulaBar.requestFocus();
        }
        
        //the formula bar has an expresssion. merge the function to expression
        if(jtxtFormulaBar.getText().trim().startsWith("="))
        {
            appendFunctionToFormulaBar("avg()",4);
        }
        else
        {
            //no expression in formula bar. its a direct value. Hence replace it
            jtxtFormulaBar.setText("=avg()");
            jtxtFormulaBar.setCaretPosition(5);
        }
    }//GEN-LAST:event_menuAvgActionPerformed

    //section of code related to handling cell styling 
    
    //Cell Styling related code
    private String[] getInstalledFonts()
    {
        GraphicsEnvironment graphics = GraphicsEnvironment.getLocalGraphicsEnvironment();
        return graphics.getAvailableFontFamilyNames();
    }
    
    private void setupStyleUI()
    {
        String[] fonts = getInstalledFonts();
        Vector<String> fontFamilies = new Vector<>(); //DefaultComboBox Model is not compatible with arraylist.
        fontFamilies.add("Default");
        
        fontFamilies.addAll(Arrays.asList(fonts));
        
        cmbFontFamilies.setModel(new DefaultComboBoxModel(fontFamilies));
        
        //listen to the changes in fonts combo box
        cmbFontFamilies.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                
                onFontFamilySelectionChanged();
            }

            
        });
        //listen to the changes of font size combo box
        cmbFontSizes.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                onFontSizeSelectionChanged();
            }
        });
        
       
    }
    
    
    private void cmbFontFamiliesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbFontFamiliesActionPerformed
            onFontFamilySelectionChanged();
    }//GEN-LAST:event_cmbFontFamiliesActionPerformed

    private void cmbFontSizesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbFontSizesActionPerformed
            onFontSizeSelectionChanged();
    }//GEN-LAST:event_cmbFontSizesActionPerformed
    
    //toggle bold
    private void btnBoldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBoldActionPerformed
        //is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                //toggle bold
                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                boolean b = btnBold.isSelected();
                style.setBold(b);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        //update ui
        refreshSelectionUI();
        jCellTable.repaint();
    
    }//GEN-LAST:event_btnBoldActionPerformed
    //toggle italic
    private void btnItalicActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnItalicActionPerformed
        // is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                //toggle italic
                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                boolean b = btnItalic.isSelected();
                style.setItalic(b);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        //refresh ui
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_btnItalicActionPerformed

    //underline
    private void btnUnderlineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUnderlineActionPerformed
        //is there a cell selected?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                boolean b = btnUnderline.isSelected();
                style.setUnderlined(b);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        //refresh ui
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_btnUnderlineActionPerformed

    private void btnAlignmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAlignmentActionPerformed
        popupCellAlignment.show(btnAlignment, 0, btnAlignment.getHeight()); //show menu consisting of cell alignments
    }//GEN-LAST:event_btnAlignmentActionPerformed

    //auto cell alignment
    private void menuAutoCellAlignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAutoCellAlignActionPerformed
        //is there a selection?
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                style.setAlignment(SerializableCellStyle.Alignment.Auto);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_menuAutoCellAlignActionPerformed

    private void menuLeftCellAlignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuLeftCellAlignActionPerformed
            if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                //apply cell style
                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                style.setAlignment(SerializableCellStyle.Alignment.Left);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_menuLeftCellAlignActionPerformed

    private void menuCenterAlignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCenterAlignActionPerformed
         if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                style.setAlignment(SerializableCellStyle.Alignment.Center);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_menuCenterAlignActionPerformed

    private void menuRightCellAlignActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuRightCellAlignActionPerformed
         if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                style.setAlignment(SerializableCellStyle.Alignment.Right);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_menuRightCellAlignActionPerformed

    //change font colour of selected cells text
    private void btnFontColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFontColorActionPerformed
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String firstColumnName = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1);
        String firstCellName = firstColumnName + String.valueOf(jCellTable.getSelectedRow()+1);
        
        Color col = JColorChooser.showDialog(this, "Font Colour", currentSheet.getCellStyle(firstCellName).getFontColor());
        if(col == null)
            return; //user has canceled the dialog
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                              
                
                style.setFontColor(col);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist



               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_btnFontColorActionPerformed
    //change background colour of selected cell
    private void btnCellColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCellColorActionPerformed
        if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String firstColumnName = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1);
        String firstCellName = firstColumnName + String.valueOf(jCellTable.getSelectedRow()+1);
        
        Color col = JColorChooser.showDialog(this, "Cell Colour", currentSheet.getCellStyle(firstCellName).getCellColor());
        
        if(col == null)
            return; //user has canceled the dialog
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                //apply style
                

                style.setCellColor(col);
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

                

               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }//GEN-LAST:event_btnCellColorActionPerformed

    //From the interface ListSelectionListner
    //Incurs when the cell selection is changed
    
    //called to refresh the ui after the content of selected cell is modified
    private void refreshSelectionUI()
    {
        
        ListSelectionModel cSel = jCellTable.getColumnModel().getSelectionModel();
            if(cSel.isSelectedIndex(0) && jCellTable.getSelectedRowCount() > 0)
            {
                //A row_header has been selected.
                //Select the whole row
                jCellTable.setRowSelectionInterval(jCellTable.getSelectedRow(),jCellTable.getSelectedRow());
                jCellTable.setColumnSelectionInterval(jCellTable.getColumnCount()-1,0);
                return;
            }

            //Find cell
            String columnName = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1);
            String cellName = columnName + String.valueOf(jCellTable.getSelectedRow()+1);
            txtSelectedCellId.setText(getSelectionId());
            //used for cell picking to backtrack the cell being edited, after a cell pick has changed the current cell selection
          

            Cell cell = currentSheet.getCell(cellName);
            //Show expression
            if(cell == null)
            {
                jtxtFormulaBar.setText("");
            }
            else
            {
                jtxtFormulaBar.setText(cell.getCellExpression());
               
            }
            //update related UI subsystems
            updateCellFormattingUI();
            updateCellStylingUI();
    }
    
    private void onFontFamilySelectionChanged() 
    {
         if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                style.setFontFamilyName((String)cmbFontFamilies.getEditor().getItem());
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }
    
    
    private void onFontSizeSelectionChanged() 
    {
         if(jCellTable.getSelectedRowCount() == 0 || jCellTable.getSelectedColumnCount() == 0)
        {
            JOptionPane.showMessageDialog(this, "Error: No Cells selected. Select the cells you want styling to be changed.", "No Selection Error",JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
         //find selection range
       int minRow = jCellTable.getSelectionModel().getMinSelectionIndex();
       int maxRow = jCellTable.getSelectionModel().getMaxSelectionIndex();
       int minCol = jCellTable.getColumnModel().getSelectionModel().getMinSelectionIndex();
       int maxCol = jCellTable.getColumnModel().getSelectionModel().getMaxSelectionIndex();
       for(int y = minRow;y <=maxRow;y++)
           for(int x = minCol; x<=maxCol;x++)
           {
                //find the selected cell
                String columnName = Spreadsheet.indexToColumnName(x - 1);
                String cellName = columnName + String.valueOf(y+1);

                SerializableCellStyle style = currentSheet.getCellStyle(cellName);
                String selection = (String)cmbFontSizes.getEditor().getItem();
                if(selection.matches("-?\\d+"))
                {
                    style.setFontSize(Integer.valueOf(selection));
                    style.setIsDefaultFontSize(false);
                }
                else
                {
                    style.setIsDefaultFontSize(true);
                }
                currentSheet.setCellStyle(cellName, style); //this step is required to make a new cell in hashmap if such a cell didnt exist

               
           }
        
        refreshSelectionUI();
        jCellTable.repaint();
    }
    
    private void updateCellStylingUI()
    {
        //load defaults
        cmbFontFamilies.getEditor().setItem("Default");
        cmbFontSizes.getEditor().setItem("Default");
        btnBold.setSelected(false);
        btnItalic.setSelected(false);
        btnUnderline.setSelected(false);
        btnAlignment.setText("Auto");
        
        if(jCellTable.getSelectedColumn() < 1 || jCellTable.getSelectedRow() < 0)
        {
            //no proper selection
            return;
        }
         //find the selected cell
        String columnName = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1);
        String cellName = columnName + String.valueOf(jCellTable.getSelectedRow()+1);
        
        //find the selected cells styling
        SerializableCellStyle cellStyle = currentSheet.getCellStyle(cellName);
        cmbFontFamilies.getEditor().setItem(cellStyle.getFontFamilyName());
        if(cellStyle.getIsDefaultFontSize())
        {
            cmbFontSizes.getEditor().setItem("Default");
        }
        else
        {
            cmbFontSizes.getEditor().setItem(String.valueOf(cellStyle.getFontSize()));
        }
        btnBold.setSelected(cellStyle.getBold());
        btnItalic.setSelected(cellStyle.getItalic());
        btnUnderline.setSelected(cellStyle.getUnderlined());
        btnAlignment.setText(cellStyle.getAlignment().toString());
        
    }
    //update the cell radioboxes related to current selected cells cell format
    private void updateCellFormattingUI()
    {
        //uncheck all
        menuDecimalNumberFormat.setSelected(false);
        menuGeneralFormat.setSelected(false);
        menuDecimal2.setSelected(false);
        menuGeneralFormat2.setSelected(false);
        menuPercentage.setSelected(false);
        menuPercentage2.setSelected(false);
        menuRs.setSelected(false);
        menuRs2.setSelected(false);
        menuDollar.setSelected(false);
        menuDollars2.setSelected(false);
        menuCurrency.setSelected(false);
        menuCurrency2.setSelected(false);
          menuDateTime.setSelected(false);
        menuDateTime2.setSelected(false);
        
        if(jCellTable.getSelectedColumn() < 1 || jCellTable.getSelectedRow() < 0)
        {
            //no proper selection
            return;
        }
        //find the selected cell
        String columnName = Spreadsheet.indexToColumnName(jCellTable.getSelectedColumn() - 1);
        String cellName = columnName + String.valueOf(jCellTable.getSelectedRow()+1);
        
        //find selected cells formattting
       cellformatting.AbstractCellFormat cellFormat = currentSheet.getCellFormatting(cellName);
        
       //update radio buttons
        if(cellFormat instanceof cellformatting.GeneralCellFormat)
        {
            menuGeneralFormat.setSelected(true);
            menuGeneralFormat.setSelected(true);
            
        }
        else if(cellFormat instanceof cellformatting.DecimalNumberCellFormat)
        {
            menuDecimalNumberFormat.setSelected(true);
            menuDecimalNumberFormat.setSelected(true);
        }
         else if(cellFormat instanceof cellformatting.PercentageCellFormat)
        {
            menuPercentage.setSelected(true);
            menuPercentage2.setSelected(true);
        }
         else if(cellFormat instanceof cellformatting.DateTimeCellFormat)
        {
            menuDateTime.setSelected(true);
            menuDateTime2.setSelected(true);
        }
         else if(cellFormat instanceof cellformatting.CurrencyCellFormat)
        {
            menuCurrency.setSelected(true);
            menuCurrency2.setSelected(true);
            
            CurrencyCellFormat currencyFormat = (CurrencyCellFormat)cellFormat;
            if(currencyFormat.getCurrencyPrefix().equals("Rs "))
            {
                menuRs.setSelected(true);
                menuRs2.setSelected(true);
            }
            else if(currencyFormat.getCurrencyPrefix().equals("$"))
            {
                menuDollar.setSelected(true);
                menuDollars2.setSelected(true);
            }
        }
        btnCurrentFormat.setText(cellFormat.getSerializedCellFormatName());
        
    }
    
    //show the list of supported features
    private void menuSupportedFeaturesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSupportedFeaturesActionPerformed
        SupportedFeaturesDialog sfd = new SupportedFeaturesDialog(this,false);
        sfd.setVisible(true);
    }//GEN-LAST:event_menuSupportedFeaturesActionPerformed

    //show about dialog
    private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
        AboutDialog about = new AboutDialog(this, true);
        about.setVisible(true);
    }//GEN-LAST:event_menuAboutActionPerformed

      
    //creates a serializable table consisting data from a file given as arguement
    //this function used to open a file through command line arguement
    private static serializables.SerializableTable loadSpreadsheetFromArguements(String arguements) throws IOException, ClassNotFoundException
    {
        FileInputStream inputStream = null;
        ObjectInputStream objectStream = null;
        File selectedFile = new File(arguements);
        try
        {
            //open input stream        
            inputStream = new FileInputStream(selectedFile);
            objectStream = new ObjectInputStream(inputStream);
            //do reading
            final serializables.SerializableTable serializableTable = (serializables.SerializableTable) objectStream.readObject();

            //close connections
            objectStream.close();
            inputStream.close();

            //set to null so finally block will no reclose these
            objectStream = null;
            inputStream = null;
            return serializableTable;
        }
        catch(FileNotFoundException ex) //throw back any errors occured
        {
            throw ex;
        }
        catch ( IOException | ClassNotFoundException ex)
        {
            throw ex;
            
        }
        finally
        {
            //close connections
            if(inputStream != null)
            {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    throw ex;
                    
                }
            }
            if(objectStream != null)
            {
                try {
                    objectStream.close();
                } catch (IOException ex) {
                    Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                    throw ex;
                }
            }
        }
        
        
    }
    
    //The entry point of application
    public static void main(String args[]) {
        //Modify the splash screen
        SplashScreen splash = SplashScreen.getSplashScreen();
        if(splash != null)
        {
            //updae the splash screen
            Dimension dimen = splash.getSize();
            Graphics2D splashGraphics =  splash.createGraphics();
            splashGraphics.setPaint(Color.WHITE);
            
            splashGraphics.drawString("Loading...", (int)dimen.getWidth() - 80,20 );
            splash.update();
        }
        //set the metal look and feel
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Metal".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    //break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SpreadsheetFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SpreadsheetFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SpreadsheetFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SpreadsheetFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        
        
        //check for any input arguements
        Spreadsheet currentSheet = null; 
        boolean locationAvailability = false; //is a file passed by arguements. then we know its locaiton
        String filePath = ""; //location of file passed  by arguement
        if(args.length > 0)
        {
            for(String s: args )
            {
                filePath += s; //develop the full path from arguements
            }
            try {
                //we have a command line arguements. It could be a fileName given to open. So lets try to open it
                serializables.SerializableTable cellTable = loadSpreadsheetFromArguements(args[0]);
                currentSheet = Spreadsheet.fromSerializableTable(cellTable);
                locationAvailability = true;
                filePath = args[0];
            } catch (IOException ex) {
                Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(SpreadsheetFrame.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(null, "Error: " + ex.getLocalizedMessage(), "Error", JOptionPane.OK_OPTION);
            }
            
        }
        
        if (currentSheet == null)
        {
            //if no file is opened by arguement, make a new spreadsheet
            currentSheet = new Spreadsheet();
        }
        
        //prepare variables to pass to inner class
        final String path = filePath;
        final boolean  locAvailability = locationAvailability;
        final Spreadsheet toOpen = currentSheet;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                //create and show a spreadshseet frame
                SpreadsheetFrame frm = new SpreadsheetFrame(toOpen);
                if(locAvailability)
                {
                    frm.setFileName(path);
                    frm.setLocationAvailability(true);
                }
                frm.setVisible(true);
                
            }
        });
    }
    
   
   
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAlignment;
    private javax.swing.JToggleButton btnBold;
    private javax.swing.JButton btnCellColor;
    private javax.swing.JButton btnCurrentFormat;
    private javax.swing.JButton btnFontColor;
    private javax.swing.JToggleButton btnItalic;
    private javax.swing.JButton btnNew;
    private javax.swing.JButton btnNewColumn;
    private javax.swing.JButton btnNewRow;
    private javax.swing.JButton btnOpen;
    private javax.swing.JButton btnRemoveColumn;
    private javax.swing.JButton btnRemoveRow;
    private javax.swing.JButton btnSave;
    private javax.swing.JToggleButton btnUnderline;
    private javax.swing.JComboBox cmbFontFamilies;
    private javax.swing.JComboBox cmbFontSizes;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JPopupMenu jCellFormatMenu;
    private javax.swing.JTable jCellTable;
    private javax.swing.JPopupMenu jCellTableContextMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuCopy;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenuItem jMenuPaste;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JLabel jStatusField;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JButton jbtnCopy;
    private javax.swing.JButton jbtnEvaluate;
    private javax.swing.JButton jbtnPaste;
    private javax.swing.JMenuItem jmnExit;
    private javax.swing.JMenuItem jmnNew;
    private javax.swing.JMenuItem jmnOpen;
    private javax.swing.JMenuItem jmnSave;
    private javax.swing.JMenuItem jmnSaveAs;
    private javax.swing.JTextField jtxtFormulaBar;
    private javax.swing.JMenuItem menuAbout;
    private javax.swing.JMenuItem menuAbs;
    private javax.swing.JMenuItem menuAutoCellAlign;
    private javax.swing.JMenuItem menuAvg;
    private javax.swing.JMenu menuCellFormats;
    private javax.swing.JMenuItem menuCenterAlign;
    private javax.swing.JMenu menuCurrency;
    private javax.swing.JMenu menuCurrency2;
    private javax.swing.JRadioButtonMenuItem menuDateTime;
    private javax.swing.JMenuItem menuDateTime2;
    private javax.swing.JMenuItem menuDecimal2;
    private javax.swing.JRadioButtonMenuItem menuDecimalNumberFormat;
    private javax.swing.JMenuItem menuDeleteColumn;
    private javax.swing.JMenuItem menuDeleteRow;
    private javax.swing.JRadioButtonMenuItem menuDollar;
    private javax.swing.JMenuItem menuDollars2;
    private javax.swing.JMenuItem menuExportCommaSeperatedValues;
    private javax.swing.JRadioButtonMenuItem menuGeneralFormat;
    private javax.swing.JMenuItem menuGeneralFormat2;
    private javax.swing.JMenuItem menuIfEquals;
    private javax.swing.JMenuItem menuIfGreaterThan;
    private javax.swing.JMenuItem menuIfLessThan;
    private javax.swing.JMenuItem menuLeftCellAlign;
    private javax.swing.JMenuItem menuMax;
    private javax.swing.JMenuItem menuMin;
    private javax.swing.JMenuItem menuNewColumn;
    private javax.swing.JMenuItem menuNewRow;
    private javax.swing.JRadioButtonMenuItem menuPercentage;
    private javax.swing.JMenuItem menuPercentage2;
    private javax.swing.JMenuItem menuRightCellAlign;
    private javax.swing.JRadioButtonMenuItem menuRs;
    private javax.swing.JMenuItem menuRs2;
    private javax.swing.JMenuItem menuSum;
    private javax.swing.JMenuItem menuSupportedFeatures;
    private javax.swing.JPopupMenu popupCellAlignment;
    private javax.swing.JMenuItem popupCopy;
    private javax.swing.JMenuItem popupNewColumn;
    private javax.swing.JMenuItem popupNewRow;
    private javax.swing.JMenuItem popupPaste;
    private javax.swing.JTextField txtSelectedCellId;
    // End of variables declaration//GEN-END:variables

    
    
    
    
    
  
}
