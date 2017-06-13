package uk.nhs.fhir.makehtml.html;

import uk.nhs.fhir.makehtml.FhirURLConstants;

/**
 * Created by kevinmayfield on 05/05/2017.
 */
public class Dstu2Fix {

    public static String fixValuesetLink(String link)
    {
        if (link.startsWith(FhirURLConstants.HL7_FHIR)
          && !link.startsWith(FhirURLConstants.HL7_DSTU2)) {
        	
            if (link.startsWith(FhirURLConstants.HL7_V3)) {
            	// e.g. http://hl7.org/fhir/v3/MaritalStatus -> http://hl7.org/fhir/DSTU2/v3/MaritalStatus/index.html
                link = link.replace(FhirURLConstants.HL7_V3, FhirURLConstants.HL7_DSTU2_V3);
                link += "/index.html";
            } else if (link.startsWith(FhirURLConstants.HL7_FHIR + "/ValueSet/")) {
            	// e.g. http://hl7.org/fhir/ValueSet/identifier-use -> http://hl7.org/fhir/DSTU2/valueset-identifier-use.html
            	link = link.replace(FhirURLConstants.HL7_FHIR + "/ValueSet/", FhirURLConstants.HL7_DSTU2 + "/valueset-");
            	link += ".html";
            } else {
            	// http://hl7.org/fhir/administrative-gender -> http://hl7.org/fhir/DSTU2/valueset-administrative-gender.html
                link = link.replace(FhirURLConstants.HL7_FHIR + "/", FhirURLConstants.HL7_DSTU2 + "/valueset-");
                link += ".html";
            }
        }
        return link;
    }
}
