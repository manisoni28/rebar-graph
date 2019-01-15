package rebar.dashboard;

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.google.common.base.Strings;

@Component
@Controller
public class AuthController {

	
	@Autowired
	private TokenManager jwtManager;
	
	@Autowired
	InternalAuthManager internalAuthManager;
	
	@Autowired 
	TokenManager tokenManager;
	
	@RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView login(HttpServletRequest request, HttpServletResponse response) {
		
	
		if (request.getMethod().toLowerCase().equals("post")) {
			String username = Strings.nullToEmpty(request.getParameter("username"));
			String password = Strings.nullToEmpty(request.getParameter("password"));
			
			if (internalAuthManager.authenticate(username, password)) {
			
			    Builder builder = JWT.create()
			        .withIssuer("rebar")
			        .withSubject(username)
			        .withExpiresAt(new Date(System.currentTimeMillis()+TimeUnit.MINUTES.toMillis(30)));
			    String token = jwtManager.sign(builder);
			    Cookie cookie = new Cookie("rebar",token);
			    cookie.setMaxAge((int) TimeUnit.MINUTES.toSeconds(30));
			    cookie.setHttpOnly(true);
			    response.addCookie(cookie);
			    return new ModelAndView("redirect:/");
			}
			
			
		}
		return new ModelAndView("login");
	}

	@RequestMapping(value = "/logout", method = { RequestMethod.GET, RequestMethod.POST })
	public ModelAndView logout(HttpServletRequest request, HttpServletResponse response) {
		
		Optional<String> jwt = AuthFilter.extractJWT(request);

		if (jwt.isPresent()) {
			tokenManager.invalidate(jwt.get());
		}
		Cookie c = new Cookie("rebar","");
		response.addCookie(c);
		return new ModelAndView("redirect:/");
	}

}
