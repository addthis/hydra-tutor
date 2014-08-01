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

import com.addthis.bundle.core.BundleField;

public class JSONBundleField implements BundleField {

    private final String fieldName;

    JSONBundleField(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getName() {
        return fieldName;
    }

    @Override
    public Integer getIndex() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JSONBundleField that = (JSONBundleField) o;

        if (fieldName != null ? !fieldName.equals(that.fieldName) : that.fieldName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return fieldName != null ? fieldName.hashCode() : 0;
    }

}
