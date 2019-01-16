package rebar.dashboard;

import java.io.File;
import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator.Builder;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;

import rebar.graph.neo4j.GraphDriver;

@Component
public class TokenManager {

	String localSecret;
	String issuer = "rebar";
	
	@Autowired
	GraphDriver neo4jDriver;
	
	public TokenManager() {
		// TODO Auto-generated constructor stub
	}

	private Algorithm algorithm() {
		return Algorithm.HMAC256(secret());
	}
	
	private String secret() {
		String jwtSecret = System.getenv("JWT_SECRET");
		if (!Strings.isNullOrEmpty(jwtSecret)) {
			return jwtSecret;
		}
		if (new File("./build.gradle").exists()) {
			return "development";
		}
		synchronized(this) {
			if (Strings.isNullOrEmpty(localSecret)) {
				byte [] data = new byte[8];
				new SecureRandom().nextBytes(data);
				localSecret = BaseEncoding.base64().encode(data);
				return localSecret;
			}
			
		}
		return localSecret;
	}
	
	String sign(Builder builder) {
		return sign(builder,algorithm());
	}
	String sign(Builder builder, Algorithm algo) {
		return builder.sign(algo);
	}
	
	JWTVerifier getVerifier() {
		
	        JWTVerifier verifier = JWT.require(algorithm())
	            .withIssuer(issuer)
	            .build(); 
	     return verifier;
	        
	}
	
	public void invalidate(String token) {
		// store the token so that we can check for invalidated JWT tokens
		
		String hash = Hashing.sha256().hashBytes(token.getBytes()).toString();
		
		neo4jDriver.cypher("merge (t:InvalidToken {tokenHash:{tokenHash}}) set t.updateTs=timestamp() return t").param("tokenHash",hash).exec();;
		
	}
}
