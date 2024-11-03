package org.omnifaces.renderkit;

import static org.omnifaces.util.Renderers.RENDERER_TYPE_CSS;
import static org.omnifaces.util.Renderers.RENDERER_TYPE_JS;
import static org.omnifaces.util.Utils.isOneOf;

import jakarta.faces.component.UIOutput;
import jakarta.faces.render.RenderKit;
import jakarta.faces.render.RenderKitWrapper;
import jakarta.faces.render.Renderer;

import org.omnifaces.component.stylesheet.StylesheetFamily;
import org.omnifaces.renderer.CriticalStylesheetRenderer;

/**
 * <p>
 * The {@link SubresourceIntegrityRenderKit} will basically wrap the standard script and stylesheet resource renderer
 * with the {@link SubresourceIntegrityRendererExtension} which in turn takes care about automatically generating the
 * base64 encoded sha384 hash as SRI value for the <code>integrity</code> attribute of the generated HTML element.
 *
 * <h2>Installation</h2>
 * <p>
 * This render kit must be registered by a factory as follows in <code>faces-config.xml</code> in order to get it to run:
 * <pre>
 * &lt;factory&gt;
 *     &lt;render-kit-factory&gt;org.omnifaces.renderkit.SubresourceIntegrityRenderKitFactory&lt;/render-kit-factory&gt;
 * &lt;/factory&gt;
 * </pre>
 *
 * @author Bauke Scholtz
 * @since 4.6
 * @see SubresourceIntegrityRenderKitFactory
 * @see SubresourceIntegrityRendererExtension
 */
public class SubresourceIntegrityRenderKit extends RenderKitWrapper {

    public SubresourceIntegrityRenderKit(RenderKit wrapped) {
        super(wrapped);
    }

    @Override
    public void addRenderer(String family, String rendererType, Renderer renderer) {
        var needsIntegrity = UIOutput.COMPONENT_FAMILY.equals(family) && isOneOf(rendererType, RENDERER_TYPE_JS, RENDERER_TYPE_CSS)
                || StylesheetFamily.COMPONENT_FAMILY.equals(family) && CriticalStylesheetRenderer.RENDERER_TYPE.equals(rendererType);
        super.addRenderer(family, rendererType, needsIntegrity ? new SubresourceIntegrityRendererExtension(renderer) : renderer);
    }
}
