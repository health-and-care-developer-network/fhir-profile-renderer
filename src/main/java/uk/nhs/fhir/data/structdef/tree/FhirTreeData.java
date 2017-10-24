package uk.nhs.fhir.data.structdef.tree;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import uk.nhs.fhir.data.structdef.ConstraintInfo;
import uk.nhs.fhir.data.structdef.ExtensionType;
import uk.nhs.fhir.makehtml.RendererError;
import uk.nhs.fhir.makehtml.RendererErrorConfig;
import uk.nhs.fhir.util.StringUtil;

public class FhirTreeData implements Iterable<FhirTreeTableContent> {
	private static Logger LOG = LoggerFactory.getLogger(FhirTreeData.class);
	
	private final FhirTreeTableContent root;
	
	public FhirTreeData(FhirTreeTableContent root) {
		Preconditions.checkNotNull(root);
		
		this.root = root;
	}

	public FhirTreeTableContent getRoot() {
		return root;
	}

	@Override
	public Iterator<FhirTreeTableContent> iterator() {
		return new FhirTreeIterator (this);
	}
	
	public void dumpTreeStructure() {
		for (FhirTreeTableContent node : this) {
			int indentSize = node.getPath().split("\\.").length - 1;
			String indent = StringUtil.nChars(indentSize, '\t');
			LOG.debug(indent + node.getDisplayName());
		}
	}

	public void tidyData() {
		removeExtensionsSlicingNodes(root);
		stripChildlessDummyNodes(root);
		removeUnwantedConstraints(root);
		stripComplexExtensionChildren(root);
	}
	
	// Remove inlined child nodes of complex extensions
	private void stripComplexExtensionChildren(FhirTreeTableContent node) {
		boolean isComplexExtension = node.getExtensionType().isPresent() 
		  && node.getExtensionType().get().equals(ExtensionType.COMPLEX)
		  // exclude root node
		  && node.getPath().contains(".");
		
		List<? extends FhirTreeTableContent> children = node.getChildren();
		
		for (int i=children.size()-1; i>=0; i--) {
			
			FhirTreeTableContent child = children.get(i);
			if (isComplexExtension) {
				children.remove(i);
			} else {
				stripComplexExtensionChildren(child);
			}
		}
	}

	private static final Set<String> constraintKeysToRemove = new HashSet<>(Arrays.asList(new String[] {"ele-1"}));
	
	private void removeUnwantedConstraints(FhirTreeTableContent node) {
		for (FhirTreeTableContent child : node.getChildren()) {
			List<ConstraintInfo> constraints = child.getConstraints();
			for (int constraintIndex=constraints.size()-1; constraintIndex>=0; constraintIndex--) {
				ConstraintInfo constraint = constraints.get(constraintIndex);
				if (constraintKeysToRemove.contains(constraint.getKey())) {
					constraints.remove(constraintIndex);
				}
			}
			
			removeUnwantedConstraints(child);
		}
	}

	private void stripChildlessDummyNodes(FhirTreeTableContent node) {
		for (int i=node.getChildren().size()-1; i>=0; i--) {
			FhirTreeTableContent child = node.getChildren().get(i);
			
			stripChildlessDummyNodes(child);
			
			if (child instanceof DummyFhirTreeNode
			  && child.getChildren().isEmpty()) {
				node.getChildren().remove(i);
			}
		}
	}

	private void removeExtensionsSlicingNodes(FhirTreeTableContent node) {
		List<? extends FhirTreeTableContent> children = node.getChildren();
		
		// if there is an extensions slicing node (immediately under root), remove it.
		for (int i=children.size()-1; i>=0; i--) {
			FhirTreeTableContent child = children.get(i);
			if (child.getPathName().equals("extension")
			  && child.hasSlicingInfo()) {
				children.remove(i);
			} else {
				// call recursively over whole tree
				removeExtensionsSlicingNodes(child);
			}
		}
	}
	
	public void stripRemovedElements() {
		stripRemovedElements(root);
	}
	
	/**
	 * Remove all nodes that have cardinality max = 0 (and their children)
	 */
	private void stripRemovedElements(FhirTreeTableContent node) {
		List<? extends FhirTreeTableContent> children = node.getChildren();
		
		for (int i=children.size()-1; i>=0; i--) {
			
			FhirTreeTableContent child = children.get(i);
			
			if (child.isRemovedByProfile()) {
				children.remove(i);
			} else {
				stripRemovedElements(child);
			}
		}
	}
	
