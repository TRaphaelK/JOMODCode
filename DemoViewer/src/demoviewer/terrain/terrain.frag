uniform sampler2D baseMap;
uniform sampler2D blend;
uniform sampler2D colr1;
uniform sampler2D colr2;
uniform sampler2D colr3;
uniform float dettile;
varying vec2 Texcoord;
//varying vec3 nrm;

void main( void )
{
      
	vec4 bl = texture2D(blend, Texcoord);
	vec2 tt=Texcoord * dettile;
	float sm=bl.r + bl.g + bl.b;
	vec4 c=vec4(0.0,0.0,0.0,1.0);
	vec4 c1;
	vec4 c2;
	vec4 c3;
	float cnt=0.0;
	if(sm>0.0) {
            if(bl.r>0.0) {
    		c1=texture2D(colr1, tt)*bl.r;
            }
            if(bl.g>0.0) {
		c2=texture2D(colr2, tt)*bl.g;
            }
            if(bl.b>0.0) {
		c3=texture2D(colr3, tt)*bl.b;
            }	
            c=(c1+c2+c3)/sm;
	}
	vec4  cl = texture2D(baseMap, Texcoord);
	// no detail color, use map color
	// add base color
	c= c * 0.3 + cl * 0.7;

      gl_FragColor      = c*gl_Color;
}