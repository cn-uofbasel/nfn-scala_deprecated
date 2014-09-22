package ExampleRunner;

import javaservice.impl.JavaWordCount;
import scala.collection.JavaConverters;
import servicegenerator.ServiceGenerator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import scala.collection.JavaConverters.*;

public class ExampleRunner {
    public static void main(String[] args) {
        ServiceGenerator sg = new ServiceGenerator();
        JavaWordCount jvcService = new JavaWordCount();
        byte[] bc = sg.generateService(jvcService);

        List<String> cmpsList = JavaConverters.asJavaListConverter(jvcService.ccnName().cmpsList()).asJava();
        StringBuilder sb = new StringBuilder();

        for (String cmp: cmpsList) {
            sb.append(cmp);
        }

        String filename = "./service-library/" + sb.toString();
        File f = new File(filename);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            bos.write(bc);
            bos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fos != null) try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.print("wrote bc size=" + bc.length + " to " + filename);
    }
}
