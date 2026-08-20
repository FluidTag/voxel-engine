#version 330 core
in vec3 TexCoord;
in float vAoFactor;
in float lightFactor;
out vec4 FragColor;

uniform sampler2DArray textureArray;
uniform int skylightValue;

void main() {
	int textureLayer = int(round(TexCoord.z));
    vec4 color = texture(textureArray, TexCoord);
	
	if (color.a < 0.1) discard;
	
	vec3 litColor = color.rgb * vAoFactor * ((lightFactor/16.0)+0.2);
	FragColor = vec4(litColor, color.a);
}