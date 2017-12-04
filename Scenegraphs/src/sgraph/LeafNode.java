package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import util.*;
import util.Color;
import util.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.awt.*;

/**
 * This node represents the leaf of a scene graph. It is the only type of node that has
 * actual geometry to render.
 * @author Amit Shesh
 */
public class LeafNode extends AbstractNode
{
    /**
     * The name of the object instance that this leaf contains. All object instances are stored
     * in the scene graph itself, so that an instance can be reused in several leaves
     */
    protected String objInstanceName;

    /**
     * A map of texture names and the texture-images
     */
    protected Map<String, TextureImage> textures;
    /**
     * The material associated with the object instance at this leaf
     */
    protected util.Material material;
    public util.Light light = null;

    Vector3f lv;
    Vector3f amb = new Vector3f();
    Vector3f dif = new Vector3f();
    Vector3f spec = new Vector3f();
    Matrix3f normalmatrix;
    Vector4f fposition;
    Vector3f viewVec;
    Vector3f reflectVec;
    Vector4f norm;
    Vector3f normalView;
    float nDotl, rDotv;

    public ArrayList<Light> lights;

    protected String textureName;

    protected Map<String,util.PolygonMesh> meshes;
    protected PolygonMesh currentMesh;

    protected Vector4f leftBotFront,leftTopFront, leftBotBack, leftTopBack;
    protected Vector4f rightBotFront,rightTopFront, rightBotBack, rightTopBack, center;//bounding box

    protected  Matrix4f modelView;

    public LeafNode(String instanceOf,IScenegraph graph,String name)
    {
        super(graph,name);
        this.objInstanceName = instanceOf;
        lights = new ArrayList<Light>();
        meshes = new HashMap<>();
        leftBotFront = new Vector4f();
        leftTopFront = new Vector4f();
        leftBotBack = new Vector4f();
        leftTopBack = new Vector4f();
        rightBotFront  = new Vector4f();
        rightTopFront  = new Vector4f();
        rightBotBack  = new Vector4f();
        rightTopBack  = new Vector4f();
        currentMesh = new PolygonMesh();
        center = new Vector4f();
        textures = new HashMap<>();
    }

    /*
	 *Set the material of each vertex in this object
	 */
    @Override
    public void setMaterial(util.Material mat)
    {
        material = new util.Material(mat);
    }

    /**
     * Set texture ID of the texture to be used for this leaf
     * @param name
     */
    @Override
    public void setTextureName(String name)
    {
        textureName = name;
    }

    /*
     * gets the material
     */
    public util.Material getMaterial()
    {
        return material;
    }

    @Override
    public INode clone()
    {
        LeafNode newclone = new LeafNode(this.objInstanceName,scenegraph,name);
        newclone.setMaterial(this.getMaterial());
        return newclone;
    }


    /**
     * Delegates to the scene graph for rendering. This has two advantages:
     * <ul>
     *     <li>It keeps the leaf light.</li>
     *     <li>It abstracts the actual drawing to the specific implementation of the scene graph renderer</li>
     * </ul>
     * @param context the generic renderer context {@link sgraph.IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     * @throws IllegalArgumentException
     */
    @Override
    public void draw(IScenegraphRenderer context,Stack<Matrix4f> modelView) throws IllegalArgumentException
    {
        //getting the textures
       this.textures = context.getTextures();
        if (objInstanceName.length()>0)
        {
            context.drawMesh(objInstanceName,material,textureName,modelView.peek());
        }
    }

    public void addLight(Light l)
    {
        lights.add(l);
    }

     public ArrayList<Light> getLights(Stack<Matrix4f> modelView) {
        ArrayList<Light> llights = new ArrayList<Light>();
        for(int i = 0; i < lights.size(); i++) {
            Light l = lights.get(i);
            l.setPosition(l.getPosition());
            llights.add(l);
        }
        return llights;
    }

    public Vector4f getMinBounds()
    {
        return leftBotFront;
    }

    public Vector4f getMaxBounds()
    {
        return  rightTopBack;
    }

    public Vector4f getleftTopFront()
    {
        return  leftTopFront;
    }

    public Vector4f getleftBotBack()
    {
        return  leftBotBack;
    }

    public Vector4f getleftTopBack() { return leftTopBack;}

    public Vector4f getrightBotBack() { return rightBotBack;}

    public Vector4f getrightTopFront() {return rightTopFront;}

    public Vector4f getrightBotFront() {return rightBotFront; }

