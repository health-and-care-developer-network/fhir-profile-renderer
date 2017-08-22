package uk.nhs.fhir.data.wrap.stu3;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.dstu3.model.Base;
import org.hl7.fhir.dstu3.model.CodeSystem;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemFilterComponent;
import org.hl7.fhir.dstu3.model.CodeSystem.CodeSystemHierarchyMeaning;
import org.hl7.fhir.dstu3.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.dstu3.model.CodeSystem.FilterOperator;
import org.hl7.fhir.dstu3.model.Enumeration;
import org.hl7.fhir.dstu3.model.Factory;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Narrative;
import org.hl7.fhir.dstu3.model.Narrative.NarrativeStatus;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import uk.nhs.fhir.data.codesystem.FhirCodeSystemConcepts;
import uk.nhs.fhir.data.codesystem.FhirCodeSystemConcept;
import uk.nhs.fhir.data.codesystem.FhirCodeSystemFilter;
import uk.nhs.fhir.data.codesystem.FhirIdentifier;
import uk.nhs.fhir.data.structdef.FhirContacts;
import uk.nhs.fhir.data.wrap.WrappedCodeSystem;
import uk.nhs.fhir.util.FhirVersion;
import uk.nhs.fhir.util.StringUtil;

public class WrappedStu3CodeSystem extends WrappedCodeSystem {

	private final CodeSystem definition;
	
	public WrappedStu3CodeSystem(CodeSystem source) {
		checkForUnexpectedFeatures(source);
		
		this.definition = source;
	}
	
	public String getNameUnsafe() {
		return definition.getName();
	}
	
	@Override
 	public String getName() {
		String unsafeName = definition.getName();
		if (!Strings.isNullOrEmpty(unsafeName)) {
			return unsafeName;
		} else if (!Strings.isNullOrEmpty(getUrl())) {
			// https://fhir.nhs.uk/spine-response-codes-1 -> Spine-Response-Codes-1
			String[] urlParts = getUrl().split("/"); 
			String unformatted = urlParts[urlParts.length-1];
			return StringUtil.capitaliseLowerCase(unformatted);
		} else {
			return "(anonymous)";
		}
	}
	
	public Optional<String> getTitle() {
		return Optional.ofNullable(definition.getTitle());
	}

	@Override
	public String getUrl() {
		return definition.getUrl();
	}

	@Override
	public String getStatus() {
		return definition.getStatus().getDisplay();
	}
	
	@Override
	public Optional<String> getVersion() {
		return Optional.ofNullable(definition.getVersion());
	}

	@Override
	public Optional<String> getDisplay() {
		return Optional.ofNullable(definition.getName());
	}

	@Override
	public Optional<String> getPublisher() {
		return Optional.ofNullable(definition.getPublisher());
	}

	@Override
	public Optional<Date> getLastUpdatedDate() {
		return Optional.ofNullable(definition.getDate());
	}

	@Override
	public Optional<String> getCopyright() {
		return Optional.ofNullable(definition.getCopyright());
	}
	
	@Override
	public FhirCodeSystemConcepts getCodeSystemConcepts() {
		FhirCodeSystemConcepts codeSystemConcepts = new FhirCodeSystemConcepts(definition.getUrl());
		
		for (ConceptDefinitionComponent concept : definition.getConcept()) {
			FhirCodeSystemConcept newConcept = 
				new FhirCodeSystemConcept(
					concept.getCode(), 
					concept.getDisplay(),
					concept.getDefinition());
			
			codeSystemConcepts.addConcept(newConcept);
		}
		
		return codeSystemConcepts;
	}

	@Override
	public IBaseResource getWrappedResource() {
		return definition;
	}

	@Override
	public IBaseMetaType getSourceMeta() {
		return definition.getMeta();
	}

	@Override
	public FhirVersion getImplicitFhirVersion() {
		return FhirVersion.STU3;
	}

	@Override
	public void setUrl(String url) {
		definition.setUrl(url);
	}

	@Override
	public Optional<String> getValueSet() {
		return Optional.ofNullable(definition.getValueSet());
	}

	@Override
	public void fixHtmlEntities() {
		
	}

	@Override
	public void addHumanReadableText(String textSection) {
		try {
			Narrative textElement = Factory.newNarrative(NarrativeStatus.GENERATED, textSection);
	        definition.setText(textElement);
		} catch (IOException | FHIRException e) {
			throw new IllegalStateException(e);
		}
	}

