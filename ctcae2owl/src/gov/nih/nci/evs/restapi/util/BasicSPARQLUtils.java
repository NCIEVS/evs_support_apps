package gov.nih.nci.evs.restapi.util;

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
public class BasicSPARQLUtils {
    gov.nih.nci.evs.restapi.util.JSONUtils jsonUtils = null;
    gov.nih.nci.evs.restapi.util.HTTPUtils httpUtils = null;
    public String named_graph = null;
    String prefixes = null;
    String serviceUrl = null;
    String restURL = null;
    String named_graph_id = ":NHC0";

    String NCIT_URI = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";

    HashMap nameVersion2NamedGraphMap = null;
    HashMap ontologyUri2LabelMap = null;
    String version = null;
    String username = null;
    String password = null;


    public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

    public void setJSONUtils(JSONUtils jsonUtils) {
		this.jsonUtils = jsonUtils;
	}

    public void setHTTPUtils(gov.nih.nci.evs.restapi.util.HTTPUtils httpUtils) {
		this.httpUtils = httpUtils;
	}

	public BasicSPARQLUtils(String serviceUrl, String named_graph, String username, String password) {
		this.serviceUrl = serviceUrl;
		this.named_graph = named_graph;
		this.restURL = serviceUrl;
		this.username = username;
		this.password = password;
		set_named_graph(named_graph);

		this.httpUtils = new gov.nih.nci.evs.restapi.util.HTTPUtils(serviceUrl, username, password);
        this.jsonUtils = new JSONUtils();
    }

	public String get_named_graph() {
		return this.named_graph;
	}

    public void set_named_graph_id(String named_graph_id) {
		this.named_graph_id = named_graph_id;
	}

    public void set_version(String version) {
		this.version = version;
	}

    public String getServiceUrl() {
		return this.serviceUrl;
	}

    public void set_named_graph(String named_graph) {
		this.named_graph = named_graph;
	}

	public void set_prefixes(String prefixes) {
		this.prefixes = prefixes;
	}

    public String getPrefixes() {
		if (prefixes != null) return prefixes;
		StringBuffer buf = new StringBuffer();
		buf.append("PREFIX :<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>").append("\n");
		buf.append("PREFIX base:<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl>").append("\n");
		buf.append("PREFIX oboInOwl:<http://www.geneontology.org/formats/oboInOwl#>").append("\n");
		buf.append("PREFIX xml:<http://www.w3.org/XML/1998/namespace>").append("\n");
		buf.append("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>").append("\n");
		buf.append("PREFIX owl:<http://www.w3.org/2002/07/owl#>").append("\n");
		buf.append("PREFIX owl2xml:<http://www.w3.org/2006/12/owl2-xml#>").append("\n");
		buf.append("PREFIX protege:<http://protege.stanford.edu/plugins/owl/protege#>").append("\n");
		buf.append("PREFIX xsd:<http://www.w3.org/2001/XMLSchema#>").append("\n");
		buf.append("PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>").append("\n");
		buf.append("PREFIX ncicp:<http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#>").append("\n");
		buf.append("PREFIX dc:<http://purl.org/dc/elements/1.1/>").append("\n");
		return buf.toString();
	}

    public String getQuery(String query_file) {
		try {
			String query = httpUtils.loadQuery(query_file, false);
			return query;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}


	public String loadQuery(String query_file) {
		try {
			String query = httpUtils.loadQuery(query_file, false);
			query = query.replace("query: ", "");
			return query;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

    public Vector execute(String query_file) {
		Vector w = null;
		try {
			String query = httpUtils.loadQuery(query_file, false);
			String json = new HTTPUtils(this.restURL).runSPARQL(query, restURL);
			System.out.println(json);
			JSONUtils jsonUtils = new JSONUtils();
			w = jsonUtils.parseJSON(json);
			w = jsonUtils.getResponseValues(w);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return w;
	}

    public Vector executeQuery(String query) {
        Vector v = null;
        try {
			if (this.password == null) {
				String json = new HTTPUtils(this.restURL).runSPARQL(query, this.restURL);
				JSONUtils jsonUtils = new JSONUtils();
				v = jsonUtils.parseJSON(json);
				v = jsonUtils.getResponseValues(v);

			} else {
				RESTUtils restUtils = new RESTUtils(this.username, this.password, 1500000, 1500000);
				String response = restUtils.runSPARQL(query, serviceUrl);
				v = new gov.nih.nci.evs.restapi.util.JSONUtils().parseJSON(response);
				v = jsonUtils.getResponseValues(v);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

    public String getJSONResponseString(String query) {
        try {
			query = httpUtils.encode(query);
            String json = httpUtils.executeQuery(query);
			return json;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
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

