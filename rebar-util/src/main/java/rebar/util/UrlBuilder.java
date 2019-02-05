package rebar.util;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class UrlBuilder {

	String host;
	int port;
	String protocol;

	List<String> pathParts = new LinkedList<>();

	Map<String, String> pathParameters = new HashMap<>();

	Map<String, String> queryParameters = new HashMap<>();
	boolean trailingSlash = false;

	private <T extends UrlBuilder> T parsePath(String x) throws UnsupportedEncodingException {
		trailingSlash = x.endsWith("/");
		pathParts.addAll(Splitter.on("/").omitEmptyStrings().splitToList(x));
		for (int i=0; i<pathParts.size(); i++) {
			pathParts.set(i, URLDecoder.decode(pathParts.get(i), "UTF8"));
		}
		return (T) this;
	}

	public <T extends UrlBuilder> T url(String u) {
		try {
			URL x = new URL(u);
			host = x.getHost();
			port = x.getPort();
			protocol = x.getProtocol();
			parsePath(x.getPath());

			return (T) this;
		} catch (Exception e) {
			throw new RebarException(e);
		}
	}

	List<String> substitute(List<String> x) {
		try {
			List<String> result = new ArrayList<>();
			for (String part : x) {
				if (part.length() > 2 && part.charAt(0) == '{' && part.charAt(part.length() - 1) == '}') {
					String pname = part.substring(1, part.length() - 1);
					String val = pathParameters.get(pname);
					if (Strings.isNullOrEmpty(val)) {
						throw new IllegalArgumentException("path param not specified: " + pname);
					}
					result.add(URLEncoder.encode(val, "UTF8"));
				} else {
					result.add(URLEncoder.encode(part, "UTF8"));
				}
			}
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new RebarException(e);
		}
	}

	public String toString() {

		StringBuffer sb = new StringBuffer();
		try {

			int count = 0;
			for (String key : queryParameters.keySet()) {
				if (count++ == 0) {
					sb.append("?");
				} else {
					sb.append("&");
				}
				sb.append(URLEncoder.encode(key, "UTF8"));
				sb.append("=");
				sb.append(URLEncoder.encode(Strings.nullToEmpty(queryParameters.get(key)), "UTF8"));
			}
		} catch (UnsupportedEncodingException e) {
			throw new RebarException(e);
		}
		String qs = sb.toString();
		String path = com.google.common.base.Joiner.on("/").join(substitute(pathParts));

		return String.format("%s://%s%s%s/%s%s%s", protocol, host, port > 0 ? ":" : "",
				port > 0 ? Integer.toString(port) : "", trailingSlash ? "/" : "", path, qs.length() > 0 ? qs : "");

	}

	public <T extends UrlBuilder> T path(String path) {
		trailingSlash = path.endsWith("/");
		pathParts.addAll(Splitter.on("/").omitEmptyStrings().splitToList(path));
		return (T) this;
	}

	public <T extends UrlBuilder> T pathParam(String key, String val) {
		pathParameters.put(key, val);
		return (T) this;
	}

	public <T extends UrlBuilder> T queryParam(String key, String val) {
		queryParameters.put(key, val);
		return (T) this;
	}
}
