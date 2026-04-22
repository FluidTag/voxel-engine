#version 330 core
in vec3 TexCoord;
out vec4 FragColor;
uniform sampler2DArray textureArray;
void main() {
	vec4 color = texture(textureArray, TexCoord);
	if (color.a < 0.1) discard;
	FragColor = color;
}