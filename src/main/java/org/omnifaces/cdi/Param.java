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
package org.omnifaces.cdi;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.beans.PropertyEditor;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.util.Nonbinding;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.validator.BeanValidator;
import jakarta.faces.validator.RequiredValidator;
import jakarta.faces.validator.Validator;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;

import org.omnifaces.cdi.param.Attribute;
import org.omnifaces.cdi.param.DynamicParamValueProducer;
import org.omnifaces.cdi.param.ParamExtension;
import org.omnifaces.cdi.param.ParamProducer;
import org.omnifaces.cdi.param.ParamValue;
import org.omnifaces.util.Utils;

/**
 * <p>
 * The CDI annotation <code>&#64;</code>{@link Param} allows you to inject, convert and validate a HTTP request or path
 * parameter in a CDI managed bean.
 * <p>
 * For HTTP request parameters it's basically like <code>&lt;f:viewParam&gt;</code>, but with the major difference that
 * the injected parameter is directly available during {@link PostConstruct}, allowing a much easier way of processing
 * without the need for a <code>&lt;f:event type="preRenderView"&gt;</code> or <code>&lt;f:viewAction&gt;</code> in the
 * view.
 *
 * <h2>Usage</h2>
 *
 * <h3>Request parameters</h3>
 * <p>
 * The example below injects the request parameter with name <code>foo</code>.
 * <pre>
 * &#64;Param
 * private String foo;
 * </pre>
 * <p>
 * By default the name of the request parameter is taken from the name of the variable into which injection takes place.
 * The name can be optionally specified via the <code>name</code> attribute. The example below injects the request
 * parameter with name <code>foo</code> into a variable named <code>bar</code>.
 * <pre>
 * &#64;Param(name="foo")
 * private String bar;
 * </pre>
 * <p>
 * The <code>name</code> attribute is only mandatory when using constructor injection in OmniFaces 3.5 or older as there
 * is no information about constructor parameter names. The example below injects the request parameter with name
 * <code>foo</code> as a constructor parameter.
 * <pre>
 * &#64;Inject
 * public Bean(&#64;Param(name="foo") String foo) {
 *     // ...
 * }
 * </pre>
 * <p>
 * Since OmniFaces 3.6 it is not necessary anymore if the parameter has a name according to the class file as per
 * <a href="https://docs.oracle.com/javase/8/docs/api/java/lang/reflect/Parameter.html#isNamePresent--"><code>Parameter#isNamePresent()</code></a>.
 * <pre>
 * &#64;Inject
 * public Bean(&#64;Param String foo) {
 *     // ...
 * }
 * </pre>
 *
 * <h3>Multi-valued request parameters</h3>
 * <p>
 * Multi-valued parameters are also supported by specifying a {@link List} or array type. The support was added in
 * OmniFaces 2.4.
 * <pre>
 * &#64;Param(name="foo")
 * private List&lt;String&gt; foos;
 *
 * &#64;Param(name="bar")
 * private String[] bars;
 * </pre>
 *
 * <h3>Path parameters</h3>
 * <p>
 * Path parameters can be injected by specifying the <code>pathIndex</code> attribute representing the zero-based index
 * of the path parameter. The support was added in OmniFaces 2.5. On an example request
 * <code>https://example.com/mypage/firstname.lastname</code>, which is mapped to <code>/mypage.xhtml</code>, the below
 * example injects the path parameter <code>firstname.lastname</code>.
 * <pre>
 * &#64;Param(pathIndex=0)
 * private String user;
 * </pre>
 * <p>
 * This takes precedence over the <code>name</code> attribute.
 *
 * <h2>Conversion and validation</h2>
 * <p>
 * Standard types for which Faces already has a build in converter like {@link String}, {@link Long}, {@link Boolean}, etc
 * or for which there's already a converter registered via <code>forClass</code>, can be injected without explicitly
 * specifying a converter.
 * <pre>
 * &#64;Param
 * private Long id;
 * </pre>
 * <p>
 * Other types do need a converter. The following is an example of the injection of request parameter <code>user</code>
 * following a request such as <code>https://example.com/mypage?user=42</code>:
 * <pre>
 * &#64;Param(converter="userConverter", validator="priviledgedUser")
 * private User user;
 * </pre>
 * <p>
 * This also works on multi-valued parameters.
 * <pre>
 * &#64;Param(name="user", converter="userConverter")
 * private List&lt;User&gt; users;
 * </pre>
 * <p>
 * This also works on path parameters. The following is an example of the injection of path parameter <code>user</code>
 * following a request such as <code>https://example.com/mypage/42</code>:
 * <pre>
 * &#64;Param(pathIndex=0, converter="userConverter", validator="priviledgedUser")
 * private User user;
 * </pre>
 * <p>
 * Note that the <code>converter</code> and <code>validator</code> attributes can be specified in 3 ways:
 * <ul>
 * <li>A string value representing the converter/validator ID like so <code>converter="userConverter"</code>.
 * <li>An EL expression returning the converter/validator ID string like so <code>converter="#{bean.converterId}"</code>.
 * <li>An EL expression returning the concrete converter/validator instance like so <code>converter="#{converterBean}"</code>.
 * </ul>
 * <p>
 * Instead of <code>converter</code> or <code>validator</code> you can also use <code>converterClass</code> or
 * <code>validatorClass</code>:
 * <pre>
 * &#64;Param(converterClass=UserConverter.class, validatorClass=PriviledgedUser.class)
 * private User user;
 * </pre>
 * <p>
 * Note that this is <strong>ignored</strong> when <code>converter</code> or <code>validator</code> is also specified.
 * <p>
 * In case you want to specify converter or validator attributes, then you can use <code>converterAttributes</code> or
 * <code>validatorAttributes</code> respectively. They accept an array of {@link Attribute} arguments whose value
 * can be a string literal or an EL expression such as <code>value = "#{bean.property}"</code>.
 * <pre>
 * &#64;Param(
 *     converterClass = DateTimeConverter.class,
 *     converterAttributes = { &#64;Attribute(name = "pattern", value = "yyyyMMdd") },
 *     converterMessage = "{1}: \"{0}\" is not the date format we had in mind! Please use the format yyyyMMdd.")
 * private Date date;
 * </pre>
 * <p>
 * Yes, you can use <code>converterMessage</code> and <code>validatorMessage</code> to customize the error message.
 * <p>
 * In case the converted parameter value is not serializable, while the managed bean is serializable, you could inject
 * it into a field of type {@link ParamValue}, with <code>V</code> the actual type of the converted parameter.
 * Deserialization in this case works by converting from the original parameter again.
 * <pre>
 * &#64;Inject &#64;Param(converter="someIdToInputStreamConverter")
 * private ParamValue&lt;InputStream&gt; content; // Extreme example :) Be careful with resource leaking.
 * </pre>
 * <p>
 * If conversion or validation fails, <code>null</code> is injected if the injection target is <b>NOT</b>
 * {@link ParamValue}. Otherwise a {@link ParamValue} instance is injected, but it will contain a <code>null</code>
 * value. In both cases, the conversion and validation messages (if any) will be set in the Faces context then, and
 * {@link FacesContext#isValidationFailed()} will return <code>true</code>.
 *
 * <h2>Faces Messages</h2>
 * <p>
 * By default, faces messages are attached to the client ID of the current {@link UIViewRoot}, so you can use the 
 * following {@code for} attribute to separate faces messages coming from {@code @Param} from the rest:
 * <pre>
 * &lt;h:messages for="#{view.id}" /&gt;
 * </pre>
 * <p>
 * In case you wish to make it a global message, then you can since OmniFaces 4.5 set the {@code globalMessage}
 * attribute to {@code true}:
 * <pre>
 * &#64;Param(globalMessage=true)
 * </pre>
 * So that these can only be displayed via below tag:
 * <pre>
 * &lt;h:messages globalOnly="true" /&gt;
 * </pre>
 * 
 * <h2>Historical note</h2>
 * <p>
 * Before OmniFaces 3.6, the <code>&#64;</code>{@link Param} which is not of type {@link ParamValue} also required
 * <code>&#64;</code>{@link Inject} as in:
 * <pre>
 * &#64;Inject &#64;Param
 * private String foo;
 * </pre>
 * <p>
 * But this is not needed anymore since OmniFaces 3.6. This has the following advantages:
 * <ul>
 * <li>Less code
 * <li>Not anymore confusing <em>"No bean is eligible for injection to the injection point [JSR-365 §5.2.2]"</em>
 * warnings in IDEs like Eclipse (caused by the dynamic/generic type of the injection point).
 * </ul>
 * These will not anymore use the {@link DynamicParamValueProducer}. Instead the injection is "manually" done while
 * creating the bean.
 *
 * @since 1.6
 * @author Arjan Tijms
 * @author Bauke Scholtz
 * @see ParamValue
 * @see Attribute
 * @see ParamExtension
 * @see ParamProducer
 * @see DynamicParamValueProducer
 *
 */
