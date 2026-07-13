#version 330 core
layout (location = 0) in vec3 aPos;
uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
void main() {
   vec4 clip = projection * view * model * vec4(aPos, 1.0);
   clip.z -= 0.0005 * clip.w;
   gl_Position = clip;
}