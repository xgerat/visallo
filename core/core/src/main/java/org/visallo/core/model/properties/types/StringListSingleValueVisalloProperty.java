package org.visallo.core.model.properties.types;

import org.json.JSONArray;
import org.visallo.core.util.JSONUtil;

import java.util.List;

public class StringListSingleValueVisalloProperty extends SingleValueVisalloProperty<List<String>, String> {
    public StringListSingleValueVisalloProperty(String key) {
        super(key);
    }

    @Override
    public String wrap(List<String> value) {
        return new JSONArray(value).toString();
    }

    @Override
    public List<String> unwrap(Object value) {
        if (value == null) {
            return null;
        }
        return JSONUtil.toStringList(JSONUtil.parseArray(value.toString()));
    }
}
