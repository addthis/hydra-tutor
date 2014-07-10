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
package com.addthis.hydratutor;

import java.util.List;
import java.util.concurrent.Future;

import com.addthis.basis.util.Parameter;

import com.addthis.bundle.value.ValueArray;
import com.addthis.bundle.value.ValueFactory;
import com.addthis.bundle.value.ValueObject;
import com.addthis.bundle.value.ValueString;
import com.addthis.codec.config.CodecConfig;
import com.addthis.hydra.data.filter.bundle.BundleFilter;
import com.addthis.hydra.data.filter.bundle.BundleFilterEvalJava;
import com.addthis.hydra.data.filter.value.ValueFilter;
import com.addthis.hydra.data.filter.value.ValueFilterEvalJava;
import com.addthis.hydratutor.bundle.JSONBundle;
import com.addthis.hydratutor.bundle.JSONBundleFormat;
import com.addthis.hydratutor.bundle.JSONBundleMap;
import com.addthis.maljson.JSONException;
import com.addthis.maljson.JSONObject;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class HydraTutorState {

    private static final boolean allowEvalJava = Parameter.boolValue("allow.eval.java", false);

    ValueFilter vFilter;
    BundleFilter bFilter;
    FilterCache filterCache;
    volatile Future<JSONObject> future;

    private static final String errorMsg = "Cannot parse the input %s. " +
                                           "You should place quotes around your input " +
                                           "to have it interpreted as a string.";

    private String generateErrorMessage(String input) {
        return String.format(errorMsg, input);
    }

    private JSONObject parseBundle(String input) throws JSONException {
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
    private ValueObject parseValue(String input) throws IllegalStateException, JSONException {
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

    private String formatOutput(ValueObject output) {
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

    public String filter(String input, String filter, String filtertype) throws Exception {
        try {
            if (input == null || filter == null) {
                return "";
            }

            if (filtertype == null || !(filtertype.equals("auto") || filtertype.equals("value") ||
                                        filtertype.equals("bundle"))) {
                throw new IllegalStateException("Internal error: the filter type " +
                                                "is not one of \"auto\", \"bundle\", or \"value\".");
            }

            filter = filter.trim();
            input = input.trim();

            FilterCache testCache = new FilterCache(filter, filtertype);

            if (filterCache == null || !filterCache.equals(testCache)) {
                filterCache = testCache;
                vFilter = null;
                bFilter = null;

                Config filterConfig = ConfigFactory.parseString(filterCache.filter);

                if (filtertype.equals("auto")) {
                    try {
                        vFilter = CodecConfig.getDefault().decodeObject(ValueFilter.class, filterConfig);
                        vFilter.setup();
                    } catch (Exception ignored) {
                    }
                    try {
                        bFilter = CodecConfig.getDefault().decodeObject(BundleFilter.class, filterConfig);
                        bFilter.initialize();
                    } catch (Exception ignored) {
                    }
                    if ((vFilter != null) && (bFilter != null)) {
                        throw new IllegalStateException(
                                "The op can be interpreted as either" +
                                " a bundle filter or a value filter. Please select 'bundle'" +
                                " or 'value' and retry.");
                    }
                    if ((vFilter == null) && (bFilter == null)) {
                        throw new IllegalStateException("Cannot recognize the op or other ambiguous error. " +
                                                        "Specify 'bundle' or 'value' for more information");
                    }
                } else if (filtertype.equals("bundle")) {
                    bFilter = CodecConfig.getDefault().decodeObject(BundleFilter.class, filterConfig);
                    bFilter.initialize();
                } else {
                    vFilter = CodecConfig.getDefault().decodeObject(ValueFilter.class, filterConfig);
                    vFilter.setup();
                }
            }

            String[] inputs = input.split("[\\r\\n]+");

            StringBuilder outputBuilder = new StringBuilder();

            if (vFilter != null) {
                if (vFilter instanceof ValueFilterEvalJava && !allowEvalJava) {
                    String msg = "eval-java has been disabled. " +
                                 "It can be enabled with \"-Dallow.eval.java=true\"";
                    throw new UnsupportedOperationException(msg);
                }

                for (String inputString : inputs) {
                    ValueObject valueInput = parseValue(inputString);

                    ValueObject output = vFilter.filter(valueInput);

                    String outputString = formatOutput(output);

                    outputBuilder.append(outputString);

                    outputBuilder.append("\n");
                }

            } else if (bFilter != null) {
                if (bFilter instanceof BundleFilterEvalJava && !allowEvalJava) {
                    String msg = "eval-java has been disabled. " +
                                 "It can be enabled with \"-Dallow.eval.java=true\"";
                    throw new UnsupportedOperationException(msg);
                }

                for (String inputString : inputs) {

                    JSONObject jsonInput = parseBundle(inputString);

                    JSONBundleFormat format = new JSONBundleFormat();

                    JSONBundle bundle = new JSONBundle(jsonInput, format);

                    boolean result = bFilter.filter(bundle);

                    outputBuilder.append(bundle.getJson().toString(2));

                    outputBuilder.append(" =========> filter result is '");

                    outputBuilder.append(result);

                    outputBuilder.append("'");

                    outputBuilder.append("\n");
                }
            } else {
                throw new IllegalStateException("Illegal state reached in hydra-tutor.");
            }
            return outputBuilder.toString();
        } catch (RuntimeException ex) {
            filterCache = null;
            bFilter = null;
            vFilter = null;
            throw ex;
        }

    }

    public void reset() {
        Future<JSONObject> theFuture = future;
        if (theFuture != null) {
            theFuture.cancel(true);
        }
        filterCache = null;
        vFilter = null;
        bFilter = null;
        future = null;
    }

    public void setFuture(Future<JSONObject> future) {
        this.future = future;
    }

    public Future<JSONObject> getFuture() {
        return future;
    }

}
