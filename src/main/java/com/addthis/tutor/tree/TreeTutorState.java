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

import java.io.File;

import com.addthis.bundle.table.DataTable;
import com.addthis.maljson.JSONObject;

/**
 * Stores the current values of a user's current session and provides some accessors and modifiers.
 */
public class TreeTutorState {

    private final File dir;

    private String input, configuration, path, ops;
    private TutorTree tree;
    private String stash;
    int lastStepOffset = 0;
    int stepOffset = 0;
    boolean built;

    private static final String defaultConfiguration = "paths.root: [\n" +
                                                       "  {const = root}\n" +
                                                       "]";


    /**
     * Constructor that a directory parameter.
     *
     * @param dir - directory with user's tree data.
     */
    public TreeTutorState(File dir) {
        this.input = "";
        this.configuration = defaultConfiguration;
        this.path = "";
        this.ops = "";
        this.dir = dir;
        stash = "[]";
        built = false;
    }

    /**
     * Constructor that accepts input, configuration, and directory parameters.
     *
     * @param input         - current csv input of TreeTutor state.
     * @param configuration - current json configuration of TreeTutor state.
     * @param dir           - directory with user's tree data.
     */
    public TreeTutorState(String input, String configuration, File dir) throws Exception {
        this.input = input;
        this.configuration = configuration;
        this.path = "";
        this.ops = "";
        this.dir = dir;
        tree = new TutorTree(this.input, this.configuration, this.dir);
        stash = "[]";
        built = false;
    }

    /**
     * Constructor that accepts input, configuration, and directory parameters.
     *
     * @param input         - current csv input of TreeTutor state.
     * @param configuration - current json configuration of TreeTutor state.
     * @param dir           - directory with user's tree data.
     */
    public TreeTutorState(String input, String configuration, String path, String ops, File dir)
            throws Exception {
        this.input = input;
        this.configuration = configuration;
        this.path = path;
        this.ops = ops;
        this.dir = dir;
        tree = new TutorTree(this.input, this.configuration, this.dir);
        stash = "[]";
        built = false;
    }

    /**
     * Provides access to the user's tree.
     *
     * @param input         - csv input of the tree.
     * @param configuration - json configuration of the tree.
     * @return the user's tree.
     */
    public TutorTree getTree(String input, String configuration) throws Exception {
        if (!input.equals(this.input) || !configuration.equals(this.configuration) || !built) {
            if (tree == null) {
                tree = new TutorTree(input, configuration, dir);
            } else {
                tree.closeTree();
                tree = new TutorTree(input, configuration, dir);
            }
            this.input = input;
            this.configuration = configuration;
            stepOffset = -1;
            built = true;
        }

        return tree;
    }

    public TutorTree getTree() {
        return tree;
    }

    /**
     * @param input
     * @param configuration
     * @return
     * @throws Exception
     */
    public TutorTree step(String input, String configuration) throws Exception {
        if (stepOffset != -1) {
            if (!input.equals(this.input) || !configuration.equals(this.configuration)) {
                lastStepOffset = 0;
                stepOffset = 0;
            }
            lastStepOffset = stepOffset;
            if (stepOffset == 0) {
                stepOffset = input.indexOf("\n");
            }
            stepOffset = input.indexOf("\n", stepOffset + 1);
            String selectBundles;
            if (stepOffset == -1) {
                selectBundles = input;
            } else {
                selectBundles = input.substring(0, stepOffset);
            }
            if (tree == null) {
                tree = new TutorTree(selectBundles, configuration, dir);
            } else {
                tree.closeTree();
                tree = new TutorTree(selectBundles, configuration, dir);
            }
            this.input = input;
            this.configuration = configuration;
        }
        return tree;
    }

    /**
     * @param input
     * @param configuration
     * @return
     * @throws Exception
     */
    public TutorTree back(String input, String configuration) throws Exception {
        if (!built) {
            String selectBundles = input.substring(0, lastStepOffset);
            stepOffset = lastStepOffset;
            lastStepOffset = selectBundles.lastIndexOf("\n");

            if (lastStepOffset == -1) {
                stepOffset = 0;
                lastStepOffset = 0;
                return null;
            }

            if (tree == null) {
                tree = new TutorTree(selectBundles, configuration, dir);
            } else {
                tree.closeTree();
                tree = new TutorTree(selectBundles, configuration, dir);
            }
            this.input = input;
            this.configuration = configuration;
        }
        return tree;
    }

    /**
     * @param path
     * @param ops
     * @return
     * @throws Exception
     */
    public DataTable query(String path, String ops) throws Exception {
        this.path = path;
        this.ops = ops;
        return tree.query(path, ops);
    }

    /**
     * Resets the user's state.
     */
    public void reset() {
        input = null;
        configuration = defaultConfiguration;
        if (tree != null) {
            tree.closeTree();
            tree = null;
        }

        lastStepOffset = 0;
        stepOffset = 0;
        built = false;
    }

    /**
     * Updates the user's stash. Could be adding/deleting a session or clearing the library.
     *
     * @param stash - user's updated stash.
     */
    public void updateStash(String stash) {
        this.stash = stash;
    }

    /**
     * Returns any data associated with the specified tree node.
     *
     * @param title
     * @return
     */
    public JSONObject getData(String title) throws Exception {
        return tree.getData(title);
    }

    /**
     * @return
     */
    public String getState() throws Exception {
        lastStepOffset = 0;
        stepOffset = 0;
        JSONObject stateJSON = new JSONObject();
        stateJSON.put("input", input);
        stateJSON.put("configuration", configuration);
        stateJSON.put("path", path);
        stateJSON.put("ops", ops);
        stateJSON.put("stash", stash);
        return "[" + stateJSON.toString() + "]";
    }

    public File getDir() {
        return dir;
    }
}
