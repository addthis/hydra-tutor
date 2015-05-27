/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.tutor.bundle;

import java.util.Iterator;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.BundleException;
import com.addthis.bundle.core.BundleField;
import com.addthis.bundle.core.BundleFormat;
import com.addthis.bundle.value.ValueArray;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueMap;
import com.addthis.bundle.value.ValueMapEntry;
import com.addthis.bundle.value.ValueObject;
import com.addthis.maljson.JSONArray;
import com.addthis.maljson.JSONObject;

public class JSONBundle implements Bundle {

    private final JSONObject json;
    private final JSONBundleFormat format;

    public JSONBundle(JSONObject json, JSONBundleFormat format) {
        this.json = json;
        this.format = format;

        for (String key : json.keySet()) {
            format.getField(key);
        }
    }

    public JSONObject getJson() {
        return json;
    }

    @Override
    public ValueObject getValue(BundleField field) throws BundleException {
        try {
            Object raw = json.opt(field.getName());
            if (raw == null) {
                return null;
            }
            return ValueFactory.decodeValue('"' + raw.toString() + '"');
        } catch (Exception ex) {
            throw new BundleException(ex);
        }
    }

    private Object valueToNative(ValueObject value) throws Exception {
        if (value == null) {
            return null;
        }
        switch (value.getObjectType()) {
            case CUSTOM:
                value = value.asCustom().asMap();
            case MAP:
                JSONObject map = new JSONObject();
                ValueMap asMap = value.asMap();
                for (ValueMapEntry entry : asMap) {
                    map.put(entry.getKey(), valueToNative(entry.getValue()));
                }
                return map;
            case ARRAY:
                // TODO
                JSONArray arr = new JSONArray();
                for (ValueObject obj : value.asArray()) {
                    arr.put(valueToNative(obj));
                }
                return arr;
            case INT:
                return value.asLong().asNative();
            case FLOAT:
                return value.asDouble().asNative();
            case BYTES:
                return value.asBytes().asNative();
            default:
            case STRING:
                return value.asString().asNative();
        }
    }

    @Override
    public void setValue(BundleField field, ValueObject value) throws BundleException {
        try {
            json.put(field.getName(), valueToNative(value));
        } catch (Exception ex) {
            throw new BundleException(ex);
        }
    }

    @Override
    public void removeValue(BundleField field) throws BundleException {
        json.remove(field.getName());
    }

    @Override
    public BundleFormat getFormat() {
        return format;
    }

    @Override
    public int getCount() {
        return json.length();
    }

    @Override
    public Bundle createBundle() {
        return format.createBundle();
    }

    @Override
    public Iterator<BundleField> iterator() {
        return format.iterator();
    }
}
