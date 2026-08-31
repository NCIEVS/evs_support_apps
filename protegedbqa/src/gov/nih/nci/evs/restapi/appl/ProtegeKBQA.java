package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.*;


public class ProtegeKBQA {


	private PrintWriter pw;

	private String ontologyNamespace;


	private Vector<String> stys;


	private String retiredBranch;

	private Vector<String> ignoreSources;
	final HashMap<String, String> duplicate_pn = new HashMap<String, String>();
	final HashMap<String, String> pn_tbl = new HashMap<String, String>();
	final HashMap<String, String> duplicate_pt = new HashMap<String, String>();
	final HashMap<String, String> pt_tbl = new HashMap<String, String>();
	final HashMap<String, String> badchar_newline = new HashMap<String, String>();
	final HashMap<String, String> badchar_at = new HashMap<String, String>();
	final HashMap<String, String> badchar_pipe = new HashMap<String, String>();
	final HashMap<String, String> badchar_tab = new HashMap<String, String>();

	final HashMap<String, String> duplicate_property = new HashMap<String, String>();

	final HashMap<String, String> duplicate_role = new HashMap<String, String>();

	final HashMap<String, String> duplicate_String = new HashMap<String, String>();
	final HashMap<String, String> multiple_DEF = new HashMap<String, String>();
	final HashMap<String, String> quoted_DEF = new HashMap<String, String>();
	final HashMap<String, String> no_DEF = new HashMap<String, String>();
	final HashMap<String, String> multiple_PT = new HashMap<String, String>();
	final HashMap<String, String> no_PT = new HashMap<String, String>();

	final HashMap<String, String> antiquated_noPT = new HashMap<String, String>();
	final HashMap<String, String> multiple_PN = new HashMap<String, String>();
	final HashMap<String, String> no_PN = new HashMap<String, String>();

	final HashMap<String, String> bad_semantictypes = new HashMap<String, String>();
	final HashMap<String, String> multiple_ST = new HashMap<String, String>();
	final HashMap<String, String> no_ST = new HashMap<String, String>();

	final HashMap<String, String> no_DefReview = new HashMap<String, String>();

	final HashMap<String, String> no_DefCurator = new HashMap<String, String>();

	private Vector<String> drugEditors;

	final HashMap<String, String> no_SynForContributingSource = new HashMap<String, String>();

	final HashMap<String, String> no_ContributingSourceForSyn = new HashMap<String, String>();

	final HashMap<String, String> nomatch_pnpt = new HashMap<String, String>();

	final HashMap<String, String> no_alt_def = new HashMap<String, String>();

	final HashMap<String, String> badCS = new HashMap<String, String>();

	final HashMap<String, String> badCS_active = new HashMap<String, String>();

	final HashMap<String, String> missingUNII = new HashMap<String, String>();

	final HashMap<String, String> missingUNII_PT = new HashMap<String, String>();

	final HashMap<String, String> PNbug = new HashMap<String, String>();

	final HashMap<String, String> emptyValue = new HashMap<String, String>();

	final HashMap<Character, UnicodeConverter> symbolMap = new HashMap<Character, UnicodeConverter>();

	final HashMap<String, String> highBitCharacters = new HashMap<String, String>();

	final HashMap<String, String> replacedHighBitCharacters = new HashMap<String, String>();

	final HashMap<String, Integer> altDefSourceCount = new HashMap<String, Integer>();

	final HashMap<String, String> lltNotMedDRA = new HashMap<String, String>();

	final HashMap<String,String> vsNoCS = new HashMap<String, String>();

	final HashMap<String,String> emptyValueSet = new HashMap<String, String>();

	final HashMap<String,String> nullRoots = new HashMap<String, String>();

	private static Properties sysProp = System.getProperties();
	Vector<String> roots = new Vector<String>();
	HashMap<String, Vector<String>> rootMap = new HashMap<String, Vector<String>>();

	HashMap code2AxiomMap = null;
	HierarchyHelper hh = null;
	Vector conceptCodes = null;
	Vector axiom_vec = null;
	HashMap propertyMap = null;
	Vector property_vec = null;

	Messages messages;

	String configFile = null;



	String owlfile = null;
	OWLData owlData = null;
	HashMap roleMap = null;
	private Vector<String> styPairs;
	Vector role_vec = null;

	public ProtegeKBQA() {
	}

	public void initialize(String owlfile) {
		owlData = new OWLData(owlfile);
		code2AxiomMap = owlData.getCode2AxiomMap();
		axiom_vec = owlData.get_axiom_vec();
		hh = owlData.getHierarchyHelper();
		conceptCodes = owlData.getConceptCodes();
		propertyMap = owlData.getPropertyMap();
		roleMap = owlData.getRoleMap();
		role_vec = owlData.get_role_vec();
		property_vec = owlData.get_property_vec();
	}

	private boolean checkNumberConcepts(){
		boolean numberConceptsOK=false;

		return numberConceptsOK;
	}

	private boolean checkRootNodes(){
		boolean rootNodesOK=false;

		return rootNodesOK;
	}

	private boolean checkProperties(){
		boolean propOK=false;

		return propOK;
	}

	private boolean checkRoles(){
		boolean rolesOK=false;

		return rolesOK;
	}

	private boolean checkAxioms(){
		boolean axiomsOK=false;

		return axiomsOK;
	}



	private void checkDEFINITIONExistanceAndUniquness(String code) {
		if (owlData.isRetired(code)) return;

		final Vector<String> v = getProperties(code, messages.getString("ProtegeKBQA.Definition"));
		Vector semType_vec = getProperties(code, messages.getString("ProtegeKBQA.Semantic_Type"));
		String semType = null;
		if (semType_vec != null && semType_vec.size() > 0) {
			semType = (String) semType_vec.elementAt(0);
		}
		if (v == null || v.size() == 0) {
			this.no_DEF.put(code + "_" + hh.getLabel(code), semType != null ? semType : "Null Semantic Type");
		} else if (v != null && v.size() > 1) {
			this.multiple_DEF.put(code + "_" + hh.getLabel(code), Integer.toString(v.size()) + " DEFs");
		}

	}

	private void checkDEFINITIONValue(String code) {
		if (owlData.isRetired(code)) return;
		final Vector<String> v = getProperties(code, messages.getString("ProtegeKBQA.Definition"));
		if (v != null) {
			for (final String def : v) {
				final String defValue = def;
				if (defValue.startsWith("\"") && defValue.endsWith("\"")) { //$NON-NLS-2$
					final String defQuote = "Definition is fully quoted " + def;
					this.quoted_DEF.put(code + "_" + hh.getLabel(code), defQuote);
				}
			}
		}
	}



	private void checkDrugDictionary(String code) {
		if (owlData.isRetired(code)) return;
		final Vector<String> curators = getProperties(code, messages.getString("ProtegeKBQA.Def_Curator"));
		if (curators != null && curators.size() > 0) {
			Vector w = (Vector) code2AxiomMap.get(code);
			Vector<String> defs = new Vector();
			for (int i=0; i<w.size(); i++) {
				String line = (String) w.elementAt(i);
				Vector u = StringUtils.parseData(line, '|');
				if (u.contains(messages.getString("ProtegeKBQA.Definition"))) {
					defs.add(line);
				}
			}
			for (int i=0; i<defs.size(); i++) {
				String def = (String) defs.elementAt(i);
				HashMap hmap = getQualifierMap(def);
				String reviewerName = (String) hmap.get("P379");

				if (reviewerName != null) {
					if ((reviewerName.equalsIgnoreCase(messages.getString("ProtegeKBQA.Special_Review"))
							|| reviewerName.equalsIgnoreCase(messages.getString("ProtegeKBQA.Default_Review")))) {
						return;
					}
					if (drugEditors.contains(reviewerName)) {
						return;
					}
					no_DefCurator.put(code + " " + hh.getLabel(code), reviewerName);
				}
			}
		}

	}

