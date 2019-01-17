/**
 * Copyright 2018-2019 Rob Schoening
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rebar.graph.doc;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;

import rebar.graph.neo4j.GraphDriver;

public class DataModelMarkdown extends Markdown {


	GraphDriver driver;
	

	public DataModelMarkdown merge(String type) {
		
		withEntity(type);
		
		
		Map<String, String> map = Maps.newHashMap();
		driver.cypher("match (a:"+type+") return a limit 1000").forEach(it->{
			it.fields().forEachRemaining(f->{
				JsonNode n = f.getValue();
				if (n!=null) {
					map.put(f.getKey(), n.getNodeType().name());
				}
			});
		});
		
		TableSection ts = withPropertiesTable(type);

		map.forEach((name,t)->{
			ts.setValue("Name", name, "Name",name,false);
			ts.setValue("Name", name, "Type",t,false);
		});
	
		
		return this;
	}
	public DataModelMarkdown withNeo4jDriver(GraphDriver driver) {
		this.driver = driver;
		return this;
	}


	



	public MarkdownSection withEntity(String name) {
		MarkdownSection dataModelSection = findSection("Data Model").get();
		List<MarkdownSection> sections = getSections();
		int idx = sections.indexOf(dataModelSection);
		int level = dataModelSection.getSectionLevel().orElse(0);
		boolean stop = false;
		int i=0;
		for (i = idx + 1; (!stop) && i < sections.size(); i++) {
			MarkdownSection section = sections.get(i);
			if (section.getSectionName().orElse("").equals(name)) {
				
				return section;
			}
			stop = i<sections.size() && (section.getSectionLevel().orElse(0) < level
					|| (section.getSectionLevel().orElse(0) == level && section.getSectionName().isPresent()));
			
			
		}
	
		
			MarkdownSection entitySection = new MarkdownSection();
			String line = "";
			for (int j=0; j<level+1; j++) {
				line = line+"#";
			}
			line = line +" "+name;
			entitySection.addLine(line);
			sections.add(i,entitySection);
			return entitySection;
		

	}

	public TableSection insertTableAfter(MarkdownSection section) {
		TableSection ts = new TableSection();
		int idx = sections.indexOf(section);
		sections.add(idx+1, ts);
		return ts;
	}
	public TableSection withPropertiesTable(String name) {
		
		Optional<TableSection> table = findPropertiesTable(name);
		if (table.isPresent()) {
			return table.get();
		}
		MarkdownSection section = withEntity(name);
		TableSection ts =  insertTableAfter(section);
		ts.header(0, "Name");
		ts.header(1, "Type");
		ts.header(2, "Description");
		
		
		return ts;
	}
	public Optional<TableSection> findPropertiesTable(String name) {
		Optional<MarkdownSection> m = findSection(name);
		if (!m.isPresent()) {
			return Optional.empty();
		}

		List<MarkdownSection> sections = getSections();
		int idx = sections.indexOf(m.get());
		for (int i = idx + 1; i < sections.size(); i++) {
			MarkdownSection section = sections.get(i);
			if (section instanceof TableSection) {
				return Optional.of((TableSection) section);
			}
			if (section.getSectionName().isPresent()) {
				return Optional.empty();
			}
		}

		return Optional.empty();

	}

	public DataModelMarkdown parse(String t) throws IOException {
		super.parse(t);
		

		if (!findSection("Data Model").isPresent()) {
			throw new IOException("does not have section 'Data Model'");
		}

		return this;
	}

	public DataModelMarkdown parse(File f) throws IOException {
		super.parse(f);
		
		
		if (!findSection("Data Model").isPresent()) {
			throw new IOException(f + " does not have section 'Data Model'");
		}

	
		return this;
	}
	
	public DataModelMarkdown mergeAll(Predicate<String> p) {
		
		driver.cypher("match (a) return distinct labels(a)[0] as label").stream().map(it->it.path("label").asText()).filter(p).forEach(it->{
			merge(it);
		});
		
		return this;
	}
}
