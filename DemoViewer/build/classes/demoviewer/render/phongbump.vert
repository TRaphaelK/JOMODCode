uniform vec3 fvLightPosition;
uniform vec3 fvEyePosition;

varying vec2 Texcoord0;
varying vec2 Texcoord1;
varying vec3 ViewDirection;
varying vec3 LightDirection;
   
attribute vec3 binormal;
attribute vec3 tangent;
   
void main( void )
{
   gl_Position = ftransform();
   Texcoord0    = gl_MultiTexCoord0.xy;
   Texcoord1    = gl_MultiTexCoord1.xy;
 
   vec4 fvObjectPosition = gl_ModelViewMatrix * gl_Vertex;
   
   vec3 fvViewDirection  = fvEyePosition - fvObjectPosition.xyz;
   vec3 fvLightDirection = fvLightPosition - fvObjectPosition.xyz;
     
   vec3 fvNormal         = gl_NormalMatrix * gl_Normal;
   vec3 fvBinormal       = gl_NormalMatrix * binormal;
   vec3 fvTangent        = gl_NormalMatrix * tangent;
      
   ViewDirection.x  = dot( fvTangent, fvViewDirection );
   ViewDirection.y  = dot( fvBinormal, fvViewDirection );
   ViewDirection.z  = dot( fvNormal, fvViewDirection );
   
   LightDirection.x  = dot( fvTangent, fvLightDirection.xyz );
   LightDirection.y  = dot( fvBinormal, fvLightDirection.xyz );
   LightDirection.z  = dot( fvNormal, fvLightDirection.xyz );
   
}
