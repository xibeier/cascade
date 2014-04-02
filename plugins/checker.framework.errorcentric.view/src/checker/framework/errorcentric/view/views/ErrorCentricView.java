package checker.framework.errorcentric.view.views;

import java.util.Set;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import checker.framework.change.propagator.ActionableMarkerResolution;
import checker.framework.change.propagator.ShadowProject;
import checker.framework.change.propagator.ShadowProjectFactory;
import checker.framework.errorcentric.propagator.commands.InferCommandHandler;
import checker.framework.errorcentric.propagator.commands.InferNullnessCommandHandler;
import checker.framework.quickfixes.descriptors.Fixer;

import com.google.common.base.Optional;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class ErrorCentricView extends ViewPart implements TreeLabelUpdater {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "checker.framework.errorcentric.view.views.ErrorCentricView";

    private TreeViewer viewer;
    private DrillDownAdapter drillDownAdapter;
    private Action refreshAction;
    private Action action2;
    private Action doubleClickAction;
    private TreeObject invisibleRoot;
    private IJavaProject javaProject;

    /**
     * The constructor.
     */
    public ErrorCentricView() {
    }

    private TreeObject initializeInput() {
        invisibleRoot = new TreeObject("");
        if (InferCommandHandler.checkerID == null) {
            return null;
        }
        if (!InferCommandHandler.selectedJavaProject.isPresent()) {
            return null;
        }
        javaProject = InferCommandHandler.selectedJavaProject.get();
        ShadowProject shadowProject = new ShadowProjectFactory(javaProject)
                .get();
        shadowProject.runChecker(InferCommandHandler.checkerID);
        Set<ActionableMarkerResolution> resolutions = shadowProject
                .getResolutions();
        invisibleRoot.addChildren(AddedErrorTreeNode.createTreeNodesFrom(
                resolutions, this));
        return invisibleRoot;
    }

    /**
     * This is a callback that will allow us to create the viewer and initialize
     * it.
     */
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        drillDownAdapter = new DrillDownAdapter(viewer);
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new DecoratingLabelProvider(
                new ViewLabelProvider(), new FixedErrorsDecorator()));
        viewer.setSorter(new NameSorter());
        viewer.setInput(initializeInput());
        viewer.getTree().setLinesVisible(true);
        makeActions();
        hookContextMenu();
        hookDoubleClickAction();
        hookSelectionAction();
        contributeToActionBars();
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                ErrorCentricView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

    private void fillLocalPullDown(IMenuManager manager) {
        manager.add(refreshAction);
        manager.add(new Separator());
        manager.add(action2);
    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(refreshAction);
        manager.add(action2);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(refreshAction);
        manager.add(action2);
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
    }

    private void makeActions() {
        refreshAction = new Action() {
            public void run() {
                viewer.refresh();
            }
        };
        refreshAction.setText("Refresh");
        refreshAction.setToolTipText("Recomputes the error/change tree.");
        refreshAction.setImageDescriptor(PlatformUI.getWorkbench()
                .getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

        action2 = new Action() {
            public void run() {
                showMessage("Action 2 executed");
            }
        };
        action2.setText("Action 2");
        action2.setToolTipText("Action 2 tooltip");
        action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
        doubleClickAction = new Action() {
            public void run() {
                Optional<TreeObject> selectedTreeObject = getSelectedTreeObject(viewer
                        .getSelection());
                Optional<MarkerResolutionTreeNode> resolution = getSelectedMarkResolution(selectedTreeObject);
                if (resolution.isPresent()) {
                    resolution.get().getResolution().run();
                }
                Optional<ErrorTreeNode> error = getSelectedError(selectedTreeObject);
                if (error.isPresent()) {
                    error.get().reveal();
                }
            }

        };
    }

    private Optional<MarkerResolutionTreeNode> getSelectedMarkResolution(
            Optional<TreeObject> optionalTreeObject) {
        Optional<MarkerResolutionTreeNode> optionalMarkerTreeNode = Optional
                .absent();
        if (optionalTreeObject.isPresent()) {
            if (optionalTreeObject.isPresent()) {
                TreeObject treeObject = optionalTreeObject.get();
                if (treeObject instanceof MarkerResolutionTreeNode) {
                    optionalMarkerTreeNode = Optional
                            .of((MarkerResolutionTreeNode) treeObject);
                }
            }
        }
        return optionalMarkerTreeNode;
    }

    private Optional<ErrorTreeNode> getSelectedError(
            Optional<TreeObject> optionalTreeObject) {
        Optional<ErrorTreeNode> optionalMarkerTreeNode = Optional.absent();
        if (optionalTreeObject.isPresent()) {
            if (optionalTreeObject.isPresent()) {
                TreeObject treeObject = optionalTreeObject.get();
                if (treeObject instanceof ErrorTreeNode) {
                    optionalMarkerTreeNode = Optional
                            .of((ErrorTreeNode) treeObject);
                }
            }
        }
        return optionalMarkerTreeNode;
    }

    private Optional<TreeObject> getSelectedTreeObject(ISelection selection) {
        Object selectedObject = ((IStructuredSelection) selection)
                .getFirstElement();
        if (selectedObject instanceof TreeObject) {
            return Optional.of((TreeObject) selectedObject);
        }
        return Optional.absent();
    }

    private void hookDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
    }

    private void hookSelectionAction() {
        viewer.addPostSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                Optional<TreeObject> selectedTreeObject = getSelectedTreeObject(event
                        .getSelection());
                if (selectedTreeObject.isPresent()) {
                    viewer.setLabelProvider(new DecoratingLabelProvider(
                            new ViewLabelProvider(), new SimpleDecorator(
                                    selectedTreeObject.get().getClass())));

                }

                Optional<MarkerResolutionTreeNode> optionalResolution = getSelectedMarkResolution(selectedTreeObject);
                if (optionalResolution.isPresent()) {
                    MarkerResolutionTreeNode resolution = optionalResolution
                            .get();
                    IJavaProject javaProject = InferNullnessCommandHandler.selectedJavaProject
                            .get();
                    Fixer fixer = resolution.getResolution().createFixer(
                            javaProject);
                    selectAndReveal(fixer);

                }
            }

            private void selectAndReveal(Fixer fixer) {
                new CodeSnippetRevealer().reveal(fixer.getCompilationUnit(),
                        fixer.getOffset(), fixer.getLength());
            }

        });
    }

    private void showMessage(String message) {
        MessageDialog.openInformation(viewer.getControl().getShell(),
                "Changes View", message);
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public void update(final TreeObject node) {
        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                viewer.update(node, null);
            }
        });
    }
}