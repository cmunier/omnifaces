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
package org.omnifaces.component.stylesheet;

import jakarta.faces.component.UIComponentBase;

/**
 * Base class which is to be shared between all components of the Stylesheet family.
 *
 * @author Bauke Scholtz
 * @since 4.5
 */
public abstract class StylesheetFamily extends UIComponentBase {

    // Public constants -----------------------------------------------------------------------------------------------

    /** The standard component family. */
    public static final String COMPONENT_FAMILY = "org.omnifaces.component.stylesheet";

    // UIComponent overrides ------------------------------------------------------------------------------------------

    /**
     * Returns {@link #COMPONENT_FAMILY}.
     */
    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }
    
    /**
     * Returns <code>false</code>.
     */
    @Override
    public boolean getRendersChildren() {
        return false;
    }
}