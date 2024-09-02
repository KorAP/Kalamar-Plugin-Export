package de.ids_mannheim.korap.plkexport;

import org.tinylog.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class ExpTempl {

/*
 * Returns export template as JSON 
 */
public static String getExportTempl(String scheme, String host, String port){
    
    String json = "";
    try {

    ObjectMapper mapper = new ObjectMapper();
    ObjectNode templ = mapper.createObjectNode();

    templ.put("name", "Export");
    templ.put("desc", "Exports Kalamar results");
   
    ObjectNode embed = mapper.createObjectNode();
    embed.put("panel", "result");
    embed.put("title", "exports KWICs and snippets");
    embed.put("icon", "\uf019");

    ArrayNode classes = mapper.createArrayNode();
    classes.add("button-icon");
    classes.add("plugin");
    embed.set("classes", classes);

    ObjectNode onClick = mapper.createObjectNode();
    onClick.put("action", "addWidget");
    onClick.put("template",  scheme + "://" + host + ":" + port +"/export");
   
    ArrayNode perm = mapper.createArrayNode();
    perm.add("forms");
    perm.add("scripts");
    perm.add("downloads");

    onClick.set("permissions", perm);
    embed.set("onClick", onClick);
    templ.set("embed", embed);

    json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(templ);

} catch (Exception ex) {
    Logger.error(ex);
    return null;
}
    return json;
    }
}
