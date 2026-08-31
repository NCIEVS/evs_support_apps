package gov.nih.nci.evs.restapi.appl;
import gov.nih.nci.evs.restapi.util.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import org.apache.commons.collections.CollectionUtils;

public class OWLSummary {
    static String OWLSUMMARY_PROPERTIES = "owlsummary.properties";
    private static final Properties sysProp = System.getProperties();

    private final HashMap<headerField, Integer> headerDiff = new HashMap<headerField, Integer>();
    private String configFile = null;
    private SummaryObject current;
    private String currentFilename = null;
    private boolean doSummary = false;
    private boolean doDetails = false;
    private boolean doDiff = false;
    private boolean doMetrics = false;
    private SummaryObject previous;
    private String previousFilename = null;
    private PrintWriter pw;
    private PrintWriter pwDiff;

    private PrintWriter pwMetrics;

    public static void run(final String[] args) {
        try {
            final OWLSummary summary = new OWLSummary();
            long ms = System.currentTimeMillis();
            long ms0 = ms;
            summary.configure(args);
            System.out.println("Total configure run time (ms): " + (System.currentTimeMillis() - ms));
            ms = System.currentTimeMillis();
            summary.performSummary();
            System.out.println("Total performSummary run time (ms): " + (System.currentTimeMillis() - ms));
            System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms0));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static HashSet<String> compareHashSet(final HashSet<String> firstSet,
                                           final HashSet<String> secondSet) {
		return setDifference(firstSet, secondSet);

    }

    public static HashMap<String, String> compareMap(
           final HashMap<String, String> firstMap,
            final HashMap<String, String> secondMap) {
        final HashMap<String, String> extraValues = new HashMap<String, String>();
        final TreeSet<String> c = new TreeSet<String>(firstMap.keySet());
        final Iterator<String> iter = c.iterator();
        String key;
        while (iter.hasNext()) {
            key = iter.next();
            if (!secondMap.containsKey(key)) {
                extraValues.put(key, firstMap.get(key));
            }
        }
        return extraValues;
    }

    public static HashSet<String> compareSet(final Set<String> firstSet,
                                       final Set<String> secondSet) {
        return setDifference(new HashSet<>(firstSet), new HashSet<>(secondSet));
    }

    public static Vector<String> compareSet(final Vector<String> firstSet,
                                      final Vector<String> secondSet) {

		HashSet set1 = Utils.vector2HashSet(firstSet);
		HashSet set2 = Utils.vector2HashSet(secondSet);
        return Utils.hashSet2Vector(setDifference(set1, set2));
    }

    public static HashMap<String, String> compareStringMap(
            final HashMap<String, String> firstMap,
            final HashMap<String, String> secondMap) {
        final HashMap<String, String> extraValues = new HashMap<String, String>();
        final TreeSet<String> c = new TreeSet<String>(firstMap.keySet());
        final Iterator<String> iter = c.iterator();
        String key;
        while (iter.hasNext()) {
            key = iter.next();
            if (!secondMap.containsKey(key)) {
                extraValues.put(key, firstMap.get(key));
            }
        }
        return extraValues;
    }

    public static HashSet<String> compareStringSet(final Set<String> firstSet,
                                       final Set<String> secondSet) {
		return setDifference(new HashSet<>(firstSet), new HashSet<>(secondSet));
    }

    public static Vector<String> compareStringVector(final Vector<String> firstSet,
                                         final Vector<String> secondSet) {

        if (firstSet == null && secondSet == null) {
            return new Vector<String>();
        } else if (firstSet == null) {
            return secondSet;
        } else if (secondSet == null) {
            return firstSet;
        }
        HashSet hset = setDifference(firstSet, secondSet);
        return Utils.hashSet2Vector(hset);
    }

    public static String set2String(HashSet<String> hset) {
		String setAsString = hset.toString();
		return setAsString;
	}

    public static HashSet setDifference(HashSet set1, HashSet set2) {
		set1.removeAll(set2);
		return set1;
	}

    public static HashSet setDifference(Vector v1, Vector v2) {
		HashSet set1 = Utils.vector2HashSet(v1);
		HashSet set2 = Utils.vector2HashSet(v2);
        return setDifference(set1, set2);
	}

