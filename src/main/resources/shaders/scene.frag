#version 330 core
in vec3 TexCoord;
in float vAoFactor;
out vec4 FragColor;

uniform sampler2DArray textureArray;

void main() {
	int textureLayer = int(round(TexCoord.z));
    vec4 color = texture(textureArray, TexCoord);
	
	if (color.a < 0.1) discard;
	
	vec3 litColor = color.rgb * vAoFactor;
	
	FragColor = vec4(litColor, color.a);
}