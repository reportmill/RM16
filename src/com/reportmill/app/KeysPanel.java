/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.app;
import com.reportmill.apptools.*;
import com.reportmill.base.*;
import com.reportmill.shape.*;
import java.util.*;
import snap.geom.Insets;
import snap.geom.Polygon;
import snap.gfx.*;
import snap.util.*;
import snap.view.*;

/**
 * This class shows the current set of keys relative to the current editor selection in a browser and lets users
 * drag and drop them to the editor.
 */
public class KeysPanel extends RMEditorPane.SupportPane {

    // The KeysPanel browser
    private BrowserView<KeyNode> _keysBrowser;

    // The current entity for the keys browser
    private Entity _entity = new Entity("Bogus");

    // The current root items for the keys browser
    private List<KeyNode> _rootItems;

    // The icon for a "to-many" branch
    private static Image _doubleArrowImage;

    // The Drag and Drop key
    private String _dragKey;

    // Whether to show built in keys
    private boolean _showBuiltIn = false;

    // The KeysTable
    private TableView<Map<String,Object>> _keysTable;

    // The KeysTable key
    private String _keysTableKey = "";

    // Contants aggregate keys
    private static final String[] _aggregateKeys = {"total", "average", "count", "countDeep", "max", "min"};

    // Constants for heritage keys
    private static final String[] _heritageKeys = {"Running", "Remaining", "Up"};

    // Constants for built-in keys
    private static final String[] _builtInKeys = {"Date", "Page", "PageMax", "Page of PageMax",
            "PageBreak", "PageBreakMax", "PageBreakPage", "PageBreakPageMax", "Row"};

    // Shared built-in key nodes
    private static List<KeyNode> _builtInKeyNodes;

    // Current active KeysPanel
    private static KeysPanel _active;

    /**
     * Constructor.
     */
    public KeysPanel(RMEditorPane anEP)
    {
        super(anEP);
        _builtInKeyNodes = new ArrayList<>();
        for (String key : _builtInKeys) _builtInKeyNodes.add(new KeyNode(key));
    }

    /**
     * Returns the entity.
     */
    public Entity getEntity()
    {
        return _entity;
    }

    /**
     * Returns the current key path selected by the browser.
     */
    public String getKeyPath()
    {
        String key = getKeysBrowserPath(); // Get normal path
        if (key.equals("Page of PageMax"))
            return "@Page@ of @PageMax@";  // Special case for Page of PageMax
        return "@" + key + "@"; // Return path with @ signs
    }

    /**
     * Returns the KeyPath entity.
     */
    public Entity getKeyPathEntity(String aKey)
    {
        Entity entity = getEntity();
        if (entity == null) return null;
        Property kprop = entity.getKeyPathProperty(aKey);
        if (kprop == null) return null;
        Entity kentity = kprop.getRelationEntity();
        return kentity;
    }

    /**
     * Returns the items for key path entity.
     */
    public List<Map<String,Object>> getKeyPathItems(String aKey)
    {
        // Get full list key from selected shape and browser
        RMShape selShape = getSelectedShape();
        String keyPrefix = selShape.getDatasetKey();
        String keySuffix = getKeysBrowserPath();
        String key = keyPrefix != null ? (keyPrefix + '.' + keySuffix) : keySuffix;

        // Get Editor.Datasource dataset (just return if null)
        RMEditor editor = getEditor();
        RMDataSource dataSource = editor.getDataSource();
        if (dataSource == null) return null;
        Map<String,Object> dataset = dataSource.getDataset();

        // Return list
        return RMKeyChain.getListValue(dataset, key);
    }

    /**
     * Returns the KeysBrowser path.
     */
    private String getKeysBrowserPath()
    {
        return _keysBrowser.getSelPathForSeparator(".");
    }

    /**
     * Returns whether selected item is to-many.
     */
    public boolean isSelectedLeaf()
    {
        KeyNode node = _keysBrowser.getSelItem();
        return node != null && !node.isParent();
    }

    /**
     * Returns whether selected item is to-many.
     */
    public boolean isSelectedToMany()
    {
        KeyNode node = _keysBrowser.getSelItem();
        return node != null && node._isToMany;
    }

