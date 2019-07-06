/*
 * DemoRenderer.java
 *
 * Created on 2006. február 26., 11:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package demoviewer.render;

/**
 *
 * @author vear
 */
public class DemoRenderer extends com.jme.renderer.lwjgl.LWJGLRenderer {
    
    /** Creates a new instance of DemoRenderer */
    public DemoRenderer(int width, int height) {
        super(width, height);
    }
    
    /**
     * <code>draw</code> renders a <code>ShaderedCompositeMesh</code> object
     * with all its materials/strips.
     * 
     * @see com.jme.renderer.Renderer#draw(com.jme.scene.CompositeMesh)
     * @param t
     *            the mesh to render.
     */
    /*
    public void draw(ShaderedCompositeMesh t) {
        CompositeMesh.IndexRange[] ranges = t.getIndexRanges();
        int[] rangematerials=t.getRangeMaterials();
        ShaderedMaterial[] materials=t.getMeshMaterials();
        
        boolean csEnabled=false;
        if(materials!=null) {
            // apply all attributes
            for(int i=0;i<materials.length;i++) {
                if(materials[i]!=null) {
                    materials[i].apply();
                    csEnabled=true;
                }
            }
        }
        if (statisticsOn) {
            int verts = t.getVertQuantity();
            numberOfVerts += verts;
            numberOfTris += t.getTriangleQuantity();
            numberOfMesh+=ranges.length;
        }

        if (t.getDisplayListID() != -1) {
            if ((t.getLocks() & Spatial.LOCKED_TRANSFORMS) == 0) {
                doTransforms(t);
                GL11.glCallList(t.getDisplayListID());
                postdrawGeometry(t);
            } else
                GL11.glCallList(t.getDisplayListID());
            return;
        }
        doTransforms(t);
        predrawGeometry(t);

        IntBuffer indices = t.getIndexBuffer().duplicate(); // returns secondary
                                                            // pointer to same
                                                            // data

        indices.position(0);
        int prevMaterial=-1;
        int newMaterial=-1;
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glLockArraysEXT(0, t.getVertQuantity());
        for (int i = 0; i < ranges.length; i++) {
            if(csEnabled) {
                 // get the new material
                newMaterial=rangematerials[i];
                // check the previous material
                if(prevMaterial!=newMaterial) {
                    // material change, use new material
                    if(materials[newMaterial]!=null) {
                        materials[newMaterial].use();
                    }
                    prevMaterial=newMaterial;
                }
            }
            int mode;
            switch (ranges[i].getKind()) {
            case CompositeMesh.IndexRange.TRIANGLES:
                mode = GL11.GL_TRIANGLES;
                break;
            case CompositeMesh.IndexRange.TRIANGLE_STRIP:
                mode = GL11.GL_TRIANGLE_STRIP;
                break;
            case CompositeMesh.IndexRange.TRIANGLE_FAN:
                mode = GL11.GL_TRIANGLE_FAN;
                break;
            case CompositeMesh.IndexRange.QUADS:
                mode = GL11.GL_QUADS;
                break;
            case CompositeMesh.IndexRange.QUAD_STRIP:
                mode = GL11.GL_QUAD_STRIP;
                break;
            default:
                throw new JmeException("Unknown index range type "
                        + ranges[i].getKind());
            }
            indices.limit(indices.position() + ranges[i].getCount());
            GL11.glDrawElements(mode, indices);
            indices.position(indices.limit());
        }
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glUnlockArraysEXT();
        indices.clear();

        postdrawGeometry(t);
        undoTransforms(t);
    }
    */
    /**
     * <code>draw</code> renders a <code>ShaderedCompositeMesh</code> object
     * but only the strips having specified material number set.
     * 
     * @see com.jme.renderer.Renderer#draw(com.jme.scene.CompositeMesh)
     * @param t
     *            the mesh to render.
     */
    /*
    public void draw(ShaderedCompositeMesh t, int material) {
        CompositeMesh.IndexRange[] ranges = t.getIndexRanges();
        int[] rangematerials=t.getRangeMaterials();
        
        if (statisticsOn) {
            int verts = 0;
            for (int i = 0; i < ranges.length; i++) {
                numberOfVerts += ranges[i].getCount();
                numberOfTris += ranges[i].getTriangleQuantityEquivalent();
                numberOfMesh++;
            }
        }

        if (t.getDisplayListID() != -1) {
            if ((t.getLocks() & Spatial.LOCKED_TRANSFORMS) == 0) {
                doTransforms(t);
                GL11.glCallList(t.getDisplayListID());
                postdrawGeometry(t);
            } else
                GL11.glCallList(t.getDisplayListID());
            return;
        }
        doTransforms(t);
        predrawGeometry(t);

        IntBuffer indices = t.getIndexBuffer().duplicate(); // returns secondary
                                                            // pointer to same
                                                            // data
        
        indices.position(0);
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glLockArraysEXT(0, t.getVertQuantity());
        for (int i = 0; i < ranges.length; i++) {
            
            int mode;
            switch (ranges[i].getKind()) {
            case CompositeMesh.IndexRange.TRIANGLES:
                mode = GL11.GL_TRIANGLES;
                break;
            case CompositeMesh.IndexRange.TRIANGLE_STRIP:
                mode = GL11.GL_TRIANGLE_STRIP;
                break;
            case CompositeMesh.IndexRange.TRIANGLE_FAN:
                mode = GL11.GL_TRIANGLE_FAN;
                break;
            case CompositeMesh.IndexRange.QUADS:
                mode = GL11.GL_QUADS;
                break;
            case CompositeMesh.IndexRange.QUAD_STRIP:
                mode = GL11.GL_QUAD_STRIP;
                break;
            default:
                throw new JmeException("Unknown index range type "
                        + ranges[i].getKind());
            }
            indices.limit(indices.position() + ranges[i].getCount());
            if(rangematerials[i]==material) {
                GL11.glDrawElements(mode, indices);
            }
            indices.position(indices.limit());
        }
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glUnlockArraysEXT();
        indices.clear();

        postdrawGeometry(t);
        undoTransforms(t);
    }
    */
    /*
    public void draw(MorphingTerrainBlock t, int stripnumber) {
        CompositeMesh.IndexRange[] ranges = t.getIndexRanges();
        t.setActiveBatch(0);
        if (statisticsOn) {
            int verts = ranges[stripnumber].getCount();
            numberOfVerts += verts;
            numberOfTris += ranges[stripnumber].getTriangleQuantityEquivalent();
            numberOfMesh+=1;
        }
        if (t.getDisplayListID() != -1) {
            if ((t.getLocks() & Spatial.LOCKED_TRANSFORMS) == 0) {
                doTransforms(t);
                GL11.glCallList(t.getDisplayListID());
                postdrawGeometry(t);
            } else
                GL11.glCallList(t.getDisplayListID());
            return;
        }
        
        doTransforms(t);
        predrawGeometry(t);

        IntBuffer indices = t.getIndexBuffer().duplicate(); // returns secondary
                                                            // pointer to same
                                                            // data
        
        indices.position(0);
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glLockArraysEXT(0, ranges[stripnumber].getCount());//t.getVertQuantity());//
        for (int i = 0; i <= stripnumber; i++) {
            if(i==stripnumber) {
                int mode;
                switch (ranges[i].getKind()) {
                case CompositeMesh.IndexRange.TRIANGLES:
                    mode = GL11.GL_TRIANGLES;
                    break;
                case CompositeMesh.IndexRange.TRIANGLE_STRIP:
                    mode = GL11.GL_TRIANGLE_STRIP;
                    break;
                case CompositeMesh.IndexRange.TRIANGLE_FAN:
                    mode = GL11.GL_TRIANGLE_FAN;
                    break;
                case CompositeMesh.IndexRange.QUADS:
                    mode = GL11.GL_QUADS;
                    break;
                case CompositeMesh.IndexRange.QUAD_STRIP:
                    mode = GL11.GL_QUAD_STRIP;
                    break;
                default:
                    throw new JmeException("Unknown index range type "
                            + ranges[i].getKind());
                }
                indices.limit(indices.position() + ranges[i].getCount());
                // if this is the strip we want
                GL11.glDrawElements(mode, indices);
            } else {
                indices.position(indices.position() + ranges[i].getCount());
            }
        }
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glUnlockArraysEXT();
        indices.clear();

        postdrawGeometry(t);
        undoTransforms(t);
    }
    
    public void draw(MorphingTerrainBlock t) {
        CompositeMesh.IndexRange[] ranges = t.getIndexRanges();
        int stripnumber=0;
        if (statisticsOn) {
            int verts = ranges[stripnumber].getCount();
            numberOfVerts += verts;
            numberOfTris += ranges[stripnumber].getTriangleQuantityEquivalent();
            numberOfMesh+=1;
        }
        if (t.getDisplayListID() != -1) {
            if ((t.getLocks() & Spatial.LOCKED_TRANSFORMS) == 0) {
                doTransforms(t);
                GL11.glCallList(t.getDisplayListID());
                postdrawGeometry(t);
            } else
                GL11.glCallList(t.getDisplayListID());
            return;
        }
        
        doTransforms(t);
        predrawGeometry(t);

        IntBuffer indices = t.getIndexBuffer().duplicate(); // returns secondary
                                                            // pointer to same
                                                            // data
        
        indices.position(0);
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glLockArraysEXT(0, t.getVertQuantity());//);//ranges[stripnumber].getCount()
        for (int i = 0; i <= stripnumber; i++) {
            if(i==stripnumber) {
                int mode;
                switch (ranges[i].getKind()) {
                case CompositeMesh.IndexRange.TRIANGLES:
                    mode = GL11.GL_TRIANGLES;
                    break;
                case CompositeMesh.IndexRange.TRIANGLE_STRIP:
                    mode = GL11.GL_TRIANGLE_STRIP;
                    break;
                case CompositeMesh.IndexRange.TRIANGLE_FAN:
                    mode = GL11.GL_TRIANGLE_FAN;
                    break;
                case CompositeMesh.IndexRange.QUADS:
                    mode = GL11.GL_QUADS;
                    break;
                case CompositeMesh.IndexRange.QUAD_STRIP:
                    mode = GL11.GL_QUAD_STRIP;
                    break;
                default:
                    throw new JmeException("Unknown index range type "
                            + ranges[i].getKind());
                }
                indices.limit(indices.position() + ranges[i].getCount());
                // if this is the strip we want
                GL11.glDrawElements(mode, indices);
            } else {
                indices.position(indices.position() + ranges[i].getCount());
            }
        }
        if (capabilities.GL_EXT_compiled_vertex_array)
            EXTCompiledVertexArray.glUnlockArraysEXT();
        indices.clear();

        postdrawGeometry(t);
        undoTransforms(t);
    }
     */
}
