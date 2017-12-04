import com.jogamp.opengl.GL;
import org.joml.Vector4f;
import util.*;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;



public class exporter
{
    public static void main(String ar[]) throws  Exception
    {
        int rad = 10;
        int rad2 = 5;
        int h = 0;
        int k = 10;
        OutputStream out;
        List<Vector4f> positions = new ArrayList<Vector4f>();
        positions.add(new Vector4f(0,5,10,1));
        positions.add(new Vector4f(0,15,10,1));
        for(int i =0;i<=50; i++) // Loop for the base of the hat
        {
            positions.add(new Vector4f(h+(rad2*(float)Math.cos(i*2*Math.PI/50)),10, k+(rad2*(float)Math.sin(i*2*Math.PI/50)), 1.0f));
            positions.add(new Vector4f(h+(rad*(float)Math.cos(i*2*Math.PI/50)),10, k+(rad*(float)Math.sin(i*2*Math.PI/50)), 1.0f));
        }
        for(int i =0;i<=50; i++) // Loop for the Body of the hat
        {
            positions.add(new Vector4f(h+((rad2-2.5f)*(float)Math.cos(i*2*Math.PI/50)),5, k+((rad2-2.5f)*(float)Math.sin(i*2*Math.PI/50)), 1.0f));
            positions.add(new Vector4f(h+(rad2*(float)Math.cos(i*2*Math.PI/50)),10, k+(rad2*(float)Math.sin(i*2*Math.PI/50)), 1.0f));
        }
        for(int i =0;i<=50; i++) // Loop for the top of the hat
        {
            positions.add(new Vector4f(h+((rad2-2.5f)*(float)Math.cos(i*2*Math.PI/50)),5, k+((rad2-2.5f)*(float)Math.sin(i*2*Math.PI/50)), 1.0f));
        }
        for(int i =0;i<=50; i++)
        {
            positions.add(new Vector4f(h+(rad2*(float)Math.cos(i*2*Math.PI/50)),10, k+(rad2*(float)Math.sin(i*2*Math.PI/50)), 1.0f));
        }
        //set up vertex attributes (in this case we have only position)
        List<IVertexData> vertexData = new ArrayList<IVertexData>();
        VertexAttribProducer producer = new VertexAttribProducer();
        for (Vector4f pos : positions) {
            IVertexData v = producer.produce();
            v.setData("position", new float[]{pos.x,
                    pos.y,
                    pos.z,
                    pos.w
            });
            vertexData.add(v);
        }
        //Vertex Indices list
        List<Integer> indices = new ArrayList<Integer>();
        l1: for (int i =2;i<=208 ;i++)
        {
            if(i==102 || i==103)
                continue l1;
            else
            {
                indices.add(i);
                indices.add(i+1);
                indices.add(i+2);
            }
        }
        for(int i=210;i<=259;i++)
        {
            indices.add(0);
            indices.add(i);
            indices.add(i+1);
        }
        //now we create a polygon mesh object for th estand
        PolygonMesh<IVertexData> mesh;
        mesh = new PolygonMesh<IVertexData>();
        mesh.setVertexData(vertexData);
        mesh.setPrimitives(indices);
        mesh.setPrimitiveType(GL.GL_TRIANGLES);
        mesh.setPrimitiveSize(3); // for GL_LINES will need this to be 2.
        out = new FileOutputStream("src/models/ObjectOfRevolution.obj");
        util.ObjExporter.exportFile(mesh,out);
    }
}