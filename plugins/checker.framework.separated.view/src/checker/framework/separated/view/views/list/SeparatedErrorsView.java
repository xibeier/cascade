package checker.framework.separated.view.views.list;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import checker.framework.change.propagator.ActionableMarkerResolution;
import checker.framework.change.propagator.ComparableMarker;
import checker.framework.separated.view.views.Colors;
import checker.framework.separated.view.views.Resolutions;
import checker.framework.separated.view.views.Views;
import checker.framework.separated.view.views.tree.MarkerResolutionTreeNode;
import checker.framework.separated.view.views.tree.SeparatedChangesView;

public class SeparatedErrorsView extends ViewPart implements ISelectionListener {
    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "checker.framework.separated.view.views.list.SeparatedErrorsView";

    private TableViewer viewer;

    /**
     * String representation of the previous selection in the changes view;
     */
    private String prevSelection = "";

    private boolean isHighlighted;

    /**
     * The constructor.
     */
    public SeparatedErrorsView() {
    }

    /**
     * This is a callback during the initialization phase.
     */
    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        Views.setErrorsView(this);
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
                | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer,
                SWT.NONE);
        final TableColumn column = viewerColumn.getColumn();
        column.setText("column1");
        column.setWidth(100);
        column.setResizable(true);
        column.setMoveable(true);
        viewerColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return element.toString();
            }
        });
        final Table table = viewer.getTable();
        table.setLinesVisible(true);
        viewer.setLabelProvider(new ListLabelProvider());
        viewer.setContentProvider(new ListContentProvider());
        viewer.setInput(prepareInput());

        getSite().setSelectionProvider(viewer);
        getSite().getPage().addSelectionListener(SeparatedChangesView.ID, this);

    }

    /**
     * Updates the error list with the supplied addedErrors and removedErrors.
     */
    public void updateErrors(Set<ComparableMarker> addedErrors,
            Set<ComparableMarker> removedErrors) {
        Table table = viewer.getTable();

        table.setRedraw(false);
        for (String message : getErrorMessages(removedErrors)) {
            removeErrorItem(table, message);
        }
        for (String message : getErrorMessages(addedErrors)) {
            addErrorItem(table, message);
        }
        table.setRedraw(true);

    }

    private Set<ComparableMarker> prepareInput() {
        Set<ActionableMarkerResolution> resolutions = Resolutions.get();
        for (ActionableMarkerResolution resolution : resolutions) {
            return resolution.getAllMarkers();
        }
        return new HashSet<ComparableMarker>();
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (!(selection instanceof TreeSelection)) {
            return;
        }
        if (selection.isEmpty()) {
            return;
        }
        if (prevSelection.equals(selection.toString())) {
            return;
        }
        prevSelection = selection.toString();

        TreeSelection treeSelection = (TreeSelection) selection;

        Iterator iterator = treeSelection.iterator();
        Set<ComparableMarker> removedMarkers = new HashSet<ComparableMarker>();
        while (iterator.hasNext()) {
            Object next = iterator.next();
            if (next instanceof MarkerResolutionTreeNode) {
                removedMarkers.addAll(((MarkerResolutionTreeNode) next)
                        .getResolution().getMarkers());
            }
        }

        Table table = viewer.getTable();
        Color defaultColor = table.getForeground();
        for (int i = 0; i < table.getItemCount(); ++i) {
            TableItem item = table.getItem(i);
            isHighlighted = removedMarkers.contains((ComparableMarker) item
                    .getData());
            item.setForeground(isHighlighted ? Colors.RED : defaultColor);
        }
    }

    private void removeErrorItem(Table table, String message) {
        int removeIndex = -1;
        for (int i = 0; i < table.getItemCount(); ++i) {
            TableItem item = table.getItem(i);
            if (item.getText().equals(message)) {
                removeIndex = i;
                break;
            }
        }
        if (removeIndex > -1) {
            table.remove(removeIndex);
        }
    }

    private void addErrorItem(Table table, String message) {
        TableItem newItem = new TableItem(table, table.getItemCount() - 1);
        newItem.setText(message);
    }

    private Set<String> getErrorMessages(Set<ComparableMarker> markers) {
        Set<String> messages = new HashSet<String>();
        for (ComparableMarker marker : markers) {
            messages.add(marker.getMessage());
        }
        return messages;
    }
}