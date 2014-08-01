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

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.addthis.basis.util.Files;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.table.DataTable;
import com.addthis.codec.config.Configs;
import com.addthis.codec.json.CodecJSON;
import com.addthis.hydra.data.tree.DataTreeNode;
import com.addthis.hydra.data.tree.ReadTree;
import com.addthis.hydra.data.tree.ReadTreeNode;
import com.addthis.hydra.task.output.tree.TreeMapper;
import com.addthis.hydra.task.run.TaskRunConfig;
import com.addthis.maljson.JSONArray;
import com.addthis.maljson.JSONException;
import com.addthis.maljson.JSONObject;

/**
 * Handles all tree operations for the Tutor such as processing, building, and querying.
 */
public class TutorTree {

    private TreeInput treeInput;
    private TreeMapper mapper;
    private JSONArray treeArray;
    private boolean inserted;
    private ReadTree readTree;
    private DataTable current;
    private File dir;

    /**
     * Constructs a TutorTree with the specified string and treeStructure structure.
     *
     * @param input         - source of data to populate the tree.
     * @param treeStructure - the specified JSON tree structure.
     */
    public TutorTree(String input, String treeStructure, File dir) throws Exception {
        treeInput = new TreeInput(input);
        mapper = Configs.decodeObject(TreeMapper.class, treeStructure);
        treeArray = new JSONArray();
        inserted = false;
        current = null;
        this.dir = dir;
        openTree(dir);
        processBundles();
    }

    /**
     * Opens the TreeMapper.
     */
    public void openTree(File dir) {
        TaskRunConfig taskRunConfig = new TaskRunConfig(0, 1, "treeTutor", dir.getAbsolutePath());
        Files.deleteDir(this.dir);
        Files.initDirectory(this.dir);
        mapper.open(taskRunConfig);
    }

    /**
     * Processes the bundles and adds them to the treeIterator.
     */
    public void processBundles() throws Exception {
        while (treeInput.hasNextBundle()) {
            TreeBundle bundle = treeInput.getNextBundle();
            int keyIndex = 0;
            while (keyIndex < treeInput.numKeys()) {
                if (!bundle.hasNext()) {
                    mapper.sendComplete();
                    throw new NoSuchElementException();
                }
                bundle.setValue(treeInput.getKey(keyIndex));
                keyIndex++;
            }
            mapper.send(bundle.toListBundle());
        }
        mapper.sendComplete();
        readTree = new ReadTree(new File(dir, "data"));
    }

    /**
     *
     */
    public void sendComplete() {
        mapper.sendComplete();
    }

    /**
     * Closes the tree when the user is finished.
     */
    public void closeTree() {
        System.out.println("Closing " + readTree.toString());
        readTree.close();
    }

    /**
     * Returns a string representation of the treeIterator.
     */
    public String toString() {
        System.out.println(readTree.getIterator().next().toString());
        return toString("", readTree.getRootNode().getIterator());
    }

    /**
     * Recursive helper for the toString() method.
     */
    public String toString(String indent, Iterator<DataTreeNode> treeIterator) {
        String treeString = "";
        while (treeIterator.hasNext()) {
            DataTreeNode next = treeIterator.next();
            treeString = treeString + indent + next.getName() + "\n" + toString(indent + "\t", next.getIterator());
        }

        return treeString;
    }

    /**
     * Converts the treeIterator into a JSON object.
     */
    public JSONArray build() {
        return treeArray = build(readTree.getRootNode().getIterator());
    }