    @Override
    public void setBoundingBox()
    {
        currentMesh = meshes.get(objInstanceName);
        currentMesh.computeBoundingBox();
        leftBotBack = currentMesh.getMinimumBounds();
        rightTopFront = currentMesh.getMaximumBounds();
        leftBotFront = new Vector4f(leftBotBack.x, leftBotBack.y, rightTopFront.z,1);
        leftTopBack = new Vector4f(leftBotBack.x, rightTopFront.y, leftBotBack.z,1);
        leftTopFront = new Vector4f(leftBotBack.x,rightTopFront.y,rightTopFront.z,1);
        rightBotFront = new Vector4f(rightTopFront.x,leftBotBack.y,rightTopFront.z,1);
        rightTopBack = new Vector4f(rightTopFront.x, leftBotBack.y,leftBotBack.z, 1);
        rightTopBack = new Vector4f(rightTopFront.x,rightTopFront.y,leftBotBack.z, 1);
        Vector4f center = new Vector4f(
                (leftBotBack.x + rightTopFront.x)/2,
                (leftBotBack.y + rightTopFront.y)/2,
                (leftBotBack.z + rightTopFront.z)/2,
                1
        );
    }
    @Override
    public void setMeshes(Map<String,PolygonMesh> meshes)
    {
        this.meshes = meshes;
    }

    public Vector4f getCenter()
    {
        //Bottom face center
        Vector4f blc = setcenter(leftBotBack, leftBotFront);
        Vector4f brc = setcenter(rightBotFront, rightBotBack);
        Vector4f bot = setcenter(blc, brc);

        //Top face center
        Vector4f tlc = setcenter(leftTopBack, leftTopFront);
        Vector4f trc = setcenter(rightTopFront, rightTopBack);
        Vector4f top = setcenter(tlc, trc);

        //center of box
        Vector4f center = new Vector4f(
                (leftBotBack.x + rightTopFront.x)/2,
                (leftBotBack.y + rightTopFront.y)/2,
                (leftBotBack.z + rightTopFront.z)/2,
                1
        );

        return center;
    }

    protected Vector4f setcenter(Vector4f v1, Vector4f v2) {
        Vector4f center = new Vector4f(((v1.x+v2.x)/2), ((v1.y+v2.y)/2), ((v1.z+v2.z)/2), ((v1.w+v2.w)/2));
        return center;
    }

    public INode explodeNode(Vector4f pc) {
        Vector4f v = new Vector4f(center.x -pc.x, center.y-pc.y, center.z-pc.z, 1).normalize();
        INode translate = new TransformNode(scenegraph, "exploded: "+ name);
        translate.setTransform(new Matrix4f().translate(10*v.x,10*v.y,10*v.z));
        translate.addChild(this);

        return translate;
    }

    public IScenegraph expload(IScenegraph sg, IScenegraph blank)
    {
        INode node =new TransformNode(blank,"Exploaded "+ name);
        node.setTransform(new Matrix4f().translate(-10,0,0));
        node.addChild(this.clone());
        blank.addNode("Exploaded"+name, node);
        blank.addPolygonMesh(objInstanceName,meshes.get(objInstanceName));
        return blank;
    }

    public Point rayCast(Ray r1, Stack<Matrix4f> modelview, ArrayList<Light> ls) //object -> view
    {
        Matrix4f model = new Matrix4f(modelview.peek());
        Matrix4f copy = new Matrix4f(modelview.peek());
        Matrix4f inv = new Matrix4f(copy.invert());

        Ray RayCopy = new Ray(r1);

        inv.transform(RayCopy.s);
        inv.transform(RayCopy.v);

        boolean isReflective = false;
        if(material.getReflection()>0.0f)
            isReflective = true;

        Matrix4f normalMatrix = new Matrix4f(model);
        normalMatrix.invert().transpose();

//        r1.s = r1.s.mul(inv);
//        r1.v = r1.v.mul(inv);
        if(this.objInstanceName.equals("Box")) {
            float tmin = (float) (-0.5 - RayCopy.s.x) / RayCopy.v.x;
            float tmax = (float) (0.5 - RayCopy.s.x) / RayCopy.v.x;

            if (tmin > tmax)
            {
                float  temp = tmax;
                tmax = tmin;
                tmin = temp;
            }

            float tymin = (float) (-0.5 - RayCopy.s.y) / RayCopy.v.y;
            float tymax = (float) (0.5 - RayCopy.s.y) / RayCopy.v.y;

            if (tymin > tymax)
            {
                float temp = tymax;
                tymax = tymin;
                tymin = temp;
            }

            if ((tmin > tymax) || (tymin > tmax))
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());

            if (tymin > tmin)
                tmin = tymin;

            if (tymax < tmax)
                tmax = tymax;

            float tzmin = (float) (-0.5 - RayCopy.s.z) / RayCopy.v.z;
            float tzmax = (float) (0.5 - RayCopy.s.z) / RayCopy.v.z;

            if (tzmin > tzmax)
            {
                float temp = tzmax;
                tzmax = tzmin;
                tzmin = temp;
            }

            if ((tmin > tzmax) || (tzmin > tmax))
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());

