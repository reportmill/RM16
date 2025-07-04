/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package com.reportmill.shape;
import com.reportmill.base.*;
import snap.util.ListUtils;
import java.util.List;

/**
 * Report generation for RMCrossTab.
 */
class RMCrossTabRPG {

    /**
     * Sets a reportmill for this crosstab (which really gets the dataset and calls setObjects).
     */
    public RMShape rpgCrossTab(ReportOwner anRptOwner, RMCrossTab aCTab)
    {
        // Get dataset: If parent TableRow available, get dataset from ReportOwner
        List<?> dataset;
        RMShape parentTableRow = aCTab.getParent(RMTableRow.class);
        if (parentTableRow != null) {

            // If no dataset key, use last data bearing object if RMGroup
            if (aCTab.getDatasetKey() == null || aCTab.getDatasetKey().isEmpty()) {
                RMGroup tableRowGroup = (RMGroup) anRptOwner.peekDataStack();
                dataset = tableRowGroup.cloneDeep(); // Make dataset a copy of table row group
            }

            // If dataset key, evaluate it
            else dataset = RMKeyChain.getListValue(anRptOwner, aCTab.getDatasetKey());
        }

        // If parent isn't table row, just ask ReportOwner for dataset
        else dataset = anRptOwner.getKeyChainListValue(aCTab.getDatasetKey());

        // Get parent table row group (if there is one) by removing so it won't be used to evaluate cells
        //Object parentTableRowGroup = parentTableRow==null? null : anRptOwner.popDataStack();

        // Apply filter key to dataset
        if (dataset != null) {
            String filterKey = aCTab.getFilterKey();
            if (filterKey != null && !filterKey.isEmpty()) {
                RMKeyChain keyChain = RMKeyChain.getKeyChain(filterKey);
                dataset = ListUtils.filter(dataset, item -> RMKeyChain.getBoolValue(item, keyChain));
            }
        }

        // Get dataset as group
        RMGroup datasetGroup = dataset instanceof RMGroup ? (RMGroup) dataset : new RMGroup(dataset);

        // Get standard clone (minus bindings)
        RMCrossTab clone = aCTab.cloneDeep();
        while (clone.getBindingCount() > 0) clone.removeBinding(clone.getBindingCount() - 1);

        // Install dataset group in all cells
        for (int i = 0, iMax = clone.getRowCount(); i < iMax; i++)
            for (int j = 0, jMax = clone.getColCount(); j < jMax; j++)
                clone.getCell(i, j).setGroup(datasetGroup);

        // Iterate over rows/columns
        for (int i = 0; i < clone.getRowCount(); i++) {
            for (int j = 0; j < clone.getColCount(); j++) {
                RMCrossTabCell cell = clone.getCell(i, j);

                // If cell didn't originate at this row,col, just continue
                if (cell.getRow() != i || cell.getCol() != j) continue;

                // Get cell group, clone cell and set clone
                RMGroup cellGroup = getCellGroup(clone, cell, datasetGroup, dataset);
                anRptOwner.pushDataStack(cellGroup);
                RMCrossTabCell cellClone = (RMCrossTabCell) anRptOwner.rpg(cell, clone);
                anRptOwner.popDataStack();
                clone.setCell(cellClone, i, j, cell.getRowSpan(), cell.getColSpan());
            }
        }

        // Set rows best height
        for (int i = 0, iMax = clone.getRowCount(); i < iMax; i++) {
            RMCrossTabRow row = clone.getRow(i);
            row.setHeight(row.getBestHeight());
        }

        // Restore parent table row group to ReportMill's data bearing objects list
        //if(parentTableRowGroup!=null) anRptOwner.pushDataStack(parentTableRowGroup);

        // Return clone
        return clone;
    }

    /**
     * Returns the cell group for a cell.
     */
    private RMGroup getCellGroup(RMCrossTab aCtab, RMCrossTabCell aCell, RMGroup aDatasetGroup, List<?> aDataset)
    {
        // Get cell grouping (if no grouping key, just return cell group)
        RMGrouping grouping = aCell.getGrouping();
        if (grouping == null)
            return aCell.getGroup();

        // Reset grouping to null
        aCell.setGrouping(null);

        // Get cell group
        RMGroup group = aCell.getGroup();

        // If group is dataset group, get new group
        if (group == aDatasetGroup)
            group = new RMGroup(aDataset);

        // Group existing group by cell grouping
        group.groupBy(grouping);

        // If cell is column header, add column for each child group
        if (aCell.isColHeader()) {

            // Add extra columns for each group item
            addNeededColumns(aCtab, aCell, group);

            // Iterate over columns added for group
            for (int i = 0, iMax = group.size() * aCell.getColSpan(); i < iMax; i++) {

                // Get column group
                RMGroup columnGroup = group.getGroup(i / aCell.getColSpan());

                // Iterate over table rows and install group in cells under column cell
                for (int j = aCell.getRow(), jMax = aCtab.getRowCount(); j < jMax; j++) {

                    // Get row cell (just continue if not cell origin)
                    RMCrossTabCell cell = aCtab.getCell(j, aCell.getCol() + i);
                    if (cell.getRow() != j || cell.getCol() != aCell.getCol() + i) continue;

                    // If beyond first row, clone column group
                    if (j > aCell.getRow())
                        columnGroup = columnGroup.clone();

                    // Set cell group to column group
                    cell.setGroup(columnGroup);
                }
            }
        }

        // If cell is row header, add row for each child group
        else if (aCell.isRowHeader()) {

            // Add necessary rows for all group items
            addNeededRows(aCtab, aCell, group);

            // Iterate over rows added for group
            for (int i = 0, iMax = group.size() * aCell.getRowSpan(); i < iMax; i++) {

                // Get the row group
                RMGroup rowGroup = group.getGroup(i / aCell.getRowSpan());

                // Set groups for cells in this row in given cell and beyond
                for (int j = aCell.getCol(), jMax = aCtab.getColCount(); j < jMax; j++) {

                    // Get current loop row cell (just continue if not cell origin)
                    RMCrossTabCell cell = aCtab.getCell(aCell.getRow() + i, j);
                    if (cell.getRow() != aCell.getRow() + i || cell.getCol() != j) continue;

                    // Get the cell's group
                    RMGroup cellGroup = cell.getGroup();

                    // If current cell group is row header's original group or dataset group, just set to row group
                    if (cellGroup == group || cellGroup == aDatasetGroup)
                        cell.setGroup(rowGroup);

                        // Otherwise, set group from current group
                    else {

                        // If column cell group hasn't been grouped by this key yet, group it
                        if (cellGroup.getKey() == null) {

                            // Get values from original cell group, so cell group will expand to the same number of groups
                            List<?> values = group.getAllValues(grouping.getKey());

                            // Group by grouping with explicit values
                            cellGroup.groupByKey(grouping.getKey(), values);
                            cellGroup.topNSortBy(grouping.getTopNSort());
                        }

                        // Set cell group to appropriate child
                        cell.setGroup(cellGroup.getGroup(i / aCell.getRowSpan()));
                    }
                }
            }
        }

        // If cell is column header cell
        return aCell.getGroup();
    }

