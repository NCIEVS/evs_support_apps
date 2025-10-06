package gov.nih.nci.evs.restapi.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public class PropertiesReader {
	private Properties properties = null;

    public PropertiesReader(String propertyfile) {
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(propertyfile)) {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Enumeration<?> keys = properties.keys();
        System.out.println("Keys in " + propertyfile + ":");
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            System.out.println(key + " --> " + properties.get(key));
        }
    }

    public String getProperty(String key) {
		return (String) properties.get(key);
	}

    public static void main(String[] args) {
		PropertiesReader reader = new PropertiesReader("ctcae2owl.properties");
	}
}