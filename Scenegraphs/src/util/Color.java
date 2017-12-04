package util;

public class Color {
    float r,g,b;

    public Color()
    {
        this.r = new Float(0.0f);
        this.g = new Float(0.0f);
        this.b = new Float(0.0f);
    }

    public Color(float r, float g, float b)
    {
        this.r = new Float(r);
        this.g = new Float(g);
        this.b = new Float(b);
    }

    public Color(int color)
    {
        this.b = new Float((float) (color & 0xff) / 255);
        this.g = new Float((float)((color & 0xff00) >> 8) / 255);
        this.r = new Float((float)((color & 0xff0000) >> 16) / 255);
    }

    public void addColor(float r, float g, float b)
    {
        this.r = new Float(r+this.r);
        if(this.r>1)
            this.r = new Float(1);
        this.g = new Float(g + this.g);
        if(this.g>1)
            this.g = new Float(1);
        this.b = new Float(b + this.b);
        if(this.b>1)
            this.b= new Float(1);;
    }

    public void mul(float r, float g, float b)
    {
        this.r=new Float(this.r*r);
        if(this.r>1)
            this.r = new Float(1);
        this.g = new Float(g * this.g);
        if(this.g>1)
            this.g = new Float(g);
        this.b = new Float(b * this.b);
        if(this.b>1)
            this.b=new Float(1);
        if(this.r<0)
            this.r= new Float(0);;
        if(this.g<0)
            this.g=new Float(0);;
        if(this.b<0)
            this.b=new Float(0);;
    }

    public int toInt()
    {
        return (int)(r*255)<<16 | (int)(g*255)<<8 | (int)b*255;
    }

    public Color toColor(int rgb)
    {

        return new Color((rgb>>16)&255,  (rgb>>8)&255,  rgb&255);
    }

    public float getRed()
    {return this.r;}

    public float getGreen()
    {return this.g;}

    public float getBlue()
    {return this.b;}

    public String toString()
    {
        return ("color = ["+r+" "+g+" "+b+"]");
    }
}
