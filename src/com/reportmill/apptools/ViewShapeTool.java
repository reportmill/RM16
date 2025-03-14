package com.reportmill.apptools;
import com.reportmill.shape.ViewShape;
import snap.util.ListUtils;
import snap.view.*;

import java.util.List;

/**
 * An RMTool subclass to edit ViewShape.
 */
public class ViewShapeTool<T extends ViewShape> extends RMTool<T> {

    // The main TabView
    private TabView _tabView;

    // The Text tab
    private Tab _textFieldTab;

    // The Button tab
    private Tab _buttonTab;

    // The Type selection ListView
    private ListView<String> _typeList;

    /**
     * Constructor.
     */
    public ViewShapeTool()
    {
        super();
    }

    /**
     * Override to customize UI.
     */
    protected void initUI()
    {
        _tabView = getUI(TabView.class);
        _textFieldTab = _tabView.getTab(1);
        _buttonTab = _tabView.getTab(2);

        _typeList = getView("TypeListView", ListView.class);
        List<String> items = ListUtils.of("TextField", "Button", "RadioButton", "CheckBox", "ListView", "ComboBox");
        _typeList.setItems(items);
    }

    /**
     * Reset UI.
     */
    protected void resetUI()
    {
        ViewShape vshape = getSelectedShape();
        String type = vshape.getViewType();

        _typeList.setSelItem(type);

        _textFieldTab.setVisible(type.equals(ViewShape.TextField_Type));
        _buttonTab.setVisible(type.equals(ViewShape.Button_Type));
        setViewValue("TextView", vshape.getText());
    }

    /**
     * Respond to UI.
     */
    protected void respondUI(ViewEvent anEvent)
    {
        ViewShape vshape = getSelectedShape();

        // Handle TypeListView
        if (anEvent.equals("TypeListView")) {
            String type = anEvent.getStringValue();
            vshape.setViewType(type);
            vshape.setStandardSize();
        }

        // Handle TextView
        else if (anEvent.equals("TextView")) {
            String text = anEvent.getStringValue();
            vshape.setText(text);
        }
    }
}