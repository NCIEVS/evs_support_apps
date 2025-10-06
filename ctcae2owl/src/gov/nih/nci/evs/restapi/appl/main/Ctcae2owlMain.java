/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */
package gov.nih.nci.evs.restapi.appl.main;
import gov.nih.nci.evs.restapi.appl.*;

import java.util.Arrays;

public class Ctcae2owlMain {

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String serviceUrl = args[0];
		String named_graph = args[1];
		String username = args[2];
		String password = args[3];
		CTCAE2OWL ctcae2OWL = new CTCAE2OWL(serviceUrl, named_graph, username, password);
		String outputfile = "ctcae6.owl";
		ctcae2OWL.run(outputfile);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}
