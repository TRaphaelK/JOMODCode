uniform vec4 fvAmbient;
uniform vec4 fvSpecular;
uniform vec4 fvDiffuse;
uniform float fSpecularPower;

uniform sampler2D basicTexture;
#ifdef TEX2
uniform sampler2D basicTexture2;
#endif

varying vec2 Texcoord;
varying vec3 ViewDirection;
varying vec3 LightDirection;
varying vec3 Normal;

void main( void )
{
   vec3  fvLightDirection = normalize( LightDirection );
   vec3  fvNormal         = normalize( Normal );
   float fNDotL           = dot( fvNormal, fvLightDirection ); 
   
   vec3  fvReflection     = normalize( ( ( 2.0 * fvNormal ) * fNDotL ) - fvLightDirection ); 
   vec3  fvViewDirection  = normalize( ViewDirection );
   float fRDotV           = max( 0.0, dot( fvReflection, fvViewDirection ) );
   
   vec4  fvBaseColor      = texture2D( basicTexture, Texcoord );
#ifdef TEX2
    fvBaseColor      *= texture2D( basicTexture2, Texcoord );
#endif
   vec4  fvTotalAmbient   = fvAmbient * fvBaseColor; 
   vec4  fvTotalDiffuse   = fvDiffuse * fNDotL * fvBaseColor; 
   vec4  fvTotalSpecular  = fvSpecular * ( pow( fRDotV, fSpecularPower ) );
  
   gl_FragColor = ( fvTotalAmbient + fvTotalDiffuse + fvTotalSpecular );
       
}