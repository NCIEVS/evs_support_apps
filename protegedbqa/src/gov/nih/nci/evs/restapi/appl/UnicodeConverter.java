package gov.nih.nci.evs.restapi.appl;
import java.io.*;
import java.util.*;


public class UnicodeConverter {

    private String unicodeString;
    private String asciiString;
    private char unicodeChar;
    private String charDescription;


    public UnicodeConverter(String inUnicodeString, String inAsciiString, char inUnicodeChar, String inCharDescription){
        unicodeString = inUnicodeString;
        asciiString = inAsciiString;
        unicodeChar = inUnicodeChar;
        charDescription = inCharDescription;
    }

    public UnicodeConverter(String line) throws Exception{

        String[] symbolArray = line.split("\\.\\|\\.", 0);

        if(symbolArray.length==4){
            unicodeString = symbolArray[0];
            charDescription = symbolArray[3];
            char[] toss = symbolArray[2].toCharArray();
            if(toss.length==1){
                unicodeChar = toss[0];
                String r = String.format("u+%04x", (int) unicodeChar);
                if(!r.toUpperCase().equals(unicodeString.toUpperCase())){
                    throw new Exception("Unicode character does not match unicode description " + unicodeString + " "+ charDescription);
                }
            } else if (toss.length==2){
            	unicodeChar = toss[0];
            	String r = String.format("u+%04x", (int) unicodeChar);
            	if(!r.toUpperCase().equals(unicodeString.toUpperCase())){
            		unicodeChar =  toss[1];
            	}
            	r = String.format("u+%04x", (int) unicodeChar);
            	if(!r.toUpperCase().equals(unicodeString.toUpperCase())){
            		throw new Exception("Unicode character does not match unicode description " + unicodeString + " "+ charDescription);
            	}

            } else {
                throw new Exception("Invalid unicode character");
            }
            asciiString = symbolArray[1];



        } else {
            throw new Exception("invalid line " + line);
        }

    }

    public String getUnicodeString() {
        return unicodeString;
    }

    public String getAsciiChar() {
        return asciiString;
    }

    public char getUnicodeChar() {
        return unicodeChar;
    }

    public String getCharDescription() {
        return charDescription;
    }



}