    /**
     * Initialize UI panel.
     */
    protected void initUI()
    {
        // Get/configure KeysBrowser
        _keysBrowser = getView("KeysBrowser", BrowserView.class);
        _keysBrowser.setResolver(new KeysBrowserResolver());
        _keysBrowser.setRowHeight(20);
        _keysBrowser.setCellConfigure(this::configureKeysBrowserCell);

        // Register KeysBrowser for click, drag
        _keysBrowser.addEventHandler(this::handleKeysBrowserMouseRelease, MouseRelease);
        _keysBrowser.addEventHandler(this::handleKeysBrowserDragGesture, DragGesture);
        _keysBrowser.addEventHandler(this::handleKeysBrowserDragSourceEnd, EventType.DragSourceEnd);
    }

    /**
     * Updates the UI from the current selection.
     */
    public void resetUI()
    {
        // Get selected shape and shape tool
        RMShape selShape = getSelectedShape();
        RMTool<?> tool = getEditor().getTool(selShape);

        // Get entity from tool/shape and set in browser
        Entity entity = !_showBuiltIn ? tool.getDatasetEntity(selShape) : null;
        if (entity != _entity) {
            _entity = entity;
            _rootItems = entity != null ? new KeyNode("Root").getChildren() : _builtInKeyNodes;
            _keysBrowser.setItems(_rootItems);
        }

        // Update BuiltInKeysButton
        setViewValue("BuiltInKeysButton", _showBuiltIn);

        // Update KeysTableKey
        if (isShowKeysTable())
            setKeysTableKey(getKeyPath());
    }

    /**
     * Updates the current selection from the UI controls.
     */
    public void respondUI(ViewEvent anEvent)
    {
        // Handle BuiltInKeysButton
        if (anEvent.equals("BuiltInKeysButton"))
            _showBuiltIn = anEvent.getBoolValue();

        // Handle ShowKeysTableMenu
        if (anEvent.equals("ShowKeysTableMenu")) {
            boolean show = !isShowKeysTable();
            setShowKeysTable(show);
            runDelayed(() -> getEditorPane().getAttributesPanel().getDrawer().setMaximized(show), 600);
        }
    }

    /**
     * Called when KeysBrowser gets MouseRelease event.
     */
    private void handleKeysBrowserMouseRelease(ViewEvent anEvent)
    {
        // Handle KeysBrowser (double-click - used to check anEvent.getClickCount()==2)
        if (anEvent.isMouseClick() && anEvent.getClickCount() == 2) {

            // If double-click on RMTable, add grouping
            RMEditor editor = getEditor();
            if (getSelectedShape() instanceof RMTable) {
                RMTableTool<?> tool = (RMTableTool<?>) editor.getTool(getSelectedShape());
                tool.addGroupingKey(getKeysBrowserPath());
            }

            // If leaf click for RMText, add key
            else if (isSelectedLeaf() && editor.getTextEditor() != null)
                editor.getTextEditor().replaceChars(getKeyPath());
        }
    }

    /**
     * Called when KeysBrowser gets DragGesture event.
     */
    private void handleKeysBrowserDragGesture(ViewEvent anEvent)
    {
        // If drag was in scrollbar, just return
        if (ViewUtils.getDeepestChildAt(anEvent.getView(), anEvent.getX(), anEvent.getY(), ScrollBar.class) != null)
            return;

        // Set the drag key and get drag key with @-signs
        _active = this;
        _dragKey = getKeysBrowserPath();
        String dragKeyFull = getKeyPath();

        // Get event Clipboard and start drag
        Clipboard cboard = anEvent.getClipboard();
        cboard.addData(dragKeyFull);
        cboard.setDragImage(ImageUtils.getImageForStringAndFont(dragKeyFull, getSelectedShape().getDocument().getFont()));
        cboard.startDrag();

        // Notify Attributes panel that dragging started
        getEditorPane().getAttributesPanel().childDragStart();
    }

    /**
     * Called when KeysBrowser gets DragSourceEnd event.
     */
    private void handleKeysBrowserDragSourceEnd(ViewEvent anEvent)
    {
        _dragKey = null;
        getEditorPane().getAttributesPanel().childDragStop();
    }

