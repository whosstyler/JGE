#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D screenTexture;
uniform bool horizontal;
uniform float blurRadius;

// Gaussian blur weights for 9-tap kernel
float weight[5] = float[] (0.2270270270, 0.1945945946, 0.1216216216, 0.0540540541, 0.0162162162);

void main() {
    vec2 tex_offset = 1.0 / textureSize(screenTexture, 0); // Size of single texel
    vec3 result = texture(screenTexture, TexCoord).rgb * weight[0]; // Current fragment contribution
    
    if (horizontal) {
        for (int i = 1; i < 5; ++i) {
            result += texture(screenTexture, TexCoord + vec2(tex_offset.x * i * blurRadius, 0.0)).rgb * weight[i];
            result += texture(screenTexture, TexCoord - vec2(tex_offset.x * i * blurRadius, 0.0)).rgb * weight[i];
        }
    } else {
        for (int i = 1; i < 5; ++i) {
            result += texture(screenTexture, TexCoord + vec2(0.0, tex_offset.y * i * blurRadius)).rgb * weight[i];
            result += texture(screenTexture, TexCoord - vec2(0.0, tex_offset.y * i * blurRadius)).rgb * weight[i];
        }
    }
    
    FragColor = vec4(result, 1.0);
}
