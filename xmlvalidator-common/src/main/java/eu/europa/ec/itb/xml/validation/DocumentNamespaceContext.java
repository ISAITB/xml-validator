/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

package eu.europa.ec.itb.xml.validation;

import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A namespace context to lookup and map namespaces based on prefix and URI.
 *
 * Implementation based on source found <a href="http://www.ibm.com/developerworks/library/x-nmspccontext/">here</a>.
 */
public class DocumentNamespaceContext implements NamespaceContext {

    public static final String DEFAULT_NS = "default";
    private final Map<String, String> prefix2Uri = new HashMap<>();
    private final Map<String, String> uri2Prefix = new HashMap<>();

    /**
     * This constructor parses the document and stores all namespaces it can
     * find. If topLevelOnly is true, only namespaces in the root are used.
     *
     * @param document Source document.
     * @param topLevelOnly Restriction of the search to enhance performance.
     */
    public DocumentNamespaceContext(Document document, boolean topLevelOnly) {
        examineNode(document.getFirstChild(), topLevelOnly);
    }

    /**
     * A single node is read, the namespace attributes are extracted and stored.
     *
     * @param node To examine.
     * @param attributesOnly If true no recursion happens.
     */
    private void examineNode(Node node, boolean attributesOnly) {
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            storeAttribute((Attr) attribute);
        }
        if (!attributesOnly) {
            NodeList chields = node.getChildNodes();
            for (int i = 0; i < chields.getLength(); i++) {
                Node chield = chields.item(i);
                if (chield.getNodeType() == Node.ELEMENT_NODE)
                    examineNode(chield, false);
            }
        }
    }

    /**
     * This method looks at an attribute and stores it, if it is a namespace attribute.
     *
     * @param attribute To examine.
     */
    private void storeAttribute(Attr attribute) {
        // examine the attributes in namespace xmlns
        if (attribute.getNodeName().equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            if (getNamespaceURI(DEFAULT_NS) == null) {
                putInCache(DEFAULT_NS, attribute.getNodeValue());
            }
        }
        if (attribute.getNamespaceURI() != null && attribute.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            putInCache(attribute.getLocalName(), attribute.getNodeValue());
        } else if (attribute.getName().startsWith(XMLConstants.XMLNS_ATTRIBUTE+":")) {
            // Extract manually local name.
            int pos = attribute.getName().indexOf(XMLConstants.XMLNS_ATTRIBUTE+":");
            int len = XMLConstants.XMLNS_ATTRIBUTE.length()+1;
            if (attribute.getName().length() >= (pos + len)) {
                putInCache(attribute.getName().substring(pos+len), attribute.getNodeValue());
            }
        }
    }

    /**
     * Cache a prefix-URI mapping.
     *
     * @param prefix The prefix.
     * @param uri The URI.
     */
    private void putInCache(String prefix, String uri) {
        prefix2Uri.put(prefix, uri);
        uri2Prefix.put(uri, prefix);
    }

    /**
     * This method is called by XPath. It returns the default namespace, if the prefix is null or "".
     *
     * @param prefix To search for.
     * @return uri The URI.
     */
    public String getNamespaceURI(String prefix) {
        if (prefix == null || prefix.equals(XMLConstants.DEFAULT_NS_PREFIX) || prefix.equals(DEFAULT_NS)) {
            return prefix2Uri.get(DEFAULT_NS);
        } else {
            return prefix2Uri.get(prefix);
        }
    }

    /**
     * This method is not needed in this context, but can be implemented in a similar way.
     *
     * @param namespaceURI To search for.
     * @return The prefix.
     */
    public String getPrefix(String namespaceURI) {
        return uri2Prefix.get(namespaceURI);
    }

    /**
     * Iterate prefixes (not implemented).
     *
     * @param namespaceURI The URI.
     * @return Always null.
     */
    public Iterator getPrefixes(String namespaceURI) {
        // Not implemented
        return null;
    }

}
