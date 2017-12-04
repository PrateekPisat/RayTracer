package sgraph;

import com.jogamp.opengl.GLAutoDrawable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import util.*;

import java.util.*;

/**
 * This node represents a transformation in the scene graph. It has only one child. The transformation
 * can be viewed as changing from its child's coordinate system to its parent's coordinate system
 * This also stores an animation transform that can be tweaked at runtime
 * @author Amit Shesh
 */
public class TransformNode extends AbstractNode
{

    /**
     * Matrices storing the static and animation transformations separately, so that they can be
     * changed separately
     */
    protected Matrix4f transform,animation_transform;
    protected ArrayList<Light> lights;

    protected Vector4f leftBotFront,leftTopFront, leftBotBack, leftTopBack;
    protected Vector4f rightBotFront,rightTopFront, rightBotBack, rightTopBack, center;

    /**
     * A reference to its only child
     */
    INode child;

    public TransformNode(IScenegraph graph,String name)
    {
        super(graph,name);
        this.transform = new Matrix4f();
        animation_transform = new Matrix4f();
        child = null;
        lights = new ArrayList<Light>();
        leftBotFront = new Vector4f();
        leftTopFront = new Vector4f();
        leftBotBack = new Vector4f();
        leftTopBack = new Vector4f();
        rightBotFront  = new Vector4f();
        rightTopFront  = new Vector4f();
        rightBotBack  = new Vector4f();
        rightTopBack  = new Vector4f();
        center = new Vector4f();
    }

    /**
     * Creates a deep copy of the subtree rooted at this node
     * @return a deep copy of the subtree rooted at this node
     */
    @Override
    public INode clone()
    {
        INode newchild;

        if (child!=null)
        {
            newchild = child.clone();
        }
        else
        {
            newchild = null;
        }

        TransformNode newtransform = new TransformNode(scenegraph,name);
        newtransform.setTransform(this.transform);
        newtransform.setAnimationTransform(animation_transform);

        if (newchild!=null)
        {
            try
            {
                newtransform.addChild(newchild);
            }
            catch (IllegalArgumentException e)
            {

            }
        }
        return newtransform;
    }

    /**
     * Determines if this node has the specified name and returns itself if so. Otherwise it recurses
     * into its only child
     * @param name name of node to be searched
     * @return
     */
    public INode getNode(String name)
    {
        INode n = super.getNode(name);
        if (n!=null)
        return n;

        if (child!=null)
        {
            return child.getNode(name);
        }

        return null;
    }

    /**
     * Since this node can have a child, it override this method and adds the child to itself
     * This will overwrite any children set for this node previously.
     * @param child the child of this node
     * @throws IllegalArgumentException this method does not throw this exception
     */
    public void addChild(INode child) throws IllegalArgumentException
    {
        if (this.child!=null)
            throw new IllegalArgumentException("Transform node already has a child");
        this.child = child;
        this.child.setParent(this);
    }

    /**
     * Draws the scene graph rooted at this node
     * After preserving the current top of the modelview stack, this "post-multiplies" its
     * animation transform and then its transform in that order to the top of the model view
     * stack, and then recurses to its child. When the child is drawn, it restores the modelview
     * matrix
     * @param context the generic renderer context {@link sgraph.IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     */

    @Override
    public void draw(IScenegraphRenderer context,Stack<Matrix4f> modelView)
    {
        modelView.push(new Matrix4f(modelView.peek()));
        modelView.peek().mul(animation_transform)
                        .mul(transform);
        if (child!=null)
            child.draw(context,modelView);
        modelView.pop();

    }


    /**
     * Sets the animation transform of this node
     * @param mat the animation transform of this node
     */
    public void setAnimationTransform(Matrix4f mat)
    {
        animation_transform = new Matrix4f(mat);
    }

    /**
     * Gets the transform at this node (not the animation transform)
     * @return
     */
    public Matrix4f getTransform()
    {
        return transform;
    }

    /**
     * Sets the transformation of this node
     * @param t
     * @throws IllegalArgumentException
     */
    @Override
    public void setTransform(Matrix4f t)throws IllegalArgumentException
    {
        this.transform = new Matrix4f(t);
    }

    /**
     * Gets the animation transform of this node
     * @return
     */
    Matrix4f getAnimationTransform()
    {
        return animation_transform;
    }

    /**
     * Sets the scene graph object of which this node is a part, and then recurses to its child
     * @param graph a reference to the scenegraph object of which this tree is a part
     */
    @Override
    public void setScenegraph(IScenegraph graph)
    {
        super.setScenegraph(graph);
        if (child!=null)
        {
            child.setScenegraph(graph);
        }
    }
    public void addLight(util.Light l)
    {
        lights.add(l);
    }


    @Override
    public ArrayList<Light> getLights(Stack<Matrix4f> modelView) {
        ArrayList<Light> ll = new ArrayList<Light>();
        modelView.push(new Matrix4f(modelView.peek()));
        modelView.peek().mul(transform);
        for(int i = 0; i < lights.size(); i++) {
            Light l = lights.get(i);
            l.setPosition(l.getPosition().mul(transform));
            ll.add(l);
        }
        ArrayList<Light> cll = new ArrayList<Light>();
                cll.addAll(child.getLights(modelView));
        for(int j = 0; j < cll.size(); j++) {
            Light l = cll.get(j);
            l.setPosition(l.getPosition());
            ll.add(l);
        }
        modelView.pop();
        return ll;
    }

