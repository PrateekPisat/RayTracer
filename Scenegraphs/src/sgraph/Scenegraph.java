package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import util.*;
import util.Color;
import util.Point;

import javax.imageio.ImageIO;
import javax.swing.plaf.LabelUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.rmi.MarshalException;
import java.util.*;
import java.util.List;

/**
 * A specific implementation of this scene graph. This implementation is still independent
 * of the rendering technology (i.e. OpenGL)
 * @author Amit Shesh
 */
public class Scenegraph<VertexType extends IVertexData> implements IScenegraph<VertexType>
{
    /**
     * The root of the scene graph tree
     */
    protected INode root;

    /**
     * A map to store the (name,mesh) pairs. A map is chosen for efficient search
     */
    protected Map<String,util.PolygonMesh<VertexType>> meshes;

    /**
     * A map to store the (name,node) pairs. A map is chosen for efficient search
     */
    protected Map<String,INode> nodes;

    protected Map<String,String> textures;

    /**
     * The associated renderer for this scene graph. This must be set before attempting to
     * render the scene graph
     */
    protected IScenegraphRenderer renderer;
    protected ArrayList<Light> lights;

    public Scenegraph()
    {
        root = null;
        meshes = new HashMap<String,util.PolygonMesh<VertexType>>();
        nodes = new HashMap<String,INode>();
        textures = new HashMap<String,String>();
        lights = new ArrayList<Light>();
    }

    public void dispose()
    {
        renderer.dispose();
    }

    /**
     * Sets the renderer, and then adds all the meshes to the renderer.
     * This function must be called when the scene graph is complete, otherwise not all of its
     * meshes will be known to the renderer
     * @param renderer The {@link IScenegraphRenderer} object that will act as its renderer
     * @throws Exception
     */
    @Override
    public void setRenderer(IScenegraphRenderer renderer) throws Exception {
        this.renderer = renderer;

        //now add all the meshes
        for (String meshName:meshes.keySet())
        {
            this.renderer.addMesh(meshName,meshes.get(meshName));
        }

    }


    /**
     * Set the root of the scenegraph, and then pass a reference to this scene graph object
     * to all its node. This will enable any node to call functions of its associated scene graph
     * @param root
     */

    @Override
    public void makeScenegraph(INode root)
    {
        this.root = root;
        this.root.setScenegraph(this);
    }

    /**
     * Draw this scene graph. It delegates this operation to the renderer
     * @param modelView
     */
    @Override
    public void draw(Stack<Matrix4f> modelView) {
        if ((root!=null) && (renderer!=null))
        {
            root.setBoundingBox();
            renderer.draw(root,modelView);
        }
    }

    public ArrayList<util.Light> getLights(INode root, Stack<Matrix4f> modelView)
    {
        if((root!=null) && (renderer!=null)) {
            renderer.setShaderLights((root.getLights(modelView)));
            return root.getLights(modelView);
        }
        return new ArrayList<>();
    }

    /**
     *
     * @param name a unique name by which this mesh may be referred to in future
     * @param mesh
     */
    @Override
    public void addPolygonMesh(String name, util.PolygonMesh<VertexType> mesh)
    {
        meshes.put(name,mesh);
    }




