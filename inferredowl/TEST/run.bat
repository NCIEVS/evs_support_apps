set OCP=%CLASSPATH%
set lib=..\lib
set CLASSPATH=.
set CLASSPATH=%CLASSPATH%;%lib%\*

java -Xms1024m -Xmx8g -classpath %CLASSPATH% gov.nih.nci.evs.restapi.appl.InferredFileGenerator Thesaurus-260720-26.07c.owl
                                                                                                

set CLASSPATH=%OCP%