    @Override
    public Vector4f getMinBounds() {
    return child.getMinBounds().mul(getTransform()).mul(getAnimationTransform());
    }

    @Override
    public Vector4f getMaxBounds() {

            return child.getMaxBounds().mul(getTransform()).mul(getAnimationTransform());
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
        if(child != null)
        {
            child.setBoundingBox();
            // Min Bounds
            if ( leftBotFront.x > child.getMinBounds().mul(getTransform()).mul(getAnimationTransform()).x)
                 leftBotFront.x = child.getMinBounds().mul(getTransform()).mul(getAnimationTransform()).x;
            if ( leftBotFront.y > child.getMinBounds().mul(getTransform()).mul(getAnimationTransform()).y)
                 leftBotFront.y = child.getMinBounds().mul(getTransform()).mul(getAnimationTransform()).y;
            if ( leftBotFront.z > child.getMinBounds().mul(getTransform()).mul(getAnimationTransform()).z)
                 leftBotFront.z = child.getMinBounds().mul(getTransform()).mul(getAnimationTransform()).z;

            // MaxBounds
            if ( rightBotFront.x < child.getMaxBounds().mul(getTransform()).mul(getAnimationTransform()).x)
                 rightBotFront.x = child.getMaxBounds().mul(getTransform()).mul(getAnimationTransform()).x;
            if (rightBotFront.y < child.getMaxBounds().mul(getTransform()).mul(getAnimationTransform()).y)
                 rightBotFront.y = child.getMaxBounds().mul(getTransform()).mul(getAnimationTransform()).y;
            if ( rightBotFront.z < child.getMaxBounds().mul(getTransform()).mul(getAnimationTransform()).z)
                 rightBotFront.z = child.getMaxBounds().mul(getTransform()).mul(getAnimationTransform()).z;

            leftBotFront = new Vector4f(leftBotBack.x, leftBotBack.y, rightTopFront.z,1).mul(getTransform()).mul(getAnimationTransform());
            leftTopBack = new Vector4f(leftBotBack.x, rightTopFront.y, leftBotBack.z,1).mul(getTransform()).mul(getAnimationTransform());
            leftTopFront = new Vector4f(leftBotBack.x,rightTopFront.y,rightTopFront.z,1).mul(getTransform()).mul(getAnimationTransform());
            rightBotFront = new Vector4f(rightTopFront.x,leftBotBack.y,rightTopFront.z,1).mul(getTransform()).mul(getAnimationTransform());
            rightTopBack = new Vector4f(rightTopFront.x, leftBotBack.y,leftBotBack.z, 1).mul(getTransform()).mul(getAnimationTransform());
            rightTopBack = new Vector4f(rightTopFront.x,rightTopFront.y,leftBotBack.z, 1).mul(getTransform()).mul(getAnimationTransform());



        }
        Vector4f center = new Vector4f(
                (leftBotBack.x + rightTopFront.x)/2,
                (leftBotBack.y + rightTopFront.y)/2,
                (leftBotBack.z + rightTopFront.z)/2,
                1
        );

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
        Vector4f center = setcenter(bot, top);

        return center;
    }

    protected Vector4f setcenter(Vector4f v1, Vector4f v2) {
        Vector4f center = new Vector4f(((v1.x+v2.x)/2), ((v1.y+v2.y)/2), ((v1.z+v2.z)/2), ((v1.w+v2.w)/2));
        return center;
    }

    @Override
    public INode explodeNode(Vector4f pc) {
        INode exploded = new TransformNode(scenegraph, "exploded: "+name);
        INode tempchild = child.clone();
        INode translatedchild = tempchild.explodeNode(center);
        Vector4f dir = new Vector4f(center.x-pc.x, center.y-pc.y, center.z-pc.z, 1).normalize();
        exploded.setTransform(new Matrix4f().translate(10*dir.x, 10*dir.y, 10*dir.z));
        exploded.addChild(translatedchild);
        return exploded;
    }

    @Override
    public void setMeshes(Map<String,PolygonMesh> meshes)  {

    }

    public IScenegraph expload(IScenegraph sg, IScenegraph blank)
    {
        INode node = new TransformNode(sg,"Exploaded "+name);
        node.setTransform(new Matrix4f().translate(-10,0,0));
        node.addChild(this.clone());
        blank.addNode("Exploaded"+name,node);
        blank = child.expload(sg, blank);
        return  blank;
    }

    public Point rayCast(Ray r1, Stack<Matrix4f> modelview, ArrayList<Light> ls)
    {
        int color;
        Point p1;
        modelview.push(new Matrix4f(modelview.peek()));
        modelview.peek().mul(transform);
            p1 = child.rayCast(r1,modelview, ls);
        modelview.pop();
        return p1;
    }
}