package org.omnifaces.renderkit;

import java.util.Iterator;

import jakarta.faces.context.FacesContext;
import jakarta.faces.render.RenderKit;
import jakarta.faces.render.RenderKitFactory;

/**
 * <p>
 * This render kit factory needs to be registered as follows in <code>faces-config.xml</code> to get the
 * {@link SubresourceIntegrityRenderKit} and in turn the {@link SubresourceIntegrityRendererExtension} to run:
 * <pre>
 * &lt;factory&gt;
 *     &lt;render-kit-factory&gt;org.omnifaces.renderkit.SubresourceIntegrityRenderKitFactory&lt;/render-kit-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 4.6
 * @see SubresourceIntegrityRenderKit
 */
public class SubresourceIntegrityRenderKitFactory extends RenderKitFactory {

    public SubresourceIntegrityRenderKitFactory(RenderKitFactory wrapped) {
        super(wrapped);
    }

    @Override
    public void addRenderKit(String renderKitId, RenderKit renderKit) {
        getWrapped().addRenderKit(renderKitId, renderKit);
    }

    @Override
    public RenderKit getRenderKit(FacesContext context, String renderKitId) {
        var renderKit = getWrapped().getRenderKit(context, renderKitId);
        return HTML_BASIC_RENDER_KIT.equals(renderKitId) ? new SubresourceIntegrityRenderKit(renderKit) : renderKit;
    }

    @Override
    public Iterator<String> getRenderKitIds() {
        return getWrapped().getRenderKitIds();
    }
}
