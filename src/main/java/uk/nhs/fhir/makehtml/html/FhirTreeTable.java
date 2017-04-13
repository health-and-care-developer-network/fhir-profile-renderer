package uk.nhs.fhir.makehtml.html;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import uk.nhs.fhir.makehtml.CSSStyleBlock;
import uk.nhs.fhir.makehtml.data.BindingResourceInfo;
import uk.nhs.fhir.makehtml.data.FhirIcon;
import uk.nhs.fhir.makehtml.data.FhirTreeData;
import uk.nhs.fhir.makehtml.data.FhirTreeNode;
import uk.nhs.fhir.makehtml.data.FhirTreeSlicingNode;
import uk.nhs.fhir.makehtml.data.LinkData;
import uk.nhs.fhir.makehtml.data.NestedLinkData;
import uk.nhs.fhir.makehtml.data.ResourceInfo;
import uk.nhs.fhir.makehtml.data.ResourceInfoType;
import uk.nhs.fhir.makehtml.data.SimpleLinkData;
import uk.nhs.fhir.util.TableTitle;

public class FhirTreeTable {
	// Hide the slicing of extensions under the root node
	private static final boolean dropExtensionSlicingNodes = true;
	
	
	private final FhirTreeData data;
	private final Style lineStyle = Style.DOTTED;
	
	public FhirTreeTable(FhirTreeData data) {
		this.data = data;
	}

	public Table asTable(boolean showRemoved) {
		return new Table(getColumns(), getRows(showRemoved), Sets.newHashSet());
	}
	
	private List<TableTitle> getColumns() {
		return Lists.newArrayList(
			new TableTitle("Name", "The logical name of the element", "30%"),
			new TableTitle("Flags", "Features of the element", "5%", "60px"),
			new TableTitle("Card.", "Minimum and maximum # of times the element can appear in the instance", "5%", "40px"),
			new TableTitle("Type", "Reference to the type of the element", "20%", "80px"),
			new TableTitle("Description/Constraints", "Additional information about the element", "40%")
		);
	}
	
	private List<TableRow> getRows(boolean showRemoved) {
		List<TableRow> tableRows = Lists.newArrayList();
		
		if (!showRemoved) {
			stripRemovedElements(data.getRoot());
		}
		
		if (dropExtensionSlicingNodes) {
			removeExtensionsSlicingNodes(data.getRoot());
		}
		
		addSlicingIcons(data.getRoot());
		
		FhirTreeNode root = data.getRoot();
		root.getId().setFhirIcon(FhirIcon.RESOURCE);
		List<Boolean> rootVlines = Lists.newArrayList(root.hasChildren());
		List<FhirTreeIcon> rootIcons = Lists.newArrayList();
		
		addTableRow(tableRows, root, rootVlines, rootIcons);
		addNodeRows(root, tableRows, rootVlines);
		
		return tableRows;
	}

	private void addSlicingIcons(FhirTreeNode node) {
		if (node.getSlicingInfo().isPresent()) {
			node.getId().setFhirIcon(FhirIcon.SLICE);
		}
		
		for (FhirTreeNode child : node.getChildren()) {
			// call recursively over whole tree
			addSlicingIcons(child);
		}
	}

	private void removeExtensionsSlicingNodes(FhirTreeNode node) {
		List<FhirTreeNode> children = node.getChildren();
		
		// if there is an extensions slicing node (immediately under root), remove it.
		for (int i=children.size()-1; i>=0; i--) {
			FhirTreeNode child = children.get(i);
			if (child.getPathName().equals("extension")
			  && child.getSlicingInfo().isPresent()) {
				children.remove(i);
			} else {
				// call recursively over whole tree
				removeExtensionsSlicingNodes(child);
			}
		}
	}

	/**
	 * Remove all nodes that have cardinality max = 0 (and their children)
	 */
	private void stripRemovedElements(FhirTreeNode node) {
		List<FhirTreeNode> children = node.getChildren();
		
		for (int i=children.size()-1; i>=0; i--) {
			
			FhirTreeNode child = children.get(i);
			
			if (child.isRemovedByProfile()) {
				children.remove(i);
			} else {
				stripRemovedElements(child);
			}
		}
	}

