/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import com.reportmill.graphics.RMColor;
import com.reportmill.shape.RMGraph.*;
import snap.util.ListUtils;

import java.util.*;

/**
 * A shape class for a generated RMGraph.
 */
abstract class RMGraphRPG {

    // The Graph
    RMGraph _graph;

    // The ReportOwner
    ReportOwner _rptOwner;

    // The graph objects (group)
    RMGroup _objects;

    // The list of series for the generated graph
    List<RMGraphSeries> _series;

    // The list of sections for the generated graph
    List<RMGraphSection> _sections;

    // The list of colors
    List<RMColor> _colors;

    // The graph shape
    RMParentShape _graphShape;

    /**
     * Creates a new RMGraphRPG for a graph and a report owner.
     */
    public RMGraphRPG(RMGraph aGraph, ReportOwner anRptOwner)
    {
        // Set graph and ReportOwner
        _graph = aGraph;
        _rptOwner = anRptOwner;

        // Get dataset: If parent TableRow was found, get dataset from table row group
        List<?> dataset;
        String datasetKey = _graph.getDatasetKey();
        RMShape parentTableRow = _graph.getParent(RMTableRow.class);
        if (parentTableRow != null) {

            // If no dataset key, use last data bearing object if RMGroup
            if (datasetKey == null || datasetKey.isEmpty()) {

                // Get table row group from ReportMill's data bearing objects list
                RMGroup tableRowGroup = (RMGroup) anRptOwner.peekDataStack();

                // Make dataset a copy of last table row group
                dataset = tableRowGroup.cloneDeep();

                // If tableRowGroup is leaf, embed it inside a list (can't remember why I thought this was needed)
                if (tableRowGroup.isLeaf())
                    dataset = Arrays.asList(dataset); //ListUtils.newList(dataset);
            }

            // If dataset key, evaluate it
            else dataset = RMKeyChain.getListValue(anRptOwner, datasetKey);
        }

        // If parent isn't table row, just ask reportmill for dataset
        else dataset = anRptOwner.getKeyChainListValue(datasetKey);

        // Get filtered list from dataset and graph filter key
        List<?> filteredList = dataset;
        if (dataset != null) {
            String filterKey = _graph.getFilterKey();
            if (filterKey != null && !filterKey.isEmpty()) {
                RMKeyChain keyChain = RMKeyChain.getKeyChain(filterKey);
                filteredList = ListUtils.filter(filteredList, item -> RMKeyChain.getBoolValue(item, keyChain));
            }
        }

        // Get filtered list as group
        _objects = filteredList instanceof RMGroup ? (RMGroup) filteredList : new RMGroup(filteredList);

        // Do grouping (sorting really)
        RMGrouping grouping = _graph.getGrouping();
        _objects.groupByLeafKey(grouping.getKey()); // Group by leaf key (just turns leaf objects into one object groups)
        _objects.topNSortBy(grouping.getTopNSort()); // Do top n sort for grouping
        _objects.sortBy(grouping); // Do sorts for grouping

        // Load Series, Sections and Intervals
        _series = RMGraphSeries.getSeries(this);
        _sections = RMGraphSection.getSections(this);

        // Generate GraphView
        _graphShape = createGraphShape();
        ((GraphShape) _graphShape).setGraphRPG(this);
    }

    /**
     * Returns the source graph.
     */
    public RMGraph getGraph()  { return _graph; }

    /**
     * Returns the ReportOwner.
     */
    public ReportOwner getReportOwner()  { return _rptOwner; }

    /**
     * Returns the graph shape.
     */
    public RMParentShape getGraphShape()  { return _graphShape; }

    /**
     * Creates the graph shape.
     */
    protected abstract RMParentShape createGraphShape();

    /**
     * Returns whether section layout is meshed.
     */
    public boolean isMeshed()
    {
        return _graph.getSectionLayout() == SectionLayout.Merge;
    }

    /**
     * Returns the graph objects.
     */
    public RMGroup getObjects()  { return _objects; }

    /**
     * Returns the number of series in this graph.
     */
    public int getSeriesCount()
    {
        return getSeries().size();
    }

    /**
     * Returns the individual series at the given index.
     */
    public RMGraphSeries getSeries(int anIndex)
    {
        return _series.get(anIndex);
    }

    /**
     * Returns the list of series.
     */
    public List<RMGraphSeries> getSeries()  { return _series; }

    /**
     * Returns the number of sections in this graph.
     */
    public int getSectionCount()
    {
        return getSections().size();
    }

    /**
     * Returns the individual section at the given index.
     */
    public RMGraphSection getSection(int anIndex)
    {
        return _sections.get(anIndex);
    }

    /**
     * Returns the graph sections.
     */
    public List<RMGraphSection> getSections()  { return _sections; }

    /**
     * Returns the colors.
     */
    public List<RMColor> getColors()
    {
        // If already set, just return
        if (_colors != null) return _colors;

        // If no graph key, just use Graph.Colors
        String colorKey = _graph.getColorKey();
        if (colorKey == null)
            return _colors = _graph.getColors();

        // Otherwise, get colors
        RMGraphSeries series = getSeries(0);
        List<RMColor> colors = new ArrayList<>();
        for (int i = 0, iMax = series.getItemCount(); i < iMax; i++) {
            RMGraphSeries.Item item = series.getItem(i);
            Object obj = item.getGroup();
            Object val = RMKeyChain.getValue(obj, colorKey);
            if (val instanceof String) {
                RMColor color = RMColor.get(val);
                if (color == null)
                    color = _graph.getColor(i);
                colors.add(color);
            } else colors.add(_graph.getColor(i));
        }

        // Return colors from ColorKey
        return _colors = colors;
    }

    /**
     * Returns the color count.
     */
    public int getColorCount()
    {
        return getColors().size();
    }

    /**
     * Returns the individual color at given index.
     */
    public RMColor getColor(int anIndex)
    {
        return getColors().get(anIndex % getColorCount());
    }

    /**
     * An interface to identify generated graph shapes.
     */
    public interface GraphShape {

        /**
         * Returns the GraphRPG.
         */
        RMGraphRPG getGraphRPG();

        /**
         * Sets the RMGraphRPG.
         */
        void setGraphRPG(RMGraphRPG aGRPG);
    }
}