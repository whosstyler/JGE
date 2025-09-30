#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D screenTexture;
uniform float time;
uniform float intensity;
uniform vec2 center;

void main() {
    vec2 coord = TexCoord;
    
    // Calculate distance from center
    float distance = length(coord - center);
    
    // Create ripple effect
    float ripple = sin(distance * 20.0 - time * 8.0) * intensity * (1.0 - distance);
    
    // Apply distortion
    vec2 direction = normalize(coord - center);
    coord += direction * ripple * 0.02;
    
    // Sample texture with distorted coordinates
    vec3 color = texture(screenTexture, coord).rgb;
    
    FragColor = vec4(color, 1.0);
}
