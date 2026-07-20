/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.base;
import java.util.*;
import com.reportmill.shape.RMArchiver;
import snap.props.PropChange;
import snap.props.PropChangeListener;
import snap.props.PropChangeSupport;
import snap.util.*;

/**
 * This class simply manages a list of groupings and has some nice convenience methods.
 */
public class RMGrouper implements Cloneable, RMArchiver.Archivable {

    // The list of groupings
    private List<RMGrouping> _groupings = new ArrayList<>();

    // Selected group index (editing only)
    private int _selectedGroupingIndex = 0;

    // The PropChangeSupport
    private PropChangeSupport _pcs = PropChangeSupport.EMPTY;

    // A listener to catch RMGrouping PropChange
    private PropChangeListener _groupingLsnr = this::handleGroupingPropChange;

    /**
     * Constructor.
     */
    public RMGrouper()
    {
    }

    /**
     * Returns the number of groupings in this grouper.
     */
    public int getGroupingCount()  { return _groupings.size(); }

    /**
     * Returns the grouping at the given index.
     */
    public RMGrouping getGrouping(int anIndex)  { return _groupings.get(anIndex); }

    /**
     * Returns the list of groupings
     */
    public List<RMGrouping> getGroupings()  { return _groupings; }

    /**
     * Adds a given grouping to grouper's list of groupings.
     */
    public void addGrouping(RMGrouping aGrouping)  { addGrouping(aGrouping, getGroupingCount()); }

    /**
     * Adds a given grouping to grouper's list of groupings.
     */
    public void addGrouping(RMGrouping aGrouping, int anIndex)
    {
        // Add grouping to list
        _groupings.add(anIndex, aGrouping);

        // Start listening to property change events
        aGrouping.addPropChangeListener(_groupingLsnr);

        // Set selected grouping index to new grouping
        _selectedGroupingIndex = anIndex;

        // Fire property change
        firePropChange("Grouping", null, aGrouping, anIndex);
    }

    /**
     * Removes the grouping at the given index.
     */
    public RMGrouping removeGrouping(int anIndex)
    {
        // Remove grouping at index
        RMGrouping grouping = _groupings.remove(anIndex);

        // Stop listening to property change events
        grouping.removePropChangeListener(_groupingLsnr);

        // Adjust selected grouping index if needed
        _selectedGroupingIndex = Math.min(_selectedGroupingIndex, getGroupingCount() - 1);

        // Fire property change
        firePropChange("Grouping", grouping, null, anIndex);

        // Return removed object
        return grouping;
    }

    /**
     * Returns the grouping with the given key.
     */
    public RMGrouping getGrouping(String aKey)
    {
        int index = indexOf(aKey);
        return index >= 0 ? getGrouping(index) : null;
    }

    /**
     * Returns the last grouping.
     */
    public RMGrouping getGroupingLast()  { return getGrouping(getGroupingCount() - 1); }

    /**
     * Returns the index for the grouping with the given key.
     */
    public int indexOf(String aKey)
    {
        for (int i = 0, iMax = getGroupingCount(); i < iMax; i++)
            if (getGrouping(i).getKey().equals(aKey))
                return i;
        return -1;
    }

    /**
     * Adds a given list of groupings to grouper's list of groupings.
     */
    public void addGroupings(List<RMGrouping> aList)
    {
        for (int i = 0, iMax = aList != null ? aList.size() : 0; i < iMax; i++)
            addGrouping(aList.get(i), getGroupingCount());
    }

    /**
     * Adds a new grouping with the given key.
     */
    public void addGroupingForKey(String aKey)
    {
        addGroupingForKey(aKey, getGroupingCount());
    }

    /**
     * Adds a new grouping with the given key at the given index.
     */
    public void addGroupingForKey(String aKey, int anIndex)
    {
        RMGrouping grouping = new RMGrouping(aKey);
        addGrouping(grouping, anIndex);
    }

    /**
     * Removes the given grouping.
     */
    public void removeGrouping(RMGrouping aGrouping)
    {
        removeGrouping(ListUtils.indexOfId(_groupings, aGrouping));
    }

