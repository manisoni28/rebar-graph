package rebar.dashboard;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.base.Splitter;
import com.google.common.io.BaseEncoding;

public class JWTTest {

	
	
	
	@Test
	public void testIt() {
		try {
		    Algorithm algorithm = Algorithm.HMAC256("secret");
		    String token = JWT.create()
		        .withIssuer("auth0")
		        .withSubject("hello")
		        .sign(algorithm);
		    
		 
		 
		    algorithm = Algorithm.HMAC256("secret");
	        JWTVerifier verifier = JWT.require(algorithm)
	            .withIssuer("auth0")
	            .build(); //Reusable verifier instance
	        DecodedJWT jwt = verifier.verify(token);
	        
	     
		} catch (JWTCreationException exception){
		    //Invalid Signing configuration / Couldn't convert Claims.
		}
		
		  
	}

}
