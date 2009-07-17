/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.blueprint.container;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.geronimo.blueprint.ComponentDefinitionRegistry;
import org.apache.geronimo.blueprint.NamespaceHandler;
import org.apache.geronimo.blueprint.reflect.BeanArgumentImpl;
import org.apache.geronimo.blueprint.reflect.BeanMetadataImpl;
import org.apache.geronimo.blueprint.reflect.BeanPropertyImpl;
import org.apache.geronimo.blueprint.reflect.CollectionMetadataImpl;
import org.apache.geronimo.blueprint.reflect.IdRefMetadataImpl;
import org.apache.geronimo.blueprint.reflect.MapEntryImpl;
import org.apache.geronimo.blueprint.reflect.MapMetadataImpl;
import org.apache.geronimo.blueprint.reflect.MetadataUtil;
import org.apache.geronimo.blueprint.reflect.PropsMetadataImpl;
import org.apache.geronimo.blueprint.reflect.RefMetadataImpl;
import org.apache.geronimo.blueprint.reflect.ReferenceListMetadataImpl;
import org.apache.geronimo.blueprint.reflect.ReferenceListenerImpl;
import org.apache.geronimo.blueprint.reflect.ReferenceMetadataImpl;
import org.apache.geronimo.blueprint.reflect.RegistrationListenerImpl;
import org.apache.geronimo.blueprint.reflect.ServiceMetadataImpl;
import org.apache.geronimo.blueprint.reflect.ServiceReferenceMetadataImpl;
import org.apache.geronimo.blueprint.reflect.ValueMetadataImpl;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.MapEntry;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NonNullMetadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.PropsMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListMetadata;
import org.osgi.service.blueprint.reflect.ReferenceListener;
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ServiceReferenceMetadata;
import org.osgi.service.blueprint.reflect.Target;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:dev@geronimo.apache.org">Apache Geronimo Project</a>
 * @version $Rev: 760378 $, $Date: 2009-03-31 11:31:38 +0200 (Tue, 31 Mar 2009) $
 */
public class Parser {

    public static final String BLUEPRINT_NAMESPACE = "http://www.osgi.org/xmlns/blueprint/v1.0.0";

    public static final String BLUEPRINT_ELEMENT = "blueprint";
    public static final String DESCRIPTION_ELEMENT = "description";
    public static final String TYPE_CONVERTERS_ELEMENT = "type-converters";
    public static final String BEAN_ELEMENT = "bean";
    public static final String ARGUMENT_ELEMENT = "argument";
    public static final String REF_ELEMENT = "ref";
    public static final String IDREF_ELEMENT = "idref";
    public static final String LIST_ELEMENT = "list";
    public static final String SET_ELEMENT = "set";
    public static final String MAP_ELEMENT = "map";
    public static final String ARRAY_ELEMENT = "array";
    public static final String PROPS_ELEMENT = "props";
    public static final String PROP_ELEMENT = "prop";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String NULL_ELEMENT = "null";
    public static final String VALUE_ELEMENT = "value";
    public static final String SERVICE_ELEMENT = "service";
    public static final String REFERENCE_ELEMENT = "reference";
    public static final String REFERENCE_LIST_ELEMENT = "reference-list";
    public static final String INTERFACES_ELEMENT = "interfaces";
    public static final String REFERENCE_LISTENER_ELEMENT = "reference-listener";
    public static final String SERVICE_PROPERTIES_ELEMENT = "service-properties";
    public static final String REGISTRATION_LISTENER_ELEMENT = "registration-listener";
    public static final String ENTRY_ELEMENT = "entry";
    public static final String KEY_ELEMENT = "key";
    public static final String DEFAULT_ACTIVATION_ATTRIBUTE = "default-activation";
    public static final String DEFAULT_TIMEOUT_ATTRIBUTE = "default-timeout";
    public static final String DEFAULT_AVAILABILITY_ATTRIBUTE = "default-availability";
    public static final String NAME_ATTRIBUTE = "name";
    public static final String ID_ATTRIBUTE = "id";
    public static final String CLASS_ATTRIBUTE = "class";
    public static final String INDEX_ATTRIBUTE = "index";
    public static final String TYPE_ATTRIBUTE = "type";
    public static final String VALUE_ATTRIBUTE = "value";
    public static final String VALUE_REF_ATTRIBUTE = "value-ref";
    public static final String KEY_ATTRIBUTE = "key";
    public static final String KEY_REF_ATTRIBUTE = "key-ref";
    public static final String REF_ATTRIBUTE = "ref";
    public static final String COMPONENT_ID_ATTRIBUTE = "component-id";
    public static final String INTERFACE_ATTRIBUTE = "interface";
    public static final String DEPENDS_ON_ATTRIBUTE = "depends-on";
    public static final String AUTO_EXPORT_ATTRIBUTE = "auto-export";
    public static final String RANKING_ATTRIBUTE = "ranking";
    public static final String TIMEOUT_ATTRIBUTE = "timeout";
    public static final String FILTER_ATTRIBUTE = "filter";
    public static final String COMPONENT_NAME_ATTRIBUTE = "component-name";
    public static final String AVAILABILITY_ATTRIBUTE = "availability";
    public static final String REGISTRATION_METHOD_ATTRIBUTE = "registration-method";
    public static final String UNREGISTRATION_METHOD_ATTRIBUTE = "unregistration-method";
    public static final String BIND_METHOD_ATTRIBUTE = "bind-method";
    public static final String UNBIND_METHOD_ATTRIBUTE = "unbind-method";
    public static final String KEY_TYPE_ATTRIBUTE = "key-type";
    public static final String VALUE_TYPE_ATTRIBUTE = "value-type";
    public static final String MEMBER_TYPE_ATTRIBUTE = "member-type";
    public static final String SCOPE_ATTRIBUTE = "scope";
    public static final String INIT_METHOD_ATTRIBUTE = "init-method";
    public static final String DESTROY_METHOD_ATTRIBUTE = "destroy-method";
    public static final String ACTIVATION_ATTRIBUTE = "activation";
    public static final String FACTORY_REF_ATTRIBUTE = "factory-ref";
    public static final String FACTORY_METHOD_ATTRIBUTE = "factory-method";