	public void resolveLinkedNodes() {
		resolveNameLinkedNodes();
		resolveIdLinkedNodes();
	}

	private void resolveIdLinkedNodes() {
		Map<String, List<FhirTreeTableContent>> expectedIds = Maps.newHashMap();
		Map<String, FhirTreeTableContent> nodesWithId = Maps.newHashMap();
		
		for (FhirTreeTableContent node : this) {
			
			Optional<String> id = node.getId();
			boolean hasId = id.isPresent();
			if (hasId) {
				nodesWithId.put(id.get(), node);
			}
			
			Optional<String> linkedNodeId = node.getLinkedNodeId();
			boolean hasIdLinkedNode = linkedNodeId.isPresent();
			if (hasIdLinkedNode) {
				List<FhirTreeTableContent> nodesLinkingToThisId;
				if (expectedIds.containsKey(linkedNodeId.get())) {
					nodesLinkingToThisId = expectedIds.get(linkedNodeId.get());
				} else {
					nodesLinkingToThisId = Lists.newArrayList();
					expectedIds.put(linkedNodeId.get(), nodesLinkingToThisId);
				}
				
				nodesLinkingToThisId.add(node);
			}
			
			if (hasId && hasIdLinkedNode) {
				if (node.getId().get().equals(node.getLinkedNodeId().get())) {
					RendererErrorConfig.handle(RendererError.LINK_REFERENCES_ITSELF, "Link " + node.getPath() + " references itself (" + node.getId().get() + ")");
				}
			}
			
			if (hasIdLinkedNode && node.getFixedValue().isPresent()) {
				RendererErrorConfig.handle(RendererError.FIXEDVALUE_WITH_LINKED_NODE, 
				  "Node " + node.getPath() + " has a fixed value (" + node.getFixedValue().get() + ") and a linked node"
				  + " (" + node.getLinkedNodeId().get() + ")");
			}
		}
		
		setLinkedNodes(expectedIds, nodesWithId);
	}

	void setLinkedNodes(Map<String, List<FhirTreeTableContent>> expectedIds,
			Map<String, FhirTreeTableContent> nodesWithId) {
		for (Map.Entry<String, List<FhirTreeTableContent>> expectedIdEntry : expectedIds.entrySet()) {
			String expectedId = expectedIdEntry.getKey();
			List<FhirTreeTableContent> nodesWithLink = expectedIdEntry.getValue();
			
			if (nodesWithId.containsKey(expectedId)) {
				for (FhirTreeTableContent nodeWithLink : nodesWithLink) {
					if (nodeWithLink instanceof FhirTreeNode) {
						((FhirTreeNode)nodeWithLink).setLinkedNode(nodesWithId.get(expectedId));
					}
					// If we are in a dummy node, we don't need to do anything since the backup node
					// should contain this information
				}
			} else {
				String nodesWithMissingLinkTarget = String.join(", ", 
					nodesWithLink
						.stream()
						.map(node -> node.getPath())
						.collect(Collectors.toList()));
				
				RendererErrorConfig.handle(RendererError.MISSING_REFERENCED_NODE, 
					"Linked node(s) at " + nodesWithMissingLinkTarget + " missing target (" + expectedId + ")");
			}
		}
	}

	public void resolveNameLinkedNodes() {
		Map<String, List<FhirTreeTableContent>> expectedNames = Maps.newHashMap();
		Map<String, FhirTreeTableContent> namedNodes = Maps.newHashMap();
		
		for (FhirTreeTableContent node : this) {
			boolean hasName = node.getName().isPresent();
			if (hasName) {
				namedNodes.put(node.getName().get(), node);
			}
			
			boolean hasLinkedNode = node.getLinkedNodeName().isPresent();
			if (hasLinkedNode) {
				List<FhirTreeTableContent> nodesLinkingToThisName;
				if (expectedNames.containsKey(node.getLinkedNodeName().get())) {
					nodesLinkingToThisName = expectedNames.get(node.getLinkedNodeName().get());
				} else {
					nodesLinkingToThisName = Lists.newArrayList();
					expectedNames.put(node.getLinkedNodeName().get(), nodesLinkingToThisName);
				}
				
				nodesLinkingToThisName.add(node);
			}
			
			if (hasName && hasLinkedNode) {
				if (node.getName().get().equals(node.getLinkedNodeName().get())) {
					RendererErrorConfig.handle(RendererError.LINK_REFERENCES_ITSELF, "Link " + node.getPath() + " references itself (" + node.getName().get() + ")");
				}
			}
			
			if (hasLinkedNode && node.getFixedValue().isPresent()) {
				RendererErrorConfig.handle(RendererError.FIXEDVALUE_WITH_LINKED_NODE, 
				  "Node " + node.getPath() + " has a fixed value (" + node.getFixedValue().get() + ") and a linked node"
				  + " (" + node.getLinkedNodeName().get() + ")");
			}
		}
		
		setLinkedNodes(expectedNames, namedNodes);
	}

