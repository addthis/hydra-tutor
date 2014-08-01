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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.addthis.bundle.core.BundleField;
import com.addthis.bundle.value.ValueArray;
import com.addthis.bundle.value.ValueBytes;
import com.addthis.bundle.value.ValueCustom;
import com.addthis.bundle.value.ValueDouble;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueLong;
import com.addthis.bundle.value.ValueMap;
import com.addthis.bundle.value.ValueMapEntry;
import com.addthis.bundle.value.ValueNumber;
import com.addthis.bundle.value.ValueObject;
import com.addthis.bundle.value.ValueString;
import com.addthis.bundle.value.ValueTranslationException;

public class JSONBundleMap<V> implements ValueMap<V> {

    private final JSONBundle json;

    public JSONBundleMap(JSONBundle json) {
        this.json = json;
    }

    @Override
    public ValueObject get(Object key) {
        return json.getValue(new JSONBundleField(key.toString()));
    }

    @Override
    public ValueObject remove(Object key) {
        return json.getValue(new JSONBundleField(key.toString()));
    }

    @Override
    public void putAll(Map<? extends String, ? extends ValueObject<V>> map) {
        for (Entry<? extends String, ? extends ValueObject<V>> entry : map.entrySet()) {
            BundleField field = new JSONBundleField(entry.getKey());
            json.setValue(field, entry.getValue());
        }
    }

    @Override
    public void clear() {
        Iterator<ValueMapEntry<V>> iterator = iterator();
        while (iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    @Override
    public Set<String> keySet() {
        return json.getJson().keySet();
    }

    @Override
    public Collection<ValueObject<V>> values() {
        List<ValueObject<V>> list = new ArrayList<>();
        Iterator<ValueMapEntry<V>> iterator = iterator();
        while (iterator.hasNext()) {
            ValueMapEntry<V> entry = iterator.next();
            list.add(entry.getValue());
        }
        return list;
    }

    @Override
    public ValueObject<V> put(String key, ValueObject val) {
        BundleField field = new JSONBundleField(key);
        ValueObject ret = json.getValue(field);
        json.setValue(field, val);
        return ret;
    }

    @Override
    public int size() {
        return json.getCount();
    }

    @Override
    public boolean isEmpty() {
        return json.getCount() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        BundleField field = new JSONBundleField(key.toString());
        ValueObject ret = json.getValue(field);
        return ret != null;
    }

    @Override
    public boolean containsValue(Object value) {
        Iterator<ValueMapEntry<V>> iterator = iterator();
        while (iterator.hasNext()) {
            ValueMapEntry entry = iterator.next();
            if (entry.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<String, ValueObject<V>>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TYPE getObjectType() {
        return TYPE.MAP;
    }

    @Override public Map<String, V> asNative() {
        return (Map<String, V>) json.asMap().asNative();
    }

    @Override
    public ValueBytes asBytes() throws ValueTranslationException {
        throw new UnsupportedOperationException("Cannot convert a map object into a byte array");
    }

    @Override
    public ValueArray asArray() throws ValueTranslationException {
        throw new UnsupportedOperationException("Cannot convert a map object into an array");
    }

    @Override
    public ValueMap asMap() throws ValueTranslationException {
        return this;
    }

    @Override
    public ValueNumber asNumeric() throws ValueTranslationException {
        throw new UnsupportedOperationException("Cannot convert a map object into a number");
    }

    @Override
    public ValueLong asLong() throws ValueTranslationException {
        throw new UnsupportedOperationException("Cannot convert a map object into a long");
    }

    @Override
    public ValueDouble asDouble() throws ValueTranslationException {
        throw new UnsupportedOperationException("Cannot convert a map object into a double");
    }

    @Override
    public ValueString asString() throws ValueTranslationException {
        return ValueFactory.create(json.toString());
    }

    @Override
    public ValueCustom asCustom() throws ValueTranslationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<ValueMapEntry<V>> iterator() {
        return new Iterator<ValueMapEntry<V>>() {
            final Iterator<BundleField> iter = json.iterator();
            String lastKey;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public ValueMapEntry<V> next() {
                final String key;
                final ValueObject<V> value;

                {
                    BundleField next = iter.next();
                    value = json.getValue(next);
                    key = next.getName();
                    lastKey = key;
                }

                return new ValueMapEntry<V>() {
                    @Override
                    public String getKey() {
                        return key;
                    }

                    @Override
                    public ValueObject<V> getValue() {
                        return value;
                    }

                    @Override
                    public ValueObject<V> setValue(ValueObject<V> val) {
                        return put(key, value);
                    }
                };
            }

            @Override
            public void remove() {
                JSONBundleMap.this.remove(lastKey);
            }
        };
    }
}
