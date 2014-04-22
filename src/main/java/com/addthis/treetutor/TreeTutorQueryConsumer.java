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

import com.addthis.bundle.channel.DataChannelError;
import com.addthis.bundle.channel.DataChannelOutput;
import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.core.list.ListBundle;
import com.addthis.bundle.core.list.ListBundleFormat;
import com.addthis.bundle.table.DataTable;
import com.addthis.hydra.data.query.Query;
import com.addthis.hydra.data.query.QueryException;
import com.addthis.hydra.data.query.QueryOpProcessor;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * Query Consumer for TreeTutor. Puts the bundles that match query and adds them to a Data Table.
 */
public class TreeTutorQueryConsumer implements DataChannelOutput {

    private final DataTable table;
    private final ListBundleFormat format = new ListBundleFormat();
    private final Semaphore gate = new Semaphore(1);
    private QueryException exception;

    /**
     * Default constructor for TreeTutorQueryConsumer.
     */
    public TreeTutorQueryConsumer() {
        table = new QueryOpProcessor.Builder(this).build().createTable(0);
        try {
            gate.acquire();
        } catch (InterruptedException e) {
            throw new DataChannelError(e);
        }
    }

    /**
     * @throws Exception
     */
    public void waitComplete() throws Exception {
        gate.acquire();
        if (exception != null) {
            throw exception;
        }
    }

    /**
     *
     */
    @Override
    public void sendComplete() {
        gate.release();
    }

    /**
     * Appends a new bundle to the table.
     *
     * @param row - new bundle to append.
     */
    @Override
    public void send(Bundle row) {
        table.append(row);
    }

    /**
     * Appends a list of bundles to the table.
     *
     * @param bundles - list of bundles to append.
     */
    @Override
    public void send(List<Bundle> bundles) {
        Iterator iterator = bundles.iterator();
        while (iterator.hasNext()) {
            table.append((Bundle) iterator.next());
        }
    }

    @Override
    public void sourceError(DataChannelError ex) {
        exception = new QueryException(ex);
        sendComplete();
    }

    /**
     * Creates a new bundle.
     *
     * @return - the new bundle.
     */
    @Override
    public Bundle createBundle() {
        return new ListBundle(format);
    }

    /**
     * Only callable once.
     *
     * @return the table of matching bundles.
     */
    public DataTable getTable() throws Exception {
        waitComplete();
        return table;
    }
}
