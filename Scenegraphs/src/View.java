import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import org.joml.*;
import org.omg.PortableInterceptor.INACTIVE;
import sgraph.INode;
import sgraph.Scenegraph;
import util.Light;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly
 * encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
  private int WINDOW_WIDTH, WINDOW_HEIGHT;
  public Stack<Matrix4f> modelView;
  public Matrix4f projection, trackballTransform;
  public Matrix3f ambient,diffuse, specular,shine;
  private float trackballRadius;
  private Vector2f mousePos;
  float x=0;
  float z=180;
  float xl=0,zl=0;
  float ang=0;
  float ang2 = 0;
  float ang3 = 0;
  float zoom =120;
  boolean isY = false;
  boolean isR = true;
  int inf=0;
  int i=100;
  float time = 0;
  //private List<Light> lights;
  private util.ShaderProgram program;
  private util.ShaderLocationsVault shaderLocations;
  private int projectionLocation;
  public  ArrayList<util.Light> lights;
  private int numLightsLocation;
  public int whichLight;
  boolean isE = false;

  private sgraph.IScenegraph<VertexAttrib> scenegraph;
    private sgraph.IScenegraph<VertexAttrib> scenegraph_clone;
  private sgraph.IScenegraph<VertexAttrib> scene;
  private sgraph.IScenegraph<VertexAttrib> table_scene;
    private sgraph.IScenegraph<VertexAttrib> empty_SG;
  AWTGLReadBufferUtil screenCaptureUtil;



  public View() {
      projection = new Matrix4f();
      lights = new ArrayList<util.Light>();
      modelView = new Stack<Matrix4f>();
      trackballRadius = 300;
      trackballTransform = new Matrix4f();
      scenegraph = null;
      screenCaptureUtil = null;
      scenegraph_clone = new Scenegraph();
      empty_SG = new Scenegraph<>();
  }

  public void initScenegraph(GLAutoDrawable gla, InputStream in) throws Exception {
    GL3 gl = gla.getGL().getGL3();

    if (scenegraph != null)
      scenegraph.dispose();

    program.enable(gl);
    sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
    renderer.setContext(gla);
   scenegraph = sgraph.SceneXMLReader.importScenegraph(in, new VertexAttribProducer());
    Map<String, String> shaderVarsToVertexAttribs = new HashMap<String, String>();
    shaderVarsToVertexAttribs.put("vPosition", "position");
    shaderVarsToVertexAttribs.put("vNormal", "normal");
    shaderVarsToVertexAttribs.put("vTexCoord", "texcoord");
    renderer.initShaderProgram(program, shaderVarsToVertexAttribs);
    scenegraph.setRenderer(renderer);
    scenegraph.setTextures();

//      scenegraph_clone.makeScenegraph(scenegraph.getRoot());
//      shaderVarsToVertexAttribs.put("vPosition", "position");
//      shaderVarsToVertexAttribs.put("vNormal", "normal");
//      shaderVarsToVertexAttribs.put("vTexCoord", "texcoord");
//      renderer.initShaderProgram(program, shaderVarsToVertexAttribs);
//      scenegraph_clone.setRenderer(renderer);
//      scenegraph_clone.setTextures();
//
//
//      table_scene = sgraph.SceneXMLReader.importScenegraph(getClass().getClassLoader()
//              .getResourceAsStream
//                      ("scenegraphmodels/table-scene.xml"), new VertexAttribProducer());
//      renderer = new sgraph.GL3ScenegraphRenderer();
//      renderer.setContext(gla);
//      shaderVarsToVertexAttribs = new HashMap<String, String>();
//      shaderVarsToVertexAttribs.put("vPosition", "position");
//      shaderVarsToVertexAttribs.put("vNormal", "normal");
//      shaderVarsToVertexAttribs.put("vTexCoord", "texcoord");
//      renderer.initShaderProgram(program, shaderVarsToVertexAttribs);
//      table_scene.setRenderer(renderer);
//      table_scene.setTextures();
//
//    scene = sgraph.SceneXMLReader.importScenegraph(getClass().getClassLoader()
//            .getResourceAsStream
//                    ("scenegraphmodels/scene.xml"), new VertexAttribProducer());
//   renderer = new sgraph.GL3ScenegraphRenderer();
//    renderer.setContext(gla);
//   shaderVarsToVertexAttribs = new HashMap<String, String>();
//    shaderVarsToVertexAttribs.put("vPosition", "position");
//    shaderVarsToVertexAttribs.put("vNormal", "normal");
//    shaderVarsToVertexAttribs.put("vTexCoord", "texcoord");
//    renderer.initShaderProgram(program, shaderVarsToVertexAttribs);
//    scene.setRenderer(renderer);
//    scene.setTextures();
    program.disable(gl);
  }



  public void init(GLAutoDrawable gla) throws Exception {
    GL3 gl = gla.getGL().getGL3();
    lights = new ArrayList<Light>();
    //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
    program = new util.ShaderProgram();
      program.createProgram(gl, "shaders/phong-multiple.vert",
              "shaders/phong-multiple.frag");
      shaderLocations = program.getAllShaderVariables(gl);
    //get input variables that need to be given to the shader program
    projectionLocation = shaderLocations.getLocation("projection");

        }

  public void draw(GLAutoDrawable gla) throws Exception {

    GL3 gl = gla.getGL().getGL3();
      FloatBuffer fb = Buffers.newDirectFloatBuffer(16);
      FloatBuffer fb4 = Buffers.newDirectFloatBuffer(4);

    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
    gl.glEnable(gl.GL_DEPTH_TEST);

      program.enable(gl);

      modelView.push(new Matrix4f());
      lights.addAll(scenegraph.getLights(scenegraph.getRoot(),modelView));
//      lights.addAll(table_scene.getLights(table_scene.getRoot(),modelView));
//      lights.addAll(scene.getLights(scene.getRoot(),modelView));
//      lights.addAll(scenegraph.getLights(scenegraph.getRoot(),modelView));


      modelView.peek().lookAt(new Vector3f(0,0,10), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));



      trackballTransform = new Matrix4f();
