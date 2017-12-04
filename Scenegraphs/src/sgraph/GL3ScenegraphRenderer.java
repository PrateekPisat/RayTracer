package sgraph;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import util.IVertexData;
import util.TextureImage;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.*;

/**
 * This is a scene graph renderer implementation that works specifically with the JOGL library
 * It mandates OpenGL 3 and above.
 * @author Amit Shesh
 */
public class GL3ScenegraphRenderer implements IScenegraphRenderer {
    /**
     * The JOGL specific rendering context
     */
    protected GLAutoDrawable glContext;
    /**
     * A table of shader locations and variable names
     */
    protected util.ShaderLocationsVault shaderLocations;
    /**
     * A table of shader variables -> vertex attribute names in each mesh
     */
    protected Map<String,String> shaderVarsToVertexAttribs;

    protected Scenegraph scenegraph;

    /**
     * A map to store all the textures
     */
    protected Map<String, TextureImage> textures;
    private util.ShaderProgram program;
    /**
     * A table of renderers for individual meshes
     */
    protected Map<String,util.ObjectInstance> meshRenderers;
    private List<LightLocation> lightLocations;
    public ArrayList<util.Light> lights = new ArrayList<util.Light>();
    public util.Light l;
    public int numLightsLocation;
    private int modelviewLocation, projectionLocation, normalmatrixLocation, texturematrixLocation, textureLocation;
    public TextureImage ti;
    private Matrix4f textureTransform;

    /**
     * A variable tracking whether shader locations have been set. This must be done before
     * drawing!
     */
    private boolean shaderLocationsSet;

    public GL3ScenegraphRenderer()
    {
        meshRenderers = new HashMap<String,util.ObjectInstance>();
        shaderLocations = new util.ShaderLocationsVault();
        shaderLocationsSet = false;
        lightLocations = new ArrayList<LightLocation>();
        textureTransform = new Matrix4f();
        ti = null;
        textures = new HashMap<String,TextureImage>();
    }
    class LightLocation {
        int ambient, diffuse, specular, position, spotangle, spotdirection;

        public LightLocation() {
            ambient = diffuse = specular = position = spotangle = spotdirection = -1;
        }
    }


    /**
     * Specifically checks if the passed rendering context is the correct JOGL-specific
     * rendering context {@link com.jogamp.opengl.GLAutoDrawable}
     * @param obj the rendering context (should be {@link com.jogamp.opengl.GLAutoDrawable})
     * @throws IllegalArgumentException if given rendering context is not {@link com.jogamp.opengl.GLAutoDrawable}
     */
    @Override
    public void setContext(Object obj) throws IllegalArgumentException
    {
        if (obj instanceof GLAutoDrawable)
        {
            glContext = (GLAutoDrawable)obj;
        }
        else
            throw new IllegalArgumentException("Context not of type GLAutoDrawable");
    }

    /**
     * Add a mesh to be drawn later.
     * The rendering context should be set before calling this function, as this function needs it
     * This function creates a new
     * {@link util.ObjectInstance} object for this mesh
     * @param name the name by which this mesh is referred to by the scene graph
     * @param mesh the {@link util.PolygonMesh} object that represents this mesh
     * @throws Exception
     */
    @Override
    public <K extends IVertexData> void addMesh(String name, util.PolygonMesh<K> mesh) throws Exception
    {
        if (!shaderLocationsSet)
            throw new Exception("Attempting to add mesh before setting shader variables. Call initShaderProgram first");
        if (glContext==null)
            throw new Exception("Attempting to add mesh before setting GL context. Call setContext and pass it a GLAutoDrawable first.");

        if (meshRenderers.containsKey(name))
            return;

        //verify that the mesh has all the vertex attributes as specified in the map
        if (mesh.getVertexCount()<=0)
            return;
        K vertexData = mesh.getVertexAttributes().get(0);
      GL3 gl = glContext.getGL().getGL3();

      for (Map.Entry<String,String> e:shaderVarsToVertexAttribs.entrySet()) {
            if (!vertexData.hasData(e.getValue()))
                throw new IllegalArgumentException("Mesh does not have vertex attribute "+e.getValue());
        }
      util.ObjectInstance obj = new util.ObjectInstance(gl,
              shaderLocations,shaderVarsToVertexAttribs,mesh,name);

      meshRenderers.put(name,obj);
    }

    @Override
    public void addTexture(String name,String path)
    {

        TextureImage image = null;
        GL3 gl = glContext.getGL().getGL3();
        String imageFormat = path.substring(path.indexOf('.')+1);
        try {
            image = new TextureImage(path,imageFormat,name);
        } catch (IOException e) {
            throw new IllegalArgumentException("Texture "+path+" cannot be read!");
        }
        image.getTexture().setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        image.getTexture().setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        image.getTexture().setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        image.getTexture().setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
        textures.put(name,image);
    }

    /**
     * Begin rendering of the scene graph from the root
     * @param root
     * @param modelView
     */
    @Override
    public void draw(INode root, Stack<Matrix4f> modelView)
    {

        root.draw(this,modelView);
    }

