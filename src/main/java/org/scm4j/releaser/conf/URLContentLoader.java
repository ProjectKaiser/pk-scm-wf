package org.scm4j.releaser.conf;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class URLContentLoader {
	
	private static final String URL_SEPARATOR = ";";
	public static final String DEFAULT_PROTOCOL = "file:///";

	public List<String> getContentsFromUrls(List<String> separatedUrls) throws IOException {
		List<String> res = new ArrayList<>();
		if (separatedUrls == null) {
			return res;
		}
		for (String separatedUrl : separatedUrls) {
			String[] urls = separatedUrl.split(URL_SEPARATOR);
			for (String url : urls) {
				res.add(getContentFromUrl(url));
			}
		}
		return res;
	}
	
	public String getWithDefaultProtocol(String urlStr) {
		if (!urlStr.trim().toLowerCase().startsWith("file:") && !urlStr.trim().toLowerCase().startsWith("http:") && !urlStr.trim().toLowerCase().startsWith("https:")) {
			return DEFAULT_PROTOCOL + urlStr;
		}
		return urlStr;
	}

	public String getContentFromUrl(String urlString) throws IOException {
		if (urlString == null || urlString.isEmpty()) {
			return "";
		}
		URL url = new URL(getWithDefaultProtocol(urlString));
		try (InputStream inputStream = url.openStream()) {
			return IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		}
	}
}
