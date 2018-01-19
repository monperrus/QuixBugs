package junit_testcases;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.EnumSet;
import javax.lang.model.element.Modifier;
import com.squareup.javawriter.JavaWriter;

/**
 * This is a Junit test cases generator
 * @author HE YE
 *
 */
public class TestsGenerator {

	static URL path = Thread.currentThread().getContextClassLoader().getResource("");
	
	public static void main(String[] args) throws IOException{
		
		traverseFolder(path.getPath()+"json_testcases");
		
	}
		
	
	public static void traverseFolder(String path) throws IOException {
		System.out.println("traverseFolder");
        File file = new File(path);
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files.length == 0) {
                System.out.println("The folder is empty!");
                return;
            } else {
                for (File f : files) {
                    if (!f.isDirectory()) {
                    	 //Create JUnit tests for each json file
                         String filePath = f.getAbsolutePath();
                         
                         //Remove extention of file
                         String jsonName = f.getName().replaceAll("[.][^.]+$", "");;
                         String clazzName = jsonName.toUpperCase();
                         createJunitTest(filePath,clazzName);                    
                    } 
                }
            }
        } else {
            System.out.println("File is not exist!");
        }
    }
	
	private static void createJunitTest(String filePath,String clazzName) throws IOException{
		try {
		String packageName = "junit_testcases";
        String className = "TEST_"+clazzName;
        File outFile = new File(path.getPath()+"" + "/" + className + ".java");       		
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile));	
		System.out.println(outFile.getAbsolutePath());
		JavaWriter jw = new JavaWriter(writer);
	
		   jw.emitPackage(packageName)
           .beginType(packageName + "." + className, "class", EnumSet.of(Modifier.PUBLIC))
           .emitJavadoc("Junit test case for "+clazzName+ " generated by tests generator.")
           .emitAnnotation("Test")
           .beginMethod("void", "testMethod", EnumSet.of(Modifier.PUBLIC))
           .beginControlFlow("assert(1,1)")
           .endControlFlow()
           .endMethod()
           .endType()
           .close();
		}catch (Exception e){
			System.out.println(e);
		}
	
	
	
}
	
}	
