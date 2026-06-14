#version 330 core
in vec3 TexCoord;
in float vAoFactor;
out vec4 FragColor;

uniform sampler2DArray textureArray;

void main() {
	vec4 color = texture(textureArray, TexCoord);
	if (color.a < 0.1) discard;
	
	FragColor = vec4(color.rgb * vAoFactor, color.a);
}