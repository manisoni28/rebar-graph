package rebar.graph.core.alert;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.net.MediaType;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import rebar.util.EnvConfig;
import rebar.util.Json;
import rebar.util.RebarException;

public class SlackAlertManager extends AlertManager<SlackAlert> {

	OkHttpClient okhttp = new OkHttpClient.Builder().build();

	static Logger logger = LoggerFactory.getLogger(SlackAlertManager.class);
	@Autowired
	EnvConfig env;

	@Override
	public rebar.graph.core.alert.SlackAlert alert() {
		return new SlackAlert(this);
	}

	protected void send(SlackAlert slackAlert) {

	
			String url = env.get("SLACK_WEBHOOK").orElse(null);
			
			if (Strings.isNullOrEmpty(url)) {
				logger.warn("SLACK_WEBHOOK not set.  Message not sent: {}",slackAlert.text);
			}
			else {
				try {
					ObjectNode n = Json.objectNode();
					n.put("text", slackAlert.text);
					okhttp.newCall(new Request.Builder().url(url)
							.post(RequestBody.create(okhttp3.MediaType.parse("application/json"), n.toString()))
							.build()).execute().close();
				} catch (IOException e) {
					throw new RebarException(e);
				}
				
			}
			
			
	
	}
}
