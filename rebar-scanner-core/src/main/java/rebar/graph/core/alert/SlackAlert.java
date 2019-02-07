package rebar.graph.core.alert;

import rebar.graph.core.RebarGraph;

public class SlackAlert extends Alert {

	SlackAlertManager slack;
	SlackAlert(SlackAlertManager m) {
		slack = m;
	}
	
	
	@Override
	public <T extends Alert> T send() {	
		slack.send(this);
		return (T) this;
	}

	public static SlackAlert create() {
		return new SlackAlert(RebarGraph.getApplicationContext().getBean(SlackAlertManager.class));
	}
}
