

// external defines:
// COLORMAP         use a colormap texture in baseMap texunit
// COLORMAPTILE     (optional) tile factor for base colormap
// DETAILBLENDMAP   use and RGBA detail blend map in blend texunit
// DETAIL1          use the R channel with colr1 texunit
// DETAIL2          use the G channel with colr2 texunit
// DETAIL3          use the B channel with colr3 texunit
// DETAIL4          use the A channel with colr4 texunit
// DETAILTILE       tile factor for detail textures
// MAPNORMAL2,3        use a normalmap in normalMap texunit with 2 or 3 vector components
// ADDNORMAL        average together vertex and map normals
// COLORRATIO       ratio of colormap to detail color (0 to 1)

#ifdef COLORMAP
uniform sampler2D baseMap;
#endif

#ifdef DETAILBLENDMAP
uniform sampler2D blend;
#endif

#ifdef DETAIL1
uniform sampler2D colr1;
#endif

#ifdef DETAIL2
uniform sampler2D colr2;
#endif

#ifdef DETAIL3
uniform sampler2D colr3;
#endif

#ifdef DETAIL4
uniform sampler2D colr4;
#endif

#ifdef MAPNORMAL
uniform sampler2D normalMap;
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

// start out with fragcolor
vec4 Color = gl_Color;

#ifdef MAPNORMAL3
    vec3 nrmt =  (2.0 * texture2D(normalMap, Texcoord) - 1.0).rgb;
    vec3 nrml = gl_NormalMatrix * nrmt;//normalize();
#endif

#ifdef MAPNORMAL2
// two channel normal map XZ components in RA
    vec3 nrmt = (2.0 * texture2D(normalMap, Texcoord) - 1.0).rga;
// x*x+z*z faster than dot?
    nrmt.y=sqrt(1.0-nrmt.x*nrmt.x-nrmt.z*nrmt.z);//dot(nrmt.xz,nrmt.xz));//
    vec3 nrml= gl_NormalMatrix * nrmt;
#endif

#ifdef ADDNORMALS
// average with vertex normal
    nrml = normalize((nrml+Nrmal)/2.0);
#endif

#ifdef MAPNORMAL
// light calculated in fragment
    float NdotL = max(dot(nrml, lightDir), 0.0);//(nrml.x*lightDir.x+nrml.y*lightDir.y+nrml.z*lightDir.z)
// diffuse
    vec4 diffuse = gl_FrontMaterial.diffuse * gl_LightSource[0].diffuse * NdotL;
    Color += diffuse;
#endif

//#ifdef COLORMAPTILE
//#define TXCO Texcoord * COLORMAPTILE
//#else
//#define TXCO Texcoord
//#endif

// colormap calculation
#ifdef COLORMAP
vec4  cl = texture2D(baseMap, Texcoord);
#endif

// detail blendmap calculation
#ifdef DETAILBLENDMAP
    // blendmap
    vec4 blend = texture2D(blend, Texcoord);
    // detail texture coords
    vec2 dtcoord=Texcoord * DETAILTILE;
    // sum of blended textures
    float sum;
    vec4 detail=vec4(0.0,0.0,0.0,0.0);
#define DETAILCOLORBLEND
#endif

#ifdef DETAIL1
    sum+=blend.r;
#endif
#ifdef DETAIL2
    sum+=blend.g;
#endif
#ifdef DETAIL3
    sum+=blend.b;
#endif
#ifdef DETAIL4
    sum+=blend.a;
#endif

#ifdef DETAIL1
    detail+=texture2D(colr1, dtcoord)*blend.r;
#endif
#ifdef DETAIL2
    detail+=texture2D(colr2, dtcoord)*blend.g;
#endif
#ifdef DETAIL3
    detail+=texture2D(colr3, dtcoord)*blend.b;
#endif
#ifdef DETAIL4
    detail+=texture2D(colr4, dtcoord)*blend.a;
#endif
#ifdef DETAILBLENDMAP
    detail/=max(sum,1.0);
#endif

#ifdef DETAILDISTANCE
    detail*=min(DETAILDISTANCE/Depth,1.0);
#endif

#ifdef DETAILBLENDPATTERN
    detail*=(1.0/COLORRATIO);
    detail+=cl;
#undef DETAILCOLORBLEND
#endif

#ifdef DETAILCOLORBLEND
    detail*=(1.0-COLORRATIO);
    detail+=cl*COLORRATIO;
#endif

// final color calculation

#ifdef COLORMAP
#define TEXCOLOR cl
#endif

#ifdef DETAILBLENDMAP
#define TEXCOLOR detail
#endif

#ifdef TEXCOLOR
Color *= TEXCOLOR;
#endif

#ifdef FOG
    float fog;
    fog = (gl_Fog.end - Depth) / (gl_Fog.end - gl_Fog.start);
    fog = clamp(fog, 0.0, 1.0);
    Color.rgb = mix( gl_Fog.color.rgb, Color.rgb, fog);
#endif

gl_FragColor = Color;

}


