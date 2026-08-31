package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;


public class SummaryObject {
	OWLData owlData = null;
    private HashMap<String, String> conceptAndDef = new HashMap<String, String>();
    private HashMap<String, Vector<String>> conceptAndSemanticTypes = new HashMap<String, Vector<String>>();

    HashMap definitionMap = null;

    private final Integer conceptCount;
    private HashMap<String, Integer> conceptCountsPerKind = new HashMap<String, Integer>();
    private final HashMap<String, Vector<String>> conceptsPerKind = new HashMap<String, Vector<String>>();
    private final HashMap<String, Vector<String>> parentCodeMap = new HashMap<String, Vector<String>>();
    private final HashMap<String, RootConcept> rootMap = new HashMap<String, RootConcept>();
    Vector<String> conceptCodes;
    HierarchyHelper hh = null;
    HashMap roleMap = null;
    HashMap associationMap = null;
    HashMap propertyMap = null;
    HashMap preferredNameMap = null;

    private Set<String> namespaces;

    Vector property_vec = null;
    Vector association_vec = null;
    Vector role_vec = null;
    Vector roots = null;

	HashMap objectPropertyCode2NameMap = null;
	HashMap objectValuedAnnotationPropertyCode2NameMap = null;
	HashMap stringValuedAnnotationPropertyCode2NameMap = null;



	public SummaryObject(OWLData owlData) {
		this.owlData = owlData;
		hh = owlData.getHierarchyHelper();
        initialize();

        String ontologyNamespace = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

        loadPropertyClasses();
        loadConceptClasses();
        conceptCount = conceptCodes.size();
        System.out.println("Number of concepts: " + conceptCount.toString());
    }

    public int getRootCount() {
		return roots.size();
	}

    public void initialize() {
		roots = owlData.getRoots();
		propertyMap = owlData.getPropertyMap();
		associationMap = owlData.getAssociationMap();
		roleMap = owlData.getRoleMap();

		definitionMap = owlData.getDefinitionMap();
		preferredNameMap = owlData.getPreferredNameMap();

	    objectPropertyCode2NameMap = owlData.getObjectPropertyCode2NameMap();
	    objectValuedAnnotationPropertyCode2NameMap = owlData.getObjectValuedAnnotationPropertyCode2NameMap();
	    stringValuedAnnotationPropertyCode2NameMap = owlData.getStringValuedAnnotationPropertyCode2NameMap();
	}

	static String DEFINITION_CODE = "P97";
	static String PREFERRED_NAME_CODE = "P108";

	public HashMap createdefinitionMap() {
		return (HashMap) propertyMap.get(DEFINITION_CODE);
	}

	public HashMap createPreferredNameMap() {
		return (HashMap) propertyMap.get(PREFERRED_NAME_CODE);
	}

    private void loadPropertyClasses() {
        loadRootConcepts();
    }

    private void loadConceptClasses() {
        loadConceptCodes();
        loadConceptAndDef();
        loadSemanticTypes();
     }


    private void loadRootConcepts() {
        System.out
                .println("Search vocabulary for root concepts and load descendants");

        Vector<String> rootConceptCodes = (Vector) roots.clone();//owlApi.getRootConceptCodes();
        for (String rootCode : rootConceptCodes) {
            RootConcept root = new RootConcept(owlData, rootCode);
            rootMap.put(rootCode, root);
        }
        System.out.println("Finished loading root classes");
    }

    private void loadConceptCodes() {

        conceptCodes = owlData.getConceptCodes();

        loadConceptParents();
        loadPreferredName();
    }

    private void loadConceptAndDef() {
		conceptAndDef = new HashMap<>(definitionMap);

    }

    static String SEMANTIC_TYPE_CODE = "P106";
    private void loadSemanticTypes() {
		conceptAndSemanticTypes = (HashMap) propertyMap.get(SEMANTIC_TYPE_CODE);

    }

