#version 330 core
out vec4 FragColor;

in vec2 TexCoords;

uniform sampler2D fontTexture;

void main() {
    float alpha = texture(fontTexture, TexCoords).r;
    FragColor = vec4(vec3(0.0, 0.0, 0.0), alpha);
}