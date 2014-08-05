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

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.BundleField;
import com.addthis.bundle.core.BundleFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class JSONBundleFormat implements BundleFormat {

    HashMap<String, JSONBundleField> fields = new HashMap<String, JSONBundleField>();
    ArrayList<JSONBundleField> fieldList = new ArrayList<JSONBundleField>();

    @Override
    public BundleField getField(String name) {
        JSONBundleField field = fields.get(name);
        if (field == null) {
            field = new JSONBundleField(name);
            fields.put(name, field);
            fieldList.add(field);
        }
        return field;
    }

    @Override
    public boolean hasField(String name) {
        return fields.get(name) != null;
    }

    @Override
    public BundleField getField(int pos) {
        return fieldList.get(pos);
    }

    @Override
    public int getFieldCount() {
        return fields.size();
    }

    @Override
    public Object getVersion() {
        return JSONBundleFormat.class;
    }

    @Override
    public Iterator<BundleField> iterator() {
        return new Iterator<BundleField>() {
            final Iterator<JSONBundleField> iter = fieldList.iterator();

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public BundleField next() {
                return iter.next();
            }

            @Override
            public void remove() {
                iter.remove();
            }
        };
    }


    @Override
    public Bundle createBundle() {
        throw new UnsupportedOperationException();
    }
}
