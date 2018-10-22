package rebar.graph.doc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import rebar.graph.neo4j.Neo4jDriver;

public class MarkdownTest {

	@Test
	public void testIt() throws IOException {
		
		
		
		DataModelMarkdown m = new DataModelMarkdown().parse("# Reference \n\n## Data Model");
		
		m.withEntity("FooBar");
		m.withEntity("FizzBuzz");
		
		
		m.withPropertiesTable("FooBar").setValue("Name", "foo", "Name","foo",false);
		m.withPropertiesTable("FooBar").setValue("Name", "foo", "Type","String",false);
		m.withPropertiesTable("FooBar").setValue("Name", "foo", "Type","Number",true);
		
		
		BufferedReader br = new BufferedReader(new StringReader(m.writeAsString()));
		
		Assertions.assertThat(br.readLine()).startsWith("# Reference");
		br.readLine();
		Assertions.assertThat(br.readLine()).startsWith("## Data Model");
		Assertions.assertThat(br.readLine()).startsWith("### FooBar");
		Assertions.assertThat(br.readLine()).startsWith("| Name | Type | Description |");
		Assertions.assertThat(br.readLine()).startsWith("|------|------|------|");
		Assertions.assertThat(br.readLine()).startsWith("| foo | Number | |");
		Assertions.assertThat(br.readLine()).startsWith("### FizzBuzz");
		
		
	
		
		
	
	}

}
