package gov.nih.nci.evs.restapi.appl;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import gov.nih.nci.evs.restapi.util.*;

public class ProgressBarMaker extends JFrame {
    // create a frame
    static JFrame f;
    static JProgressBar b;
    public static final int EXIT_ON_CLOSE = 3;

    InferredFileGenerator generator = null;
    Vector classIdVec = null;
    int total = 0;
    int num_increments = 0;

    public ProgressBarMaker(InferredFileGenerator generator, Vector classIdVec) {
		this.generator = generator;
		this.classIdVec = classIdVec;
	}

    public Vector run() {
        // create a frame
        f = new JFrame("Progress Bar");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        // create a panel
        JPanel p = new JPanel();

        // create a progressbar
        b = new JProgressBar();
		b.setMinimum(0);
		b.setMaximum(this.classIdVec.size());

        // set initial value
        b.setValue(0);

        b.setStringPainted(true);

        // add progressbar
        p.add(b);

        // add panel
        f.add(p);

        // set the size of the frame
        f.setSize(400, 80);
        f.setVisible(true);
        Vector w = fill();
        f.setVisible(false);
        return w;
    }

    public Vector fill() {
		Vector w = new Vector();
		int lcv = 1;
		int increment = 10000;
		int k = -1;

		for (int i=0; i<classIdVec.size(); i++) {
			int j = i+1;
			if (lcv == increment) {
				k++;
				b.setValue(k*increment);
				lcv = 0;
			}
			lcv++;
			String code = (String) classIdVec.elementAt(i);
			if (StringUtils.isNCItCode(code)) {
				Vector classData = generator.getOWLClassLoader().getClassData(code);
				if (generator.disjointWithMap.containsKey(code)) {
					classData = generator.fixOwlDisjointWith(code, classData);
		        }
				//=========================================================================
				classData = generator.appendInheritedRestrictions(code, classData);
				//=========================================================================
				classData = generator.remove_axioms(classData, generator.P325, generator.LITERAL);
				classData = generator.addPrefix2PropertyValue(classData, generator.HASDBXREF);
				w.add("\n\n");
				w.addAll(classData);
			} else {
				generator.removed_concepts.add(code);
			}
		}
		return w;
	}

}