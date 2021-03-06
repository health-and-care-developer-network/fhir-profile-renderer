package uk.nhs.fhir.render.format;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import uk.nhs.fhir.data.url.FhirURL;
import uk.nhs.fhir.data.wrap.WrappedResource;
import uk.nhs.fhir.render.html.Elements;
import uk.nhs.fhir.render.html.style.CSSRule;
import uk.nhs.fhir.render.html.style.CSSStyleBlock;
import uk.nhs.fhir.render.html.style.CSSTag;
import uk.nhs.fhir.render.html.style.FhirCSS;
import uk.nhs.fhir.render.html.style.FhirColour;
import uk.nhs.fhir.util.FhirSimpleTypes;
import uk.nhs.fhir.util.FhirURLConstants;
import uk.nhs.fhir.util.StringUtil;

public abstract class TableFormatter<T extends WrappedResource<T>> extends ResourceFormatter<T> {
	
	public TableFormatter(T wrappedResource) {
		super(wrappedResource);
	}

	protected static final String VERSION_DATE = "Version date";
	
	public static final String BLANK = "";

	protected Element getColGroup(int columns) {
		Element colgroup = Elements.newElement("colgroup");
		Preconditions.checkState(100 % columns == 0, "Table column count divides 100% evenly");
		
		int percentPerColumn = 100/columns;
		
		for (int i=0; i<columns; i++) {
			colgroup.addContent(
				Elements.withAttributes("col", 
					Lists.newArrayList(
						new Attribute("width", Integer.toString(percentPerColumn) + "%"))));
		}
		return colgroup;
	}
	
	protected Element labelledValueCell(String label, Optional<String> value, int colspan) {
		String displayValue = value.orElse(BLANK);
		return labelledValueCell(label, displayValue, colspan);
	}
	
	protected Element labelledValueCell(String label, String value, int colspan) {
		return labelledValueCell(label, value, colspan, false);
	}
		
	protected Element labelledValueCell(String label, String value, int colspan, boolean alwaysBig) {
		String nonNullValue = Preconditions.checkNotNull(value, "value data");
		boolean hasValue = !nonNullValue.isEmpty();
		
		List<Element> cellSpans = Lists.newArrayList();
		
		if (label.length() > 0) {
			cellSpans.add(labelSpan(label, hasValue));
		}
		
		if (hasValue) {
			cellSpans.add(valueSpan(nonNullValue, alwaysBig));
		}
		
		return cell(cellSpans, colspan);
	}
	
	protected Element cell(List<? extends Content> content, int colspan) {
		return Elements.withAttributesAndChildren("td", 
			Lists.newArrayList(
				new Attribute("class", FhirCSS.DATA_CELL),
				new Attribute("colspan", Integer.toString(colspan))),
			content);
	}
	
	protected Element labelSpan(String label, boolean hasValue) {
		String cssClass = FhirCSS.DATA_LABEL;
		if (!hasValue) {
			cssClass += " " + FhirCSS.DATA_LABEL_EMPTY;
		}
		
		if (label.length() > 0) {
			label += ": ";
		} else {
			// if the content is entirely empty, the title span somehow swallows the value span
			// so use a zero-width space character.
			label = "&#8203;";
		}
		
		return Elements.withAttributeAndText("span", 
			new Attribute("class", cssClass), 
			label);
	}
	
	private static final Map<String, String> HL7_LOGICAL_URL_TO_WEB_URL;
	static {
		Map<String, String> hl7LogicalUrlToWebUrl = Maps.newHashMap();
		
		for (String type : FhirSimpleTypes.URL_CORRECTION_TYPES) {
			String logical = "http://hl7.org/fhir/StructureDefinition/" + type;
			String web = "http://hl7.org/fhir/datatypes.html#" + type.toLowerCase(Locale.UK);
			hl7LogicalUrlToWebUrl.put(logical, web);
		}
		
		for (String type : FhirSimpleTypes.METADATA_URL_CORRECTION_TYPES) {
			String logical = "http://hl7.org/fhir/StructureDefinition/" + type;
			String web = "http://hl7.org/fhir/metadatatypes.html#" + type;
			hl7LogicalUrlToWebUrl.put(logical, web);
		}
		
		hl7LogicalUrlToWebUrl.put("http://hl7.org/fhir/StructureDefinition/Extension", "http://hl7.org/fhir/extensibility.html#extension");
		
		HL7_LOGICAL_URL_TO_WEB_URL = ImmutableMap.copyOf(hl7LogicalUrlToWebUrl);
	}
	