    public static final String AUTO_EXPORT_DISABLED = "disabled";
    public static final String AUTO_EXPORT_INTERFACES = "interfaces";
    public static final String AUTO_EXPORT_CLASS_HIERARCHY = "class-hierarchy";
    public static final String AUTO_EXPORT_ALL = "all-classes";
    public static final String AUTO_EXPORT_DEFAULT = AUTO_EXPORT_DISABLED;
    public static final String RANKING_DEFAULT = "0";
    public static final String AVAILABILITY_MANDATORY = "mandatory";
    public static final String AVAILABILITY_OPTIONAL = "optional";
    public static final String AVAILABILITY_DEFAULT = AVAILABILITY_MANDATORY;
    public static final String TIMEOUT_DEFAULT = "300000";
    public static final String USE_SERVICE_OBJECT = "service-object";
    public static final String USE_SERVICE_REFERENCE = "service-reference";
    public static final String ACTIVATION_EAGER = "eager";
    public static final String ACTIVATION_LAZY = "lazy";
    public static final String ACTIVATION_DEFAULT = ACTIVATION_EAGER;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

    private static DocumentBuilderFactory documentBuilderFactory;

    private List<Document> documents;
    private ComponentDefinitionRegistry registry;
    private NamespaceHandlerRegistry namespaceHandlerRegistry;
    private String idPrefix = "component-";
    private Set<String> ids = new HashSet<String>();
    private int idCounter;
    private String defaultTimeout;
    private String defaultAvailability;
    private String defaultActivation;
    private Set<URI> namespaces;
    private boolean validation;
    private boolean validated;

    public Parser() {
    }

    public Parser(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public void setValidation(boolean validation) {
        this.validation = validation;
    }

    public void parse(List<URL> urls) throws Exception {
        List<Document> documents = new ArrayList<Document>();
        // Create document builder factory
        // Load documents
        for (URL url : urls) {
            InputStream inputStream = url.openStream();
            try {
                InputSource inputSource = new InputSource(inputStream);
                DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
                Document doc = builder.parse(inputSource);
                documents.add(doc);
            } finally {
                inputStream.close();
            }
        }
        this.documents = documents;
    }

    public Set<URI> getNamespaces() {
        if (this.namespaces == null) {
            if (documents == null) {
                throw new IllegalStateException("Documents should be parsed before retrieving required namespaces");
            }
            Set<URI> namespaces = new LinkedHashSet<URI>();
            for (Document doc : documents) {
                findNamespaces(namespaces, doc);
            }
            this.namespaces = namespaces;
        }
        return this.namespaces;
    }

    private void findNamespaces(Set<URI> namespaces, Node node) {
        if (node instanceof Element || node instanceof Attr) {
            String ns = node.getNamespaceURI();
            if (ns != null && !isBlueprintNamespace(ns)) {
                namespaces.add(URI.create(ns));
            }
        }
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            findNamespaces(namespaces, nl.item(i));
        }
    }

    public void populate(NamespaceHandlerRegistry handlers,
                         ComponentDefinitionRegistry registry) {
        this.namespaceHandlerRegistry = handlers;
        this.registry = registry;
        if (this.documents == null) {
            throw new IllegalStateException("Documents should be parsed before populating the registry");
        }
        // Validate xmls
        if (!this.validated && validation) {
            validate();
            this.validated = true;
        }
        // Parse components
        for (Document doc : this.documents) {
            loadComponents(doc);
        }
    }

    private void validate() {
        // Use a LinkedHashSet to ensure that the blueprint schema is loaded first
        try {
            Schema schema = this.namespaceHandlerRegistry.getSchema(getNamespaces());
            Validator validator = schema.newValidator();
            for (Document doc : this.documents) {
                validator.validate(new DOMSource(doc));
            }
        } catch (Exception e) {
            throw new ComponentDefinitionException("Unable to validate xml", e);
        }
    }

