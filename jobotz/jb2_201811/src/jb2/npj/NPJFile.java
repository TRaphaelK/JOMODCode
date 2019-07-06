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
import jb2.math.BoundingBox;
import jb2.math.Vector3f;
import jb2.util.FastList;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ContentFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * NL NPJ file is a malformed XML file, this class gets around the bugs and
 * loads it into an XML DOM
 *
 * @author vear
 */
public class NPJFile {
    
    protected static final Logger log = Logger.getLogger(NPJFile.class.getName());
    protected Document npjDoc = null;
    
    // the default radius for markers
    public float markerRadius = 1f;
    
    protected class Bot {
        int team;
        int count;
        int typeId;
    }
    protected FastList<Bot> addlist = new FastList<>();
    
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
            if (positionElement == null) {
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
    
    public void prepareMap() {
        // ai+vehicle, markers, max id
        final int[] vals = new int[3];
        // bounds for generation
        final BoundingBox[] genbounds = new BoundingBox[2];

        // count the total number of AI + VEHICLE
        // count the total number of MARKER with TYPE_ID 6005
        // change radius to 0.5 units
        // get the max ID value
        final Vector3f botpos = new Vector3f();

        // if number of markers less than needed, add them with position 1,1,1
        this.forEachItem((element) -> {
            // get the max id 
            int id = Integer.valueOf(element.getChildText("ID"));
            if (id > vals[2]) {
                vals[2] = id;
            }
            String en = element.getName();
            switch (en) {
                case "AI": {
                    vals[0]++;
                    int team = Integer.valueOf(element.getChildText("TEAM"));
                    if (team != 1 && team != 2) {
                        return;
                    }
                    team--;
                    botpos.x = Float.valueOf(element.getChild("POSITION").getChildText("X"));
                    botpos.z = Float.valueOf(element.getChild("POSITION").getChildText("Z"));
                    if (genbounds[team] == null) {
                        genbounds[team] = new BoundingBox();
                        genbounds[team].center.set(botpos);
                        genbounds[team].extents.set(1f, 1f, 1f);
                    } else {
                        genbounds[team].add(botpos);
                    }
                    // set default properties, and respawn
                    /*
                <ACCURACY1>75</ACCURACY1>
		<ACCURACY2>100</ACCURACY2>
		<FIRE_TIMER>5</FIRE_TIMER>
		<SPAWN>1</SPAWN>
		<VISION_RANGE>100</VISION_RANGE>
		<MIN_ATTACK>0</MIN_ATTACK>
		<MAX_ATTACK>100</MAX_ATTACK>
                    */

                }
                break;
                case "VEHICLE": {
                    vals[0]++;
                    
                }
                break;
                case "MARKER": {
                    // check type ID
                    int typeId = Integer.valueOf(element.getChildText("TYPE_ID"));
                    if (typeId == 6005) {
                        // marker, count it
                        vals[1]++;
                        // set the radius
                        element.getChild("RADIUS").setText(String.valueOf(markerRadius));
                    }
                }
                break;
            }
        });
        Element rte = npjDoc.getRootElement();
        
        // TODO: add additional bots
        
        // if there are more bots than markers, then generate new markers
        while(vals[1]<vals[0]) {
            // increase the ssn id
            vals[2]++;
            Element marker = new Element("MARKER");
            marker.addContent(new Element("ID").setText(String.valueOf(vals[2])));
            marker.addContent(new Element("NAME").setText("waypoint"+String.valueOf(vals[2])));
            marker.addContent(new Element("TYPE_ID").setText(String.valueOf(6005)));
            marker.addContent(new Element("DESCRIPTION").setText("waypoint"+String.valueOf(vals[2])));
            Element pose = new Element("POSITION");
            pose.addContent(new Element("X").setText(String.valueOf(1)));
            pose.addContent(new Element("Y").setText(String.valueOf(1)));
            pose.addContent(new Element("Z").setText(String.valueOf(1)));
            marker.addContent(pose);
            marker.addContent(new Element("RADIUS").setText(String.valueOf(markerRadius)));
            rte.addContent(marker);
            vals[1]++;
        }
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
    
    public void addBotToGen(int team, int count, int typeid) {
        Bot bot = new Bot();
        bot.team = team;
        bot.count = count;
        bot.typeId = typeid;
        addlist.add(bot);
    }
    
    public static void main(String[] args) {
        String filename = "C:\\games\\JO\\\\AS-KaroBOTZ.npj"; //"C:\\games\\JO\\AS-KutuArmsBOTZ.npj"
        String outFilename = "C:\\games\\JO\\AS-KaroBOTZ_fixed.npj"; //"C:\\games\\JO\\AS-KutuArmsBOTZ_fixed.npj"
        
        NPJFile file = new NPJFile();
        file.fromXML(filename);
        file.fixNPJ();
        // TODO: add bots
        
        file.prepareMap();
        file.toXML(outFilename);
    }
}
