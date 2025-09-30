#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D screenTexture;
uniform float contrast;
uniform float brightness;
uniform float saturation;
uniform vec3 colorFilter;

void main() {
    vec3 color = texture(screenTexture, TexCoord).rgb;
    
    // Apply brightness
    color += brightness;
    
    // Apply contrast
    color = (color - 0.5) * contrast + 0.5;
    
    // Apply saturation
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(gray), color, saturation);
    
    // Apply color filter
    color *= colorFilter;
    
    // Clamp to valid range
    color = clamp(color, 0.0, 1.0);
    
    FragColor = vec4(color, 1.0);
}
