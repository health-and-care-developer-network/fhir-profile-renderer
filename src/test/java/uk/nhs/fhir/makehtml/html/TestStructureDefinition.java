package uk.nhs.fhir.makehtml.html;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.Test;

import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.model.dstu2.resource.StructureDefinition;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import uk.nhs.fhir.data.wrap.WrappedResource;
import uk.nhs.fhir.data.wrap.WrappedStructureDefinition;
import uk.nhs.fhir.makehtml.FormattedOutputSpec;
import uk.nhs.fhir.makehtml.html.jdom2.HTMLUtil;
import uk.nhs.fhir.makehtml.render.HTMLDocSection;
import uk.nhs.fhir.makehtml.render.ResourceFormatter;
import uk.nhs.fhir.makehtml.render.ResourceFormatterFactory;
import uk.nhs.fhir.makehtml.render.SectionedHTMLDoc;
import uk.nhs.fhir.util.FhirContexts;
import uk.nhs.fhir.util.FhirVersion;
import uk.nhs.fhir.util.SimpleFhirFileLocator;

public class TestStructureDefinition {
	private int BOM = 0xFEFF;
	
	private static final String testOutputPath = System.getProperty("user.home") + "/Desktop/test.html";
	
	@Ignore
	@Test
	public void testBuildStructureDefinition() throws FileNotFoundException, IOException, ConfigurationException, DataFormatException, ParserConfigurationException {
		IParser parser = FhirContexts.forVersion(FhirVersion.DSTU2).newXmlParser();
		try (
			BufferedReader reader = new BufferedReader(new FileReader(getClass().getClassLoader().getResource("example_structure_definition3.xml").getFile()));
		) {
			reader.mark(1);
			int read = reader.read();
			if (read != BOM) {
				System.out.println("First char: " + Integer.toHexString(read));
				reader.reset();
			}
			
			StructureDefinition structureDefinition = (StructureDefinition)parser.parseResource(reader);
			WrappedStructureDefinition wrappedStructureDefinition = (WrappedStructureDefinition) WrappedResource.fromBaseResource(structureDefinition);
			SectionedHTMLDoc doc = new SectionedHTMLDoc();
			
			SimpleFhirFileLocator renderingFileLocator = new SimpleFhirFileLocator(Paths.get("this/path/isnt/used"), Paths.get("this/path/isnt/used"));
			
			for (FormattedOutputSpec<?> formatSpec : new ResourceFormatterFactory().allFormatterSpecs(wrappedStructureDefinition, renderingFileLocator, null)) {
				ResourceFormatter<?> formatter = formatSpec.getFormatter();
				HTMLDocSection sectionHTML = formatter.makeSectionHTML();
				if (sectionHTML != null) {
					doc.addSection(sectionHTML);
				}
			}
			
			Files.write(Paths.get(testOutputPath), HTMLUtil.docToString(doc.getHTML(), true, false).getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		}
	}
}
