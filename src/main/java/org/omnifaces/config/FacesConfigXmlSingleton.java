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
package org.omnifaces.config;

import static org.omnifaces.util.Reflection.toClass;
import static org.omnifaces.util.Utils.isEmpty;
import static org.omnifaces.util.Utils.parseLocale;
import static org.omnifaces.util.Xml.createDocument;
import static org.omnifaces.util.Xml.getNodeList;
import static org.omnifaces.util.Xml.getTextContent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jakarta.faces.application.ResourceHandler;
import jakarta.faces.context.ExceptionHandlerFactory;
import jakarta.servlet.ServletContext;

import org.omnifaces.util.Servlets;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Enum singleton implementation of {@link FacesConfigXml}.
 *
 * @author Bauke Scholtz
 * @author Michele Mariotti
 * @since 3.1
 */
enum FacesConfigXmlSingleton implements FacesConfigXml {

    // Enum singleton -------------------------------------------------------------------------------------------------

    /**
     * Returns the lazily loaded enum singleton instance.
     */
    INSTANCE;

    // Private constants ----------------------------------------------------------------------------------------------

    private static final String APP_FACES_CONFIG_XML =
        "/WEB-INF/faces-config.xml";
    private static final String LIB_FACES_CONFIG_XML =
        "META-INF/faces-config.xml";
    private static final String XPATH_RESOURCE_BUNDLE =
        "application/resource-bundle";
    private static final String XPATH_DEFAULT_LOCALE =
        "application/locale-config/default-locale";
    private static final String XPATH_SUPPORTED_LOCALE =
        "application/locale-config/supported-locale";
    private static final String XPATH_RESOURCE_HANDLER =
        "application/resource-handler";
    private static final String XPATH_EXCEPTION_HANDLER_FACTORY =
        "factory/exception-handler-factory";
    private static final String XPATH_VAR =
        "var";
    private static final String XPATH_BASE_NAME =
        "base-name";
    private static final String ERROR_INITIALIZATION_FAIL =
        "FacesConfigXml failed to initialize. Perhaps your faces-config.xml contains a typo?";

    // Properties -----------------------------------------------------------------------------------------------------

    private Map<String, String> resourceBundles;
    private List<Locale> supportedLocales;
    private List<Class<? extends ResourceHandler>> resourceHandlers;
    private List<Class<? extends ExceptionHandlerFactory>> exceptionHandlerFactories;

    // Init -----------------------------------------------------------------------------------------------------------

    /**
     * Perform automatic initialization whereby the servlet context is obtained from CDI.
     */
    private FacesConfigXmlSingleton() {
        try {
            ServletContext servletContext = Servlets.getContext();
            Element facesConfigXml = loadFacesConfigXml(servletContext).getDocumentElement();
            XPath xpath = XPathFactory.newInstance().newXPath();
            resourceBundles = parseResourceBundles(facesConfigXml, xpath);
            supportedLocales = parseSupportedLocales(facesConfigXml, xpath);
            resourceHandlers = parseResourceHandlers(facesConfigXml, xpath);
            exceptionHandlerFactories = parseExceptionHandlerFactories(facesConfigXml, xpath);
        }
        catch (Exception e) {
            throw new IllegalStateException(ERROR_INITIALIZATION_FAIL, e);
        }
    }

    // Getters --------------------------------------------------------------------------------------------------------

    @Override
    public Map<String, String> getResourceBundles() {
        return resourceBundles;
    }

    @Override
    public List<Locale> getSupportedLocales() {
        return supportedLocales;
    }

    @Override
    public List<Class<? extends ResourceHandler>> getResourceHandlers() {
        return resourceHandlers;
    }

    @Override
    public List<Class<? extends ExceptionHandlerFactory>> getExceptionHandlerFactories() {
        return exceptionHandlerFactories;
    }

    // Helpers --------------------------------------------------------------------------------------------------------