    /**
     * Adds necessary rows for a header cell and a group with multiple items.
     */
    void addNeededRows(RMCrossTab aCTab, RMCrossTabCell aCell, RMGroup aGroup)
    {
        int row = aCell.getRow();
        int col = aCell.getCol();
        int rowSpan = aCell.getRowSpan();
        int colCount = aCTab.getColCount();
        for (int g = 1, gMax = aGroup.size(); g < gMax; g++)
            addRowsForGroup(aCTab, row + g * rowSpan, row, col, rowSpan, colCount);
    }

    /**
     * Adds necessary rows for a header cell and a group.
     */
    void addRowsForGroup(RMCrossTab aCTab, int index, int row, int col, int rowSpan, int colCount)
    {
        // Create new row cells for group
        RMCrossTabCell[][] cells = new RMCrossTabCell[rowSpan][colCount];
        for (int i = 0; i < rowSpan; i++)
            for (int j = 0, jMax = colCount; j < jMax; j++) {
                if (cells[i][j] != null) continue;

                // If before ref column and cell spans ref cell, merge cell. If at or after ref column index, copy cell
                RMCrossTabCell cell = aCTab.getCell(row + i, j), ncell = cell;
                int newRowSpan;
                if (j < col && cell.getRowEnd() >= row + rowSpan - 1)
                    newRowSpan = rowSpan;
                else {
                    ncell = cell.clone();
                    newRowSpan = Math.min(cell.getRowEnd() + 1 - row - i, rowSpan - i);
                }

                // Pad cell into cells
                for (int k = 0; k < newRowSpan; k++)
                    for (int l = 0, lMax = ncell.getColSpan(); l < lMax; l++) cells[i + k][j + l] = ncell;
            }

        // Create new rows, add cells to row and add row to CrossTab
        for (int i = 0; i < rowSpan; i++) {
            RMCrossTabRow newRow = aCTab.getRow(row + i).clone();
            for (int j = 0; j < colCount; j++)
                newRow.addCell(cells[i][j]);
            aCTab.addRow(newRow, index + i);
        }
    }

    /**
     * Adds necessary rows for a header cell and a group with multiple items.
     */
    void addNeededColumns(RMCrossTab aCTab, RMCrossTabCell aCell, RMGroup aGroup)
    {
        int row = aCell.getRow();
        int col = aCell.getCol();
        int colSpan = aCell.getColSpan();
        int rowCount = aCTab.getRowCount();
        for (int g = 1, gMax = aGroup.size(); g < gMax; g++)
            addColsForGroup(aCTab, col + g * colSpan, row, col, colSpan, rowCount);
    }

    /**
     * Adds necessary columns for a header cell and a group.
     */
    void addColsForGroup(RMCrossTab aCTab, int index, int row, int col, int colSpan, int rowCount)
    {
        // Create new column cells for group
        RMCrossTabCell[][] cells = new RMCrossTabCell[rowCount][colSpan];
        for (int i = 0; i < rowCount; i++)
            for (int j = 0; j < colSpan; j++) {
                if (cells[i][j] != null) continue;

                // If before ref row and cell spans ref cell, merge cell. If at or after ref column index, copy cell.
                RMCrossTabCell cell = aCTab.getCell(i, col + j), ncell = cell;
                int newColSpan;
                if (i < row && cell.getColEnd() >= col + colSpan - 1)
                    newColSpan = colSpan;
                else {
                    ncell = cell.clone();
                    newColSpan = Math.min(cell.getColEnd() + 1 - col - j, colSpan - j);
                }

                // Pad cell into cells
                for (int k = 0; k < newColSpan; k++)
                    for (int l = 0, lMax = ncell.getRowSpan(); l < lMax; l++)
                        cells[i + l][j + k] = ncell;
            }

        // Create new columns, add cells to column and add column to CrossTab
        for (int j = 0; j < colSpan; j++) {
            RMCrossTabCol newCol = aCTab.getCol(col + j).clone();
            for (int i = 0; i < rowCount; i++)
                newCol.addCell(cells[i][j]);
            aCTab.addCol(newCol, index + j);
        }
    }

}