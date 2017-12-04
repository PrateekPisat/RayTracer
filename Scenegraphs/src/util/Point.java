package util;

import org.joml.Vector4f;

public class Point {
    public float x, y, z;
    public int color;
    public Vector4f normal;
    public boolean reflective;
    public  util.Material material;

    public Point(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point(Vector4f p1,int c)
    {
        this.x = new Float(p1.x);
        this.y = new Float(p1.y);
        this.z = new Float(p1.z);
        this.color = c;
    }

    public Point(Vector4f p1,int c,Vector4f norm,boolean reflective, util.Material material)
    {
        this.x = new Float(p1.x);
        this.y = new Float(p1.y);
        this.z = new Float(p1.z);
        this.color = c;
        this.normal = norm;
        this.reflective = reflective;
        this.material = material;
    }

    public double distanceTo(Point p) {
        return Math.sqrt((p.x - x)*(p.x - x) + (p.y - y)*(p.y - y) + (p.z - z)*(p.z - z));
    }

    public Point plus(Vector4f v) {
        return new Point(x + v.x, y + v.y, z + v.z);
    }

    public String toString() {
        return "[" + x + ", " + y + ", " + z + "]";
    }
}