	public static Vector dumpHashSet(String label, String action, HashSet set) {
		Vector w = new Vector();
		Iterator it = set.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			String s = label + "\t" + action + "\t" + t;
			w.add(s);
		}
		return w;
	}

    private HashMap<String, String> compareDefinitions(
            final HashMap<String, String> firstMap,
            final HashMap<String, String> secondMap) {

        final HashMap<String, String> changedDefinitions = new HashMap<String, String>();
        final Set<String> c = firstMap.keySet();
        final Iterator<String> iter = c.iterator();
        String key;

        while (iter.hasNext()) {
            key = iter.next();
            String secondDef = secondMap.get(key);
            String firstDef = firstMap.get(key);
            String fullDef = "";
            if (firstDef == null) {
                firstDef = "";
            }
            if (secondDef == null) {
                secondDef = "";
            }

            if (!secondDef.equals(firstDef)) {
                fullDef = firstDef + "\t" + secondDef;
                changedDefinitions.put(key, fullDef);
            }

        }
        return changedDefinitions;
    }


    private String compareParents(final Vector<String> currentIn,
                                  final Vector<String> previousIn) {
        String changedParents = "";

        String item;
        boolean match = true;
        String previousPar = "";
        String currentPar = "";

        final TreeSet<String> currentParents = new TreeSet<String>(currentIn);
        final TreeSet<String> previousParents = new TreeSet<String>(previousIn);
        final Iterator<String> firstIt = currentParents.iterator();
        int count = 0;
        while (firstIt.hasNext()) {

            item = firstIt.next();
            if (count > 0) {
                previousPar = previousPar + " | ";
            }
            previousPar = previousPar + item;
            count++;
            if (!previousParents.contains(item)) {
                match = false;
            }

        }

        count = 0;
        match = true;
        for (String previousParent : previousParents) {
            item = previousParent;
            if (count > 0) {
                currentPar = currentPar + " | ";
            }
            currentPar = currentPar + item;
            count++;
            if (!currentParents.contains(item)) {
                match = false;
            }
        }

        if (!match) {
            changedParents = currentPar + "\t" + previousPar;
        }
        return changedParents;
    }



    private void conceptsChangingDefs() {
        System.out.println("Starting concepts changing definitions");
        final HashMap<String, String> currentDefs = current.getConceptAndDef();
        final HashMap<String, String> previousDefs = previous
                .getConceptAndDef();

        final HashMap<String, String> changed = compareDefinitions(currentDefs,
                previousDefs);

        printDefinitions(changed);
    }

    private void conceptsChangingKinds() {
        System.out.println("Starting concepts changing kinds");
        final HashMap<String, Vector<String>> currentConceptsPerKind = current
                .getConceptsPerKind();
        final HashMap<String, Vector<String>> previousConceptsPerKind = previous
                .getConceptsPerKind();
        Set<String> keySet = currentConceptsPerKind.keySet();
        Iterator<String> iter = keySet.iterator();

        final HashMap<String, Vector<String>> movedConcepts = new HashMap<String, Vector<String>>();
        final HashMap<String, HashMap<String, String>> movedConceptsMap = new HashMap<String, HashMap<String, String>>();

        while (iter.hasNext()) {
            final String missingFromKind = iter.next();
            if (!missingFromKind.equals("Retired Concept")
                    && !missingFromKind.equals("C28428")) {
                final Vector<String> currentKind = currentConceptsPerKind
                        .get(missingFromKind);
                final Vector<String> previousKind = previousConceptsPerKind
                        .get(missingFromKind);
                final Vector<String> missing = compareStringVector(previousKind,
                        currentKind);
                movedConcepts.put(missingFromKind, missing);
            }
        }

        keySet = movedConcepts.keySet();
        iter = keySet.iterator();
        while (iter.hasNext()) {
            final String keyOldKind = iter.next();
            final HashMap<String, String> currentLocation = new HashMap<String, String>();
            final Vector<String> missing = movedConcepts.get(keyOldKind);
            for (String conceptCode : missing) {
                final String newKind = current.whatKindIsThis(conceptCode);
                if (newKind != null) {
                    if (!newKind.equals("Retired Concept")
                            && !newKind.equals("C28428")) {
                        currentLocation.put(conceptCode, newKind);
                    }
                } else {
                    System.out.println(conceptCode + " resulted in a null kind");
                }
            }
            movedConceptsMap.put(keyOldKind, currentLocation);
        }

        printChangedKinds(movedConceptsMap);

    }


    private void conceptsChangingParents() {
        System.out.println("Starting concepts changing parents");
        final HashMap<String, HashMap<String, String>> changedParentsPerRoot = new HashMap<String, HashMap<String, String>>();

        HashMap<String, RootConcept> roots = current.getRootMap();
        Set<String> rootIter = roots.keySet();
        for (String rootCode : rootIter) {
            RootConcept rootConcept = (RootConcept) roots.get(rootCode);
            Vector descendantCodes = rootConcept.getAllDescendantCodes();
			for (int i=0; i<descendantCodes.size(); i++) {
				String conceptCode = (String) descendantCodes.elementAt(i);
                HashMap<String, String> parentMap = new HashMap<String, String>();

                Vector<String> currentParents = current
                        .getConceptParents(conceptCode);
                Vector<String> previousParents = previous
                        .getConceptParents(conceptCode);
                String changedParents = null;
                if (previousParents != null) {
                    changedParents = compareParents(currentParents,
                            previousParents);
                }
                if (changedParents != null && changedParents.length() > 0) {
                    parentMap.put(conceptCode, changedParents);
                }
                changedParentsPerRoot.put(rootCode, parentMap);
            }

        }

        printChangedParents(changedParentsPerRoot);
    }

    private void conceptsChangingPreferredNames() {
        System.out.println("Starting concepts changing preferred names");
        final HashMap<String, String> currentPreferredNames = current
                .getConceptAndPreferredName();
        final HashMap<String, String> previousPreferredNames = previous
                .getConceptAndPreferredName();

        final HashMap<String, String> changed = compareDefinitions(
                currentPreferredNames, previousPreferredNames);

        final HashMap<String, Vector<String>> currentSemanticTypes = current
                .getSemanticTypes();

        printPreferredNames(changed, currentSemanticTypes, pwDiff);
    }

    private void configPrintWriter(final String fileLoc) throws Exception {
        try {
            final File file = new File(new String(fileLoc));
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF8")), true);
        }
        catch (final Exception e) {
            System.out.println("Error in SummaryFile PrintWriter ");
            e.printStackTrace();
            throw new Exception("Unable to create PrintWriter for Summary File");
        }
    }

    private void configPrintWriterDiff(String fileLoc) {
        try {
            final File file = new File(new String(fileLoc));
            pwDiff = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF8")), true);

        }
        catch (final Exception e) {
            System.out.println("Error in PrintWriter");
        }
    }

    private void configPrintWriterMetrics(String fileLoc) {
        try {
            final File file = new File(new String(fileLoc));
			pwMetrics = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), "UTF8")));

        }
        catch (final Exception e) {
            System.out.println("Error in PrintWriter");
        }
    }

    public void configure(String[] args) throws Exception {
        String summaryFile = null;
        String detailsFile = null;
        String metricsFile = null;
        Properties props = new Properties();
        try {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equalsIgnoreCase("-c")
                            || args[i].equalsIgnoreCase("--config")) {
                        configFile = args[++i];
                    } else if (args[i].equalsIgnoreCase("-i")
                            || args[i].equalsIgnoreCase("--input")) {
                        currentFilename = args[++i];
                    } else if (args[i].equalsIgnoreCase("-p")
                            || args[i].equalsIgnoreCase("--previous")) {
                        previousFilename = args[++i];
                    } else if (args[i].equalsIgnoreCase("-s")
                            || args[i].equalsIgnoreCase("--summary")) {
                        summaryFile = args[++i];
                    } else if (args[i].equalsIgnoreCase("-d")
                            || args[i].equalsIgnoreCase("--details")) {
                        detailsFile = args[++i];
                    } else if (args[i].equalsIgnoreCase("-m")
                            || args[i].equalsIgnoreCase("--metrics")) {
                        metricsFile = args[++i];
                    } else {
                        printHelp();
                    }
                }
            } else {
                printHelp();
            }

            if (configFile == null) {
                final String filename = sysProp
                        .getProperty(OWLSUMMARY_PROPERTIES);
                configFile = filename;
            }

            if (configFile != null) {
                System.out.println("Config file at: " + configFile);

                FileInputStream instream = new FileInputStream(configFile);
                props.load(instream);
                instream.close();
            } else {
                System.out.println("No config file specified");
            }

            if (summaryFile == null) {
                summaryFile = props.getProperty("outputfile");
            }
            if (summaryFile != null && summaryFile.length() > 0) {
                doSummary = true;
                configPrintWriter(summaryFile);
            }

            if (detailsFile == null) {
                detailsFile = props.getProperty("detailsfile");
            }
            if (detailsFile != null && detailsFile.length() > 0) {
                doDetails = true;
                configPrintWriterDiff(detailsFile);
            }

            if (metricsFile == null) {
                metricsFile = props.getProperty("metricsfile");
            }

            System.out.println("metricsFile: " + metricsFile);
            if (metricsFile != null && metricsFile.length() > 0) {
                doMetrics = true;
            }
            if (currentFilename == null) {
                currentFilename = props.getProperty("ontology_current");
            }
            System.out.println("Loading current vocabulary " + currentFilename);
            current = new SummaryObject(new OWLData(currentFilename));
            System.out.println("current vocabulary " + currentFilename + " loaded.");

            try {
                if (previousFilename == null) {
                    previousFilename = props.getProperty("ontology_previous");
                }
                 if (previousFilename != null && previousFilename.length() > 0) {
					System.out.println("\nLoading previous vocabulary " + previousFilename);
					previous = new SummaryObject(new OWLData(previousFilename));
					System.out.println("previous vocabulary " + previousFilename + " loaded.");

                    doDiff = true;
                }
            }
            catch (final NullPointerException e) {
                System.out
                        .println("Only one file input.  No diff being performed.");
            }
            catch (final java.lang.IllegalArgumentException e) {
                System.out.println("Error reading previous ontology from URL");
            }
        }
        catch (final FileNotFoundException e) {
            System.out.println("File not found");
            throw new Exception("File not found");
        }
        catch (final IOException e) {
            System.out.println("Trouble reading config file");
            throw new Exception("Trouble reading config file");
        }
        catch (final Exception e) {
            e.printStackTrace();
        }

    }

    @SuppressWarnings("unchecked")
    private Collection<String> difference(
            HashMap<String, String> hashMap1,
            HashMap<String, String> hashMap2) {
        Collection<String> result = CollectionUtils.disjunction(hashMap1
                .keySet(), hashMap2.keySet());
        return result;
    }

    final HashMap<String, Integer> doDiff(
            final HashMap<String, Integer> newMap,
            final HashMap<String, Integer> oldMap) {
        final HashMap<String, Integer> diffMap = new HashMap<String, Integer>();
        Integer countCurrent, countPrevious, countDiff;
        String key;

        Set<String> set = newMap.keySet();
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            key = iter.next();
            countCurrent = newMap.get(key);
            countPrevious = oldMap.get(key);
            if (countCurrent == null) {
                countCurrent = 0;
            }
            if (countPrevious == null) {
                countPrevious = 0;
            }
            countDiff = countCurrent - countPrevious;
            diffMap.put(key, countDiff);
        }

        set = oldMap.keySet();
        iter = set.iterator();
        while (iter.hasNext()) {
            key = iter.next();
            countPrevious = oldMap.get(key);
            if (!diffMap.containsKey(key)) {
                diffMap.put(key, countPrevious * (-1));
            }
        }

        return diffMap;
    }

    public final void doDualFileSummary() {
        final HashMap<headerField, Integer> headerCounts = doHeaderCounts(current);
        printHeaderCounts(headerCounts);
        pw.flush();

        doHeaderDiff();
        pw.flush();
        System.out.println("Finished doHeaderDiff");

        doEntityCounts(current);
        pw.flush();
        System.out.println("Finished doEntityCounts");

        doEntityDiff();
        pw.flush();
        System.out.println("Finished doEntityDiff");

        pw.close();

        if (doDetails) {
            doEntityDetails();
        }

        if (pwDiff != null) {
            pwDiff.close();
        }
    }

    final void doEntityCounts(final SummaryObject summary) {

        System.out.println("Step 5 - Starting entity counts");
        System.out.println("Step 5a - getDefinedConceptCountsPerKind");

        final HashMap<String, Integer> conceptCounts = summary
                .getConceptCountsPerKind();
        final HashMap<String, Integer> defined = summary
                .getDefinedConceptCountsPerKind();
        printConceptCounts(conceptCounts, defined);

        System.out.println("Step 5b - getPropertyCountPerKind");
        final HashMap<String, HashMap<String, Integer>> propertyCounts = summary
                .getPropertyCountPerKind();
        printProperties(propertyCounts);

         System.out.println("Step 5c - getRoleCountPerKind");
        final HashMap<String, HashMap<String, Integer>> roleCounts = summary
                .getRoleCountPerKind();
        printRoles(roleCounts);

        System.out.println("Step 5d - getAssociationCountPerKind");
        final HashMap<String, HashMap<String, Integer>> assocCounts = summary
                .getAssociationCountPerKind();
        printAssociations(assocCounts);
        System.out.println("Finished entity counts");
    }

    final void doEntityDetails() {
        System.out.println("Starting entity details");
        newConcepts();
        System.out.println("Finished computing new concepts");

        retiredConcepts();
        System.out.println("Finished computing retired and unretired concepts");

        conceptsChangingKinds();
        System.out.println("Finished computing concepts that changed roots");

        conceptsChangingParents();
        System.out.println("Finished computing concepts that changed parents");

        conceptsChangingDefs();
        System.out
                .println("Finished computing concepts with changed definitions");

        conceptsChangingPreferredNames();
        System.out
                .println("Finished reporting concepts with changed Preferred_Name");

    }

    final void doEntityDiff() {
        System.out.println("Step 5e - getConceptCountsPerKind ...");
        final HashMap<String, Integer> currentConceptCount = current
                .getConceptCountsPerKind();
        final HashMap<String, Integer> previousConceptCount = previous
                .getConceptCountsPerKind();
        HashMap<String, Integer> conceptDiff = doDiff(currentConceptCount,
                previousConceptCount);

        System.out.println("Step 5f - getDefinedConceptCountsPerKind ...");
        final HashMap<String, Integer> definedCurrentConceptCount = current
                .getDefinedConceptCountsPerKind();
        final HashMap<String, Integer> definedPreviousConceptCount = previous
                .getDefinedConceptCountsPerKind();
        HashMap<String, Integer> definedConceptDiff = doDiff(
                definedCurrentConceptCount, definedPreviousConceptCount);

        printConceptDiff(conceptDiff, definedConceptDiff);

        System.out.println("Step 5g - getPropertyCountPerKind ...");
        final HashMap<String, HashMap<String, Integer>> currentProperties = current
                .getPropertyCountPerKind();
        final HashMap<String, HashMap<String, Integer>> previousProperties = previous
                .getPropertyCountPerKind();
        final HashMap<String, HashMap<String, Integer>> propertyDiff = doThingsPerKind(
                currentProperties, previousProperties);
        printPropertyDiff(propertyDiff);

        System.out.println("Step 5h - getRoleCountPerKind ...");
        final HashMap<String, HashMap<String, Integer>> currentRoles = current
                .getRoleCountPerKind();
        final HashMap<String, HashMap<String, Integer>> previousRoles = previous
                .getRoleCountPerKind();
        final HashMap<String, HashMap<String, Integer>> roleDiff = doThingsPerKind(
                currentRoles, previousRoles);
        printRoleDiff(roleDiff);

        System.out.println("Step 5i - getAssociationCountPerKind ...");
        final HashMap<String, HashMap<String, Integer>> currentAssocs = current
                .getAssociationCountPerKind();
        final HashMap<String, HashMap<String, Integer>> previousAssocs = previous
                .getAssociationCountPerKind();
        final HashMap<String, HashMap<String, Integer>> assocDiff = doThingsPerKind(
                currentAssocs, previousAssocs);
        printAssocDiff(assocDiff);
        System.out.println("Finished entity diff");
    }

    public void dumpHeaderFieldCountHashMap(HashMap<headerField, Integer> hmap) {
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			headerField key = (headerField) it.next();
			Integer int_obj = (Integer) hmap.get(key);
			System.out.println(key.toString() + " --> " + int_obj.intValue());
		}
	}

    final HashMap<headerField, Integer> doHeaderCounts(
            final SummaryObject summary) {

        final HashMap<headerField, Integer> headerCounts = new HashMap<headerField, Integer>();
        headerCounts.put(headerField.Roles, Integer.valueOf(summary.getRoleMap().keySet().size()));
        headerCounts.put(headerField.Associations, Integer.valueOf(summary.getAssociationMap().keySet().size()));
        headerCounts.put(headerField.Properties, Integer.valueOf(summary.getPropertyMap().keySet().size()));

        headerCounts.put(headerField.Namespaces, Integer.valueOf(0));

        headerCounts.put(headerField.Roots, Integer.valueOf(summary.getRootMap().size()));
        headerCounts.put(headerField.Roots, Integer.valueOf(summary.getRootCount()));
        headerCounts.put(headerField.Concepts, Integer.valueOf(summary.getConceptCount()));

        return headerCounts;
    }

    public Vector getMultivaluedMapKeys(HashMap hmap) {
		Vector w = new Vector();
		Iterator it = hmap.keySet().iterator();
		while(it.hasNext()) {
			String key = (String) it.next();
			w.add(key);
		}
		return w;
	}

    final void doHeaderDiff() {

        System.out.println("Step 1: doHeaderCounts current ...");
        final HashMap<headerField, Integer> headerCurrent = doHeaderCounts(current);

        System.out.println("Step 2: doHeaderCounts previous ...");
        final HashMap<headerField, Integer> headerPrevious = doHeaderCounts(previous);

        headerField key;
        int countCurrent, countPrevious, countDiff;

        System.out.println("Step 3: doHeaderCounts diff ...");
        final Set<headerField> set = headerCurrent.keySet();
        Iterator<headerField> iter = set.iterator();
        while (iter.hasNext()) {
            key = iter.next();
            countCurrent = headerCurrent.get(key);
            countPrevious = headerPrevious.get(key);
            countDiff = countCurrent - countPrevious;
            headerDiff.put(key, countDiff);
        }
        printHeaderDiff(headerDiff);

        System.out.println("Step 4: find out what the actual header item is different ...");
        final TreeSet<headerField> tSet = new TreeSet<headerField>(
                headerDiff.keySet());
        iter = tSet.iterator();
        while (iter.hasNext()) {
            key = iter.next();
            if (headerDiff.get(key) != 0) {
                switch (key) {
                    case Roles:
                        HashMap<String, String> extra = compareStringMap(
                                current.getObjectPropertyCode2NameMap(), previous.getObjectPropertyCode2NameMap());
                        if (extra.size() > 0) {
                            printStringHashMap("Roles added", extra);
                        }
                        HashMap<String, String> missing = compareStringMap(
                                previous.getRoleMap(), current.getRoleMap());
                        if (missing.size() > 0) {
                            printStringHashMap("Roles removed", missing);
                        }
                        break;
                    case Associations:
                        extra = compareStringMap(current.getObjectValuedAnnotationPropertyCode2NameMap(),
                                previous.getObjectValuedAnnotationPropertyCode2NameMap());

                        if (extra.size() > 0) {
                            printStringHashMap("Associations added", extra);
                        }

                        missing = compareStringMap(previous.getAssociationMap(),
                                current.getAssociationMap());
                        if (missing.size() > 0) {
                            printStringHashMap("Associations removed", missing);
                        }
                        break;
                    case Properties:
                        extra = compareStringMap(current.getStringValuedAnnotationPropertyCode2NameMap(),
                                previous.getStringValuedAnnotationPropertyCode2NameMap());
                        if (extra.size() > 0) {
                            printStringHashMap("Properties added", extra);
                        }
                        missing = compareStringMap(previous.getStringValuedAnnotationPropertyCode2NameMap(),
                                current.getStringValuedAnnotationPropertyCode2NameMap());
                        if (missing.size() > 0) {
                            printStringHashMap("Properties removed", missing);
                        }
                        break;
                    case Namespaces:
                        HashSet<String> extraS = compareSet(
                                current.getNamespaces(), previous.getNamespaces());
                        if (extraS.size() > 0) {
                            printHashSet("Namespaces added", extraS);
                        }
                        HashSet<String> missingS = compareSet(
                                previous.getNamespaces(), current.getNamespaces());
                        if (missingS.size() > 0) {
                            printHashSet("Namespaces removed", missingS);
                        }
                        break;
                    case Roots:
                        HashSet<String> extraU = compareStringSet(current.getRootConceptNames(),
                                previous.getRootConceptNames());
                        if (extraU.size() > 0) {
                            printStringHashSet("Root Concepts added", extraU);
                        }
                        HashSet<String> missingU = compareStringSet(previous.getRootConceptNames(),
                                current.getRootConceptNames());
                        if (missingU.size() > 0) {
                            printStringHashSet("Root Concepts removed", missingU);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void doMetrics() {
        current.doMetrics();
    }

    public final void doSingleFileSummary() {
        final HashMap<headerField, Integer> headerCounts = doHeaderCounts(current);
        printHeaderCounts(headerCounts);
        pw.flush();
        System.out.println("Finished Header counts");

        doEntityCounts(current);
        System.out.println("Finished entity counts");
        pw.close();
    }

    private HashMap<String, HashMap<String, Integer>> doThingsPerKind(
            final HashMap<String, HashMap<String, Integer>> currentKind,
            final HashMap<String, HashMap<String, Integer>> previousKind) {

        final HashMap<String, HashMap<String, Integer>> thingDiff = new HashMap<String, HashMap<String, Integer>>();

        int mapsize = currentKind.size();
        Iterator<Entry<String, HashMap<String, Integer>>> testIter = currentKind
                .entrySet().iterator();
        for (int i = 0; i < mapsize; i++) {
            Map.Entry<String, HashMap<String, Integer>> entry = testIter.next();
            String key1 = entry.getKey();
            HashMap<String, Integer> currentThing = entry.getValue();
            if (previousKind.containsKey(key1)) {
                final HashMap<String, Integer> previousThing = previousKind
                        .get(key1);
                final HashMap<String, Integer> diffThing = doDiff(currentThing,
                        previousThing);
                thingDiff.put(key1, diffThing);
            }
        }

        mapsize = previousKind.size();
        testIter = previousKind.entrySet().iterator();
        for (int i = 0; i < mapsize; i++) {
            Map.Entry<String, HashMap<String, Integer>> entry = testIter.next();
            String key1 = entry.getKey();
            final HashMap<String, Integer> previousThing = entry.getValue();
            if (!currentKind.containsKey(key1)) {
                final HashMap<String, Integer> negatedThing = negate(previousThing);
                thingDiff.put(key1, negatedThing);
            }
        }
        return thingDiff;
    }

    private HashMap<String, Integer> negate(final HashMap<String, Integer> thing) {
        final HashMap<String, Integer> negative = new HashMap<String, Integer>();
        final Set<String> item = thing.keySet();
        final Iterator<String> iter = item.iterator();
        int currentCount, negatedCount;
        while (iter.hasNext()) {
            String key = iter.next();
            currentCount = thing.get(key);
            negatedCount = currentCount * (-1);
            negative.put(key, negatedCount);
        }
        return negative;
    }

    private void newConcepts() {
        System.out.println("Starting new concepts");
        try {
            final Vector<String> currentClasses = current.getAllConceptCodes();
            System.out.println("currentClasses: " + currentClasses.size());
            final Vector<String> previousClasses = previous
                    .getAllConceptCodes();
            System.out.println("previousClasses: " + previousClasses.size());
            final Vector<String> extra = compareStringVector(currentClasses,
                    previousClasses);
            System.out.println("printNewConcepts: ");
            printNewConcepts(extra);
            final Vector<String> missing = compareStringVector(previousClasses,
                    currentClasses);
            System.out.println("printDeletedConcepts: ");
            printDeletedConcepts(missing);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println("Program exiting");
        }
    }

    final void performSummary() {
        try {
            if (doDiff) {
                doDualFileSummary();
            }
            if (doSummary && !doDiff) {
                doSingleFileSummary();
            }
            if (doMetrics) {
                doMetrics();
            }
        }
        catch (final Exception e) {
            e.printStackTrace();
            System.out.println("Program exiting");
        }
    }

    final void printAssocDiff(
            final HashMap<String, HashMap<String, Integer>> assocDiff) {

        pw.println("Association count diff per root for file "
                + currentFilename + " minus " + previousFilename);
        final TreeSet<String> assocSet = new TreeSet<String>(current
                .getAssociationMap().keySet());
        final Iterator<String> assocIter = assocSet.iterator();
        final TreeSet<String> rootSet = new TreeSet<String>(current
                .getRootMap().keySet());
        Iterator<String> rootIter = rootSet.iterator();
        pw.print("\t");
        while (rootIter.hasNext()) {
            pw.print("\t" + rootIter.next());
        }
        pw.print("\tCumulative");
        pw.println();

        while (assocIter.hasNext()) {
            rootIter = rootSet.iterator();
            final String assocCode = assocIter.next();
            final String assocName = (String) current.getObjectValuedAnnotationPropertyCode2NameMap().get(assocCode);
            Integer assocCumulative = 0;
            pw.print(assocCode + "\t" + assocName + "\t");
            while (rootIter.hasNext()) {
                final String root = rootIter.next();
                final HashMap<String, Integer> assocMap = assocDiff.get(root);
                Integer assocCount = 0;
                if (assocMap != null) {
                    assocCount = assocMap.get(assocCode);
                    if (assocCount == null) {
                        assocCount = 0;
                    }
                }

                assocCumulative = assocCumulative + assocCount;
                pw.print(assocCount + "\t");
            }
            pw.print(assocCumulative);
            pw.println();
        }
        pw.println();
        pw.flush();
    }

    final void printAssociations(
            final HashMap<String, HashMap<String, Integer>> assocCounts) {

        pw.println("Association counts per root for file " + currentFilename);
        final TreeSet<String> assocSet = new TreeSet<String>(current
                .getAssociationMap().keySet());
        final Iterator<String> assocIter = assocSet.iterator();
        final TreeSet<String> rootSet = new TreeSet<String>(current
                .getRootMap().keySet());
        Iterator<String> rootIter = rootSet.iterator();
        pw.print("\t");
        while (rootIter.hasNext()) {
            pw.print("\t" + rootIter.next());
        }
        pw.print("\tCumulative");
        pw.println();

        while (assocIter.hasNext()) {
            rootIter = rootSet.iterator();
            final String assocCode = assocIter.next();
            final String assocName = (String) current.getObjectValuedAnnotationPropertyCode2NameMap().get(assocCode);
            Integer assocCumulative = 0;
            pw.print(assocCode + "\t" + assocName + "\t");
            while (rootIter.hasNext()) {
                final String root = rootIter.next();
                final HashMap<String, Integer> assocMap = assocCounts.get(root);
                Integer assocCount = assocMap.get(assocCode);
                if (assocCount == null) {
                    assocCount = 0;
                }
                assocCumulative = assocCumulative + assocCount;
                pw.print(assocCount + "\t");
            }
            pw.print(assocCumulative);
            pw.println();
        }
        pw.println();
        pw.flush();
    }

    private void printChangedKinds(
            final HashMap<String, HashMap<String, String>> moveMap) {
        pwDiff.println("Concepts with changed roots "
                + "(excluding retirements/unretirements) found in  "
                + currentFilename);
        pwDiff.println("Code\tName\tCurrent Root\tName\tPrevious Root\tName");

        final TreeSet<String> oldKinds = new TreeSet<String>(
                previous.getRootConceptNames());

        for (String oldKind : oldKinds) {
            final HashMap<String, String> concepts = moveMap.get(oldKind);

            if (concepts != null && concepts.size() > 0) {
                final TreeSet<String> conceptSet = new TreeSet<String>(
                        concepts.keySet());

                for (String code : conceptSet) {
                    final String currentKind = concepts.get(code);
                    final String currentKindName = current
                            .getPreferredName(currentKind);
                    final String previousKindName = current
                            .getPreferredName(oldKind);
                    final String name = current.getPreferredName(code);
                    pwDiff.println(code + "\t" + name + "\t" + currentKind
                            + "\t" + currentKindName + "\t" + oldKind + "\t"
                            + previousKindName);
                }
            }
        }
        pwDiff.println();
        pwDiff.flush();
    }

    final void printChangedParents(
            final HashMap<String, HashMap<String, String>> changedParents) {
        pwDiff.println("Concepts that have been retreed in " + currentFilename
                + " relative to " + previousFilename);
        pwDiff.println("Code\tName\tKind\tCurrent Parents\tPrevious Parents");
        final TreeSet<String> rootSet = new TreeSet<String>(
                changedParents.keySet());
        for (String rootName : rootSet) {
            final HashMap<String, String> concepts = changedParents
                    .get(rootName);
            final TreeSet<String> conceptSet = new TreeSet<String>(
                    concepts.keySet());
            for (String code : conceptSet) {
                final String changes = concepts.get(code);
                final String name = current.getPreferredName(code);

                pwDiff.println(code + "\t" + name + "\t" + rootName + "\t"
                        + changes);
            }
        }
        pwDiff.println();
        pwDiff.flush();
    }

    final void printConceptCounts(final HashMap<String, Integer> conceptCounts,
                                  final HashMap<String, Integer> defined) {

        pw.println("Concept counts per root for file " + currentFilename
                + " (count of defined concepts in second row)");
        final TreeSet<String> tSet = new TreeSet<String>(current.getRootMap()
                .keySet());
        Iterator<String> iter = tSet.iterator();
        while (iter.hasNext()) {
            pw.print(iter.next() + "\t");
        }
        pw.println();
        iter = tSet.iterator();
        while (iter.hasNext()) {
            final String key = iter.next();
            pw.print(conceptCounts.get(key).toString() + "\t");
        }
        pw.println();
        iter = tSet.iterator();
        while (iter.hasNext()) {
            final String key = iter.next();
            pw.print(defined.get(key).toString() + "\t");
        }

        pw.println();
    }

    final void printConceptDiff(final HashMap<String, Integer> conceptCounts,
                                final HashMap<String, Integer> defined) {

        pw.println("Concept count diff per root for file " + currentFilename
                + " (count of defined concepts in second row)");
        final TreeSet<String> tSet = new TreeSet<String>(current.getRootMap()
                .keySet());
        Iterator<String> iter = tSet.iterator();
        while (iter.hasNext()) {
            pw.print(iter.next() + "\t");
        }
        pw.println();
        iter = tSet.iterator();
        while (iter.hasNext()) {
            final String key = iter.next();
            pw.print(conceptCounts.get(key).toString() + "\t");
        }
        pw.println();
        iter = tSet.iterator();
        while (iter.hasNext()) {
            final String key = iter.next();
            pw.print(defined.get(key).toString() + "\t");
        }

        pw.println();
        pw.println();
    }

    final void printDefinitions(final HashMap<String, String> definitions) {
        pwDiff.println("Concepts with changed definitions");
        pwDiff.println("Code\tName\tNew_Definition\tOld_Definition");
        final TreeSet<String> keySet = new TreeSet<String>(definitions.keySet());
        final Iterator<String> iter = keySet.iterator();
        String fullDef = new String();
        while (iter.hasNext()) {
            final String key = iter.next();
            fullDef = definitions.get(key);
            final String name = current.getPreferredName(key);
            pwDiff.println(key + "\t" + name + "\t" + fullDef);
        }
        pwDiff.println();
        pwDiff.flush();
    }

    private void printDeletedConcepts(final Vector<String> concepts) {
        pwDiff.println("Deleted concepts present in " + previousFilename
                + " but not in " + currentFilename);
        pwDiff.println("Previous Root\tRoot Name\tCode\tName");
        final HashMap<String, Vector<String>> previousCpk = previous
                .getConceptsPerKind();
        final TreeSet<String> rootSet = new TreeSet<String>(
                previousCpk.keySet());
        final Iterator<String> rootIter = rootSet.iterator();
        final TreeSet<String> sortConcepts = new TreeSet<String>(concepts);
        while (rootIter.hasNext()) {
            final String root = rootIter.next();
            for (String searchCode : sortConcepts) {
                final Vector<String> conceptSet = previousCpk.get(root);
                if (conceptSet.contains(searchCode)) {
                    final String name = previous.getPreferredName(searchCode);
                    final String rootName = previous.getPreferredName(root);
                    pwDiff.println(root + "\t" + rootName + "\t" + searchCode
                            + "\t" + name);
                }
            }
        }
        pwDiff.println();
        pwDiff.flush();
    }

    final void printHashSet(final String comment, final HashSet<String> set) {
        pw.println(comment);
        if (set != null) {
            pw.println(set.toString());
        }
        pw.println();
        pw.flush();
    }

    final void printHeaderCounts(
            final HashMap<headerField, Integer> headerCounts) {

        pw.println("Header definition counts for file " + currentFilename);
        pw.println("Concepts\tNamespaces\tRoots\tRoles\tProperties\tAssociations");
        String printOut = headerCounts.get(headerField.Concepts).toString();
        printOut = printOut + "\t";
        printOut = printOut
                + headerCounts.get(headerField.Namespaces).toString();
        printOut = printOut + "\t";
        printOut = printOut + headerCounts.get(headerField.Roots).toString();
        printOut = printOut + "\t";
        printOut = printOut + headerCounts.get(headerField.Roles).toString();
        printOut = printOut + "\t";
        printOut = printOut
                + headerCounts.get(headerField.Properties).toString();
        printOut = printOut + "\t";
        printOut = printOut
                + headerCounts.get(headerField.Associations).toString();
        pw.println(printOut);

        System.out.println(printOut);

        pw.println();
        pw.flush();
    }

    final void printHeaderDiff(final HashMap<headerField, Integer> inHeaderDiff) {

        pw.println("Header diff for file " + currentFilename + " minus "
                + previousFilename);
        pw.println("Concepts\tNamespaces\tRoots\tRoles\tProperties\tAssociations");
        String printOut = inHeaderDiff.get(headerField.Concepts).toString();
        printOut = printOut + "\t";
        printOut = printOut
                + inHeaderDiff.get(headerField.Namespaces).toString();
        printOut = printOut + "\t";
        printOut = printOut + inHeaderDiff.get(headerField.Roots).toString();
        printOut = printOut + "\t";
        printOut = printOut + inHeaderDiff.get(headerField.Roles).toString();
        printOut = printOut + "\t";
        printOut = printOut
                + inHeaderDiff.get(headerField.Properties).toString();
        printOut = printOut + "\t";
        printOut = printOut
                + inHeaderDiff.get(headerField.Associations).toString();
        pw.println(printOut);
        pw.println();
        pw.flush();
    }

    public void printHelp() {
        System.out.println("");
        System.out.println("Usage: OWLSummary [OPTIONS] ");
        System.out
                .println("  -C [configFile]\tRelative path to owlsummary.properties file (optional)");
        System.out.println("  -I, --Input\t\tString of OWL file to be summarized");
        System.out
                .println("  -P, --Previous\tString of previous OWL file if diff desired (optional)");
        System.out.println("  -S, --Summary\t\tString to print Summary File");
        System.out
                .println("  -D, --Details\t\tString to print Details File (optional)");
        System.out.println("");
        System.exit(1);
    }

    private void printNewConcepts(final Vector<String> concepts) {
        pwDiff.println("New concepts present in " + currentFilename
                + " but not in " + previousFilename);
        pwDiff.println("Root\tRootName\tCode\tName");
        final HashMap<String, Vector<String>> currentCpk = current
                .getConceptsPerKind();
        final TreeSet<String> rootSet = new TreeSet<String>(currentCpk.keySet());
        for (String root : rootSet) {
            for (String searchCode : concepts) {
                final TreeSet<String> conceptSet = new TreeSet<String>(
                        currentCpk.get(root));
                if (conceptSet.contains(searchCode)) {
                    final String rootname = current.getPreferredName(root);
                    final String name = current.getPreferredName(searchCode);
                    pwDiff.println(root + "\t" + rootname + "\t" + searchCode
                            + "\t" + name);
                }
            }
        }
        pwDiff.println();
        pwDiff.flush();
    }

    final void printPreferredNames(
            final HashMap<String, String> preferredNames,
            final HashMap<String, Vector<String>> semanticTypes,
            final PrintWriter writer) {
        writer.println("Concepts with changed Preferred_Name");
        writer.println("Code\tCurrent Semantic_Type\tNew Preferred_Name\tOld Preferred_Name");
        final TreeSet<String> keySet = new TreeSet<String>(
                preferredNames.keySet());
        final Iterator<String> iter = keySet.iterator();

        String fullPN = new String();
        while (iter.hasNext()) {
            final String key = iter.next();
            fullPN = preferredNames.get(key);
            if (semanticTypes.containsKey(key)) {
                Vector<String> stys = new Vector<String>();
                stys = semanticTypes.get(key);
                if (stys != null) {
                    for (String sty : stys) {
                        writer.println(key + "\t" + sty + "\t" + fullPN);
                    }
                }
            } else {
                writer.println(key + "\t\t" + fullPN);
            }
        }
        writer.println();
        writer.flush();
    }

    final void printProperties(
            final HashMap<String, HashMap<String, Integer>> propertyCounts) {

        pw.println("Property counts per root for file " + currentFilename);
        final TreeSet<String> propSet = new TreeSet<String>(current
                .getPropertyMap().keySet());
        final Iterator<String> propIter = propSet.iterator();
        final TreeSet<String> rootSet = new TreeSet<String>(current
                .getRootMap().keySet());
        Iterator<String> rootIter = rootSet.iterator();
        pw.print("\t");
        while (rootIter.hasNext()) {
            pw.print("\t" + rootIter.next());
        }
        pw.print("\tCumulative");
        pw.println();

        while (propIter.hasNext()) {
            rootIter = rootSet.iterator();
            final String propCode = propIter.next();
            final String propName = (String) current.getStringValuedAnnotationPropertyCode2NameMap().get(propCode);
            Integer propCumulative = 0;


            pw.print(propCode + "\t" + propName + "\t");
            while (rootIter.hasNext()) {
                final String root = rootIter.next();
                final HashMap<String, Integer> propMap = propertyCounts
                        .get(root);
                Integer propertyCount = propMap.get(propCode);
                if (propertyCount == null) {
                    propertyCount = 0;
                }
                propCumulative = propCumulative + propertyCount;
                pw.print(propertyCount + "\t");
            }
            pw.print(propCumulative);
            pw.println();
        }
        pw.println();
        pw.flush();
    }

    final void printPropertyDiff(
            final HashMap<String, HashMap<String, Integer>> propertyDiff) {

        pw.println("Property count diff per root for file " + currentFilename
                + " minus " + previousFilename);
        final TreeSet<String> propSet = new TreeSet<String>(current
                .getPropertyMap().keySet());
        final Iterator<String> propIter = propSet.iterator();
        final TreeSet<String> rootSet = new TreeSet<String>(current
                .getRootMap().keySet());
        Iterator<String> rootIter = rootSet.iterator();
        pw.print("\t");
        while (rootIter.hasNext()) {
            pw.print("\t" + rootIter.next());
        }
        pw.print("\tCumulative");
        pw.println();

        while (propIter.hasNext()) {
            rootIter = rootSet.iterator();
            final String propCode = propIter.next();
            final String propName = (String) current.getStringValuedAnnotationPropertyCode2NameMap().get(propCode);
            Integer propCumulative = 0;
            pw.print(propCode + "\t" + propName + "\t");
            while (rootIter.hasNext()) {
                final String root = rootIter.next();
                final HashMap<String, Integer> propMap = propertyDiff.get(root);
                if (propMap != null) {
                    Integer propertyCount = propMap.get(propCode);
                    if (propertyCount == null) {
                        propertyCount = 0;
                    }
                    propCumulative = propCumulative + propertyCount;
                    pw.print(propertyCount + "\t");
                }
            }
            pw.print(propCumulative);
            pw.println();
        }
        pw.println();
        pw.flush();

    }

    private void printRetiredConcepts(final Vector<String> concepts) {
        pwDiff.println("Concepts retired in " + currentFilename
                + " but not in " + previousFilename);
        pwDiff.println("Previous Root\tRoot Name\tCode\tName");
        final HashMap<String, Vector<String>> previousCpk = previous
                .getConceptsPerKind();
        final TreeSet<String> rootSet = new TreeSet<String>(
                previousCpk.keySet());
        for (String root : rootSet) {
            for (String searchCode : concepts) {
                final TreeSet<String> conceptSet = new TreeSet<String>(
                        previousCpk.get(root));
                if (conceptSet.contains(searchCode)) {
                    final String rootName = current.getPreferredName(root);
                    final String name = current.getPreferredName(searchCode);
                    pwDiff.println(root + "\t" + rootName + "\t" + searchCode
                            + "\t" + name);
                }
            }
        }
        pwDiff.println();
        pwDiff.flush();
    }

    final void printRoleDiff(
            final HashMap<String, HashMap<String, Integer>> roleDiff) {

        pw.println("Role count diff per root for file " + currentFilename
                + " minus " + previousFilename);
        final TreeSet<String> roleSet = new TreeSet<String>(current
                .getRoleMap().keySet());
        final Iterator<String> roleIter = roleSet.iterator();
        final TreeSet<String> rootSet = new TreeSet<String>(current
                .getRootMap().keySet());
        Iterator<String> rootIter = rootSet.iterator();
        pw.print("\t");
        while (rootIter.hasNext()) {
            pw.print("\t" + rootIter.next());
        }
        pw.print("\tCumulative");
        pw.println();

        while (roleIter.hasNext()) {
            rootIter = rootSet.iterator();
            final String roleCode = roleIter.next();
            final String roleName = (String) current.getObjectPropertyCode2NameMap().get(roleCode);
            Integer roleCumulative = 0;
            pw.print(roleCode + "\t" + roleName + "\t");
            while (rootIter.hasNext()) {
                final String root = rootIter.next();
                final HashMap<String, Integer> roleMap = roleDiff.get(root);
                if (roleMap != null) {
                    Integer roleCount = roleMap.get(roleCode);
                    if (roleCount == null) {
                        roleCount = 0;
                    }
                    pw.print(roleCount + "\t");
                    roleCumulative = roleCumulative + roleCount;
                }
            }
            pw.print(roleCumulative);
            pw.println();
        }
        pw.println();
        pw.flush();
    }

    final void printRoles(
            final HashMap<String, HashMap<String, Integer>> roleCounts) {

        pw.println("Role counts per root for file " + currentFilename);
        final TreeSet<String> roleSet = new TreeSet<String>(current
                .getRoleMap().keySet());
        final Iterator<String> roleIter = roleSet.iterator();
        final TreeSet<String> rootSet = new TreeSet<String>(current
                .getRootMap().keySet());
        Iterator<String> rootIter = rootSet.iterator();
        pw.print("\t");
        while (rootIter.hasNext()) {
            pw.print("\t" + rootIter.next());
        }
        pw.print("\tCumulative");
        pw.println();

        while (roleIter.hasNext()) {
            rootIter = rootSet.iterator();
            final String roleCode = roleIter.next();
            final String roleName = (String) current.getObjectPropertyCode2NameMap().get(roleCode);
            Integer roleCumulative = 0;
            pw.print(roleCode + "\t" + roleName + "\t");
            while (rootIter.hasNext()) {
                final String root = rootIter.next();
                final HashMap<String, Integer> roleMap = roleCounts.get(root);
                Integer roleCount = roleMap.get(roleCode);
                if (roleCount == null) {
                    roleCount = 0;
                }
                pw.print(roleCount + "\t");
                roleCumulative = roleCumulative + roleCount;
            }
            pw.print(roleCumulative);
            pw.println();
        }
        pw.println();
        pw.flush();

    }


    final void printStringHashMap(final String comment,
                               final HashMap<String, String> map) {
        pw.println(comment);
        if (map != null) {
            pw.println(map);
        }
        pw.println();
        pw.flush();
    }

    final void printStringHashSet(final String comment, final HashSet<String> set) {
        pw.println(comment);
        if (set != null) {
            pw.println(set.toString());
        }
        pw.println();
        pw.flush();
    }

    private void printUnretiredConcepts(final Vector<String> concepts) {
        pwDiff.println("Concepts retired in " + previousFilename
                + " but not in " + currentFilename);
        pwDiff.println("Current Root\tRootName\tCode\tName");
        final HashMap<String, Vector<String>> currentCpk = current
                .getConceptsPerKind();
        final TreeSet<String> rootSet = new TreeSet<String>(currentCpk.keySet());
        for (String root : rootSet) {
            for (String searchCode : concepts) {
                final TreeSet<String> conceptSet = new TreeSet<String>(
                        currentCpk.get(root));
                if (conceptSet.contains(searchCode)) {
                    final String name = current.getPreferredName(searchCode);
                    final String rootName = current.getPreferredName(root);
                    pwDiff.println(root + "\t" + rootName + "\t" + searchCode
                            + "\t" + name);
                }
            }
        }
        pwDiff.println();
        pwDiff.flush();
    }

    private void retiredConcepts() {
        final HashMap<String, Vector<String>> currentConceptsPerKind = current
                .getConceptsPerKind();
        final HashMap<String, Vector<String>> previousConceptsPerKind = previous
                .getConceptsPerKind();
        Vector<String> currentRetiredConcepts = currentConceptsPerKind
                .get("Retired Concept");
        if (currentRetiredConcepts == null) {
            currentRetiredConcepts = currentConceptsPerKind.get("Retired_Kind");
        }
        if (currentRetiredConcepts == null) {
            currentRetiredConcepts = currentConceptsPerKind.get("C28428");
        }
        Vector<String> previousRetiredConcepts = previousConceptsPerKind
                .get("Retired Concept");
        if (previousRetiredConcepts == null) {
            previousRetiredConcepts = previousConceptsPerKind
                    .get("Retired_Kind");
        }
        if (previousRetiredConcepts == null) {
            previousRetiredConcepts = previousConceptsPerKind.get("C28428");
        }
        final Vector<String> extra = compareStringVector(currentRetiredConcepts,
                previousRetiredConcepts);
        printRetiredConcepts(extra);
        final Vector<String> missing = compareStringVector(previousRetiredConcepts,
                currentRetiredConcepts);
        printUnretiredConcepts(missing);
    }

    private enum headerField {

        Associations,
        Concepts,
        Namespaces,
        Properties,
        Roles,
        Roots
    }
}

