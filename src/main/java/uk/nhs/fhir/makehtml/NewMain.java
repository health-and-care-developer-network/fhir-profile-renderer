/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.nhs.fhir.makehtml;

import static uk.nhs.fhir.makehtml.XMLParserUtils.getDescription;
import static uk.nhs.fhir.makehtml.XMLParserUtils.getElementCardinality;
import static uk.nhs.fhir.makehtml.XMLParserUtils.getElementName;
import static uk.nhs.fhir.makehtml.XMLParserUtils.getElementTypeName;
import static uk.nhs.fhir.makehtml.XMLParserUtils.getFlags;
import static uk.nhs.fhir.makehtml.XMLParserUtils.getTitle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import static uk.nhs.fhir.makehtml.XMLParserUtils.getElementTypeList;

import uk.nhs.fhir.util.FileLoader;
import uk.nhs.fhir.util.FileWriter;

/**
 *
 * @author tim.coates@hscic.gov.uk
 */
public class NewMain implements Constants {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        NewMain instance = new NewMain();
        instance.run();
    }
    private static final Logger LOG = Logger.getLogger(NewMain.class.getName());

    private void run() {
        //String filename = "/uk/nhs/fhir/makehtml/NRLS-DocumentReference-1-0.xml";
        String filename = "/uk/nhs/fhir/makehtml/account.profile.xml";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family: sans-serif;\" xmlns=\"http://www.w3.org/1999/xhtml\">");
        sb.append(" <table border='0' cellpadding='0' cellspacing='0'>\n");
        sb.append("  <tr>\n");
        sb.append("   <th style=\"font-size: small;\">Name</th>\n");
        sb.append("   <th style=\"font-size: small;\">Flags</th>\n");
        sb.append("   <th style=\"font-size: small;\">Card.</th>\n");
        sb.append("   <th style=\"font-size: small;\">Type</th>\n");
        sb.append("   <th style=\"font-size: small;\">Description &amp; Constraints</th>\n");
        sb.append("  </tr>\n");

        ArrayList<MyElement> elementList = new ArrayList<MyElement>();
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document document = docBuilder.parse(FileLoader.loadFileOnClasspathAsStream(filename));

            // We already have a text block, abort...
            //NodeList narrative = document.getElementsByTagName("text");
            //if(narrative.getLength() == 0) {
            NodeList names = document.getElementsByTagName("name");
            Node name = names.item(0);
            NamedNodeMap typeAttributes = name.getAttributes();
            String resourceName = typeAttributes.getNamedItem("value").getTextContent();

            NodeList snapshot = document.getElementsByTagName("snapshot");
            Element node = (Element) snapshot.item(0);
            NodeList elements = node.getElementsByTagName("element");

            NodeList differential = document.getElementsByTagName("differential");
            Element diffNode = (Element) differential.item(0);
            NodeList diffElements = diffNode.getElementsByTagName("element");
            
            //for(int i = 0; i < diffElements.getLength(); i++) {    
            //}
            
            // Here we're looping through the elements...
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                String elementName = getElementName(element);
                String cardinality = getElementCardinality(element);
                String typeName = getElementTypeName(element);
                String flags = getFlags(element);
                String description = getTitle(element);
                String hoverText = getDescription(element);
                
                // Here we loop through the differential elements to see if this element has been changed by this profile
                boolean hasChanged = false;
                for(int j = 0; j < diffElements.getLength(); j++) {
                    Element diffElement = (Element) diffElements.item(j);
                    if(getElementName(diffElement).equals(elementName)) {
                        hasChanged = true;
                    }
                }
                
                // Catch elements which can be of multiple types...
                if(typeName.equals("Multiple_Type_Choice")) {
                    ArrayList<String> types = getElementTypeList(element);
                    elementList.add(new MyElement(elementName, cardinality, typeName, flags, description, hoverText, hasChanged));
                    for(String type : types) {
                        elementList.add(new MyElement(elementName + "." + type, "", type, flags, "", "", hasChanged));
                    }
                } else {
                    if(typeName.equals("Reference")) {
                        typeName = getReferenceTypes(element);
                        elementList.add(new MyElement(elementName, cardinality, typeName, flags, description, hoverText, hasChanged));
                    } else {
                        elementList.add(new MyElement(elementName, cardinality, typeName, flags, description, hoverText, hasChanged));
                    }
                }
            }

            int mutedAtLevel = 100;
            for (int i = 0; i < elementList.size(); i++) {

                MyElement item = (MyElement) elementList.get(i);
                if (item.isDisplay() == false) {
                    mutedAtLevel = item.getLevel();
                } else {
                    if (item.getLevel() > mutedAtLevel) {
                        item.setDisplay(false);
                    } else {
                        mutedAtLevel = 100;
                    }
                }
            }

            for (int i = 0; i < elementList.size(); i++) {
                MyElement item = (MyElement) elementList.get(i);
                if (item.isDisplay() == false) {
                    elementList.remove(i);
                }
            }

            // Set the last item to be 'last'
            elementList.get(elementList.size() - 1).setIsLast(true);

            // Now work through and draw each element
            for (int i = 0; i < elementList.size(); i++) {
                MyElement item = (MyElement) elementList.get(i);

                sb.append(START_TABLE_ROW);

                // Make a cell for the tree images and the name
                sb.append(START_TABLE_CELL);
                            // Tree and object type images need to go here
                // Simplest cases...
                //<editor-fold defaultstate="collapsed" desc="Handle tree icons for Level 1 elements">
                if (item.getLevel() == 1) {
                    if (i == elementList.size() - 1) {
                        // This is the last item, so:
                        sb.append(CORNER);
                    } else {
                        sb.append(LINEWITHT);
                    }
                } else {
                    //</editor-fold>
                    boolean oneContinues = false;
                    for (int n = i; n < elementList.size(); n++) {
                        int d = elementList.get(n).getLevel();
                        if (d == 1) {
                            // We need to show the level 1 line continuing beside our line
                            oneContinues = true;
                            break;
                        }
                    }
                    //<editor-fold defaultstate="collapsed" desc="Handle tree icons for Level 2 elements">
                    if (item.getLevel() == 2) {
                        if (i == elementList.size() - 1) {
                            // It's the last item so a spacer then the 'end corner'
                            sb.append(SPACER);
                            sb.append(CORNER);
                        } else {

                            if (oneContinues) {
                                sb.append(LINE);
                            } else {
                                sb.append(SPACER);
                            }
                            if (elementList.get(i + 1).getLevel() == 1) {
                                // We're the last at level 2, so corner
                                sb.append(CORNER);
                            } else {
                                // Here we need to determine whether this is the last at level two
                                boolean lastOfTwos = false;
                                for (int n = i; n < elementList.size(); n++) {
                                    if (elementList.get(n).getLevel() == 1) {
                                        lastOfTwos = true;
                                        break;
                                    }
                                }
                                if (lastOfTwos) {
                                    sb.append(CORNER);
                                } else {
                                    sb.append(LINEWITHT);
                                }
                            }
                        }
                    } else {
    //</editor-fold>
                        // Now figure out what level two is doing...
                        boolean twoContinues = false;
                        for (int n = i; n < elementList.size(); n++) {
                            int d = elementList.get(n).getLevel();
                            if (d == 2) {
                                twoContinues = true;
                            }
                            if (d == 1) {
                                break;
                            }
                        }
                        //<editor-fold defaultstate="collapsed" desc="Handle tree icons for Level 3 elements">
                        if (item.getLevel() == 3) {
                            if (i == elementList.size() - 1) {
                                // It's the last item so two spacers then the 'end corner'
                                sb.append(SPACER);
                                sb.append(SPACER);
                                sb.append(CORNER);
                            } else {
                                // Now figure out whether there are more level one elements to come, so do we continue the very left leg of the tree?
                                if (oneContinues) {  // We have more items coming at Level one, so continue the tree...
                                    sb.append(LINE);
                                } else {
                                    // No more at Level one, so we add a spacer...
                                    sb.append(SPACER);
                                }
                                if (twoContinues) {  // We have more items coming at Level two, so continue the tree...
                                    sb.append(LINE);
                                } else {
                                    sb.append(SPACER);
                                }
                                // Now just figure out whether we're the last at Level 3, and add icon...
                                if (elementList.get(i + 1).getLevel() != 3) {
                                    // We're last, add a corner
                                    sb.append(CORNER);
                                } else {
                                    sb.append(LINEWITHT);
                                }

                            }
                        } else {
    //</editor-fold>
                            // Now figure out what level three is doing
                            boolean threeContinues = false;
                            for (int n = i; n < elementList.size(); n++) {
                                int d = elementList.get(n).getLevel();
                                if (d == 3) {
                                    threeContinues = true;
                                }
                                if (d == 1 || d == 2) {
                                    break;
                                }
                            }
                            //<editor-fold defaultstate="collapsed" desc="Handle tree icons for Level 4 elements">
                            if (item.getLevel() == 4) {
                                if (i == elementList.size() - 1) {
                                    // It's the last item so two spacers then the 'end corner'
                                    sb.append(SPACER);
                                    sb.append(SPACER);
                                    sb.append(SPACER);
                                    sb.append(CORNER);
                                } else {

// Now figure out whether there are more level one elements to come, so do we continue the very left leg of the tree?
                                    if (oneContinues) {  // We have more items coming at Level one, so continue the tree...
                                        sb.append(LINE);
                                    } else {
                                        // No more at Level one, so we add a spacer...
                                        sb.append(SPACER);
                                    }
                                    if (twoContinues) {  // We have more items coming at Level two, so continue the tree...
                                        sb.append(LINE);
                                    } else {
                                        sb.append(SPACER);
                                    }
                                    if (threeContinues) {  // We have more items coming at Level two, so continue the tree...
                                        sb.append(LINE);
                                    } else {
                                        sb.append(SPACER);
                                    }
                                    // Now just figure out whether we're the last at Level 4, and add icon...
                                    if (elementList.get(i + 1).getLevel() != 4) {
                                        // We're last, add a corner
                                        sb.append(CORNER);
                                    } else {
                                        sb.append(LINEWITHT);
                                    }
                                }
                            } else {
    //</editor-fold>
                                // Now figure out what level four is doing
                                boolean fourContinues = false;
                                for (int n = i; n < elementList.size(); n++) {
                                    int d = elementList.get(n).getLevel();
                                    if (d == 4) {
                                        fourContinues = true;
                                    }
                                    if (d == 1 || d == 2 || d == 3) {
                                        break;
                                    }
                                }
                                //<editor-fold defaultstate="collapsed" desc="Handle tree icons for Level 5 elements">
                                if (item.getLevel() == 5) {
                                    if (i == elementList.size() - 1) {
                                        // It's the last item so two spacers then the 'end corner'
                                        sb.append(SPACER);
                                        sb.append(SPACER);
                                        sb.append(SPACER);
                                        sb.append(SPACER);
                                        sb.append(CORNER);
                                    } else {

                                        if (oneContinues) {  // We have more items coming at Level one, s ocontinue the tree...
                                            sb.append(LINE);
                                        } else {
                                            sb.append(SPACER);
                                        }
                                        if (twoContinues) {  // We have more items coming at Level two, s ocontinue the tree...
                                            sb.append(LINE);
                                        } else {
                                            sb.append(SPACER);
                                        }
                                        if (threeContinues) {  // We have more items coming at Level three, s ocontinue the tree...
                                            sb.append(LINE);
                                        } else {
                                            sb.append(SPACER);
                                        }
                                        if (fourContinues) {  // We have more items coming at Level four, s ocontinue the tree...
                                            sb.append(LINE);
                                        } else {
                                            sb.append(SPACER);
                                        }
                                        // Now just figure out whether we're the last at Level 5, and add icon...
                                        if (elementList.get(i + 1).getLevel() != 5) {
                                            // We're last, add a corner
                                            sb.append(CORNER);
                                        } else {
                                            sb.append(LINEWITHT);
                                        }
                                    }
                                } else {
    //</editor-fold>
                                    // Now figure out what level five is doing
                                    boolean fiveContinues = false;
                                    for (int n = i; n < elementList.size(); n++) {
                                        int d = elementList.get(n).getLevel();
                                        if (d == 5) {
                                            fiveContinues = true;
                                        }
                                        if (d == 1 || d == 2 || d == 3 || d == 4) {
                                            break;
                                        }
                                    }
                                    //<editor-fold defaultstate="collapsed" desc="Handle tree icons for Level 6 elements">
                                    if(i == elementList.size() - 1) {
                                        // It's the last item so two spacers then the 'end corner'
                                        sb.append(SPACER);
                                        sb.append(SPACER);
                                        sb.append(SPACER);
                                        sb.append(SPACER);
                                        sb.append(SPACER);
                                        sb.append(CORNER);
                                    } else {
                                        if (item.getLevel() == 6) {
                                            if (oneContinues) {  // We have more items coming at Level one, s ocontinue the tree...
                                                sb.append(LINE);
                                            } else {
                                                sb.append(SPACER);
                                            }
                                            if (twoContinues) {  // We have more items coming at Level two, s ocontinue the tree...
                                                sb.append(LINE);
                                            } else {
                                                sb.append(SPACER);
                                            }
                                            if (threeContinues) {  // We have more items coming at Level three, s ocontinue the tree...
                                                sb.append(LINE);
                                            } else {
                                                sb.append(SPACER);
                                            }
                                            if (fourContinues) {  // We have more items coming at Level four, s ocontinue the tree...
                                                sb.append(LINE);
                                            } else {
                                                sb.append(SPACER);
                                            }
                                            if (fiveContinues) {  // We have more items coming at Level five, s ocontinue the tree...
                                                sb.append(LINE);
                                            } else {
                                                sb.append(SPACER);
                                            }
                                            if(elementList.get(i + 1).getLevel() != 6) {
                                                // We're last, add a corner
                                                sb.append(CORNER);
                                            } else {
                                                sb.append(LINEWITHT);
                                            }
                                        }
                                    }
                                    //</editor-fold>
                                }

                            }
                        }
                    }
                }

                // Simple case, the base resource node...
                if (item.getLevel() == 0) {
                    sb.append(RESOURCE);
                }
                
                DataTypes thisType = null;

                if (item.getTypeName() != null) {
                    // If a simle datatype...
                    if (item.getTypeName().equals("boolean")
                            || item.getTypeName().equals("code")
                            || item.getTypeName().equals("date")
                            || item.getTypeName().equals("dateTime")
                            || item.getTypeName().equals("instant")
                            || item.getTypeName().equals("unsignedInt")
                            || item.getTypeName().equals("string")
                            || item.getTypeName().equals("decimal")
                            || item.getTypeName().equals("uri")
                            || item.getTypeName().equals("integer")) {
                        sb.append(BASETYPE);
                        thisType = DataTypes.Simple;
                    }
                    // If a Resource Type...
                    if (item.getTypeName().equals("Identifier")
                            || item.getTypeName().equals("ContactPoint")
                            || item.getTypeName().equals("Address")
                            || item.getTypeName().equals("CodeableConcept")
                            || item.getTypeName().equals("Attachment")
                            || item.getTypeName().equals("Resource")
                            || item.getTypeName().equals("Signature")
                            || item.getTypeName().equals("BackboneElement")
                            || item.getTypeName().equals("HumanName")
                            || item.getTypeName().equals("Period")
                            || item.getTypeName().equals("Coding")) {
                        sb.append(DATATYPE);
                        thisType = DataTypes.Resource;
                    }
                    if(item.getTypeName().equals("Reference")) {
                        sb.append(REFERENCE);
                        thisType = DataTypes.Reference;
                    }
                    if(item.getTypeName().equals("Multiple_Type_Choice")) {
                        sb.append(CHOICETYPE);
                    }                
                } else {
                    // Seems to be a special case, used in eg Bundle resource types
                    sb.append(BUNDLE);
                }

                if(item.isChanged()) {
                    sb.append("<b>");
                    sb.append(item.getNiceTitle());
                    sb.append("</b>");
                } else {
                    sb.append(item.getNiceTitle());
                }
                sb.append(END_TABLE_CELL);

                // Now the flags column
                sb.append(START_TABLE_CELL);
                sb.append(item.getFlags());
                sb.append(END_TABLE_CELL);

                // Now the Cardinality column
                sb.append(START_TABLE_CELL);
                sb.append(item.getCardinality());
                sb.append(END_TABLE_CELL);

                // Now the type column
                sb.append(START_TABLE_CELL);
                if(item.getTypeName().equals("Multiple_Type_Choice") == false) {
                    if(thisType == DataTypes.Resource) {
                        sb.append("<a href=\"https://www.hl7.org/fhir/" + item.getTypeName().toLowerCase() + ".html\">");
                        sb.append(item.getTypeName());
                        sb.append("</a>");
                    } else {
                        if(thisType == DataTypes.Reference) {
                            sb.append("<a href=\"https://www.hl7.org/fhir/references.html\">");
                            sb.append(item.getTypeName());
                            sb.append("</a>");
                        } else {
                            sb.append(item.getTypeName());
                        }
                    }
                }                
                sb.append(END_TABLE_CELL);

                // And now the description
                sb.append(START_TABLE_CELL);
                sb.append(item.getDescription());
                sb.append(END_TABLE_CELL);

                sb.append(END_TABLE_ROW);
            }
            //}

        } catch (SAXException ex) {
            Logger.getLogger(NewMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(NewMain.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(NewMain.class.getName()).log(Level.SEVERE, null, ex);
        }
        sb.append(" </table>\n");
        sb.append("</div>");

        // Now, create a StructureDefinition resource, add our test section to the top, and serialise it back out
        String augmentedResource = ResourceBuilder.addTextSectionToResource(FileLoader.loadFileOnClasspath(filename), sb.toString());
        FileWriter.writeFile("output.xml", augmentedResource.getBytes());

        // And finally let's also wrap our HTML and write it to another file to see how it looks...
        String html = "<html>\n<body>\n" + sb.toString() + "</body>\n</html>";
        FileWriter.writeFile("output.html", html.getBytes());
    }
}
