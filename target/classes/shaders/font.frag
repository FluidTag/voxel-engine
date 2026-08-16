#version 330 core
out vec4 FragColor;

in vec2 TexCoords;

uniform sampler2D fontTexture;
uniform vec4 fontColor;

void main() {
    float alpha = texture(fontTexture, TexCoords).r;
    FragColor = vec4(fontColor.rgb, fontColor.a * alpha);
}