#version 300 es
in vec4 vPosition;
in vec4 aColor;
out vec4 vColor;
void main() {
    gl_Position  = vPosition;
    vColor = aColor;
}