	private void addNodeRows(FhirTreeNode node, List<TableRow> tableRows, List<Boolean> vlines) {
		
		List<FhirTreeNode> children = node.getChildren();
		for (int i=0; i<children.size(); i++) {
			FhirTreeNode childNode = children.get(i);
			List<Boolean> childVlines = null;
			
			childVlines = Lists.newArrayList(vlines);
			childVlines.add(childNode.hasChildren());
			
			boolean lastChild = (i == children.size() - 1);
			if (lastChild) {
				childVlines.set(childVlines.size()-2, Boolean.FALSE);
			}
			
			ArrayList<FhirTreeIcon> treeIcons = Lists.newArrayList();
			for (int j=0; j<vlines.size(); j++) {
				boolean lineBelow = childVlines.get(j);
				boolean lastIcon = (j == vlines.size() - 1);
				
				if (lineBelow && !lastIcon) {
					treeIcons.add(FhirTreeIcon.VLINE);
				} else if (lineBelow && lastIcon) {
					treeIcons.add(FhirTreeIcon.VJOIN);
				} else if (!lineBelow && lastIcon) {
					treeIcons.add(FhirTreeIcon.VJOIN_END);
				} else if (!lineBelow && !lastIcon) {
					treeIcons.add(FhirTreeIcon.BLANK);
				}
			}
			
			if (childNode.hasChildren() && !(childNode instanceof FhirTreeSlicingNode)) {
				FhirIcon currentIcon = childNode.getId().getFhirIcon();
				// update default icon to folder icon
				if (currentIcon.equals(FhirIcon.DATATYPE)) {
					childNode.getId().setFhirIcon(FhirIcon.ELEMENT);
				}
			}

			addTableRow(tableRows, childNode, childVlines, treeIcons);
			addNodeRows(childNode, tableRows, childVlines);
		}
	}
	
	private void addTableRow(List<TableRow> tableRows, FhirTreeNode node, List<Boolean> rootVlines, List<FhirTreeIcon> treeIcons) {
		boolean[] vlinesRequired = listToBoolArray(rootVlines);
		String backgroundCSSClass = TablePNGGenerator.getCSSClass(lineStyle, vlinesRequired);
		List<LinkData> typeLinks = node.getTypeLinks();
		tableRows.add(
			new TableRow(
				new TreeNodeCell(treeIcons, node.getId().getFhirIcon(), node.getId().getName(), backgroundCSSClass),
				new ResourceFlagsCell(node.getResourceFlags()),
				new SimpleTextCell(node.getCardinality().toString()), 
				new LinkCell(typeLinks), 
				new ValueWithInfoCell(node.getInformation(), getNodeResourceInfos(node))));
	}
	
	private List<ResourceInfo> getNodeResourceInfos(FhirTreeNode node) {
		List<ResourceInfo> resourceInfos = Lists.newArrayList();
		
		// slicing
		if (node.getSlicingInfo().isPresent()) {
			Optional<ResourceInfo> slicingResourceInfo = node.getSlicingInfo().get().toResourceInfo();
			if (slicingResourceInfo.isPresent()) {
				resourceInfos.add(slicingResourceInfo.get());
			}
		}
		
		// slicing discriminator
		FhirTreeNode ancestor = node.getParent();
		while (ancestor != null) {
			if (ancestor.getSlicingInfo().isPresent()) {
				List<String> discriminatorPaths = ancestor.getSlicingInfo().get().getDiscriminatorPaths();
				String discriminatorPathRoot = ancestor.getPath() + ".";
				for (String discriminatorPath : discriminatorPaths) {
					if ((discriminatorPathRoot + discriminatorPath).equals(node.getPath())) {
						resourceInfos.add(new ResourceInfo("Slice discriminator", discriminatorPath, ResourceInfoType.SLICING_DISCRIMINATOR));
					}
				}
			}
			ancestor = ancestor.getParent();
		}
		
		// FixedValue
		if (node.isFixedValue()) {
			String description = node.getFixedValue().get();
			resourceInfos.add(makeResourceInfoWithMaybeUrl("Fixed Value", description, ResourceInfoType.FIXED_VALUE));
		}
		
		// Example
		if (node.hasExample()) {
			// never display as an actual link
			resourceInfos.add(new ResourceInfo("Example Value", node.getExample().get(), ResourceInfoType.EXAMPLE_VALUE));
		}
		
		// Default Value
		if (node.hasDefaultValue()) {
			resourceInfos.add(makeResourceInfoWithMaybeUrl("Default Value", node.getDefaultValue().get(), ResourceInfoType.DEFAULT_VALUE));
		}
		
		// Binding
		if (node.hasBinding()) {
			resourceInfos.add(new BindingResourceInfo(node.getBinding().get()));
		}
		
		// Extensions
		if (node.getPathName().equals("extension")) {
			List<LinkData> typeLinks = node.getTypeLinks();
			for (LinkData link : typeLinks) {
				if (link instanceof NestedLinkData
				  && link.getPrimaryLinkData().getText().equals("Extension")) {
					NestedLinkData extensionLinkData = (NestedLinkData)link;
					for (SimpleLinkData nestedLink : extensionLinkData.getNestedLinks()) {
						try {
							resourceInfos.add(new ResourceInfo("URL", new URL(nestedLink.getURL()), ResourceInfoType.EXTENSION_URL));
						} catch (MalformedURLException e) {
							throw new IllegalStateException("Failed to create URL for extension node");
						}
					}
				}
			}
		}
		
		return resourceInfos;
	}

