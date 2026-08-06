package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.util.*;

public class NCItMetadataUtils {
    static String SCRUBBED_PROPERTIES_FILE = "scrubbedProperties.txt";
    //static String NCIT_OWL = "ThesaurusInferred_forTS.owl";
    static String NAMESPACE = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";
    static String NAMESPACE_TARGET = "<!-- " + NAMESPACE;

/*
    public static HashMap generatePropertyHashMap() {
		return generatePropertyHashMap(NCIT_OWL);
	}

    public static HashMap generatePropertyHashMap(String owlfile) {
		File f = new File(owlfile);
		if (!f.exists()) {
			NCItDownload.download();
		}
		HashMap propCode2propLabelMap = new HashMap();
		OWLScanner scanner = new OWLScanner(owlfile);
		Vector supportedProperties = scanner.getSupportedProperties();
        for (int i=0; i<supportedProperties.size(); i++) {
			String line = (String) supportedProperties.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			propCode2propLabelMap.put((String) u.elementAt(1), (String) u.elementAt(0));
		}
        Vector objectProperties = scanner.extractObjectProperties(scanner.get_owl_vec());
        for (int i=0; i<objectProperties.size(); i++) {
			String line = (String) objectProperties.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			propCode2propLabelMap.put((String) u.elementAt(0), (String) u.elementAt(1));
		}
		Iterator it = propCode2propLabelMap.keySet().iterator();
		Vector keys = new Vector();
		while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new SortUtils().quickSort(keys);
		Vector w = new Vector();
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			w.add(key + "|" + (String) propCode2propLabelMap.get(key));
		}
		int n = owlfile.lastIndexOf(".");
		String propfile = "properties_" + owlfile.substring(0, n) + ".txt";
		Utils.saveToFile(propfile, w);
		return propCode2propLabelMap;
	}

	public static Vector generateScrubbedProperties(String assertedOWL) {
		return generateScrubbedProperties(assertedOWL, null);
	}

	public static Vector generateScrubbedProperties(String assertedOWL, String inferredOWL) {
		if (inferredOWL == null) {
			inferredOWL = NCIT_OWL;
		}
		HashMap oldMap = generatePropertyHashMap(assertedOWL);
		HashMap newMap = generatePropertyHashMap(inferredOWL);
		Vector w = diff(oldMap, newMap);
		w.add("default_on_create_class");
		w.add("default_on_edit_class");
		w.add("restricted_by");
		w.add("P379");
		w.add("P380");
		w.add("TVS_Location-enum");
		return new SortUtils().quickSort(w);
	}

	public static Vector diff(HashMap oldMap, HashMap newMap) {
		Vector w = new Vector();
		Iterator it = oldMap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (!newMap.containsKey(key)) {
				w.add(key);
			}
		}
		return w;
	}
*/

	public static Vector extractMetadata(Vector v) {
	    String target = "// Annotation properties";
	    int istart = TextFileExtractor.findLineNumber(v, target) - 3;
	    target = "// Classes";
	    int iend = TextFileExtractor.reverseFindLineNumber(v, target) - 3;
	    Vector w = TextFileExtractor.extractLines(v, istart, iend);
	    return w;
	}

    public static String extractPropertyId(String line) {
        String t = line;
        String id = null;
		int n1 = t.lastIndexOf("#");
		int n2 = t.lastIndexOf("\"");
		id = t.substring(n1+1, n2);
		return id;
	}

    public static String extractClassId(String line) {
        String t = line;
        String classId = null;
		if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
			int n = t.lastIndexOf("#");
			t = t.substring(n, t.length());
			n = t.lastIndexOf(" ");
			classId = t.substring(1, n);
		}
		return classId;
	}

    public static Vector filterProperties(Vector metadata_vec, Vector scrubbedProperties) {
		Vector w0 = new Vector();
		Vector w = new Vector();
		Vector scrubbed = new Vector();
		String id = null;
		int knt = 0;
		for (int i=0; i<metadata_vec.size(); i++) {
			String line = (String) metadata_vec.elementAt(i);
			String t = line;
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				knt++;
				if (knt == 1) {
					w0.addAll(w);
					w = new Vector();
				} else if (id != null) {
					if (!scrubbedProperties.contains(id)) {
						w0.addAll(w);
						w = new Vector();
					} else {
						scrubbed.add(id);
						w = new Vector();
					}
				} else {
					w = new Vector();
				}
				id = extractClassId(t);
				//System.out.println(id);
			}
			w.add(line);
		}
		if (!scrubbedProperties.contains(id)) {
			w0.addAll(w);
		} else {
			scrubbed.add(id);
		}
     	//Utils.dumpVector("scrubbed", scrubbed);
		return w0;
	}

    public static Vector searchPropertyIDs(Vector metadata_vec) {
		Vector ids = new Vector();
		String id = null;
		int knt = 0;
		for (int i=0; i<metadata_vec.size(); i++) {
			String line = (String) metadata_vec.elementAt(i);
			String t = line;
			if (t.indexOf(NAMESPACE_TARGET) != -1 && t.endsWith("-->")) {
				id = extractClassId(t);
				ids.add(id);
			}
		}
		return ids;
	}

	public static Vector getFilteredMetadata(String assertedOWL, Vector scrubbedProperties) {
		Vector w = extractMetadata(Utils.readFile(assertedOWL));
        w = filterProperties(w, scrubbedProperties);
        return w;
	}

/*
	public static void test(String[] args) {
		String assertedOWL = args[0];

		int n = assertedOWL.lastIndexOf(".");
		String outputfile = "metadata_" + assertedOWL.substring(0, n) + ".txt";
		Vector w = extractMetadata(Utils.readFile(assertedOWL));
		Vector asserted_propCodes = searchPropertyIDs(w);
		Utils.saveToFile(outputfile, w);
		Vector scrubbedProperties = Utils.readFile(SCRUBBED_PROPERTIES_FILE);
		Utils.dumpVector("scrubbedProperties", scrubbedProperties);
        w = filterProperties(w, scrubbedProperties);
        Utils.saveToFile("filtered_" + outputfile, w);

        Vector inferred_propCodes = searchPropertyIDs(w);
	    HashSet propCodeSet = Utils.vector2HashSet(inferred_propCodes);
        int scrubbed_count = 0;
        Vector v1 = new Vector();
        for (int i=0; i<asserted_propCodes.size(); i++) {
			String s = (String) asserted_propCodes.elementAt(i);
			if (!propCodeSet.contains(s)) {
				System.out.println("scrubbed: " + s);
				scrubbed_count++;
				v1.add(s);
			}
		}
		System.out.println("Numbe of observed scrubbed properties: " + scrubbed_count);
		System.out.println("Numbe of expected scrubbed properties: " + scrubbedProperties.size());
		if (scrubbed_count != scrubbedProperties.size()) {
			HashSet hset = Utils.vector2HashSet(scrubbedProperties);
			for (int k=0; k<v1.size(); k++) {
				String s = (String) v1.elementAt(k);
				if (!hset.contains(s)) {
					System.out.println("Missing property: " + s);
				}
			}
		}
	}

	public static void main(String[] args) {
		//test(args);
		String assertedOWL = args[0];
		Vector scrubbedProperties = Utils.readFile(SCRUBBED_PROPERTIES_FILE);
		Vector w = getFilteredMetadata(assertedOWL, scrubbedProperties);
		Utils.saveToFile("test_filtered_metadata.txt", w);

	}
	*/
}

//TVS_Location-enum