    /**
     * Returns the current editor's selected shape.
     */
    public RMShape getSelectedShape()
    {
        return getEditor().getSelectedOrSuperSelectedShape();
    }

    /**
     * Returns the window title for this panel.
     */
    public String getWindowTitle()
    {
        return "Keys Panel";
    }

    /**
     * Called to configure KeysBrowser cell: Make aggregate keys bold and .
     */
    private void configureKeysBrowserCell(ListCell<KeyNode> aCell)
    {
        KeyNode knode = aCell.getItem();
        if (knode != null && knode._special) aCell.setFont(_keysBrowser.getFont().getBold());
        aCell.setToolTip(aCell.getText());
    }

    /**
     * Returns whether KeysTable is showing.
     */
    public boolean isShowKeysTable()
    {
        return _keysTable != null && _keysTable.isShowing();
    }

    /**
     * Shows the KeysTable.
     */
    public void setShowKeysTable(boolean aValue)
    {
        // If already set, just return
        if (aValue == isShowKeysTable()) return;

        // Get SplitView
        SplitView splitView = getView("SplitView", SplitView.class);
        splitView.setBorder(null);

        // If showing
        if (aValue) {

            // Create table if needed
            if (_keysTable == null) {
                _keysTable = new TableView<>();
                _keysTable.setGrowHeight(true);
                _keysTable.setShowHeader(true);
                _keysTable.setFont(Font.Arial12);
                _keysTable.setCellPadding(new Insets(2, 4, 2, 4));
                _keysTable.setFocusable(false);
            }

            // Add KeysTable to split
            splitView.addItemWithAnim(_keysTable, 100);
        }

        // If hiding
        else splitView.removeItemWithAnim(_keysTable);
    }

    /**
     * Sets the KeysTableKey.
     */
    public void setKeysTableKey(String aKey)
    {
        // If already set, just return
        String key = aKey.replace("@", "");
        if (key.equals(_keysTableKey)) return;
        _keysTableKey = key;

        // Set columns
        while (_keysTable.getColCount() > 0) _keysTable.removeCol(0);

        // Get entity for key
        Entity entity = getKeyPathEntity(key);
        if (entity == null) return;

        // Iterate over entity properties and add column for each
        List<Property> attrs = entity.getAttributes();
        for (Property prop : attrs) {
            if (prop.isRelation()) continue;
            TableCol<Map<String,Object>> col = new TableCol<>();
            col.setHeaderText(prop.getName());
            col.setItemKey(prop.getName());
            _keysTable.addCol(col);
        }

        // Get/set items
        List<Map<String,Object>> items = getKeyPathItems(key);
        _keysTable.setItems(items);
    }

    /**
     * Returns the icon of a double right arrow to indicate branch nodes of a "to-many" relationship in a browser.
     */
    public static Image getDoubleArrowImage()
    {
        // If double arrow icon hasn't been created, create it
        if (_doubleArrowImage != null) return _doubleArrowImage;
        Image img = Image.getImageForSize(16, 11, true);
        Polygon poly = new Polygon(1.5, 1.5, 7.5, 5.5, 1.5, 9.5);
        Painter pntr = img.getPainter();
        pntr.setColor(Color.BLACK);
        pntr.fill(poly);
        pntr.translate(7, 0);
        pntr.fill(poly);
        pntr.flush();
        return _doubleArrowImage = img;
    }

    /**
     * Returns the current drag key.
     */
    public static String getDragKey()
    {
        return _active != null ? _active._dragKey : null;
    }

    /**
     * Drops a drag key.
     */
    public static void dropDragKey(RMShape aShape, ViewEvent anEvent)
    {
        // Get editor
        RMEditor editor = (RMEditor) anEvent.getView();

        // Handle KeysPanel to-many drop - run dataset key panel (after delay)
        if (_active.isSelectedToMany()) {
            String datasetKey = StringUtils.delete(KeysPanel.getDragKey(), "@");
            editor.getEnv().runLater(() -> RMEditorUtils.runDatasetKeyPanel(editor, datasetKey));
        }

        // Otherwise, just drop string as text shape
        else {
            aShape.repaint();
            editor.undoerSetUndoTitle("Drag and Drop Key");
            Clipboard cb = anEvent.getClipboard();
            RMEditorClipboard.paste(editor, cb, (RMParentShape) aShape, anEvent.getPoint());
        }
    }

