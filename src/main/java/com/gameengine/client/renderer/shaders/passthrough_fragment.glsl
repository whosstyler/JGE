#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D screenTexture;

void main() {
    // Simple passthrough - just display the texture as-is
    FragColor = texture(screenTexture, TexCoord);
}