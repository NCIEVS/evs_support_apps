package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.bean.*;
import gov.nih.nci.evs.restapi.common.*;
import java.io.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class SimpleReasoner {
	OWLScanner owlscanner = null;
	HierarchyHelper hh = null;
	HashMap roleMap = null;
	String assertedOWL = null;
	Vector parent_child_vec = null;
	Vector role_vec = null;
	Vector owl_vec = null;

	public SimpleReasoner(Vector owl_vec) {
		this.owl_vec = owl_vec;
		initialize();
	}

    public void initialize() {
		owlscanner = new OWLScanner(owl_vec);
		parent_child_vec = owlscanner.extractHierarchicalRelationships(owlscanner.get_owl_vec());
		hh = new HierarchyHelper(parent_child_vec);
		role_vec = owlscanner.extractOWLRestrictions(owlscanner.get_owl_vec());
		roleMap = createRoleMap(role_vec);
	}

	public OWLScanner getOWLScanner() {
		return owlscanner;
	}

	public HashMap getRoleMap() {
		return roleMap;
	}

	public Vector get_owl_vec() {
		return owl_vec;
	}

	public Vector get_parent_child_vec() {
		return parent_child_vec;
	}

    public HashMap createRoleMap(Vector v) {
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String srcCode = (String) u.elementAt(0);
			Vector w = new Vector();
			if (hmap.containsKey(srcCode)) {
				w = (Vector) hmap.get(srcCode);
			}
			String t = (String) u.elementAt(1) + "|" + (String) u.elementAt(2);
			w.add(t);
			hmap.put(srcCode, w);
		}
		return hmap;
	}

    public Vector get_ancestor_codes(String code) {
		Vector w = new Vector();
		Stack stack = new Stack();
		stack.push(code);
		HashSet hset = new HashSet();
		while (!stack.isEmpty()) {
			String next_code = (String) stack.pop();
			if (!hset.contains(next_code)) {
				hset.add(next_code);
				w.add(next_code);
			}
			Vector v = hh.getSuperclassCodes(next_code);
			if (v != null) {
				for (int i=0; i<v.size(); i++) {
					String child_code = (String) v.elementAt(i);
					stack.push(child_code);
				}
			}
		}
		hset.clear();
		return w;
	}

    public Vector get_roles(String code) {
		Vector roles = (Vector) roleMap.get(code);
		return roles;
	}


    public Vector get_ancestor_roles(String code) {
		Vector my_roles = (Vector) roleMap.get(code);
		Vector parent_codes = get_ancestor_codes(code);
		if (parent_codes == null || parent_codes.size() == 0) return new Vector();
		Vector w = new Vector();
		Stack stack = new Stack();
		stack.push(code);
		HashSet hset = new HashSet();

		while (!stack.isEmpty()) {
			String next_code = (String) stack.pop();
			if (next_code != null) {
				if (!hset.contains(next_code)) {
					hset.add(next_code);
					if (next_code.compareTo(code) != 0) {
						Vector roles = (Vector) roleMap.get(next_code);
						if (roles != null) {
							for (int k=0; k<roles.size(); k++) {
								String role = (String) roles.elementAt(k);
								if (!w.contains(role)) {
									w.add(role);
								}
							}
						}
					}
				}
				Vector v = hh.getSuperclassCodes(next_code);
				if (v != null) {
					for (int i=0; i<v.size(); i++) {
						String parent_code = (String) v.elementAt(i);
						stack.push(parent_code);
					}
				}
			}
		}
		hset.clear();
		return w;
	}

    public Vector get_ancestor_codes_and_roles(String code) {
		Vector my_roles = (Vector) roleMap.get(code);
		Vector parent_codes = get_ancestor_codes(code);
		if (parent_codes == null || parent_codes.size() == 0) return new Vector();
		Vector w = new Vector();
		Stack stack = new Stack();
		stack.push("@@|" + code);
		HashSet hset = new HashSet();
		while (!stack.isEmpty()) {
			String next_line = (String) stack.pop();
			Vector u = StringUtils.parseData(next_line, '|');
			String next_code = (String) u.elementAt(1);
			if (next_code != null) {
				if (!hset.contains(next_code)) {
					hset.add(next_code);
					if (next_code.compareTo(code) != 0) {
						Vector roles = (Vector) roleMap.get(next_code);
						if (roles != null) {
							for (int k=0; k<roles.size(); k++) {
								String role = (String) roles.elementAt(k);
								if (!w.contains(next_code + "|" + role)) {
									w.add(next_code + "|" + role);
								}
						    }
						}
					}
				}
				Vector v = hh.getSuperclassCodes(next_code);
				if (v != null) {
					for (int i=0; i<v.size(); i++) {
						String parent_code = (String) v.elementAt(i);
						stack.push(next_code + "|" + parent_code);
					}
				}
			}
		}
		hset.clear();
		return w;
	}

    public static Vector generateOWLRestrictionStmts(String roleCode, String targetCode) {
		Vector w = new Vector();
		w.add("        <rdfs:subClassOf>");
		w.add("            <owl:Restriction>");
		w.add("                <owl:onProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + roleCode + "\"/>");
		w.add("                <owl:someValuesFrom rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#" + targetCode + "\"/>");
		w.add("            </owl:Restriction>");
		w.add("        </rdfs:subClassOf>");
        return w;
	}

    public Vector generateOWLRestrictionStmts(Vector v) {
		Vector w0 = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String propCode = (String) u.elementAt(0);
			String targetCode = (String) u.elementAt(1);
			w0.addAll(generateOWLRestrictionStmts(propCode, targetCode));
		}
        return w0;
	}

    // In v1 but not in v2
	public static Vector v1_v2(Vector v1, Vector v2) {
		if (v2 == null || v2.size() == 0) {
			return v1;
		}
		if (v1 == null) return new Vector();
		Vector w = new Vector();
        HashSet hset = Utils.vector2HashSet(v2);
        for (int i=0; i<v1.size(); i++) {
			String t = (String) v1.elementAt(i);
			if (!hset.contains(t)) { // not in v2
				w.add(t);
			}
		}
		return new SortUtils().quickSort(w);
	}


	public static void main(String[] args) {
		String assertedOWL = args[0];
		Vector v = Utils.readFile(assertedOWL);
		SimpleReasoner reasoner = new SimpleReasoner(v);
		long ms = System.currentTimeMillis();
		String code = args[1];

		Vector w1 = reasoner.get_roles(code);
		Utils.dumpVector("get_roles", w1);

		//w1 = reasoner.get_ancestor_codes(code);
		//Utils.dumpVector("get_ancestor_codes", w1);

		Vector w = reasoner.get_ancestor_roles(code);
		Utils.dumpVector("get_ancestor_roles", w);

		w = v1_v2(w, w1);
		Utils.dumpVector("inherited roles", w);



		w = reasoner.generateOWLRestrictionStmts(w);

		Utils.dumpVector("get_ancestor_roles", w);
		System.out.println("\tTotal run time (ms): " + (System.currentTimeMillis() - ms));
	}

}