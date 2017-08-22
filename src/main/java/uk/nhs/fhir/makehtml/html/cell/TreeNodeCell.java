package uk.nhs.fhir.makehtml.html.cell;

import java.util.List;
import java.util.Optional;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import uk.nhs.fhir.makehtml.html.jdom2.Elements;
import uk.nhs.fhir.makehtml.html.style.FhirCSS;
import uk.nhs.fhir.makehtml.html.tree.FhirIcon;
import uk.nhs.fhir.makehtml.html.tree.FhirTreeIcon;

public class TreeNodeCell extends TableCell {

	private final List<FhirTreeIcon> treeIcons;
	private final FhirIcon fhirIcon;
	private final String name;
	private final String backgroundClass;
	private final String nodeKey;
	private final Optional<String> mouseOverText;
	
	public TreeNodeCell(List<FhirTreeIcon> treeIcons, FhirIcon fhirIcon, String name, String backgroundClass, boolean strikethrough, String nodeKey, Optional<String> mouseOverText) {
		super(false, strikethrough);
		
		Preconditions.checkNotNull(name);
		Preconditions.checkNotNull(fhirIcon);
		Preconditions.checkNotNull(treeIcons);
		Preconditions.checkNotNull(backgroundClass);
		Preconditions.checkNotNull(nodeKey);
		
		this.name = name;
		this.fhirIcon = fhirIcon;
		this.treeIcons = treeIcons;
		this.backgroundClass = backgroundClass;
		this.nodeKey = nodeKey;
		this.mouseOverText = mouseOverText;
	}
	
	@Override
	public Element makeCell() {
		List<Content> contents = Lists.newArrayList();
		
		for (FhirTreeIcon icon : treeIcons) {
			//contents.add(Elements.withAttributes("img", Lists.newArrayList(new Attribute("class", icon.getCssClass()))));
			contents.add(
				Elements.withAttributes("img", 
					Lists.newArrayList(
						new Attribute("src", icon.getNhsSrc()),
						new Attribute("class", FhirCSS.TREE_ICON))));
		}
		
		contents.add(Elements.withAttributes("img", 
			Lists.newArrayList(
				new Attribute("src", fhirIcon.getUrl()),
				new Attribute("class", FhirCSS.TREE_RESOURCE_ICON))));
		
		List<Attribute> elementNameAttributes = Lists.newArrayList(
			new Attribute("class", String.join(" ", Lists.newArrayList(FhirCSS.LINK, "tabLink"))),
			new Attribute("href", "details.html#" + nodeKey));
		if (mouseOverText.isPresent()) {
			elementNameAttributes.add(new Attribute("title", mouseOverText.get()));
		}
		
		contents.add(
			getStrikethrough()
				? Elements.withAttributeAndText("span", new Attribute("class", FhirCSS.TEXT_STRIKETHROUGH), name)
				: Elements.withAttributesAndChild(
					"a",
					elementNameAttributes, 
					new Text(name)));
		
		return Elements.withAttributeAndChildren("td", 
			new Attribute("class", String.join(" ", backgroundClass, FhirCSS.TREE_ICONS)), 
			contents);
	}

}