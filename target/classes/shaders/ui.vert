#version 330 core
layout (location = 0) in vec2 aPos;
layout (location = 1) in vec2 aTexCoords;

out vec2 TexCoords;

uniform mat4 projection;
uniform vec4 transform;

void main() {
    TexCoords = aTexCoords;
    gl_Position = projection * vec4(aPos.x * transform.z + transform.x, aPos.y * transform.w + transform.y, 0.0, 1.0);
}