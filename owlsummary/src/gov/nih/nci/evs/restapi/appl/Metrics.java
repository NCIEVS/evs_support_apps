package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.*;
import java.io.*;

public class Metrics {
    private HashMap<String, Integer> fullSynGroupedCount = new HashMap<String, Integer>();
    private HashMap<String, Integer> fullSynRawCount = new HashMap<String, Integer>();
    int conceptCount;
    static String OWLSUMMARY_PROPERTIES = "owlsummary.properties";






    OWLData owlData = null;
    private String metricsFile = null;
    private PrintWriter pwMetrics = null;

    private void configPrintWriterMetrics(String fileLoc) {
		System.out.println("configPrintWriterMetrics " + fileLoc);
        try {
            final File file = new File(new String(fileLoc));
			pwMetrics = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF8")));

        }
        catch (final Exception e) {
            System.out.println("Error in PrintWriter");
        }
    }

    public PrintWriter getWrintWriter() {
		return pwMetrics;
	}

    public void initialize() {
		Properties sysProp = System.getProperties();
		FileInputStream instream = null;
		Properties props = new Properties();
		try {
			instream = new FileInputStream(OWLSUMMARY_PROPERTIES);
			props.load(instream);
			instream.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		metricsFile = props.getProperty("metricsfile");
		if (metricsFile != null && metricsFile.length() > 0) {
			configPrintWriterMetrics(metricsFile);
		}

	}

	public Metrics(OWLData owlData) {
		System.out.println("Instantiating Metrics ...");
		this.owlData = owlData;
		initialize();







        countFullSyns();
        debugPrintCounts();

        if (pwMetrics != null) {
            pwMetrics.close();
        }

    }


    public HashMap getTermSourceCountMap() {
		HashMap sourceCountMap = new HashMap();
		Vector v = owlData.get_axiom_vec();//ScannerUtils.extractAxioms(scanner.get_owl_vec());
		for (int i=0; i<v.size(); i++) {
			String line = (String) v.elementAt(i);
			Vector u = StringUtils.parseData(line, '|');
			String code = (String) u.elementAt(0);
			if (StringUtils.isNCItCode(code)) {
				String propCode = (String) u.elementAt(1);
				if (propCode.compareTo("P90") == 0) {
					for (int j=3; j<u.size(); j++) {
						String t = (String) u.elementAt(j);
						Vector u2 = StringUtils.parseData(t, '$');
						String qualCode = (String) u2.elementAt(0);
						if (qualCode.compareTo("P384") == 0) {
							String qualvalue = (String) u2.elementAt(1);
							int count = 0;
							if (sourceCountMap.containsKey(qualvalue)) {
								Integer int_obj = (Integer) sourceCountMap.get(qualvalue);
								count = Integer.valueOf(int_obj);
								count++;
							}
							Integer int_obj = Integer.valueOf(count);
							sourceCountMap.put(qualvalue, int_obj);
						}
					}
				}
			}
		}
		return sourceCountMap;
	}

    private void countFullSyns() {
        Vector concepts = owlData.getConceptCodes();
        conceptCount = concepts.size();
        fullSynRawCount = getTermSourceCountMap();

        Vector sources = new Vector();
        Iterator it = fullSynRawCount.keySet().iterator();
        while (it.hasNext()) {
			String key = (String) it.next();
			if (!sources.contains(key)) {
				sources.add(key);
			}
		}

		String sourceString = "";
		for (int i=0; i<sources.size(); i++) {
			String source = (String) sources.elementAt(i);
			sourceString = sourceString + source + "+";
		}
		sourceString = sourceString.substring(0, sourceString.length() - 1);

		if (fullSynGroupedCount.get(sourceString) == null) {
			fullSynGroupedCount.put(sourceString, 1);
		} else {
			Integer groupedCount = fullSynGroupedCount.get(sourceString);
			groupedCount++;
			fullSynGroupedCount.put(sourceString, groupedCount);
		}
     }

     private void debugPrintCounts() {
        Set<String> keySet = fullSynRawCount.keySet();
        if (pwMetrics != null) pwMetrics.println("Raw Count");
        for (String key : keySet) {
            if (pwMetrics != null) pwMetrics.println(key + "\t" + fullSynRawCount.get(key));
        }
        if (pwMetrics != null) pwMetrics.println("Grouped Count");
        keySet = fullSynGroupedCount.keySet();
        for (String key : keySet) {
            if (pwMetrics != null) pwMetrics.println(key + "\t" + fullSynGroupedCount.get(key));
        }
        System.out.println("Finished debugPrintCounts.");
    }

}
