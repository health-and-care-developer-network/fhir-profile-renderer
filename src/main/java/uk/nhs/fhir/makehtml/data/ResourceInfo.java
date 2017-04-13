package uk.nhs.fhir.makehtml.data;

import java.net.URL;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class ResourceInfo {
	private final String constraintName;
	private final Optional<String> description;
	private final Optional<URL> descriptionLink;
	private final List<String> extraTags= Lists.newArrayList();
	
	private final ResourceInfoType type;

	public ResourceInfo(String constraintName, String description, ResourceInfoType type) {
		this(constraintName, Optional.of(description), Optional.empty(), type);
	}
	public ResourceInfo(String constraintName, URL descriptionLink, ResourceInfoType type) {
		this(constraintName, Optional.empty(), Optional.of(descriptionLink), type);
	}
	public ResourceInfo(String constraintName, String description, URL descriptionLink, ResourceInfoType type) {
		this(constraintName, Optional.of(description), Optional.of(descriptionLink), type);
	}
	
	public ResourceInfo(String constraintName, Optional<String> description, Optional<URL> descriptionLink, ResourceInfoType type) {
		Preconditions.checkArgument(description.isPresent() || descriptionLink.isPresent(), "Constraint without description or link");
		
		this.constraintName = constraintName;
		this.description = description;
		this.descriptionLink = descriptionLink;
		this.type = type;
	}
	
	public void addExtraTag(String tag) {
		extraTags.add(tag);
	}
	
	public String getName() {
		return constraintName;
	}
	
	public Optional<String> getDescription() {
		return description;
	}
	
	public Optional<URL> getDescriptionLink() {
		return descriptionLink;
	}
	
	public List<String> getExtraTags() {
		return extraTags;
	}
	
	public ResourceInfoType getType() {
		return type;
	}
}