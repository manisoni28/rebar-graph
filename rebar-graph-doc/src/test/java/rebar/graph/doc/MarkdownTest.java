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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

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