	private void checkDefCurator(String code) {
		if (owlData.isRetired(code)) return;
        boolean hasDefCurator = false;
		Vector w = (Vector) code2AxiomMap.get(code);
		if (w == null) return;
		Vector<String> defs = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (u.contains("P97")) {
				defs.add(line);
			}
		}
		String editor = null;
		for (String def : defs) {
			HashMap hmap = getQualifierMap(def);


			if (hmap.containsKey("Definition Source") && hmap.containsKey("NCI-DEFCURATOR")) {
				hasDefCurator = true;
			} else if (hmap.containsKey("Definition_Reviewer_Name")) {
				editor = (String) hmap.get("Definition_Reviewer_Name");
			}
			if (hasDefCurator && !drugEditors.contains(editor)) {
				no_DefCurator.put(code + " " + hh.getLabel(code), editor);
			}
		}
	}




	private void checkDuplicateProperties(String code) {
		if (owlData.isRetired(code)) return;
	}



	private void checkDuplicateRoles() {
		HashSet hset = new HashSet();
		for (int i=0; i<role_vec.size(); i++) {
			String line = (String) role_vec.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (hset.contains(line)) {
				String roleSrc = (String) u.elementAt(0);
				String roleCode = (String) u.elementAt(1);
				String roleTarget = (String) u.elementAt(2);
				String roleDef = roleCode + " " + roleTarget;
				this.duplicate_role.put(roleDef, roleSrc + " " + hh.getLabel(roleSrc));
			} else {
				hset.add(line);
			}
		}
	}

	private String checkHighBitCharactersInString(String str) {
		StringBuilder returnString = new StringBuilder();
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			final Integer iPosition = Integer.valueOf(i+1);//new Integer(i + 1);
			final char c = str.charAt(i);
			final int cast = (int)c;
			if (cast >= 32 && cast <= 126) {
			} else if (cast >= 127 && cast <= 191) {
				if (symbolMap.containsKey(c)) // magic number for em-dash
				{
					returnString.append("Character replaced ").append("Char: " //$NON-NLS-2$
					).append(str.charAt(i)).append(" Windows-1252 code:").append(cast).append(" " //$NON
					).append(symbolMap.get(c).getCharDescription()).append(" Position: ").append(iPosition).append("  " +
							"  \t");

				} else {
					returnString.append("Unexpected Character ").append("Char: ").append(str.charAt(i)).append(" Windows-1252:" //$NON-NLS-1$
					).append(cast).append(" Position: ").append(iPosition).append("   \t "); //$NON-NLS-2$
				}

			} else if (cast == 215 || cast == 247) {
				returnString.append("Unexpected Character ").append("Char: ").append(str.charAt(i)).append(
						" Windows-1252:" //$NON-NLS-1$
				).append(cast).append(" Position: ").append(iPosition).append("   \t "); //$NON-NLS-2$
			}
		}
		return returnString.toString();
	}

	private void checkHighBitCharacters(Vector v) {
		int lcv = 1;
		int increment = 10000;
		int k = -1;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			checkHighBitCharactersInString(line);
			int j = i+1;
			if (lcv == increment) {
				k++;
				int n = k*increment;
				System.out.println("" + n + " out of " + v.size() + " completed.");
				lcv = 0;
			}
			lcv++;
		}
		System.out.println("" + v.size() + " out of " + v.size() + " completed.");
	}

	private void checkHighBitCharacters(String code) {
		if (owlData.isRetired(code)) return;
		int unexpectedNumber = 1;
		int replacedNumber = 1;
		boolean hasIssue = false;

		Vector w = (Vector) code2AxiomMap.get(code);
		if (w == null) return;
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String propCode = (String) u.elementAt(1);
			String propValue = (String) u.elementAt(2);
			StringBuilder StringValue = new StringBuilder(propValue);
			for (int j=3; j<u.size();j ++) {
				String t = (String) u.elementAt(j);
				Vector u2 = StringUtils.parseData(t, '$');
				String qualValue = (String) u2.elementAt(1);
				StringValue.append(" ").append(qualValue);
			}
			String highBits = checkHighBitCharactersInString(StringValue.toString());

			if (highBits.length() > 0) {
				String bitReport = "String: " + propCode + "\t" + highBits + "String: "	+ propValue;
				if (bitReport.contains("Unexpected Character")) {
					highBitCharacters.put(code + " " + hh.getLabel(code) + " issue " + unexpectedNumber, bitReport);
					unexpectedNumber= unexpectedNumber+1;
				} else {
					replacedHighBitCharacters.put(code + " " + hh.getLabel(code) + " issue "+replacedNumber, bitReport);
					replacedNumber = replacedNumber+1;
				}
			}
		}
	}


	private void checkRootConcept(String code) {
		if (owlData.isRetired(code)) return;
		Vector v1 = hh.getSuperclassCodes(code);
		if (v1 == null || v1.size() == 0) {
			nullRoots.put(code + " " + hh.getLabel(code), "Class not found in any descendent set");
		}
	}

	private void checkNCIPTFullSynExistenceAndUniqueness(String code) {
		if (owlData.isRetired(code)) return;
		Vector<String> syns = getFullSynBySourceAndGroup(code, "NCI", "PT");
		if (syns.size() < 1) {
			syns = getFullSynBySourceAndGroup(code, "NCI", "HD");
		}
		if (syns.size() < 1) {
			syns = getFullSynBySourceAndGroup(code, "CTRM", "PT");
		}
		if (syns.size() < 1) {
			final Vector<String> aqs = getFullSynByGroup(code, "AQ");
			if (aqs.size() == 0) {
				this.no_PT.put(code, Integer.toString(syns.size()) + " PTs");
			} else {
				this.antiquated_noPT.put(code, Integer.toString(syns.size()) + " PTs");
			}
		} else if (syns.size() > 1) {
			this.multiple_PT.put(code, Integer.toString(syns.size()) + " PTs");
		}
	}

	private void checkPNExistenceAndUniqueness(String code) {
		if (owlData.isRetired(code)) return;
		final Vector<String> pn = getProperties(code, messages.getString("ProtegeKBQA.PreferredName"));

		if (pn != null) {
			if (pn.size() < 1) {
				this.no_PN.put(code+"_"+hh.getLabel(code), Integer.toString(pn.size()) + " PNs");
			} else if (pn.size() > 1) {
				this.multiple_PN.put(code+"_"+hh.getLabel(code), Integer.toString(pn.size()) + " PNs");
			}
		}
	}

	private void checkPNFullSynMatch(String code) {
		if (owlData.isRetired(code)) return;

		Vector<String> syns = getFullSynBySourceAndGroup(code, "NCI", "PT");
		if (syns.size() == 0) {
			syns = getFullSynBySourceAndGroup(code, "NCI", "HD");
		}
		if (syns.size() == 0) {
			syns = getFullSynBySourceAndGroup(code, "NCI", "AQ");
		}
		Vector pn_vec = getProperties(code, messages.getString("ProtegeKBQA.PreferredName"));
		if (pn_vec != null && pn_vec.size() > 0) {
			String pn = (String) pn_vec.elementAt(0);
			if (pn == null || syns.size() == 0) {
				System.out.println("Unable to do FullSynMatch for " + code);
			} else {
				for (final String pt : syns) {
					if (!pt.equals(pn)) {
						this.nomatch_pnpt.put(code + "_" + hh.getLabel(code), "PN:" + pn + " PT:" + pt);
					}
				}
			}
		}
	}




	private void checkSameAtoms() {
		HashMap ncipt2CodesMap = new HashMap();
		for (int i=0; i<axiom_vec.size(); i++) {
			String line = (String) axiom_vec.elementAt(i);
			Vector u = StringUtils.parseData(line,'|');
			String code = (String) u.elementAt(0);
			if (StringUtils.isNCItCode(code)) {
				if (!owlData.isRetired(code)) {
					String propCode = (String) u.elementAt(1);
					if (propCode.compareTo("P90") == 0) {
						String termname = (String) u.elementAt(2);
						if (!owlData.isRetired(code)) {
							if (u.contains("P383$PT") && u.contains("P384$NCI")) {
								Vector w = new Vector();
								if (ncipt2CodesMap.containsKey(termname)) {
									w = (Vector) ncipt2CodesMap.get(termname);
								}
								if (!w.contains(code)) {
									w.add(code);
								}
								ncipt2CodesMap.put(termname, w);
							}
						}
					}
				}
			}
		}

        Iterator it = ncipt2CodesMap.keySet().iterator();
        while (it.hasNext()) {
			String pt = (String) it.next();
			Vector codes = (Vector) ncipt2CodesMap.get(pt);
			if (codes.size() > 1) {
				StringBuffer buf = new StringBuffer();
				for (int i=0; i<codes.size(); i++) {
					String code = (String) codes.elementAt(i);
					buf.append(code);
					if (i < codes.size()-1) {
						buf.append(" ");
					}
				}
				String storeCode = buf.toString();
				this.duplicate_pt.put(pt, storeCode);
			}
		}
	}


	private void checkSamePreferredName() {
		final String propCode = messages.getString("ProtegeKBQA.PreferredName");
        HashMap hmap = (HashMap) propertyMap.get(propCode);
        Iterator it = hmap.keySet().iterator();
        HashMap pn2codesMap = new HashMap();
        while (it.hasNext()) {
			String code = (String) it.next();
			if (!owlData.isRetired(code)) {
				Vector pn_vec = (Vector) hmap.get(code);
				if (pn_vec.size() > 1) {
					System.out.println("WARNING: multiple PreferredName found in " + code);
				}
				String pn = (String) pn_vec.elementAt(0);
				Vector w = new Vector();
				if (pn2codesMap.containsKey(pn)) {
					w = (Vector) pn2codesMap.get(pn);
				}
				if (!w.contains(code)) {
					w.add(code);
				}
				w = new SortUtils().quickSort(w);
				pn2codesMap.put(pn, w);
			}
		}
        it = pn2codesMap.keySet().iterator();
        while (it.hasNext()) {
			String pn = (String) it.next();
			Vector codes = (Vector) pn2codesMap.get(pn);
			if (codes.size() > 1) {
				StringBuffer buf = new StringBuffer();
				for (int i=0; i<codes.size(); i++) {
					String code = (String) codes.elementAt(i);
					buf.append(code);
					if (i < codes.size()-1) {
						buf.append(" ");
					}
				}
				String storeCode = buf.toString();
				this.duplicate_pn.put(pn, storeCode);
			}
		}
	}

	private void checkSemanticTypeExistenceAndUniqueness(String code) {
		if (owlData.isRetired(code)) return;

		final Vector<String> styValues = getProperties(code, messages.getString("ProtegeKBQA.Semantic_Type"));

		if (styValues != null && styValues.size() < 1) {

			if (!owlData.isRetired(code)) {

				this.no_ST.put(code+"_"+hh.getLabel(code), Integer.toString(styValues.size()) + " SemanticTypes");
			}
		} else if (styValues != null && styValues.size() > 1) {
			if (!checkSemanticPairs(styValues)) {
				StringBuilder semanticTypeValues = new StringBuilder();
				for (final String prop : styValues) {
					semanticTypeValues.append(prop).append("|");
				}
				semanticTypeValues = new StringBuilder(semanticTypeValues.substring(0, semanticTypeValues.length() - 1));
				this.multiple_ST.put(code+"_"+hh.getLabel(code),
						Integer.toString(styValues.size()) + " SemanticTypes " + semanticTypeValues);
			}
		}
	}

	private boolean checkSemanticPairs(Vector<String> styValues) {
		Collections.sort(styValues);
		StringBuilder styValue = new StringBuilder();
		for (final String prop : styValues) {
			styValue.append(prop).append("|");
		}
		styValue = new StringBuilder(styValue.substring(0, styValue.length() - 1));

		for (final String group : this.styPairs) {
			if (group.equals(styValue.toString())) {
				return true;
			}
		}
		return false;
	}

	private void checkSemanticTypes(String code) {
		if (!owlData.isRetired(code)) return;
		final Vector<String> props = getProperties(code,messages.getString("ProtegeKBQA.Semantic_Type"));
		if (props != null && props.size() > 0) {
			for (final String prop : props) {
				if (!this.stys.contains(prop)) {
					this.bad_semantictypes.put(code, prop);
				}
			}
		}
	}


	private HashMap<String, String> getSynSources(Vector<String> syns) {
		final HashMap<String, String> synMap = new HashMap<String, String>();
		for (int i=0; i<syns.size(); i++) {
			String line = (String) syns.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String syn = (String) u.elementAt(2);
			HashMap hmap = getQualifierMap(line);
			String source = (String) hmap.get(messages.getString("ProtegeKBQA.Term_Source"));
			final String fullSyn = concatenateFullSyn(line);

			synMap.put(fullSyn, source != null ? source : "");

		}
		return synMap;
	}

	private String concatenateFullSyn(String fullsyn_line) {
		String source="";
		String group="";
		Vector u = StringUtils.parseData(fullsyn_line, '|');
		String value=(String) u.elementAt(2);
		HashMap qualMap = getQualifierMap(fullsyn_line);
		String synSource = (String) qualMap.get(messages.getString("ProtegeKBQA.Term_Source"));
		if (synSource != null) {
			source = synSource;
		}
		String synGroup = (String) qualMap.get(messages.getString("ProtegeKBQA.Term_Group"));
		if (synGroup != null) {
			group = synGroup;
		}
		return source + "|" + group + "|" + value; //$NON-NLS-2$
	}



	private void checkTermSource_error() {
		String propCode = messages.getString("ProtegeKBQA.FULL_SYN");
		Vector qualCodes = new Vector();
		qualCodes.add(messages.getString("ProtegeKBQA.Term_Source"));
		Vector qualValues = new Vector();
		HashMap contributingSrcMap = (HashMap) propertyMap.get(messages.getString("ProtegeKBQA.Contributing_Source"));

        for (int i=0; i<conceptCodes.size(); i++) {
			String code = (String) conceptCodes.elementAt(i);
			if (!owlData.isRetired(code)) {
				Vector fullsyn_lines = getAxiomLines(code, propCode);
				for (int j=0; j<fullsyn_lines.size(); j++) {
					String fullSyn = (String) fullsyn_lines.elementAt(j);
					Vector u = StringUtils.parseData(fullSyn, '|');
					String termname = (String) u.elementAt(2);
					HashMap hmap = getQualifierMap(fullSyn);
					String synSource = (String) hmap.get(messages.getString("ProtegeKBQA.Term_Source"));
					boolean debub = this.ignoreSources.contains(synSource);
					Vector contributingSources = (Vector) contributingSrcMap.get(code);
					if (contributingSources != null) {
						if (!(synSource.equals("NCI"))) {
							if (!(synSource.equals("")) && !this.ignoreSources.contains(synSource)) {
								boolean found = false;
								for (int k=0; k<contributingSources.size(); k++) {
									String contributingSource = (String) contributingSources.elementAt(k);
									if (contributingSource.equals(synSource)) {
										found = true;
									}
								}
								if (!found) {
									this.no_ContributingSourceForSyn.put(code, termname + "|" + synSource);
								}
							}
						}
					} else {
					}
				}
			}
		}

		Iterator it = contributingSrcMap.keySet().iterator();
		boolean found = false;
		while (it.hasNext()) {
			String code = (String) it.next();
			if (!owlData.isRetired(code)) {
				Vector cs_vec = (Vector) contributingSrcMap.get(code);
				if (cs_vec != null && cs_vec.size() > 0) {
					for (int i=0; i<cs_vec.size(); i++) {
						String cs = (String) cs_vec.elementAt(i);
						qualValues = new Vector();
						if (cs.equals("CTCAE")) {
							qualValues.add("CTCAE 3.0");
							qualValues.add("CTCAE 5.0");
							qualValues.add("CTCAE 6.0");
						} else if (cs.equals("BRIDG")) {
							qualValues.add("BRIDG 3.0.3");
							qualValues.add("BRIDG 5.3");
						} else {
							qualValues.add(cs);
						}
						found = checkPropertyQualifierExistence(code, propCode, qualCodes, qualValues);
						if (!found) {
							this.no_SynForContributingSource.put(code, cs);
						}
					}
				}
			}
		}
	}


	private boolean checkPreferredNameToSource(String code, String source){
		final Vector<String> vsl = getProperties(code, messages.getString("ProtegeKBQA.PreferredName"));
		for (int i=0; i<vsl.size(); i++) {
			String t = (String) vsl.elementAt(i);
			if(t.contains(source)){
				return true;
			}
		}
		return false;
	}


	private void checkFDA_UNII(String code) {
		if (owlData.isRetired(code)) return;

		Vector w = (Vector) code2AxiomMap.get(code);
		if (w == null) return;
		Vector unii_syn = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (u.contains(messages.getString("ProtegeKBQA.FULL_SYN"))) {
				unii_syn.add(line);
			}
		}
		Vector<String> subsets = getProperties(code, messages.getString("ProtegeKBQA.Concept_In_Subset"));
		if (subsets != null && subsets.size() > 0) {
			boolean hasFULL_SYN = false;
			boolean hasUNII_Code = true;
			for (int i=0; i<subsets.size(); i++) {
				String subset = (String) subsets.elementAt(i);
				if (subset.contains(messages.getString("ProtegeKBQA.FDA_UNII_Code_Terminology"))) {
					Vector unii_code_vec = getProperties(code, messages.getString("ProtegeKBQA.FDA_UNII_Code"));
					String unii_code = (String) unii_code_vec.elementAt(0);
					if (unii_code == null || unii_code.length() == 0) {
						hasUNII_Code = false;
					}
					for (int j=0; j<unii_syn.size(); j++) {
						String line = (String) unii_syn.elementAt(j);
						HashMap hmap = getQualifierMap(line);
						String termSource = (String) hmap.get(messages.getString("ProtegeKBQA.term_source"));

						if (termSource != null && termSource.equals("FDA")) {
							hasFULL_SYN = true;
						}

					}
					if (!hasFULL_SYN || !hasUNII_Code) {
						String message = " ";
						if (!hasFULL_SYN) {
							message = message.concat("Missing FDA Full Syn ");
						}
						if (!hasUNII_Code) {
							message = message.concat("Missing UNII Code ");
						}
						this.missingUNII.put(code, message);
					}
				}
			}
		}
	}

	private boolean checkMetrics() {
		int x = axiom_vec.size();
		if (x == 0) {
			System.out.println("Axiom Count = 0");
			return false;
		}
		if (x > 4000000) {
			System.out.println("AxiomCount > 4000000");
			return false;
		}
		return true;
	}


	private void checkRetiredConceptStatus(String code) {

		final String retiredRoot = "C28428";//this.ontology.getConcept(this.retiredBranch);
		final Vector<String> retiredConceptCodes = hh.get_transitive_closure_v3(retiredRoot);//retiredRoot.getAllDescendantCodes();
		if (retiredConceptCodes != null) {
			for (final String conceptCode : retiredConceptCodes) {

				final Vector<String> v = getProperties(conceptCode, "P310");
				if (v == null || v.size() == 0) {
					this.badCS.put(code, "no Concept Status");
				} else if (v.size() > 1) {
					if (!v.contains("Retired_Concept")) {
						this.badCS.put(code, "Multiple Concept Status, no Retired Status");
					}

				} else {
					String status = (String) v.elementAt(0);
					if (!status.equals("Retired_Concept")) {
						this.badCS.put(code, "Concept Status = " + status);
				    }
				}
			}
		}

		for (int i=0; i<conceptCodes.size(); i++) {
			String activeConcept = (String) conceptCodes.elementAt(i);
			final boolean retired = owlData.isRetired(activeConcept);
			if (!retired) {
				final Vector<String> v = getProperties(activeConcept, "P310");//"Concept_Status");
				for (final String prop : v) {
					if (prop.equals("Retired_Concept")) {
						this.badCS_active.put(code, "Active marked Retired_Concept");
					}
				}
			}

		}

	}

	private void checkPreferredNameBug(String code) {
		if (owlData.isRetired(code)) return;
		final String propCode = messages.getString("ProtegeKBQA.PreferredName");
        HashMap hmap = (HashMap) propertyMap.get(propCode);
		final Vector<String> properties = (Vector) hmap.get(code);
	    if (properties != null) {
			for (final String prop : properties) {
				if (!propCode.equals(messages.getString("ProtegeKBQA.PreferredName"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.FULL_SYN"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.rdfs_Label"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.Legacy_Name"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.Display_Name"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.Has_Salt_Form"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.NICHD_Hierarchy_Term"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.Semantic_Type"))
						&& !propCode.equals(messages.getString("ProtegeKBQA.Maps_To"))) {
					this.PNbug.put(code+"_"+hh.getLabel(code), propCode + " " + propCode);
				} else if (propCode.equals(messages.getString("ProtegeKBQA.Semantic_Type"))) {
					if (!this.stys.contains(propCode)) {
						this.PNbug.put(code+"_"+hh.getLabel(code), propCode + " " + propCode);
					}
				}
			}
		}
	}

	private void checkForEmptyQualifiers() {
		for (int i=0; i<axiom_vec.size(); i++) {
			String line = (String) axiom_vec.elementAt(i);
			Vector u = StringUtils.parseData(line,'|');
			String code = (String) u.elementAt(0);
			if (!owlData.isRetired(code)) {
				String propCode = (String) u.elementAt(1);
				for (int j=3; j<u.size(); j++) {
					String t = (String) u.elementAt(j);
					Vector u2 = StringUtils.parseData(t,'$');
					if (u2.size() < 2) {
						System.out.println(line);
						System.out.println("t: " + t);
						String qualCode = (String) u2.elementAt(0);
						String qualValue = (String) u2.elementAt(1);
						if (qualValue.length() == 0) {
							this.emptyValue.put(code +"_"+ hh.getLabel(code), propCode + " " + qualCode);
						}
					}
				}
			}
		}

	}

	private void checkMedDRA_LLT(String code) {
		if (owlData.isRetired(code)) return;
		Vector w = (Vector) code2AxiomMap.get(code);
		if (w == null) return;
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String syn = (String) u.elementAt(2);
			if (line.indexOf("|P90|") != -1) {
				HashMap hmap = getQualifierMap(line);
				if (hmap.containsKey(messages.getString("ProtegeKBQA.Term_Group")) &&
		            hmap.containsKey(messages.getString("ProtegeKBQA.Term_Source"))) {
					String termGroup = (String) hmap.get("ProtegeKBQA.Term_Group");
					String termSource = (String) hmap.get("ProtegeKBQA.Term_Source");
					if (termGroup != null && termSource != null) {
						if (termGroup.compareTo("LLT") == 0 && termSource.compareTo("MedDRA") == 0) {
							lltNotMedDRA.put(code+"_" + hh.getLabel(code), syn);
						}
					}
				}
			}
		}
	}

	private void checkAltsWithNCI(String code) {
		if (owlData.isRetired(code)) return;
		String altDef = null;
		Vector<String> results = new Vector<String>();
		Vector v = (Vector) code2AxiomMap.get(code);
		if (v == null) return;
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String def = (String) u.elementAt(3);
			if (line.indexOf("|P325|") != -1) {
				HashMap hmap = getQualifierMap(line);
				String source = (String) hmap.get(messages.getString("ProtegeKBQA.Def_Source"));
				String src = (String) hmap.get(source);
				if (src != null && src.equals("NCI")) {
					System.out.println("Adding Alt-Def with NCI source " + code);
					results.add(def);
				}
			}
		}
		for (final String def : results) {
			if (altDef == null) {
				altDef = def;
			} else {
				altDef = altDef.concat("|" + def);
			}
		}
	}

	private void checkForValueSetSources(){

		final String subsetRoot = "C54443";//this.ontology.getConcept("C54443");
		Vector<String> subsetConcepts = hh.get_transitive_closure_v3(subsetRoot);//subsetRoot.getAllDescendantCodes();

		if (subsetConcepts != null) {
			for (final String code : subsetConcepts) {
				if (!owlData.isRetired(code)) {
					if (getProperties(code, messages.getString("ProtegeKBQA.Publish_Value_Set")) == null) {
					} else {
						final Vector<String> publish = getProperties(code, messages.getString("ProtegeKBQA.Publish_Value_Set"));
						if (publish != null && publish.size() == 1) {
							if (publish.elementAt(0).toUpperCase().equals("YES")) {
								checkEmptyValueSets(code);
								final Vector<String> v = getProperties(code, messages.getString("ProtegeKBQA.Contributing_Source"));
								if (v == null || v.size() == 0) {
									this.vsNoCS.put(code, "no Contributing Source " + hh.getLabel(code));
								} else if (v.size() > 1) {
									this.vsNoCS.put(code,
											"Multiple Contributing Sources " + hh.getLabel(code));
								}
							}
						} else if (publish != null && publish.size() > 1) {
							this.vsNoCS.put(code, "Multiple Publish_Value_Set " + hh.getLabel(code));
						}
					}
				}
			}
		}

	}

	private void checkEmptyValueSets(String codeconcept) {


	}

	private void configPrintWriter(String outputfile) throws Exception {
		try {
			final File file = new File(outputfile);
			this.pw = new PrintWriter(file, StandardCharsets.UTF_8.name());

		} catch (final Exception e) {
			System.out.println("Error in PrintWriter");
			throw e;
		}
	}

	public void printHelp() {
		System.out.println("java -jar path/owlnciqa.jar");
		System.out.println();
		System.out.println("-c --config\t\tPath to configuration file");
		System.out.println("-i --input\t\tString of vocabulary file (optional)");
		System.out.println("-o --output\t\tString of output file(optional)");
		System.out.println();
		System.out.println("If input and output not passed in as parameters, they must be specified in config file");
		System.exit(0);
	}

	public void configure(String[] args) {
		String physicalString = null;
		String outputFile = null;
		try {

			if (args.length > 0) {
				for (int i = 0; i < args.length; i++) {
					if (args[i].equalsIgnoreCase("-c") || args[i].equalsIgnoreCase("--config")) {
						this.configFile = args[++i];
					} else if (args[i].equalsIgnoreCase("-i") || args[i].equalsIgnoreCase("--input")) {
						physicalString = args[++i];
					} else if (args[i].equalsIgnoreCase("-o") || args[i].equalsIgnoreCase("--output")) {
						outputFile = args[++i];
					} else {
						printHelp();
					}
				}
			} else {
				printHelp();
			}

			if (this.configFile == null) {
				this.configFile = "nciowlqa.properties";
			}
			System.out.println("Config file at: " + this.configFile);

			final Properties props = new Properties();
			props.load(new FileInputStream(this.configFile));
			this.ontologyNamespace = props.getProperty("namespace");
			if (physicalString == null) {
				physicalString = props.getProperty("physicalString");
			}
			System.out.println("Input file is " + physicalString);
			owlfile = physicalString;
			System.out.println("initialize " + owlfile);
			initialize(owlfile);

			this.stys = setFromConfig(props.getProperty("semantictypefile"));
			this.styPairs = setFromConfig(props.getProperty("semanticPairsFile"));
			this.ignoreSources = setFromConfig(props.getProperty("ignorefile"));
			this.drugEditors = setFromConfig(props.getProperty("drugeditorsfile"));



			this.retiredBranch = props.getProperty("deprecatedConceptBranch");
			this.messages = new Messages(props.getProperty("messagesLocation"));

			if (outputFile == null) {
				outputFile = props.getProperty("outputfile");
			}
			if (outputFile.toString().length() == 0) {
				System.out.println("No output file specified");
				printHelp();
			}
			System.out.println("Output file is " + outputFile.toString());

			readSymbolMap(props.getProperty("symbolMap"));

			configPrintWriter(outputFile);

		} catch (final java.io.FileNotFoundException e) {
			System.out.println("Error in reading config files");
			e.printStackTrace();
			System.exit(1);
		} catch (final IllegalArgumentException e) {
			System.out.println("Error in parameter");
			e.printStackTrace();
			System.exit(1);
		} catch (final Exception e) {
			System.out.println("Error in reading ontology");
			e.printStackTrace();
			System.exit(1);
		}
	}


	private Vector<String> setFromConfig(String f) {
		Vector<String> stys = new Vector<String>();
		stys = readConfigFile(f);
		return stys;
	}

	public Vector<String> readConfigFile(String filename) {
		final Vector<String> v = new Vector<String>();
		FileReader configFile = null;
		BufferedReader buff = null;
		try {
			configFile = new FileReader(filename);
			buff = new BufferedReader(configFile);
			boolean eof = false;
			while (!eof) {
				final String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else {
					v.add(line);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			try {
				assert buff != null;
				buff.close();
				assert configFile!=null;
				configFile.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		if (!v.isEmpty()) {
			return v;
		} else {
			return null;
		}
	}

	public void readSymbolMap(String filename) {
		final Vector<String> v = new Vector<String>();
		BufferedReader buff = null;
		try {
			buff = new BufferedReader(
					new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8.name()));
			boolean eof = false;
			while (!eof) {
				final String line = buff.readLine();
				if (line == null) {
					eof = true;
				} else if (!line.startsWith("#")) {
					UnicodeConverter uc = new UnicodeConverter(line);
					symbolMap.put(uc.getUnicodeChar(), uc);
					v.add(line);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			try {
				assert buff!=null;
				buff.close();
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
	}


	public void performQA() {
		boolean metricsGood = checkMetrics();
		if (!metricsGood) {
			System.out.println("Metrics Failed");
			pw.println("Metrics failed");
			pw.close();
			System.exit(0);
		}

		loadRootConcepts();


		int lcv = 1;
		int increment = 10000;
		int k = 0;

        System.out.println("check properties for all concepts ...");
		for (int i=0; i<conceptCodes.size(); i++) {
            String code = (String) conceptCodes.elementAt(i);
			int j = i+1;
			if (lcv == increment) {
				k++;
				int n = k*increment;
				System.out.println("" + n + " out of " + conceptCodes.size() + " completed.");
				lcv = 0;
			}
			lcv++;

			if (!owlData.isRetired(code)) {
				checkHighBitCharacters(code);
				checkDuplicateProperties(code);
				checkDEFINITIONExistanceAndUniquness(code);
				checkDEFINITIONValue(code);
				checkPNExistenceAndUniqueness(code);
				checkPNFullSynMatch(code);
				checkNCIPTFullSynExistenceAndUniqueness(code);
				checkMedDRA_LLT(code);
				checkDrugDictionary(code);
				checkAltsWithNCI(code);
				checkDefCurator(code);
				checkSemanticTypes(code);
				checkSemanticTypeExistenceAndUniqueness(code);
				checkFDA_UNII(code);
				checkPreferredNameBug(code);
				checkRootConcept(code);
			}
		}

		System.out.println("" + conceptCodes.size() + " out of " + conceptCodes.size() + " completed.");
        System.out.println("checkSamePreferredName ...");
		checkSamePreferredName();
		System.out.println("checkSameAtoms ...");
		checkSameAtoms();
        System.out.println("checkCharacters ...");
		checkCharacter('\n');
		checkCharacter('@');
		checkCharacter('|');
		checkCharacter('\t');

        System.out.println("checkTermSource ...");
		checkTermSource();

		System.out.println("checkDuplicateRoles ...");
		checkDuplicateRoles();

		System.out.println("checkForEmptyQualifiers ...");
		checkForEmptyQualifiers();

		System.out.println("checkForValueSetSources ...");
		checkForValueSetSources();

		System.out.println("printReport ...");
		printReport();

		this.pw.close();
	}

	private void loadRootConcepts() {
		System.out
				.println("Search vocabulary for root concepts and load descendants");

		Vector<String> rootConceptCodes = hh.getRoots();
		for (String root : rootConceptCodes) {
			Vector v = hh.get_transitive_closure_v3(root);
			roots.add( root);
			rootMap.put(root, v);
		}
		System.out.println("Finished loading root classes");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	List sortHashMapByKey(HashMap hm) {
		final List<Map.Entry<String, String>> list = new Vector<Map.Entry<String, String>>(hm.entrySet());

		final Comparator byValue = (Comparator<Map.Entry<String, String>>) (entry, entry1) -> {
			return (entry.getValue().compareTo(entry1.getValue()));
		};

		final Comparator byKey = (Comparator<Map.Entry<String, String>>) (entry, entry1) -> {
			return (entry.getKey().compareTo(entry1.getKey()));
		};

		list.sort(new CompositeComparator(byKey, byValue));

		return list;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	List sortHashMapByValue(HashMap hm) {
		final List<Map.Entry<String, String>> list = new Vector<Map.Entry<String, String>>(hm.entrySet());

		final Comparator byValue = (Comparator<Map.Entry<String, String>>) (entry, entry1) -> {
			if (entry.getValue() == null) {
				return -1;
			} else if (entry1.getValue() == null) {
				return +1;
			} else {
				return (entry.getValue().compareTo(entry1.getValue()));
			}
		};

		final Comparator byKey = new Comparator<Map.Entry<String, String>>() {
			@Override
			public int compare(Map.Entry<String, String> entry, Map.Entry<String, String> entry1) {
				return (entry.getKey().compareTo(entry1.getKey()));
			}
		};



		list.sort(new CompositeComparator(byValue, byKey));

		return list;

	}

	private Vector<String> tokenize(String pattern0) {
		final Vector<String> v = new Vector<String>();
		final StringTokenizer st = new StringTokenizer(pattern0);
		while (st.hasMoreTokens()) {
			v.add(st.nextToken());
		}
		return v;
	}

	private String XML2Pipe(String pattern) {


		int n = pattern.indexOf(">");
		StringBuilder retstr = new StringBuilder();
		int lcv = 0;
		while (n != -1) {
			String tag = pattern.substring(0, n + 1);
			pattern = pattern.substring(n + 1, pattern.length());
			n = pattern.indexOf("<");
			if (n == -1) {
				break;
			}

			final String value = pattern.substring(0, n);

			if (value.length() > 0) {
				retstr.append(value);
				retstr.append("|");
				lcv++;
			}

			pattern = pattern.substring(n);

			n = pattern.indexOf(">");
			if (n == -1) {
				break;
			}

			tag = pattern.substring(0, n + 1);

		}

		if (lcv > 0) {
			retstr = new StringBuilder(retstr.substring(0, retstr.length() - 1));
		} else {
			retstr = new StringBuilder(pattern);
		}
		return (retstr.toString());
	}



	@SuppressWarnings({ "rawtypes" })
	private void printReport() {

		this.pw.println("Replaced high bit characters: " + this.replacedHighBitCharacters.size());
		List sortedReturn = sortHashMapByKey(this.replacedHighBitCharacters);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Property values containing an incorrect Semantic_Type: " + this.bad_semantictypes.size());
		sortedReturn = sortHashMapByKey(this.bad_semantictypes);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with no Semantic_Type property: " + this.no_ST.size());
		sortedReturn = sortHashMapByKey(this.no_ST);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Retired Concepts with bad Concept_Status: " + this.badCS.size());
		sortedReturn = sortHashMapByKey(this.badCS);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Active concepts with retired Concept_Status: " + this.badCS_active.size());
		sortedReturn = sortHashMapByKey(this.badCS_active);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with null root: "+ this.nullRoots.size());
		sortedReturn=sortHashMapByKey(this.nullRoots);
		for (final Object o:sortedReturn){
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with duplicate roles within: " + this.duplicate_role.size());
		sortedReturn = sortHashMapByKey(this.duplicate_role);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with duplicate properties within: " + this.duplicate_property.size());
		sortedReturn = sortHashMapByKey(this.duplicate_property);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with multiple NCI|PT FULL-SYN properties: " + this.multiple_PT.size());
		sortedReturn = sortHashMapByKey(this.multiple_PT);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with quotes around entire definition: " + this.quoted_DEF.size());
		sortedReturn = sortHashMapByKey(this.quoted_DEF);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with no Preferred_Name property: " + this.no_PN.size());
		sortedReturn = sortHashMapByKey(this.no_PN);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with multiple Preferred_Name properties: " + this.multiple_PN.size());
		sortedReturn = sortHashMapByKey(this.multiple_PN);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts where the NCI|PT and Preferred_Name don't match: " + this.nomatch_pnpt.size());
		sortedReturn = sortHashMapByKey(this.nomatch_pnpt);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Possible instances of Preferred_Name / Last property bug: " + this.PNbug.size());
		sortedReturn = sortHashMapByKey(this.PNbug);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"Concepts with no NCI|PT FULL-SYN property (Excludes HD, AQ and CTRM concepts): " + this.no_PT.size());
		sortedReturn = sortHashMapByKey(this.no_PT);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Empty properties and qualifiers:" + this.emptyValue.size());
		sortedReturn = sortHashMapByKey(this.emptyValue);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("High bit characters: " + this.highBitCharacters.size());
		sortedReturn = sortHashMapByKey(this.highBitCharacters);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Property values containing character @: " + this.badchar_at.size());
		sortedReturn = sortHashMapByKey(this.badchar_at);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Property values containing character \\n : " + this.badchar_newline.size());
		sortedReturn = sortHashMapByKey(this.badchar_newline);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Property values containing character | : " + this.badchar_pipe.size());
		sortedReturn = sortHashMapByKey(this.badchar_pipe);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Property values containing \\t : " + this.badchar_tab.size());
		sortedReturn = sortHashMapByKey(this.badchar_tab);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"Concepts where the FDA_UNII_Code or FDA PT are missing for UNII Concepts: " + this.missingUNII.size());
		sortedReturn = sortHashMapByKey(this.missingUNII);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with an LLT FULL_SYN that is not MedDRA: " + this.lltNotMedDRA.size());
		sortedReturn = sortHashMapByKey(this.lltNotMedDRA);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Concepts with multiple DEFINITION properties: " + this.multiple_DEF.size());
		sortedReturn = sortHashMapByKey(this.multiple_DEF);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Def_Curator concepts without proper Definition_Review_Name: " + this.no_DefReview.size());
		sortedReturn = sortHashMapByKey(this.no_DefReview);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("NCI-DEFCURATOR sources Definitions without proper Definition_Reviewer_Name: "
				+ this.no_DefCurator.size());
		sortedReturn = sortHashMapByKey(this.no_DefCurator);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println("Value Set concepts with no Contributing Source or multiple Contributing Sources: " + this.vsNoCS.size());
		sortedReturn = sortHashMapByKey(this.vsNoCS);
		for(final  Object o:sortedReturn){
			this.pw.println(o.toString());
		}


		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("*  Concepts with multiple, potentially conflicting, Semantic_Type properties: "
				+ this.multiple_ST.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");

		sortedReturn = sortHashMapByKey(this.multiple_ST);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("Preferred Names duplicated between concepts: " + this.duplicate_pn.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");
		sortedReturn = sortHashMapByKey(this.duplicate_pn);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("NCI|PT duplicated between concepts: " + this.duplicate_pt.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");
		sortedReturn = sortHashMapByKey(this.duplicate_pt);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("Contributing source concepts without proper ALT_DEFINITION: " + this.no_alt_def.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");

		sortedReturn = sortHashMapByKey(this.no_alt_def);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("Contributing sources with no matching FULL_SYN: " + this.no_SynForContributingSource.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");
		sortedReturn = sortHashMapByKey(this.no_SynForContributingSource);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("FULL_SYNs with no matching contributing source: " + this.no_ContributingSourceForSyn.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");
		sortedReturn = sortHashMapByKey(this.no_ContributingSourceForSyn);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("Antiquated, Header or CTRM concepts with no NCI|PT FULL-SYN property : "
				+ this.antiquated_noPT.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");
		sortedReturn = sortHashMapByKey(this.antiquated_noPT);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}

		this.pw.println();
		this.pw.println(
				"********************************************************************************************************");
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println("Concepts with no DEFINITION property, sorted by Semantic Type: " + this.no_DEF.size());
		this.pw.println(
				"*                                                                                                      *");
		this.pw.println(
				"********************************************************************************************************");
		sortedReturn = sortHashMapByValue(this.no_DEF);
		for (final Object o : sortedReturn) {
			this.pw.println(o.toString());
		}
	}

	public void checkHighBitCharacters() {
		checkHighBitCharacters(axiom_vec);
	}

	public Vector getProperties(String code, String propCode) {
		HashMap hmap = (HashMap) propertyMap.get(propCode);
		if (hmap != null) {
			Vector v = (Vector) hmap.get(code);
			if (v == null || v.size() == 0) {
			}
			return v;
		}
		return null;
	}

	public HashMap getQualifierMap(String line) {
		HashMap hmap = new HashMap();
		Vector u = StringUtils.parseData(line, '|');
		for (int i=3; i<u.size(); i++) {
			String t = (String) u.elementAt(i);
			Vector u2 = StringUtils.parseData(t, '$');
			hmap.put((String) u2.elementAt(0), (String) u2.elementAt(1));
		}
		return hmap;
	}

	private Vector<String> getFullSynBySourceAndGroup(String code, String source, String group) {
		final Vector<String> out = new Vector<String>();
		Vector v = (Vector) code2AxiomMap.get(code);
		if (v == null) return new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (line.indexOf("|P90|") != -1) {
				HashMap hmap = getQualifierMap(line);
				String sourceQual =  (String) hmap.get(messages.getString("ProtegeKBQA.Term_Source"));
				String groupQual =  (String) hmap.get(messages.getString("ProtegeKBQA.Term_Group"));
				if (sourceQual != null && groupQual != null) {
					if (sourceQual.equals(source) && groupQual.equals(group)) {
						out.add((String) u.elementAt(2));
					}
				}
			}
		}
		return out;
	}

	private Vector<String> getFullSynBySource(String code, String source) {
        final Vector<String> out = new Vector<String>();
		Vector v = (Vector) code2AxiomMap.get(code);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (line.indexOf("|P90|") != -1) {
				HashMap hmap = getQualifierMap(line);
				String sourceQual =  (String) hmap.get(messages.getString("ProtegeKBQA.Term_Source"));
				if (sourceQual != null) {
					if (sourceQual.equals(source)) {
						out.add((String) u.elementAt(2));
					}
				}
			}
		}
		return out;
	}

	private Vector<String> getFullSynByGroup(String code, String group) {
        final Vector<String> out = new Vector<String>();
		Vector v = (Vector) code2AxiomMap.get(code);
		if (v == null) return new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (line.indexOf("|P90|") != -1) {
				HashMap hmap = getQualifierMap(line);
				String groupQual = (String) hmap.get(messages.getString("ProtegeKBQA.Term_Group"));
				if (groupQual != null) {
					if (groupQual.equals(group)) {
						out.add((String) u.elementAt(2));
					}
				}
			}
		}
		return out;
	}

	private boolean isRetiredInSet(Vector<String> statusSet) {
		boolean isRetired = false;
		for (final String status : statusSet) {
			if (status.equals("Retired_Concept")) {
				isRetired = true;
			}
		}
		return isRetired;
	}

	public boolean checkPropertyQualifierExistence(String code, String propCode, Vector qualCodes, Vector qualValues) {
		Vector<String> v = getProperties(code, propCode);
		if (v == null || v.size() == 0) return false;
		Vector w = (Vector) code2AxiomMap.get(code);
		Vector<String> properties = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (u.contains(propCode)) {
				properties.add(line);
			}
		}
		boolean bool = false;
		for (int i=0; i<properties.size(); i++) {
			String line = (String) properties.elementAt(i);
			HashMap hmap = getQualifierMap(line);
			for (int j=0; j<qualCodes.size(); j++) {
				String qCode = (String) qualCodes.elementAt(j);
				if (hmap.containsKey(qCode)) {
					String value = (String) hmap.get(qCode);
					String expectedValue = (String) qualValues.elementAt(j);
					if (value.equals(expectedValue)) {
						return true;
					}
				}
			}
		}
		return bool;
	}

	private void checkDuplicateProperties() {
		property_vec = owlData.get_property_vec();
		HashSet hset = new HashSet();
		for (int i=0; i<property_vec.size(); i++) {
			String line = (String) property_vec.elementAt(i);
			if (!hset.contains(line)) {
				hset.add(line);
			} else {
				Vector u = StringUtils.parseData(line, '|');
				String code = (String) u.elementAt(0);
				String propCode = (String) u.elementAt(1);
				this.duplicate_property.put(code + ":" + hh.getLabel(code), propCode);
			}
		}
	}

	private void checkCharacter(char c) {
		String c1;
		c1 = "" + c;

        for (int i=0; i<property_vec.size(); i++) {
			String property = (String) property_vec.elementAt(i);
			Vector u = StringUtils.parseData(property, '|');
			String code = (String) u.elementAt(0);
			String propCode = (String) u.elementAt(1);
			String propValue = (String) u.elementAt(2);
			if (propValue.contains(c1)) {
				if (c == '@') {
					this.badchar_at.put(code + " " + hh.getLabel(code), propValue);
				} else if (c == '\n') {
					this.badchar_newline.put(code + " " + hh.getLabel(code), propValue);
				} else if (c == '|') {
					if (!propCode.equals(messages.getString("ProtegeKBQA.Value_Set_Location"))) {
						this.badchar_pipe.put(code + " " + hh.getLabel(code), propValue);
					}
				} else if (c == '\t') {
					this.badchar_tab.put(hh.getLabel(code), propValue);
				}
			}
		}
	}


    public Vector getAxiomLines(String code, String propCode) {
		Vector lines = (Vector) code2AxiomMap.get(code);
		if (lines == null) return new Vector();
		Vector w = new Vector();
		for (int i=0; i<lines.size(); i++) {
			String line = (String) lines.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			if (u.contains(propCode)) {
				w.add(line);
			}
		}
		return w;
	}

	private boolean checkContributingSourceAndAltDef(String code, String source) {
        String propCode = messages.getString("ProtegeKBQA.Definition");
		final Vector<String> defs = getAxiomLines(code, propCode);
		final Vector<String> unii_codes = getProperties(code, messages.getString("ProtegeKBQA.FDA_UNII_Code"));

		boolean hasUNII = false;
		if (unii_codes != null && unii_codes.size() > 0) {
			hasUNII = true;
		}

		final String sCode = code;
		List<String> exclude = Arrays.asList("UCUM", "MedDRA", "ICH", "HL7", "NCPDP"); //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$

        propCode = messages.getString("ProtegeKBQA.Alt_Definition");
		final Vector<String> alt_defs = getAxiomLines(code, propCode);
		if (alt_defs.size() > 0) {
			if (!this.ignoreSources.contains(source)) {
				for (final String def : alt_defs) {
					HashMap defSourceMap = getQualifierMap(def);
					String defSource = (String) defSourceMap.get(messages.getString("ProtegeKBQA.Def_Source"));
					if (defSource != null && defSource.contains(source)) {
						return true;
					} else if (source.equals("FDA") && hasUNII) {
						return true;
					}
				}


				if (!exclude.contains(source)) {
					this.no_alt_def.put(sCode, "No ALT_DEFINITION for " + source);

					if (altDefSourceCount.containsKey(source)) {
						Integer tempInt = altDefSourceCount.get(source) + 1;
						altDefSourceCount.put(source, tempInt);
					} else {
						altDefSourceCount.put(source, 1);
					}
					return false;
				}
			}
		} else {


			if (hasUNII && source.equals("FDA")) {
				return true;
			}
			if (this.ignoreSources.contains(source)) {
				return true;
			}

			if (exclude.contains(source)) {
				return true;
			}

			this.no_alt_def.put(sCode, "No ALT_DEFINITION for " + source);
			if (altDefSourceCount.containsKey(source)) {
				Integer tempInt = altDefSourceCount.get(source) + 1;
				altDefSourceCount.put(source, tempInt);
			} else {
				altDefSourceCount.put(source, 1);
			}
		}
		return false;
	}

	private void checkTermSource() {

		String propCode = messages.getString("ProtegeKBQA.FULL_SYN");
		Vector qualCodes = new Vector();
		qualCodes.add(messages.getString("ProtegeKBQA.Term_Source"));
		Vector qualValues = new Vector();
		HashMap contributingSrcMap = (HashMap) propertyMap.get(messages.getString("ProtegeKBQA.Contributing_Source"));

        for (int i=0; i<conceptCodes.size(); i++) {
			String code = (String) conceptCodes.elementAt(i);
			if (!owlData.isRetired(code)) {
				Vector fullsyn_lines = getAxiomLines(code, propCode);
				final HashMap<String, String> synMap = getSynSources(fullsyn_lines);
				Vector<String> contributingSources = getProperties(code, messages.getString("ProtegeKBQA.Contributing_Source"));
				if (contributingSources != null && fullsyn_lines != null) {
					if (contributingSources.size() > 0 && fullsyn_lines.size() > 0) {
						for (int j=0; j<contributingSources.size(); j++) {
							String contributingSource = (String) contributingSources.elementAt(j);
							if (!(this.ignoreSources.contains(contributingSource))) {
								boolean hasSource = false;
								for (int k=0; k<fullsyn_lines.size(); k++) {
									String syn = (String) fullsyn_lines.elementAt(k);
									HashMap hmap = getQualifierMap(syn);
									String qualValue = (String) hmap.get(messages.getString("ProtegeKBQA.Term_Source"));
									if (qualValue != null) {
										if (qualValue.equals(contributingSource)) {
											hasSource = true;
										}
										if(contributingSource.equals("CTCAE")){
											if (qualValue.contains(contributingSource)) {
												hasSource = true;
											}
										}
										if (contributingSource.equals("BRIDG")){
											if (qualValue.contains(contributingSource)) {
												hasSource = true;
											}
										}
									}
								}
								if (!hasSource) {
									boolean hasDef = checkContributingSourceAndAltDef(code, contributingSource);
									boolean hasValueSet = checkPreferredNameToSource(code,contributingSource);
									if(!hasDef && !hasValueSet){
										this.no_SynForContributingSource.put(code, contributingSource);
									}
								}
							}
						}
					}
				}
				for (int j=0; j<fullsyn_lines.size(); j++) {
					String syn = (String) fullsyn_lines.elementAt(j);
					Vector u = StringUtils.parseData(syn, '|');
					String termname = (String) u.elementAt(2);
					final String fullSyn = concatenateFullSyn(syn);
					final String synSource = synMap.get(fullSyn);
					if (!(synSource.equals("NCI"))) {
						if (!(synSource.equals("")) && !this.ignoreSources.contains(synSource)) {
							boolean found = false;
							if (contributingSources != null) {
								for (int k=0; k<contributingSources.size(); k++) {
									String contributingSource = (String) contributingSources.elementAt(k);
									if (synSource.contains(contributingSource)) {
										found = true;
									}
								}
								if (!found) {
									this.no_ContributingSourceForSyn.put(code, termname + "|" + synSource);
								}
							}
						}
					}
				}
			}
		}
	}


	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String owlfile = args[0];
		ProtegeKBQA qaRun = new ProtegeKBQA();
		qaRun.configure(args);
		qaRun.performQA();
        System.out.println("\tTotal run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

