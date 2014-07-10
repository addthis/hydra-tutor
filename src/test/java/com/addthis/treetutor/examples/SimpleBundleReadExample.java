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
package com.addthis.treetutor.examples;

import java.util.concurrent.atomic.AtomicBoolean;

import com.addthis.bundle.core.Bundle;
import com.addthis.codec.json.CodecJSON;
import com.addthis.hydra.task.run.TaskRunConfig;
import com.addthis.hydra.task.source.TaskDataSource;
import com.addthis.maljson.JSONObject;

/**
 * A simple class to read bundles from a data source using meshy
 */
public class SimpleBundleReadExample {

    private static final String INPUT_JOB = "077b0af0-eac8-4829-b54e-2b644e086404";

    public static void main(String args[]) {
        new SimpleBundleReadExample().simpleExample();
    }

    private void simpleExample() {
        String source = "{\n" +                               //TODO: Do I need to change any of this for different IDs?
                        "    type:\"mesh2\",\n" +
                        "    processAllData:true,\n" +
                        "    mesh:{\n" +
                        "      startDate:\"{{now-1}}\",\n" +
                        "      endDate:\"{{now}}\",\n" +
                        "      meshHost:\"adm03\",\n" +
                        "      files:[\"/job*/" + INPUT_JOB + "/0/gold/split/{Y}{M}{D}/{H}/*\"],\n" +
                        "    },\n" +
                        "    format:{\n" +
                        "       type:\"channel\",\n" +
                        "	 },\n" +
                        "    workers:1,\n" +
                        "}\n";

        TaskRunConfig taskRunConfig = new TaskRunConfig(0, 1, "test");

        TaskDataSource dataSource = null;

        try {
            JSONObject jsonObject = new JSONObject(source);
            dataSource = CodecJSON.decodeObject(TaskDataSource.class, jsonObject);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dataSource.init(taskRunConfig, new AtomicBoolean());

        try {
            long bundleCount = 0;
            Bundle next = dataSource.next();
            while (next != null && bundleCount < 10) {
                System.out.println(next);
                bundleCount++;
            }

            System.out.println("Read " + bundleCount + " bundles");
        } finally {
            dataSource.close();
        }
    }
}
