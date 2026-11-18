#version 460 core
layout (location = 0) in ivec2 packedData;
layout (std430, binding = 0) readonly buffer ModelPositions {
    int modelData[];
};

out vec3 TexCoord;
out float vAoFactor;
out float lightFactor;

uniform mat4 view;
uniform mat4 projection;

void main() {
    int modelIndex = gl_BaseInstance * 3;
    ivec3 modelPos = ivec3(
        modelData[modelIndex],
        modelData[modelIndex + 1],
        modelData[modelIndex + 2]
    );

	int dat1 = packedData.x;
	int dat2 = packedData.y;
	
	int x = (dat1 & 0x1FF);
	int y = ((dat1 >> 9) & 0x1FF);
	int z = ((dat1 >> 18) & 0x1FF);
	
	int u = ((dat2 >> 8) & 0x3F);
	int v = ((dat2 >> 14) & 0x3F);
	int TexId = (dat2 & 0xFF);
	int scaleFlag = (dat2 >> 22) & 1;
	int light = (dat2 >> 23) & 0xF;
	
	int aoLevel = (dat2 >> 20) & 0x3;
	if (aoLevel == 3) vAoFactor = 1.0;
    else if (aoLevel == 2) vAoFactor = 0.79;
    else if (aoLevel == 1) vAoFactor = 0.55;
    else vAoFactor = 0.4;
    lightFactor = light;
	float multiplier = (scaleFlag == 1) ? 0.1 : 1.0;

	vec3 pos = vec3(float(x), float(y), float(z)) * multiplier + vec3(modelPos);

	gl_Position = projection * view * vec4(pos, 1.0);
	TexCoord = vec3(float(u), float(v), int(TexId));
}