    private void loadConceptParents() {
        try {
            for (String conceptCode : conceptCodes) {
                Vector parentCodes = hh.getSuperclassCodes(conceptCode);
                if (parentCodes != null) {
					parentCodeMap.put(conceptCode, parentCodes);
				}
            }
            System.out.println("Finished loading concept parents");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPreferredName() {
    }

    public String getDefinition(String code) {
		return (String) definitionMap.get(code);
	}


    public String getPreferredName(String code) {
		return (String) preferredNameMap.get(code);
	}


    public void doMetrics() {
        Metrics metrics = new Metrics(owlData);
    }

    public final Vector<String> getAllConceptCodes() {


        return conceptCodes;
    }

    public HashMap<String, String> getAssociationMap() {
        return associationMap;
    }

    public final HashMap<String, HashMap<String, Integer>> getAssociationCountPerKind() {
        HashMap<String, HashMap<String, Integer>> associationsPerKind = new HashMap<String, HashMap<String, Integer>>();
        Set<String> set = rootMap.keySet();
        HashMap associationCode2CountMap = new HashMap();
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            Vector decendants = hh.get_transitive_closure_v3(root.getRootCode());
            HashMap hmap = new HashMap();
            Iterator it = associationMap.keySet().iterator();
            while (it.hasNext()) {
				String associationCode = (String) it.next();
				int count = 0;
				HashMap hmap2 = (HashMap) associationMap.get(associationCode);
				Iterator it2 = hmap2.keySet().iterator();
				while (it.hasNext()) {
					 String srcCode = (String) it.next();
					 if (decendants.contains(srcCode)) {
					 	Vector values = (Vector) hmap2.get(srcCode);
					 	if (values != null) {
							count = count + values.size();
						}
					}
				}
				associationCode2CountMap.put(associationCode, Integer.valueOf(count));
			}
			associationsPerKind.put(root.getRootCode(), associationCode2CountMap);
		}
        return associationsPerKind;
    }

    public final HashMap<String, Vector> getAssociationsPerKind() {
        HashMap<String, Vector> associationsPerKind = new HashMap<String, Vector>();
        Set<String> set = rootMap.keySet();
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            if(root!=null) {
                associationsPerKind.put(key, root.getAssociations());
            }
        }
        return associationsPerKind;
    }

    public final HashMap<String, String> getConceptAndDef() {
        return conceptAndDef;
    }

    public final HashMap<String, String> getConceptAndPreferredName() {


        return preferredNameMap;
    }

    public final Integer getConceptCount() {
        return conceptCount;
    }


    public final HashMap<String, Integer> getConceptCountsPerKind() {
		conceptCountsPerKind = new HashMap();
		for (int i=0; i<roots.size(); i++) {
			String root = (String) roots.elementAt(i);
			Vector decendants = hh.get_transitive_closure_v3(root);
			conceptCountsPerKind.put(root, decendants.size());
		}
        return conceptCountsPerKind;
    }

    public Vector<String> getConceptParents(String conceptCode) {
        return hh.getSuperclassCodes(conceptCode);
    }

    public final HashMap<String, Vector<String>> getConceptsPerKind() {
        if (conceptsPerKind.size() < 1) {

            Set<String> set = rootMap.keySet();
            for (String key : set) {
                RootConcept root = rootMap.get(key);
                if(root!=null) {
                    Vector<String> descendantCodes = root.getAllDescendantCodes();
                    conceptsPerKind.put(key, descendantCodes);
                }
            }
        }
        return conceptsPerKind;
    }

    public final HashMap<String, Integer> getDefinedConceptCountsPerKind() {
        HashMap<String, Integer> definedConceptCountsPerKind = new HashMap<String, Integer>();
        Set<String> set = rootMap.keySet();
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            definedConceptCountsPerKind.put(key, root.getDefinedDescendantSize());
        }

