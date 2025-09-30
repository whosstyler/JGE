#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D screenTexture;
uniform float vignetteStrength;
uniform float vignetteRadius;

void main() {
    vec3 color = texture(screenTexture, TexCoord).rgb;
    
    // Calculate distance from center
    vec2 center = vec2(0.5, 0.5);
    float distance = length(TexCoord - center);
    
    // Create vignette effect
    float vignette = smoothstep(vignetteRadius, vignetteRadius - 0.3, distance);
    vignette = mix(1.0 - vignetteStrength, 1.0, vignette);
    
    color *= vignette;
    
    FragColor = vec4(color, 1.0);
}
