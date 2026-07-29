#version 330 core
out vec4 FragColor;

in vec2 TexCoords;

uniform sampler2D uiTexture;
uniform vec4 colorTint;
uniform bool useTexture;

uniform vec2 u_resolution;
uniform vec4 u_rect;
uniform int u_border;

void main() {
    if (useTexture) {
        vec4 texColor = texture(uiTexture, TexCoords);
        ivec2 pixelCoord = ivec2(gl_FragCoord.xy);
        int x = pixelCoord.x;
        int y = pixelCoord.y;

        if (texColor.a < 0.05) discard;
        FragColor = texColor * colorTint;
    } else {
        FragColor = colorTint;
    }
}