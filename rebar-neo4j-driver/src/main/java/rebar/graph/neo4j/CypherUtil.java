package rebar.graph.neo4j;

import com.google.common.base.Preconditions;

public class CypherUtil {

	
	public static void assertValidLabel(String label) {
		Preconditions.checkArgument(isValidLabel(label));
	}
	public static boolean isValidLabel(String label) {
		if (label==null || label.isEmpty()) {
			return false;
		}
		
		for (int c: label.toCharArray()) {
			if (Character.isLetterOrDigit(c)) {
				// ok
			}
			else if (c=='_') {
				// ok
			}
			else {
				return false;
			}
		}
		return true;
	}
	public static String escapePropertyName(String attribute) {
		Preconditions.checkArgument(!attribute.contains("`"), "illegal char: {}",attribute);
		return String.format("`%s`", attribute);
	}
	
}