    /**
     * An inner class to provide data for keys browser.
     */
    private static class KeysBrowserResolver extends TreeResolver<KeyNode> {

        @Override
        public KeyNode getParent(KeyNode anItem)  { return anItem._parent; }
        @Override
        public boolean isParent(KeyNode anItem)  { return anItem.isParent(); }
        @Override
        public List<KeyNode> getChildren(KeyNode anItem)  { return anItem.getChildren(); }
        @Override
        public String getText(KeyNode anItem)  { return anItem.getKey(); }
        @Override
        public Image getBranchImage(KeyNode anItem)
        {
            return anItem._isToMany ? getDoubleArrowImage() : null;
        }
    }

    /**
     * An inner class for Node of KeysBrowser.
     */
    private class KeyNode {

        // Node parent
        KeyNode _parent;

        // Node key
        String _key;

        // The Property (if based on one)
        Property _prop;

        // Node children
        List<KeyNode> _children;

        // Whether node is "to-many"
        boolean _isToMany;

        // Whether node is special key (aggregate or heritage)
        boolean _special;

        /**
         * Creates a new KeyNode for key.
         */
        public KeyNode(String aKey)
        {
            _key = aKey;
        }

        /**
         * Creates a new KeyNode for given parent, key and optional property.
         */
        public KeyNode(KeyNode aParent, String aKey, Property aProp)
        {
            this(aKey);
            _parent = aParent;
            _prop = aProp;
        }

        /**
         * Returns the node key.
         */
        public String getKey()  { return _key; }

        /**
         * Returns whether node is a parent.
         */
        public boolean isParent()  { return _prop != null && _prop.isRelation() || _special; }

        /**
         * Returns the list of children for this node.
         */
        public List<KeyNode> getChildren()
        {
            if (_children != null) return _children;
            return _children = createChildren();
        }

        /**
         * Creates the list of children for this node.
         */
        private List<KeyNode> createChildren()
        {
            // Create children list and get entity for node
            List<KeyNode> children = new ArrayList<>();
            Entity entity = getEntity();
            if (entity == null) return children;

            // Add attributes
            for (int i = 0, iMax = entity.getAttributeCount(); i < iMax; i++) {
                Property attr = entity.getAttributeSorted(i);
                if (attr.isPrivate()) continue;
                KeyNode child = new KeyNode(this, attr.getName(), attr);
                children.add(child);
            }

            // Add relations
            for (int i = 0, iMax = entity.getRelationCount(); i < iMax; i++) {
                Property rel = entity.getRelationSorted(i);
                if (rel.isPrivate()) continue;
                KeyNode child = new KeyNode(this, rel.getName(), rel);
                child._isToMany = rel.isToMany();
                children.add(child);
            }

            // Add aggregate keys
            if (getShowAggregates())
                for (String aggregateKey : _aggregateKeys) {
                    KeyNode child = new KeyNode(this, aggregateKey, null);
                    children.add(child);
                    child._special = true;
                }

            // If Root, add heritage keys
            if (_parent == null) {
                for (String heritageKey : _heritageKeys) {
                    KeyNode child = new KeyNode(this, heritageKey, null);
                    children.add(child);
                    child._special = true;
                }
            }

            return children;
        }

        /**
         * Returns whether node should have aggregates.
         */
        protected boolean getShowAggregates()
        {
            if (_parent == null) return true;
            if (ArrayUtils.contains(_heritageKeys, getKey())) return true;
            if (_prop != null && _prop.isRelation() && _prop.isToMany()) return true;
            return false;
        }

        /**
         * Returns the entity.
         */
        protected Entity getEntity()
        {
            if (_parent == null) return KeysPanel.this.getEntity();
            if (_prop != null) return _prop.getRelationEntity();
            return _parent.getEntity();
        }

        // Returns node as string
        public String toString()  { return _key;  }
    }
}