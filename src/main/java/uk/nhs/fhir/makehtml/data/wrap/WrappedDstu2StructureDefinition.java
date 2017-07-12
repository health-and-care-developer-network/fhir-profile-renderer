package uk.nhs.fhir.makehtml.data.wrap;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseMetaType;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import ca.uhn.fhir.model.dstu2.composite.CodeableConceptDt;
import ca.uhn.fhir.model.dstu2.composite.CodingDt;
import ca.uhn.fhir.model.dstu2.composite.ContactPointDt;
import ca.uhn.fhir.model.dstu2.composite.ElementDefinitionDt;
import ca.uhn.fhir.model.dstu2.composite.IdentifierDt;
import ca.uhn.fhir.model.dstu2.resource.StructureDefinition;
import ca.uhn.fhir.model.dstu2.resource.StructureDefinition.Contact;
import ca.uhn.fhir.model.dstu2.resource.StructureDefinition.Mapping;
import ca.uhn.fhir.model.primitive.StringDt;
import uk.nhs.fhir.makehtml.FhirVersion;
import uk.nhs.fhir.makehtml.data.FhirContact;
import uk.nhs.fhir.makehtml.data.FhirDstu2TreeNodeBuilder;
import uk.nhs.fhir.makehtml.data.FhirMapping;
import uk.nhs.fhir.makehtml.data.FhirRelease;
import uk.nhs.fhir.makehtml.data.FhirTreeData;
import uk.nhs.fhir.makehtml.data.FhirTreeDataBuilder;
import uk.nhs.fhir.makehtml.data.FhirTreeNode;

public class WrappedDstu2StructureDefinition extends WrappedStructureDefinition {
	private final StructureDefinition definition;
	
	public WrappedDstu2StructureDefinition(StructureDefinition definition) {
		this.definition = definition;
	}

	@Override
	public FhirVersion getImplicitFhirVersion() {
		return FhirVersion.DSTU2;
	}

	@Override
	public boolean isExtension() {
		return definition.getConstrainedType().equals("Extension");
	}

	@Override
	public String getName() {
		return definition.getName();
	}

	@Override
	public String getUrl() {
		return definition.getUrl();
	}

	@Override
	public String getKind() {
		return definition.getKind();
	}

	@Override
	public String getStatus() {
		return definition.getStatus();
	}

	@Override
	public Boolean getAbstract() {
		return definition.getAbstract();
	}

	@Override
	public Optional<String> getConstrainedType() {
		return Optional.of(definition.getConstrainedType());
	}

	@Override
	public String getBase() {
		return definition.getBase();
	}

	@Override
	public Optional<String> getVersion() {
		return Optional.ofNullable(definition.getVersion());
	}

	@Override
	public Optional<String> getDisplay() {
		return Optional.ofNullable(definition.getDisplay());
	}

	@Override
	public Optional<String> getPublisher() {
		return Optional.ofNullable(definition.getPublisher());
	}

	@Override
	public Date getDate() {
		return definition.getDate();
	}

	@Override
	public Optional<String> getCopyright() {
		return Optional.ofNullable(definition.getCopyright());
	}

	@Override
	public Optional<String> getFhirVersion() {
		Optional<String> fhirVersionDesc = Optional.empty();
		
		if (!Strings.isNullOrEmpty(definition.getFhirVersion())) {
			fhirVersionDesc = Optional.of(FhirRelease.forString(definition.getFhirVersion()).getDesc());
		}
		
		return fhirVersionDesc;
	}

	@Override
	public Optional<String> getContextType() {
		return Optional.ofNullable(definition.getContextType());
	}

	@Override
	public List<FhirContact> getContacts() {
		List<FhirContact> contacts = Lists.newArrayList();
		
		for (Contact contact : definition.getContact()) {
			FhirContact fhirContact = new FhirContact(contact.getName());
			
			for (ContactPointDt telecom : contact.getTelecom()){
				fhirContact.addTelecom(telecom.getValue());
			}
			
			contacts.add(fhirContact);
		}
		
		return contacts;
	}

	@Override
	public List<String> getUseContexts() {
		List<String> useContexts = Lists.newArrayList();
		
		for (CodeableConceptDt useContext : definition.getUseContext()) {
			for (CodingDt coding : useContext.getCoding()) {
				throw new NotImplementedException("Don't know what to do with use context code: " + coding.toString());
			}
			
			String text = useContext.getText();
			useContexts.add(text);
		}
		
		return useContexts;
	}
	
	@Override
	public void checkUnimplementedFeatures() {
		//List<List<Content>> identifierCells = Lists.newArrayList();
		for (IdentifierDt identifier : definition.getIdentifier()) {
			/*List<Content> identifierCell = Lists.newArrayList();
			identifierCells.add(identifierCell);
			
			Optional<String> use = Optional.ofNullable(identifier.getUse());
			Optional<String> type = Optional.ofNullable(identifier.getType().getText());
			Optional<String> system = Optional.ofNullable(identifier.getSystem());
			Optional<String> value = Optional.ofNullable(identifier.getValue());
			Optional<PeriodDt> period = Optional.ofNullable(identifier.getPeriod());
			ResourceReferenceDt assigner = identifier.getAssigner();*/
			
			throw new NotImplementedException("Identifier");
		}
		
		//List<String> indexingCodes = Lists.newArrayList();
		for (CodingDt code : definition.getCode()) {
			//indexingCodes.add(code.getCode());
			
			throw new NotImplementedException("Code");
		}
		
		if (!definition.getRequirements().isEmpty()) {
			throw new NotImplementedException("NHS Digital StructureDefinitions shouldn't contain requirements");
		}
				
	}

	@Override
	public List<FhirMapping> getMappings() {
		List<FhirMapping> mappings = Lists.newArrayList();
		
		for (Mapping mapping : definition.getMapping()) {
			mappings.add(new FhirMapping(mapping.getIdentity(), mapping.getUri(), mapping.getName(), mapping.getComments()));
		}
		
		return mappings;
	}

	@Override
	public List<String> getUseLocationContexts() {
		List<String> useLocationContexts = Lists.newArrayList();
		
		for (StringDt context : definition.getContext()) {
			useLocationContexts.add(context.getValue());
		}
		
		return useLocationContexts;
	}

	@Override
	public IBaseMetaType getSourceMeta() {
		return definition.getMeta();
	}

	private static final FhirDstu2TreeNodeBuilder treeNodeBuilder = new FhirDstu2TreeNodeBuilder();
	
	@Override
	public FhirTreeData getSnapshotTree() {
		FhirTreeDataBuilder fhirTreeDataBuilder = new FhirTreeDataBuilder();
		
		for (ElementDefinitionDt element : definition.getSnapshot().getElement()) {
			FhirTreeNode node = treeNodeBuilder.fromElementDefinition(element);
			fhirTreeDataBuilder.addFhirTreeNode(node);
		}
		
		return fhirTreeDataBuilder.getTree();
	}

	@Override
	public FhirTreeData getDifferentialTree() {
		FhirTreeDataBuilder fhirTreeDataBuilder = new FhirTreeDataBuilder();
		fhirTreeDataBuilder.permitDummyNodes();
		
		for (ElementDefinitionDt element : definition.getDifferential().getElement()) {
			FhirTreeNode node = treeNodeBuilder.fromElementDefinition(element);
			fhirTreeDataBuilder.addFhirTreeNode(node);
		}

		return fhirTreeDataBuilder.getTree();
	}
}