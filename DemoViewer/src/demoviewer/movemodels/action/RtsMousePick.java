/*
 * Copyright (c) 2003-2006 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package demoviewer.movemodels.action;

import com.jme.input.AbsoluteMouse;
import com.jme.input.MouseInput;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.MouseInputAction;
import com.jme.intersection.BoundingPickResults;
import com.jme.intersection.PickData;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Ray;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.CameraNode;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.SceneElement;
import com.jme.scene.TriMesh;
import com.jme.system.DisplaySystem;
import demoviewer.movemodels.RtsInputHandler;
import java.nio.FloatBuffer;
import java.util.ArrayList;

/**
 * <code>MousePick</code>
 * @author Mark Powell
 * @version
 */
public class RtsMousePick extends MouseInputAction {

    private AbsoluteMouse am;
    private CameraNode camera;
    private Node scene;
    private float shotTime = 0;
    private int hits = 0;
    private int shots = 0;
    private String hitItems;
    private Vector3f tloc;
    private Quaternion trot;
    private float clicktime;
    RtsInputHandler handler;
    private static ArrayList tris=new ArrayList();
    
    public RtsMousePick(RtsInputHandler handler, Node scene, AbsoluteMouse am, Vector3f tloc, Quaternion trot) {
        this.handler=handler;
        this.scene = scene;
        this.am=am;
        this.tloc=tloc;
        this.trot=trot;
    }
    
