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

package demoviewer.test;

import com.jme.app.SimpleGame;
import com.jme.bounding.BoundingSphere;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.Text;
import com.jme.scene.TriMesh;
import com.jme.scene.shape.Box;
import com.jme.scene.state.CullState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

/**
 * <code>TestMaxTextureUnits</code>
 * @author Arpad Vekas
 */
public class TestMaxTextureUnits extends SimpleGame {
  private TriMesh t;
  private Quaternion rotQuat;
  private float angle = 0;
  private Vector3f axis;
  private TextureState[] fixedTexStates;
  private TextureState shaderTexState;
  int numFixedTexUnit;
  int numShaderOnlyTexUnits;
  int numShaderTexUnits;
  GLSLShaderObjectsState shader0;
  GLSLShaderObjectsState shader1;
  GLSLShaderObjectsState shader2;
  Texture []tx;
  
  /**
   * Entry point for the test,
   * @param args
   */
  public static void main(String[] args) {
    TestMaxTextureUnits app = new TestMaxTextureUnits();
    app.setDialogBehaviour(ALWAYS_SHOW_PROPS_DIALOG);
    app.start();
  }

  /**
   * @see com.jme.app.SimpleGame#update
   */
  protected void simpleUpdate() {
    if (timer.getTimePerFrame() < 1) {
      angle = angle + (timer.getTimePerFrame() * 25);
      if (angle > 360) {
        angle = 0;
      }
    }

    rotQuat.fromAngleAxis(angle*FastMath.DEG_TO_RAD, axis);
    t.setLocalRotation(rotQuat);
  }

  /**
   * builds the trimesh.
   * @see com.jme.app.SimpleGame#initGame()
   */
  protected void simpleInitGame() {

    rotQuat = new Quaternion();
    axis = new Vector3f(1, 1, 0.5f);

    display.setTitle("Max Texture Units Test");
    cam.setLocation(new Vector3f(0, 0, 60));
    cam.update();

    Vector3f max = new Vector3f(2, 2, 2);
    Vector3f min = new Vector3f( -2, -2, -2);
    
    t = new Box("Box", min, max);
    CullState cs=display.getRenderer().createCullState();
    cs.setCullMode(CullState.CS_BACK);
    t.setRenderState(cs);
    
    // call init once
    display.getRenderer().createTextureState();
    tx=new Texture[10];
    for(int i=0;i<10;i++) {
        tx[i] = TextureManager.loadTexture(
            TestMaxTextureUnits.class.getClassLoader().getResource(
            "jmetest/data/images/number"+i+".png"),
            Texture.MM_LINEAR,
            Texture.FM_LINEAR);
        tx[i].setWrap(Texture.WM_CLAMP_S_CLAMP_T);
    }
    numFixedTexUnit=TextureState.getNumberOfFixedUnits();
    fixedTexStates=new TextureState[numFixedTexUnit];
    for(int i=0;i<numFixedTexUnit;i++) {
        fixedTexStates[i]=display.getRenderer().createTextureState();
        fixedTexStates[i].setEnabled(true);
        fixedTexStates[i].setTexture(tx[i], i);
        if(i>0)
            t.getBatch(0).copyTextureCoordinates(0,i,1.0f);
    }
    numShaderTexUnits=TextureState.getNumberOfFragmentUnits();
    numShaderOnlyTexUnits=Math.max(numShaderTexUnits-numFixedTexUnit,0);
    if(numShaderOnlyTexUnits>0) {
        shaderTexState=display.getRenderer().createTextureState();
        shaderTexState.setEnabled(true);
        for(int i=0;i<numShaderTexUnits;i++) {
            shaderTexState.setTexture(tx[i%10], i);
        }
        shader0=createShader(0,0,false);
        if(numShaderTexUnits>9) {
            shader1=createShader(0,1,false);
        }
    }
    shader2=createShader(0/*numFixedTexUnit*/, numShaderTexUnits-1,true);
    for(int i=0/*numFixedTexUnit*/;i<numShaderTexUnits;i++) {
        shader2.setUniform("tex"+i+"map",i);
    }
    System.out.println("This video card has " + TextureState.getNumberOfFixedUnits() +" fixed functionality texture units.");
    System.out.println("This video card has " + numShaderTexUnits +" fragment shader texture units.");
    System.out.println("This video card has " + TextureState.getNumberOfVertexUnits() +" vertex shader texture units.");
    

  }
  
  public void simpleRender() {
      Renderer r=display.getRenderer();
      // draw fixed functionality textured boxes
      
      t.setRenderState(Renderer.defaultStateList[RenderState.RS_GLSL_SHADER_OBJECTS]);
      
      for(int i=0;i<numFixedTexUnit;i++) {
          //fixedTexStates[i].apply();
          t.setRenderState(fixedTexStates[i]);
          t.updateRenderState();
          t.setLocalTranslation(new Vector3f((i-numFixedTexUnit/2f)*6f, 10, 0));
          t.onDraw(r);
      }
      
      if(numShaderOnlyTexUnits>0) {
          // draw boxes with shader
          t.setRenderState(shaderTexState);
          t.updateRenderState();
          for(int i=numFixedTexUnit;i<numShaderTexUnits;i++) {
              
              GLSLShaderObjectsState shader;
              if(i>9) {
                  shader=shader1;
                  shader.setUniform("tex1map", i/10);
              } else shader=shader0;
              shader.setUniform("tex0map", i);
              //t.setRenderState(shader);
              shader.apply();
              
              t.setLocalTranslation(new Vector3f(((i-numFixedTexUnit)-numShaderOnlyTexUnits/2f)*6f, -10, 0));
              t.onDraw(r);
              
              // draw a box with all the texture units
              t.setRenderState(shader2);
              t.updateRenderState();
              shader2.apply();
              t.setLocalTranslation(new Vector3f(0,0,0));
              t.onDraw(r);
          }
          
          
          Renderer.defaultStateList[RenderState.RS_GLSL_SHADER_OBJECTS].apply();
      }
      
  }
  
  public GLSLShaderObjectsState createShader(int numStart, int numEnd, boolean order) {
      GLSLShaderObjectsState shader = display.getRenderer()
                    .createGLSLShaderObjectsState();
      String vert="varying vec2 Texcoord; void main(void) { Texcoord    = gl_MultiTexCoord0.xy; gl_Position = ftransform(); }";
      String frag="varying vec2 Texcoord;\n";
      for(int i=numStart;i<=numEnd;i++) {
        frag+="uniform sampler2D tex"+i+"map;\n";
      }
      frag+="void main( void ) { \n";
      frag+="vec4 clr;\n";
      for(int i=numStart;i<=numEnd;i++) {
          if(!order) {
              if(i%2==0)
                frag+="clr+= texture2D( tex"+i+"map, Texcoord.xy );\n";
              else
                frag+="clr+= texture2D( tex"+i+"map, Texcoord.yx );\n";
          } else {
              int cells=(int)FastMath.sqrt(numEnd-numStart+1);
              int x=(i-numStart)%cells;
              int y=(i-numStart)/cells;
              frag+="clr+= texture2D( tex"+i+"map, " +
                      "vec2(Texcoord.x/"+cells+".0 + "+x+".0/"+cells+".0," +
                           "Texcoord.y/"+cells+".0 + "+y+".0/"+cells+".0));\n";
          }
      }
      if(!order) {
         frag+="gl_FragColor = clr/"+(numEnd-numStart+1)+".0;\n";
      } else {
         frag+="gl_FragColor = clr;\n";
      }
      frag+="}\n";
      shader.load(vert, frag);
      shader.setEnabled(true);
      return shader;
  }
}
