/*
 * 
 * Vear 2017-2018  * 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package jb2.npj;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ContentFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * NL NPJ file is a malformed XML file, this class gets around the bugs and loads it into an XML DOM
 * @author vear
 */
public class NPJFile {

    protected static final Logger log = Logger.getLogger(NPJFile.class.getName());
    protected Document npjDoc = null;

    protected String getFiltered(FileReader flr) {
        try {
            // nile places the & character in the file, which is invalid by itself
            // so filter it out
            StringWriter writer = new StringWriter();
            int c;
            while ((c = flr.read()) != -1) {
                if (c != '&') {
                    writer.write(c);
                }
            }

            BufferedReader reader = new BufferedReader(new StringReader(writer.toString()));
            writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            // filter out search/replace lines with bad XML tags
            // <CORNER2_X>.*</CORNER1_X>
            String rdln;
            Pattern p1 = Pattern.compile("[^<]*<CORNER2_X>([^<]*)</CORNER1_X>");
            Pattern p2 = Pattern.compile("[^<]*<CORNER2_Y>([^<]*)</CORNER1_Y>");
            Pattern p3 = Pattern.compile("[^<]*<CORNER2_Z>([^<]*)</CORNER1_Z>");
            while ((rdln = reader.readLine()) != null) {
                Matcher m = p1.matcher(rdln);
                if (m.matches() && m.groupCount() == 1) {
                    // found that offending line
                    rdln = "<CORNER2_X>" + m.group(1) + "</CORNER2_X>";
                } else {
                    m = p2.matcher(rdln);
                    if (m.matches() && m.groupCount() == 1) {
                        // found that offending line
                        rdln = "<CORNER2_Y>" + m.group(1) + "</CORNER2_Y>";
                    } else {
                        m = p3.matcher(rdln);
                        if (m.matches() && m.groupCount() == 1) {
                            // found that offending line
                            rdln = "<CORNER2_Z>" + m.group(1) + "</CORNER2_Z>";
                        } else {
                            // other workarounds?
                        }
                    }
                }
                printWriter.println(rdln);
            }
            return writer.toString();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void fromXML(String filename) {
        // feldolgozzuk az XML-t
        SAXBuilder build = new SAXBuilder();
        //build.setValidation(true);
        //build.setIgnoringElementContentWhitespace(true);
        // TODO filterelni a & karaktert
        // construct a string filtered for the & character
        try {
            String xmlString = getFiltered(new FileReader(filename));
            npjDoc = build.build(new StringReader(xmlString));
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        }
        ArrayList<Element> all = new ArrayList();
        Iterator ei = npjDoc.getDescendants(new ContentFilter(ContentFilter.ELEMENT));
        while (ei.hasNext()) {
            Element e = (Element) ei.next();
            if (e.getChildren().size() > 0) {
                all.add(e);
            }
        }

        //npj.getRootElement().removeContent(new org.jdom.filter.ContentFilter(org.jdom.filter.ContentFilter.TEXT));
        // clean up and remove emtpy text content
        for (Element e : all) {
            if (e.getChildren().size() > 0) {
                e.removeContent(new ContentFilter(ContentFilter.TEXT));
            }
        }
    }

    public void toXML(String filename) {
        FileWriter out = null;

        XMLOutputter xmlo = new XMLOutputter();
        Format fmt = xmlo.getFormat();
        fmt.setOmitDeclaration(true);
        fmt.setEncoding("ISO-8859-2");
        fmt.setExpandEmptyElements(true);
        fmt.setIndent("\t");
        fmt.setLineSeparator("\r\n");
        xmlo.setFormat(fmt);
        try {
            out = new FileWriter(filename);
            xmlo.output(npjDoc, out);
            
            out.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        } finally {
            try {
                out.close();
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
    }

    public void forEachItem(Consumer<Element> function) {
        // loop at all
        Element rte = npjDoc.getRootElement();
        rte.getChildren().forEach((ce) -> {
            String en = ce.getName();
            switch (en) {
                // ignore events
                case "EVENTS":
                    break;
                case "WAYPOINT_LIST":
                    break;
                case "GROUP":
                    break;
                default:
                    // stuff with an ID
                    Element ide = ce.getChild("ID");
                    if (ide != null) {
                        function.accept(ce);
                    }
                    break;
            }
        });
    }

    public float getRadius(Integer id) {
        float radius = 0;
        Element ce = getElementWithID(id);
        if (ce != null) {
            String re = ce.getChildText("RADIUS");
            if (re != null) {
                radius = Float.valueOf(re);
            }
        }
        return radius;
    }

    public Element getElementWithID(Integer id) {
        Element se = null;
        String stringId = id.toString();
        // loop at elements, ignoring WAYPOINTS, and GROUPS
        Iterator ei = npjDoc.getRootElement().getChildren().iterator();
        while (ei.hasNext()) {
            Element element = (Element) ei.next();
            String elementName = element.getName();

            if (elementName.equals("WAYPOINT_LIST") || elementName.equals("GROUP")) {
                continue;
            }

            Element idElement = element.getChild("ID");
            if (idElement != null) {
                if (stringId.equals(idElement.getText())) {
                    return element;
                }
            }
        }
        return null;
    }

    public void fixNPJ() {

        final boolean[] hasTypeId = new boolean[1];

        this.forEachItem((element) -> {

            hasTypeId[0] = false;
            int elementId = Integer.valueOf(element.getChildText("ID"));

            element.getChildren("TYPE_ID").forEach(typeElement -> {

                if (hasTypeId[0] == true) {
                    // remoe all addition type_id elements
                    typeElement.detach();
                    return;
                }

                String tp = typeElement.getText();
                int itp = Integer.valueOf(tp);
                if (itp < 0) {
                    log.log(Level.WARNING, "Object ID {0} has bad TYPE_ID {1}", new Object[]{elementId, tp});
                    typeElement.detach();
                    return;
                }

                hasTypeId[0] = true;
            });
            if (hasTypeId[0] == false) {
                log.log(Level.WARNING, "Object ID {0} has no valid TYPE_ID", elementId);
            }
            Element positionElement = element.getChild("POSITION");
            if(positionElement==null) {
                return;
            }
            //if it has THETA then normalize it to -180 to 180
            Element thetaElement = positionElement.getChild("THETA");
            if (thetaElement == null) {
                return;
            }
            float thetaValue = Float.valueOf(thetaElement.getText());
            if (thetaValue < -180) {
                thetaValue = 360 + thetaValue;
                thetaElement.setText(String.valueOf(thetaValue));
                log.log(Level.WARNING, "Object ID {0} has invalid theta", elementId);
            }
            if (thetaValue > 180) {
                thetaValue = -360 + thetaValue;
                thetaElement.setText(String.valueOf(thetaValue));
                log.log(Level.WARNING, "Object ID {0} has invalid theta", elementId);
            }
        });
    }

    public int getMaxId(String type) {
        final int[] maxId = new int[1];
        this.forEachItem((Element element) -> {
            String sid = element.getChildText("ID");
            if (sid != null) {
                int nno = Integer.valueOf(sid);
                if (nno > maxId[0]) {
                    maxId[0] = nno;
                }
            }
        });
        return maxId[0];
    }

    public static void main(String[] args) {
        // test
        NPJFile file = new NPJFile();
        file.fromXML("C:\\games\\JO\\AS-KutuArmsBOTZ.npj");
        file.fixNPJ();
        file.toXML("C:\\games\\JO\\AS-KutuArmsBOTZ_fixed.npj");
    }
}
