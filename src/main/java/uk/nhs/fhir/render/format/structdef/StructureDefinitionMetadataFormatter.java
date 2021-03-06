package uk.nhs.fhir.render.format.structdef;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.NotImplementedException;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import uk.nhs.fhir.data.structdef.FhirContacts;
import uk.nhs.fhir.data.wrap.WrappedStructureDefinition;
import uk.nhs.fhir.render.format.FhirContactRenderer;
import uk.nhs.fhir.render.format.HTMLDocSection;
import uk.nhs.fhir.render.format.TableFormatter;
import uk.nhs.fhir.render.html.Elements;
import uk.nhs.fhir.render.html.panel.FhirPanel;
import uk.nhs.fhir.render.html.style.FhirCSS;
import uk.nhs.fhir.render.html.table.Table;
import uk.nhs.fhir.util.StringUtil;

public class StructureDefinitionMetadataFormatter extends TableFormatter<WrappedStructureDefinition> {

	public StructureDefinitionMetadataFormatter(WrappedStructureDefinition wrappedResource) {
		super(wrappedResource);
	}

	@Override
	public HTMLDocSection makeSectionHTML() throws ParserConfigurationException {
		HTMLDocSection section = new HTMLDocSection();
		
		Element metadataPanel = getMetadataTable(wrappedResource);
		section.addBodyElement(metadataPanel);
		
		getStyles().forEach(section::addStyle);
		Table.getStyles().forEach(section::addStyle);
		FhirPanel.getStyles().forEach(section::addStyle);
		
		return section;
	}
	
	private static final Map<String, String> HL7_LOGICAL_URL_TO_TRUE_URL = Maps.newHashMap();
	static {
		HL7_LOGICAL_URL_TO_TRUE_URL.put("http://hl7.org/fhir/StructureDefinition/Period", "http://hl7.org/fhir/datatypes.html#period");
	}
	
