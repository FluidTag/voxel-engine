#version 330 core
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec2 uv;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
out vec2 uvOut;

void main() {
   vec4 clip = projection * view * model * vec4(aPos, 1.0);
   uvOut = uv;
   clip.z -= 0.0005 * clip.w;
   gl_Position = clip;
}