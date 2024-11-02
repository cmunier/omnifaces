package org.omnifaces.renderkit;

import static org.omnifaces.util.Utils.isOneOf;

import java.io.IOException;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.render.Renderer;
import jakarta.faces.render.RendererWrapper;

import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * <p>
 * The {@link SubresourceIntegrityRendererExtension} is intended as an extension to the standard script and stylesheet
 * resource renderer in order to add the <code>integrity</code> attribute as a pass-through attribute with a base64
 * encoded sha384 hash as SRI, see also <a href="https://developer.mozilla.org/en-US/docs/Web/Security/Subresource_Integrity">MDN</a>.
 * Additionaly, the <code>crossorigin</code> attribute will be added with the value of <code>anonymous</code>.
 *
 * @author Bauke Scholtz
 * @since 4.6
 * @see SubresourceIntegrityRenderKit
 */
public class SubresourceIntegrityRendererExtension extends RendererWrapper {

    public SubresourceIntegrityRendererExtension(Renderer wrapped) {
        super(wrapped);
    }

    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        var passThroughAttributes = component.getPassThroughAttributes();

        if (passThroughAttributes.get("integrity") == null && component.getAttributes().get("name") != null && isOneOf(passThroughAttributes.get("crossorigin"), "anonymous", null)) {
            passThroughAttributes.put("integrity", new ResourceIdentifier(component).getIntegrity(context));
            passThroughAttributes.put("crossorigin", "anonymous");
        }

        super.encodeBegin(context, component);
    }
}
