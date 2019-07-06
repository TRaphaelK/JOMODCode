uniform sampler2D basicTexture;

varying vec2 Texcoord;

void main( void )
{
    gl_FragColor = texture2D( basicTexture, Texcoord );
    
}