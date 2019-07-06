// phong texturing using a normal map

uniform vec3 fvLightPosition;
uniform vec3 fvEyePosition;

varying vec2 Texcoord0;
varying vec2 Texcoord1;
varying vec3 ViewDirection;
varying vec3 LightDirection;
//varying vec3 Normal;
   
void main( void )
{
   gl_Position = ftransform();
   Texcoord0    = gl_MultiTexCoord0.xy;
   Texcoord1    = gl_MultiTexCoord1.xy;
 
   vec4 fvObjectPosition = gl_ModelViewMatrix * gl_Vertex;
   
   ViewDirection  = fvEyePosition - fvObjectPosition.xyz;
   LightDirection = fvLightPosition - fvObjectPosition.xyz;
   //Normal         = gl_NormalMatrix * gl_Normal;
   
}