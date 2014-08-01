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

import java.util.List;

import com.addthis.bundle.value.ValueArray;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueObject;
import com.addthis.bundle.value.ValueString;
import com.addthis.maljson.JSONException;
import com.addthis.maljson.JSONObject;
import com.addthis.tutor.filter.LosslessTokenizer;

public final class JSONBundles {
    private static final String errorMsg = "Cannot parse the input %s. " +
                                           "You should place quotes around your input " +
                                           "to have it interpreted as a string.";

    private static String generateErrorMessage(String input) {
        return String.format(errorMsg, input);
    }

    public static JSONObject parseBundle(String input) throws JSONException {
        if (input == null) {
            return null;
        }

        input = input.trim();

        if (input.equals("")) {
            return new JSONObject();
        } else if (input.length() < 2 || !input.startsWith("{") || !input.endsWith("}")) {
            String message = String.format("The input string %s does not " +
                                           "appear to be in bundle format. " +
                                           "Bundles are of the form {\"key1\" : val1, \"key2\" : val2, ...} .",
                    input);
            throw new IllegalStateException(message);
        }

        JSONObject jsonInput = new JSONObject(input);

        return jsonInput;
    }

    /**
     * @param input
     * @return
     * @throws IllegalStateException
     */
    public static ValueObject parseValue(String input) throws IllegalStateException, JSONException {
        if (input == null) {
            return null;
        }

        input = input.trim();

        if (input.equals("")) {
            return ValueFactory.create(input);
        } else if (input.equals("null") || input.equals("(null)")) {
            return null;
        }
        if (input.length() >= 2) {
            if (input.startsWith("\"") && input.endsWith("\"")) {
                return ValueFactory.create(input.substring(1, input.length() - 1));
            } else if (input.startsWith("{") && input.endsWith("}")) {
                JSONObject jsonInput = new JSONObject(input);

                JSONBundleFormat format = new JSONBundleFormat();

                JSONBundle bundle = new JSONBundle(jsonInput, format);

                JSONBundleMap valueMap = new JSONBundleMap(bundle);

                return valueMap;
            } else if (input.startsWith("[") && input.endsWith("]")) {
                String[] groups = {"{}", "[]", "\""};

                LosslessTokenizer tokenizer = new LosslessTokenizer(",", groups, false);

                List<String> tokens = tokenizer.tokenize(input.substring(1, input.length() - 1));

                ValueArray valueArray = ValueFactory.createArray(tokens.size());

                for (int i = 0; i < tokens.size(); i++) {
                    valueArray.add(parseValue(tokens.get(i)));
                }

                return valueArray;

            }
        }
        if (input.indexOf('.') >= 0 || input.equals("NaN")) {
            try {
                double dValue = Double.parseDouble(input);
                return ValueFactory.create(dValue);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(generateErrorMessage(input));
            }
        } else {
            try {
                long lValue = Long.parseLong(input);
                return ValueFactory.create(lValue);
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(generateErrorMessage(input));
            }
        }
    }

    public static String formatOutput(ValueObject output) {
        if (output == null) {
            return "(null)";
        } else if (output instanceof ValueString) {
            return '"' + output.toString() + '"';
        } else if (output instanceof ValueArray) {
            ValueArray asArray = (ValueArray) output;
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (int i = 0; i < asArray.size(); i++) {
                builder.append(formatOutput(asArray.get(i)));
                if (i < asArray.size() - 1) {
                    builder.append(" , ");
                }
            }
            builder.append(']');
            return builder.toString();
        } else {
            return output.toString();
        }
    }
}
