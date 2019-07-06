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

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import java.util.logging.Logger;
import jb2.math.Vector3f;
import jb2.util.Context;
import jb2.util.LocalContext;

/**
 * Does the communication with JO game
 * @author vear
 */
public final class JOGame {
    
    protected static final Logger log = Logger.getLogger(JOGame.class.getName());
    public static WinNT.HANDLE handle;
    
    // the coordinte multiplier value
    public static float coordMul;
    
    public static boolean openProcessByWindowName() {
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, JOAdd.windowName);
        if(hwnd==null)
            return false;
        IntByReference lpdwProcessId = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(hwnd, lpdwProcessId);
        if(lpdwProcessId.getValue()==0)
            return false;
        handle = Kernel32.INSTANCE.OpenProcess(Kernel32.PROCESS_ALL_ACCESS, false, lpdwProcessId.getValue());
        if(handle == null)
            return false;
        return true;
    }
    
    public static void closeProcess() {
        Kernel32.INSTANCE.CloseHandle(handle);
    }
    
    public static String readStringZ(JOPointer ptr, long offset, int max) {
        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        // get address of buffer into dest
        // read 128 bytes at once
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, max, ctx.readBytes);
        // TODO: if max != ctx.readBytes, then failed, set to exit
        if(max != ctx.readBytes.getValue()) {
            String msg ="Failed reading "+max+" bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
        // null-terminate the buffer, just in case
        ctx.destBuffer.setByte(max, (byte)0);
        
        return ctx.destBufferPointer.getString(0);
    }
    
    public static byte readByte(JOPointer ptr, long offset) {
        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 2, ctx.readBytes);
         if(2 != ctx.readBytes.getValue()) {
            String msg ="Failed reading 2 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
        return ctx.destBufferPointer.getByte(0);
    }
    
    public static int readInt(long ptr, long offset) {
        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr + offset);
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 4, ctx.readBytes);
         if(4 != ctx.readBytes.getValue()) {
            String msg ="Failed reading 4 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
         return ctx.destBufferPointer.getInt(0);
    }
    
    public static int readInt(JOPointer ptr, long offset) {
        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 4, ctx.readBytes);
         if(4 != ctx.readBytes.getValue()) {
            String msg ="Failed reading 4 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
         /*
        return ctx.destBufferPointer.getByte(0) 
               + ctx.destBufferPointer.getByte(1)<<8 
               + ctx.destBufferPointer.getByte(2)<<16
               + ctx.destBufferPointer.getByte(3)<<24;
             */
         return ctx.destBufferPointer.getInt(0);
    }
    
    public static short readShort(JOPointer ptr, long offset) {
        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 2, ctx.readBytes);
         if(2 != ctx.readBytes.getValue()) {
            String msg ="Failed reading 2 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
         return ctx.destBufferPointer.getShort(0);
    }
    
    public static float readFloat(JOPointer ptr, long offset) {
        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 4, ctx.readBytes);
         if(4 != ctx.readBytes.getValue()) {
            String msg ="Failed reading 4 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
        return ctx.destBufferPointer.getFloat(0);
    }
    
    public static JOPointer readPointer(JOPointer ptr, long offset) {
        return readPointer(ptr,offset,null);
    }
    
    public static JOPointer readPointer(JOPointer ptr, long offset, JOPointer dest) {
        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 4, ctx.readBytes);
         if(4 != ctx.readBytes.getValue()) {
            String msg ="Failed reading 4 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
        long addr = intToLong(ctx.destBufferPointer.getInt(0));
        if(dest==null)
            dest = new JOPointer(addr);
        else
            dest.set(addr);
        return dest;
    }
    
    public static final long intToLong(int value) {
        return ((long)value)&0xffffffff;
    }
    
    public static float readCoordMul() {
        coordMul = readFloat(JOAdd.Adress.Coordmul.value,0);
        return coordMul;
    }
    
    public static Vector3f readObjectPosition(JOPointer ptr, long offset, Vector3f store) {
        if(coordMul==0)
            readCoordMul();

        Context ctx = LocalContext.getContext();
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.ReadProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 12, ctx.readBytes);
         if(12 != ctx.readBytes.getValue()) {
            String msg ="Failed reading 12 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
        store.z = (float)(ctx.destBufferPointer.getInt(0)) * coordMul;
        store.x = (float)(-ctx.destBufferPointer.getInt(4)) * coordMul;
        store.y = (float)(ctx.destBufferPointer.getInt(8)) * coordMul;
        return store;
    }
    
    public static void writeObjectPosition(JOPointer ptr, long offset, Vector3f source) {
        if(coordMul==0)
            readCoordMul();

        Context ctx = LocalContext.getContext();
        ctx.destBufferPointer.setInt(0, (int)(source.z / coordMul));
        ctx.destBufferPointer.setInt(4, -(int)(source.x / coordMul));
        ctx.destBufferPointer.setInt(8, (int)(source.y / coordMul));
        
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.WriteProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 12, ctx.readBytes);
         if(12 != ctx.readBytes.getValue()) {
            String msg ="Failed writing 12 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
    }
    
    public static void writeByte(JOPointer ptr, long offset, byte value) {
        Context ctx = LocalContext.getContext();
        ctx.destBufferPointer.setByte(0, value);
        
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.WriteProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 1, ctx.readBytes);
         if(1 != ctx.readBytes.getValue()) {
            String msg ="Failed writing 1 byte"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
    }
    
    public static void writeInt(JOPointer ptr, long offset, int value) {
        Context ctx = LocalContext.getContext();
        ctx.destBufferPointer.setInt(0, value);
        
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.WriteProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 4, ctx.readBytes);
         if(4 != ctx.readBytes.getValue()) {
            String msg ="Failed writing 4 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
    }
    
    public static void writeShort(JOPointer ptr, long offset, short value) {
        Context ctx = LocalContext.getContext();
        ctx.destBufferPointer.setShort(0, value);
        
        Pointer.nativeValue(ctx.tmpPointer, ptr.get() + offset);
        Kernel32.INSTANCE.WriteProcessMemory(handle, ctx.tmpPointer, ctx.destBufferPointer, 2, ctx.readBytes);
         if(2 != ctx.readBytes.getValue()) {
            String msg ="Failed writing 2 bytes"; 
            log.warning(msg);
            throw new FailedMemoryAccess(msg);
        }
    }
    
    public static int getTypeId(JOPointer ssnptr, JOPointer targettypebase) {
        if(targettypebase==null)
            return getTypeId(ssnptr);
        
        // read the typebase
        long typeaddr = ssnptr.getPointerAsLong(JOAdd.GameObjectStruct.TypeBasePtr.value);
        if(typeaddr!=0) {
            targettypebase.set(typeaddr);
            return targettypebase.getInt(JOAdd.TypeStruct.Typeid.value);
        }
        targettypebase.set(0);
        return 0;
    }
    
    public static int getTypeId(JOPointer ssnptr) {
        // read the typebase
        long typeaddr = ssnptr.getPointerAsLong(JOAdd.GameObjectStruct.TypeBasePtr.value);
        if(typeaddr!=0) {
            JOPointer typeBase = new JOPointer(typeaddr);
            return typeBase.getInt(JOAdd.TypeStruct.Typeid.value);
        }
        return 0;
    }
}