@Qualifier
@Retention(RUNTIME)
@Target({ METHOD, FIELD, PARAMETER })
@SuppressWarnings("rawtypes")
public @interface Param {

    /**
     * (Optional) The name of the request parameter. If not specified the name of the injection target field will be used.
     *
     * @return The name of the request parameter.
     */
    @Nonbinding String name() default "";

    /**
     * (Optional) The index of the path parameter. If specified the parameter will be extracted from the request path
     * info on the given index instead of as request parameter. The first path parameter has an index of <code>0</code>.
     * This takes precedence over <code>name</code> attribute.
     *
     * @return The index of the path parameter.
     * @since 2.5
     */
    @Nonbinding int pathIndex() default -1;

    /**
     * (Optional) the label used to refer to the parameter.
     *
     * @return The label used to refer the parameter, defaults to the <code>name</code> attribute.
     */
    @Nonbinding String label() default "";

    /**
     * (Optional/Required) The converter to be used for converting the parameter to the type that is to be injected.
     * Optional if the target type is String, otherwise required.
     * <p>
     * A converter can be specified in 3 ways:
     * <ol>
     * <li>A string value representing the <em>converter-id</em> as used by {@link
     * jakarta.faces.application.Application#createConverter(String)}
     * <li>An EL expression that resolves to a String representing the <em>converter-id</em>
     * <li>An EL expression that resolves to a {@link Converter} instance.
     * </ol>
     * <p>
     * If this attribute is specified in addition to {@link Param#converterClass()}, this attribute takes precedence.
     *
     * @return The converter used to convert the parameter to model value.
     */
    @Nonbinding String converter() default "";

