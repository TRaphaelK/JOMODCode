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
package jb2.jo;

import java.util.EnumMap;
import jb2.util.IntIntMap;
import jb2.util.IntMap;

/**
 *
 * @author vear
 */

public class TypeMap {

    protected static final IntMap<TypeId> typeMap = new IntMap<TypeId>();

    public static enum TypeFolder {
        Host,  //dedicated host
        Player, 
        AI, // commandable bot
        Vehicle,
        LFP, // strategic target
        Dynamic,
        Emplaced,
        Static,
        Marker,
        Waypoint,   // type id: 6005
        Sound,
        Effect,
        ;
    }
    public static final int numFolders = TypeFolder.Effect.ordinal()+1;
    
    protected static final TypeFolder V = TypeFolder.Vehicle;
    protected static final TypeFolder L = TypeFolder.LFP;
    protected static final TypeFolder D = TypeFolder.Dynamic;
    protected static final TypeFolder E = TypeFolder.Emplaced;
    protected static final TypeFolder M = TypeFolder.Marker;
    protected static final TypeFolder W = TypeFolder.Waypoint;
    protected static final TypeFolder S = TypeFolder.Sound;
    protected static final TypeFolder F = TypeFolder.Effect;
    
    
    

    public static enum TypeId {
        Dcycle2(74, V),
        Dt801(165, V),
        T80trret(167, D),
        Dapche1(171, V),
        bdtret1(191, E),
        FX_Mosquitos(204, F),
        Chmlfp1(302, D),
        satlfp1(303, D),
        satlfp2(304, D),
        klfp1(305, D),
        oillfp1(309, D),
        oillfp2(310, D), // added to excel this far
        LFPtowr2(508, D), 
        O_tanka(1190, D),
        Comdish1(1194, D),
        Dmil261(1214, V),
        Indonesian_Helo(1215, V),
        Daav1(1295, V),
        Dmrk51(1299, V),
        Datv1(1301, V),
        Dsuv1(1305, V),
        DAH62(1306, V),
        DAH63(1307, V),
        LFPtowr1(1336, D),
        JO_Tent(1397, D),
        Bunker2(1359, D),
        RebelHQ(1398, D),
        Ihqlfp2(1399, D),
        DTruck2(1420, V),
        MK19(1869, E),
        EWepBnkr(1880, E),
        CTflag(1884, D),
        D50cal(1887, E),
        EWepBnkrD(1889, E),
        E50triB(1991, E),
        E50triB2(1902, E),
        Hmk19(1990, E),
        Dblkhwk1(2010, V),
        Map_center(2043, M),
        Dcboat04(2080, V),
        WpnCt5B(2154, D),
        flagblu(4091, D),
        flaggrn(4095, D),
        Flag_blue(4098, D),
        Flag_red(4100, D),
        Start_Blue(6003, M),
        Start_Red(6004, M),
        waypoint(6005, W),
        KOTH_center(6006, D),
        snd_bird(6200, S),;

        public final int typeId;
        public final TypeFolder folder;
        public float xzextent;
        public float yextent;

        private TypeId(int i, TypeFolder typefolder) {
            typeId = i;
            folder = typefolder;            
            // default extents based on type
            if(typefolder==TypeFolder.LFP) {
                xzextent = 5f;
                yextent = 2;
            } else {
                xzextent = 1f;
                yextent = 1f;
            }
        }
        
        private TypeId(int i, TypeFolder typefolder, float xzExtent, float yExtent) {
            typeId = i;
            folder = typefolder;
            xzextent = xzExtent;
            yextent = yExtent;
        }

    }
    static {
        initMap();
    }

    protected static void initMap() {
        TypeId[] ids = TypeId.values();
        for(int i=0; i<ids.length; i++) {
            TypeId typ = ids[i];
            typeMap.put(typ.typeId, typ);
        }
    }
    public static TypeFolder getTypeFolder(int typeId) {        
        TypeId typ=typeMap.get(typeId);
        if(typ==null)
            return null;
        return (TypeFolder) typ.folder;
    }
    
    public static TypeId getType(int typeId) {
        return typeMap.get(typeId);
    }
}
