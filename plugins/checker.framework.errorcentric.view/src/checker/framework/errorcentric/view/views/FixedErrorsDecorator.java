package checker.framework.errorcentric.view.views;

import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class FixedErrorsDecorator implements ILabelDecorator {

    // Method to decorate Image
    public Image decorateImage(Image image, Object object) {
        if (object instanceof MarkerResolutionTreeNode) {
            Image overlayImage = PlatformUI.getWorkbench().getSharedImages()
                    .getImage(ISharedImages.IMG_DEC_FIELD_ERROR);
            OverlayImageIcon newImage = new OverlayImageIcon(image,
                    overlayImage);
            return newImage.createImage();
        }
        return null;
    }

    // Method to decorate Text
    public String decorateText(String label, Object object) {
        if (object instanceof MarkerResolutionTreeNode) {
            return "what! " + label;
        }
        return "hey! " + label;
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        // TODO Auto-generated method stub

    }
}
