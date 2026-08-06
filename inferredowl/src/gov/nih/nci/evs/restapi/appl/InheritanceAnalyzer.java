package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;
import java.io.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.nio.file.attribute.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;

public class InheritanceAnalyzer {
    static String NAMESPACE = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";
    static String NAMESPACE_TARGET = "<!-- " + NAMESPACE;
    public static String SUBCLASSOF = "subClassOf";

    static String[] RELATIONSHIP_KEYS = new String[] {"CECI", "CECICI", "subClassOf"};
    static List RELATIONSHIP_KEY_LIST = Arrays.asList(RELATIONSHIP_KEYS);

    static String owlfile = null;
    Vector parent_child_vec = null;
	OWLClassLoader loader = null;
	HashMap classDataHashMap = null;
	Vector classIdVec = null;
	static HashSet equiv_class_set = null;
	HierarchyHelper hh = null;
	SimpleReasoner reasoner = null;
	HashMap roleMap = null;

	static HashMap MATCH_PATTERN = null;
	Vector owl_vec = null;
	OWLScanner owlscanner = null;

	static {
	    MATCH_PATTERN = new HashMap();
		MATCH_PATTERN.put(SUBCLASSOF, Integer.valueOf(1));
        MATCH_PATTERN.put("CECICI", Integer.valueOf(2));
	}

	public InheritanceAnalyzer(String owlfile) {
		this.owlfile = owlfile;
		initialize();
	}

