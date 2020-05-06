#version 300 es
in vec4 aPosition;
in vec4 aColor;
out vec4 vColor;
uniform mat4 uMatrix;
void main() {
    gl_Position = aPosition * uMatrix;
    vColor = aColor;
}