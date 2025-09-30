#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D scene;
uniform sampler2D bloomBlur;
uniform bool bloom;
uniform float exposure;

void main() {
    const float gamma = 2.2;
    vec3 hdrColor = texture(scene, TexCoord).rgb;      
    vec3 bloomColor = texture(bloomBlur, TexCoord).rgb;
    
    if (bloom) {
        hdrColor += bloomColor; // Additive blending
    }
    
    // Tone mapping
    vec3 result = vec3(1.0) - exp(-hdrColor * exposure);
    
    // Gamma correction
    result = pow(result, vec3(1.0 / gamma));
    
    FragColor = vec4(result, 1.0);
}