	public void cacheSlicingDiscriminators() {
		for (FhirTreeTableContent content : this) {
			if (content instanceof FhirTreeNode) {
				FhirTreeNode node = (FhirTreeNode)content;
				node.cacheSlicingDiscriminator();
			}
		}
	}
}

class FhirTreeIterator implements Iterator<FhirTreeTableContent> {

	// Each node down the tree to the current node
	Deque<FhirNodeAndChildIndex> chain = new ArrayDeque<>();
	
	private final FhirTreeData data;
	
	public FhirTreeIterator(FhirTreeData data) {
		Preconditions.checkNotNull(data);
		this.data = data;
	}

	@Override
	public boolean hasNext() {
		if (!returnedRoot()) {
			return true;
		}

		// true if any node in the chain to the current node has further children to offer
		for (FhirNodeAndChildIndex node : chain) {
			if (node.hasMoreChildren()) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean returnedRoot() {
		return !chain.isEmpty();
	}

	@Override
	public FhirTreeTableContent next() {
		if (!returnedRoot()) {
			return supplyRoot();
		}
		
		while (true) {
			if (chain.getLast().hasMoreChildren()) {
				return nextChildOfCurrent();
			} else if (chain.size() > 1) {
				chain.removeLast();
			} else {
				throw new NoSuchElementException();
			}
		}
		
		/*int parentOfCurrentIndex = chain.size()-2;
		for (int nodeIndex=parentOfCurrentIndex; nodeIndex>=0; nodeIndex--) {
			FhirNodeAndChildIndex lastNode = chain.get(nodeIndex);
			if (nodeHasNextChild(nodeIndex)) {
				// update chain
				int targetSize = nodeIndex + 1;
				while (chain.size() > targetSize) {
					int currentNodeIndex = maxChainIndex();
					chain.remove(currentNodeIndex);
					if (childIndexes.size() > currentNodeIndex) {
						childIndexes.remove(childIndexes.size()-1);
					}
				}
				
				// increment index entry
				int nextChildIndex = childIndexes.get(nodeIndex)+1;
				childIndexes.set(nodeIndex, nextChildIndex);

				// add new node to chain and return
				FhirTreeTableContent nextChildNode = fhirNodeAndChildIndex.getChildren().get(childIndexes.get(nodeIndex));
				chain.add(nextChildNode);
				return nextChildNode;
			}
		}
		
		throw new NoSuchElementException();*/
	}

	private FhirTreeTableContent supplyRoot() {
		FhirTreeTableContent root = data.getRoot();
		chain.add(new FhirNodeAndChildIndex(root));
		return root;
	}
	
	private FhirTreeTableContent nextChildOfCurrent() {
		FhirTreeTableContent child = chain.getLast().nextChild();
		chain.add(new FhirNodeAndChildIndex(child));
		return child;
	}
	
	private class FhirNodeAndChildIndex {
		private final FhirTreeTableContent node;
		private Integer currentChildIndex;
		
		FhirNodeAndChildIndex(FhirTreeTableContent node) {
			this.node = node;
			this.currentChildIndex = null;
		}

		public boolean hasMoreChildren() {
			if (node.getChildren().isEmpty()) {
				return false;
			} else if (currentChildIndex == null) {
				return true;
			} else {
				int maxChildIndex = node.getChildren().size()-1;
				return maxChildIndex > currentChildIndex;
			}
		}
		
		public FhirTreeTableContent nextChild() {
			if (currentChildIndex == null) {
				currentChildIndex = 0;
			} else {
				currentChildIndex++;
			}
			
			return node.getChildren().get(currentChildIndex);
		}
	}
}