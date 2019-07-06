uniform vec4 fvAmbient;
uniform vec4 fvSpecular;
uniform vec4 fvDiffuse;
uniform float fSpecularPower;

// texture map
uniform sampler2D basicTexture;
// normal map
uniform sampler2D tex1map;

varying vec2 Texcoord0;
varying vec2 Texcoord1;
varying vec3 ViewDirection;
varying vec3 LightDirection;
//varying vec3 Normal;

void main( void )
{
   vec3  fvLightDirection = normalize( LightDirection );
   vec3  fvNormal         = normalize( texture2D( tex1map, Texcoord1 ).xyz );
//normalize( ( texture2D( tex1map, Texcoord1 ).xyz * 2.0 ) - 1.0 );
//normalize( gl_NormalMatrix * cNormal.xyz );
   float fNDotL           = dot( fvNormal, fvLightDirection ); 
   
   vec3  fvReflection     = normalize( ( ( 2.0 * fvNormal ) * fNDotL ) - fvLightDirection ); 
   vec3  fvViewDirection  = normalize( ViewDirection );
   float fRDotV           = max( 0.0, dot( fvReflection, fvViewDirection ) );
   
   vec4  fvBaseColor      = texture2D( basicTexture, Texcoord0 );
   
   vec4  fvTotalAmbient   = fvAmbient * fvBaseColor; 
   vec4  fvTotalDiffuse   = fvDiffuse * fNDotL * fvBaseColor; 
   vec4  fvTotalSpecular  = fvSpecular * ( pow( fRDotV, fSpecularPower ) );
  
   gl_FragColor = ( fvTotalAmbient + fvTotalDiffuse + fvTotalSpecular );
       
}