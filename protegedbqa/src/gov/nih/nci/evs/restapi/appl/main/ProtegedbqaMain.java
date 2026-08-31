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

public class ProtegedbqaMain {

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String owlfile = args[0];
		ProtegeKBQA qaRun = new ProtegeKBQA();
		qaRun.configure(args);
		qaRun.performQA();
        System.out.println("\tTotal run time (ms): " + (System.currentTimeMillis() - ms));
	}


}
