package util;

import org.joml.Vector4f;

public class Ray
{
    public Vector4f s; // origin
    public Vector4f v; // direction

    public Ray(Vector4f s, Vector4f v)
    {
        this.s = s;
        this.v = v;
    }

}
