package uk.nhs.fhir.makehtml.render.structdef;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import uk.nhs.fhir.makehtml.FhirURLConstants;
import uk.nhs.fhir.makehtml.data.BindingInfo;
import uk.nhs.fhir.makehtml.data.ConstraintInfo;
import uk.nhs.fhir.makehtml.data.FhirURL;
import uk.nhs.fhir.makehtml.data.LinkData;
import uk.nhs.fhir.makehtml.data.ResourceFlags;
import uk.nhs.fhir.makehtml.data.SlicingInfo;
import uk.nhs.fhir.makehtml.html.FhirCSS;
import uk.nhs.fhir.makehtml.html.LinkCell;
import uk.nhs.fhir.makehtml.html.jdom2.Elements;
import uk.nhs.fhir.makehtml.html.style.CSSRule;
import uk.nhs.fhir.makehtml.html.style.CSSStyleBlock;
import uk.nhs.fhir.util.StringUtil;

public class StructureDefinitionDetails {
	
	private final String pathName;
	private final String key;
	private final Optional<String> definition;
	private final String cardinality;
	private final Optional<BindingInfo> binding;
	private final List<LinkData> typeLinks;
	private final Optional<String> requirements;
	private final List<String> aliases;
	private final ResourceFlags resourceFlags;
	private final Optional<String> comments;
	private final Optional<SlicingInfo> slicing;
	private final List<ConstraintInfo> inheritedConstraints;
	private final List<ConstraintInfo> profileConstraints;
	
	public StructureDefinitionDetails(String pathName, String key, Optional<String> definition, String cardinality, Optional<BindingInfo> binding, 
			List<LinkData> typeLinks, Optional<String> requirements, List<String> aliases, ResourceFlags resourceFlags,
			Optional<String> comments, Optional<SlicingInfo> slicing, List<ConstraintInfo> inheritedConstraints, 
			List<ConstraintInfo> profileConstraints) {
		this.pathName = pathName;
		this.key = key;
		this.definition = definition;
		this.cardinality = cardinality;
		this.binding = binding;
		this.typeLinks = typeLinks;
		this.requirements = requirements;
		this.aliases = aliases;
		this.resourceFlags = resourceFlags;
		this.comments = comments;
		this.slicing = slicing;
		this.inheritedConstraints = inheritedConstraints;
		this.profileConstraints = profileConstraints;
	}

	public void addContent(List<Element> tableContent) {
		tableContent.add(getHeaderRow(key));
		
		addDataIfPresent(tableContent, "Definition", definition);
		addLabelWithLinkDataRow(tableContent, "Cardinality", FhirURL.createOrThrow(FhirURLConstants.HL7_CONFORMANCE + "#cardinality"), cardinality);
		addBindingRowIfPresent(tableContent);
		tableContent.add(getLinkRow("Type", FhirURL.createOrThrow(FhirURLConstants.HL7_DATATYPES), typeLinks));
		addDataIfPresent(tableContent, "Requirements", requirements);
		addListDataIfPresent(tableContent, "Alternate Names", aliases);
		addChoiceNoteIfPresent(tableContent);
		addResourceFlags(tableContent);
		addDataIfPresent(tableContent, "Comments", comments);
		addConstraints(tableContent);
		addSlicing(tableContent);
	}

