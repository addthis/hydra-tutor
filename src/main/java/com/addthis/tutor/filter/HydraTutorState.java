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
package com.addthis.tutor.filter;

import java.util.concurrent.Future;

import com.addthis.basis.util.Parameter;

import com.addthis.bundle.value.ValueObject;
import com.addthis.codec.annotations.Pluggable;
import com.addthis.codec.config.Configs;
import com.addthis.codec.plugins.PluginRegistry;
import com.addthis.hydra.data.filter.bundle.BundleFilter;
import com.addthis.hydra.data.filter.bundle.BundleFilterEvalJava;
import com.addthis.hydra.data.filter.value.ValueFilter;
import com.addthis.hydra.data.filter.value.ValueFilterEvalJava;
import com.addthis.maljson.JSONObject;
import com.addthis.tutor.bundle.JSONBundle;
import com.addthis.tutor.bundle.JSONBundleFormat;

import com.google.common.collect.BiMap;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import static com.addthis.tutor.bundle.JSONBundles.formatOutput;
import static com.addthis.tutor.bundle.JSONBundles.parseBundle;
import static com.addthis.tutor.bundle.JSONBundles.parseValue;

public class HydraTutorState {

    private static final boolean allowEvalJava = Parameter.boolValue("allow.eval.java", false);

    ValueFilter vFilter;
    BundleFilter bFilter;
    FilterCache filterCache;
    volatile Future<JSONObject> future;

    static final BiMap<String, Class<?>> vClassMap;
    static final BiMap<String, Class<?>> bClassMap;

    static {
        Pluggable valueFilterPluggable = ValueFilter.class.getAnnotation(Pluggable.class);
        Pluggable bundleFilterPluggable = BundleFilter.class.getAnnotation(Pluggable.class);

        PluginRegistry pluginRegistry = PluginRegistry.defaultRegistry();
        vClassMap = pluginRegistry.asMap().get(valueFilterPluggable.value()).asBiMap();
        bClassMap = pluginRegistry.asMap().get(bundleFilterPluggable.value()).asBiMap();
    }

    public String filter(String input, String filter, String filterType) throws Exception {
        try {
            if (input == null || filter == null) {
                return "";
            }

            if (filterType == null || !(filterType.equals("auto") || filterType.equals("value") ||
                                        filterType.equals("bundle"))) {
                throw new IllegalStateException("Internal error: the filter type " +
                                                "is not one of \"auto\", \"bundle\", or \"value\".");
            }

            filter = filter.trim();
            input = input.trim();

            FilterCache testCache = new FilterCache(filter, filterType);

            if (filterCache == null || !filterCache.equals(testCache)) {
                filterCache = testCache;
                vFilter = null;
                bFilter = null;

                Config filterConfig = ConfigFactory.parseString(filterCache.filter);

                String stype = null;
                if (filterConfig.hasPath("op")) {
                    stype = filterConfig.getString("op");
                }

                if (filterType.equals("auto")) {
                    if (stype != null) {
                        if (vClassMap.get(stype) != null && bClassMap.get(stype) != null) {
                            throw new IllegalStateException(
                                    "The 'op' : \"" + stype + "\" can be interpreted as either" +
                                    " a bundle filter or a value filter. Please select 'bundle'" +
                                    " or 'value' and retry.");
                        }
                        if (vClassMap.get(stype) != null) {
                            vFilter = Configs.decodeObject(ValueFilter.class, filterConfig);
                            vFilter.setup();
                        } else if (bClassMap.get(stype) != null) {
                            bFilter = Configs.decodeObject(BundleFilter.class, filterConfig);
                            bFilter.initialize();
                        } else {
                            throw new IllegalStateException("Cannot recognize the 'op' : \"" + stype + "\"");
                        }
                    } else {
                        try {
                            vFilter = Configs.decodeObject(ValueFilter.class, filterConfig);
                            vFilter.setup();
                        } catch (Exception ignored) {
                        }
                        try {
                            bFilter = Configs.decodeObject(BundleFilter.class, filterConfig);
                            bFilter.initialize();
                        } catch (Exception ignored) {
                        }
                        if ((vFilter != null) && (bFilter != null)) {
                            throw new IllegalStateException(
                                    "The op can be interpreted as either" +
                                    " a bundle filter or a value filter. Please select 'bundle'" +
                                    " or 'value' and retry.");
                        } else if ((vFilter == null) && (bFilter == null)) {
                            throw new IllegalStateException(
                                    "Cannot convert the filter to a bundle filter or a value filter. " +
                                    "Specify 'bundle' or 'value' for more information");
                        }
                    }
                } else if (filterType.equals("bundle")) {
                    bFilter = Configs.decodeObject(BundleFilter.class, filterConfig);
                    bFilter.initialize();
                } else {
                    vFilter = Configs.decodeObject(ValueFilter.class, filterConfig);
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
