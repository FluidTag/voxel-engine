#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aTexCoords;

out vec2 TexCoords;

uniform mat4 projection;
uniform vec4 transform;

void main() {
    vec2 pixelPosition = transform.xy + (aPos * transform.zw);
    gl_Position = projection * vec4(pixelPosition, 0.0, 1.0);
    TexCoords = aTexCoords;
}