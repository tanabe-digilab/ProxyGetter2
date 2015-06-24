package jp.co.digilab.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Calendar;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nu.validator.htmlparser.dom.HtmlDocumentBuilder;


public class ProxyGetter {

	private static final String TEST_SERVER_URL = "http://www.digilab.co.jp/";
	private static final int TIMEOUT = 30 * 1000; // unit:ms

	public static final void main(String[] arguments) {
		TreeMap<Long, String> resultMap = new TreeMap<>();
		try {
			resultMap.putAll(fromXroxyCom());
			resultMap.putAll(fromAliveproxyCom());
			System.out.println("result:");
			System.out.println(resultMap);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static TreeMap<Long, String> fromXroxyCom() throws IOException, SAXException, XPathException {
		System.out.println("fromXroxyCom()");
		TreeMap<Long, String> resultMap = new TreeMap<>();
		URL url = new URL("http://www.xroxy.com/proxylist.php?country=VN");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(TIMEOUT);
		connection.setReadTimeout(TIMEOUT);
		connection.setRequestMethod("GET");
		connection.connect();
		try (InputStream is = connection.getInputStream()) {
			try (InputStreamReader isr = new InputStreamReader(is, "UTF-8")) {
				try (BufferedReader br = new BufferedReader(isr)) {
					HtmlDocumentBuilder documentBuilder = new HtmlDocumentBuilder();
					documentBuilder.setScriptingEnabled(false);
					Document document = documentBuilder.parse(new InputSource(br));

					// XPath は org.w3c.dom のものは program が複雑になるので javax.xml を用ゐる
					XPath xPath = Util.getXPathForXHTML();

					NodeList trList = (NodeList) xPath.evaluate("//h:div[@id='content']/h:table/h:tbody/h:tr[starts-with(@class, 'row')]", document, XPathConstants.NODESET);
					if (trList == null || trList.getLength() < 1) {
						System.err.println("trList == null || trList.getLength() < 1");
						return resultMap;
					}
					for (int i = 0, iMax = trList.getLength();  i < iMax;  ++i) {
						Element tr = (Element) trList.item(i);
						NodeList tdList = (NodeList) xPath.evaluate("h:td", tr, XPathConstants.NODESET);
						int index = 1;
						String ipAddress = tdList.item(index++).getTextContent().trim();
						String port = tdList.item(index++).getTextContent();
						String type = tdList.item(index++).getTextContent();
						String ssl = tdList.item(index++).getTextContent();
						String country = tdList.item(index++).getTextContent();
						String latency = tdList.item(index++).getTextContent();
						String reliability = tdList.item(index++).getTextContent();
						System.out.printf("IP:%s, port:%s, type:%s, SSL:%s, country:%s, latency:%s, reliability:%s\n", ipAddress, port, type, ssl, country, latency, reliability);

						long elapsed = testProxy(ipAddress, port);
						if (elapsed < Long.MAX_VALUE) {
							System.out.println("elapsed: " + elapsed);
							resultMap.put(elapsed, ipAddress + ":" + port);
						}
					}
				}
			}
		}
		return resultMap;
	}

	private static TreeMap<Long, String> fromAliveproxyCom() throws IOException, SAXException, XPathException {
		System.out.println("fromAliveproxyCom()");
		TreeMap<Long, String> resultMap = new TreeMap<>();
		URL url = new URL("http://www.aliveproxy.com/proxy-list/proxies.aspx/Vietnam-vn");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setConnectTimeout(TIMEOUT);
		connection.setReadTimeout(TIMEOUT);
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Referer", "http://aliveproxy.com/");
		connection.setRequestProperty("Cache-Control", "max-age=0");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0");
		connection.connect();
		int status = connection.getResponseCode();
		switch (status) {
		case HttpURLConnection.HTTP_FORBIDDEN:
			System.err.println(url + " ... status:" + status);
			return resultMap;
		}
		try (InputStream is = connection.getInputStream()) {
			try (InputStreamReader isr = new InputStreamReader(is, "UTF-8")) {
				try (BufferedReader br = new BufferedReader(isr)) {
					HtmlDocumentBuilder documentBuilder = new HtmlDocumentBuilder();
					documentBuilder.setCheckingNormalization(true);
					documentBuilder.setHtml4ModeCompatibleWithXhtml1Schemata(true);
					documentBuilder.setScriptingEnabled(true);
					Document document = documentBuilder.parse(new InputSource(br));

					XPath xPath = Util.getXPathForXHTML();

					NodeList trList = (NodeList) xPath.evaluate("//h:table/h:tbody/h:tr/h:td/h:table/h:tbody/h:tr[@class='cw-list']", document, XPathConstants.NODESET);
					if (trList == null || trList.getLength() < 1) {
						System.err.println("trList == null || trList.getLength() < 1");
						return resultMap;
					}
					for (int i = 0, iMax = trList.getLength();  i < iMax;  ++i) {
						Element tr = (Element) trList.item(i);
						NodeList tdList = (NodeList) xPath.evaluate("h:td", tr, XPathConstants.NODESET);
						int index = 0;
						String ipAndPort = tdList.item(index++).getTextContent().trim();
						// ipAndPort は <br/> までのテキスト部分
						Pattern pattern = Pattern.compile("^((?:\\d+\\.){3}\\d+:\\d+).*$");
						Matcher matcher = pattern.matcher(ipAndPort);
						if (matcher.find()) {
							ipAndPort = matcher.group(1);
						}
						String ipAddress = ipAndPort.split(":")[0];
						String port = ipAndPort.split(":")[1];
						String country = tdList.item(index++).getTextContent();
						String type = tdList.item(index++).getTextContent();
						String ssl = tdList.item(index++).getTextContent();
						System.out.printf("IP:%s, port:%s, country:%s, type:%s, SSL:%s\n", ipAddress, port, country, type, ssl);

						long elapsed = testProxy(ipAddress, port);
						if (elapsed < Long.MAX_VALUE) {
							System.out.println("elapsed: " + elapsed);
							resultMap.put(elapsed, ipAddress + ":" + port);
						}
					}
				}
			}
		}
		return resultMap;
	}

	private static long testProxy(String ipAddress, String port) throws MalformedURLException {
		long time0 = Calendar.getInstance().getTimeInMillis();
		URL url = new URL(TEST_SERVER_URL);
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ipAddress, Integer.parseInt(port)));
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
			connection.setConnectTimeout(TIMEOUT);
			connection.setReadTimeout(TIMEOUT);
			connection.setRequestMethod("GET");
			connection.connect();
			try (InputStream is = connection.getInputStream()) {
				try (InputStreamReader isr = new InputStreamReader(is, "UTF-8")) {
					try (BufferedReader br = new BufferedReader(isr)) {
						HtmlDocumentBuilder documentBuilder = new HtmlDocumentBuilder();
						try {
							Document document = documentBuilder.parse(new InputSource(br));
						} catch (SAXException e) {
							System.err.println("DigiLab のサイト、変: " + e.getMessage());
						}
						long time1 = Calendar.getInstance().getTimeInMillis();
						return time1 - time0;
					}
				}
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return Long.MAX_VALUE;
	}
}
