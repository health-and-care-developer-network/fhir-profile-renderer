package uk.nhs.fhir.render.format.codesys;

import java.util.Optional;

import com.google.common.base.Preconditions;

public class CodeSystemConceptTableRowData {
	private final String code;
	private final Optional<String> description;
	private final Optional<String> definition;

	public CodeSystemConceptTableRowData(String code, Optional<String> description, Optional<String> definition) {
		this.code = Preconditions.checkNotNull(code);
		this.description = description;
		this.definition = definition;
	}

	public String getCode() {
		return code;
	}
	
	public Optional<String> getDescription() {
		return description;
	}

	public boolean hasDescription() {
		return description.isPresent();
	}

	public Optional<String> getDefinition() {
		return definition;
	}

	public boolean hasDefinition() {
		return definition.isPresent();
	}
}
