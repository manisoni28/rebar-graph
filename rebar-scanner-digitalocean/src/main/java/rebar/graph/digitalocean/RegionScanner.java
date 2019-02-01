package rebar.graph.digitalocean;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.pojo.Region;
import com.myjeeva.digitalocean.pojo.Regions;

public class RegionScanner extends DigitalOceanEntityScanner<Region> {

	@Override
	protected ObjectNode toJson(Region entity) {
	
		ObjectNode n = super.toJson(entity);
		n.put("region",entity.getSlug());
		return n;
	}

	public RegionScanner(DigitalOceanScanner scanner) {
		super(scanner);
	}

	@Override
	protected void doScan() {
		
		try {
			Regions regions = getClient().getAvailableRegions(1);
			adviseRateLimit(getEntityType(), regions.getRateLimit());
			regions.getRegions().forEach(it->{
				
				project(it);
			});
			
		}
		catch (DigitalOceanException | RequestUnsuccessfulException  e) {
			maybeThrow(e);
		}
	}

	@Override
	protected void project(Region entity) {
		ObjectNode n = toJson(entity);
		digitalOceanNodes(getEntityType().name()).idKey("region").properties(n).merge();
		
		
	}

	@Override
	public DigitalOceanEntityType getEntityType() {
		return DigitalOceanEntityType.DigitalOceanRegion;
	}

	
}
