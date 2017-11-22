package sgraph;

import com.jogamp.opengl.GL3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import util.IVertexData;
import util.Light;
import util.PolygonMesh;
import util.TextureImage;

import javax.imageio.ImageIO;
import javax.swing.plaf.LabelUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
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
    public List<Integer> raytrace(int width, int height, Stack<Matrix4f> modelview) throws Exception
    {
        List<Integer> hitRecord = new ArrayList<>();
        File file =  new File("Image.png");
        BufferedImage out = new BufferedImage(800,800,BufferedImage.TYPE_INT_RGB);
        //lights
        ArrayList<Light> lights = new ArrayList<Light>();
        //ray
        Vector4f s = new Vector4f(0,0,0,1);//view co-ordinate system
        Vector4f v;
        for(int y=0;y<height;y++)
        {
            for(int x=0;x<width;x++)
            {
                v = new Vector4f(x-width/2,y-height/2,(float)(-0.5*height/Math.tan(Math.toRadians(30))),0);
                Matrix4f mult = modelview.peek();
                //s = s.mul(mult); // view-to-world
                //v = v.mul(mult); // view-to-world

                util.Ray r1 = new util.Ray(s,v);//world coordinate System
                int color = root.rayCast(r1, modelview, lights);
                //color = color*100;
                out.setRGB(x,y,color);
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
