#ifdef GL_ES
    #define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture; //light mask

void main() {
    vec4 textel = texture2D(u_texture, v_texCoords);

    gl_FragColor = v_color * textel.a;
}

//Color.rgb = Color.rgb * (DarknessMultiplier + (1 - DarknessMultiplier) * LightingPixelValue);
//darkness mult = brightness; lighting pixel val = alpha value from timeutils color
//pass these vals in batch's color to don't create not neceserry uniforms