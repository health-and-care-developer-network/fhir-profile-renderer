package uk.nhs.fhir.makehtml.opdef;

import com.google.common.collect.Lists;

import uk.nhs.fhir.makehtml.html.LinkCell;
import uk.nhs.fhir.makehtml.html.SimpleTextCell;
import uk.nhs.fhir.makehtml.html.TableRow;

public class OperationDefinitionMetaDataRowFormatter {
	public TableRow formatRow(OperationDefinitionMetaDataRowData source) {
		return new TableRow(
			new SimpleTextCell(source.getRowTitle()),
			new LinkCell(source.getTypeLink(), Lists.newArrayList(), Lists.newArrayList("fhir-link")),
			new SimpleTextCell(source.getContent()));
	}
}