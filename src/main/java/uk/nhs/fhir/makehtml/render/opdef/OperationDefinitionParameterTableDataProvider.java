package uk.nhs.fhir.makehtml.render.opdef;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;

import com.google.common.collect.Lists;

import ca.uhn.fhir.model.dstu2.composite.ResourceReferenceDt;
import ca.uhn.fhir.model.dstu2.resource.OperationDefinition.Parameter;
import ca.uhn.fhir.model.dstu2.resource.OperationDefinition.ParameterBinding;
import ca.uhn.fhir.model.primitive.CodeDt;
import uk.nhs.fhir.makehtml.data.BindingResourceInfo;
import uk.nhs.fhir.makehtml.data.FhirURL;
import uk.nhs.fhir.makehtml.data.LinkData;
import uk.nhs.fhir.makehtml.data.ResourceInfo;
import uk.nhs.fhir.makehtml.data.ResourceInfoType;
import uk.nhs.fhir.makehtml.html.TableTitle;
import uk.nhs.fhir.util.FhirDocLinkFactory;
import uk.nhs.fhir.util.HAPIUtils;

// KGM 8/May/2017 Altered meta table column to % widths

public class OperationDefinitionParameterTableDataProvider {

	private final List<Parameter> parameters;
	private final FhirDocLinkFactory fhirDocLinkFactory;
	
	public OperationDefinitionParameterTableDataProvider(List<Parameter> parameters, FhirDocLinkFactory linkDataFactory) {
		this.parameters = parameters;
		this.fhirDocLinkFactory = linkDataFactory;
	}

	public List<TableTitle> getColumns() {

		// KGM 8/May/2017 Altered meta table column to % widths
		return Lists.newArrayList(
			new TableTitle("Name", "The logical name of the element", "20%"),
			new TableTitle("Card.", "Minimum and maximum # of times the element can appear in the instance", "10%"),
			new TableTitle("Type", "Reference to the type of the element", "20%x"),
			new TableTitle("Value", "Additional information about the element", "50%")
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
		
		List<ResourceInfo> resourceFlags = Lists.newArrayList();
		
		ParameterBinding binding = parameter.getBinding();
		if (!binding.isEmpty()) {
			ResourceInfo bindingFlag = buildBindingResourceInfo(binding);
			resourceFlags.add(bindingFlag);
		}
		
		ResourceReferenceDt profile = parameter.getProfile();
		if (!profile.isEmpty()) {
			try {
				resourceFlags.add(new ResourceInfo("Profile", new FhirURL(profile.getReferenceElement().getValue()),  ResourceInfoType.PROFILE));
			} catch (MalformedURLException e) {
				throw new IllegalStateException(e);
			}
		}
		
		//TODO tuple parameters
		List<Parameter> parts = parameter.getPart();
		if (!parts.isEmpty()) {
			throw new NotImplementedException("Tuple parameter");
		}
		
		return resourceFlags;
	}
	
	ResourceInfo buildBindingResourceInfo(ParameterBinding binding) {
		String choice = HAPIUtils.resolveDatatypeValue(binding.getValueSet());
		String strength = binding.getStrength();
		
		try {
			return new BindingResourceInfo(Optional.empty(), Optional.of(new FhirURL(choice)), strength);
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}
	}
}