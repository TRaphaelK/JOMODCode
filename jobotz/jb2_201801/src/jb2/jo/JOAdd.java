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

/**
 * JO Game adresses
 *
 * @author vear
 */
public class JOAdd {

    public static double JO_VER = 1.757;
    public static String windowName = "Joint Operations: Typhoon Rising V1.7.5.7";
    // TODO: steam version might differ from standalone
    // lloks like the steam and standalone versions have the same memory layout
    public static boolean isSteamVersion;

    public enum Adress {

        /* Base adress for writing console messages
             * Howto: Open up console, write into it, do not close, then search for it
         */
        ConsoleBase(0xb3e1dc),
        /* Check adress if the console is open
        * Howto: Open up console, search 1L, close, filter 0L
        *        open chat, filter 1L, close filter 0L, first one
         */
        ConsoleCheck(0xb3e308),
        /* Chat messages base adress
        * Howto: write in chat, and find it
         */
        ConsoleChatbase(0xb3ea38),
        /* Length of a chat record
        * Howto: write two chat messages, compare their start adresses
         */
        ConsoleChatitem(0x64),
        /* Notification messages
         * Howto: Kill yourself, search for "name is dead."
         * Notificaitons are not present textually on a dedicated server.
         */
        ConsoleNotfBase(0),
        /* Length of an notification record
         * Howto: Do two actions, compare string start adresses
         */
        ConsoleNotfItem(0),
        /* Game variables:
         */
        /* Base of local variables:
     * Howto: run wac with v0=999, v1=998, v2=997
     * Search for 0xE7030000E6030000E503000000000000 in consecutive L-s
     * Set one from WAC, it should change in mem
     * Set one in mem, test it with WAC
         */
        WacVariableLocalBase(0),
        /* Base of global variables:
     * Howto: run wac with g0=996, g1=995, g2=994
     * Search for 0xE4030000E3030000E203000000 in consecutive L-s
     * Set one in WAc, it should change in mem
     * Set one in mem, test it with WAC
         */
        WacVariableGlobalBase(0),
        /* Game object classtable:
         */
        /* Base adress for pointers on game object classes:
        * Howto: run amap with a single bot in it, search for its ssn number
        * removessn(1008), filter for changed to 0, get first
        * substract 0x7c, this is the ssnbase_class0, search for this reference
        * Tha classtable has format:
        * base(0,1,2,3,4)    structurelen    currentitems  maxitems
        * Sample 1.6.7.13:
        * class0: 0x3a90080    904         3       256 bot+player
        * class1: 0x3ac8c64   1360        8       1200 vehicle
        * class2: 0x3c57748    812         0       1200 static
        * class3: 0x3d4596c    988         2       768 
        * class4: 0x3dff150    988         0       128
         */
        ClassTableBase(0xA892E0),
        /* Coord blocks base
            * Howto: Search for reading an ssnbase+0xc, at one near:
            * add esi, 0x2c86830
            * from fun_5d6a30
         */
        CoordBase(0x2be3d30), // from fun_5d6a30 not tested
        // RAW coordinate handling
        /* Coordinate multiplier
        * Howto: Breakpoint on writing dyn coords, its read before that
        * by code. Float
         */
        Coordmul(0x7C3310),
        /* players table
         */
        /* Pointer to players table
         * Howto: Find the name of the host, it should be around 0x120f5d8
         * the base is -0x28 from that, which should contain the ssnbase of ssn 10000
         */
        PlayerdataPlayersPtr(0x24C0CA8),
        /* Maximum players on the server
         * Howto: on adress playersptr-4
         */
        PlayerDataMaxPlayers(0x24C0CA8-4),
        /* Server state 
         */
        /* Points to a pointer to the serverinfo base adress
         * Howto: Search for "SERVERNAME", search for adress it is on
         * exit game, reeenter if its the same again, its ok
        * Check stuff in dedicated mode.
         */
        ServerinfoBasePtr(0xc9480c),
        /* Dedicated or serve&play?
         * Howto: value of 1 before the time passed when dedicated hosting
         * 
         */
        ServerinfoDedicated(0x024C1964),
        /* Time left: on the map
         * Howto: not the current time left. use  ((minutes+1)*60*60) to ((minutes/2)*60*60) search
         * it should be a steadily decreasing value
         */
        ServerinfoTimeleft(0x24C1958),
        /* Time passed
         * Howto: its near time left
         */
        ServerinfoTimepassed(0x024C1968),
        /* Server state:
         * Howto: search for 3 when running a map, 0 when in main menu, 1-map loading
         * 0xC87598 is jó
         */
        ServerinfoServerstate(0xC87020),
        /* Winning side:
         * Howto: use a WAC to set win(1) or win(2), search for 1 or 2 during
         * map end. Filter for 0 during the map runs, repeat until adress got 
         * near the timepassed
         */
        ServerinfoSidewin(0),
        /* Terrain file
         * Howto: Search for the terrain name
         */
        MapinfoTerrain(0),
        MapinfoEnv(0),
        MapinfoWatercolor(0),
        MapinfoTilename(0),;

