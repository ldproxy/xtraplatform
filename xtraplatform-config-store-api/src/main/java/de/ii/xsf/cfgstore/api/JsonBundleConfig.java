package de.ii.xsf.cfgstore.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import de.ii.xsf.core.api.Resource;

/**
 *
 * @author zahnen
 */
public class JsonBundleConfig /*extends ObjectNode*/ implements Resource {

    private String id;
    private ObjectNode cfg;

    public JsonBundleConfig(String id, ObjectNode cfg) {
        //super(JsonNodeFactory.instance);
        this.id = id;
        this.cfg = cfg;
    }

    @Override
    public String getResourceId() {
        return id;
    }

    @Override
    public void setResourceId(String id) {
        this.id = id;
    }

    public ObjectNode getCfg() {
        return cfg;
    }

    public void setCfg(ObjectNode cfg) {
        this.cfg = cfg;
    }
    
    
}