    /**
     * Moves a grouping from given fromIndex to given toIndex.
     */
    public void moveGrouping(int fromIndex, int toIndex)
    {
        // If from or to index is invalid, just return, otherwise remove grouping from given index and add to given index
        if (fromIndex >= getGroupingCount() - 1 || toIndex >= getGroupingCount() - 1) return;
        RMGrouping grouping = removeGrouping(fromIndex);
        addGrouping(grouping, toIndex);
    }

    /**
     * Returns the currently selected grouping's index (for editing, mostly).
     */
    public int getSelectedGroupingIndex()  { return _selectedGroupingIndex; }

    /**
     * Sets the currently selected grouping by index (for editing, mostly).
     */
    public void setSelectedGroupingIndex(int anIndex)  { _selectedGroupingIndex = anIndex; }

    /**
     * Returns the currently selected grouping (while editing only).
     */
    public RMGrouping getSelectedGrouping()  { return getGrouping(_selectedGroupingIndex); }

    /**
     * Separates given objects into RMGroups defined by groupings.
     */
    public RMGroup groupObjects(List<?> aList)
    {
        RMGroup group = new RMGroup(aList);
        group.groupBy(this, 0);
        return group;
    }

    /**
     * Listen for property changes and forward to grouper's property change listeners.
     */
    protected void handleGroupingPropChange(PropChange anEvent)
    {
        firePropChange(anEvent);
    }

    /**
     * Add listener.
     */
    public void addPropChangeListener(PropChangeListener aLsnr)
    {
        if (_pcs == PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
        _pcs.addPropChangeListener(aLsnr);
    }

    /**
     * Remove listener.
     */
    public void removePropChangeListener(PropChangeListener aLsnr)
    {
        _pcs.removePropChangeListener(aLsnr);
    }

    /**
     * Fires a property change for given property name, old value, new value and index.
     */
    protected void firePropChange(String aProp, Object oldVal, Object newVal)
    {
        if (!_pcs.hasListener(aProp)) return;
        PropChange pc = new PropChange(this, aProp, oldVal, newVal);
        firePropChange(pc);
    }

    /**
     * Fires a property change for given property name, old value, new value and index.
     */
    protected void firePropChange(String aProp, Object oldVal, Object newVal, int anIndex)
    {
        if (!_pcs.hasListener(aProp)) return;
        PropChange pc = new PropChange(this, aProp, oldVal, newVal, anIndex);
        firePropChange(pc);
    }

    /**
     * Fires a given property change.
     */
    protected void firePropChange(PropChange aPC)
    {
        _pcs.firePropChange(aPC);
    }

    /**
     * Standard clone implementation.
     */
    public RMGrouper clone()
    {
        // Do normal clone
        RMGrouper clone;
        try { clone = (RMGrouper) super.clone(); }
        catch (CloneNotSupportedException e) { throw new RuntimeException(e); }

        // Clear PropChangeSupport
        clone._pcs = PropChangeSupport.EMPTY;

        // Clone deep grouping
        clone._groupings = new ArrayList<>();
        for (RMGrouping grp : _groupings) {
            RMGrouping grp2 = grp.clone();
            clone.addGrouping(grp2);
        }

        // Return clone
        return clone;
    }

    /**
     * Standard equals implementation.
     */
    public boolean equals(Object anObj)
    {
        if (anObj == this) return true;
        return anObj instanceof RMGrouper other && other._groupings.equals(_groupings);
    }

    /**
     * Standard toString implementation.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder("RMGrouper { Keys=");
        for (RMGrouping grp : getGroupings())
            sb.append(grp.getKey()).append(", ");
        if (getGroupingCount() > 0)
            sb.delete(sb.length() - 2, sb.length());
        return sb.append(" }").toString();
    }

    /**
     * XML archival.
     */
    public XMLElement toXML(RMArchiver anArchiver)
    {
        XMLElement e = new XMLElement("grouper");
        for (int i = 0, iMax = getGroupingCount(); i < iMax; i++)
            e.add(getGrouping(i).toXML(anArchiver));
        return e;
    }

    /**
     * XML unarchival.
     */
    public Object fromXML(RMArchiver anArchiver, XMLElement anElement)
    {
        _groupings = anArchiver.readListFromXmlForNameAndClass(anElement, "grouping", RMGrouping.class);
        return this;
    }
}