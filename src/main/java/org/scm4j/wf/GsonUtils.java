package org.scm4j.wf;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

public class GsonUtils {

	public static <T> T fromJson(String contentString, Type type) {
		Gson gson = new Gson();
		return gson.fromJson(contentString, type);
	}
	
	public static <T> T fromJson(String contentString, Type t, String nodeName) {
		Gson gson = new Gson();
		Reader reader = new StringReader(contentString);
		JsonReader jr = gson.newJsonReader(reader);
		JsonParser parser = new JsonParser();
		JsonElement je = parser.parse(jr);
		JsonObject jo = (JsonObject) je;
		je = jo.get(nodeName);
		return gson.fromJson(je, t);
	}

}
