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

import jb2.math.Vector3f;

/**
 *
 * @author vear
 */
public class JOPointer  {

    protected long peer;
    
    public JOPointer(long peer) {
        this.peer = peer;
    }
        
    public void set(long adr) {
        peer = adr;
    }
    
    public long get() {
        return peer;
    }
    
    public boolean isNull() {
        return peer == 0;
    }

    public int getInt(long offset) {
        return JOGame.readInt(this, offset);
    }
    public int getInt(JOAdd.GameObjectStruct gameObjectStruct) {
        return JOGame.readInt(this, gameObjectStruct.value);
    }
    
    public long getPointerAsLong(long offset) {
        return JOGame.intToLong(JOGame.readInt(this, offset));
    }
    
    public JOPointer getPointer(long offset) {
        return JOGame.readPointer(this, offset);
    }

    public JOPointer getPointer(long offset, JOPointer dest) {
        return JOGame.readPointer(this, offset, dest);
    }
        
     public String getString(long offset) {
        return JOGame.readStringZ(this, offset, 255);
    }
     
    public float getFloat(long offset) {
        return JOGame.readFloat(this, offset);
    }
    
    public Vector3f getVector3f(long offset, Vector3f store) {
        return JOGame.readObjectPosition(this, offset, store);
    }
    
    public void setVector3f(long offset, Vector3f value) {
        JOGame.writeObjectPosition(this, offset, value);
    }
    
    public byte getByte(long offset) {
        return (byte) JOGame.readByte(this, offset);
    }
    
    public void setByte(long offset, byte val) {
        JOGame.writeByte(this, offset, val);
    }

    public void setInt(long offset, int val) {
        JOGame.writeInt(this, offset, val);
    }
    
    public void setInt(JOAdd.GameObjectStruct gameObjectStruct, int value) {
        JOGame.writeInt(this, gameObjectStruct.value, value);
    }
    
    public void setInt(JOAdd.BotDataStruct botData, int value) {
        JOGame.writeInt(this, botData.value, value);
    }
    
    public void setShort(JOAdd.BotDataStruct botData, short value) {
        JOGame.writeShort(this, botData.value, value);
    }

    public void setShort(JOAdd.GameObjectStruct gameObjectStruct, short value) {
        JOGame.writeShort(this, gameObjectStruct.value, value);
    }
        
    public short getShort(JOAdd.GameObjectStruct gameObjectStruct) {
        return JOGame.readShort(this, gameObjectStruct.value);
    }

    public void setByte(JOAdd.GameObjectStruct gameObjectStruct, byte value) {
        JOGame.writeByte(this, gameObjectStruct.value, value);
    }

}
