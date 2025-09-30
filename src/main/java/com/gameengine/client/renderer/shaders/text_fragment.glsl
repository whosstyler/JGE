#version 330 core

in vec2 TexCoords;
out vec4 color;

uniform sampler2D text;
uniform vec3 textColor;
uniform float outlineWidth;
uniform vec3 outlineColor;
uniform float shadowOffset;
uniform vec3 shadowColor;
uniform int hasOutline;
uniform int hasShadow;

void main() {
    vec4 sampled = vec4(1.0, 1.0, 1.0, texture(text, TexCoords).r);

    if (hasShadow == 1) {
        // Sample shadow offset
        float shadowAlpha = texture(text, TexCoords + vec2(shadowOffset, -shadowOffset)).r;
        vec4 shadow = vec4(shadowColor, shadowAlpha * 0.5);

        if (sampled.a < 0.1) {
            color = shadow;
        } else {
            color = vec4(textColor, 1.0) * sampled;
        }
    } else if (hasOutline == 1) {
        // Simple outline by sampling neighbors
        float alpha = sampled.a;
        float outline = 0.0;

        for (float x = -outlineWidth; x <= outlineWidth; x += outlineWidth) {
            for (float y = -outlineWidth; y <= outlineWidth; y += outlineWidth) {
                outline = max(outline, texture(text, TexCoords + vec2(x, y)).r);
            }
        }

        if (alpha < 0.1 && outline > 0.1) {
            color = vec4(outlineColor, outline);
        } else {
            color = vec4(textColor, 1.0) * sampled;
        }
    } else {
        color = vec4(textColor, 1.0) * sampled;
    }
}