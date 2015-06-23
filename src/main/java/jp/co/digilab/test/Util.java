package jp.co.digilab.test;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.xpath.XPathEvaluator;


public class Util {

	private static DOMImplementation domImplementation = null;
	private static DOMImplementationLS domImplementationLS = null;
	private static XPathEvaluator xPathEvaluator = null;

	private static void initializeDOMImplementation() {
		if (domImplementation == null) {
			try {
				domImplementation = DOMImplementationRegistry.newInstance().getDOMImplementation("+LS 3.0 +XPath 3.0");
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
			domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("+LS", "3.0");
			xPathEvaluator = (XPathEvaluator) domImplementation.getFeature("+XPath", "3.0");
		}
	}

	/**
	 * get global DOMImplementationLS object (load and save)
	 * @return
	 */
	public static DOMImplementationLS getLS() {
		initializeDOMImplementation();
		return domImplementationLS;
	}

	/**
	 * get global XPathEvaluator object
	 * @return
	 */
	public static XPathEvaluator getXPathEvaluator() {
		initializeDOMImplementation();
		return xPathEvaluator;
	}

	/**
	 * get XPath object for XHTML (prefix is h:)
	 * @return
	 */
	public static XPath getXPathForXHTML() {
		return getXPathForXHTML("h");
	}
	public static XPath getXPathForXHTML(String prefix) {
		XPath xPath = XPathFactory.newInstance().newXPath();
		xPath.setNamespaceContext(new NamespaceContext() {
			@Override
			public String getNamespaceURI(String prefix2) {
				if (prefix.equals(prefix2)) {
					return "http://www.w3.org/1999/xhtml";
				}
				return XMLConstants.NULL_NS_URI;
			}
			@Override
			public String getPrefix(String namespaceURI) {
				throw new UnsupportedOperationException();
			}
			@Override
			public Iterator<String> getPrefixes(String namespaceURI) {
				throw new UnsupportedOperationException();
			}
		});
		return xPath;
	}

	/**
	 * put DOM node into OutputStream object
	 * (for debugging)
	 * @param node
	 * @param outputStream
	 */
	public static void putNode(Node node, OutputStream outputStream) {
		initializeDOMImplementation();
		DOMImplementationLS ls = getLS();
		LSOutput lsOutput = ls.createLSOutput();
		lsOutput.setByteStream(outputStream);
		lsOutput.setEncoding("UTF-8"); // 固定でいいや
		LSSerializer lsSerializer = ls.createLSSerializer();
		DOMConfiguration domConfig = lsSerializer.getDomConfig();
		domConfig.setParameter("comments", false);
		domConfig.setParameter("format-pretty-print", true);
		domConfig.setParameter("xml-declaration", true);
		lsSerializer.write(node, lsOutput);
	}

	/**
	 * put DOM node into standard out
	 * (for debugging)
	 * @param node
	 */
	public static void printNode(Node node) {
		putNode(node, new FileOutputStream(FileDescriptor.out));
	}
}