	private String getLinkUrl(String displayUrl) {
		if (displayUrl == null) {
			return null;
		} else if (HL7_LOGICAL_URL_TO_WEB_URL.containsKey(displayUrl)) {
			return HL7_LOGICAL_URL_TO_WEB_URL.get(displayUrl);
		} else if (displayUrl.startsWith("http://hl7.org/fhir/StructureDefinition")
		  && !displayUrl.endsWith(".html")){
			return FhirURLConstants.HTTP_HL7_FHIR + displayUrl.substring(displayUrl.lastIndexOf('/')) + ".html";
		}
		
		return displayUrl;
	}
	
	protected Element valueSpan(String value, boolean alwaysLargeText) {
		boolean url = StringUtil.looksLikeUrl(value);
		boolean largeText = alwaysLargeText || value.length() < 25;
		String fhirMetadataClass = FhirCSS.DATA_VALUE;
		if (!largeText) fhirMetadataClass += " " + FhirCSS.DATA_VALUE_SMALLTEXT;
		
		if (url) {
			return Elements.withAttributeAndChild("span", 
				new Attribute("class", fhirMetadataClass), 
				Elements.withAttributesAndText("a", 
					Lists.newArrayList(
						new Attribute("class", FhirCSS.LINK), 
						new Attribute("href", FhirURL.buildOrThrow(getLinkUrl(value), getResourceVersion()).toLinkString())), 
					value));
		} else {
			return Elements.withAttributeAndText("span", 
				new Attribute("class", fhirMetadataClass), 
				value);
		}
	}
	
	public static List<CSSStyleBlock> getStyles() {
		List<CSSStyleBlock> styles = Lists.newArrayList();

		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.DATA_CELL),
				Lists.newArrayList(
					new CSSRule(CSSTag.BORDER, "1px solid " + FhirColour.DATA_CELL_BORDER))));
		styles.add(
				new CSSStyleBlock(
					Lists.newArrayList("." + FhirCSS.DATA_LABEL, "." + FhirCSS.TELECOM_NAME),
					Lists.newArrayList(
						new CSSRule(CSSTag.COLOR, FhirColour.DATA_LABEL),
						new CSSRule(CSSTag.FONT_WEIGHT, "bold"),
						new CSSRule(CSSTag.FONT_SIZE, "13"))));
		styles.add(
				new CSSStyleBlock(
					Lists.newArrayList("." + FhirCSS.DATA_LABEL_EMPTY),
					Lists.newArrayList(
						new CSSRule(CSSTag.COLOR, FhirColour.DATA_LABEL_WITHOUT_VALUE),
						new CSSRule(CSSTag.FONT_WEIGHT, "normal"))));
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.DATA_VALUE, "." + FhirCSS.TELECOM_VALUE),
				Lists.newArrayList(
					new CSSRule(CSSTag.COLOR, FhirColour.DATA_VALUE),
					new CSSRule(CSSTag.FONT_SIZE, "13"))));
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.DATA_VALUE_SMALLTEXT),
				Lists.newArrayList(
					new CSSRule(CSSTag.FONT_SIZE, "10"))));
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.DATA_BLOCK_TITLE),
				Lists.newArrayList(
					new CSSRule(CSSTag.COLOR, FhirColour.MULTILINE_DATA_TITLE),
					new CSSRule(CSSTag.FONT_WEIGHT, "bold"),
					new CSSRule(CSSTag.TEXT_DECORATION, "underline"),
					new CSSRule(CSSTag.FONT_SIZE, "13"))));
		
		return styles;
	}

}
