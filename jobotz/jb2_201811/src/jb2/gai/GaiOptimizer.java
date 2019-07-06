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
package jb2.gai;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import jb2.AppContext;
import jb2.Config;
import jb2.math.FastMath;
import jb2.math.Tensor;
import jb2.util.FastList;

/**
 *
 * @author vear
 */
public class GaiOptimizer {

    protected static final Logger log = Logger.getLogger(GaiOptimizer.class.getName());

    // the list of all bots
    protected FastList<EntParameters> entList = new FastList<>();
    protected FastList<EntParameters> entTopList = new FastList<>();

    // TODO: save min and max values of parameters
    // reload min and max, generate then generate new random nodes
    public final Tensor minMaxValues = new Tensor(EntParameters.weightsCount, 2);
    protected long lastHparamUpdate;

    protected String fileName;
    protected String bakFile;

    public GaiOptimizer() {

    }

    protected void initMinMaxValues() {
        // only take min/max from enum, if not loaded
        for (EntParameters.ParamType t : EntParameters.ParamType.values()) {
            minMaxValues.set(t.ordinal(), t.min, t.max);
        }
    }

    public EntParameters getEntParameters() {
        if (entList.size < 100) {
            EntParameters navParams = new EntParameters();
            navParams.weights.setRandomValues(minMaxValues);
            return navParams;
        }

        recalcTopList();

        // get one in top 10
        EntParameters parm1 = entTopList.get(FastMath.rand.nextInt(10));
        // get another one in top 100
        EntParameters parm2 = entTopList.get(FastMath.rand.nextInt(100));

        // combine them
        EntParameters navParams = new EntParameters();
        navParams.combine(parm1, parm2, minMaxValues);

        // add it
        return navParams;
    }

    public void addEntParameters(EntParameters params) {
        if (entList.size > 100) {
            if (FastMath.rand.nextFloat() < ((float) entList.size / 300f)) {
                EntParameters p = entList.get(0);
                log.log(Level.INFO, "Removed params with XP {0}", p.xp);
                // remove the oldest entry
                entList.remove(0);
            } else {
                // only add if the XP of item to add is bigger than the last one
                EntParameters last = entTopList.get(entTopList.size-1);
                if(last.xp > params.xp) {
                    log.log(Level.INFO, "Parameters with XP {0} ignored", params.xp);
                    return;
                }
            }
        }
        entList.add(params);
    }

    protected void recalcTopList() {
        // the ordered list of bot parameters by their success scores
        entTopList.clear();
        entTopList.addAll(entList);
        entTopList.sort((Comparator<EntParameters>) (EntParameters o1, EntParameters o2)
                -> -Float.compare(o1.xp, o2.xp));
    }
    
    /*
    protected void recalcMinMax() {
        boolean first = true;
        // recalculate min and max based on current values        
        if (entList.size > 100) {
            recalcTopList();

            for (int epi = 0; epi < 100; epi++) {
                EntParameters p = entTopList.get(epi);
                // go trough and calculate the max of max and min of min
                for (int i = 0; i < p.weights.size(); i++) {
                    float value = p.weights.get(i, 0);
                    if (first) {
                        minMaxValues.setComponent(i, 0, value);
                        minMaxValues.setComponent(i, 1, value);
                        continue;
                    }
                    float min = minMaxValues.get(i, 0);
                    float max = minMaxValues.get(i, 1);
                    if (value < min) {
                        minMaxValues.setComponent(i, 0, value);
                    }
                    if (value > max) {
                        minMaxValues.setComponent(i, 1, value);
                    }
                }
                first = false;
            }
        }

        logMinMax();
    }
*/

    protected void logMinMax() {
        StringBuilder sb = new StringBuilder();
        for (EntParameters.ParamType p : EntParameters.ParamType.values()) {
            float min = minMaxValues.get(p.ordinal(), 0);
            float max = minMaxValues.get(p.ordinal(), 1);
            sb.append("(");
            sb.append(p.ordinal());
            sb.append(",");
            sb.append(p.name());
            sb.append(",");
            sb.append(min);
            sb.append(",");
            sb.append(max);
            sb.append(")");
        }
        log.log(Level.INFO, "Hyperparameters min/max {0}", sb.toString());
    }

