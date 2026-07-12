#version 330 core
out vec4 FragColor;

in vec2 TexCoords;

uniform sampler2D uiTexture;
uniform vec4 colorTint;
uniform bool useTexture;

void main() {
    if (useTexture) {
        vec4 texColor = texture(uiTexture, TexCoords);
        if (texColor.a < 0.05) discard;
        FragColor = texColor * colorTint;
    } else {
        FragColor = colorTint;
    }
}