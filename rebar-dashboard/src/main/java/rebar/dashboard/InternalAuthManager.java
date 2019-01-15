package rebar.dashboard;

import java.util.Optional;

import javax.annotation.PostConstruct;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import rebar.graph.neo4j.Neo4jDriver;

@Component
public class InternalAuthManager {

	Logger logger = LoggerFactory.getLogger(InternalAuthManager.class);
	
	@Autowired
	Neo4jDriver driver;

	public InternalAuthManager() {

	}

	public boolean authenticate(String username, String password) {

		try {
			Optional<JsonNode> n = driver.cypher("match (p:InternalShadow {username:{username}}) return p")
					.param("username", username).findFirst();
			if (n == null) {
				return false;
			}
			return BCrypt.checkpw(password, n.get().path("bcrypt").asText());
		} catch (java.lang.StringIndexOutOfBoundsException e) {
			// If the hashed value doesn't conform to bcrypt encoding (i.e. empty string) we can end up here
			return false;
		}
		catch (Exception e) {
			logger.warn("unexpected",e);
		}

		return false;
	}

	@PostConstruct
	public void seedUser() {
		Optional<JsonNode> n = driver.cypher("match (p:InternalShadow {username:{username}}) return p")
				.param("username", "admin").findFirst();
		if (!n.isPresent()) {
			String bcryptPassword = BCrypt.hashpw("admin", BCrypt.gensalt());
			driver.cypher("merge (p:InternalShadow {username:{username}}) set p.bcrypt={bcrypt}")
					.param("username", "admin").param("bcrypt", bcryptPassword).exec();
		}

	}

}
