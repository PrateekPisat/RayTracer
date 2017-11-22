package util;

import org.joml.Vector4f;

public class Color {
    int r,g,b;

    public Color(int r, int g, int b)
    {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public Color()
    {
        r=g=b=0;
    }

    public Color(Vector4f color)
    {
        this.r = (int)color.x;
        this.g = (int)color.y;
        this.b = (int)color.z;
    }

    public int toInt()
    {
        return (r*255<<16)|(g*255<<8)|(b*255);
    }

}
