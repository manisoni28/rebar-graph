package rebar.dashboard.report;

import java.util.Map;

public interface Report {
	
	public static interface Parameter {
		public String getName();
	}
	public String getDescription();
	public String getName();
	public String getQuery();
	public Map<String,Parameter> getParameters();
}
