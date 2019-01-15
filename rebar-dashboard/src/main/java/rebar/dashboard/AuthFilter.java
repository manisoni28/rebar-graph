package rebar.dashboard;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;

public class AuthFilter implements Filter {

	static Logger logger = LoggerFactory.getLogger(AuthFilter.class);
	
	@Autowired
	TokenManager jwtManager;

	public AuthFilter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		System.out.println("init");

	}

	static Optional<String> extractJWT(HttpServletRequest request) {
		String tokenVal = null;
		String val = request.getHeader("Authorization");
		if (val != null) {
			Iterator<String> t = Splitter.on(" ").omitEmptyStrings().split(val).iterator();

			if (t.hasNext()) {
				tokenVal = t.next();
				if (tokenVal.toLowerCase().startsWith("bearer")) {
					if (t.hasNext()) {
						tokenVal = t.next();
					}
				}
			}
		}
		if (tokenVal != null) {
			return Optional.ofNullable(tokenVal);
		}
		if (tokenVal == null || tokenVal.isEmpty()) {
			Cookie[] cookies = request.getCookies();
			if (cookies != null) {
				for (Cookie cookie : cookies) {
					String cookieName = cookie.getName();
					if (cookieName != null && cookieName.equals("rebar")) {
						tokenVal = cookie.getValue();
						return Optional.ofNullable(tokenVal);
					}
				}
			}
		}
		return Optional.empty();
	}

	boolean isWhitelisted(HttpServletRequest request) {

		String path = request.getRequestURI();
		if (path.startsWith("/login") || path.startsWith("/logout") || path.startsWith("/webjars")
				|| path.startsWith("/src-noconflict/")) {
			return true;
		}
		return false;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest r = (HttpServletRequest) request;
		HttpServletResponse resp = (HttpServletResponse) response;

		if (isWhitelisted(r)) {
			chain.doFilter(request, response);
			return;
		}

		Optional<String> jwt = extractJWT(r);
		if (!jwt.isPresent()) {
			resp.sendRedirect("/login");
			return;
		}

		try {
		
			JWTVerifier verifier = jwtManager.getVerifier();
			DecodedJWT decoded = verifier.verify(jwt.get());

			String username = decoded.getSubject();
			request.setAttribute("username", username);
			
			long expiration = decoded.getExpiresAt().getTime();
			long msUntilExpiration = decoded.getExpiresAt().getTime()-System.currentTimeMillis();
			
			logger.debug("{}ms until expiration",msUntilExpiration);
	
		
		} catch (JWTDecodeException e) {
			resp.sendRedirect("/login");
		} catch (SignatureVerificationException e) {
			e.printStackTrace();
			unsetCookie(resp);
			resp.sendRedirect("/login");
			return;
		}

		chain.doFilter(request, response);
	}

	public static void unsetCookie(HttpServletResponse resp) {
		Cookie c = new Cookie("rebar", "");
		c.setHttpOnly(true);
		resp.addCookie(c);
	}
	@Override
	public void destroy() {
		

	}

}