//              .translate(20,50,zoom)
//              .rotate(-ang2,1,0,0)// up and down
//              .translate(0,0,0);
    /*
     *Supply the shader with all the matrices it expects.
    */

    gl.glUniformMatrix4fv(projectionLocation, 1, false, projection.get(fb));

    if(!isE)
    {
        scenegraph.draw(modelView);
        scenegraph.raytrace(800,800,modelView);
//        scenegraph.animate(i);
//        scenegraph_clone.explode(0);
    }

//    table_scene.draw(modelView);
//    scene.draw(modelView);
//     if(isE)
//      {
//          scenegraph_clone.draw(modelView);
//          scenegraph_clone.explode(10);
//      }


    gl.glFlush();
    modelView.pop();

    program.disable(gl);


  }
  public void captureFrame(String filename,GLAutoDrawable gla) throws
          FileNotFoundException,IOException
  {
    if (screenCaptureUtil==null)
    {
      screenCaptureUtil = new AWTGLReadBufferUtil(gla.getGLProfile(),false);
    }

    File f = new File(filename);
    GL3 gl = gla.getGL().getGL3();

    BufferedImage image = screenCaptureUtil.readPixelsToBufferedImage(gl,true);
    OutputStream file = null;
    file = new FileOutputStream(filename);

    ImageIO.write(image,"png",file);

  }

  public void mousePressed(int x, int y) {
    mousePos = new Vector2f(x, y);
  }

  public void mouseReleased(int x, int y) {
    System.out.println("Released");
      }

  public void mouseDragged(int x, int y) {

  }

  public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
    GL gl = gla.getGL();
    WINDOW_WIDTH = width;
    WINDOW_HEIGHT = height;
    gl.glViewport(0, 0, width, height);

    projection = new Matrix4f().perspective((float)Math.toRadians(60.0f), (float) width / height, 0.1f, 10000.0f);
    // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

  }

  public void dispose(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();

  }


}
