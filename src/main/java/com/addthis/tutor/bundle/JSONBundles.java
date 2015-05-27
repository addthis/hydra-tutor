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

import java.io.IOException;

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

    public static ValueObject parseValue(String input) throws IOException {
        if (input == null) {
            return null;
        }
        return ValueFactory.decodeValue(input);
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