	private void addChoiceNoteIfPresent(List<Element> tableContent) {
		try {
			if (pathName.endsWith("[x]")) {
				tableContent.add(
					getDataRow(
						dataCell("[x] Note", FhirCSS.DETAILS_DATA_CELL), 
						Elements.withAttributeAndChildren("td", 
							new Attribute("class", FhirCSS.DETAILS_DATA_CELL), 
							Lists.newArrayList(
								new Text("See "),
								Elements.withAttributesAndText("a",
									Lists.newArrayList(
										new Attribute("href", new FhirURL(FhirURLConstants.HL7_FORMATS + "#choice").toLinkString()),
										new Attribute("class", FhirCSS.LINK)), 
									"Choice of Data Types"),
								new Text(" for further information about how to use [x]")))));
			}
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}

	private Element getHeaderRow(String header) {
		return Elements.withAttributeAndChild("tr", 
			new Attribute("class", FhirCSS.DETAILS_HEADER_ROW), 
			Elements.withAttributesAndChildren("td",
				Lists.newArrayList(
					new Attribute("class", FhirCSS.DETAILS_HEADER_CELL), 
					new Attribute("colspan", "2")), 
				Lists.newArrayList(
					Elements.withAttribute("a", 
						new Attribute("name", header)),
					new Text(header))));
	}

	private void addDataIfPresent(List<Element> tableContent, String label, Optional<String> content) {
		if (content.isPresent()) {
			addData(tableContent, label, content.get());
		}
	}

	private void addData(List<Element> tableContent, String label, String content) {
		tableContent.add(simpleStringDataRow(label, content)); 
	}
	
	private void addLabelWithLinkDataRow(List<Element> tableContent, String label, FhirURL url, String content) {
		Element labelCell = linkCell(label, url);
		Element dataCell = dataCell(content, FhirCSS.DETAILS_DATA_CELL);
		
		tableContent.add(getDataRow(labelCell, dataCell));
	}
	
	private Element linkCell(String label, FhirURL url) {
		return Elements.withAttributeAndChild("td", 
			new Attribute("class", FhirCSS.DETAILS_DATA_CELL),
			Elements.withAttributesAndText("a", 
				Lists.newArrayList(
					new Attribute("href", url.toLinkString()),
					new Attribute("class", FhirCSS.LINK)),
				label));
	}
	
	private Element simpleStringDataRow(String title, String content) {
		
		Element labelCell = dataCell(title, FhirCSS.DETAILS_DATA_CELL);
		Element dataCell = dataCell(content, FhirCSS.DETAILS_DATA_CELL);
		
		return getDataRow(labelCell, dataCell);
	}
	
	private Element dataCell(String content, String classString) {
		return Elements.withAttributeAndText("td",
			new Attribute("class", classString),
			content);
	}
	
	private Element getDataRow(Element labelCell, Element dataCell) {
		return Elements.withAttributeAndChildren("tr", 
			new Attribute("class", FhirCSS.DETAILS_DATA_ROW),
				Lists.newArrayList(
					labelCell,
					dataCell));
	}

	private void addBindingRowIfPresent(List<Element> tableContent) {
		if (binding.isPresent()) {
			BindingInfo info = binding.get();

			String bindingInfo = "";
			
			boolean hasUrl = info.getUrl().isPresent();
			boolean hasDesc = info.getDescription().isPresent();
			
			if (hasUrl) {
				String fullUrl = info.getUrl().get().toFullString();
				String hyphenatedUrlName = fullUrl.substring(fullUrl.lastIndexOf('/') + 1);
				String urlName = StringUtil.hyphenatedToPascalCase(hyphenatedUrlName);
				bindingInfo += urlName;
			}
			
			if (hasUrl && hasDesc) {
				bindingInfo += ": ";
			}
			
			if (hasDesc) {
				bindingInfo += info.getDescription().get();
			}
			
			bindingInfo += " (" + info.getStrength() + ")";
			
			addLabelWithLinkDataRow(tableContent, "Binding", FhirURL.createOrThrow(FhirURLConstants.HL7_TERMINOLOGIES), bindingInfo);
		}
	}
	
	private Element getLinkRow(String title, FhirURL titleLink, List<LinkData> linkDatas) {
		return Elements.withAttributeAndChildren("tr", 
			new Attribute("class", FhirCSS.DETAILS_DATA_ROW),
				Lists.newArrayList(
					linkCell(title, titleLink),
					linkCell(linkDatas)));
	}
	
	private Element linkCell(List<LinkData> linkDatas) {
		return new LinkCell(linkDatas, Sets.newHashSet(FhirCSS.DETAILS_DATA_CELL), Sets.newHashSet(), false, false).makeCell();
	}

	private void addListDataIfPresent(List<Element> tableContent, String label, List<String> listData) {
		if (!aliases.isEmpty()) {
			addData(tableContent, label, String.join("; ", listData));
		}
	}

	private void addResourceFlags(List<Element> tableContent) {
		addDataIfTrue(tableContent, "Summary", FhirURL.createOrThrow(FhirURLConstants.HL7_SEARCH + "#summary"), resourceFlags.isSummary());
		addDataIfTrue(tableContent, "Modifier", FhirURL.createOrThrow(FhirURLConstants.HL7_CONFORMANCE + "#ismodifier"), resourceFlags.isModifier());
		//addDataIfTrue(tableContent, "Is Constrained", FhirURL.createOrThrow(HTMLConstants.HL7_CONFORMANCE + "#constraints"), resourceFlags.isConstrained()); // implied by Invariants entry
		addDataIfTrue(tableContent, "Must-Support", FhirURL.createOrThrow(FhirURLConstants.HL7_CONFORMANCE + "#mustSupport"), resourceFlags.isMustSupport());
	}

	private void addDataIfTrue(List<Element> tableContent, String label, FhirURL url, boolean condition) {
		if (condition) {
			addLabelWithLinkDataRow(tableContent, label, url, "True");
		}
	}
	
	private void addConstraints(List<Element> tableContent) {
		if (!profileConstraints.isEmpty() || !inheritedConstraints.isEmpty()) {
			
			List<Content> constraintInfos = Lists.newArrayList();
			addConstraintInfos(constraintInfos, profileConstraints, "Defined on this element");
			addConstraintInfos(constraintInfos, inheritedConstraints, "Affect this element");
			
			tableContent.add(
				getDataRow(
					dataCell("Invariants", FhirCSS.DETAILS_DATA_CELL), 
					Elements.withAttributeAndChildren("td", 
						new Attribute("class", FhirCSS.DETAILS_DATA_CELL), 
						constraintInfos)));
		}
	}
	
	private void addConstraintInfos(List<Content> constraintInfos, List<ConstraintInfo> profileConstraints, String label) {
		if (!profileConstraints.isEmpty()) {
			if (!constraintInfos.isEmpty()) {
				constraintInfos.add(Elements.newElement("br"));
			}
			constraintInfos.add(Elements.withText("b", label));
			
			for (ConstraintInfo constraint : profileConstraints) {
				constraintInfos.add(Elements.newElement("br"));
				
				constraintInfos.add(Elements.withText("b", constraint.getKey() + ": "));
				String constraintContent = constraint.getDescription();
				if (constraint.getRequirements().isPresent()) {
					constraintContent += ". " + constraint.getRequirements().get();
				}
				constraintContent += " (xpath: " + constraint.getXPath() + ")";
				constraintContent += " severity: " + constraint.getSeverity();
				constraintInfos.add(new Text(constraintContent));
			}
		}
	}

	private void addSlicing(List<Element> tableContent) {
		if (slicing.isPresent()) {
			SlicingInfo slicingInfo = slicing.get();
			
			List<Content> renderedSlicingInfo = Lists.newArrayList();
			String description = slicingInfo.getDescription();
			if (!Strings.isNullOrEmpty(description)) {
				addSlicingInfo(renderedSlicingInfo, slicingInfo.getDescription());
			}
			addSlicingInfo(renderedSlicingInfo, slicingInfo.getOrderedDesc());
			addSlicingInfo(renderedSlicingInfo, slicingInfo.getRules());
			addSlicingInfo(renderedSlicingInfo, "discriminators: " + String.join(", ", slicingInfo.getDiscriminatorPaths()));
			
			tableContent.add(
				getDataRow(
					dataCell("Slicing", FhirCSS.DETAILS_DATA_CELL),
					Elements.withAttributeAndChildren("td", 
						new Attribute("class", FhirCSS.DETAILS_DATA_CELL), 
						Lists.newArrayList(
							new Text("This element introduces a set of slices. The slicing rules are:"),
							Elements.withAttributeAndChildren("ul", 
								new Attribute("class", FhirCSS.LIST), 
								renderedSlicingInfo)))));
		}
	}

	private void addSlicingInfo(List<Content> renderedSlicingInfo, String data) {
		if (!Strings.isNullOrEmpty(data)) {
			renderedSlicingInfo.add(Elements.withText("li", data));
		}
	}

	public Optional<String> getDefinition() {
		return definition;
	}

	public String getCardinality() {
		return cardinality;
	}

	public Optional<BindingInfo> getBindingInfo() {
		return binding;
	}

	public List<LinkData> getTypeLinks() {
		return typeLinks;
	}

	public Optional<String> getRequirements() {
		return requirements;
	}

	public  List<String> getAliases() {
		return aliases;
	}

	public ResourceFlags getResourceFlags() {
		return resourceFlags;
	}

	public Optional<String> getComments() {
		return comments;
	}

	public List<ConstraintInfo> getInheritedConstraints() {
		return inheritedConstraints;
	}

	public List<ConstraintInfo> getProfileConstraints() {
		return profileConstraints;
	}

	public void assertEqualTo(StructureDefinitionDetails detail) {

		if (!getDefinition().equals(detail.getDefinition())) {
			throw new IllegalStateException("Same key, different definition (" + key + ").");
		}
		if (!getCardinality().equals(detail.getCardinality())) {
			throw new IllegalStateException("Same key, different cardinality (" + key + ").");
		}
		if (!getBindingInfo().equals(detail.getBindingInfo())) {
			throw new IllegalStateException("Same key, different binding info (" + key + ").");
		}
		if (!getTypeLinks().stream().allMatch(link -> detail.getTypeLinks().contains(link))) {
			throw new IllegalStateException("Same key, different types info (" + key + ").");
		}
		if (!getRequirements().equals(detail.getRequirements())) {
			throw new IllegalStateException("Same key, different requirements info (" + key + ").");
		}
		if (!getAliases().stream().allMatch(alias -> detail.getAliases().contains(alias))) {
			throw new IllegalStateException("Same key, different alias info (" + key + ").");
		}
		if (!getResourceFlags().equals(detail.getResourceFlags())) {
			throw new IllegalStateException("Same key, different resource flags info (" + key + ").");
		}
		if (!getComments().equals(detail.getComments())) {
			throw new IllegalStateException("Same key, different comments info (" + key + ").");
		}
		if (!getInheritedConstraints().stream().allMatch(constraint -> detail.getInheritedConstraints().contains(constraint))) {
			throw new IllegalStateException("Same key, different inherited constraints info (" + key + ").");
		}
		if (!getProfileConstraints().stream().allMatch(constraint -> detail.getProfileConstraints().contains(constraint))) {
			throw new IllegalStateException("Same key, different profile constraints info (" + key + ").");
		}
	}

	public static List<CSSStyleBlock> getStyles() {
		List<CSSStyleBlock> iconStyles = Lists.newArrayList();
		
		iconStyles.add(
				new CSSStyleBlock(Lists.newArrayList("." + FhirCSS.LIST),
					Lists.newArrayList(
						new CSSRule("margin", "0px"))));
			
		return iconStyles;
	}
	
}