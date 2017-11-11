#ifdef GL_ES
    #define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform sampler2D light_map;

uniform vec3 ambient_color;
uniform vec2 viewport_size;

vec2 getLigthMapCoords() {
    return gl_FragCoord.xy / viewport_size;
}

void main() {
    vec4 textel = texture2D(u_texture, v_texCoords);

    vec4 light_textel = texture2D(light_map, getLigthMapCoords());
    light_textel.a = 1.0;

    gl_FragColor = v_color * textel * max(vec4(ambient_color, 1.0), light_textel);
}