    /**
     * (Optional) Flag indicating if this parameter is required (must be present) or not. The required check is done
     * after conversion and before validation. A value is said to be not present if it turns out to be empty according to
     * the semantics of {@link Utils#isEmpty(Object)}.
     *
     * @return Whether the absence of the parameter should cause a validation error.
     */
    @Nonbinding boolean required() default false;

    /**
     * (Optional) The validators to be used for validating the (converted) parameter.
     *
     * <p>
     * A validator can be specified in 3 ways:
     * <ol>
     * <li>A string value representing the <em>validator-id</em> as used by {@link
     * jakarta.faces.application.Application#createValidator(String)}
     * <li>An EL expression that resolves to a String representing the <em>validator-id</em>
     * <li>An EL expression that resolves to a {@link Validator} instance.
     * </ol>
     * <p>
     * If this attribute is specified in addition to {@link Param#validatorClasses()} then the validators from both
     * attributes will be added to the final collection of validators. The validators from this attribute will however
     * be called first.
     *
     * @return The validators used to validate the (converted) parameter.
     */
    @Nonbinding String[] validators() default {};

    /**
     * (Optional) Class of the converter to be used for converting the parameter to the type that is to be injected.
     * This is ignored when {@link #converter()} is specified.
     *
     * @return The converter class used to convert the parameter to model value.
     */
    @Nonbinding Class<? extends Converter> converterClass() default Converter.class;

    /**
     * (Optional) Class of one ore more validators to be used for validating the (converted) parameter.
     * These will run <i>after</i> the ones specified in {@link #validators()}.
     *
     * @return The validator classes used to validate the (converted) parameter.
     */
    @Nonbinding Class<? extends Validator>[] validatorClasses() default {};

