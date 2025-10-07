package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import java.text.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2022 Guidehouse. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by Guidehouse and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "Guidehouse" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or GUIDEHOUSE
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      GUIDEHOUSE, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@nih.gov
 *
 */

public class CTCAE2OWL extends BasicSPARQLUtils {
    String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;

    String NCIT_ID = "NHC0";
    String named_graph_id = ":NHC0";
    String NCIT_NS = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
    String CTCAE6_NS = "http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl";
    String CTCAE6_ROOT = "C220612";
    String RELEASE_DATE = getToday();

    String username = null;
    String password = null;
    String root = null;
    HashMap code2LabelMap = null;

    HashMap code2NCIPTMap = null;
    HashMap code2CTCAEPTMap = null;

    HashMap parentMap = null;
    HashMap preferredNameMap = null;
    HashMap fullsynMap = null;
    HashMap defMap = null;

    String PROPERTY_FILE = "ctcae2owl.properties";
    PropertiesReader propertiesReader = null;

    HashMap altDefMap = null;
    HashMap mapsToMap = null;

    static String CTCAE6_DESCRIPTION = "Common Terminology Criteria for Adverse Events (CTCAE) is widely accepted throughout the oncology community as the standard classification and severity grading scale for adverse events in cancer therapy clinical trials and other oncology settings. Version 6 was released by the NCI Cancer Therapy Evaluation Program (CTEP) in 2025. It is organized by MedDRA System Organ Class and mapped to MedDRA LLTs with corresponding MedDRA codes, and harmonized with MedDRA at the Adverse Event (AE) level including revised AE terms and severity indicators to reflect clinical effects identified with current oncology interventions.Severity grades are assigned and most are defined to clarify the meaning of the term. CTCAE is designed to integrate into information networks for safety data exchange, and is the primary standard for data management for AE data collection, analysis, and patient outcomes associated with cancer research and care.";

    public CTCAE2OWL(String serviceUrl, String named_graph, String username, String password) {
		super(serviceUrl, named_graph, username, password);
		this.named_graph = named_graph;
		File f = new File(PROPERTY_FILE);
		if (f.exists()) {
			this.propertiesReader = new PropertiesReader(PROPERTY_FILE);
		}
        initialize(CTCAE6_ROOT);
    }

    public void initialize(String root) {
		long ms = System.currentTimeMillis();
		this.root = root;
		code2LabelMap = new HashMap();
		boolean codeOnly = false;
		System.out.println("getConceptsInSubset...");
		Vector v = getConceptsInSubset(named_graph, root, codeOnly);
		System.out.println("Subset size: " + v.size());
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			String label = (String) u.elementAt(0);
			label = encodeHTML(label);
			code2LabelMap.put(code, label);
		}

		System.out.println("getParents...");
		parentMap = new HashMap();
		v = getParents(named_graph, root);
		v = encodeHTML(v);
		//Utils.saveToFile("parents.txt", v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			String parentCode = (String) u.elementAt(3);
			Vector w = new Vector();
			if (parentMap.containsKey(code)) {
				w = (Vector) parentMap.get(code);
			}
			if (!w.contains(parentCode)) {
				w.add(parentCode);
			}
			parentMap.put(code, w);
		}