    /**
     * Load, merge and return all <code>faces-config.xml</code> files found in the classpath
     * into a single {@link Document}.
     */
    private static Document loadFacesConfigXml(ServletContext context) throws IOException, SAXException {
        List<URL> facesConfigURLs = new ArrayList<>();
        facesConfigURLs.add(context.getResource(APP_FACES_CONFIG_XML));
        facesConfigURLs.addAll(Collections.list(Thread.currentThread().getContextClassLoader().getResources(LIB_FACES_CONFIG_XML)));
        return createDocument(facesConfigURLs);
    }

    /**
     * Create and return a mapping of all resource bundle base names by var found in the given document.
     * @throws XPathExpressionException
     */
    private static Map<String, String> parseResourceBundles(Element facesConfigXml, XPath xpath) throws XPathExpressionException {
        Map<String, String> resourceBundles = new LinkedHashMap<>();
        NodeList resourceBundleNodes = getNodeList(facesConfigXml, xpath, XPATH_RESOURCE_BUNDLE);

        for (int i = 0; i < resourceBundleNodes.getLength(); i++) {
            Node node = resourceBundleNodes.item(i);

            String var = xpath.compile(XPATH_VAR).evaluate(node).trim();
            String baseName = xpath.compile(XPATH_BASE_NAME).evaluate(node).trim();
            resourceBundles.computeIfAbsent(var, k -> baseName);
        }

        return Collections.unmodifiableMap(resourceBundles);
    }

    /**
     * Create and return a list of default locale and all supported locales in same order as in the given document.
     * @throws XPathExpressionException
     */
    private static List<Locale> parseSupportedLocales(Element facesConfigXml, XPath xpath) throws XPathExpressionException {
        List<Locale> supportedLocales = new ArrayList<>();
        String defaultLocale = xpath.compile(XPATH_DEFAULT_LOCALE).evaluate(facesConfigXml).trim();

        if (!isEmpty(defaultLocale)) {
            supportedLocales.add(parseLocale(defaultLocale));
        }

        NodeList supportedLocaleNodes = getNodeList(facesConfigXml, xpath, XPATH_SUPPORTED_LOCALE);

        for (int i = 0; i < supportedLocaleNodes.getLength(); i++) {
            Locale supportedLocale = parseLocale(getTextContent(supportedLocaleNodes.item(i)));

            if (!supportedLocales.contains(supportedLocale)) {
                supportedLocales.add(supportedLocale);
            }
        }

        return Collections.unmodifiableList(supportedLocales);
    }

    /**
     * Create and return a list of all resource handlers in same order as in the given document.
     * @throws XPathExpressionException
     */
    private static List<Class<? extends ResourceHandler>> parseResourceHandlers(Element facesConfigXml, XPath xpath) throws XPathExpressionException {
        List<Class<? extends ResourceHandler>> resourceHandlers = new ArrayList<>();
        NodeList resourceHandlerNodes = getNodeList(facesConfigXml, xpath, XPATH_RESOURCE_HANDLER);

        for (int i = 0; i < resourceHandlerNodes.getLength(); i++) {
            Class<? extends ResourceHandler> resourceHandler = toClass(getTextContent(resourceHandlerNodes.item(i)));
            resourceHandlers.add(resourceHandler);
        }

        return Collections.unmodifiableList(resourceHandlers);
    }

    /**
     * Create and return a list of all exception handler factories in same order as in the given document.
     * @throws XPathExpressionException
     */
    private static List<Class<? extends ExceptionHandlerFactory>> parseExceptionHandlerFactories(Element facesConfigXml, XPath xpath) throws XPathExpressionException {
        List<Class<? extends ExceptionHandlerFactory>> exceptionHandlerFactories = new ArrayList<>();
        NodeList exceptionHandlerFactoryNodes = getNodeList(facesConfigXml, xpath, XPATH_EXCEPTION_HANDLER_FACTORY);

        for (int i = 0; i < exceptionHandlerFactoryNodes.getLength(); i++) {
            Class<? extends ExceptionHandlerFactory> exceptionHandlerFactory = toClass(getTextContent(exceptionHandlerFactoryNodes.item(i)));
            exceptionHandlerFactories.add(exceptionHandlerFactory);
        }

        return Collections.unmodifiableList(exceptionHandlerFactories);
    }

}