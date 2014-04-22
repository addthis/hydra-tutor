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
package com.addthis.hydratutor.bundle;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.BundleException;
import com.addthis.bundle.core.BundleField;
import com.addthis.bundle.core.BundleFormat;
import com.addthis.bundle.value.ValueArray;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueMap;
import com.addthis.bundle.value.ValueMapEntry;
import com.addthis.bundle.value.ValueObject;

import java.util.Iterator;

import com.addthis.maljson.JSONArray;
import com.addthis.maljson.JSONObject;

public class JSONBundle implements Bundle {

    final private JSONObject json;
    final private JSONBundleFormat format;

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

    // TODO handle nested objects
    private ValueMap convertJSONObject(JSONObject input) {
        ValueMap map = ValueFactory.createMap();
        for (String key : input.keySet()) {
            Object raw = input.opt(key);
            Class<?> clazz = raw.getClass();
            map.put(key, createPrimitiveBundle(clazz, raw));
        }
        return map;
    }

    @Override
    public ValueObject getValue(BundleField field) throws BundleException {
        try {
            Object raw = json.opt(field.getName());
            if (raw == null) {
                return null;
            }
            Class<?> clazz = raw.getClass();

                /* unwrap JSONObject to ValueMap */
            if (clazz == JSONObject.class) {
                return convertJSONObject((JSONObject) raw);
            } else if (clazz == JSONArray.class) {
                // TODO: still only one dimension supported
                JSONArray jarr = (JSONArray) raw;
                ValueArray arr = ValueFactory.createArray(jarr.length());
                for (int i = 0; i < jarr.length(); i++) {
                    arr.add(i, createPrimitiveBundle(jarr.opt(i).getClass(), jarr.opt(i)));
                }
                return arr;
            } else {
                return createPrimitiveBundle(clazz, raw);
            }
        } catch (Exception ex) {
            throw new BundleException(ex);
        }
    }

    private ValueObject createPrimitiveBundle(Class<?> clazz, Object raw) {
        // it would be cool to not do this twice, but I gave up fighting with generics
        //Class<?> clazz = raw.getClass();
        if (clazz == Integer.class) {
            return ValueFactory.create(((Integer) raw).longValue());
        } else if (clazz == Long.class) {
            return ValueFactory.create(((Long) raw).longValue());
        } else if (clazz == Float.class) {
            return ValueFactory.create(((Float) raw).doubleValue());
        } else if (clazz == Double.class) {
            return ValueFactory.create(((Double) raw).doubleValue());
        } else {
            return ValueFactory.create(raw.toString());
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
                for (ValueMapEntry entry : value.asMap()) {
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
                return value.asLong();
            case FLOAT:
                return value.asDouble();
            case BYTES:
                return value.asBytes();
            default:
            case STRING:
                return value.asString();
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
        return new JSONBundle(new JSONObject(), format);
    }

    @Override
    public Iterator<BundleField> iterator() {
        return format.iterator();
    }

}