	public Element getMetadataTable(WrappedStructureDefinition structureDefinition) {
		
		String name = structureDefinition.getName();
		String url = structureDefinition.getUrl().get();
		String kind = structureDefinition.getKindDisplay();
		
		String status = structureDefinition.getStatus();
		Boolean isAbstract = structureDefinition.getAbstract();
		String displayIsAbstract = isAbstract ? "Yes" : "No";
		
		Optional<String> constrainedType = structureDefinition.getConstrainedType();
		
		Optional<String> baseUrl = Optional.ofNullable(structureDefinition.getBase());
		
		Optional<String> version = StringUtil.firstPresent(structureDefinition.getVersionId(), structureDefinition.getVersion());
		Optional<String> display = structureDefinition.getDisplay();
		
		Optional<String> description = structureDefinition.getDescription();
		
		// never used in NHS Digital profiles
		/*
		String displayExperimental;
		Boolean experimental = source.getExperimental();
		if (experimental == null) {
			displayExperimental = BLANK;
		} else {
			displayExperimental = experimental ? "Yes" : "No";
		}*/
		
		Optional<String> publisher = structureDefinition.getPublisher();
		
		
		Optional<String> displayDate = 
			structureDefinition.getDate()
				.map(date -> StringUtil.dateToString(date));
		
		Optional<String> copyrightInfo = structureDefinition.getCopyright();
		
		Optional<String> fhirVersionDesc = structureDefinition.getFhirVersion();
		
		Optional<String> contextType = structureDefinition.getContextType();
		
		List<FhirContacts> publishingOrgContacts = structureDefinition.getContacts();
		
		List<String> useContexts = structureDefinition.getUseContexts();
		
		// JE - information only relevant to the base resource - not relevant to the profile
		/*List<FhirMapping> mappings = structureDefinition.getMappings();
		List<Content> externalSpecMappings = Lists.newArrayList();
		boolean multipleMappings = mappings.size() >= 2;
		if (multipleMappings) {
			externalSpecMappings.add(0, Elements.withAttributeAndText("span", new Attribute("class", FhirCSS.DATA_BLOCK_TITLE), "External Specifications"));
		}
		for (FhirMapping mapping : mappings) {
			String displayName = mapping.getName().orElse(mapping.getIdentity());
			
			if (!externalSpecMappings.isEmpty()) {
				externalSpecMappings.add(Elements.newElement("br"));
			}
			
			displayName += ": ";
			
			externalSpecMappings.add(
				Elements.withAttributeAndText("span", new Attribute("class", FhirCSS.DATA_LABEL), displayName));
			if (mapping.getUri().isPresent()) {
				externalSpecMappings.add(Elements.withAttributeAndText("span", new Attribute("class", FhirCSS.DATA_VALUE), mapping.getUri().get()));
			}
			if (mapping.getComments().isPresent()) {
				externalSpecMappings.add(Elements.withAttributeAndText("span", new Attribute("class", FhirCSS.DATA_VALUE), "(" + mapping.getComments().get() + ")"));
			}
		}*/
		
		List<String> useLocationContexts = structureDefinition.getUseLocationContexts();
		
		Element colgroup = getColGroup(4);
		
		List<Element> tableContent = Lists.newArrayList(colgroup);
		
		tableContent.add(
			Elements.withChildren("tr",
				labelledValueCell("Name", name, 2, true),
				labelledValueCell("URL", url, 2, true)));
		tableContent.add(
			Elements.withChildren("tr",
				labelledValueCell("Version", version, 1),
				labelledValueCell("Constrained type", constrainedType, 1),
				labelledValueCell("Constrained URL", baseUrl, 1),
				labelledValueCell("Status", status, 1)));
		
		if (description.isPresent()) {
			tableContent.add(
				Elements.withChildren("tr",
					labelledValueCell("Description", description.get(), 4, true)));
		}
		
		tableContent.add(
			Elements.withChildren("tr",
				labelledValueCell("Published by", publisher, 1),
				labelledValueCell("Created date", displayDate, 1),
				labelledValueCell("Last updated", structureDefinition.getLastUpdated(), 1),
				labelledValueCell("Kind", StringUtil.capitaliseLowerCase(kind), 1)));
		tableContent.add(
			Elements.withChildren("tr",
				labelledValueCell("FHIR Version", fhirVersionDesc, 1),
				labelledValueCell("DisplayName", display, 1),
				labelledValueCell("Abstract", displayIsAbstract, 1),
				labelledValueCell("Context type", contextType, 2)));
		
		if (!publishingOrgContacts.isEmpty()) {
			List<Content> renderedPublishingOrgContacts = new FhirContactRenderer().getPublishingOrgContactsContents(publishingOrgContacts);
			tableContent.add(
				Elements.withChild("tr", 
					cell(renderedPublishingOrgContacts, 4)));
		}
		
		// JE - don't want to show this
		/*if (!externalSpecMappings.isEmpty()) {
			tableContent.add(
				Elements.withChild("tr", 
					cell(externalSpecMappings, 4)));
		}*/
		
		if (!useContexts.isEmpty()) {
			throw new NotImplementedException("UseContext");
		}
		
		if (!useLocationContexts.isEmpty()) {
			String useLocationContextsDescription = useLocationContexts.size() > 1 ? "Use context" : "Use contexts";  
			
			tableContent.add(
				Elements.withChild("tr", 
						labelledValueCell(useLocationContextsDescription, String.join(", ", useLocationContexts), 4)));
		}
		
		if (copyrightInfo.isPresent()) {
			tableContent.add(
				Elements.withChild("tr", 
					labelledValueCell("", copyrightInfo, 4)));
		}
		
		Element table = 
			Elements.withAttributeAndChildren("table",
				new Attribute("class", FhirCSS.TABLE),
				tableContent);
		
		String panelTitleName = display.isPresent() ? display.get() : name;
		String panelTitle = "Structure definition: " + panelTitleName;
		
		FhirPanel panel = new FhirPanel(panelTitle, table);
		
		return panel.makePanel();
	}
}
