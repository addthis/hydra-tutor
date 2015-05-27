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
package com.addthis.tutor.tree;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.StringReader;

import java.util.Iterator;
import java.util.List;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.BundleFactory;
import com.addthis.bundle.core.BundleField;
import com.addthis.bundle.core.BundleFormat;
import com.addthis.maljson.JSONException;
import com.addthis.tutor.bundle.JSONBundles;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;

import org.supercsv.io.CsvListReader;
import org.supercsv.prefs.CsvPreference;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Class that handles the input data source for the tree.
 */
public class TreeInput extends AbstractIterator<Bundle> {

    private final CsvListReader csvParser;
    private final BundleFactory bundleFactory;
    private final List<BundleField> fields;

    private static final CsvPreference csvParserOptions = new CsvPreference.Builder(CsvPreference.STANDARD_PREFERENCE)
            .surroundingSpacesNeedQuotes(true)
            .build();

    /**
     * Constructs a TreeInput object from the specified input string.
     *
     * @param input - string input to read bundles from.
     */
    public TreeInput(String input, final BundleFormat format, BundleFactory bundleFactory) throws IOException {
        checkArgument(format.getFieldCount() == 0);
        this.bundleFactory = bundleFactory;
        this.csvParser = new CsvListReader(new StringReader(input), csvParserOptions);

        List<String> keys = csvParser.read();
        if (keys == null) {
            throw new IllegalArgumentException("header row is missing");
        }
        fields = Lists.transform(keys, new Function<String, BundleField>() {
            @Nullable @Override public BundleField apply(@Nullable String input) {
                return format.getField(input);
            }
        });
    }

    @Override protected Bundle computeNext() {
        try {
            List<String> nextRow = csvParser.read();
            if (nextRow == null) {
                return endOfData();
            } else {
                if (nextRow.size() < fields.size()) {
                    String message = String.format("row at %s had %s < %s values; was %s",
                                                   csvParser.getRowNumber(), nextRow.size(), fields.size(), nextRow);
                    throw new IllegalArgumentException(message);
                }
                Bundle bundle = bundleFactory.createBundle();
                Iterator<String> values = nextRow.iterator();
                for (BundleField field : fields) {
                        bundle.setValue(field, JSONBundles.parseValue(values.next()));
                }
                return bundle;
            }
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
