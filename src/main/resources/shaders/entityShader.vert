#version 330 core
layout (location = 0) in vec3 pos;
layout (location = 1) in int uvData;

out vec3 TexCoord;
out float vAoFactor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

void main() {
	gl_Position = projection * view * model * vec4(pos * 0.3, 1.0);
	int u = ((uvData >> 8) & 0x3F);
    	int v = ((uvData >> 14) & 0x3F);
    	int TexId = (uvData & 0xFF);

	TexCoord = vec3(float(u), float(v), int(TexId));
	vAoFactor = 1.0;
}