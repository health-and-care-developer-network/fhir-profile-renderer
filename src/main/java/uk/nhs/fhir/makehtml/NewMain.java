/*
 * Copyright (C) 2016 Health and Social Care Information Centre.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.nhs.fhir.makehtml;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Level;
import java.util.logging.Logger;

import uk.nhs.fhir.makehtml.resources.ImplementationGuideHTMLMaker;

/**
 *
 * @author tim.coates@hscic.gov.uk
 */
public class NewMain {
    private static final String fileExtension = ".xml";
    private static final Logger LOG = Logger.getLogger(NewMain.class.getName());

    private final File inputDirectory;
    private final String outPath;
    private final String newBaseURL;
    
    private NewMain(File inputDirectory, String outPath, String newBaseURL) {
    	this.inputDirectory = inputDirectory;
    	this.outPath = outPath;
    	this.newBaseURL = newBaseURL;
    }
    
    /**
     * Main entry point.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

    	if((args.length == 2) || (args.length == 3)) {
            String inputDir = args[0];
            String outputDir = args[1];
            String newBaseURL = null;
            if (args.length == 3) {
            	LOG.log(Level.INFO, "Using new base URL: " + newBaseURL);
            	newBaseURL = args[2];
            }
            
            NewMain instance = new NewMain(new File(inputDir), outputDir, newBaseURL);
            
            FileProcessor fileProcessor =
        	    	new FileProcessor(
        	    		new StructureDefinitionHTMLMaker(),
        	    		new ValueSetHTMLMaker(),
        	    		new OperationDefinitionHTMLMaker(),
        	    		new ImplementationGuideHTMLMaker(new File(inputDir), newBaseURL));
            
            instance.process(fileProcessor);
        }
    }

    /**
     * Process a directory of Profile files.
     *
     * @param directoryPath
     */
    private void process(FileProcessor fileProcessor) {
    	
        File[] allProfiles = inputDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(fileExtension);
            }
        });

        for (File thisFile : allProfiles) {
        	boolean success = fileProcessor.processFile(outPath, newBaseURL, inputDirectory, thisFile);
        	if (!success) {
        		break;
        	}
        }
    }
}
