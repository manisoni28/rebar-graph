package rebar.graph.gcp;

public class ProjectZone {

	String projectId;
	String zoneName;
	
	ProjectZone(String projectId, String zone) {
		this.projectId = projectId;
		this.zoneName = zone;
	}
	
	public String getProjectId() {
		return projectId;
	}
	
	public String getZoneName() {
		return zoneName;
	}
}