	private ResourceInfo makeResourceInfoWithMaybeUrl(String title, String value, ResourceInfoType type) {
		if (looksLikeUrl(value)) {
			try {
				return new ResourceInfo(title, new URL(value), type);
			} catch (MalformedURLException e) {
				// revert to non-link version
			}
		} 
		
		return new ResourceInfo(title, value, type);
	}

	private boolean looksLikeUrl(String description) {
		return description.startsWith("http://") || description.startsWith("https://");
	}

	public Set<FhirIcon> getIcons() {
		Set<FhirIcon> icons = Sets.newHashSet();
		addIcons(data.getRoot(), icons);
		return icons;
	}
	
	public void addIcons(FhirTreeNode node, Set<FhirIcon> icons) {
		icons.add(node.getId().getFhirIcon());
		
		for (FhirTreeNode child : node.getChildren()) {
			addIcons(child, icons);
		}
	}
	
	private boolean[] listToBoolArray(List<Boolean> bools) {
		boolean[] boolArray = new boolean[bools.size()];
		for (int i=0; i<bools.size(); i++) {
			boolArray[i] = bools.get(i);
		}
		return boolArray;
	}
	
	public List<CSSStyleBlock> getStyles() {
		List<CSSStyleBlock> tableStyles = Lists.newArrayList();

		tableStyles.addAll(getIconStyles());
		tableStyles.addAll(FhirTreeIcon.getCssRules());
		tableStyles.addAll(getLayoutStyles());
		
		return tableStyles;
	}
	
	private List<CSSStyleBlock> getLayoutStyles() {
		List<CSSStyleBlock> styles = Lists.newArrayList();
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList(".fhir-tree-icons"),
				Lists.newArrayList(
					new CSSRule("padding", "0 4px"),
					new CSSRule("border-collapse", "collapse"))));

		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList(".fhir-tree-icons img"),
				Lists.newArrayList(
					new CSSRule("vertical-align", "top"),
					new CSSRule("float", "left"),
					new CSSRule("border-collapse", "collapse"))));

		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList(".fhir-table", ".fhir-table tbody tr"),
				Lists.newArrayList(
					new CSSRule("border-collapse", "collapse"),
					new CSSRule("vertical-align", "top"),
					new CSSRule("border-style", "none"))));
		
		styles.add(
			new CSSStyleBlock(
				Lists.newArrayList(".fhir-tree-icons", ".fhir-tree-icons img", ".fhir-table"),
				Lists.newArrayList(
					new CSSRule("-webkit-border-horizontal-spacing", "0"),
					new CSSRule("-webkit-border-vertical-spacing", "0"))));
		
		return styles;
	}

	private List<CSSStyleBlock> getIconStyles() {
		List<CSSStyleBlock> iconStyles = Lists.newArrayList();
		
		/*for (FhirIcon icon : getIcons()) {
			iconStyles.add(
				new CSSStyleBlock(Lists.newArrayList("." + icon.getCSSClass()),
					Lists.newArrayList(
						new CSSRule("padding-right", "4px"),
						new CSSRule("background-color", "white"),
						new CSSRule("border", "0"),
						new CSSRule("content", icon.getAsDataUrl()),
						new CSSRule("width", "20"),
						new CSSRule("height", "16"))));
		}*/
		
		iconStyles.add(
			new CSSStyleBlock(Lists.newArrayList(".fhir-tree-resource-icon"),
				Lists.newArrayList(
					new CSSRule("padding-right", "4px"),
					new CSSRule("background-color", "white"),
					new CSSRule("border", "0"),
					new CSSRule("width", "20"),
					new CSSRule("height", "16"))));
		
		return iconStyles;
	}
}