    private void loadComponents(Document doc) {
        defaultTimeout = TIMEOUT_DEFAULT;
        defaultAvailability = AVAILABILITY_DEFAULT;
        defaultActivation = ACTIVATION_DEFAULT;
        Element root = doc.getDocumentElement();
        if (!isBlueprintNamespace(root.getNamespaceURI()) ||
                !nodeNameEquals(root, BLUEPRINT_ELEMENT)) {
            throw new ComponentDefinitionException("Root element must be {" + BLUEPRINT_NAMESPACE + "}" + BLUEPRINT_ELEMENT + " element");
        }
        // Parse global attributes
        if (root.hasAttribute(DEFAULT_ACTIVATION_ATTRIBUTE)) {
            defaultActivation = root.getAttribute(DEFAULT_ACTIVATION_ATTRIBUTE);
        }
        if (root.hasAttribute(DEFAULT_TIMEOUT_ATTRIBUTE)) {
            defaultTimeout = root.getAttribute(DEFAULT_TIMEOUT_ATTRIBUTE);
        }
        if (root.hasAttribute(DEFAULT_AVAILABILITY_ATTRIBUTE)) {
            defaultAvailability = root.getAttribute(DEFAULT_AVAILABILITY_ATTRIBUTE);
        }
        
        // Parse custom attributes
        NamedNodeMap attributes = root.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node node = attributes.item(i);
                if (node instanceof Attr
                        && node.getNamespaceURI() != null
                        && !isBlueprintNamespace(node.getNamespaceURI())
                        && !XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(node.getNamespaceURI())
                        && XMLConstants.XMLNS_ATTRIBUTE.equals(node.getNodeName())) {
                    decorateCustomNode(node, null);
                }
            }
        }

        // Parse elements
        NodeList nl = root.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                String namespaceUri = element.getNamespaceURI();
                if (isBlueprintNamespace(namespaceUri)) {
                    parseBlueprintElement(element);
                } else {
                    Metadata component = parseCustomElement(element, null);
                    if (component != null) {
                        if (!(component instanceof ComponentMetadata)) {
                            throw new ComponentDefinitionException("Expected a ComponentMetadata to be returned when parsing element " + element.getNodeName());
                        }
                        registry.registerComponentDefinition((ComponentMetadata) component);
                    }
                }
            }
        }
    }

    public <T> T parseElement(Class<T> type, ComponentMetadata enclosingComponent, Element element) {
        if (BeanArgument.class.isAssignableFrom(type)) {
            return type.cast(parseBeanArgument(enclosingComponent, element));
        } else if (BeanProperty.class.isAssignableFrom(type)) {
            return type.cast(parseBeanProperty(enclosingComponent, element));
        } else if (MapEntry.class.isAssignableFrom(type)) {
            return type.cast(parseMapEntry(element, enclosingComponent));
        } else if (MapMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseMap(element, enclosingComponent));
        } else if (BeanMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseBeanMetadata(element, false));
        } else if (NullMetadata.class.isAssignableFrom(type)) {
            return type.cast(NullMetadata.NULL);
        } else if (CollectionMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseCollection(Collection.class, element, enclosingComponent));
        } else if (PropsMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseProps(element));
        } else if (ReferenceMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseReference(element, enclosingComponent == null));
        } else if (ReferenceListMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseRefList(element, enclosingComponent == null));
        } else if (IdRefMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseIdRef(element));
        } else if (RefMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseRef(element));
        } else if (ValueMetadata.class.isAssignableFrom(type)) {
            return type.cast(parseValue(element));
        } else {
            throw new ComponentDefinitionException("Unknown type to parse element: " + type.getName());
        }
    }

    private void parseBlueprintElement(Element element) {
        if (nodeNameEquals(element, DESCRIPTION_ELEMENT)) {
            // Ignore description
        } else if (nodeNameEquals(element, TYPE_CONVERTERS_ELEMENT)) {
            parseTypeConverters(element);
        } else if (nodeNameEquals(element, BEAN_ELEMENT)) {
            ComponentMetadata component = parseBeanMetadata(element, true);
            registry.registerComponentDefinition(component);
        } else if (nodeNameEquals(element, SERVICE_ELEMENT)) {
            ComponentMetadata service = parseService(element, true);
            registry.registerComponentDefinition(service);
        } else if (nodeNameEquals(element, REFERENCE_ELEMENT)) {
            ComponentMetadata reference = parseReference(element, true);
            registry.registerComponentDefinition(reference);
        } else if (nodeNameEquals(element, REFERENCE_LIST_ELEMENT) ) {
            ComponentMetadata references = parseRefList(element, true);
            registry.registerComponentDefinition(references);
        } else {
            throw new ComponentDefinitionException("Unknown element " + element.getNodeName() + " in namespace " + BLUEPRINT_NAMESPACE);
        }
    }

    private void parseTypeConverters(Element element) {
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                Object target = null;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, BEAN_ELEMENT)) {
                        target = parseBeanMetadata(e, true);
                    } else if (nodeNameEquals(e, REF_ELEMENT)) {
                        String componentName = e.getAttribute(COMPONENT_ID_ATTRIBUTE);
                        target = new RefMetadataImpl(componentName);
                    } else if (nodeNameEquals(e, REFERENCE_ELEMENT)) {
                        target = parseReference(e, true);
                    }
                } else {
                    target = parseCustomElement(e, null);
                }
                if (!(target instanceof Target)) {
                    throw new ComponentDefinitionException("Metadata parsed for element " + e.getNodeName() + " can not be used as a type converter");
                }
                registry.registerTypeConverter((Target) target);
            }
        }
    }

    private ComponentMetadata parseBeanMetadata(Element element, boolean topElement) {
        BeanMetadataImpl metadata = new BeanMetadataImpl();
        if (topElement) {
            metadata.setId(getId(element));
            if (element.hasAttribute(SCOPE_ATTRIBUTE)) {
                metadata.setScope(element.getAttribute(SCOPE_ATTRIBUTE));
                if (metadata.getScope().equals(BeanMetadata.SCOPE_PROTOTYPE)) {
                    if (element.hasAttribute(ACTIVATION_ATTRIBUTE)) {
                        if (element.getAttribute(ACTIVATION_ATTRIBUTE).equals(ACTIVATION_EAGER)) {
                            throw new ComponentDefinitionException("A <bean> with a prototype scope can not have an eager activation");
                        }
                    }
                    metadata.setActivation(ComponentMetadata.ACTIVATION_LAZY);
                } else {
                    metadata.setActivation(parseActivation(element));
                }
            } else {
                metadata.setActivation(parseActivation(element));
            }
        } else {
            metadata.setActivation(ComponentMetadata.ACTIVATION_LAZY);
        }
        if (element.hasAttribute(CLASS_ATTRIBUTE)) {
            metadata.setClassName(element.getAttribute(CLASS_ATTRIBUTE));
        }
        if (element.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            metadata.setDependsOn(parseList(element.getAttribute(DEPENDS_ON_ATTRIBUTE)));
        }
        if (element.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
            metadata.setInitMethod(element.getAttribute(INIT_METHOD_ATTRIBUTE));
        }
        if (element.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
            metadata.setDestroyMethod(element.getAttribute(DESTROY_METHOD_ATTRIBUTE));
        }
        if (element.hasAttribute(FACTORY_REF_ATTRIBUTE)) {
            metadata.setFactoryComponent(new RefMetadataImpl(element.getAttribute(FACTORY_REF_ATTRIBUTE)));
        }
        if (element.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) {
            String factoryMethod = element.getAttribute(FACTORY_METHOD_ATTRIBUTE);
            metadata.setFactoryMethod(factoryMethod);
        }

        // Do some validation
        if (metadata.getClassName() == null && metadata.getFactoryComponent() == null) {
            throw new ComponentDefinitionException("Bean class or factory-ref must be specified");
        }
        if (metadata.getFactoryComponent() != null && metadata.getFactoryMethod() == null) {
            throw new ComponentDefinitionException("factory-method is required when factory-component is set");
        }
        if (MetadataUtil.isPrototypeScope(metadata) && metadata.getDestroyMethod() != null) {
            throw new ComponentDefinitionException("destroy-method must not be set for a <bean> with a prototype scope");
        }

        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(node.getNamespaceURI())) {                
                    if (nodeNameEquals(node, ARGUMENT_ELEMENT)) {
                        metadata.addArgument(parseBeanArgument(metadata, e));
                    } else if (nodeNameEquals(node, PROPERTY_ELEMENT)) {
                        metadata.addProperty(parseBeanProperty(metadata, e));
                    }
                }
            }
        }

        MetadataUtil.validateBeanArguments(metadata.getArguments());
        
        ComponentMetadata m = metadata;

        // Parse custom attributes
        m = handleCustomAttributes(element.getAttributes(), m);
        
        // Parse custom elements;
        m = handleCustomElements(element, m);
        
        return m;
    }

    public BeanProperty parseBeanProperty(ComponentMetadata enclosingComponent, Element element) {
        String name = element.hasAttribute(NAME_ATTRIBUTE) ? element.getAttribute(NAME_ATTRIBUTE) : null;
        Metadata value = parseArgumentOrPropertyValue(element, enclosingComponent);
        return new BeanPropertyImpl(name, value);
    }

    private BeanArgument parseBeanArgument(ComponentMetadata enclosingComponent, Element element) {
        int index = element.hasAttribute(INDEX_ATTRIBUTE) ? Integer.parseInt(element.getAttribute(INDEX_ATTRIBUTE)) : -1;
        String type = element.hasAttribute(TYPE_ATTRIBUTE) ? element.getAttribute(TYPE_ATTRIBUTE) : null;
        Metadata value = parseArgumentOrPropertyValue(element, enclosingComponent);
        return new BeanArgumentImpl(value, type, index);
    }

    private ComponentMetadata parseService(Element element, boolean topElement) {
        ServiceMetadataImpl service = new ServiceMetadataImpl();
        boolean hasInterfaceNameAttribute = false;
        if (topElement) {
            service.setId(getId(element));
            service.setActivation(parseActivation(element));
        } else {
            service.setActivation(ComponentMetadata.ACTIVATION_LAZY);
        }
        if (element.hasAttribute(INTERFACE_ATTRIBUTE)) {
            service.setInterfaceNames(Collections.singletonList(element.getAttribute(INTERFACE_ATTRIBUTE)));
            hasInterfaceNameAttribute = true;
        }
        if (element.hasAttribute(REF_ATTRIBUTE)) {
            service.setServiceComponent(new RefMetadataImpl(element.getAttribute(REF_ATTRIBUTE)));
        }
        if (element.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            service.setDependsOn(parseList(element.getAttribute(DEPENDS_ON_ATTRIBUTE)));
        }
        String autoExport = element.hasAttribute(AUTO_EXPORT_ATTRIBUTE) ? element.getAttribute(AUTO_EXPORT_ATTRIBUTE) : AUTO_EXPORT_DEFAULT;
        if (AUTO_EXPORT_DISABLED.equals(autoExport)) {
            service.setAutoExport(ServiceMetadata.AUTO_EXPORT_DISABLED);
        } else if (AUTO_EXPORT_INTERFACES.equals(autoExport)) {
            service.setAutoExport(ServiceMetadata.AUTO_EXPORT_INTERFACES);
        } else if (AUTO_EXPORT_CLASS_HIERARCHY.equals(autoExport)) {
            service.setAutoExport(ServiceMetadata.AUTO_EXPORT_CLASS_HIERARCHY);
        } else if (AUTO_EXPORT_ALL.equals(autoExport)) {
            service.setAutoExport(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
        } else {
            throw new ComponentDefinitionException("Illegal value (" + autoExport + ") for " + AUTO_EXPORT_ATTRIBUTE + " attribute");
        }
        String ranking = element.hasAttribute(RANKING_ATTRIBUTE) ? element.getAttribute(RANKING_ATTRIBUTE) : RANKING_DEFAULT;
        try {
            service.setRanking(Integer.parseInt(ranking));
        } catch (NumberFormatException e) {
            throw new ComponentDefinitionException("Attribute " + RANKING_ATTRIBUTE + " must be a valid integer (was: " + ranking + ")");
        }
        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, INTERFACES_ELEMENT)) {
                        if (hasInterfaceNameAttribute) {
                            throw new ComponentDefinitionException("Only one of " + INTERFACE_ATTRIBUTE + " attribute or " + INTERFACES_ELEMENT + " element must be used");
                        }
                        service.setInterfaceNames(parseInterfaceNames(e));
                    } else if (nodeNameEquals(e, SERVICE_PROPERTIES_ELEMENT)) {
                        List<MapEntry> entries = parseServiceProperties(e, service).getEntries();
                        service.setServiceProperties(entries); 
                    } else if (nodeNameEquals(e, REGISTRATION_LISTENER_ELEMENT)) {
                        service.addRegistrationListener(parseRegistrationListener(e, service));
                    } else if (nodeNameEquals(e, BEAN_ELEMENT)) {
                        if (service.getServiceComponent() != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + BEAN_ELEMENT + " element or " + REF_ELEMENT + " element can be set");
                        }
                        service.setServiceComponent((Target) parseBeanMetadata(e, false));
                    } else if (nodeNameEquals(e, REF_ELEMENT)) {
                        if (service.getServiceComponent() != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + BEAN_ELEMENT + " element or " + REF_ELEMENT + " element can be set");
                        }
                        String component = e.getAttribute(COMPONENT_ID_ATTRIBUTE);
                        if (component == null || component.length() == 0) {
                            throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ID_ATTRIBUTE + " attribute");
                        }
                        service.setServiceComponent(new RefMetadataImpl(component));
                    }
                }
            }
        }
        // Check service
        if (service.getServiceComponent() == null) {
            throw new ComponentDefinitionException("One of " + REF_ATTRIBUTE + " attribute, " + BEAN_ELEMENT + " element or " + REF_ELEMENT + " element must be set");
        }
        // Check interface
        if (service.getAutoExport() == ServiceMetadata.AUTO_EXPORT_DISABLED && service.getInterfaces().isEmpty()) {
            throw new ComponentDefinitionException(INTERFACE_ATTRIBUTE + " attribute or " + INTERFACES_ELEMENT + " element must be set when " + AUTO_EXPORT_ATTRIBUTE + " is set to " + AUTO_EXPORT_DISABLED);
        }
        // Check for non-disabled auto-exports and interfaces
        if (service.getAutoExport() != ServiceMetadata.AUTO_EXPORT_DISABLED && !service.getInterfaces().isEmpty()) {
            throw new ComponentDefinitionException(INTERFACE_ATTRIBUTE + " attribute or  " + INTERFACES_ELEMENT + " element must not be set when " + AUTO_EXPORT_ATTRIBUTE + " is set to anything else than " + AUTO_EXPORT_DISABLED);
        }
        ComponentMetadata s = service;
        
        // Parse custom attributes
        s = handleCustomAttributes(element.getAttributes(), s);

        // Parse custom elements;
        s = handleCustomElements(element, s);
        
        return s;
    }

    private CollectionMetadata parseArray(Element element, ComponentMetadata enclosingComponent) {
        return parseCollection(Object[].class, element, enclosingComponent);
    }

    private CollectionMetadata parseList(Element element, ComponentMetadata enclosingComponent) {
        return parseCollection(List.class, element, enclosingComponent);
    }

    private CollectionMetadata parseSet(Element element, ComponentMetadata enclosingComponent) {
        return parseCollection(Set.class, element, enclosingComponent);
    }

    private CollectionMetadata parseCollection(Class collectionType, Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String valueType = element.hasAttribute(VALUE_TYPE_ATTRIBUTE) ? element.getAttribute(VALUE_TYPE_ATTRIBUTE) : null;
        // Parse elements
        List<Metadata> list = new ArrayList<Metadata>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Metadata val = parseValueGroup((Element) node, enclosingComponent, true);
                list.add(val);
            }
        }
        return new CollectionMetadataImpl(collectionType, valueType, list);
    }

    public PropsMetadata parseProps(Element element) {
        // Parse elements
        List<MapEntry> entries = new ArrayList<MapEntry>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI()) && nodeNameEquals(e, PROP_ELEMENT)) {
                    entries.add(parseProperty(e));
                }
            }
        }
        return new PropsMetadataImpl(entries);
    }

    private MapEntry parseProperty(Element element) {
        // Parse attributes
        if (!element.hasAttribute(KEY_ATTRIBUTE)) {
            throw new ComponentDefinitionException(KEY_ATTRIBUTE + " attribute is required");
        }
        String value = null;
        if (element.hasAttribute(VALUE_ATTRIBUTE)) {
            value = element.getAttribute(VALUE_ATTRIBUTE);
        } else {
            value = getTextValue(element);
        }
        String key = element.getAttribute(KEY_ATTRIBUTE);
        return new MapEntryImpl(new ValueMetadataImpl(key), new ValueMetadataImpl(value));
    }

    public MapMetadata parseMap(Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String keyType = element.hasAttribute(KEY_TYPE_ATTRIBUTE) ? element.getAttribute(KEY_TYPE_ATTRIBUTE) : null;
        String valueType = element.hasAttribute(VALUE_TYPE_ATTRIBUTE) ? element.getAttribute(VALUE_TYPE_ATTRIBUTE) : null;
        // Parse elements
        List<MapEntry> entries = new ArrayList<MapEntry>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, ENTRY_ELEMENT)) {
                    entries.add(parseMapEntry(e, enclosingComponent));
                }
            }
        }
        return new MapMetadataImpl(keyType, valueType, entries);
    }

    private MapEntry parseMapEntry(Element element, ComponentMetadata enclosingComponent) {
        // Parse attributes
        String key = element.hasAttribute(KEY_ATTRIBUTE) ? element.getAttribute(KEY_ATTRIBUTE) : null;
        String keyRef = element.hasAttribute(KEY_REF_ATTRIBUTE) ? element.getAttribute(KEY_REF_ATTRIBUTE) : null;
        String value = element.hasAttribute(VALUE_ATTRIBUTE) ? element.getAttribute(VALUE_ATTRIBUTE) : null;
        String valueRef = element.hasAttribute(VALUE_REF_ATTRIBUTE) ? element.getAttribute(VALUE_REF_ATTRIBUTE) : null;
        // Parse elements
        NonNullMetadata keyValue = null;
        Metadata valValue = null;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, KEY_ELEMENT)) {
                    keyValue = parseMapKeyEntry(e, enclosingComponent);
                } else {
                    valValue = parseValueGroup(e, enclosingComponent, true);
                }
            }
        }
        // Check key
        if (keyValue != null && (key != null || keyRef != null) || (keyValue == null && key == null && keyRef == null)) {
            throw new ComponentDefinitionException("Only and only one of " + KEY_ATTRIBUTE + " attribute, " + KEY_REF_ATTRIBUTE + " attribute or " + KEY_ELEMENT + " element must be set");
        } else if (keyValue == null && key != null) {
            keyValue = new ValueMetadataImpl(key);
        } else if (keyValue == null /*&& keyRef != null*/) {
            keyValue = new RefMetadataImpl(keyRef);
        }
        // Check value
        if (valValue != null && (value != null || valueRef != null) || (valValue == null && value == null && valueRef == null)) {
            throw new ComponentDefinitionException("Only and only one of " + VALUE_ATTRIBUTE + " attribute, " + VALUE_REF_ATTRIBUTE + " attribute or sub element must be set");
        } else if (valValue == null && value != null) {
            valValue = new ValueMetadataImpl(value);
        } else if (valValue == null /*&& valueRef != null*/) {
            valValue = new RefMetadataImpl(valueRef);
        }
        return new MapEntryImpl(keyValue, valValue);
    }

    private NonNullMetadata parseMapKeyEntry(Element element, ComponentMetadata enclosingComponent) {
        NonNullMetadata keyValue = null;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (keyValue != null) {
                    // TODO: throw an exception
                }
                keyValue = (NonNullMetadata) parseValueGroup(e, enclosingComponent, false);
                break;
            }
        }
        if (keyValue == null) {
            // TODO: throw an exception
        }
        return keyValue;
    }
    
    public MapMetadata parseServiceProperties(Element element, ComponentMetadata enclosingComponent) {
        // TODO: need to handle this better
        MapMetadata map = parseMap(element, enclosingComponent);
        handleCustomElements(element, enclosingComponent);
        return map;
    }
    
    public RegistrationListener parseRegistrationListener(Element element, ComponentMetadata enclosingComponent) {
        RegistrationListenerImpl listener = new RegistrationListenerImpl();
        Metadata listenerComponent = null;
        // Parse attributes
        if (element.hasAttribute(REF_ATTRIBUTE)) {
            listenerComponent = new RefMetadataImpl(element.getAttribute(REF_ATTRIBUTE));
        }
        String registrationMethod = null;
        if (element.hasAttribute(REGISTRATION_METHOD_ATTRIBUTE)) {
            registrationMethod = element.getAttribute(REGISTRATION_METHOD_ATTRIBUTE);
            listener.setRegistrationMethod(registrationMethod);
        }
        String unregistrationMethod = null;
        if (element.hasAttribute(UNREGISTRATION_METHOD_ATTRIBUTE)) {
            unregistrationMethod = element.getAttribute(UNREGISTRATION_METHOD_ATTRIBUTE);
            listener.setUnregistrationMethod(unregistrationMethod);
        }
        if (registrationMethod == null && unregistrationMethod == null) {
            throw new ComponentDefinitionException("One of " + REGISTRATION_METHOD_ATTRIBUTE + " or " + UNREGISTRATION_METHOD_ATTRIBUTE + " must be set");
        }
        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, REF_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BEAN_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        String component = e.getAttribute(COMPONENT_ID_ATTRIBUTE);
                        if (component == null || component.length() == 0) {
                            throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ID_ATTRIBUTE + " attribute");
                        }
                        listenerComponent = new RefMetadataImpl(component);
                    } else if (nodeNameEquals(e, BEAN_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BEAN_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        listenerComponent = parseBeanMetadata(e, false);
                    } else if (nodeNameEquals(e, REFERENCE_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BEAN_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        listenerComponent = parseReference(e, false);
                    } else if (nodeNameEquals(e, SERVICE_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BEAN_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        listenerComponent = parseService(e, false);
                    }
                } else {
                    if (listenerComponent != null) {
                        throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BEAN_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                    }
                    listenerComponent = parseCustomElement(e, enclosingComponent);
                }
            }
        }
        if (listenerComponent == null) {
            throw new ComponentDefinitionException("One of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BEAN_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element must be set");
        }
        listener.setListenerComponent((Target) listenerComponent);
        return listener;
    }

    private ComponentMetadata parseReference(Element element, boolean topElement) {       
        ReferenceMetadataImpl reference = new ReferenceMetadataImpl();
        if (topElement) {
            reference.setId(getId(element));
        }
        parseReference(element, reference, topElement);
        String timeout = element.hasAttribute(TIMEOUT_ATTRIBUTE) ? element.getAttribute(TIMEOUT_ATTRIBUTE) : this.defaultTimeout;
        try {
            reference.setTimeout(Long.parseLong(timeout));
        } catch (NumberFormatException e) {
            throw new ComponentDefinitionException("Attribute " + TIMEOUT_ATTRIBUTE + " must be a valid long (was: " + timeout + ")");
        }
        
        ComponentMetadata r = reference;

        // Parse custom attributes
        r = handleCustomAttributes(element.getAttributes(), r);

        // Parse custom elements;
        r = handleCustomElements(element, r);
        
        return r;
    }

    private ComponentMetadata parseRefList(Element element, boolean topElement) {
        ReferenceListMetadataImpl references = new ReferenceListMetadataImpl();
        if (topElement) {
            references.setId(getId(element));
        }
        if (element.hasAttribute(MEMBER_TYPE_ATTRIBUTE)) {
            String memberType = element.getAttribute(MEMBER_TYPE_ATTRIBUTE);
            if (USE_SERVICE_OBJECT.equals(memberType)) {
                references.setMemberType(ReferenceListMetadata.USE_SERVICE_OBJECT);
            } else if (USE_SERVICE_REFERENCE.equals(memberType)) {
                references.setMemberType(ReferenceListMetadata.USE_SERVICE_REFERENCE);
            }
        } else {
            references.setMemberType(ReferenceListMetadata.USE_SERVICE_OBJECT);
        }
        parseReference(element, references, topElement);

        ComponentMetadata r = references;
        
        // Parse custom attributes
        r = handleCustomAttributes(element.getAttributes(), r);

        // Parse custom elements;
        r = handleCustomElements(element, r);
        
        return r;
    }

    private void parseReference(Element element, ServiceReferenceMetadataImpl reference, boolean topElement) {
        // Parse attributes
        if (topElement) {
            reference.setActivation(parseActivation(element));
        } else {
            reference.setActivation(ComponentMetadata.ACTIVATION_LAZY);
        }
        if (element.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
            reference.setDependsOn(parseList(element.getAttribute(DEPENDS_ON_ATTRIBUTE)));
        }
        if (element.hasAttribute(INTERFACE_ATTRIBUTE)) {
            reference.setInterface(element.getAttribute(INTERFACE_ATTRIBUTE));
        }
        if (element.hasAttribute(FILTER_ATTRIBUTE)) {
            reference.setFilter(element.getAttribute(FILTER_ATTRIBUTE));
        }
        if (element.hasAttribute(COMPONENT_NAME_ATTRIBUTE)) {
            reference.setComponentName(element.getAttribute(COMPONENT_NAME_ATTRIBUTE));
        }
        String availability = element.hasAttribute(AVAILABILITY_ATTRIBUTE) ? element.getAttribute(AVAILABILITY_ATTRIBUTE) : defaultAvailability;
        if (AVAILABILITY_MANDATORY.equals(availability)) {
            reference.setAvailability(ServiceReferenceMetadata.AVAILABILITY_MANDATORY);
        } else if (AVAILABILITY_OPTIONAL.equals(availability)) {
            reference.setAvailability(ServiceReferenceMetadata.AVAILABILITY_OPTIONAL);
        } else {
            throw new ComponentDefinitionException("Illegal value for " + AVAILABILITY_ATTRIBUTE + " attribute: " + availability);
        }
        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, REFERENCE_LISTENER_ELEMENT)) {
                        reference.addServiceListener(parseServiceListener(e, reference));
                    }
                }
            }
        }
    }

    private ReferenceListener parseServiceListener(Element element, ComponentMetadata enclosingComponent) {
        ReferenceListenerImpl listener = new ReferenceListenerImpl();
        Metadata listenerComponent = null;
        // Parse attributes
        if (element.hasAttribute(REF_ATTRIBUTE)) {
            listenerComponent = new RefMetadataImpl(element.getAttribute(REF_ATTRIBUTE));
        }
        String bindMethodName = null;
        String unbindMethodName = null;
        if (element.hasAttribute(BIND_METHOD_ATTRIBUTE)) {
            bindMethodName = element.getAttribute(BIND_METHOD_ATTRIBUTE);
            listener.setBindMethod(bindMethodName);
        }
        if (element.hasAttribute(UNBIND_METHOD_ATTRIBUTE)) {
            unbindMethodName = element.getAttribute(UNBIND_METHOD_ATTRIBUTE);
            listener.setUnbindMethod(unbindMethodName);
        }
        if (bindMethodName == null && unbindMethodName == null) {
            throw new ComponentDefinitionException("One of " + BIND_METHOD_ATTRIBUTE + " or " + UNBIND_METHOD_ATTRIBUTE + " must be set");
        }
        // Parse elements
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(e.getNamespaceURI())) {
                    if (nodeNameEquals(e, REF_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BLUEPRINT_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        String component = e.getAttribute(COMPONENT_ID_ATTRIBUTE);
                        if (component == null || component.length() == 0) {
                            throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ID_ATTRIBUTE + " attribute");
                        }
                        listenerComponent = new RefMetadataImpl(component);
                    } else if (nodeNameEquals(e, BEAN_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BLUEPRINT_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        listenerComponent = parseBeanMetadata(e, false);
                    } else if (nodeNameEquals(e, REFERENCE_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BLUEPRINT_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        listenerComponent = parseReference(e, false);
                    } else if (nodeNameEquals(e, SERVICE_ELEMENT)) {
                        if (listenerComponent != null) {
                            throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BLUEPRINT_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                        }
                        listenerComponent = parseService(e, false);
                    }
                } else {
                    if (listenerComponent != null) {
                        throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BLUEPRINT_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element can be set");
                    }
                    listenerComponent = parseCustomElement(e, enclosingComponent);
                }
            }
        }
        if (listenerComponent == null) {
            throw new ComponentDefinitionException("One of " + REF_ATTRIBUTE + " attribute, " + REF_ELEMENT + ", " + BLUEPRINT_ELEMENT + ", " + REFERENCE_ELEMENT + ", " + SERVICE_ELEMENT + " or custom element must be set");
        }
        listener.setListenerComponent((Target) listenerComponent);
        return listener;
    }

    public List<String> parseInterfaceNames(Element element) {
        List<String> interfaceNames = new ArrayList<String>();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (nodeNameEquals(e, VALUE_ELEMENT)) {
                    String v = getTextValue(e).trim();
                    if (interfaceNames.contains(v)) {
                        throw new ComponentDefinitionException("The element " + INTERFACES_ELEMENT + " should not contain the same interface twice");
                    }
                    interfaceNames.add(getTextValue(e));
                } else {
                    throw new ComponentDefinitionException("Unsupported element " + e.getNodeName() + " inside an " + INTERFACES_ELEMENT + " element");
                }
            }
        }
        return interfaceNames;
    }

    private Metadata parseArgumentOrPropertyValue(Element element, ComponentMetadata enclosingComponent) {
        Metadata [] values = new Metadata[3];
        
        if (element.hasAttribute(REF_ATTRIBUTE)) {
            values[0] = new RefMetadataImpl(element.getAttribute(REF_ATTRIBUTE));
        } 
        
        if (element.hasAttribute(VALUE_ATTRIBUTE)) {
            values[1] = new ValueMetadataImpl(element.getAttribute(VALUE_ATTRIBUTE));
        } 
        
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (isBlueprintNamespace(node.getNamespaceURI()) && nodeNameEquals(node, DESCRIPTION_ELEMENT)) {
                    // Ignore description elements
                } else {
                    values[2] = parseValueGroup(e, enclosingComponent, true);                    
                    break;
                }
            }
        }
        
        Metadata value = null;
        for (Metadata v : values) {
            if (v != null) {
                if (value == null) {
                    value = v;
                } else {
                    throw new ComponentDefinitionException("Only one of " + REF_ATTRIBUTE + " attribute, " + VALUE_ATTRIBUTE + " attribute or sub element must be set");
                }
            }
        }

        if (value == null) {
            throw new ComponentDefinitionException("One of " + REF_ATTRIBUTE + " attribute, " + VALUE_ATTRIBUTE + " attribute or sub element must be set");
        }
        
        return value;
    }

    private Metadata parseValueGroup(Element element, ComponentMetadata enclosingComponent, boolean allowNull) {
        if (isBlueprintNamespace(element.getNamespaceURI())) {
            if (nodeNameEquals(element, BEAN_ELEMENT)) {
                return parseBeanMetadata(element, false);
            } else if (nodeNameEquals(element, REFERENCE_ELEMENT)) {
                return parseReference(element, false);
            } else if (nodeNameEquals(element, SERVICE_ELEMENT)) {
                return parseService(element, false);
            } else if (nodeNameEquals(element, REFERENCE_LIST_ELEMENT) ) {
                return parseRefList(element, false);
            } else if (nodeNameEquals(element, NULL_ELEMENT) && allowNull) {
                return NullMetadata.NULL;
            } else if (nodeNameEquals(element, VALUE_ELEMENT)) {
                return parseValue(element);
            } else if (nodeNameEquals(element, REF_ELEMENT)) {
                return parseRef(element);
            } else if (nodeNameEquals(element, IDREF_ELEMENT)) {
                return parseIdRef(element);
            } else if (nodeNameEquals(element, LIST_ELEMENT)) {
                return parseList(element, enclosingComponent);
            } else if (nodeNameEquals(element, SET_ELEMENT)) {
                return parseSet(element, enclosingComponent);
            } else if (nodeNameEquals(element, MAP_ELEMENT)) {
                return parseMap(element, enclosingComponent);
            } else if (nodeNameEquals(element, PROPS_ELEMENT)) {
                return parseProps(element);
            } else if (nodeNameEquals(element, ARRAY_ELEMENT)) {
                return parseArray(element, enclosingComponent);
            } else {
                throw new ComponentDefinitionException("Unknown blueprint element " + element.getNodeName());
            }
        } else {
            return parseCustomElement(element, enclosingComponent);
        }
    }

    private ValueMetadata parseValue(Element element) {
        String type = null;
        if (element.hasAttribute(TYPE_ATTRIBUTE)) {
            type = element.getAttribute(TYPE_ATTRIBUTE);
        }
        return new ValueMetadataImpl(getTextValue(element), type);
    }

    private RefMetadata parseRef(Element element) {
        String component = element.getAttribute(COMPONENT_ID_ATTRIBUTE);
        if (component == null || component.length() == 0) {
            throw new ComponentDefinitionException("Element " + REF_ELEMENT + " must have a valid " + COMPONENT_ID_ATTRIBUTE + " attribute");
        }
        return new RefMetadataImpl(component);
    }
    private Metadata parseIdRef(Element element) {
        String component = element.getAttribute(COMPONENT_ID_ATTRIBUTE);
        if (component == null || component.length() == 0) {
            throw new ComponentDefinitionException("Element " + IDREF_ELEMENT + " must have a valid " + COMPONENT_ID_ATTRIBUTE + " attribute");
        }
        return new IdRefMetadataImpl(component);
    }

    private int parseActivation(Element element) {
        String initialization = element.hasAttribute(ACTIVATION_ATTRIBUTE) ? element.getAttribute(ACTIVATION_ATTRIBUTE) : defaultActivation;
        if (ACTIVATION_EAGER.equals(initialization)) {
            return ComponentMetadata.ACTIVATION_EAGER;
        } else if (ACTIVATION_LAZY.equals(initialization)) {
            return ComponentMetadata.ACTIVATION_LAZY;
        } else {
            throw new ComponentDefinitionException("Attribute " + ACTIVATION_ATTRIBUTE + " must be equal to " + ACTIVATION_EAGER + " or " + ACTIVATION_LAZY);
        }
    }
    
    private ComponentMetadata handleCustomAttributes(NamedNodeMap attributes, ComponentMetadata enclosingComponent) {
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node node = attributes.item(i);
                if (node instanceof Attr && 
                    node.getNamespaceURI() != null && 
                    !isBlueprintNamespace(node.getNamespaceURI())) {
                    enclosingComponent = decorateCustomNode(node, enclosingComponent);
                }
            }
        }
        return enclosingComponent;
    }
    
    private ComponentMetadata handleCustomElements(Element element, ComponentMetadata enclosingComponent) {
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                if (!isBlueprintNamespace(node.getNamespaceURI())) {
                    enclosingComponent = decorateCustomNode(node, enclosingComponent);
                }
            }
        }
        return enclosingComponent;
    }
    
    private ComponentMetadata decorateCustomNode(Node node, ComponentMetadata enclosingComponent) {
        NamespaceHandler handler = getNamespaceHandler(node);
        ParserContextImpl context = new ParserContextImpl(this, registry, enclosingComponent, node);
        return handler.decorate(node, enclosingComponent, context);
    }

    private Metadata parseCustomElement(Element element, ComponentMetadata enclosingComponent) {
        NamespaceHandler handler = getNamespaceHandler(element);
        ParserContextImpl context = new ParserContextImpl(this, registry, enclosingComponent, element);
        return handler.parse(element, context);
    }

    private NamespaceHandler getNamespaceHandler(Node node) {
        if (namespaceHandlerRegistry == null) {
            throw new ComponentDefinitionException("Unsupported node (namespace handler registry is not set): " + node);
        }
        URI ns = URI.create(node.getNamespaceURI());
        NamespaceHandler handler = this.namespaceHandlerRegistry.getNamespaceHandler(ns);
        if (handler == null) {
            throw new ComponentDefinitionException("Unsupported node namespace: " + node.getNamespaceURI());
        }
        return handler;
    }
    
    public String getId(Element element) {
        String id;
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            id = element.getAttribute(ID_ATTRIBUTE);
        } else {
            do {
                id = "." + idPrefix + ++idCounter;
            } while (ids.contains(id));
        }
        ids.add(id);
        return id;
    }

    public static boolean isBlueprintNamespace(String ns) {
        return BLUEPRINT_NAMESPACE.equals(ns);
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return (name.equals(node.getNodeName()) || name.equals(node.getLocalName()));
    }

    private static List<String> parseList(String list) {
        String[] items = list.split(" ");
        List<String> set = new ArrayList<String>();
        for (String item : items) {
            item = item.trim();
            if (item.length() > 0) {
                set.add(item);
            }
        }
        return set;                   
    }

    private static String getTextValue(Element element) {
        StringBuffer value = new StringBuffer();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                value.append(item.getNodeValue());
            }
        }
        return value.toString();
    }

    private static DocumentBuilderFactory getDocumentBuilderFactory() {
        if (documentBuilderFactory == null) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            documentBuilderFactory = dbf;
        }
        return documentBuilderFactory;
    }

}
