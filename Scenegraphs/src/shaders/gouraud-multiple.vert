#version 330 core

struct MaterialProperties
{
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float shininess;
};

struct LightProperties
{
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    vec4 position;
    vec4 spotDirection;
    float spotAngle;
};

const int MAXLIGHTS = 10;

uniform MaterialProperties material;
uniform LightProperties light[MAXLIGHTS];
uniform int numLights;


in vec4 vPosition;
in vec4 vNormal;
in vec4 vTexCoord;

uniform mat4 projection;
uniform mat4 modelview;
uniform mat4 normalmatrix;
uniform mat4 texturematrix;
out vec4 fColor;
out vec4 fPosition;
out vec4 fTexCoord;

void main()
{
    vec3 lightVec,viewVec,reflectVec;
    vec3 normalView;
    vec3 ambient,diffuse,specular;
    float nDotL,rDotV;
    float cosphi, cosTheta;

    fPosition = modelview * vec4(vPosition.xyzw);
    gl_Position = projection * fPosition;
    int si=1;

    vec4 tNormal = normalmatrix * vNormal;
    normalView = normalize(tNormal.xyz);

    fColor = vec4(0,0,0,1);

    for (int i=0;i<numLights;i++)
    {
        if (light[i].position.w!=0)
            {
                lightVec = normalize(light[i].position.xyz - fPosition.xyz);
                si=1;
            }
        else
            {
                lightVec = normalize(-light[i].position.xyz);
                cosphi = dot(normalize(light[i].spotDirection.xyz),-lightVec);
                cosTheta = cos(light[i].spotAngle);
                if(cosphi>cosTheta)
                    si=1;
                else
                    si=0;
            }


        nDotL = dot(normalView,lightVec);

        viewVec = -fPosition.xyz;
        viewVec = normalize(viewVec);

        reflectVec = reflect(-lightVec,normalView);
        reflectVec = normalize(reflectVec);

        rDotV = max(dot(reflectVec,viewVec),0.0);

        ambient = material.ambient * light[i].ambient;
        diffuse = material.diffuse * light[i].diffuse * max(nDotL,0);
        if (nDotL>0)
            specular = material.specular * light[i].specular * pow(rDotV,material.shininess);
        else
            specular = vec3(0,0,0);
        fColor = fColor + vec4(ambient+diffuse+specular,1.0);
    }

    fTexCoord =  vec4(1*vTexCoord.s,1*vTexCoord.t,0,1);

}