    private void cameraClick() {
        camera = handler.getCameraNode();
        Vector2f screenPos = new Vector2f();
        // Get the position that the mouse is pointing to
        screenPos.set(am.getHotSpotPosition().x, am.getHotSpotPosition().y);
        // Get the world location of that X,Y value
        DisplaySystem display=DisplaySystem.getDisplaySystem();
        Vector3f worldCoords = display.getWorldCoordinates(screenPos, 1.0f);
        // Create a ray starting from the camera, and going in the direction
        // of the mouse's location
        Ray ray = new Ray(camera.getCamera().getLocation(), worldCoords
                        .subtractLocal(camera.getCamera().getLocation()));
        //Ray ray = new Ray(camera.getCamera().getLocation(), camera.getCamera().getDirection());
        PickResults results = new BoundingPickResults();
        results.setCheckDistance(true);
        scene.findPick(ray,results);


        hits += results.getNumber();
        hitItems = "";
        float mindist=Float.POSITIVE_INFINITY;
        PickData hit=null;
        Vector3f tt=null;
        boolean []bad=new boolean[results.getNumber()];
        Vector3f ct=camera.getCamera().getLocation();
        int numtocheck=results.getNumber();
        SceneElement g=null;
        
        while(numtocheck > 0 && tt==null) {
            int idx=-1;
            for(int i = 0; i < results.getNumber(); i++) {
                PickData rs=results.getPickData(i);
                if( !bad[i] && ( hit==null || rs.getDistance()<mindist ) ) {
                    hit=rs;
                    mindist=rs.getDistance();
                    idx=i;
                }
            }
            if(hit!=null) {
                // mark the hit as to be not checked again
                bad[idx]=true;
                // check the hit
                g=hit.getTargetMesh();
                Vector3f ht=null;
                if(g instanceof TriMesh) {
                    // okay we were waiting for this
                    //SectoredTerrainBlock t=(SectoredTerrainBlock)g;
                    TriMesh t=(TriMesh)g;
                    ht=t.getWorldTranslation();
                    tt=t.getWorldTranslation();
                    // get triangle accurate pick
                    tris.clear();
                    t.findTrianglePick(ray, tris,0);
                    //tris = hit.getTargetTris();

                    // find the closest triangle
                    int[] indices = new int[3];
                    
                    // get the rotation to calculate 
                    //Quaternion q=t.getLocalRotation();
                    FloatBuffer buff = t.getBatch(0).getVertexBuffer();
                    //buff.clear();
                    float [] vrt=new float[3];
                    Vector3f avrt=new Vector3f();
                    mindist=Float.POSITIVE_INFINITY;
                    for (int i = 0; i < tris.size(); i++) {
                        int triIndex = ((Integer) tris.get(i)).intValue();
                        t.getTriangle(triIndex, indices);
                        buff.position(indices[0]);
                        buff.get(vrt);
                        avrt.x+=vrt[0];avrt.y+=vrt[1];avrt.z+=vrt[2];
                        buff.position(indices[1]);
                        buff.get(vrt);
                        avrt.x+=vrt[0];avrt.y+=vrt[1];avrt.z+=vrt[2];
                        buff.position(indices[2]);
                        buff.get(vrt);
                        avrt.x+=vrt[0];avrt.y+=vrt[1];avrt.z+=vrt[2];
                        // calculate the hit position as the average
                        // of three vertices
                        avrt.divideLocal(3);
                        // calculate the rotated position of the point
                        //q.multLocal(avrt);
                        // this is the position of the triangle
                        avrt.addLocal(ht);
                        // check if this is the closest one
                        float td=avrt.distance(ct);
                        if(td<mindist) {
                            tt=new Vector3f(avrt);
                            mindist=td;
                        }
                    }
                    /*
                } else if(g instanceof ShaderedCompositeMesh) {
                    ShaderedCompositeMesh t=(ShaderedCompositeMesh)g;
                    */
                }
            }
            numtocheck--;
        }

        results.clear();
        tris.clear();
        if(tt!=null) {
            shots++;
            System.out.println("Hits: " + hits + " Shots: " + shots + " : " + g.getName() + " : " + mindist);
            // the target is: maintain current height, and look at the target
            // at 45 angle
            float ch=handler.getTerrain().getHeightFromWorld(ct);
            float th=handler.getTerrain().getHeightFromWorld(tt);
            if( !Float.isNaN(ch) && !Float.isNaN(th)) {//ct.y<tt.y&&
                tloc.y=th+ct.y-ch;
            } else {
                tloc.y=ct.y;
            }
            float hd=(tloc.y-tt.y)/2f;
            tloc.x=tt.x+Math.signum(ct.x-tt.x)*(ct.y-ch);
            tloc.z=tt.z+Math.signum(ct.z-tt.z)*(ct.y-ch);
            if(tt.distance(ct)<tt.distance(tloc)) {
                tloc.set(ct).multLocal(1f/5f).addLocal(tt.mult(4f/5f));
            }
            // set rotation:
            // 45 angle down
            // theta is current position to target
            trot.lookAt(tt.subtract(tloc), new Vector3f(0,1,0));
            float[] ang=new float[3];
            trot.toAngles(ang);
            // set omega to 0
            ang[2]=0;
            // set phi to most 45 down
            //ang[0]=Math.max(Math.min(ang[0],FastMath.PI/3f),-FastMath.PI/3);
            trot.fromAngles(ang);
            handler.getMoveModel().setAutocamera(true);
        }
    }
    
    /*
     * This is used for camera free-look,
     * but is handled in FpsNodeMouseLook
     */
    private void cameraMouseDrag(InputActionEvent evt)
    {
       // first, stop movement
        camera = handler.getCameraNode();
        trot.set(camera.getLocalRotation());
        tloc.set(camera.getLocalTranslation());
        handler.getMoveModel().setAutocamera(false);
    }
    
    /* (non-Javadoc)
     * @see com.jme.input.action.MouseInputAction#performAction(float)
     */
    public void performAction(InputActionEvent evt) {
        shotTime += evt.getTime();
        
        // check for camera click
        boolean clicked=false;
        if(MouseInput.get().isButtonDown(1)) {
            clicktime+=evt.getTime();
        } else {
            if(clicktime>0 && clicktime<0.1f) clicked=true;
            clicktime=0;
        }
        if( clicked ) {
            cameraClick();
        }
        if((clicktime>0.1f)&&(MouseInput.get().isButtonDown(1))) {
            // we have a cameramousedrag
            cameraMouseDrag(evt);
        }
    }
}
