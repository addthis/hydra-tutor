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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Class that handles the input data source for the tree.
 */
public class TreeInput {

    private final List<String> keys;
    private final List<String[]> values;
    private final Iterator<String[]> iterator;

    private static final CsvPreference csvParserOptions = new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE)
            .surroundingSpacesNeedQuotes(true)
            .build();

    /**
     * Constructs a TreeInput object from the specified input string.
     *
     * @param input - string input to read bundles from.
     */
    public TreeInput(String input) throws IOException {
        Reader reader = new StringReader(input);
        CsvListReader csvParser = new CsvListReader(reader, csvParserOptions);

        List<String[]> accumulate = new ArrayList<String[]>();

        keys = csvParser.read();

        if (keys == null) {
            throw new IllegalStateException("header row is missing");
        }

        List<String> next;
        while ((next = csvParser.read()) != null) {
            accumulate.add(next.toArray(new String[next.size()]));
        }

        values = accumulate;
        iterator = values.iterator();

        for (int i = 0; i < keys.size(); i++) {
            keys.set(i, keys.get(i).trim());
        }
    }

    /**
     * Checks to see if the input source has another bundle.
     *
     * @return true if there is another bundle, false otherwise.
     */
    public boolean hasNextBundle() {
        return iterator.hasNext();
    }

    /**
     * Returns the next bundle from the input source.
     *
     * @return a new TreeBundle
     */
    public TreeBundle getNextBundle() {
        return new TreeBundle(iterator.next());
    }

    /**
     * @param keyIndex - desired index.
     * @return the key at the specified index.
     */
    public String getKey(int keyIndex) {
        return keys.get(keyIndex);
    }

    /**
     * @return the number of keys for the input.
     */
    public int numKeys() {
        return keys.size();
    }
}