	public void initialize() {
		long ms = System.currentTimeMillis();
        this.owl_vec = Utils.readFile(owlfile);

		reasoner = new SimpleReasoner(this.owl_vec);
		roleMap = reasoner.getRoleMap();

		owlscanner = reasoner.getOWLScanner();
		parent_child_vec = owlscanner.extractHierarchicalRelationships(owlscanner.get_owl_vec());
		parent_child_vec = HTMLDecoder.run(parent_child_vec);
		hh = new HierarchyHelper(parent_child_vec);
		Vector equiv_classes = owlscanner.extractEquivalenceClasses();
		equiv_classes = DelimitedDataExtractor.extract(equiv_classes, "0", '|');
		equiv_class_set = Utils.vector2HashSet(equiv_classes);
		loader = new OWLClassLoader(owlfile);
		classDataHashMap = loader.getClassDataHashMap();
		classIdVec = loader.getClassIdVec();
		reasoner.get_owl_vec().clear();
        System.out.println("Total InheritanceAnalyzer initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}
	public static void dumpIntegerValuedHashMap(HashMap hmap) {
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			Integer value = (Integer) hmap.get(key);
			System.out.println(key + " --> " + value.intValue());
		}
	}

	public SimpleReasoner getSimpleReasoner() {
		return reasoner;
	}

	public Vector get_parent_child_vec() {
		return parent_child_vec;
	}

	public void clear() {
		parent_child_vec.clear();
		classDataHashMap.clear();
		classIdVec.clear();
		equiv_class_set.clear();
		roleMap.clear();
	}

    public void deleteTemporaryFiles() {
		FileUtils.deleteFile("classIds.txt");
	}


    public Vector get_ancestor_codes_and_roles(String code) {
		Vector ancestors = getAncestorCodes(code);
		return get_ancestor_codes_and_roles(ancestors);
	}

    public Vector get_ancestor_codes_and_roles(Vector ancestors) {
		Vector w = new Vector();
		for (int i=0; i<ancestors.size(); i++) {
			String ancestor = (String) ancestors.elementAt(i);
			Vector roles = reasoner.get_ancestor_codes_and_roles(ancestor);
			for (int j=0; j<roles.size(); j++) {
				String role = (String) roles.elementAt(j);
				if (!w.contains(role)) {
					w.add(role);
				}
			}
		}
		return w;
	}

    public Vector get_ancestor_roles(String code) {
		Vector ancestors = getAncestorCodes(code);
		Vector w = new Vector();
		for (int i=0; i<ancestors.size(); i++) {
			String ancestor = (String) ancestors.elementAt(i);
			Vector roles = reasoner.get_ancestor_roles(ancestor);
			for (int j=0; j<roles.size(); j++) {
				String role = (String) roles.elementAt(j);
				if (!w.contains(role)) {
					w.add(role);
				}
			}
		}
		return w;
	}

	public static String getClassId(String line, int m) {
		int n = line.lastIndexOf("#C");
		if (n != -1) {
			String s = line.substring(n+1, line.length()-m);
			s = s.trim();
			return s;
		}
		return null;
	}

	public void printPathToRoots(String code) {
		System.out.println("printPathToRoots " + code);
		long ms = System.currentTimeMillis();
		boolean traverseDown = false;
		Vector ancestors = hh.getTransitiveClosure(code, traverseDown);
		Utils.dumpVector("ancestors", ancestors);
		System.out.println("HierarchyHelper.printPathToRoots " + code);
		HierarchyHelper.printPathToRoots(parent_child_vec, code, null) ;
		System.out.println("\tTotal run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public Vector getClassVec(String code) {
		return (Vector) classDataHashMap.get(code);
	}

    public static Vector getNodesOnPaths(Vector paths, int pos) {
        Vector w = new Vector();
		for (int i=0; i<paths.size(); i++) {
			String path = (String) paths.elementAt(i);
			Vector u = StringUtils.parseData(path, '|');
			if (u.size() > pos) {
				String code = (String) u.elementAt(pos);
				w.add(code);
			}
		}
        return w;
	}

	public Vector getSubclassCodes(String code) {
		return hh.getSubclassCodes(code);
	}

    public Vector getRoles(String code) {
		return (Vector) roleMap.get(code);
	}

	public Vector getPaths2Roots(String code) {
		return hh.getPaths2Roots(code);
	}

	public Vector get_transitive_closure_v4(String code) {
		return hh.get_transitive_closure_v4(code);
	}

	public Vector get_transitive_closure_v3(String code) {
		return hh.get_transitive_closure_v3(code);
	}

	public Vector getSuperclassCodes(String code) {
		return hh.getSuperclassCodes(code);
	}

    public Vector getAncestorCodes(String code) {
		boolean traverseDown = false;
		Vector v = hh.getTransitiveClosure(code, traverseDown);
		String removed = (String) v.remove(0);
		return v;
	}

	static boolean isDefined(String code) {
		return equiv_class_set.contains(code);
	}

    public String extractClassId(String t) {
		int n = t.lastIndexOf("#");
		t = t.substring(n, t.length());
		n = t.lastIndexOf(" ");
		String classId = t.substring(1, n);
		return classId;
	}

    public HashMap getRelationshipHashMap(String code) {
		Vector w = extractRelationships(code);
		HashMap hmap = createRelationshipHashMap(w);
		return hmap;
	}

	public Vector extractRelationships(String code) {
        Vector class_vec = (Vector) classDataHashMap.get(code);
        return ScannerUtils.extractRelationships(class_vec);
	}

	public Vector extractClassData(String code) {
        Vector class_vec = (Vector) classDataHashMap.get(code);
        return ScannerUtils.extractClassData(class_vec);
	}

    public void dumpHashMapValueSize(HashMap hmap) {
		Iterator it = hmap.keySet().iterator();
		Vector keys = new Vector();
		while (it.hasNext()) {
			String key = (String) it.next();
			keys.add(key);
		}
		keys = new SortUtils().quickSort(keys);
		for (int i=0; i<keys.size(); i++) {
			String key = (String) keys.elementAt(i);
			Vector w = (Vector) hmap.get(key);
			System.out.println(key + ": " + w.size());
		}
	}

    public Vector getHashMapValueSize(HashMap hmap) {
		Vector w = new Vector();
		Iterator it = hmap.keySet().iterator();
		Vector keys = new Vector();
		while (it.hasNext()) {
			String key = (String) it.next();
			Vector v = (Vector) hmap.get(key);
			w.add(key + ": " + v.size());
		}
		return w;
	}

	public static HashMap createRelationshipHashMap(Vector v) {
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String rel = (String) u.elementAt(1);
			Vector w = new Vector();
			if (rel.indexOf(SUBCLASSOF) != -1) {
				if (hmap.containsKey(rel)) {
					w = (Vector) hmap.get(rel);
			    }
			    w.add((String) u.elementAt(2));
			    hmap.put(rel, w);
			} else {
				u = StringUtils.parseData(line, ' ');
				String pathId = (String) u.elementAt(1);
				w = new Vector();
				if (hmap.containsKey(pathId)) {
					w = (Vector) hmap.get(pathId);
			    }
			    w.add((String) u.elementAt(0));
			    hmap.put(pathId, w);
			}
		}
	    return hmap;
	}

	public HashMap getAncestorRelationships(String code) {
		Vector ancestors = getAncestorCodes(code);
		return mergeRelationships(ancestors);
	}

	public HashMap mergeRelationships(Vector codes) {
		HashMap map = new HashMap();
        for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			HashMap hmap = getRelationshipHashMap(code);
			Iterator it = hmap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				Vector w0 = new Vector();
				if (map.containsKey(key)) {
					w0 = (Vector) map.get(key);
				}
				w0.addAll((Vector) hmap.get(key));
				map.put(key, w0);
			}
		}
        return map;
	}

	public HashMap getCode2RelationshipHashMap(Vector codes) {
		HashMap map = new HashMap();
        for (int i=0; i<codes.size(); i++) {
			String code = (String) codes.elementAt(i);
			HashMap hmap = getRelationshipHashMap(code);
			map.put(code, hmap);
		}
		return map;
	}

	public Vector getMissingRelationships(String code) {
        HashMap map = getAncestorRelationships(code);
        HashMap hmap = getRelationshipHashMap(code);
        return compareRelationshipHashMaps(map, hmap);
	}

    //hmap1: inferred
    //hmap2: asserted
	public static Vector compareRelationshipHashMaps(HashMap hmap1, HashMap hmap2) {
        Vector w = new Vector();
        Iterator it = hmap1.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			Vector values = (Vector) hmap1.get(key);
			if (hmap2.containsKey(key)) {
				Vector hmap2_values = (Vector) hmap2.get(key);
				HashSet hset2 = Utils.vector2HashSet(hmap2_values);
				for (int i=0; i<values.size(); i++) {
					String value = (String) values.elementAt(i);
					if (!hset2.contains(value)) {
						w.add(key + "\t" + value);
					}
				}
			} else {
				for (int i=0; i<values.size(); i++) {
					String value = (String) values.elementAt(i);
					w.add(key + "\t" + value);
				}
			}
		}
		return w;
	}

    public Vector matchAncestorRelationships() {
		return matchAncestorRelationships(this.classIdVec);
	}

	public boolean debugger = false;
	public void setDebugger(boolean bool) {
		debugger = bool;
	}

    public Vector matchAncestorRelationships(Vector codes) {
		if (debugger) {
			dumpIntegerValuedHashMap(MATCH_PATTERN);
		}
		Vector w = new Vector();
		int lcv = 1;
		int total = codes.size();
		int increment = 10;
		if (total > 2000) {
		    increment = 2000;
		}
		for (int i=0; i<codes.size(); i++) {
			int j = i+1;
			if (lcv == increment) {
				if (debugger) System.out.println("" + j + " out of " + total + " completed.");
				lcv = 0;
			}
			lcv++;
			String code = (String) codes.elementAt(i);
			int k = i+1;
			if (isDefined(code)) {
				Vector ancestors = getAncestorCodes(code);
				for (int k2=0; k2<ancestors.size(); k2++) {
					String ancestor = (String) ancestors.elementAt(k2);
					HashMap sup_hmap = createRelationshipHashMap(ancestor);
					boolean matched = matchRelationshipConditions(sup_hmap);
					if (matched) {
						w.add(code + "|" + ancestor);
						break;
					}
				}
			}
		}
		if (debugger) {
			System.out.println("" + total + " out of " + total + " completed.");
		}
		return w;
	}

    public Vector traceRelationships(HashMap hmap) {
		Vector w = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = (String) hmap.get(key);
			w.add(key + "|" + value);
		}
		return traceRelationships(w);
	}

    public Vector traceRelationships(Vector v) {
		Vector w0 = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			String ancestor = (String) u.elementAt(1);
			w0.addAll(traceRelationships(code, ancestor));
		}
		return w0;
	}

    public Vector traceRelationships(String code, String ancestor) {
		Vector w = new Vector();
		Vector sup_class_vec = (Vector) classDataHashMap.get(ancestor);
		w.add("\nAsserted " + ancestor + ":" );
		w.addAll(sup_class_vec);
		w.add("\nAnonymous class to be inherited by it's subclasses: ");
		HashMap sup_hmap = createRelationshipHashMap(ancestor);
		w.addAll(generateInheritedSubClassOfStmts(sup_hmap));
		w.add("\nAsserted " + code + ":" );
		Vector class_vec = (Vector) classDataHashMap.get(code);
		w.addAll(class_vec);
		w.add("\nInferred " + code + ":" );
		Vector inferredOWL = composeInferredOWLClass(code, sup_hmap);
		w.addAll(inferredOWL);
		return w;
	}

	public boolean matchRelationshipConditions(HashMap hmap) {
		return matchRelationshipConditions(MATCH_PATTERN, hmap);
	}

	public boolean matchRelationshipConditions(HashMap map, HashMap hmap) {
		if (map.keySet().size() != hmap.keySet().size()) {
			return false;
		}
		Iterator it = map.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (!hmap.containsKey(key)) {
				return false;
			} else {
				Integer int_obj = (Integer) map.get(key);
				int count = int_obj.intValue();
				Vector values = (Vector) hmap.get(key);
				if (key.compareTo(SUBCLASSOF) == 0) {
					if (values.size() != count) {
						return false;
					}
				} else {
					if (values.size() < count) {
						return false;
					}
				}
			}
		}
		return true;
	}

    public Vector generateInheritedSubClassOfStmts(HashMap hmap) {
		Vector sups = (Vector) hmap.get(SUBCLASSOF);
		Vector roles = (Vector) hmap.get("CECICI");
		Vector w = new Vector();
w.add("        <rdfs:subClassOf>");
w.add("            <owl:Class>");
w.add("                <owl:intersectionOf rdf:parseType=\"Collection\">");
for (int i=0; i<sups.size(); i++) {
	String sup = (String) sups.elementAt(i);
w.add("                    <rdf:Description rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + sup + "\"/>");
}
w.add("                    <owl:Class>");
w.add("                        <owl:intersectionOf rdf:parseType=\"Collection\">");
        if (roles != null) {
			for (int j=0; j<roles.size(); j++) {
				String line = (String) roles.elementAt(j);
				Vector u = StringUtils.parseData(line, '|');
				String roleCode = (String) u.elementAt(0);
				String targetCode = (String) u.elementAt(1);
	w.add("                            <owl:Restriction>");
	w.add("                                <owl:onProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + roleCode + "\"/>");
	w.add("                                <owl:someValuesFrom rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + targetCode + "\"/>");
	w.add("                            </owl:Restriction>");
			}
	    }
w.add("                        </owl:intersectionOf>");
w.add("                    </owl:Class>");
w.add("                </owl:intersectionOf>");
w.add("            </owl:Class>");
w.add("        </rdfs:subClassOf>");
         return w;
	}

    public HashMap createRelationshipHashMap(String code) {
		Vector class_vec = (Vector) classDataHashMap.get(code);
		Vector v = ScannerUtils.extractRelationships(class_vec);
		return createRelationshipHashMap(v);
	}

    public Vector composeInferredOWLClass(String code, String ancestor) {
		HashMap sup_hmap = createRelationshipHashMap(ancestor);
		return composeInferredOWLClass(code, sup_hmap);
	}

    public Vector composeInferredOWLClass(String code, HashMap hmap) {
		Vector class_vec = (Vector) classDataHashMap.get(code);
		Vector w = new Vector();
	    String target = "</owl:equivalentClass>";
	    int iend = TextFileExtractor.reverseFindLineNumber(class_vec, target);

	    for (int i=0; i<=iend; i++) {
			String line = (String) class_vec.elementAt(i);
			w.add(line);
		}
		Vector stmts = generateInheritedSubClassOfStmts(hmap);
	    for (int i=0; i<stmts.size(); i++) {
			String line = (String) stmts.elementAt(i);
			w.add(line);
		}
	    for (int i=iend+1; i<class_vec.size(); i++) {
			String line = (String) class_vec.elementAt(i);
			w.add(line);
		}
		return w;
	}

/*
	public static HashMap getFrequencyHashMap(Vector v) {
		HashMap countMap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String node = (String) v.elementAt(i);

			Integer int_obj = Integer.valueOf(0);
			if (countMap.containsKey(node)) {
				int_obj = (Integer) countMap.get(node);
			}
			int count = int_obj.intValue();
			int_obj = Integer.valueOf(count+1);
			countMap.put(node, int_obj);
		}
        return countMap;
	}
*/

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String owlfile = args[0];
		System.out.println("OWL File: " + owlfile);
		InheritanceAnalyzer analyzer = new InheritanceAnalyzer(owlfile);

        Vector w = analyzer.matchAncestorRelationships();
        Utils.saveToFile("analyzer_results.txt", w);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

