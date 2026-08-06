package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import gov.nih.nci.evs.restapi.config.*;
import gov.nih.nci.evs.restapi.bean.*;
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
import java.text.*;
import java.util.*;

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

public class OWL2MEME {
	static String ANNOTATION_DECLARATION = "annotationDeclaration";
	static String CLASS_DECLARATION = "classDeclaration";
	static String DATATYPE_DECLARATION = "datatypeDeclaration";
	static String HIERARCHY_DECLARATION = "hierarchy";
	static String OBJECT_DECLARATION = "objectDeclaration";
	static String RELATION_DECLARATION = "relationsDeclaration";

	static String ANNOTATION_DECLARATION_FILE = "annotationDeclaration" + "-" + getTimeStamp() + ".txt";
	static String CLASS_DECLARATION_FILE  = "classDeclaration" + "-" + getTimeStamp() + ".txt";
	static String DATATYPE_DECLARATION_FILE  = "datatypeDeclaration" + "-" + getTimeStamp() + ".txt";
	static String HIERARCHY_DECLARATION_FILE  = "hierarchy" + "-" + getTimeStamp() + ".txt";
	static String OBJECT_DECLARATION_FILE  = "objectDeclaration" + "-" + getTimeStamp() + ".txt";
	static String RELATION_DECLARATION_FILE  = "relationsDeclaration" + "-" + getTimeStamp() + ".txt";

	Vector owl_vec = null;
    String owlfile = null;
    OWLScanner owlscanner = null;

    public OWL2MEME(String owlfile) {
		this.owlfile = owlfile;
		this.owlscanner = new OWLScanner(owlfile);
		this.owl_vec = owlscanner.get_owl_vec();
	}

    public static String getTimeStamp() {
		return StringUtils.getToday("yyyyMMdd");
	}

    public Vector generateAnnotationDeclaration() {
		Vector v = owlscanner.extractAnnotationPropertyRanges();
		Vector w =new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			w.add((String) u.elementAt(0) + "|rdfs:label|" + (String) u.elementAt(1));
			w.add((String) u.elementAt(0) + "|rdfs:range|" + (String) u.elementAt(2));
		}
		return w;
	}

    public Vector generateObjectDeclaration() {
		Vector w = new Vector();
		//OWLScanner owlscanner = new OWLScanner(owlfile);
		Vector v = owlscanner.extractObjectProperties(owl_vec);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			w.add((String) u.elementAt(0) + "|rdfs:label|" + (String) u.elementAt(1));
		}
		return w;
	}

	//// Datatypes
    public Vector generateDatatypesDeclaration() {
		Vector w = owlscanner.extractEnumDataTypes(owl_vec);
		Vector w0 = new Vector();
		for (int i=0; i<w.size(); i++) {
			String dataType = (String) w.elementAt(i);
			Vector v = owlscanner.extractEnum(owlscanner.get_owl_vec(), dataType);
			v = new SortUtils().quickSort(v);
			StringBuffer buf = new StringBuffer();
			buf.append(dataType);
			for (int j=0; j<v.size(); j++) {
				String s = (String) v.elementAt(j);
				buf.append("|").append(s);
			}
			String t = buf.toString();
			w0.add(t);
		}
		return w0;
	}

	public Vector addEmptyField(Vector v) { // relations
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			w.add((String) u.elementAt(0) + "||" + (String) u.elementAt(1) + "|" + (String) u.elementAt(2));
		}
		return w;
	}

    public Vector generateRelationDeclaration() {
		Vector w = new Vector();
		w = owlscanner.extractOWLRestrictions(owl_vec);
		w = addEmptyField(w);
		return w;
	}

    public Vector generateHierarchyDeclaration() {
		Vector w = new Vector();
		w = owlscanner.extractHierarchicalRelationships(owl_vec);
		return w;
	}

	public static Vector removeLabels(Vector v) {
		Vector w = new Vector();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			int n = line.indexOf("|");
			w.add(line.substring(n+1, line.length()));
		}
		return w;
	}

	public Vector sortClassData(Vector v) {
		Vector ids = owlscanner.extractClassIDs(owlscanner.get_owl_vec());
		SortUtils sort = new SortUtils();
		HashMap hmap = new HashMap();
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String id = (String) u.elementAt(0);
			Vector w = new Vector();
			if (hmap.containsKey(id)) {
				w = (Vector) hmap.get(id);
			}
			w.add(line);
			hmap.put(id, w);
		}
		Vector w1 = new Vector();
		for (int i=0; i<ids.size(); i++) {
			String id = (String) ids.elementAt(i);
			Vector w = (Vector) hmap.get(id);
			w = sort.quickSort(w);
			w1.addAll(w);
		}
		return w1;
	}

    public Vector generateClassDeclaration() {
		Vector w = new Vector();
		Vector v = owlscanner.extractAxiomData(null);
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			line = line.replace("$", "|");
			w.add(line);
		}
		w = removeLabels(w);
        Vector w1 = owlscanner.extractProperties(owlscanner.get_owl_vec());
        w.addAll(w1);
        return sortClassData(w);
	}

	public void generate() {
		Vector w = null;
	    System.out.println("Generating " + CLASS_DECLARATION_FILE);
	    w = generateClassDeclaration();
	    Utils.saveToFile(CLASS_DECLARATION_FILE, w);
	    w.clear();

	    System.out.println("Generating " + ANNOTATION_DECLARATION_FILE);
	    w = generateAnnotationDeclaration();
	    Utils.saveToFile(ANNOTATION_DECLARATION_FILE, w);
	    w.clear();

        System.out.println("Generating " + OBJECT_DECLARATION_FILE);
        w = generateObjectDeclaration();
        Utils.saveToFile(OBJECT_DECLARATION_FILE, w);
        w.clear();

        System.out.println("Generating " + DATATYPE_DECLARATION_FILE);
	    w = generateDatatypesDeclaration();
	    Utils.saveToFile(DATATYPE_DECLARATION_FILE, w);
	    w.clear();

	    System.out.println("Generating " + HIERARCHY_DECLARATION_FILE);
        w = generateHierarchyDeclaration();
        Utils.saveToFile(HIERARCHY_DECLARATION_FILE, w);
        w.clear();

	    System.out.println("Generating " + RELATION_DECLARATION_FILE);
        w = generateRelationDeclaration();
        Utils.saveToFile(RELATION_DECLARATION_FILE, w);
        w.clear();
	}


}
