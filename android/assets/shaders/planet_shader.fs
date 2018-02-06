#ifdef GL_ES
    #define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture; //planet noise texture
uniform sampler2D color_texture; //texture which containes all possible colors to color planet texture

void main() {
    vec4 textel = texture2D(u_texture, v_texCoords);
    if(textel.a < 0.1)
        discard;

    float noise = textel.r;

    vec4 color = texture2D(color_texture, vec2(noise, 0.1));

    gl_FragColor = v_color * color;
}