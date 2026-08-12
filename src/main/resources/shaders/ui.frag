#version 330 core
out vec4 FragColor;

in vec2 TexCoords;

uniform sampler2D uiTexture;
uniform vec4 colorTint;
uniform int useTexture;

uniform vec2 u_resolution;
uniform vec4 u_rect;

uniform vec4 u_uvTransform;

void main() {
    if (useTexture == 1) {
        vec2 tileUv = TexCoords * u_uvTransform.zw + u_uvTransform.xy;
        FragColor = texture(uiTexture, tileUv);
    } else if (useTexture == 2) {
        vec4 texColor = texture(uiTexture, TexCoords);
        if (texColor.a < 0.05) discard;
        FragColor = texColor * colorTint;
    } else {
        FragColor = colorTint;
    }
}