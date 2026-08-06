package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import java.io.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class OWLScrubber {

	public static Vector run(Vector v, String target) {
		int num_lines = 0;
		int k = 0;
		long ms = System.currentTimeMillis();
		Vector w = new Vector();
		target = "<" + target + ">";
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			if (line.indexOf(target) == -1) {
				w.add(line);
			} else {
				k++;
			}
		}
		System.out.println("\tNumber of lines: " + v.size());
		System.out.println("\tNumber of lines removed: " + k);
        System.out.println("\tTotal run time (ms): " + (System.currentTimeMillis() - ms));
        return w;
	}

	public static Vector run(Vector v, Vector propVec) {
		for (int i=0; i<propVec.size(); i++) {
			String propCode = (String) propVec.elementAt(i);
			int k = i+1;
			System.out.println("(" + k + ") " + propCode);
			v = run(v, propCode);
		}
		System.out.println("Post scrub: " + v.size());
		return v;
	}

/*
	public static Vector run(String dataVec, String scrubbedProperties) {
		Vector v = load(dataVec);
		Vector propVec = Utils.readFile(scrubbedProperties);
		for (int i=0; i<propVec.size(); i++) {
			String propCode = (String) propVec.elementAt(i);
			int k = i+1;
			System.out.println("(" + k + ") " + propCode);
			v = run(v, propCode);
		}
		System.out.println("Post scrub: " + v.size());
		return v;
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String owlClassData = args[0];
		String scrubbedProperties = args[1];
		Vector v = run(owlClassData, scrubbedProperties);
		Utils.saveToFile("scrubbed_data.txt", v);
		System.out.println("\tTotal run time (ms): " + (System.currentTimeMillis() - ms));
	}
*/
}