            if (tzmin > tmin)
                tmin = tzmin;

            if (tzmax < tmax)
                tmax = tzmax;

            if(tmin<0 && tmax<0)
            {
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());}

            if(tmin==0 && tmax==0)
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());

            Vector4f p1, p2;
            p1 = new Vector4f(RayCopy.s.x + (RayCopy.v.x * tmin),
                              RayCopy.s.y + (RayCopy.v.y * tmin),
                              RayCopy.s.z + (RayCopy.v.z * tmin),1);

            p2 = new Vector4f(RayCopy.s.x + (RayCopy.v.x * tmax),
                              RayCopy.s.y + (RayCopy.v.y * tmax),
                              RayCopy.s.z + (RayCopy.v.z * tmax),1);




            float d1,d2;
            d1 = (float)Math.sqrt((Math.pow((p1.x-RayCopy.s.x),2))+Math.pow((p1.y-RayCopy.s.y),2)+Math.pow((p1.z-RayCopy.s.z),2));
            d2 = (float)Math.sqrt((Math.pow((p2.x-RayCopy.s.x),2))+Math.pow((p2.y-RayCopy.s.y),2)+Math.pow((p2.z-RayCopy.s.z),2));


            if(d1<d2)
            {
                norm = new Vector4f(getNormalBox(p1));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                fposition = new Vector4f(p1);
                fposition.mul(model,fposition);
                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p1,ls,model).toInt(),new Vector4f(normalView,0), isReflective,this.material);
            }
            else
            {
                norm = new Vector4f(getNormalBox(p2));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                fposition = new Vector4f(p2);
                fposition.mul(model,fposition);
                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p2,ls,model).toInt(),new Vector4f(normalView,0), isReflective, this.material);
            }
        }
        else if(objInstanceName.equals("Sphere"))
        {
            float tmax, tmin;
            float A,B,C;


            A = ((float)(Math.pow((double)(RayCopy.v.x), 2)) + (float)(Math.pow((double)(RayCopy.v.y), 2))  + (float)(Math.pow((double)(RayCopy.v.z), 2)) );
            B = ((2*(RayCopy.v.x*RayCopy.s.x))+ (2*(RayCopy.v.y*RayCopy.s.y))+(2*(RayCopy.v.z*RayCopy.s.z)));
            C = (((float)(Math.pow((double)(RayCopy.s.x), 2)) + (float)(Math.pow((double)(RayCopy.s.y), 2)) + (float)(Math.pow((double)(RayCopy.s.z), 2))) - 1);

            if((float)((Math.pow((double) B, 2))) < (4*(A*C))) {
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());
            }

            if((float)((Math.pow((double) B, 2))) == (4*(A*C))) {
                tmin = (- B - ((float)Math.sqrt((Math.pow((double)(B), 2))-(4*(A*C)))))/(2*A);
                Vector4f p1;
                p1 = new Vector4f(0,0,0,1);
                p1.x = RayCopy.s.x + (RayCopy.v.x * tmin);
                p1.y = RayCopy.s.y + (RayCopy.v.y * tmin);
                p1.z = RayCopy.s.z + (RayCopy.v.z * tmin);
                Vector4f pointCopy = new Vector4f(p1);
                fposition = new Vector4f(pointCopy);
                fposition.mul(model,fposition);
                norm = new Vector4f(getNormalSphere(p1));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p1,ls,model).toInt(),new Vector4f(normalView,0), isReflective, this.material);
            }
            tmin = (- B - ((float)Math.sqrt((Math.pow((double)(B), 2))-(4*(A*C)))))/(2*A);
            tmax = (- B + ((float)Math.sqrt((Math.pow((double)(B), 2))-(4*(A*C)))))/(2*A);



            if(tmin<0)
            {
                tmin = tmax;
                if(tmin<=0)
                    return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());
            }


            // Points of intersection
            Vector4f p1, p2;
            p1 = new Vector4f(0,0,0,1);
            p1.x = RayCopy.s.x + (RayCopy.v.x * tmin);
            p1.y = RayCopy.s.y + (RayCopy.v.y * tmin);
            p1.z = RayCopy.s.z + (RayCopy.v.z * tmin);

            p2 = new Vector4f(0,0,0,1);
            p2.x = RayCopy.s.x + (RayCopy.v.x * tmax);
            p2.y = RayCopy.s.y + (RayCopy.v.y * tmax);
            p2.z = RayCopy.s.z + (RayCopy.v.z * tmax);




            float d1,d2;
            d1 = (float)Math.sqrt((Math.pow((p1.x-RayCopy.s.x),2))+Math.pow((p1.y-RayCopy.s.y),2)+Math.pow((p1.z-RayCopy.s.z),2));
            d2 = (float)Math.sqrt((Math.pow((p2.x-RayCopy.s.x),2))+Math.pow((p2.y-RayCopy.s.y),2)+Math.pow((p2.z-RayCopy.s.z),2));

            if(d1<d2)
            {
                norm = new Vector4f(getNormalSphere(p1));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                Vector4f pointCopy = new Vector4f(p1);
                fposition = new Vector4f(pointCopy);
                fposition.mul(model,fposition);

                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p1,ls,model).toInt(),new Vector4f(normalView,0), isReflective,this.material);
            }
            else
            {
                norm = new Vector4f(getNormalSphere(p2));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                Vector4f pointCopy = new Vector4f(p2);
                fposition = new Vector4f(pointCopy);
                fposition.mul(model,fposition);
                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p2,ls,model).toInt(),new Vector4f(normalView,0), isReflective, this.material);
            }
        }
        else
        {
            float tmax, tmin;
            float A,B,C;
            A = (float)(Math.pow((double)(RayCopy.v.x), 2)) + (float)(Math.pow((double)(RayCopy.v.z), 2));
            B = (2*(RayCopy.v.x*RayCopy.s.x))+ (2*(RayCopy.v.z*RayCopy.s.z));
            C = (float)(Math.pow((double)(RayCopy.s.x), 2)) + (float)(Math.pow((double)(RayCopy.s.z), 2)) - (float)Math.pow(1,2);

            if((float)((Math.pow((double) B, 2))) < (4*(A*C))) {
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());
            }

            if((float)((Math.pow((double) B, 2))) == (4*(A*C))) {
                tmin = (- B - ((float)Math.sqrt((Math.pow((double)(B), 2))-(4*(A*C)))))/(2*A);
                Vector4f p1;
                p1 = new Vector4f(0,0,0,1);
                p1.x = RayCopy.s.x + (RayCopy.v.x * tmin);
                p1.y = RayCopy.s.y + (RayCopy.v.y * tmin);
                p1.z = RayCopy.s.z + (RayCopy.v.z * tmin);
                if((p1.y<=-0.5 && p1.y>=0.5))
                    return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());
                Vector4f pointCopy = new Vector4f(p1);
                fposition = new Vector4f(pointCopy);
                fposition.mul(model,fposition);
                norm = new Vector4f(getNormalCylinder(p1));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p1,ls,model).toInt(),new Vector4f(normalView,0), isReflective, this.material);
            }

            tmin = (- B - ((float)Math.sqrt((Math.pow((double)(B), 2))-(4*(A*C)))))/(2*A);
            tmax = (- B + ((float)Math.sqrt((Math.pow((double)(B), 2))-(4*(A*C)))))/(2*A);

