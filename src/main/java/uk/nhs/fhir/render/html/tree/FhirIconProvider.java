package uk.nhs.fhir.render.html.tree;

import uk.nhs.fhir.data.structdef.ExtensionType;
import uk.nhs.fhir.data.structdef.FhirElementDataType;
import uk.nhs.fhir.render.tree.AbstractFhirTreeTableContent;

public class FhirIconProvider {

	public FhirIcon getIcon(AbstractFhirTreeTableContent node) {
		
		if (node.getLinkedNode().isPresent()) {
			return FhirIcon.REUSE;
		}
		
		if (node.getSlicingInfo().isPresent()) {
			return FhirIcon.SLICE;
		}
		
		if (node.getParent() == null) {
			return FhirIcon.RESOURCE;
		}
		
		if (node.getExtensionType().isPresent()) {
			ExtensionType extensionType = node.getExtensionType().get();
			switch (extensionType) {
				case SIMPLE:
					return FhirIcon.EXTENSION_SIMPLE;
				case COMPLEX:
					return FhirIcon.EXTENSION_COMPLEX;
				default:
					throw new IllegalStateException("which icon should be used for extension type " + extensionType.toString());
			}
		}

		FhirElementDataType dataType = node.getDataType();
		switch (dataType) {
			case CHOICE:
				return FhirIcon.CHOICE;
			case REFERENCE:
				return FhirIcon.REFERENCE;
			case PRIMITIVE:
				return FhirIcon.PRIMITIVE;
			case RESOURCE:
			case COMPLEX_ELEMENT:
			case XHTML_NODE:
				return FhirIcon.DATATYPE;
			default:
				if (node.hasChildren()) {
					return FhirIcon.DATATYPE;
				} else {
					return FhirIcon.ELEMENT;
				}
		}
	}
}