        public final JOPointer value;

        Adress(long add) {
            this.value = new JOPointer(add);
        }
    }
    
    public static int ssnStrulen[] = { 904, 1360, 812, 988, 988};

    // pointed to by GameObjectStruct.TypeBasePtr
    public enum TypeStruct {
        // pointed to by TypeidBasePtr
        Objname(0x0),
        Typeid(0x50),
        Objcode(0x60),
        ;
        public final int value;

        TypeStruct(int add) {
            this.value = add;
        }

    }

    public enum CoordStruct {
        Xcoord(0x54),
        Ycoord(0x58),
        Zcoord(0x5c);

        public final int value;

        CoordStruct(int add) {
            this.value = add;
        }

    }

    public enum ClassTableIndex {
        // from base Adress.ClassTableBase
        Class0(0),
        Class1(0x10),
        Class2(0x20),
        Class3(0x30),
        Class4(0x40);

        public final int value;

        ClassTableIndex(int add) {
            this.value = add;
        }

    }

    public enum ClassTableStruct {

        // from base Adress.ClassTableBase + ClassTableIndex.Class0
        BasePtr(0x0),
        Strulen(0x4),
        Items(0x8),
        MaxItems(0xc),
        ;

        public final int value;

        ClassTableStruct(int add) {
            this.value = add;

        }

    }

    public enum PlayerDataStruct {

        /* Structure length of player record
         * Howto: find difference in adresses of names of host and player 1
         */
        Strulen(0x188E8),
        /* Player points/kills/deaths
         * Howto: Kill a player with another, then compare, 10 exp (0xA) for a kill
         */
        GameObjectPtr(0),
        // pointer to the next player structure
        NextPtr(0xc),
        Slot(0x18), // same as ssnbase + 0x78, 0 for the host when dedicated
        Name(0x28),
        Points(0xbc),
        Kills(0x5c),
        Deaths(0x64),;

        public final int value;

        PlayerDataStruct(int add) {
            this.value = add;
        }

    }

    public enum GameObjectStruct {
        /* SSN data
        * The main structure of game object data
         */
        // some coordinate part use * coordmul
        IntType(0),
        // multiply by (*Adress.Coordmul)
        RawZ(0x4),
        RawNX(0x8),
        RawY(0xc),
        // headings, divide by .C_GAMEOBJECT_DEGMUL
        Theta(0x10),
        Phi(0x14),
        Omega(0x18),
        
        TypeBasePtr(0x20),

        /* Renderflags, use it bitwise (thanks DV)
         */
        RenderFlags(0x24),
        //RenderFlags2(0x28),
        // pointer to building the entity is inside/on
        InsideBuildingPtr(0x28),
        
        // type_id structure base pointer (model pointer?)
        ModelPtr(0x30),
        Model2Ptr(0x34),
        // only for players
        LoadoutPtr(0x3c), 
        /* Pointer to botdata structure
            * Howto: compare
         */
        BotdataPtr(0x68),
        // counts down for bots
        CountDown(0x74),
        // renumbered ssn's (1757 ok)
        SSN(0x7c), 
        // 80, 84, 88 position
        // 8c look direction, only bots & players(1757)
        
        // only player name
        PlayerName(0xf4),
        // a0 vertical speed
        
        
        /*
         * Respawn (thanks DV)
         */
        // Respawn timer
        //set to 1 for no more spawns, 0 for infinite
        //NumberOfSpawns(0x128),
        //0x100 prone, 0x200 crouch, move direction with flags
        MoveDirection(0x12c),

        //also the abandon timer. hardcoded to respawn after 14 secs
        GameobjectRespawnTimer(0x148),
        /* Player idle timer
            * Howto: increases when player is not moving
            * resets to 0 when moved
         */
        PlayerIdle(0x148), //1757 ok
        
        // 0x154 team?
        
        // marker radius
        Radius(0x15e), //1757 ok

        /* Team property of game objects
        * Howto: Compare objects
        * Compare changed data, when PSP captured
        * byte data!
         */
        Team(0x162), //1757 ok
        
        //byte . seatnumber the player is in (DV)
        // 1-passanger, 2- driver, 3-weapon
        Seatnumber(0x168), //1757 ok
        