    public void setShaderLights(ArrayList<util.Light> lights) {
        //sends all the info of each light in the list to the shader
        GL3 gl = glContext.getGL().getGL3();

        numLightsLocation = shaderLocations.getLocation("numLights");
        System.out.println("Got numLights");
        FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

        for (int i = 0; i < lights.size(); i++) {
            LightLocation ll = new LightLocation();
            String name;

            name = "light[" + i + "]";
            ll.ambient = shaderLocations.getLocation(name + ".ambient");
            ll.diffuse = shaderLocations.getLocation(name + ".diffuse");
            ll.specular = shaderLocations.getLocation(name+ ".specular");
            ll.spotangle = shaderLocations.getLocation(name + ".spotAngle");
            ll.spotdirection = shaderLocations.getLocation(name+".spotDirection");
            ll.position = shaderLocations.getLocation(name+".position");
            lightLocations.add(ll);
        }
        System.out.println("Got Locations");

        for (int i = 0; i < lights.size(); i++) {
            String name;
            name = "light[" + i + "]";
            Vector4f pos = lights.get(i).getPosition();
            System.out.println("Got Position");
                gl.glUniform1f(lightLocations.get(i).spotangle, lights.get(i).getSpotCutoff());
                gl.glUniform3fv(lightLocations.get(i).spotdirection, 1, lights.get(i).getSpotDirection().get(fb4));
                gl.glUniform4fv(lightLocations.get(i).position, 1, pos.get(fb4));
            System.out.println("End of Loop");
        }

        gl.glUniform1i(numLightsLocation, lights.size());
        for (int i = 0; i < lights.size(); i++) {
            gl.glUniform3fv(lightLocations.get(i).ambient, 1, lights.get(i).getAmbient().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).diffuse, 1, lights.get(i).getDiffuse().get(fb4));
            gl.glUniform3fv(lightLocations.get(i).specular, 1, lights.get(i).getSpecular().get(fb4));
            System.out.println("End of lights functions");

        }
        //program.disable(gl);
    }


    @Override
    public void dispose()
    {
        for (util.ObjectInstance s:meshRenderers.values())
            s.cleanup(glContext);
    }
    /**
     * Draws a specific mesh.
     * If the mesh has been added to this renderer, it delegates to its correspond mesh renderer
     * This function first passes the material to the shader. Currently it uses the shader variable
     * "vColor" and passes it the ambient part of the material. When lighting is enabled, this method must
     * be overriden to set the ambient, diffuse, specular, shininess etc. values to the shader
     * @param name
     * @param material
     * @param transformation
     */
    @Override
    public void drawMesh(String name, util.Material material,String textureName,final Matrix4f transformation) {
        if (meshRenderers.containsKey(name))
        {
            GL3 gl = glContext.getGL().getGL3();

            //get the color
            FloatBuffer fb = Buffers.newDirectFloatBuffer(4);
            FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);

            ti = textures.get(textureName);
            normalmatrixLocation = shaderLocations.getLocation("normalmatrix");

            int loc = shaderLocations.getLocation("material.ambient");
            int loc1 = shaderLocations.getLocation("material.diffuse");
            int loc2 = shaderLocations.getLocation("material.specular");
            int loc3 = shaderLocations.getLocation("material.shininess");

            //set the color for all vertices to be drawn for this object
            if (loc<0)
                throw new IllegalArgumentException("No shader variable for \" ambient \"");
            if (loc1<0)
                throw new IllegalArgumentException("No shader variable for \" diffuse \"");
            if (loc2<0)
                throw new IllegalArgumentException("No shader variable for \" specular \"");
            if (loc3<0)
                throw new IllegalArgumentException("No shader variable for \" shininess \"");

            modelviewLocation = shaderLocations.getLocation("modelview");
            if (loc<0)
                throw new IllegalArgumentException("No shader variable for \" modelview \"");
            Matrix4f normalmatrix = new Matrix4f(transformation);
            normalmatrix = normalmatrix.invert().transpose();
            texturematrixLocation = shaderLocations.getLocation("texturematrix");
            textureLocation = shaderLocations.getLocation("image");

            gl.glEnable(GL.GL_TEXTURE_2D);
            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glUniform1i(textureLocation, 0);

            gl.glUniformMatrix4fv(modelviewLocation,1,false,transformation.get(fb16));
            gl.glUniformMatrix4fv(normalmatrixLocation, 1, false, normalmatrix.get(fb16));

            if (!ti.getTexture().getMustFlipVertically()) //for
            // flipping the
            // image vertically
            {
                textureTransform = new Matrix4f().translate(0, 1, 0).scale(1, -1, 1);
            } else
                textureTransform = new Matrix4f();

            textureTransform = new Matrix4f(textureTransform);
            gl.glUniformMatrix4fv(texturematrixLocation, 1, false, textureTransform.get(fb16));
            gl.glUniform3fv(loc,1,material.getAmbient().get(fb));
            gl.glUniform3fv(loc1,1,material.getDiffuse().get(fb));
            gl.glUniform3fv(loc2,1,material.getSpecular().get(fb));
            gl.glUniform1f(loc3,material.getShininess());

            ti.getTexture().setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL
                    .GL_NEAREST);
            ti.getTexture().bind(gl);
            meshRenderers.get(name).draw(glContext);
        }
    }



    /**
     * Queries the shader program for all variables and locations, and adds them to itself
     * @param shaderProgram
     */
    @Override
    public void initShaderProgram(util.ShaderProgram shaderProgram,Map<String,String> shaderVarsToVertexAttribs)
    {
        Objects.requireNonNull(glContext);
        GL3 gl = glContext.getGL().getGL3();

        shaderLocations = shaderProgram.getAllShaderVariables(gl);
        this.shaderVarsToVertexAttribs = new HashMap<String,String>(shaderVarsToVertexAttribs);
        shaderLocationsSet = true;

    }

    public void setTextures(Map<String,String> tex)
    {
        for (Map.Entry<String, String> entry : tex.entrySet())
        {
            this.addTexture(entry.getKey(),entry.getValue());
        }
    }

    public Map<String, TextureImage> getTextures()
    {
        return this.textures;
    }

    @Override
    public int getShaderLocation(String name)
    {
        return shaderLocations.getLocation(name);
    }
}