//            if(tmin<tmax)
//            {
//                float temp = tmin;
//                tmin = tmax;
//                tmax = temp;
//            }

            if(tmin<0)
            {
                tmin = tmax;
                if(tmin<=0)
                    return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());
            }

            //Points of intersection
            Vector4f p1, p2;
            p1 = new Vector4f(0,0,0,1);
            p1.x = RayCopy.s.x + (RayCopy.v.x * tmin);
            p1.y = RayCopy.s.y + (RayCopy.v.y * tmin);
            p1.z = RayCopy.s.z + (RayCopy.v.z * tmin);

            if((p1.y<=-0.5 && p1.y>=0.5))
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());


            p2 = new Vector4f(0,0,0,1);
            p2.x = RayCopy.s.x + (RayCopy.v.x * tmax);
            p2.y = RayCopy.s.y + (RayCopy.v.y * tmax);
            p2.z = RayCopy.s.z + (RayCopy.v.z * tmax);

            if((p2.y<=-0.5 && p2.y>=0.5))
                return new Point(new Vector4f(0,0,0,1),new Color(-1,-1,-1).toInt());



            float d1,d2;
            d1 = (float)Math.sqrt((Math.pow((p1.x-RayCopy.s.x),2))+Math.pow((p1.y-RayCopy.s.y),2)+Math.pow((p1.z-RayCopy.s.z),2));
            d2 = (float)Math.sqrt((Math.pow((p2.x-RayCopy.s.x),2))+Math.pow((p2.y-RayCopy.s.y),2)+Math.pow((p2.z-RayCopy.s.z),2));

            if(d1<d2)
            {
                norm = new Vector4f(getNormalCylinder(p1));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                Vector4f pointCopy = new Vector4f(p1);
                fposition = new Vector4f(pointCopy);
                fposition.mul(model,fposition);
                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p1,ls,model).toInt(),new Vector4f(normalView,0), isReflective,this.material);
            }
            else
            {
                norm = new Vector4f(getNormalCylinder(p2));
                norm.mul(model,norm).normalize();
                norm.mul(normalMatrix);
                norm.normalize();
                normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
                Vector4f pointCopy = new Vector4f(p2);
                fposition = new Vector4f(pointCopy);
                fposition.mul(model,fposition);
                return new Point(new Vector4f(fposition.x(),fposition.y(),fposition.z(),1),shade(p2,ls,model).toInt(),new Vector4f(normalView,0), isReflective, this.material);
            }
        }
    }

    private Color shade(Vector4f p1, ArrayList<Light> ls, Matrix4f modelView)
    {

        Matrix4f model = new Matrix4f(modelView);
        int si;
        Color c = new Color(0,0,0);
        if(this.objInstanceName.equals("Box"))
            norm = new Vector4f(getNormalBox(p1));
        else if(this.objInstanceName.equals("Sphere"))
            norm = new Vector4f(getNormalSphere(p1));
        else
            norm = new Vector4f(getNormalCylinder(p1));
        norm.mul(model,norm);
        fposition = new Vector4f(p1);
        fposition.mul(model,fposition);
        Matrix4f normalMatrix = new Matrix4f(model);
        normalMatrix.invert().transpose();

        for(int i=0;i<ls.size();i++)
        {
            if(ls.get(i).getPosition().w != 0)
            {
                lv = new Vector3f(ls.get(i).getPosition().x - fposition.x,
                                  ls.get(i).getPosition().y - fposition.y,
                                  ls.get(i).getPosition().z - fposition.z);
                lv = lv.normalize();
                si = 1;
            }
            else
            {
                lv = new Vector3f(-ls.get(i).getPosition().x,
                                  -ls.get(i).getPosition().y,
                                  -ls.get(i).getPosition().z);
                lv = lv.normalize();
                // Calculation of si
                Vector4f dVec = new Vector4f(ls.get(i).getSpotDirection()).normalize();
                float phi = (float)Math.toDegrees(Math.acos(dVec.dot(-lv.x,-lv.y,-lv.z,0)));
                float Theta = (float)(ls.get(i).getSpotCutoff());
                if(phi<Theta)
                    si=1;
                else
                    si=0;
            }

            norm.mul(normalMatrix);
            norm.normalize();
            normalView = new Vector3f(norm.x,norm.y,norm.z).normalize();
            nDotl = normalView.dot(lv);
            viewVec = new Vector3f(-fposition.x,-fposition.y,-fposition.z).normalize();
            Vector3f negLight = new Vector3f(-lv.x,-lv.y,-lv.z).normalize();
            reflectVec = new Vector3f(negLight.reflect(normalView).normalize());

            rDotv = (Math.max(reflectVec.dot(viewVec),0.0f));

            amb = new Vector3f(material.getAmbient().x * ls.get(i).getAmbient().x,
                               material.getAmbient().y * ls.get(i).getAmbient().y,
                               material.getAmbient().z * ls.get(i).getAmbient().z);
            c.addColor(amb.x,amb.y,amb.z);
            dif = new Vector3f(material.getDiffuse().x * ls.get(i).getDiffuse().x * Math.max(nDotl,0.0f),
                               material.getDiffuse().y * ls.get(i).getDiffuse().y * Math.max(nDotl,0.0f),
                               material.getDiffuse().z * ls.get(i).getDiffuse().z * Math.max(nDotl,0.0f));
            c.addColor(dif.x,dif.y,dif.z);

            if(nDotl>0)
            {
                spec = new Vector3f(material.getSpecular().x * ls.get(i).getSpecular().x * (float)Math.pow(rDotv,material.getShininess()),
                                    material.getSpecular().y * ls.get(i).getSpecular().y * (float)Math.pow(rDotv,material.getShininess()),
                                    material.getSpecular().z * ls.get(i).getSpecular().z * (float)Math.pow(rDotv,material.getShininess()));
                c.addColor(spec.x, spec.y, spec.z);
            }
            c.mul(si,si,si);
        }
        c.mul(material.getAbsorption(),material.getAbsorption(),material.getAbsorption());
        //Texture code commented,
//        Vector4f texVector;
//        TextureImage tex = textures.get(textureName);
//
//        if(objInstanceName.equals("Sphere"))
//            texVector  = new Vector4f(getSphereTexture(p1,tex));
//        else
//            texVector = new Vector4f(getBoxTexture(p1,tex));
//        Vector4f texColor = tex.getColor(texVector.x,texVector.y);
//        c.mul(texColor.x,texColor.y,texColor.z);
//        System.out.println("Shade ends");
        return c;
    }

    private Vector4f getNormalBox(Vector4f p1)
    {
        if(p1.x <= 0.51 && p1.x >= 0.49)
            return new Vector4f(1,0,0,0).normalize();
        else if(p1.x >= -0.51 && p1.x <= -0.49)
            return new Vector4f(-1,0,0,0).normalize();
        else if(p1.y <= 0.51 && p1.y >= 0.49)
            return new Vector4f(0,1,0,0).normalize();
        else if(p1.y >= -0.51 && p1.y <= -0.49)
            return new Vector4f(0,-1,0,0).normalize();
        else if(p1.z <= 0.51 && p1.z >= 0.49)
            return new Vector4f(0,0,1,0).normalize();
        else
            return new Vector4f(0,0,-1,0).normalize();
    }

    private Vector4f getNormalSphere(Vector4f p1)
    {
        return new Vector4f(p1.x,p1.y,p1.z,0).normalize();
    }

    private Vector4f getBoxTexture(Vector4f p1, TextureImage tex)
    {
        float s=0,t=0;
        if(p1.x <= 0.51 && p1.x >= 0.49)
        {
             t = (float)(0.25*(p1.y+0.5)+0.50);
             s = (float)(0.25*(p1.z+0.5)+0.25);
        }
        else if(p1.x >= -0.51 && p1.x <= -0.49)
        {
             t = (float)(0.25*(p1.y+0.5)+0.25);
             s = (float)(0.25*(p1.z+0.5)+0);
        }
        else if(p1.z <= 0.51 && p1.z >= 0.49)
        {
             t = (float)(0.25*(p1.y+0.5)+0.25);
             s = (float)(0.25*(p1.x+0.5)+0.75);
        }
        else if(p1.z >= -0.51 && p1.z <= -0.49)
        {
             t = (float)(0.25*(p1.y+0.5)+0.5);
             s = (float)(0.25*(p1.x+0.5)+0.5);
        }
        else if(p1.y <= 0.51 && p1.y >= 0.49)
        {
             t = (float)(0.25*(p1.z+0.5)+0.75);
             s = (float)(0.25*(p1.x+0.5)+0.5);
        }
        else if(p1.y >= -0.51 && p1.y <= -0.49)
        {
             t = (float)(0.25*(p1.z+0.5)+0);
             s = (float)(0.25*(p1.x+0.5)+0.25);
        }
        return new Vector4f(s,t,0,1);
        
    }
    private Vector4f getNormalCylinder(Vector4f p1)
    {
        if(p1.y==0.5)
            return new Vector4f(0,1,0,0);
        if(p1.y==-0.5)
            return new Vector4f(0,-1,0,0);
        else
        {
            return new Vector4f(-p1.x,p1.y,-p1.z,0);
        }
    }
    private Vector4f getSphereTexture(Vector4f p1, TextureImage tex)
    {
        float phi = (float)Math.asin(p1.y);
        float theta = (float)Math.atan2(-p1.z,p1.x);
        float s = (float)(theta+Math.PI)/(float)(2*Math.PI);
        float t = (float)(phi +(Math.PI/2))/(float)(Math.PI);
        return new Vector4f(s,1-t,0,1);
    }
}
