package uk.nhs.fhir.makehtml.html.cell;

import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import com.google.common.collect.Lists;

import uk.nhs.fhir.data.ResourceInfo;
import uk.nhs.fhir.data.ResourceInfoType;
import uk.nhs.fhir.data.url.FhirURL;
import uk.nhs.fhir.makehtml.html.jdom2.Elements;
import uk.nhs.fhir.makehtml.html.style.CSSRule;
import uk.nhs.fhir.makehtml.html.style.CSSStyleBlock;
import uk.nhs.fhir.makehtml.html.style.FhirCSS;
import uk.nhs.fhir.util.StringUtil;

public class ValueWithInfoCell extends TableCell {

	private final String value;
	private final List<ResourceInfo> resourceInfos;
	
	public ValueWithInfoCell(String value, List<ResourceInfo> constraints) {
		this.value = value;
		this.resourceInfos = constraints;
	}

	@Override
	public Element makeCell() {
		List<Content> valueDataNodes = Lists.newArrayList();
		if (!value.isEmpty()) {
			valueDataNodes.add(new Text(StringUtil.capitaliseLowerCase(value)));
		}
		for (ResourceInfo resourceInfo : resourceInfos) {
			if (!valueDataNodes.isEmpty()) {
				valueDataNodes.add(new Element("br"));
			}
			valueDataNodes.addAll(nodesForResourceFlag(resourceInfo));
		}
		
		return Elements.withAttributeAndChildren("td", new Attribute("class", FhirCSS.RESOURCE_INFO_CELL), valueDataNodes);
	}
	
	private List<Content> nodesForResourceFlag(ResourceInfo resourceInfo) {
		List<Content> constraintInfoNodes = Lists.newArrayList();
		
		ResourceInfoType type = resourceInfo.getType();
		boolean useTidyStyle = useTidyStyle(type);
		String nameClass = useTidyStyle ?
			FhirCSS.INFO_NAME_BOLD :
			FhirCSS.INFO_NAME_BLOCK;
		
		// The tidy style doesn't have a box around the title, so needs a colon to separate it from the content
		String name = resourceInfo.getName();
		if (useTidyStyle) {
			name += ": ";
		}
		constraintInfoNodes.add(
			Elements.withAttributeAndText("span", 
				new Attribute("class", nameClass),
				name));
		
		List<Content> resourceInfoText = getResourceInfoContents(resourceInfo);
		
		if (useTidyStyle) {
			constraintInfoNodes.add(
				Elements.withAttributeAndChildren("span",
					new Attribute("class", FhirCSS.TEXT_ITALIC),
					resourceInfoText));
		} else {
			constraintInfoNodes.addAll(resourceInfoText);
		}
		
		for (String tag: resourceInfo.getExtraTags()) {
			constraintInfoNodes.add(getFormattedTag(tag));
		}
		
		return constraintInfoNodes;
	}

	List<Content> getResourceInfoContents(ResourceInfo resourceInfo) {
		boolean hasText = resourceInfo.getDescription().isPresent();
		boolean hasLink = resourceInfo.getDescriptionLink().isPresent();
		boolean bracketLink = hasText && hasLink;
		
		if (!hasText && !hasLink) {
			throw new IllegalStateException("Resource info without text or link");
		}
		
		String description = hasText ? resourceInfo.getDescription().get() : "";
		FhirURL fhirURL = hasLink ? resourceInfo.getDescriptionLink().get() : null;
		
		List<Content> constraintInfoText = Lists.newArrayList();
		if (hasText) {
			constraintInfoText.add(new Text(StringUtil.capitaliseLowerCase(description)));
		}
		
		if (bracketLink) {
			constraintInfoText.add(new Text(" ("));
		}
		
		if (hasLink) {
			if (resourceInfo.getTextualLink()) {
				// This URL would result in a broken link - it is only intended to be used as an identifier
				constraintInfoText.add(new Text(fhirURL.toFullString()));
			} else {
				constraintInfoText.add(
					Elements.withAttributesAndText("a", 
						Lists.newArrayList(
							new Attribute("href", fhirURL.toLinkString()),
							new Attribute("class", FhirCSS.LINK)),
						fhirURL.toFullString()));
			}
		}
			
		if (bracketLink) {
			constraintInfoText.add(new Text(")"));
		}
		
		return constraintInfoText;
	}
	
	private boolean useTidyStyle(ResourceInfoType type) {
		switch (type) {
		case SLICING:
		case EXTENSION_URL:
		case CONSTRAINT:
			return true;
		default:
			return false;
		}
	}

	private Content getFormattedTag(String tag) {
		return 
			Elements.withAttributeAndText("span", 
				new Attribute("class", FhirCSS.INFO_TAG_BLOCK),
				tag);
	}
	
	public static List<CSSStyleBlock> getStyles() {
		
		List<CSSStyleBlock> styles = Lists.newArrayList();
		
		styles.add(
			new CSSStyleBlock(Lists.newArrayList("." + FhirCSS.INFO_NAME_BLOCK, "." + FhirCSS.INFO_TAG_BLOCK),
				Lists.newArrayList(
					new CSSRule("display", "inline"),
					new CSSRule("color", "#ffffff"),
					new CSSRule("font-weight", "bold"),
					new CSSRule("font-size", "10px"),
					new CSSRule("padding", ".2em .6em .3em"),
					new CSSRule("text-align", "center"),
					new CSSRule("vertical-align", "baseline"),
					new CSSRule("white-space", "nowrap"),
					new CSSRule("line-height", "2em"),
					new CSSRule("border-radius", ".25em"))));
		
		styles.add(
			new CSSStyleBlock(Lists.newArrayList("." + FhirCSS.INFO_NAME_BLOCK),
				Lists.newArrayList(new CSSRule("background-color", "#cccccc"))));
		
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.INFO_TAG_BLOCK),
				Lists.newArrayList(new CSSRule("background-color", "#ffbb55"))));
		
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.INFO_NAME_BOLD),
				Lists.newArrayList(new CSSRule("font-weight", "bold"))));
		
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.TEXT_ITALIC),
				Lists.newArrayList(new CSSRule("font-style", "italic"))));
		
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList("." + FhirCSS.RESOURCE_INFO_CELL),
				Lists.newArrayList(
					new CSSRule("padding", "5px 4px"),
					new CSSRule("border-bottom", "1px solid #F0F0F0"))));
		
		return styles;
	}
}