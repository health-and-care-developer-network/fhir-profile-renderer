/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package uk.nhs.fhir.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Convenience class to read configuration from a property file
 * @author Adam Hatherly
 */
public class PropertyReader {
	public static final String PROPERTY_FILE = "config.properties";

	private static final Logger logger = Logger.getLogger(PropertyReader.class.getName());
    private static final Properties defaultProperties = new Properties();

    // When this class is loaded by the JVM, immediately read the property file
    static {
    	initialise(defaultProperties, PROPERTY_FILE);        
    }

    /**
     * Load the property values into a local object from the property file.
     */
    private static void initialise(Properties props, String filename) {
        
        try (InputStream in = PropertyReader.class.getClassLoader().getResourceAsStream(filename)){
            props.load(in);
        } catch (IOException ex) {
       		logger.severe("Config file not found: " + filename);
        }
    }

    /**
     * Retrieve the value of the property with the specified name
     * @param propertyName Name of property to retrieve
     * @return Value of property
     */
    public static String getProperty(String propertyName) {
		String val =  defaultProperties.getProperty(propertyName);
		if (val != null)
			return val;
		else
			return null;
    }

}
