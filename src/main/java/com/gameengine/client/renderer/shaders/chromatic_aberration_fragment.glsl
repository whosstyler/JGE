#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D screenTexture;
uniform float aberrationStrength;
uniform vec2 center;

void main() {
    vec2 coord = TexCoord;
    vec2 direction = coord - center;
    
    // Sample RGB channels with slight offset
    float r = texture(screenTexture, coord + direction * aberrationStrength).r;
    float g = texture(screenTexture, coord).g;
    float b = texture(screenTexture, coord - direction * aberrationStrength).b;
    
    FragColor = vec4(r, g, b, 1.0);
}