        //type to what it is attached
        //AttachedToType(0x168),
        // attached to vehicle gameobject 
        AttachedToPtr(0x16c), //1757 ok
        // driver of vehicle in case of a vehicle
        VehiclesDriver(0x170), //1757 ok
        KilledBy(0x178),    //1757 ok
        //coordbase = ((coordid & 0x7fff) << 7) + Adress.Coordbase.value
        Coordid(0x1b6), // 0x1bc probably
        
        LFPGroup(0x21a), // 1757 ok
        // flag height of LFP word max 65536, min 0
        LFPFlagheight(0x21c), // 1757 ok
        PlayersInLFPTeam(0x220), // 1757, byte, own team players
        PlayersInLFPTeamEnemy(0x221), // 1757, byte, enemy team players
        
        // Respawn position data (thanks DV)
        // does not work, can't change the respawn
         
        OrgPosZ(0x234), //1757 ok
        OrgPosNX(0x238), //1757 ok
        OrgPosY(0x23c), //1757 ok
        OrgRot(0x240), //1757 ok
        
        OrgPitch(0x25c),
        OrgRoll(0x260),
        //pointer to owner object ( for emplaced weapons) (DV)
        Powner(0x2EC),
        // movement speed, direction
        // speed signed word
        Speed(0x29c),
        // speed direction 65535 back
        SpeedDir(0x29e),;

        public final int value;

        GameObjectStruct(int add) {
            this.value = add;
        }
    }

        // from base GameObjectStruct.BotdataPtr
    public enum BotDataStruct {
        
        // pointer to bot/vehicle game object
        GameObjectPtr(0),
        
        //positions, unknown usage
        PosZ(0x10),
        PosNX(0x14),
        PosY(0x18),
        Theta(0x1c),
        // 100-accuracy2
        Accuracy2(0x28),
        // 100-accuracy1
        Accuracy1(0x2c),

        /* Bot attack distance (word)
         * Howto: Issue SSNatt from wac, see changed
         */
        //distance at which they start shooting enemy
        AttDis(0x3e),
        // 0x42 minvis, but should be the same as the maxvix (word)
        //distance at which they stop advancing
        MinVis(0x42),
        /* Bot vision distance (word)
         * Howto: Issue SSNmax from wac, see changed
         */
        //distance at which they detect enemy
        MaxVis(0x46),
        /* Botdata structure length
        * Howto: distance of two botdata bases
         */
        Strulen(0xac), 
        
        // 0x80 ?
        
        // 0x88 0 - crouch, 1 - walk, 2 - run
        Posture(0x88),
        // pointer to waypoint ssbase
        MoveTargetPtr(0x90),
        /* Move action/ waypoint list for bots
         * Howto: issue ssn2ssn from wac for a bot, this shoud change to 125,
         *  send him to wpn list, it will contain the wpnlist number
         */
        MoveWpn(0x94), // 1757 ok
        /* Move ssn target for bot
         * Howto: issue ssn2ssn from wac for a bot, this should change to ssn number
         */
        MoveSSN(0x98),; // 1757 ok
        public final int value;

        BotDataStruct(int add) {
            this.value = add;
        }
    }

    public enum RenderFlag {

        /*
                * Render bits (thanks DV), 
                * set with & bitmask
                * check with | <bitmask
         */
        Normal(0),
        Norender(1),
        // kill, is killed bit for players and bots
        Kill(2),
        // destroyed bits for vehicles
        Destroyed(0x6),
        //DV: when object is a player, it sits. if it's a vehicle there is  player attached(i think)
        Sit(0x40),
        Removed(0x7f),
        // x400
        Jumping(0x2000),
        Shooting(0x4000),
        // 0x20000
        OnLadder(0x100000),
        // player near armory
        NearArmory(0x400000);

        public final int value;

        RenderFlag(int val) {
            this.value = val;
        }
    }

    public enum RenderFlag2 {
        Norespawn(0x1),
        Noradar(0x80),
        
        ;

        public final int value;

        RenderFlag2(int val) {
            this.value = val;
        }
    }

    public enum AttachedToType {
        Seat(0x1),
        Driver(0x2),
        Emplaced(0x3),;

        public final int value;

        AttachedToType(int val) {
            this.value = val;
        }
    }

    public enum ServerState {
        NOP(0),
        LOAD(1),
        START(2),
        RUN(3),
        END(4);
        
        public final int value;
        ServerState(int val) {
            value = val;
        }
    }
    
    
    
    // max flag height
    public static long C_MAXFLAGHEIGHT = 65536;
    public static float C_GAMEOBJECT_DEGMUL = 11930464.705555555555555555555556F;
    public static int C_GAMEOBJECT_BACKSPEED = 65535;

}
