
package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import java.net.*;
import java.util.HashMap;
import java.util.*;

public class RootConcept {
    private final HashMap<String, Integer> associations = new HashMap<String, Integer>();
    private final Vector<String> definedDescendants = new Vector<String>();
    private final Vector<String> primitiveDescendants = new Vector<String>();
    private final HashMap<String, Integer> props = new HashMap<String, Integer>();
    private final HashMap<String, Integer> roles = new HashMap<String, Integer>();
    public Vector<String> disjoints;
    private Vector<String> descendantMap = new Vector<String>();
    private Integer descendantSize = 0;
    private Integer definedDescendantsSize = 0;
    private Integer primitiveDescedantsSize = 0;

    private String rootCode = null;
    OWLData owlData = null;
	HierarchyHelper hh = null;
	HashSet equivalentClassCodes = null;
    Vector role_vec = null;
    HashMap roleMap = null;
    Vector association_vec = null;
    HashMap associationMap = null;
    Vector property_vec = null;
    HashMap propertyMap = null;

    Vector nonHierRelationships = null;


	public RootConcept(OWLData owlData, String rootCode) {
        this.owlData = owlData;
        this.hh = owlData.getHierarchyHelper();
        this.rootCode = rootCode;
        initialize();

    }

    public String getRootCode() {
		return this.rootCode;
	}

