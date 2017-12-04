package sgraph;

import com.jogamp.opengl.GLAutoDrawable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import util.*;
import util.Vector;

import java.util.*;

/**
 * This class represents a group node in the scenegraph. A group node is simply a logical grouping
 * of other nodes. It can have an arbitrary number of children. Its children can be nodes of any type
 * @author Amit Shesh
 */
public class GroupNode extends AbstractNode {
    /**
     * A list of its children
     */
    protected List<INode> children;
    protected ArrayList<Light> lights;

    protected Vector4f leftBotFront,leftTopFront, leftBotBack, leftTopBack;
    protected Vector4f rightBotFront,rightTopFront, rightBotBack, rightTopBack;

    protected Vector4f center;

    public GroupNode(IScenegraph graph, String name) {
        super(graph, name);
        children = new ArrayList<INode>();
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
     * Searches recursively into its subtree to look for node with specified name.
     *
     * @param name name of node to be searched
     * @return the node whose name this is if it exists within this subtree, null otherwise
     */
    @Override
    public INode getNode(String name) {
        INode n = super.getNode(name);
        if (n != null) {
            return n;
        }

        int i = 0;
        INode answer = null;

        while ((i < children.size()) && (answer == null)) {
            answer = children.get(i).getNode(name);
            i++;
        }
        return answer;
    }

    /**
     * Sets the reference to the scene graph object for this node, and then recurses down
     * to children for the same
     *
     * @param graph a reference to the scenegraph object of which this tree is a part
     */
    @Override
    public void setScenegraph(IScenegraph graph) {
        super.setScenegraph(graph);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).setScenegraph(graph);
        }
    }

    /**
     * To draw this node, it simply delegates to all its children
     *
     * @param context   the generic renderer context {@link sgraph.IScenegraphRenderer}
     * @param modelView the stack of modelview matrices
     */
    @Override
    public void draw(IScenegraphRenderer context, Stack<Matrix4f> modelView) {
        for (int i = 0; i < children.size(); i++) {

            children.get(i).draw(context, modelView);
        }

    }

    /**
     * Makes a deep copy of the subtree rooted at this node
     *
     * @return a deep copy of the subtree rooted at this node
     */
    @Override
    public INode clone() {
        ArrayList<INode> newc = new ArrayList<INode>();

        for (int i = 0; i < children.size(); i++) {
            newc.add(children.get(i).clone());
        }

        GroupNode newgroup = new GroupNode(scenegraph, name);

        for (int i = 0; i < children.size(); i++) {
            try {
                newgroup.addChild(newc.get(i));
            } catch (IllegalArgumentException e) {

            }
        }
        return newgroup;
    }

    /**
     * Since a group node is capable of having children, this method overrides the default one
     * in {@link sgraph.AbstractNode} and adds a child to this node
     *
     * @param child
     * @throws IllegalArgumentException this class does not throw this exception
     */
    @Override
    public void addChild(INode child) throws IllegalArgumentException {
        children.add(child);
        child.setParent(this);
    }

    /**
     * Get a list of all its children, for convenience purposes
     *
     * @return a list of all its children
     */

    public List<INode> getChildren() {
        return children;
    }

    public void addLight(Light l) {
        lights.add(l);
    }


    @Override
    public ArrayList<Light> getLights(Stack<Matrix4f> modelView) {
        ArrayList<Light> ll = new ArrayList<Light>();
        for(int i = 0; i < lights.size(); i++) {
            Light l = lights.get(i);
            l.setPosition(l.getPosition());
            ll.add(l);
        }
        for(int j = 0; j < children.size(); j++) {
            ArrayList<Light> cll = new ArrayList<Light>();
                    cll.addAll(children.get(j).getLights(modelView));
            for(int k = 0; k < cll.size(); k++) {
                Light l = cll.get(k);
                l.setPosition(l.getPosition());
                ll.add(l);
            }
        }
        return ll;
    }

    @Override
    public Vector4f getMinBounds() {
        return leftBotFront;
    }

    @Override
    public Vector4f getMaxBounds() {
        return rightBotFront;
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

    public void setBoundingBox()
    {
        if(children.size() != 0)
        {

        for(int i=0;i<children.size();i++)
        {
            children.get(i).setBoundingBox();
            //Min Bounds
            if ( leftBotFront.x > children.get(i).getMinBounds().x)
                leftBotFront.x = children.get(i).getMinBounds().x;
            if ( leftBotFront.y > children.get(i).getMinBounds().y)
                leftBotFront.y = children.get(i).getMinBounds().y;
            if ( leftBotFront.z > children.get(i).getMinBounds().z)
                leftBotFront.z = children.get(i).getMinBounds().z;

            // MaxBounds
            if ( rightTopBack.x < children.get(i).getMaxBounds().x)
                rightTopBack.x = children.get(i).getMaxBounds().x;
            if (rightTopBack.y < children.get(i).getMaxBounds().y)
                rightTopBack.y = children.get(i).getMaxBounds().y;
            if ( rightTopBack.z < children.get(i).getMaxBounds().z)
                rightTopBack.z = children.get(i).getMaxBounds().z;

            leftBotFront = new Vector4f(leftBotBack.x, leftBotBack.y, rightTopFront.z,1);
            leftTopBack = new Vector4f(leftBotBack.x, rightTopFront.y, leftBotBack.z,1);
            leftTopFront = new Vector4f(leftBotBack.x,rightTopFront.y,rightTopFront.z,1);
            rightBotFront = new Vector4f(rightTopFront.x,leftBotBack.y,rightTopFront.z,1);
            rightTopBack = new Vector4f(rightTopFront.x, leftBotBack.y,leftBotBack.z, 1);
            rightTopBack = new Vector4f(rightTopFront.x,rightTopFront.y,leftBotBack.z, 1);
            }
        }
        Vector4f center = new Vector4f(
                (leftBotBack.x + rightTopFront.x)/2,
                (leftBotBack.y + rightTopFront.y)/2,
                (leftBotBack.z + rightTopFront.z)/2,
                1
        );
    }

    public INode explodeNode(Vector4f pc) {
       INode explodedG = new GroupNode(scenegraph, "exploded: "+name);
       for(int i=0; i<children.size(); i++) {
           INode child = children.get(i).clone();
           INode newChild = child.explodeNode(center);
           explodedG.addChild(newChild);
       }
       if(parent != null) {
           Vector4f dir = new Vector4f(center.x-pc.x, center.y-pc.y, center.z-pc.z, 1).normalize();
           INode translate = new TransformNode(scenegraph, "translate" + explodedG.getName());
           translate.setTransform(new Matrix4f().translate(10*dir.x, 10*dir.y, 10*dir.z));
           translate.addChild(explodedG);
           return translate;
       } else {
           return explodedG;
       }
    }
    @Override
    public void setMeshes(Map<String,PolygonMesh> meshes) {

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

    public IScenegraph expload(IScenegraph sg, IScenegraph blank)
    {
        INode node = new TransformNode(blank,"Exploaded "+name);
        node.setTransform(new Matrix4f().translate(-10,0,0));
        node.addChild(this.clone());
        blank.addNode("Exploaded"+name, node);
        for(int i=0;i<children.size();i++)
        {
            blank = children.get(i).expload(sg, blank);
        }
        return blank;
    }

    public Point rayCast(Ray r1, Stack<Matrix4f> modelview, ArrayList<Light> ls)
    {
        float shortest = 999999.0f;
        Point p1;
        ArrayList<Point> hitRecord = new ArrayList<>();
        ArrayList<Float> distList = new ArrayList<>();
        for(int i=0;i<children.size();i++)
        {
            p1 = children.get(i).rayCast(r1, modelview, ls);
            if (p1.color > 0)
            {
                hitRecord.add(p1);
                float dist = (float)Math.sqrt(Math.pow(p1.x-r1.s.x,2)+Math.pow(p1.y-r1.s.y,2)+Math.pow(p1.z-r1.s.z,2));
                distList.add(dist);
            }
        }
        if(hitRecord.size()>0)
        {
            for(int i=0;i<distList.size();i++)
            {
                if(distList.get(i)<shortest)
                    shortest = distList.get(i);
            }
            return hitRecord.get(distList.indexOf(shortest));
        }
        return new Point(new Vector4f(0,0,0,1), new Color(-1,-1,-1).toInt());
    }
}
