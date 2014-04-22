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
package com.addthis.treetutor;

import org.apache.commons.collections.iterators.ArrayIterator;

import java.util.Iterator;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.kvp.KVBundle;
import com.addthis.bundle.value.ValueFactory;

public class TreeBundle {

    private final KVBundle bundle;
    private final String[] values;
    private final Iterator<String> iterator;

    /**
     * Constructs a TreeBundle object from the specified string.
     */
    public TreeBundle(String[] values) {
        this.bundle = new KVBundle();
        this.values = values;
        this.iterator = new ArrayIterator(values);

    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * @return the next bundle value.
     */
    public String getNextValue() {
        return iterator.next().trim();
    }

    /**
     * Sets the a bundle value given the given key.
     * TODO: Could initialize keys first time through so I don't have to do it every single time.
     */
    public void setValue(String key) {
        bundle.setValue(bundle.getFormat().getField(key), ValueFactory.create(getNextValue()));

    }


    /**
     * @return a ListBundle representation of this TreeBundle.
     */
    public Bundle toListBundle() {
        return bundle;
    }
}