    /**
     * Recursive helper for the build() method.
     */
    public JSONArray build(Iterator<DataTreeNode> treeIterator) {
        JSONArray tempTreeArray = new JSONArray();
        while (treeIterator.hasNext()) {
            JSONObject jsonObject = new JSONObject();
            ReadTreeNode next = (ReadTreeNode) treeIterator.next();
            Iterator<DataTreeNode> nextNodes = next.getIterator();
            try {
                if (nextNodes.hasNext()) {
                    if (CodecJSON.encodeJSON(next).has("data")) {
                        jsonObject.put("title", next.getName() + "*");
                    } else {
                        jsonObject.put("title", next.getName());
                    }
                    jsonObject.put("children", build(nextNodes));
                    jsonObject.put("folder", true);
                } else {
                    if (CodecJSON.encodeJSON(next).has("data")) {
                        jsonObject.put("title", next.getName() + "*");
                    } else {
                        jsonObject.put("title", next.getName());
                    }
                    jsonObject.put("children", build(nextNodes));
                    jsonObject.put("folder", false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            tempTreeArray.put(jsonObject);
        }
        return tempTreeArray;
    }

    /**
     * Builds the tree one node at a time.
     */
    public JSONArray step() {
        return step(treeArray, readTree.getRootNode().getIterator());
    }

    /**
     * step() helper method
     *
     * @param jArray       - JSONArray representing the tree.
     * @param treeIterator - current node iterator.
     */
    public JSONArray step(JSONArray jArray, Iterator<DataTreeNode> treeIterator) {
        inserted = false;
        if (!treeIterator.hasNext()) {
            return new JSONArray();
        }

        while (treeIterator.hasNext() && !inserted) {
            DataTreeNode next = treeIterator.next();
            Iterator<DataTreeNode> nextNodes = next.getIterator();
            JSONObject currentObject = null;
            int i;
            for (i = 0; i < jArray.length(); i++) {
                try {
                    currentObject = jArray.getJSONObject(i);

                    if (currentObject.get("title").equals(next.getName()) || currentObject.get("title").equals(next.getName() + "*")) {
                        step(currentObject.getJSONArray("children"), nextNodes);
                        if (treeIterator.hasNext()) {
                            next = treeIterator.next();
                            nextNodes = next.getIterator();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Object currentName = new Object();
            if (currentObject != null) {
                try {
                    currentName = currentObject.get("title");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


            if (i == jArray.length() && !inserted && !(currentName.equals(next.getName()) || currentName.equals(next.getName() + "*"))) {
                JSONObject jsonObject = new JSONObject();
                try {
                    if (CodecJSON.encodeJSON(next).has("data")) {
                        jsonObject.put("title", next.getName() + "*");
                    } else {
                        jsonObject.put("title", next.getName());
                    }
                    jsonObject.put("children", new JSONArray());
                    if (nextNodes.hasNext()) {
                        jsonObject.put("folder", true);
                    } else {
                        jsonObject.put("folder", false);
                    }
                    inserted = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }

                jArray.put(jsonObject);
            }
        }
        return jArray;
    }

    /**
     * Method to query data directory.
     *
     * @param path - specified query path.
     * @param ops  - specified query ops.
     */
    public DataTable query(String path, String ops) throws Exception {
        TreeTutorQueryUtil queryUtil = new TreeTutorQueryUtil();
        current = queryUtil.runQuery(path, ops, dir);
        if (current.size() == 0) {
            int index = path.indexOf(":");
            do {
                String tempPath = path.substring(0, index) + "/+";
                current = queryUtil.runQuery(tempPath, null, dir);
                index = path.indexOf(":", index + 1);
            }
            while (current.size() != 0 && index < path.length());
        }
        return current;
    }

    /**
     * Checks the query path to check for bad paths that return no matches.
     *
     * @param path
     * @return
     * @throws Exception
     */
    public String checkPath(String path) throws Exception {
        TreeTutorQueryUtil queryUtil = new TreeTutorQueryUtil();
        String temp = path.substring(0, path.indexOf(":") > 0 ? path.indexOf(":") : path.length());
        int index = -1;
        String tempPath;
        do {
            index = temp.indexOf("/", index + 1);
            tempPath = temp.substring(0, index > 0 ? index : temp.length()) + "/+";
            current = queryUtil.runQuery(tempPath, null, dir);
        }
        while (current.size() != 0 && index > -1);

        tempPath = tempPath.substring(0, tempPath.length() - 2);

        if (current.size() == 0) {
            return "The path '" + tempPath + "' isn't returning any matches. Double check to make sure '" +
                   tempPath.substring(tempPath.lastIndexOf("/") + 1, tempPath.length()) + "' is a valid branch or if it " +
                   "actually doesn't have any matches.";
        } else {
            return "The beginning of the path specified in your query appears to be correct, but the rest of your " +
                   "query isn't returning any results. Double check to make sure the rest of your query is correct.";
        }
    }

    /**
     * Returns a JSON representation of the query results.
     */
    public JSONArray getTable() {
        JSONArray dataArray = new JSONArray();

        for (int i = 0; i < current.size(); i++) {
            Bundle bundle = current.get(i);
            JSONArray bundleArray = new JSONArray();
            for (int j = 0; j < bundle.getCount(); j++) {
                bundleArray.put(bundle.getValue(bundle.getFormat().getField(j)).asString());
            }
            dataArray.put(bundleArray);
        }

        return dataArray;
    }

    /**
     * Finds and returns the node specified by the given path.
     *
     * @param path
     * @return
     */
    public DataTreeNode find(String path) {
        DataTreeNode node = readTree.getRootNode();
        String nodeName;

        while (path != null) {
            if (path.indexOf("/") > 0) {
                nodeName = path.substring(0, path.indexOf("/"));
                path = path.substring(path.indexOf("/") + 1, path.length());
            } else {
                nodeName = path.substring(0, path.length());
                path = null;
            }

            node = node.getNode(nodeName);
        }

        return node;
    }

    /**
     * Returns the data associated with the specified title.
     */
    public JSONObject getData(String path) throws Exception {
        ReadTreeNode node = (ReadTreeNode) find(path);
        JSONObject nodeJSON = CodecJSON.encodeJSON(node);
        JSONObject data = null;
        if (nodeJSON.has("data")) {
            data = nodeJSON.getJSONObject("data");
        }
        return data;
    }
}