package junit_testcases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumSet;
import javax.lang.model.element.Modifier;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.squareup.javawriter.JavaWriter;

/**
 * This is a Junit test cases generator
 * 
 * @author HE YE
 *
 */
public class TestsGenerator {

	private static String path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
	private static String jsonPath = new File(path, "json_testcases").toString();
	private static String junitTestsPath = new File(path, "junit_testcases").toString();
	private static int TIMEOUT = 100;

	/**
	 * Assuming target class name is the capital of Json testcase name and
	 * method name is the same with Json testcase name.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File[] files = traverseFolder(jsonPath);
		for (File f : files) {
			if (!f.isDirectory()) {
				String filePath = f.getAbsolutePath();
				String jsonName = f.getName().replaceAll("[.][^.]+$", "");
				String clazzName = jsonName.toUpperCase();
				createJunitTest(filePath, clazzName, jsonName);
			}
		}
	}

	/**
	 * 
	 * @param path
	 * @return All Json testcases file path
	 * @throws IOException
	 */
	public static File[] traverseFolder(String path) throws IOException {
		File file = new File(path);
		if (file.exists()) {
			File[] files = file.listFiles();
			if (files.length == 0) {
				System.out.println("The folder is empty!");
				return null;
			} else {
				return files;
			}
		} else {
			System.out.println("File is not exist!");
			return null;
		}
	}

	/**
	 * To write Junit tests for per Json testcase using JavaWriter
	 * 
	 * @param filePath
	 * @param clazzName
	 * @param methodName
	 * @throws IOException
	 */
	private static void createJunitTest(String filePath, String clazzName, String methodName) throws IOException {
		try {
			Class targetClass = Class.forName("java_programs." + clazzName);
			Method[] methods = targetClass.getDeclaredMethods();
			Type[] types = null;
			for (Method method : methods) {
				if (methodName.equals(method.getName())) {
					types = method.getParameterTypes();
				}
			}
			String packageName = "junit_testcases";
			String testClassName = "TEST_" + clazzName;
			File outFile = new File(junitTestsPath, testClassName + ".java");
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outFile));
			JavaWriter jw = new JavaWriter(writer);
			// Configure package name, class name and Javadoc
			jw.emitPackage(packageName).emitStaticImports("org.junit.Assert.*").emitImports("org.junit.Test")
					.emitImports("java_programs." + clazzName).emitImports("com.google.gson.JsonParser").emitEmptyLine()
					.beginType(packageName + "." + testClassName, "class", EnumSet.of(Modifier.PUBLIC))
					.emitJavadoc("Junit test case for " + clazzName + " generated by tests generator.");

			// Create test method per line of Json test case
			ArrayList<JsonElement> jsonElements = readJsonFile(filePath);
			int count = 1;
			for (JsonElement jsonElement : jsonElements) {
				JsonArray jsonArray = jsonElement.getAsJsonArray();
				JsonElement input = jsonArray.get(0);
				JsonElement output = jsonArray.get(1);
				String outputStr = output.toString().replace("\"", "\\\"");
				JsonArray inputArray;
				if (input.isJsonArray()) {
					inputArray = input.getAsJsonArray();
				} else {
					inputArray = new JsonArray();
					inputArray.add(input);
				}

				String parameterStr = getParametersString(types, inputArray);

				jw.emitAnnotation("Test(timeout = " + TIMEOUT + ")")
						.beginMethod("void", methodName + "_test" + count, EnumSet.of(Modifier.PUBLIC))
						.beginControlFlow("try")
						.emitStatement("Object actual = " + clazzName + "." + methodName + "(" + parameterStr + ")",
								null)
						.emitStatement("assertEquals(\"" + outputStr + "\"" + ",actual.toString() )", null)
						.endControlFlow().beginControlFlow("catch(IllegalArgumentException e)")
						.emitStatement("throw new IllegalArgumentException(\"Arguments are illegal!\") ", null)
						.endControlFlow().endMethod().emitEmptyLine();
				count++;
			}
			jw.endType().close();
			System.out.println("Completed generating testcase for " + clazzName + ".");
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	/**
	 * 
	 * @param path
	 * @return A list of each line of Json element
	 * @throws IOException
	 */
	@SuppressWarnings("finally")
	public static ArrayList<JsonElement> readJsonFile(String path) {
		ArrayList<JsonElement> jsonElements = new ArrayList<JsonElement>();
		JsonParser parser = new JsonParser();
		JsonElement je = null;
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(path));
			String s = null;
			while ((s = br.readLine()) != null) {
				je = parser.parse(s);
				jsonElements.add(je);
			}
		} catch (Exception e) {
			System.out.println("Errors occurs when read Json files.");
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					System.out.println("IOException: Errors occurs when close file reader.");
				}
			}
			return jsonElements;
		}
	}

	/**
	 * Generate input parameters as String
	 * 
	 * @param types
	 * @param inputArray
	 * @return
	 */
	public static String getParametersString(Type[] types, JsonArray inputArray) {
		String parameterStr = "";
		if (types.length == inputArray.size()) {
			for (int i = 0; i < types.length; i++) {
				JsonElement j = inputArray.get(i);
				String thisType = types[i].getTypeName();
				if ("java.util.ArrayList".equals(thisType)) {
					String arrStr = j.toString().replace("[", "(").replace("]", ")");
					parameterStr = parameterStr + "new " + thisType + "(java.util.Arrays.asList" + arrStr + "),";
				} else if (thisType.contains("[]")) {
					String arrStr = j.toString().replace("[", "{").replace("]", "}");
					parameterStr = parameterStr + "new " + thisType + "" + arrStr + ",";
				} else if ("java.lang.Object".equals(thisType)) {
					new JsonParser().parse(j.toString());
					String str = j.toString().replace("\"", "\\\"");
					parameterStr = parameterStr + "new JsonParser().parse(\"" + str + "\"),";
				} else {
					parameterStr = parameterStr + "(" + thisType + ")" + j.toString() + ",";
				}
			}
		} else {
			System.out.println("Incompatible types: Object cannot be converted.");
		}
		return parameterStr.substring(0, parameterStr.length() - 1);
	}

}
