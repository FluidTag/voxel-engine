#version 330 core
layout (location = 0) in ivec2 packedData;
out vec3 TexCoord;
out float vAoFactor;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
void main() {
	int dat1 = packedData.x;
	int dat2 = packedData.y;
	
	int x = (dat1 & 0x1FF);
	int y = ((dat1 >> 9) & 0x1FF);
	int z = ((dat1 >> 18) & 0x1FF);
	
	int u = ((dat2 >> 8) & 0x3F);
	int v = ((dat2 >> 14) & 0x3F);
	int TexId = (dat2 & 0xFF);
	int scaleFlag = (dat2 >> 22) & 1;
	
	int aoLevel = (dat2 >> 20) & 0x3;
	if (aoLevel == 3) vAoFactor = 1.0;
    else if (aoLevel == 2) vAoFactor = 0.75;
    else if (aoLevel == 1) vAoFactor = 0.5;
    else vAoFactor = 0.25;
	
	vec3 pos = vec3(float(x), float(y), float(z));
	
    float multiplier = (scaleFlag == 1) ? 0.1 : 1.0;
    pos *= multiplier;
	
	gl_Position = projection * view * model * vec4(pos, 1.0);
	TexCoord = vec3(float(u), float(v), int(TexId));
}