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
import java.io.File;

public class Jowl2memeMain {

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String owlfile = args[0];
		File f = new File(owlfile);
		if (!f.exists()) {
			System.out.println(owlfile + " not found -- program abort.");
		}
        OWL2MEME owl2meme = new OWL2MEME(owlfile);
        owl2meme.generate();
        System.out.println("\tTotal run time (ms): " + (System.currentTimeMillis() - ms));
	}


}
