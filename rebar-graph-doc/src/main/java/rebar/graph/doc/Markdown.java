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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import rebar.graph.doc.Markdown.MarkdownSection;

public class Markdown {

	static String TABLE_PATTERN = "\\s*(\\|.*)";
	private static String SECTION_START = "\\s*(#+)(\\s+(.*?))\\s*";
	List<MarkdownSection> sections = Lists.newArrayList();

	File editedFile;

	public void write(PrintWriter pw) {
		sections.forEach(it -> {
			it.write(pw);
		});
	}

	public List<MarkdownSection> getSections() {
		return sections;
	}

	public void write(File f) throws IOException {
		Preconditions.checkArgument(f != null, "file must be specified");
		try (FileWriter fw = new FileWriter(f)) {
			fw.write(writeAsString());
		}

	}

	public void write() throws IOException {

		write(editedFile);
	}

	public String writeAsString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		write(pw);
		pw.close();
		return sw.toString();
	}

	public class TableSection extends MarkdownSection {
		List<String> th = Lists.newArrayList();

		List<List<String>> rows = Lists.newArrayList();

		public TableSection header(int i, String name) {

			while (th.size() < i + 1) {
				th.add("");
			}
			th.set(i, name);

			return this;
		}

		public TableSection setValue(String key, String keyVal, String col, String val) {
			return setValue(key, keyVal, col, val, true);
		}

		public TableSection setValueIfNotSet(String key, String keyVal, String col, String val) {
			return setValue(key, keyVal, col, val, false);
		}

		protected TableSection setValue(String key, String keyVal, String col, String val, boolean unconditional) {

			int insertCol = th.indexOf(col);
			int colIdx = th.indexOf(key);

			for (int i = 0; i < rows.size(); i++) {
				List<String> row = rows.get(i);

				if (colIdx >= 0 && colIdx < row.size()) {
					String keyValCandidate = row.get(colIdx);
					if (keyValCandidate.replace("*", "").trim().equals(keyVal)) {
						while (row.size() < insertCol + 1) {
							row.add("");
						}

						if ((!unconditional) && Strings.isNullOrEmpty(row.get(insertCol))) {
							row.set(insertCol, val);
						} else if (unconditional) {
							row.set(insertCol, val);
						}

						return this;
					}
				}
			}

			List<String> row = Lists.newArrayList();
			while (row.size() < colIdx + 1) {
				row.add("");
			}
			if ((!unconditional) && Strings.isNullOrEmpty(row.get(insertCol))) {
				row.set(insertCol, val);
			} else if (unconditional) {
				row.set(insertCol, val);
			}
			rows.add(row);
			return this;
		}

		public TableSection cell(int row, int col, String val) {

			while (rows.size() < row + 1) {
				rows.add(Lists.newArrayList());
			}

			List<String> cells = rows.get(row);

			while (cells.size() < col + 1) {
				cells.add("");
			}
			cells.set(col, val);

			return this;

		}

		private String safeGet(List<String> list, int i) {
			if (i < 0 || i >= list.size()) {
				return null;
			} else {
				return list.get(i);
			}
		}

		private List<String> parseRow(String data) {
			data = data.trim();
			if (data.startsWith("|")) {
				data = data.substring(1);
			}
			if (data.endsWith("|")) {
				data = data.substring(0, data.length() - 1);
			}
			return Lists.newArrayList(Splitter.on("|").trimResults().splitToList(data));
		}

		public void parse() {

			if (lines.size() > 0) {
				String headerLine = lines.get(0).trim();
				if (headerLine.startsWith("|")) {
					headerLine = headerLine.substring(1);
				}
				if (headerLine.endsWith("|")) {
					headerLine = headerLine.substring(0, headerLine.length() - 1);
				}

				List<String> headers = parseRow(headerLine);
				th = headers;
			}
			for (int i = 2; i < lines.size(); i++) {
				List<String> row = parseRow(lines.get(i));
				rows.add(row);
			}

		}

		@Override
		public void write(PrintWriter pw) {

			int colCount = Math.max(0, th.size());
			for (List<String> row : rows) {
				colCount = Math.max(colCount, row.size());
			}
			if (rows.isEmpty()) {
				return;
			}
			lines.clear();

			StringBuffer sb = new StringBuffer();
			StringBuffer separator = new StringBuffer();
			for (int i = 0; i < colCount; i++) {
				if (i == 0) {
					sb.append("|");
					separator.append("|");
				}
				separator.append("------|");
				sb.append(" ");
				sb.append(Strings.nullToEmpty(safeGet(th, i)));

				sb.append(" |");
			}

			pw.println(removeDoubleSpace(sb.toString()));
			pw.println(separator.toString());

			for (int i = 0; i < rows.size(); i++) {
				List<String> row = rows.get(i);

				sb = new StringBuffer();
				for (int c = 0; c < colCount; c++) {
					if (c == 0) {
						sb.append("| ");

					}
					sb.append(" ");
					sb.append(Strings.nullToEmpty(safeGet(row, c)));
					sb.append(" |");

				}
				pw.println(removeDoubleSpace(sb.toString()));
			}

		}

	}

	public class MarkdownSection {

		public void parse() {

		}

		public void addLine(String line) {
			lines.add(line);
		}

		public Optional<Integer> getSectionLevel() {
			String line = lines.isEmpty() ? "" : lines.get(0);
			Pattern p = Pattern.compile(SECTION_START);
			Matcher m = p.matcher(line);
			if (m.matches()) {
				return Optional.of(m.group(1).length());
			}

			int idx = sections.indexOf(this);
			for (int i = idx - 1; i >= 0; i--) {
				MarkdownSection s = sections.get(i);
				if (s.getSectionLevel().isPresent()) {
					return s.getSectionLevel();
				}
			}
			return Optional.empty();
		}

		public Optional<String> getSectionName() {
			String line = lines.isEmpty() ? "" : lines.get(0);

			Matcher m = Pattern.compile(SECTION_START).matcher(line);
			if (m.matches()) {
				String val = Strings.emptyToNull(m.group(2).trim());
				return Optional.ofNullable(val);
			}
			return Optional.empty();
		}

		public String writeAsString() {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			write(pw);
			pw.close();
			return sw.toString();
		}

		public void write(PrintWriter pw) {
			lines.forEach(line -> {
				pw.println(line);
			});
		}

		List<String> lines = Lists.newArrayList();
	}

	protected String removeDoubleSpace(String input) {
		while (input.contains("  ")) {
			input = input.replace("  ", " ");
		}
		return input;
	}
	private List<MarkdownSection> parse(List<String> lines) {

		List<MarkdownSection> sections = Lists.newArrayList();
		Pattern startPattern = Pattern.compile(SECTION_START);
		Pattern tablePattern = Pattern.compile(Markdown.TABLE_PATTERN);
		MarkdownSection section = null;

		for (String line : lines) {

			Matcher m = startPattern.matcher(line);
			Matcher tableMatcher = tablePattern.matcher(line);

			if (tableMatcher.matches()) {
				if (section == null) {
					section = new TableSection();
					section.addLine(line);
					sections.add(section);
				}

				else if (section instanceof TableSection) {
					section.addLine(line);
				} else {
					section = new TableSection();
					section.addLine(line);
					sections.add(section);
				}
			} else if (m.matches()) {

				section = new MarkdownSection();
				sections.add(section);
				section.addLine(line);
			} else {
				if (section == null) {
					section = new MarkdownSection();
					sections.add(section);
					section.addLine(line);
				} else if (section instanceof TableSection) {
					section = new MarkdownSection();
					sections.add(section);
					section.addLine(line);
				} else {
					section.addLine(line);
				}
			}

		}
		for (MarkdownSection s : sections) {
			s.parse();
		}
		return sections;
	}

	public Markdown parse(String input) throws IOException {
		return parse(new StringReader(input));
	}

	public Markdown parse(Reader r) throws IOException {

		List<String> lines = CharStreams.readLines(r);

		this.sections = parse(lines);
		return this;
	}

	public Markdown parse(File f) throws IOException {

		try (FileReader fr = new FileReader(f)) {
			this.editedFile = f;
			Markdown x = parse(fr);

			return x;

		}

	}

	public Optional<MarkdownSection> findSection(String name) {
		for (MarkdownSection section : getSections()) {

			if (section.getSectionName().isPresent() && section.getSectionName().get().equals(name)) {
				return Optional.ofNullable(section);
			}
		}
		return Optional.empty();
	}
}
