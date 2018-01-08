package uk.nhs.fhir.render.tree;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import uk.nhs.fhir.data.structdef.ConstraintInfo;
import uk.nhs.fhir.data.structdef.FhirElementDataType;
import uk.nhs.fhir.data.structdef.ResourceFlags;
import uk.nhs.fhir.data.structdef.SlicingInfo;
import uk.nhs.fhir.data.url.LinkDatas;
import uk.nhs.fhir.data.wrap.WrappedElementDefinition;
import uk.nhs.fhir.event.EventHandlerContext;
import uk.nhs.fhir.event.RendererEventType;
import uk.nhs.fhir.render.RendererContext;
import uk.nhs.fhir.util.FhirFileRegistry;
import uk.nhs.fhir.util.FhirVersion;

public class FhirTreeNodeBuilder {
	
	public FhirTreeNode fromElementDefinition(WrappedElementDefinition elementDefinition) {
		
		Optional<String> name = Optional.ofNullable(elementDefinition.getName());

		LinkDatas typeLinks;
		if (elementDefinition.isRootElement()) {
			/*
			typeLinks = new LinkDatas(
				new LinkData(
					FhirURL.buildOrThrow(FhirURLConstants.HTTP_HL7_FHIR + "/profiling.html", elementDefinition.getVersion()), 
					"Profile"));*/
			typeLinks = new LinkDatas();
		} else {
			typeLinks = elementDefinition.getTypeLinks();
		}
		
		Set<FhirElementDataType> dataTypes = elementDefinition.getDataTypes();
		FhirElementDataType dataType;
		if (dataTypes.isEmpty()) {
			dataType = FhirElementDataType.DELEGATED_TYPE; 
		} else if (dataTypes.size() == 1) {
			dataType = dataTypes.iterator().next();
		} else if (dataTypes.size() > 1
		  && elementDefinition.getPath().endsWith("[x]")) {
			dataType = FhirElementDataType.CHOICE;
		} else {
			throw new IllegalStateException("Found " + dataTypes.size() + " data types for node " + elementDefinition.getPath());
		}

		ResourceFlags flags = elementDefinition.getResourceFlags();
		
		Integer min = elementDefinition.getCardinalityMin();
		String max = elementDefinition.getCardinalityMax();
		
		String shortDescription = elementDefinition.getShortDescription();
		if (shortDescription == null) {
			shortDescription = "";
		}
		
		Set<String> conditionIds = elementDefinition.getConditionIds();
		
		List<ConstraintInfo> constraints = elementDefinition.getConstraintInfos();
		
		for (ConstraintInfo constraint : constraints) {
			String key = constraint.getKey();
			if (!conditionIds.contains(key)) {
				EventHandlerContext.forThread().event(RendererEventType.CONSTRAINT_WITHOUT_CONDITION, 
					"Constraint " + key + " doesn't have an associated condition pointing at it");
			}
		}

		//check for duplicate keys
		for (int i=0; i<constraints.size(); i++) {
			ConstraintInfo constraint1 = constraints.get(i);
			for (int j=i+1; j<constraints.size(); j++) {
				ConstraintInfo constraint2 = constraints.get(j);
				if (constraint1.getKey().equals(constraint2.getKey())) {
					EventHandlerContext.forThread().event(RendererEventType.DUPLICATE_CONSTRAINT_KEYS, 
						"Node constraints with duplicate keys: '" + constraint1.getKey() + "' for node "
						+ elementDefinition.getId().orElse(elementDefinition.getPath()));
				}
			}
		}
		
		String path = elementDefinition.getPath();
		FhirVersion version = elementDefinition.getVersion();

		FhirTreeNode node = new FhirTreeNode(
			name,
			flags,
			min,
			max,
			typeLinks, 
			shortDescription,
			constraints,
			path,
			dataType,
			version);

		Optional<String> definition = elementDefinition.getDefinition();
		if (definition.isPresent() && !definition.get().isEmpty()) {
			node.setDefinition(definition);
		}
		
		Optional<SlicingInfo> slicing = elementDefinition.getSlicing();
		if (slicing.isPresent()) {
			node.setSlicingInfo(slicing);	
		}
		
		node.setFixedValue(elementDefinition.getFixedValue());
		node.setExamples(elementDefinition.getExamples());
		node.setDefaultValue(elementDefinition.getDefaultValue());
		node.setBinding(elementDefinition.getBinding());
		
		Optional<String> requirements = elementDefinition.getRequirements();
		if (requirements.isPresent() && !requirements.get().isEmpty()) {
			node.setRequirements(requirements);
		}

		Optional<String> comments = elementDefinition.getComments();
		if (comments.isPresent() && !comments.get().isEmpty()) {
			node.setComments(comments);
		}

		node.setAliases(elementDefinition.getAliases());
		FhirFileRegistry fhirFileRegistry = RendererContext.forThread().getFhirFileRegistry();
		node.setExtensionType(elementDefinition.getExtensionType(fhirFileRegistry));
		
		Optional<String> nameReference = elementDefinition.getLinkedNodeName();
		if (nameReference.isPresent() && !nameReference.get().isEmpty()) {
			node.setLinkedNodeName(nameReference);
		}
		
		Optional<String> nodePath = elementDefinition.getLinkedNodePath();
		if (nodePath.isPresent() && !nodePath.get().isEmpty()) {
			node.setLinkedNodeId(nodePath);
		}
		
		node.setMappings(elementDefinition.getMappings());
		NodeMappingValidator.validate(node);
		
		node.setSliceName(elementDefinition.getSliceName());
		
		node.setId(elementDefinition.getId());
		
		return node;
	}

}
