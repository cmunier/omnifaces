/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.renderer;

import static org.omnifaces.resourcehandler.DefaultResourceHandler.RES_NOT_FOUND;
import static org.omnifaces.util.FacesLocal.createResource;
import static org.omnifaces.util.Utils.isEmpty;

import java.io.IOException;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;
import jakarta.faces.render.FacesRenderer;
import jakarta.faces.render.Renderer;

import org.omnifaces.component.script.DeferredScript;
import org.omnifaces.component.script.ScriptFamily;
import org.omnifaces.resourcehandler.CombinedResourceHandler;
import org.omnifaces.resourcehandler.ResourceIdentifier;

/**
 * This renderer is the default renderer of {@link DeferredScript}. The rendering is extracted from the component so
 * that it can be reused by {@link CombinedResourceHandler}.
 *
 * @author Bauke Scholtz
 * @since 1.8
 */
@FacesRenderer(componentFamily=ScriptFamily.COMPONENT_FAMILY, rendererType=DeferredScriptRenderer.RENDERER_TYPE)
public class DeferredScriptRenderer extends Renderer {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The standard renderer type. */
    public static final String RENDERER_TYPE = "org.omnifaces.DeferredScript";

    // Actions --------------------------------------------------------------------------------------------------------

    /**
     * Writes a <code>&lt;script&gt;</code> element which calls <code>OmniFaces.DeferredScript.add</code> with as
     * arguments the script URL and, if any, the onbegin, onsuccess and/or onerror callbacks. If the script resource is
     * not resolvable, then a <code>RES_NOT_FOUND</code> will be written to <code>src</code> attribute instead.
     */
    @Override
    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        var id = new ResourceIdentifier(component);
        var resource = createResource(context, id);

        var writer = context.getResponseWriter();
        writer.startElement("script", component);
        writer.writeAttribute("type", "text/javascript", "type");

        if (resource != null) {
            writer.write("OmniFaces.DeferredScript.add('");
            writer.write(resource.getRequestPath());
            writer.write("','anonymous','");
            writer.write(id.getIntegrity(context));
            writer.write("'");

            var attributes = component.getAttributes();
            var onbegin = (String) attributes.get("onbegin");
            var onsuccess = (String) attributes.get("onsuccess");
            var onerror = (String) attributes.get("onerror");
            var hasOnbegin = !isEmpty(onbegin);
            var hasOnsuccess = !isEmpty(onsuccess);
            var hasOnerror = !isEmpty(onerror);

            if (hasOnbegin || hasOnsuccess || hasOnerror) {
                encodeFunctionArgument(writer, onbegin, hasOnbegin);
            }

            if (hasOnsuccess || hasOnerror) {
                encodeFunctionArgument(writer, onsuccess, hasOnsuccess);
            }

            if (hasOnerror) {
                encodeFunctionArgument(writer, onerror, true);
            }

            writer.write(");");
        }
        else {
            writer.writeURIAttribute("src", RES_NOT_FOUND, "src");
        }
    }

    private static void encodeFunctionArgument(ResponseWriter writer, String function, boolean hasFunction) throws IOException {
        if (hasFunction) {
            writer.write(",function() {");
            writer.write(function);
            writer.write('}');
        }
        else {
            writer.write(",null");
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        var writer = context.getResponseWriter();
        writer.endElement("script");
    }

}