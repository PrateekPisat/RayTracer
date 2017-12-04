package util;

import org.joml.Vector3f;
import org.joml.Vector4f;

public class Ray
{
    public Vector4f s; // origin
    public Vector4f v; // direction

    public Ray(Vector4f s, Vector4f v)
    {
        this.s = new Vector4f(s);
        this.v = new Vector4f(v);
    }
    public Ray(Vector4f s, Vector3f v)
    {
        this.s = new Vector4f(s);
        this.v = new Vector4f(new Vector3f(v),0);
    }
    public Ray(Ray r1)
    {
        this.s = new Vector4f(r1.s);
        this.v = new Vector4f(r1.v);
    }

}