    @Override
    public void animate(float time)
    {
       nodes.get("helicop").setAnimationTransform(new Matrix4f().translate(time,0,0));
       nodes.get("prop").setAnimationTransform(new Matrix4f().rotate(time,0,1,0));
       nodes.get("tail-prop").setAnimationTransform(new Matrix4f()
               .translate(90,0,0)
               .rotate(time,0, 0, 1)
               .translate(-90,0,0));


    }
    @Override
    public List<Integer> raytrace(int width, int height, Stack<Matrix4f> modelview, ArrayList<Light> ls) throws Exception
    {
        List<Integer> hitRecord = new ArrayList<>();
        File file =  new File("Image.png");
        int color=0;
        Vector3f lv;
        BufferedImage out = new BufferedImage(800,800,BufferedImage.TYPE_INT_RGB);
        //ray
        Vector4f s = new Vector4f(0,0,0,1);//view co-ordinate system
        Vector4f v;
        for(int y=0;y<height;y++)
        {
            for(int x=0;x<width;x++)
            {
                v = new Vector4f(x-width/2,y-height/2,(float)(-0.5*height/Math.tan(Math.toRadians(30))),0);
                util.Ray r1 = new util.Ray(s,v);
                util.Point p1 = root.rayCast(r1, modelview, ls);
                if(p1.color<0)
                    p1.color=0;
                color = p1.color;

//                // shadow ray
                    for (int i = 0; i < ls.size(); i++) {
                        if(ls.get(i).getPosition().w != 0)
                        {
                            lv = new Vector3f(ls.get(i).getPosition().x - p1.x,
                                    ls.get(i).getPosition().y - p1.y,
                                    ls.get(i).getPosition().z - p1.z);
                            lv = lv.normalize();
                        }
                        else
                        {
                            lv = new Vector3f(-ls.get(i).getPosition().x,
                                    -ls.get(i).getPosition().y,
                                    -ls.get(i).getPosition().z);
                            lv = lv.normalize();
                        }

                        Vector4f shadowStart = new Vector4f(p1.x + (0.1f * lv.x), p1.y + (0.1f * lv.y), p1.z + (0.1f * lv.z), 1);
                        Vector4f shadowDirection = new Vector4f(lv.x,lv.y,lv.z,0);
                        Ray shadowRay = new Ray(shadowStart, shadowDirection);
                        Point p2 = root.rayCast(shadowRay, modelview, ls);
                        if (p2.color >= 0)
                        {
                            Color c = new Color(color);
                            c.mul(0.2f,0.2f,0.2f);
                            color = c.toInt();
                        }
                    }

                //reflected Ray
                if(p1.reflective)
                {
                    Vector4f reflectVec;
                    Vector3f normalView= new Vector3f(p1.normal.x,p1.normal.y,p1.normal.z).normalize();
                    Vector3f viewVector = new Vector3f(p1.x,p1.y,p1.z).normalize();
                    reflectVec = new Vector4f(viewVector.reflect(normalView),0);
                    Vector4f reflectStart = new Vector4f(p1.x + (reflectVec.x*0.1f),p1.y + (reflectVec.y*0.1f),p1.z + (reflectVec.z*0.1f),1);
                    reflectVec.normalize();
                  // System.out.println("Incomming ray= "+r1.v);
                   //System.out.println("Reflected Ray= "+reflectVec);
                    Ray reflectRay = new Ray(reflectStart,reflectVec);
                    Point p3 = root.rayCast(reflectRay,modelview,ls);
                    if(p3.color<0)
                        p3.color = new Color(0.1f,0.1f,0.1f).toInt();

                    Color nc = new Color(color);
                    Color rc = new Color(p3.color);

                    nc.addColor(p1.material.getReflection() * rc.getRed(),
                                p1.material.getReflection() * rc.getGreen(),
                                p1.material.getReflection() * rc.getBlue());
                    color = nc.toInt();
                }
                out.setRGB(x,(height-1)-y,color);
            }
        }
        ImageIO.write(out, "PNG",file);
        return hitRecord;
    }

    @Override
    public void addNode(String name, INode node) {
        nodes.put(name,node);
    }


    @Override
    public INode getRoot() {
        return root;
    }

    @Override
    public Map<String, PolygonMesh<VertexType>> getPolygonMeshes() {
       Map<String,util.PolygonMesh<VertexType>> meshes = new HashMap<String,PolygonMesh<VertexType>>(this.meshes);
        return meshes;
    }

    @Override
    public Map<String, INode> getNodes() {
        Map<String,INode> nodes = new TreeMap<String,INode>();
        nodes.putAll(this.nodes);
        return nodes;
    }

    @Override
    public void addTexture(String name, String path)
    {
       textures.put(name,path);
    }

    public Map<String, String> getTextures()
    {
        return textures;
    }

    public void setTextures()
    {
        renderer.setTextures(textures);
    }

    public IScenegraph exploded(INode root)
    {
        IScenegraph cloneSceneGraph = new Scenegraph();
        cloneSceneGraph.makeScenegraph(root.explodeNode(root.getCenter()));
        return cloneSceneGraph;
    }

    public IScenegraph update(IScenegraph SG_soFar)
    {
        return SG_soFar;
    }

    public void  explode(float time)
    {
        nodes.get("body-transform").setAnimationTransform(new Matrix4f().translate(-time,0,0));
        nodes.get("tail-transform").setAnimationTransform(new Matrix4f().translate(time,0,0));
        nodes.get("wheel-transform1").setAnimationTransform(new Matrix4f().translate(0,-time,time));
        nodes.get("wheel-transform2").setAnimationTransform(new Matrix4f().translate(0,-time,-time));
        nodes.get("wing1").setAnimationTransform(new Matrix4f().translate(0,time*3,0));
        nodes.get("wing2").setAnimationTransform(new Matrix4f().translate(0,time*2,0));
        nodes.get("prop-base").setAnimationTransform(new Matrix4f().translate(0,time,0));
        nodes.get("tail-wing1").setAnimationTransform(new Matrix4f().translate(time,time*3,0));
        nodes.get("tail-wing2").setAnimationTransform(new Matrix4f().translate(time,time*2,0));
        nodes.get("tail-base").setAnimationTransform(new Matrix4f().translate(time,time,0));
    }

}
