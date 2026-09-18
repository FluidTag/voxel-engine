#version 330 core
out vec4 FragColor;
uniform vec3 outlineColor;
uniform bool drawFace;
uniform int textureId;
uniform sampler2DArray textureArray;
in vec2 uvOut;

void main() {
   vec4 targetColor = drawFace ? textureLod(textureArray, vec3(uvOut.xy, textureId), 0.0) : vec4(outlineColor, 1.0);

   FragColor = targetColor;
}