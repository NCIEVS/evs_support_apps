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
import java.text.*;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2020 MSC. This software was developed in conjunction
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
 *      "This product includes software developed by MSC and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "MSC" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or MSC
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      MSC, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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


public class OWLDisjointWithScanner {
    static String TARGET = "<!-- http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#";
	public static String extractClassId(String line) {
		line = line.trim();
		int n1 = line.lastIndexOf("#");
		int n2 = line.lastIndexOf(" ");
		String id = line.substring(n1+1, n2);
		return id;
	}

	public static HashMap loadOWLDisjointWithData(Vector owl_vec) {
		HashMap disjointHashMap = new HashMap();
		Vector w = new Vector();
		String classId = null;
		for (int i=0; i<owl_vec.size(); i++) {
			String line = (String) owl_vec.elementAt(i);
			if (line.indexOf(TARGET) != -1) {
				if (classId != null && w.size() > 0) {
					disjointHashMap.put(classId, w);
				}
				classId = extractClassId(line);
				w = new Vector();
			}
			if (line.indexOf("<owl:disjointWith rdf:resource") != -1) {
				int n = line.lastIndexOf("#");
				String code = line.substring(n+1, line.length()-3);
				w.add(code);
			}
		}
		if (classId != null && w.size() > 0) {
			disjointHashMap.put(classId, w);
		}
		return disjointHashMap;
	}

	public static HashMap fixdOWLDisjointWithData(HashMap hmap) {
		HashMap map = new HashMap();
		Vector w = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			if (!w.contains(key)) {
				w.add(key);
			}
			Vector values = (Vector) hmap.get(key);
			for (int j=0; j<values.size(); j++) {
				String value = (String) values.elementAt(j);
				if (!w.contains(value)) {
					w.add(value);
				}
			}
		}
		map = new HashMap();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector v = (Vector) w.clone();
			v.remove(t);
            hmap.put(t, v);
		}
		return hmap;
	}

	public static HashMap run(Vector owl_vec) {
		HashMap hmap = loadOWLDisjointWithData(owl_vec);
		return fixdOWLDisjointWithData(hmap);
	}

	public static void main(String[] args) {
		String owlfile = (String) args[0];
		HashMap hmap = OWLDisjointWithScanner.run(Utils.readFile(owlfile));
		Utils.dumpMultiValuedHashMap("disjointWith", hmap);
	}
}