        return definedConceptCountsPerKind;
    }

    public final Set<String> getNamespaces() {
        return namespaces;
    }


    public final HashMap<String, Integer> getPrimitiveConceptCountsPerKind() {
        HashMap<String, Integer> primitiveConceptCountsPerKind = new HashMap<String, Integer>();
        Set<String> set = rootMap.keySet();
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            if((root!=null) && root.getPrimitiveDescendants()!=null) {
                primitiveConceptCountsPerKind.put(key, root
                        .getPrimitiveDescendants().size());
            }
        }

        return primitiveConceptCountsPerKind;
    }

    public final HashMap<String, Vector> getPropertiesPerKind() {
        HashMap<String, Vector> propertiesPerKind = new HashMap<String, Vector>();
        Set<String> set = rootMap.keySet();
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            propertiesPerKind.put(key, root.getProperties());
        }

        return propertiesPerKind;
    }

    public final HashMap<String, HashMap<String, Integer>> getPropertyCountPerKind() {
        HashMap<String, HashMap<String, Integer>> propertiesPerKind = new HashMap<String, HashMap<String, Integer>>();
        Set<String> set = rootMap.keySet();


        HashMap code2PropertyCountMap = owlData.getCode2PropertyCountMap();

        HashMap rootPropertyCountMap = new HashMap();
        int knt= 0;
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            knt++;
            Vector descendants = hh.get_transitive_closure_v3(root.getRootCode());

            Iterator it = stringValuedAnnotationPropertyCode2NameMap.keySet().iterator();
            while (it.hasNext()) {
				String propCode = (String) it.next();
				int total = 0;
				HashMap countMap = new HashMap();
				for (int i=0; i<descendants.size(); i++) {
					String descendant = (String) descendants.elementAt(i);
					HashMap propertyCountMap = (HashMap) code2PropertyCountMap.get(descendant);
					rootPropertyCountMap = mergePropertyCountMap(rootPropertyCountMap, propertyCountMap);
				}
			}
			propertiesPerKind.put(root.getRootCode(), rootPropertyCountMap);
		}
        return propertiesPerKind;
    }

    public HashMap mergePropertyCountMap(HashMap rootMap, HashMap hmap) {
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String propCode = (String) it.next();
			Integer int_obj = (Integer) hmap.get(propCode);
			int knt = int_obj.intValue();
			int total = 0;
			if (rootMap.containsKey(propCode)) {
				Integer int_obj_0 = (Integer) rootMap.get(propCode);
				total = int_obj_0.intValue();
			}
			total = total + knt;
			rootMap.put(propCode, total);
		}
		return rootMap;
	}

    public final HashMap<String, String> getPropertyMap() {
        return propertyMap;
    }




    public HashMap<String, HashMap<String, Integer>> getRoleCountPerKind() {
        HashMap<String, HashMap<String, Integer>> rolesPerKind = new HashMap<String, HashMap<String, Integer>>();
        Set<String> set = rootMap.keySet();
        HashMap roleCode2CountMap = new HashMap();
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            Vector decendants = hh.get_transitive_closure_v3(root.getRootCode());
            HashMap hmap = new HashMap();
            Iterator it = roleMap.keySet().iterator();
            while (it.hasNext()) {
				String roleCode = (String) it.next();
				int count = 0;
				HashMap hmap2 = (HashMap) roleMap.get(roleCode);
				Iterator it2 = hmap2.keySet().iterator();
				while (it.hasNext()) {
					 String srcCode = (String) it.next();
					 if (decendants.contains(srcCode)) {
					 	Vector values = (Vector) hmap2.get(srcCode);
					 	if (values != null) {
							count = count + values.size();
						}
					}
				}
				roleCode2CountMap.put(roleCode, Integer.valueOf(count));
			}
			rolesPerKind.put(root.getRootCode(), roleCode2CountMap);
		}
        return rolesPerKind;
    }

    public final HashMap<String, String> getRoleMap() {
        return roleMap;
    }

    public final HashMap<String, Vector> getRolesPerKind() {
        HashMap<String, Vector> rolesPerKind = new HashMap<String, Vector>();
        Set<String> set = rootMap.keySet();
        for (String key : set) {
            RootConcept root = rootMap.get(key);
            rolesPerKind.put(key, root.getRoles());
        }
        return rolesPerKind;
    }

    public final Set<String> getRootConceptNames() {
        return rootMap.keySet();

    }

    public String getRootForConcept(String conceptCode) {
        for (String rootCode : this.getRootMap().keySet()) {
            if (rootMap.get(rootCode).getDescendantMap().contains(conceptCode)) {
                return rootCode;
            }

        }
        return null;
    }

    public final HashMap<String, RootConcept> getRootMap() {
        return rootMap;
    }

    public final HashMap<String, Vector<String>> getSemanticTypes() {


        return conceptAndSemanticTypes;
    }

    public String whatKindIsThis(String conceptString) {

        for (String rootCode : rootMap.keySet()) {
            if (rootMap.get(rootCode).isDescendant(conceptString)) {
                return rootCode;
            }
        }
        return null;


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

	public HashMap getObjectPropertyCode2NameMap() {
		return objectPropertyCode2NameMap;
	}

	public HashMap getObjectValuedAnnotationPropertyCode2NameMap() {
		return objectValuedAnnotationPropertyCode2NameMap;
	}

	public HashMap getStringValuedAnnotationPropertyCode2NameMap() {
		return stringValuedAnnotationPropertyCode2NameMap;
	}

}