	@Override
	public List<FhirCodeSystemFilter> getFilters() {
		List<FhirCodeSystemFilter> filters = Lists.newArrayList();
		
		for (CodeSystemFilterComponent filter : definition.getFilter()) {
			FhirCodeSystemFilter newFilter = new FhirCodeSystemFilter(filter.getCode(), filter.getDescription(), filter.getValue());
			
			for (Enumeration<FilterOperator> operator : filter.getOperator()) {
				newFilter.addOperator(operator.asStringValue());
			}
			
			filters.add(newFilter);
		}
		
		return filters;
	}

	@Override
	public Optional<FhirIdentifier> getIdentifier() {
		Identifier identifier = definition.getIdentifier();
		if (identifier == null) {
			return Optional.empty();
		} else {
			return Optional.of(new FhirIdentifier(identifier.getValue(), identifier.getSystem()));
		}
	}

	@Override
	public Optional<Boolean> getExperimental() {
		return Optional.ofNullable(definition.getExperimental());
	}
	
	@Override
	public Optional<String> getDescription() {
		return Optional.ofNullable(definition.getDescription());
	}

	@Override
	public Optional<String> getPurpose() {
		return Optional.ofNullable(definition.getPurpose());
	}
	
	@Override
	public Optional<Boolean> getCaseSensitive() {
		return Optional.ofNullable(definition.getCaseSensitive());
	}
	
	@Override
	public Optional<Boolean> getCompositional() {
		if (definition.getCompositionalElement() == null) {
			return Optional.empty();
		} else {
			return Optional.of(definition.getCompositional());
		}
	}

	@Override
	public Optional<String> getHierarchyMeaning() {
		CodeSystemHierarchyMeaning hierarchyMeaning = definition.getHierarchyMeaning();
		if (hierarchyMeaning == null) {
			return Optional.empty();
		} else {
			return Optional.of(hierarchyMeaning.getDisplay());
		}
	}

	@Override
	public List<FhirContacts> getContacts() {
		return new Stu3FhirContactConverter().convertList(definition.getContact());
	}

	@Override
	public Optional<String> getContent() {
		return Optional.ofNullable(definition.getContent().getDisplay());
	}
	
	public static void checkForUnexpectedFeatures(CodeSystem source) {
		// accessible features
		// source.getUrl();
		// source.getVersion();
		// source.getName();
		// source.getTitle();
		// source.getStatus();
		// source.getDate();
		// source.getPublisher();
		// source.getCopyright();
		// source.getValueSet();
		// source.getFilter(); // (including all nested fields)
		// source.getConcept();
		// source.getIdentifier()
		// source.getCaseSensitiveElement().getValue();
		// source.getContent()
		// source.getContact()
		// source.getHierarchyMeaning()
		// source.getExperimentalElement().getValue()
		// source.getPurpose()
		
		checkNoInfoPresent(source.getUseContext());
		checkNoInfoPresent(source.getJurisdiction());
		checkNoInfoPresent(source.getCompositionalElement().getValue());
		checkNoInfoPresent(source.getVersionNeededElement().getValue());
		checkNoInfoPresent(source.getCountElement().getValue());

		Identifier identifier = source.getIdentifier();
		if (identifier != null) {
			checkNoInfoPresent(identifier.getUse());
			checkNoInfoPresent(identifier.getType());
			checkNoInfoPresent(identifier.getPeriod());
			checkNoInfoPresent(identifier.getAssigner());
		}
		
		checkNoInfoPresent(source.getProperty());
		
		for (ConceptDefinitionComponent concept : source.getConcept()) {
			checkNoInfoPresent(concept.getDesignation());
			checkNoInfoPresent(concept.getProperty());
			// nested concepts
			checkNoInfoPresent(concept.getConcept());
		}
		
		// actually the filter panel rendering is implemented, but worth being aware once it is being used so we can test with live data rather than test data
		checkNoInfoPresent(source.getFilter());
		
	}

	private static void checkNoInfoPresent(Object o) {
		if (o instanceof Collection<?>) {
			if (!((Collection<?>) o).isEmpty()) {
				throw new IllegalStateException("Expected " + o.toString() + " to be empty");
			}
		} else if (o instanceof Base) {
			if (!((Base) o).isEmpty()) {
				throw new IllegalStateException("Expected " + o.toString() + " to be empty");
			}
		} else {
			if (o != null) {
				throw new IllegalStateException("Expected " + o.toString() + " to be empty");
			}
		}
	}
}
