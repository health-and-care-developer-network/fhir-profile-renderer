package uk.nhs.fhir.makehtml.opdef;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.Lists;

import ca.uhn.fhir.model.api.IDatatype;
import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.OperationDefinition.Parameter;
import ca.uhn.fhir.model.dstu2.resource.OperationDefinition.ParameterBinding;
import ca.uhn.fhir.model.dstu2.valueset.BindingStrengthEnum;
import ca.uhn.fhir.model.primitive.BoundCodeDt;
import ca.uhn.fhir.model.primitive.CodeDt;
import ca.uhn.fhir.model.primitive.UriDt;
import uk.nhs.fhir.makehtml.data.ResourceInfo;
import uk.nhs.fhir.makehtml.data.ResourceInfoType;
import uk.nhs.fhir.util.FhirDocLinkFactory;
import uk.nhs.fhir.util.LinkData;
import uk.nhs.fhir.util.TableTitle;

public class OperationDefinitionParameterTableDataProvider {

	private final List<Parameter> parameters;
	private final FhirDocLinkFactory fhirDocLinkFactory;
	
	public OperationDefinitionParameterTableDataProvider(List<Parameter> parameters, FhirDocLinkFactory linkDataFactory) {
		this.parameters = parameters;
		this.fhirDocLinkFactory = linkDataFactory;
	}

	public List<TableTitle> getColumns() {
		return Lists.newArrayList(
			new TableTitle("Name", "The logical name of the element", "200px"),
			new TableTitle("Card.", "Minimum and maximum # of times the element can appear in the instance", "100px"),
			new TableTitle("Type", "Reference to the type of the element", "150px"),
			new TableTitle("Value", "Additional information about the element", "500px")
		);
	}

	public List<OperationDefinitionParameterTableData> getRows() {
		List<OperationDefinitionParameterTableData> data = Lists.newArrayList();
		
		for (Parameter parameter : parameters) {
			String rowTitle = parameter.getName();
			String cardinality = parameter.getMin() + ".." + parameter.getMax(); 
			CodeDt typeElement = parameter.getTypeElement();
			LinkData typeLink = fhirDocLinkFactory.forDataType(typeElement);
			String documentation = parameter.getDocumentation();
			List<ResourceInfo> flags = getParameterFlags(parameter);

			data.add(new OperationDefinitionParameterTableData(rowTitle, cardinality, typeLink, documentation, flags));
		}
		
		return data;
	}
	
	private List<ResourceInfo> getParameterFlags(Parameter parameter) {
		
		try {
		
			List<ResourceInfo> resourceFlags = Lists.newArrayList();
			
			ParameterBinding binding = parameter.getBinding();
			if (!binding.isEmpty()) {
				ResourceInfo bindingFlag = null;
				IDatatype choice = binding.getValueSet();
				if (choice instanceof UriDt) {
					UriDt uri = (UriDt)choice;
					bindingFlag = new ResourceInfo("Binding", new URL(uri.getValueAsString()), ResourceInfoType.BINDING);
				} else if (choice instanceof ResourceReferenceDt) {
					//TODO need to test this
					ResourceReferenceDt ref = (ResourceReferenceDt)choice;
					bindingFlag = new ResourceInfo("Binding", new URL(ref.getReferenceElement().getValue()), ResourceInfoType.BINDING);
				}
				
				BoundCodeDt<BindingStrengthEnum> strengthElement = binding.getStrengthElement();
				bindingFlag.addExtraTag("Strength: " + strengthElement.getValueAsEnum().getCode());
				resourceFlags.add(bindingFlag);
			}
			
			ResourceReferenceDt profile = parameter.getProfile();
			if (!profile.isEmpty()) {
				resourceFlags.add(new ResourceInfo("Profile", new URL(profile.getReferenceElement().getValue()),  ResourceInfoType.BINDING));
			}
			
			//TODO tuple parameters
			List<Parameter> parts = parameter.getPart();
			if (!parts.isEmpty()) {
				throw new NotImplementedException("Tuple parameter");
			}
			
			return resourceFlags;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
