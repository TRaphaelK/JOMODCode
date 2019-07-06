

// external defines:
// MORPH            morph the terrain using pregenerated morph arrays
// MAPNORMAL        use a normalmap instead of vertex normals
// ADDNORMALS        average together vertex and map normals
// COLORMAP|MAPNORMAL|DETAILBLENDMAP|TEXCOORDS use texture coordinates 0
// FOG              enable fog

#ifdef MORPH
uniform float MiddleLod;
uniform vec4 BorderLod;
#endif

#ifdef MORPHUSECOMPILEDARRAY
#define Morph gl_Color.r
#define IdxMorph gl_Color.g
#define LvlMorph gl_Color.b
#else
attribute float Morph;
attribute float LvlMorph;
attribute float IdxMorph;
#endif

#ifdef MAPNORMAL
varying vec3 lightDir;
#endif

#ifdef ADDNORMALS
varying vec3 Nrmal;
#endif

#ifdef DETAILDISTANCE
#define DEPZ
#endif

#ifdef FOG
#define DEPZ
#endif

#ifdef DEPZ
varying float Depth;
#endif

// do we need texture coordinates
#ifdef COLORMAP
#define TEXCOORDS
#endif

#ifdef DETAILBLENDMAP
#define TEXCOORDS
#endif

#ifdef MAPNORMAL
#define TEXCOORDS
#endif

#ifdef TEXCOORDS
varying vec2 Texcoord;
#endif

void main() {

#ifdef ADDNORMALS
#define CALCNORMAL
#endif

#ifdef VTXNORMAL
// light calculated in vertex shader
    vec3 Nrmal, lightDir;
#define CALCNORMAL
#define CALCLIGHT
#endif

#ifdef MAPNORMAL
#define CALCLIGHT
#endif

#ifdef CALCLIGHT
lightDir = normalize(vec3(gl_LightSource[0].position));
#endif

// do we calculate vertex normal here?
#ifdef CALCNORMAL
Nrmal = normalize(gl_NormalMatrix * gl_Normal);
#endif

// do we calculate diffuse in vertex?
#ifdef VTXNORMAL
      float NdotL = max(dot(Nrmal, lightDir), 0.0);
// diffuse
      vec4 diffuse = gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse * NdotL;
#endif

// ambient, globalambient
    vec4 ambient = gl_FrontMaterial.ambient * gl_LightSource[0].ambient;
    vec4 globalAmbient = gl_LightModel.ambient * gl_FrontMaterial.ambient;

    
// the vertex shading
#ifdef MAPNORMAL
    gl_FrontColor =  globalAmbient + ambient;
#endif

#ifdef VTXNORMAL
    gl_FrontColor =  diffuse + globalAmbient + ambient;
#endif

// texture coordinates
#ifdef TEXCOORDS
Texcoord    = gl_MultiTexCoord0.xy;
#endif

// do we implement morphing
#ifndef MORPH
// no morphing
gl_Position     = ftransform();
#define VTXPOS  gl_Vertex
#else
// morphing
      vec4 fvObjectPosition =  gl_Vertex;

      float lodlevel;
      float midx=IdxMorph;
      /*
      // determine which lod to use for the vertex
      if(midx<1.0) {
        // most common, the center
        lodlevel=MiddleLod;
      } else if(midx<2.0) {
        // get one of the borders lod values
        lodlevel=BorderLod[0];
      } else if(midx<3.0) {
        // get one of the borders lod values
        lodlevel=BorderLod[1];
      } else if(midx<4.0) {
        // get one of the borders lod values
        lodlevel=BorderLod[2];
      } else {
        lodlevel=BorderLod[3];
      }
      */

      lodlevel=step(4.0, midx)*BorderLod[3];
      lodlevel+=step(midx, 3.0)*BorderLod[2]*step(3.0, midx);
      lodlevel+=step(midx, 2.0)*BorderLod[1]*step(2.0, midx);
      lodlevel+=step(midx, 1.0)*BorderLod[0]*step(1.0, midx);
      lodlevel+=(1.0-step(1.0, midx))*MiddleLod;

    // if vertex's lod level between current lod -1 and lod, do the morphing
    // above 1, only morphed value counts, this should never happen,
    // because the program would not send those vertices anyway

    float morph=clamp(lodlevel-LvlMorph,0.0,1.0);
    // between 0 and 1 do the morphing (lerp between original, and position in lower detail level position)
    fvObjectPosition.y=mix(fvObjectPosition.y, Morph, morph);
    // transform into view
    gl_Position = gl_ModelViewProjectionMatrix * fvObjectPosition;
#define VTXPOS  fvObjectPosition
#endif

#ifdef DEPZ
// fog calculation
    vec4 ecPosition = gl_ModelViewMatrix * VTXPOS;
//gl_FogFragCoord = 
    Depth=abs(ecPosition.z);
#endif

//#ifdef DETAILDISTANCE
//Depth=gl_Position.z;
//#endif

}