/////////////////////////////////////////////////////////////////////////////////////////////

		System.out.println("getPreferredNames...");
		preferredNameMap = new HashMap();
		String prop_code = "P108";
		v = getPreferredNames(named_graph, root, prop_code);
		v = encodeHTML(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			String preferredName = (String) u.elementAt(3);
			Vector w = new Vector();
			if (preferredNameMap.containsKey(code)) {
				w = (Vector) preferredNameMap.get(code);
			}
			if (!w.contains(preferredName)) {
				w.add(preferredName);
			}
			preferredNameMap.put(code, w);
		}

		System.out.println("getAxiomFullsyns...");
        fullsynMap = new HashMap();
		v = getAxiomFullsyns(named_graph, root);
		v = encodeHTML(v);

		HashMap code2NCIPTMap = new HashMap();
		HashMap code2CTCAEPTMap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			String termName = (String) u.elementAt(2);
			String termSource = (String) u.elementAt(4);
			String termType = (String) u.elementAt(6);
			Vector w = new Vector();
			if (fullsynMap.containsKey(code)) {
				w = (Vector) fullsynMap.get(code);
			}
			if (termSource.compareTo("CTCAE 6.0") == 0 || (termSource.compareTo("NCI") == 0 && termType.compareTo("PT") == 0)) {
					w.add(termName + "|" + termSource + "|" + termType);
			}

			if (termSource.compareTo("CTCAE 6.0") == 0 && termType.compareTo("PT") == 0) {
				code2CTCAEPTMap.put(code, termName);
			} else if (termSource.compareTo("NCI") == 0 && termType.compareTo("PT") == 0) {
				code2NCIPTMap.put(code, termName);
			}
			fullsynMap.put(code, w);
		}

		if (propertiesReader != null) {
			String rdfsLabel_config = propertiesReader.getProperty("rdfs:label");
			if (rdfsLabel_config.compareTo("P90|P384$CTCAE 6.0|P383$PT") == 0) {
				code2LabelMap = code2CTCAEPTMap;
			} else if (rdfsLabel_config.compareTo("P90|P384$NCI|P383$PT") == 0) {
				code2LabelMap = code2NCIPTMap;
			}
			String preferredName_config = propertiesReader.getProperty("Preferred_Name");
			if (preferredName_config.compareTo("P90|P384$CTCAE 6.0|P383$PT") == 0) {
				preferredNameMap = code2CTCAEPTMap;
			} else if (rdfsLabel_config.compareTo("P90|P384$NCI|P383$PT") == 0) {
				preferredNameMap = code2NCIPTMap;
			}
		}

        System.out.println("getAxiomDef...");
        defMap = new HashMap();
        v = getAxiomDef(named_graph, root);
        v = encodeHTML(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			String def = (String) u.elementAt(2);
			String def_src = (String) u.elementAt(4);
			Vector w = new Vector();
			if (defMap.containsKey(code)) {
				w = (Vector) defMap.get(code);
			}
			w.add(def + "|" + def_src);
			defMap.put(code, w);
		}

        System.out.println("getAxiomAltdef...");
        altDefMap = new HashMap();
        v = getAxiomAltdef(named_graph, root);
        v = encodeHTML(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			String def = (String) u.elementAt(2);
			String def_src = (String) u.elementAt(4);
			if (def_src.compareTo("CTCAE 6.0") == 0) {
				Vector w = new Vector();
				if (altDefMap.containsKey(code)) {
					w = (Vector) altDefMap.get(code);
				}
				w.add(def + "|" + def_src);
				altDefMap.put(code, w);
			}
		}

        System.out.println("getAxiomMapsto...");
        mapsToMap = new HashMap();
        v = getAxiomMapsto(named_graph, root);
        v = encodeHTML(v);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = parseData(line, '|');
			String code = (String) u.elementAt(1);
			String target = (String) u.elementAt(2);
			String p393 = (String) u.elementAt(4);
			String p394 = (String) u.elementAt(6);
			String p395 = (String) u.elementAt(8);
			String p396 = (String) u.elementAt(10);
			String p397 = (String) u.elementAt(12);

			if (p397.compareTo("28.0") == 0) {
				Vector w = new Vector();
				if (mapsToMap.containsKey(code)) {
					w = (Vector) mapsToMap.get(code);
				}
				w.add(target + "|" + p393 + "|" + p394 + "|" + p395 + "|" + p396 + "|" + p397);
				mapsToMap.put(code, w);
			}
		}
		System.out.println("Total initialization run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public Vector getConceptsInSubset(String named_graph, String code) {
		return getConceptsInSubset(named_graph, code, false);
	}

	public Vector getConceptsInSubset(String named_graph, String code, boolean codeOnly) {
		String query = construct_get_concepts_in_subset(named_graph, code, codeOnly);
		//System.out.println(query);
        Vector v = executeQuery(query);
        if (v == null) return null;
        if (v.size() == 0) return v;
        return new SortUtils().quickSort(v);
	}

	public String construct_get_concepts_in_subset(String named_graph, String subset_code, boolean codeOnly) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		if (codeOnly) {
			buf.append("SELECT ?x_code").append("\n");
		} else {
			buf.append("SELECT ?x_label ?x_code").append("\n");
	    }
		buf.append("{").append("\n");
		if (named_graph != null) {
			buf.append("    graph <" + named_graph + ">").append("\n");
		}

		buf.append("    {").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("            ?x " + named_graph_id + " ?x_code .").append("\n");
		buf.append("            ?y a owl:AnnotationProperty .").append("\n");
		buf.append("            ?x ?y ?z .").append("\n");
		buf.append("            ?z " + named_graph_id + " \"" + subset_code + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label " + "\"" + "Concept_In_Subset" + "\"^^xsd:string ").append("\n");
		buf.append("    }").append("\n");
		buf.append("}").append("\n");
		return buf.toString();
	}

	public String construct_get_parents(String named_graph, String subsetcode) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("SELECT distinct ?x_label ?x_code ?z_label ?z_code").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y :NHC0 \"" + subsetcode + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("            ?z a owl:Class .").append("\n");
		buf.append("            ?z :NHC0 ?z_code .").append("\n");
		buf.append("            ?z rdfs:label ?z_label .  ").append("\n");
		buf.append("            ?z ?p ?y .").append("\n");
		buf.append("            ?x rdfs:subClassOf ?z .").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getParents(String named_graph, String subsetcode) {
		String query = construct_get_parents(named_graph, subsetcode);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		return new SortUtils().quickSort(v);
	}


	public String construct_get_preferrednames(String named_graph, String subsetcode, String prop_code) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?p2_code ?p2_value ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y :NHC0 \"" + subsetcode + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append("                ?p2 :NHC0 ?p2_code .").append("\n");
		buf.append("                ?p2 :NHC0 \"" + prop_code + "\"^^xsd:string .").append("\n");
		buf.append("                ?x ?p2 ?p2_value .").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getPreferredNames(String named_graph, String subsetcode, String prop_code) {
		String query = construct_get_preferrednames(named_graph, subsetcode, prop_code);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		return new SortUtils().quickSort(v);
	}


	public String construct_get_axiom_fullsyns(String named_graph, String subsetcode) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?a_target ?q1_label ?q1_value ?q2_label ?q2_value").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y :NHC0 \"" + subsetcode + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("            ").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?p2 :NHC0 \"P90\"^^xsd:string .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a_target .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q1 :NHC0 \"P384\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q2 :NHC0 \"P383\"^^xsd:string .  ").append("\n");
		buf.append("                ?q2 rdfs:label ?q2_label . ").append("\n");
		buf.append("                ?a1 ?q2 ?q2_value .").append("\n");
		buf.append("").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getAxiomFullsyns(String named_graph, String subsetcode) {
		String query = construct_get_axiom_fullsyns(named_graph, subsetcode);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		return new SortUtils().quickSort(v);
	}

	public String construct_get_axiom_def(String named_graph, String subsetcode) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?a_target ?q1_label ?q1_value ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y :NHC0 \"" + subsetcode + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?p2 :NHC0 \"P97\"^^xsd:string .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a_target .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q1 :NHC0 \"P378\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getAxiomDef(String named_graph, String subsetcode) {
		String query = construct_get_axiom_def(named_graph, subsetcode);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		return new SortUtils().quickSort(v);
	}

	public String construct_get_axiom_altdef(String named_graph, String subsetcode) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?a_target ?q1_label ?q1_value ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y :NHC0 \"" + subsetcode + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("            	").append("\n");
		buf.append("            	").append("\n");
		buf.append("            	").append("\n");
		buf.append("").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append(" ").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?p2 :NHC0 \"P325\"^^xsd:string .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a_target .").append("\n");
		buf.append("").append("\n");
		buf.append("                ?q1 :NHC0 \"P378\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getAxiomAltdef(String named_graph, String subsetcode) {
		String query = construct_get_axiom_altdef(named_graph, subsetcode);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		return new SortUtils().quickSort(v);
	}

	public String construct_get_axiom_mapsto(String named_graph, String subsetcode) {
		String prefixes = getPrefixes();
		StringBuffer buf = new StringBuffer();
		buf.append(prefixes);
		buf.append("select distinct ?x_label ?x_code ?a_target ?q1_label ?q1_value ?q2_label ?q2_value ?q3_label ?q3_value ?q4_label ?q4_value ?q5_label ?q5_value  ").append("\n");
		buf.append("from <" + named_graph + ">").append("\n");
		buf.append("where  { ").append("\n");
		buf.append("            ?x a owl:Class .").append("\n");
		buf.append("            ?x :NHC0 ?x_code .").append("\n");
		buf.append("            ?x rdfs:label ?x_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?y a owl:Class .").append("\n");
		buf.append("            ?y :NHC0 ?y_code .").append("\n");
		buf.append("            ?y :NHC0 \"" + subsetcode + "\"^^xsd:string .").append("\n");
		buf.append("            ?y rdfs:label ?y_label .").append("\n");
		buf.append("").append("\n");
		buf.append("            ?x ?p ?y .").append("\n");
		buf.append("            ?p rdfs:label ?p_label .").append("\n");
		buf.append("            ?p rdfs:label \"Concept_In_Subset\"^^xsd:string .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?p2 a owl:AnnotationProperty .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?a1 a owl:Axiom .").append("\n");
		buf.append("                ?a1 owl:annotatedSource ?x .").append("\n");
		buf.append("                ?a1 owl:annotatedProperty ?p2 .").append("\n");
		buf.append("                ?p2 :NHC0 \"P375\"^^xsd:string .").append("\n");
		buf.append("                ?a1 owl:annotatedTarget ?a_target .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?q1 :NHC0 \"P393\"^^xsd:string .").append("\n");
		buf.append("                ?q1 rdfs:label ?q1_label .").append("\n");
		buf.append("                ?a1 ?q1 ?q1_value .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?q2 :NHC0 \"P394\"^^xsd:string .").append("\n");
		buf.append("                ?q2 rdfs:label ?q2_label .").append("\n");
		buf.append("                ?a1 ?q2 ?q2_value .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?q3 :NHC0 \"P395\"^^xsd:string .").append("\n");
		buf.append("                ?q3 rdfs:label ?q3_label .").append("\n");
		buf.append("                ?a1 ?q3 ?q3_value .").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?q4 :NHC0 \"P396\"^^xsd:string .").append("\n");
		buf.append("                ?q4 rdfs:label ?q4_label .").append("\n");
		buf.append("                ?a1 ?q4 ?q4_value .    ").append("\n");
		buf.append("                ").append("\n");
		buf.append("                ?q5 :NHC0 \"P397\"^^xsd:string .").append("\n");
		buf.append("                ?q5 rdfs:label ?q5_label .").append("\n");
		buf.append("                ?a1 ?q5 ?q5_value .                ").append("\n");
		buf.append("                ").append("\n");
		buf.append("}").append("\n");
		buf.append("").append("\n");
		buf.append("").append("\n");
		return buf.toString();
	}


	public Vector getAxiomMapsto(String named_graph, String subsetcode) {
		String query = construct_get_axiom_mapsto(named_graph, subsetcode);
		Vector v = executeQuery(query);
		if (v == null) return null;
		if (v.size() == 0) return v;
		return new SortUtils().quickSort(v);
	}

    public HashMap getClassData(String code) {
		//System.out.println("\ncode: " + code);
        HashMap dataMap = new HashMap();
        //System.out.println("label: " + (String) code2LabelMap.get(code));
        dataMap.put("LABEL", (String) code2LabelMap.get(code));

        //Utils.dumpVector("parents: ", (Vector) parentMap.get(code));
        dataMap.put("PARENT", (Vector) parentMap.get(code));

        //Utils.dumpVector("PREFERRED_NAME: ", (Vector) preferredNameMap.get(code));
        dataMap.put("PREFERRED_NAME", (Vector) preferredNameMap.get(code));

        //Utils.dumpVector("FULL_SYN: ", (Vector) fullsynMap.get(code));
        dataMap.put("FULL_SYN", (Vector) fullsynMap.get(code));

        //Utils.dumpVector("DEFINITION: ", (Vector) defMap.get(code));
        dataMap.put("DEFINITION", (Vector) defMap.get(code));

        //Utils.dumpVector("ALT_DEFINITION: ", (Vector) altDefMap.get(code));
        dataMap.put("ALT_DEFINITION", (Vector) altDefMap.get(code));

        //Utils.dumpVector("MAPS_TO: ", (Vector) mapsToMap.get(code));
        dataMap.put("MAPS_TO", (Vector) mapsToMap.get(code));

        return dataMap;
	}

	public void printHeader(PrintWriter out) {
		out.println("<?xml version=\"1.0\"?>");
		out.println("<rdf:RDF xmlns=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#\"");
		out.println("     xml:base=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl\"");
		out.println("     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
		out.println("     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"");
		out.println("     xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"");
		out.println("     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
		out.println("     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
		out.println("     xmlns:ncit=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#\"");
		out.println("     xmlns:protege=\"http://protege.stanford.edu/plugins/owl/protege#\"");
		out.println("     xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");

		out.println("    <owl:Ontology rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl\">");
		out.println("        <dc:date>" + RELEASE_DATE + "</dc:date>");
		out.println("        <owl:versionInfo>6.0</owl:versionInfo>");
		out.println("        <protege:defaultLanguage>en</protege:defaultLanguage>");
		out.println("        <rdfs:comment>" + CTCAE6_DESCRIPTION + "</rdfs:comment>");
		out.println("    </owl:Ontology>");
		out.println("    ");
	}

	public void run(PrintWriter out) {
		printHeader(out);
		printContent(out);
		printFooter(out);
	}

	public void printAnnotationProperties(PrintWriter out) {
		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Annotation properties");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("");

		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P108 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P108\">");
		out.println("        <NHC0>P108</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P107>Preferred Name</ncit:P107>");
		out.println("        <ncit:P108>Preferred_Name</ncit:P108>");
		out.println("        <ncit:P90>Preferred Name</ncit:P90>");
		out.println("        <ncit:P90>Preferred Term</ncit:P90>");
		out.println("        <ncit:P90>Preferred_Name</ncit:P90>");
		out.println("        <ncit:P97>A property representing the word or phrase that NCI uses by preference to refer to the concept.</ncit:P97>");
		out.println("        <rdfs:label>Preferred_Name</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://www.w3.org/2001/XMLSchema#string\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P108\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Preferred Name</owl:annotatedTarget>");
		out.println("        <ncit:P383>SY</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P108\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Preferred Term</owl:annotatedTarget>");
		out.println("        <ncit:P383>SY</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P108\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Preferred_Name</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P108\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing the word or phrase that NCI uses by preference to refer to the concept.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\">");
		out.println("        <NHC0>P90</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P107>Term &amp; Source Data</ncit:P107>");
		out.println("        <ncit:P108>FULL_SYN</ncit:P108>");
		out.println("        <ncit:P90>FULL_SYN</ncit:P90>");
		out.println("        <ncit:P90>Synonym with Source Data</ncit:P90>");
		out.println("        <ncit:P97>A property representing a fully qualified synonym, contains the string, term type, source, and an optional source code if appropriate. Each subfield is deliniated to facilitate interpretation by software.</ncit:P97>");
		out.println("        <rdfs:label>FULL_SYN</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#textArea\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>FULL_SYN</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Synonym with Source Data</owl:annotatedTarget>");
		out.println("        <ncit:P383>SY</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing a fully qualified synonym, contains the string, term type, source, and an optional source code if appropriate. Each subfield is deliniated to facilitate interpretation by software.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\">");
		out.println("        <NHC0>P97</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P107>Definition</ncit:P107>");
		out.println("        <ncit:P108>DEFINITION</ncit:P108>");
		out.println("        <ncit:P90>DEFINITION</ncit:P90>");
		out.println("        <ncit:P97>A property representing the English language definitions of what NCI means by the concept. They may also include information about the definition&apos;s source and attribution in a form that can easily be interpreted by software.</ncit:P97>");
		out.println("        <rdfs:label>DEFINITION</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#textArea\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>DEFINITION</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing the English language definitions of what NCI means by the concept. They may also include information about the definition&apos;s source and attribution in a form that can easily be interpreted by software.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P325 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P325\">");
		out.println("        <NHC0>P325</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P107>[source] Definition</ncit:P107>");
		out.println("        <ncit:P108>ALT_DEFINITION</ncit:P108>");
		out.println("        <ncit:P90>ALT_DEFINITION</ncit:P90>");
		out.println("        <ncit:P97>A property representing the English language definition of a concept from a source other than NCI.</ncit:P97>");
		out.println("        <rdfs:label>ALT_DEFINITION</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#textArea\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P325\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>ALT_DEFINITION</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P325\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing the English language definition of a concept from a source other than NCI.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P375 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P375\">");
		out.println("        <NHC0>P375</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Maps_To</ncit:P108>");
		out.println("        <ncit:P90>Maps_To</ncit:P90>");
		out.println("        <ncit:P97>A property representing that a term in another terminology has been mapped to a term in NCIt and describes the relationship between the mapped terms.</ncit:P97>");
		out.println("        <rdfs:label>Maps_To</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#textArea\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P375\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Maps_To</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P375\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing that a term in another terminology has been mapped to a term in NCIt and describes the relationship between the mapped terms.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P383 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P383\">");
		out.println("        <NHC0>P383</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>term-group</ncit:P108>");
		out.println("        <ncit:P97>A property representing a two or three character abbreviation that indicates the nature of each FULL_SYN term associated with a concept. (e.g., PT=Preferred Term; SY=Synonym)</ncit:P97>");
		out.println("        <required rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</required>");
		out.println("        <rdfs:label>Term Type</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#term-group-enum\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P384 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P384\">");
		out.println("        <NHC0>P384</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>term-source</ncit:P108>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("        <ncit:P97>A property representing the organization that is the supplier or owner of each FULL_SYN term.</ncit:P97>");
		out.println("        <required rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</required>");
		out.println("        <rdfs:label>Term Source</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#term-source-enum\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P378 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P378\">");
		out.println("        <NHC0>P378</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Definition Source</ncit:P108>");
		out.println("        <ncit:P90>Definition Source</ncit:P90>");
		out.println("        <ncit:P97>A property representing the organization that is the supplier or owner of each DEFINITION or ALT_DEFINITION associated with a concept.</ncit:P97>");
		out.println("        <required rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</required>");
		out.println("        <rdfs:label>Definition Source</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#def-source-enum\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P378\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Definition Source</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P378\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing the organization that is the supplier or owner of each DEFINITION or ALT_DEFINITION associated with a concept.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P393 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P393\">");
		out.println("        <NHC0>P393</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Relationship_to_Target</ncit:P108>");
		out.println("        <ncit:P90>Relationship_to_Target</ncit:P90>");
		out.println("        <ncit:P97>A property representing the relationship of the NCI Thesaurus (NCIt) concept to the term from the outside source. There are four possibilities: 1) Has Synonym: the two terms are synonymous; 2) Broader Than: the NCIt concept is broader than the mapped (target) term; 3) Narrower Than: the NCIt concept is narrower than the mapped (target) term; 4) Related To: the NCIt concept is somehow related to the mapped (target) term.</ncit:P97>");
		out.println("        <required rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</required>");
		out.println("        <rdfs:label>Relationship_to_Target</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Relationshipt_to_Target-enum\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P393\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Relationship_to_Target</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P393\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing the relationship of the NCI Thesaurus (NCIt) concept to the term from the outside source. There are four possibilities: 1) Has Synonym: the two terms are synonymous; 2) Broader Than: the NCIt concept is broader than the mapped (target) term; 3) Narrower Than: the NCIt concept is narrower than the mapped (target) term; 4) Related To: the NCIt concept is somehow related to the mapped (target) term.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P394 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P394\">");
		out.println("        <NHC0>P394</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Target_Term_Type</ncit:P108>");
		out.println("        <ncit:P90>Target_Term_Type</ncit:P90>");
		out.println("        <ncit:P97>A property representing the term type designation in the mapped (target) database.</ncit:P97>");
		out.println("        <required rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</required>");
		out.println("        <rdfs:label>Target_Term_Type</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#Target_Term_Type-enum\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P394\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Target_Term_Type</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P394\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing the term type designation in the mapped (target) database.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P395 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P395\">");
		out.println("        <NHC0>P395</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Target_Code</ncit:P108>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("        <ncit:P90>Target_Code</ncit:P90>");
		out.println("        <ncit:P97>A property representing the code assigned to the target term in the mapped (target) database. Some databases may not have codes.</ncit:P97>");
		out.println("        <required rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</required>");
		out.println("        <rdfs:label>Target_Code</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://www.w3.org/2001/XMLSchema#string\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P395\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Target_Code</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P395\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing the code assigned to the target term in the mapped (target) database. Some databases may not have codes.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P386 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P386\">");
		out.println("        <NHC0>P386</NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Subsource Name</ncit:P108>");
		out.println("        <ncit:P90>Subsource Name</ncit:P90>");
		out.println("        <ncit:P90>subsource-name</ncit:P90>");
		out.println("        <ncit:P97>A property indicating that a subgroup within the term-source is the supplier or owner of a FULL_SYN term.</ncit:P97>");
		out.println("        <rdfs:label>Subsource Name</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://www.w3.org/2001/XMLSchema#string\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P386\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>Subsource Name</owl:annotatedTarget>");
		out.println("        <ncit:P383>PT</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P386\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget>subsource-name</owl:annotatedTarget>");
		out.println("        <ncit:P383>SY</ncit:P383>");
		out.println("        <ncit:P384>NCI</ncit:P384>");
		out.println("    </owl:Axiom>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P386\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property indicating that a subgroup within the term-source is the supplier or owner of a FULL_SYN term.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("    </owl:Axiom>");
		out.println("    ");
		out.println("");
		out.println("");
		out.println("");
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P387 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P387\">");
		out.println("        <NHC0>P387</NHC0>");
		out.println("        <ncit:P108>go-id</ncit:P108>");
		out.println("        <ncit:P97>A property representing a unique zero-padded seven digit identifier supplied by the Gene Ontology (GO) that has no inherent meaning or relation to the position of the term in GO and is prefixed by &quot;GO:&quot;.</ncit:P97>");
		out.println("        <required rdf:datatype=\"http://www.w3.org/2001/XMLSchema#boolean\">true</required>");
		out.println("        <rdfs:label>go-id</rdfs:label>");
		out.println("        <rdfs:range rdf:resource=\"http://www.w3.org/2001/XMLSchema#string\"/>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P387\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget>A property representing a unique zero-padded seven digit identifier supplied by the Gene Ontology (GO) that has no inherent meaning or relation to the position of the term in GO and is prefixed by &quot;GO:&quot;.</owl:annotatedTarget>");
		out.println("        <ncit:P378>NCI</ncit:P378>");
		out.println("        <ncit:P381>http://www.geneontology.org/page/ontology-structure</ncit:P381>");
		out.println("    </owl:Axiom>");
		out.println("    ");
	}

	public void printContent(PrintWriter out) {
		printAnnotationProperties(out);
        out.println("");
		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Classes");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("");

		Iterator it = code2LabelMap.keySet().iterator();
		while (it.hasNext()) {
			String code = (String) it.next();
			//System.out.println("printClassData: " + code);
			try {
				printClassData(out, code);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void printClassData(PrintWriter out, String code) {
		HashMap dataMap = getClassData(code);
		out.println("");
		out.println("    <!-- http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#" + code + " -->");
		out.println("");
		out.println("    <owl:Class rdf:about=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#" + code + "\">");
        Vector parents = (Vector) dataMap.get("PARENT");
        if (parents != null) {
			for (int i=0; i<parents.size(); i++) {
				String parentCode = (String) parents.elementAt(i);
                out.println("\t<rdfs:subClassOf rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#" + parentCode + "\"/>");
			}
		}
		out.println("        <ncit:NHC0>" + code + "</ncit:NHC0>");
        Vector preferredNames = (Vector) dataMap.get("PREFERRED_NAME");
        if (preferredNames != null) {
			for (int i=0; i<preferredNames.size(); i++) {
				String preferredName = (String) preferredNames.elementAt(i);
				out.println("        <ncit:P108>" + preferredName + "</ncit:P108>");
			}
		}
        Vector fullsyns = (Vector) dataMap.get("FULL_SYN");
        if (fullsyns != null) {
			for (int i=0; i<fullsyns.size(); i++) {
				String line = (String) fullsyns.elementAt(i);
				Vector u = parseData(line, '|');
				String termName = (String) u.elementAt(0);
				out.println("        <ncit:P90>" + termName + "</ncit:P90>");
			}
		}
        Vector defs = (Vector) dataMap.get("DEFINITION");
        if (defs != null) {
			for (int i=0; i<defs.size(); i++) {
				String line = (String) defs.elementAt(i);
				Vector u = parseData(line, '|');
				String def = (String) u.elementAt(0);
				out.println("        <ncit:P97>" + def + "</ncit:P97>");
			}
		}
        Vector altdefs = (Vector) dataMap.get("ALT_DEFINITION");
        if (altdefs != null) {
			for (int i=0; i<altdefs.size(); i++) {
				String line = (String) altdefs.elementAt(i);
				Vector u = parseData(line, '|');
				String altdef = (String) u.elementAt(0);
				out.println("        <ncit:P325>" + altdef + "</ncit:P325>");
			}
		}
        Vector mapsTos = (Vector) dataMap.get("MAPS_TO");
        if (mapsTos != null) {
			for (int i=0; i<mapsTos.size(); i++) {
				String line = (String) mapsTos.elementAt(i);
				Vector u = parseData(line, '|');
				String target = (String) u.elementAt(0);
				out.println("        <ncit:P375>" + target + "</ncit:P375>");
			}
		}

		String label = (String) code2LabelMap.get(code);
		//out.println("        <rdfs:label rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + label + "</rdfs:label>");
		out.println("        <rdfs:label>" + label + "</rdfs:label>");
		out.println("    </owl:Class>");
		out.println("");

        if (fullsyns != null) {
			for (int i=0; i<fullsyns.size(); i++) {
				String line = (String) fullsyns.elementAt(i);
				Vector u = parseData(line, '|');
				String termName = (String) u.elementAt(0);
				String termSource = (String) u.elementAt(1);
				String termType = (String) u.elementAt(2);

		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#" + code + "\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P90\"/>");
		out.println("        <owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + termName + "</owl:annotatedTarget>");
		out.println("        <ncit:P383 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + termType + "</ncit:P383>");
		out.println("        <ncit:P384 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + termSource + "</ncit:P384>");
		out.println("    </owl:Axiom>");

			}
		}

        if (defs != null) {
			for (int i=0; i<defs.size(); i++) {
				String line = (String) defs.elementAt(i);
				Vector u = parseData(line, '|');
				String def = (String) u.elementAt(0);
				String def_src = (String) u.elementAt(1);

		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#" + code + "\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P97\"/>");
		out.println("        <owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + def + "</owl:annotatedTarget>");
		out.println("        <ncit:P378 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + def_src + "</ncit:P378>");
		out.println("    </owl:Axiom>");

			}
		}

        if (altdefs != null) {
			for (int i=0; i<altdefs.size(); i++) {
				String line = (String) altdefs.elementAt(i);
				Vector u = parseData(line, '|');
				String def = (String) u.elementAt(0);
				String def_src = (String) u.elementAt(1);

		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#" + code + "\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P325\"/>");
		out.println("        <owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + def + "</owl:annotatedTarget>");
		out.println("        <ncit:P378 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + def_src + "</ncit:P378>");
		out.println("    </owl:Axiom>");

			}
		}

        if (mapsTos != null) {
			for (int i=0; i<mapsTos.size(); i++) {
				String line = (String) mapsTos.elementAt(i);
				Vector u = parseData(line, '|');
				//			w.add(target + "|" + p393 + "|" + p394 + "|" + p395 + "|" + p396 + "|" + p397);
				String target = (String) u.elementAt(0);
				String p393 = (String) u.elementAt(1);
				String p394 = (String) u.elementAt(2);
				String p395 = (String) u.elementAt(3);
				String p396 = (String) u.elementAt(4);
				String p397 = (String) u.elementAt(5);

		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ctcae6.owl#" + code + "\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P375\"/>");
		out.println("        <owl:annotatedTarget rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + target + "</owl:annotatedTarget>");
		out.println("        <ncit:P393 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + p393 + "</ncit:P393>");
		out.println("        <ncit:P394 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + p394 + "</ncit:P394>");
		out.println("        <ncit:P395 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + p395 + "</ncit:P395>");
		out.println("        <ncit:P396 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + p396 + "</ncit:P396>");
		out.println("        <ncit:P397 rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">" + p397 + "</ncit:P397>");
		out.println("    </owl:Axiom>");
			}
		}


	}

	public void printFooter(PrintWriter out) {
        out.println("\n</rdf:RDF>");
	}

    public void run(String outputfile) {
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			run(pw);
		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static String encodeHTML(String line) {
		line = line.replace("<", "&lt;");
		line = line.replace(">", "&gt;");
		line = line.replace("&", "&amp;");
		line = line.replace("'", "&apos;");
		return line;
	}

	public static String getToday() {
		return getToday("MM-dd-yyyy");
	}

	public static String getToday(String format) {
		java.util.Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.format(date);
	}

	public static Vector encodeHTML(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = encodeHTML(line);
			w.add(line);
 		}
 		return w;
	}

    public static Vector parseData(String line, char delimiter) {
		if(line == null) return null;
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delimiter) {
				w.add(buf.toString());
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		w.add(buf.toString());
		return w;
	}

}



