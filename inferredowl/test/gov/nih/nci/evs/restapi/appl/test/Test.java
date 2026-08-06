/*
 * Copyright (c) 2004-2010, P. Simon Tuffs (simon@simontuffs.com)
 * All rights reserved.
 *
 * See the full license at http://one-jar.sourceforge.net/one-jar-license.html
 * This license is also included in the distributions of this software
 * under doc/one-jar-license.txt
 */
package gov.nih.nci.evs.restapi.appl.test;

import com.simontuffs.onejar.test.Testable;

public class Test extends Testable {
    
    public static void main(String args[]) throws Exception {
        Test test = new Test();
        test.runTests();
    }
    
    // Test other aspects of the application at unit level (e.g. library
    // methods).
    public void testInferredowl1() {
        System.out.println("testInferredowl1: OK");
    }
    public void testInferredowl2() {
        System.out.println("testInferredowl2: OK");
    }
    public void testInferredowl3() {
        System.out.println("testInferredowl3: OK");
    }
    
}
