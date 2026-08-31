/*
Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
* All rights reserved.
*
* See the full license at http://one-jar.sourceforge.net/one-jar-license.html
* This license is also included in the distributions of this software
* under doc/one-jar-license.txt
*/
package gov.nih.nci.evs.restapi.appl.main;
import gov.nih.nci.evs.restapi.appl.*;
import java.util.Arrays;

public class OwlsummaryMain {

    public static void main(String[] args) {
        try {
			OWLSummary.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