    public void saveParams() {
        if (AppContext.gameMatch.timestamp - AppContext.gameMatch.starttime < 2 * 60 * 1000) {
            // if map has run less than two minutes, do not save the refinedhparams
            log.log(Level.INFO, "Map has run less than 2 minutes, hparams are not updated");
            return;
        }

        //recalcMinMax();

        // save min and max of hyperparameters
        File bakfile = new File(bakFile);
        // delete bak file if it exists
        if (bakfile.exists()) {
            bakfile.delete();
        }

        Path path = Paths.get(fileName);
        // copy previous file to bakfile
        File file = path.toFile();
        if (file.exists()) {
            log.log(Level.INFO, "Backing up old hparam file to {0}", bakFile);
            file.renameTo(bakfile);
        }

        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(path))) {
            pw.print("min");
            for (EntParameters.ParamType p : EntParameters.ParamType.values()) {
                float min = minMaxValues.get(p.ordinal(), 0);
                pw.print("," + min);
            }
            pw.println();

            pw.print("max");
            for (EntParameters.ParamType p : EntParameters.ParamType.values()) {
                float max = minMaxValues.get(p.ordinal(), 1);
                pw.print("," + max);
            }
            pw.println();
            // number of genomes
            pw.println(entList.size);
            // print all genomes
            entList.forEachEntry(parm-> {
                // xp of the genome
                pw.print(parm.xp);
                for (EntParameters.ParamType p : EntParameters.ParamType.values()) {
                    float val = parm.getParameter(p);
                    pw.print("," + val);
                }
                pw.println();
            });
            pw.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public void loadParams() {
        entList.clear();
        
        fileName = AppContext.serverInfo.missionFilename;
        fileName = fileName.replace(".", "_");
        fileName = Config.basedir + "\\" + fileName + "_hparam";

        bakFile = fileName + "_bak.txt";
        fileName = fileName + ".txt";

        Path path = Paths.get(fileName);

        // load parameters        
        File file = path.toFile();
        if (!file.exists() || !file.canRead()) {
            log.log(Level.WARNING, "No hparam file exists, starting with default hparam");
            initMinMaxValues();
            return;
        }

        final int[] lineNo = new int[1];
        final int[] numGenomes = new int[1];
        try (Stream<String> stream = Files.lines(path)) {
            stream.forEach(line -> {
                // split line
                String[] vals = line.split(",");
                
                switch(lineNo[0]) {
                    case 0: {
                        // min values
                        for (EntParameters.ParamType p : EntParameters.ParamType.values()) {
                            float val = Float.valueOf(vals[p.ordinal()+1]);
                            minMaxValues.setComponent(p.ordinal(), 0, val);
                        }
                    } break;
                    case 1: {
                        // max values
                        for (EntParameters.ParamType p : EntParameters.ParamType.values()) {
                            float val = Float.valueOf(vals[p.ordinal()+1]);
                            minMaxValues.setComponent(p.ordinal(), 1, val);
                        }
                    } break;
                    case 2: {
                        // number of genomes
                        numGenomes[0] = Integer.valueOf(line);
                    } break;
                    default: {
                        // new genome
                        EntParameters ep = new EntParameters();
                        ep.xp = Float.valueOf(vals[0]);
                        for (EntParameters.ParamType p : EntParameters.ParamType.values()) {
                            float val = Float.valueOf(vals[p.ordinal()+1]);
                            ep.weights.setComponent(p.ordinal(), 0, val);
                        }
                        entList.add(ep);
                    } break;
                }
                lineNo[0]++;
            });
            stream.close();
        } catch (Exception e) {
            log.log(Level.WARNING, "Error loading hparam file", e);
        }
        
        if(numGenomes[0] != entList.size) {
            log.log(Level.SEVERE, "Missmatch in declared and loaded hparam sets");
        }

        // log out the loaded hparams
        log.log(Level.INFO, "Loaded hparams from file");
        logMinMax();
    }
}
