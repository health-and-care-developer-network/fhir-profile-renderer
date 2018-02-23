package uk.nhs.fhir.error;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import uk.nhs.fhir.data.wrap.WrappedResource;
import uk.nhs.fhir.makehtml.render.RendererContext;
import uk.nhs.fhir.makehtml.render.RendererErrorHandler;
import uk.nhs.fhir.util.StringUtil;

public abstract class RendererEventAccumulator implements RendererErrorHandler {

	protected abstract void displaySortedEvents(List<RendererEvents> events);
	
	private final Map<File, RendererEvents> events = Maps.newHashMap();
	
	private RendererContext context = null;
	
	@Override
	public void setContext(RendererContext context) {
		this.context = context;
	}
	
	public void addEvent(RendererEvent event) {
		File sourceFile = event.getSourceFile();
		
		if (events.containsKey(sourceFile)) {
			events.get(sourceFile).add(event);
		} else {
			events.put(sourceFile, new RendererEvents(event));
		}
	}

	public boolean foundErrors() {
		return foundAnyOfType(EventType.ERROR);
	}

	public boolean foundWarnings() {
		return foundAnyOfType(EventType.WARNING);
	}
	
	public boolean foundAnyOfType(EventType eventType) {
		return events.values()
				.stream()
				.flatMap(events -> events.getEvents().stream())
				.anyMatch(event -> event.getEventType().equals(eventType));
	}
	
	public Map<File, RendererEvents> getEvents() {
		return ImmutableMap.copyOf(events);
	}

	@Override
	public void ignore(String info, Optional<Exception> throwable) {
		// nothing
	}

	@Override
	public void log(String info, Optional<Exception> throwable) {
		File source = context.getCurrentSource();
		Optional<WrappedResource<?>> resource = context.getCurrentParsedResource();
		
		if (throwable.isPresent()) {
			addEvent(RendererEvent.warning(info, source, resource, throwable.get()));
		} else {
			addEvent(RendererEvent.warning(info, source, resource));
		}
	}

	@Override
	public void error(Optional<String> info, Optional<Exception> error) {
		File source = context.getCurrentSource();
		Optional<WrappedResource<?>> resource = context.getCurrentParsedResource();
		addEvent(RendererEvent.error(info, source, resource, error));
	}

	private static final Comparator<RendererEvents> BY_TYPE_THEN_NAME =
		new Comparator<RendererEvents>(){
			@Override
			public int compare(RendererEvents o1, RendererEvents o2) {
				Optional<WrappedResource<?>> resource1 = o1.getResource();
				Optional<WrappedResource<?>> resource2 = o2.getResource();
				
				int typeNameComparison;
				if (!resource1.isPresent() 
				  || !resource2.isPresent()) {
					typeNameComparison = 0;					
				} else {
					String typeName1 = resource1.get().getResourceType().getDisplayName();
					String typeName2 = resource2.get().getResourceType().getDisplayName();
					typeNameComparison = typeName1.compareTo(typeName2);
				}
				
				if (typeNameComparison != 0) {
					return typeNameComparison;
				}
				
				return o1.getFile().getName().compareTo(o2.getFile().getName());
			}};
	
	@Override
	public void displayOutstandingEvents() {
		List<RendererEvents> events = new ArrayList<>(this.events.values());
		
		// sort first by resourceType, then by filename
		Collections.sort(events, BY_TYPE_THEN_NAME);
		
		displaySortedEvents(events);
	}
	
	protected String combineLoggableInfo(Optional<String> info, Optional<Exception> error) {
		List<String> combinedInfo = Lists.newArrayList();
		
		if (info.isPresent()) {
			combinedInfo.add(info.get());
		}
		
		if (error.isPresent()) {
			combinedInfo.add(StringUtil.getStackTrace(error.get()));
		}
		
		return String.join("\n", combinedInfo);
	}
}