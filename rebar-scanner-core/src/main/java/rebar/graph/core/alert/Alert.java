package rebar.graph.core.alert;

public abstract class Alert {

	String text;
	

	public <T extends Alert> T text(String text) {
		this.text = text;
		return (T) this;
	}


	public abstract <T extends Alert> T send();
}
