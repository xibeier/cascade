package checker.framework.errorcentric.propagator.commands;

import org.checkerframework.eclipse.actions.CheckerHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import checker.framework.change.propagator.CheckerID;
import checker.framework.errorcentric.view.views.ErrorCentricView;

import com.google.common.base.Optional;

public abstract class InferCommandHandler extends CheckerHandler {

    public static CheckerID checkerID;

    public static Optional<IJavaProject> selectedJavaProject = Optional
            .absent();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            selectedJavaProject = getSelectedProject(getSelection(event));
            if (selectedJavaProject.isPresent()) {
                // Adapted from http://stackoverflow.com/a/172082
                IWorkbenchPage activePage = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                IViewPart view = activePage.findView(ErrorCentricView.ID);
                if (view != null) {
                    ((ErrorCentricView) view).refreshView();
                } else {
                    activePage.showView(ErrorCentricView.ID);
                }
            }
        } catch (PartInitException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Optional<IJavaProject> getSelectedProject(ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            return Optional
                    .of((IJavaProject) ((IStructuredSelection) selection)
                            .getFirstElement());
        } else {
            return Optional.absent();
        }
    }

}
