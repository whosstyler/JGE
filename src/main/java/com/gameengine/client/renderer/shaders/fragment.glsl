#version 330 core

in vec3 fragColor;
in vec3 fragNormal;
in vec3 fragPos;

out vec4 outColor;

uniform vec3 lightDir = vec3(-0.3, -1.0, -0.5); // Directional light (sun)
uniform vec3 lightColor = vec3(1.0, 1.0, 1.0); // White light
uniform float ambientStrength = 0.8; // Strong ambient

void main() {
    // Ambient lighting
    vec3 ambient = ambientStrength * lightColor;

    // Diffuse lighting
    vec3 norm = normalize(fragNormal);
    vec3 lightDirection = normalize(-lightDir);
    float diff = max(dot(norm, lightDirection), 0.0);
    vec3 diffuse = diff * lightColor * 0.5; // Reduce diffuse contribution

    // Combine lighting (clamped to prevent washout)
    vec3 lighting = min(ambient + diffuse, vec3(1.0));
    vec3 result = lighting * fragColor;

    outColor = vec4(result, 1.0);
}