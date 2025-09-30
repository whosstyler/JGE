#version 330 core

in vec2 TexCoord;
out vec4 FragColor;

uniform sampler2D screenTexture;
uniform float edgeThreshold;

void main() {
    vec2 texelSize = 1.0 / textureSize(screenTexture, 0);
    
    // Sobel edge detection
    vec3 tl = texture(screenTexture, TexCoord + vec2(-texelSize.x, -texelSize.y)).rgb;
    vec3 tm = texture(screenTexture, TexCoord + vec2(0.0, -texelSize.y)).rgb;
    vec3 tr = texture(screenTexture, TexCoord + vec2(texelSize.x, -texelSize.y)).rgb;
    vec3 ml = texture(screenTexture, TexCoord + vec2(-texelSize.x, 0.0)).rgb;
    vec3 mm = texture(screenTexture, TexCoord).rgb;
    vec3 mr = texture(screenTexture, TexCoord + vec2(texelSize.x, 0.0)).rgb;
    vec3 bl = texture(screenTexture, TexCoord + vec2(-texelSize.x, texelSize.y)).rgb;
    vec3 bm = texture(screenTexture, TexCoord + vec2(0.0, texelSize.y)).rgb;
    vec3 br = texture(screenTexture, TexCoord + vec2(texelSize.x, texelSize.y)).rgb;
    
    vec3 sobelX = tl + 2.0 * ml + bl - tr - 2.0 * mr - br;
    vec3 sobelY = tl + 2.0 * tm + tr - bl - 2.0 * bm - br;
    
    vec3 sobel = sqrt(sobelX * sobelX + sobelY * sobelY);
    float edge = length(sobel);
    
    if (edge > edgeThreshold) {
        FragColor = vec4(1.0, 1.0, 1.0, 1.0); // White edges
    } else {
        FragColor = vec4(mm, 1.0); // Original color
    }
}
