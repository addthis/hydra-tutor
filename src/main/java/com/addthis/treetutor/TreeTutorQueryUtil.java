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


import java.io.File;

import com.addthis.bundle.table.DataTable;
import com.addthis.hydra.data.query.Query;
import com.addthis.hydra.data.query.QueryOpProcessor;
import com.addthis.hydra.data.query.engine.QueryEngine;
import com.addthis.hydra.data.query.source.QuerySource;
import com.addthis.hydra.data.tree.ReadTree;
import com.addthis.hydra.query.QueryEngineSource;

/**
 * Utility class to run queries for the TreeTutor.
 */
public class TreeTutorQueryUtil {

    /**
     * Default constructor for the TreeTutorQueryUtil class.
     */
    public TreeTutorQueryUtil() {
        super();
    }

    /**
     * Method to run query against data directory produced by TreeMapper with specified query path and ops.
     *
     * @param path - query path
     * @param ops  - query ops
     */
    public DataTable runQuery(String path, String ops, File directory) throws Exception {
        String[] paths = {path};
        Query query = new Query(null, paths, null);

        final File dir = new File(directory, "data");
        QuerySource client = new QueryEngineSource() {
            @Override
            public QueryEngine getEngineLease() {
                try {
                    QueryEngine temp = new QueryEngine(new ReadTree(dir));
                    temp.lease();
                    temp.closeWhenIdle();
                    return temp;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        TreeTutorQueryConsumer consumer = new TreeTutorQueryConsumer();
        QueryOpProcessor proc = new QueryOpProcessor.Builder(consumer, ops).build();
        client.query(query, proc);
        DataTable table = consumer.getTable();
        return table;
    }
}