    /**
     * (Optional) Attributes that will be set on the converter instance obtained from {@link Param#converter()} or {@link Param#converterClass()}.
     * <p>
     * For each attribute the converter instance should have a writable JavaBeans property with the same name. The value can be a string literal
     * or an EL expression. String literals are coerced if necessary if there's a {@link PropertyEditor} available (the JDK provides these for
     * the primitive types and their corresponding boxed types).
     * <p>
     * Attributes for which the converter doesn't have a property (setter) are silently ignored.
     *
     * @return The attributes which need to be set on the converter.
     */
    @Nonbinding Attribute[] converterAttributes() default {};

    /**
     * (Optional) Attributes that will be set on each of the validator instances obtained from {@link Param#validators()} and {@link Param#validatorClasses()}.
     * <p>
     * For each attribute the validator instances should have a writable JavaBeans property with the same name. The value can be a string literal
     * or an EL expression. String literals are coerced if necessary if there's a {@link PropertyEditor} available (the JDK provides these for
     * the primitive types and their corresponding boxed types).
     * <p>
     * Attributes for which any given validator doesn't have a property (setter) are silently ignored.
     *
     * @return The attributes which need to be set on the validators.
     */
    @Nonbinding Attribute[] validatorAttributes() default {};

    /**
     * (Optional) A message that will be used if conversion fails instead of the message set by the converter.
     * <p>
     * The value for which conversion failed is available as <code>{0}</code>. The label associated with this
     * parameter value (see the {@link Param#label()} attribute) is available as <code>{1}</code>.
     *
     * @return The error message to be used when the {@link #converter()} or {@link #converterClass()} fail.
     */
    @Nonbinding String converterMessage() default "";

    /**
     * (Optional) A message that will be used if validation fails instead of the message set by the validator(s).
     * <p>
     * The value for which validation failed is available as <code>{0}</code>. The label associated with this
     * parameter value (see the {@link Param#label()} attribute) is available as <code>{1}</code>.
     *
     * @return The error message to be used when any of the {@link #validators()} or {@link #validatorClasses()} fail.
     */
    @Nonbinding String validatorMessage() default "";

    /**
     * (Optional) A message that will be used if a non-empty value is submitted instead of the default message associated
     * with the {@link RequiredValidator}.
     * <p>
     * The (empty) value for which the required check failed is available as <code>{0}</code>. (this will be either null or the empty string)
     * The label associated with this parameter value (see the {@link Param#label()} attribute) is available as <code>{1}</code>.
     *
     * @return The error message to be used on empty submit while {@link #required()} is <code>true</code>.
     */
    @Nonbinding String requiredMessage() default "";

    /**
     * (Optional) Whether to add the faces message as a global message instead of a message for the current {@link UIViewRoot}.
     * @return Whether to add the faces message as a global message instead of a message for the current {@link UIViewRoot}.
     * @since 4.5
     */
    @Nonbinding boolean globalMessage() default false;

    /**
     * (Optional) Flag that disables bean validation for this instance.
     * <p>
     * <b>NOTE:</b> bean validation at the moment (OmniFaces 1.6) is done against the {@link ParamValue} that is injected. In many cases this will
     * be of limited use. We hope to directly inject the converted type in OmniFaces 1.7 and then bean validation will make more sense.
     * <p>
     * If <code>true</code> no bean validation will be attempted. If <code>false</code> (the default) no specific action is taken, and it
     * will depend on the availability of bean validation and the global {@link BeanValidator#DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME} setting
     * whether bean validation is attempted or not.
     *
     * @return Whether to disable bean validation or not.
     */
    @Nonbinding boolean disableBeanValidation() default false;

    /**
     * (Optional) Flag that overrides the global {@link BeanValidator#DISABLE_DEFAULT_BEAN_VALIDATOR_PARAM_NAME} setting.
     * <p>
     * If <code>true</code> bean validation will be performed for this instance (given that bean validation is available) despite
     * it globally being disabled. If <code>false</code> (the default) no specific action is taken.
     *
     * @return Whether to override that Faces bean validation is globally disabled.
     */
    @Nonbinding boolean overrideGlobalBeanValidationDisabled() default false;

}