    public void initialize() {

		equivalentClassCodes = owlData.getEquivalentClassCodes();
		System.out.println("equivalentClassCodes: " + equivalentClassCodes.size());

		roleMap = owlData.getRoleMap();
		System.out.println("roleMap: " + roleMap.keySet().size());

		associationMap = owlData.getAssociationMap();
		System.out.println("associationMap: " + associationMap.keySet().size());

		propertyMap = owlData.getPropertyMap();
		System.out.println("propertyMap: " + propertyMap.keySet().size());

		nonHierRelationships = owlData.getNonHierarchicalRelationships(rootCode);
		System.out.println("nonHierRelationships: " + nonHierRelationships.size());

        try {
            loadDescendantMap();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
	}

    public static int getMultiValuedRelMapCount(HashMap relMap, String srcCode) {
		int knt = 0;
		Iterator it = relMap.keySet().iterator();
		while (it.hasNext()) {
			String relCode = (String) it.next();
			HashMap hmap = (HashMap) relMap.get(relCode);
			if (hmap.containsKey(srcCode)) {
				Vector v = (Vector) hmap.get(srcCode);
				knt = knt + v.size();
			}
		}
		return knt;
	}

    public static HashMap createMultiValuedRelMap(Vector rel_vec) {
		HashMap relMap = null;
		relMap = new HashMap();
		for (int i=0; i<rel_vec.size(); i++) {
			String line = (String) rel_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String src_code = (String) u.elementAt(0);
			String asso_code = (String) u.elementAt(1);
			String target_code = (String) u.elementAt(2);
			HashMap hmap = new HashMap();
			if (relMap.containsKey(asso_code)) {
				hmap = (HashMap) relMap.get(asso_code);
			}
			Vector w = new Vector();
			if (hmap.containsKey(src_code)) {
				w = (Vector) hmap.get(src_code);
			}
			if (!w.contains(target_code)) {
				w.add(target_code);
			}
			hmap.put(src_code, w);
			relMap.put(asso_code, hmap);
		}
		return relMap;
	}

	public boolean isDefined(String code) {
		return equivalentClassCodes.contains(code);
	}


	public RootConcept(String rootCode, String name) {
        loadDescendantMap();
    }

    public Vector getAllDescendantCodes() {
        Vector v = hh.get_transitive_closure_v3(rootCode);
        return v;
 	}

    private void loadDescendantMap() {
        descendantMap = getAllDescendantCodes();
        System.out.println("descendantMap: " + descendantMap.size());

        descendantSize = descendantMap.size();
        System.out.println("descendantSize: " + descendantSize);

        loadDefinedDescendants();
    }

    private void loadDefinedDescendants() {
        for (String code : descendantMap) {
			if (isDefined(code)) {
				definedDescendants.add(code);
			} else {
				primitiveDescendants.add(code);
			}
        }
        definedDescendantsSize = definedDescendants.size();
        primitiveDescedantsSize = primitiveDescendants.size();
    }



    public Integer getDefinedDescendantSize() {
        return definedDescendantsSize;
    }

    public Vector<String> getDefinedDescendants() {
        return definedDescendants;
    }

    public HashMap<String, Integer> getDescendantAssociationsCount() {
        if (associations.size() > 0) {
            return associations;
        }
        loadDescendantAssociations();
        return associations;
    }

    private void loadDescendantAssociations() {
        try {
            for (String code : descendantMap) {
                int knt1 = getMultiValuedRelMapCount(roleMap, code);
                int knt2 = getMultiValuedRelMapCount(associationMap, code);
                associations.put(code, Integer.valueOf(knt1 + knt2));
            }
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public final Vector<String> getDescendantMap() {
        if (descendantMap != null && descendantMap.size() > 0) {
            return descendantMap;
        }
        loadDescendantMap();
        return descendantMap;
    }

    public Integer getDescendantMapSize() {
        return descendantSize;
    }

    public HashMap<String, Integer> getDescendantPropertiesCount() {
        if (props.size() > 0) {
            return props;
        }
        loadDescendantProperties();
        return props;
    }

    private void loadDescendantProperties() {
        try {
            for (String code : descendantMap) {
				int knt = getMultiValuedRelMapCount(propertyMap, code);
                props.put(code, Integer.valueOf(knt));
            }
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Integer> getDescendantRolesCount() {
        if (roles.size() > 0) {
            return roles;
        }
        loadDescendantRoles();
        return roles;
    }

    private void loadDescendantRoles() {
        try {
            for (String code : descendantMap) {
				int count = getMultiValuedRelMapCount(roleMap, code);
                if (isDefined(code)) {
					count++;
				}
                roles.put(code, Integer.valueOf(count));
            }
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public Vector<String> getPrimitiveDescendants() {
        return primitiveDescendants;

    }

    public Integer getPrimitiveDescendantsSize() {
        return primitiveDescedantsSize;
    }

    public boolean isDescendant(String conceptCode) {
        return descendantMap.contains(conceptCode);
    }

    private void loadPrimitiveDescendants() {
		for (String code : descendantMap) {
			int count = 0;
			if (!isDefined(code)) {
				primitiveDescendants.add(code);
			}
		}
    }

    public Vector getNonHierarchicalRelationships() {
		Vector descendants = hh.get_transitive_closure_v3(rootCode);
		Vector w = new Vector();
		for (int i=0; i<descendants.size(); i++) {
			String descedant = (String) descendants.elementAt(i);

			Vector roles = (Vector) roleMap.get(descedant);
			if (roles != null) {
				w.addAll(roles);
			}
			Vector associations = (Vector) associationMap.get(descedant);
			if (associations != null) {
				w.addAll(associations);
			}
		}
		return w;
	}



	public Vector getAssociations() {
		return nonHierRelationships;
	}

    public Vector getProperties() {
		Vector descendants = hh.get_transitive_closure_v3(rootCode);
		Vector w = new Vector();
		Iterator it = propertyMap.keySet().iterator();
		while (it.hasNext()) {
			String propCode = (String) it.next();
			HashMap hmap = (HashMap) propertyMap.get(propCode);
			for (int i=0; i<descendants.size(); i++) {
				String descedant = (String) descendants.elementAt(i);
				Vector v = (Vector) hmap.get(descedant);
				if (v != null) {
					w.addAll(v);
				}
			}
		}
        return w;
	}

    public Vector getRoles() {
		Vector descendants = hh.get_transitive_closure_v3(rootCode);
		Vector w = new Vector();
		Iterator it = roleMap.keySet().iterator();
		while (it.hasNext()) {
			String roleCode = (String) it.next();
			HashMap hmap = (HashMap) roleMap.get(roleCode);
			for (int i=0; i<descendants.size(); i++) {
				String descedant = (String) descendants.elementAt(i);
				Vector v = (Vector) hmap.get(descedant);
				if (v != null) {
					w.addAll(v);
				}
			}
		}
        return w;
	}

}
