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
package com.addthis.tutor.dropwizard;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import com.addthis.basis.util.LessFiles;

import com.addthis.bundle.table.DataTable;
import com.addthis.codec.annotations.Pluggable;
import com.addthis.codec.plugins.PluginMap;
import com.addthis.codec.plugins.PluginRegistry;
import com.addthis.hydra.data.tree.TreeNodeData;
import com.addthis.maljson.JSONException;
import com.addthis.maljson.JSONObject;
import com.addthis.tutor.tree.TreeRemovalListener;
import com.addthis.tutor.tree.TreeTutorShutdownThread;
import com.addthis.tutor.tree.TreeTutorState;
import com.addthis.tutor.tree.TutorTree;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Resource class for TreeTutor. Provides resources for building and querying a tree, resetting the user's session, and
 * updating and providing acess to a user's stash.
 */
@Path("/tree")
public class TreeTutorResource {

    private final Cache<String, TreeTutorState> userState;

    /**
     *
     */
    public TreeTutorResource(HydraTutorConfiguration config) {
        userState = CacheBuilder.newBuilder().maximumSize(1000)
                .removalListener(new TreeRemovalListener())
                .expireAfterWrite(2, TimeUnit.DAYS).build();

        Runtime.getRuntime().addShutdownHook(new TreeTutorShutdownThread(userState));
    }

    private File generatePath(String uid) throws IOException {
        return LessFiles.createTempDir("treetutor", uid).getAbsoluteFile();
    }

    /**
     * Resource for building a tree given a csv input and configuration.
     *
     * @return a JSON representation of the tree.
     * @QueryParam inputText, @Param input - specified csv input to populate the tree.
     * @QueryParam configuration, @Param configuration - specified json configuration to construct the tree.
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     */
    @GET
    @Path("/build")
    @Produces(MediaType.APPLICATION_JSON)
    public Response build(@QueryParam("inputText") String input,
            @QueryParam("configuration") String configuration,
            @QueryParam("uid") String uid) {
        try {
            TreeTutorState state = userState.getIfPresent(uid);

            if (state == null) {
                File path = generatePath(uid);
                state = new TreeTutorState(input, configuration, path);
                userState.put(uid, state);
            }

            TutorTree tutor = state.getTree(input, configuration);


            return Response.ok(tutor.build().toString()).build();
        } catch (JSONException e) {
            e.printStackTrace();
            return Response.ok(e.getMessage()).build();
        } catch (NoSuchElementException e) {
            String error = "It appears that you have entered an invalid Input. Double check to make sure that you "
                           + "haven't forgotten a data value or are missing a ','.";
            e.printStackTrace();
            return Response.ok(error).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(e.getMessage()).build();
        }
    }

    /**
     * Resource for step function which builds the tree one node at a time.
     *
     * @return a JSON representation of the tree.
     * @QueryParam inputText, @Param input - specified csv input to populate the tree.
     * @QueryParam configuration, @Param configuration - specified json configuration to construct the tree.
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     */
    @GET
    @Path("/step")
    @Produces(MediaType.APPLICATION_JSON)
    public Response step(@QueryParam("inputText") String input, @QueryParam("configuration") String configuration,
            @QueryParam("uid") String uid) {
        try {
            TreeTutorState state = userState.getIfPresent(uid);

            if (state == null) {
                File path = generatePath(uid);
                state = new TreeTutorState(input, configuration, path);
                userState.put(uid, state);
            }

            TutorTree tutor = state.step(input, configuration);


            return Response.ok(tutor.build().toString()).build();
        } catch (JSONException e) {
            e.printStackTrace();
            return Response.ok(e.getMessage()).build();
        } catch (NoSuchElementException e) {
            String error = "It appears that you have entered an invalid Input. Double check to make sure that you "
                           + "haven't forgotten a data value or are missing a ','.";
            e.printStackTrace();
            return Response.ok(error).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(e.getMessage()).build();
        }

    }

    /**
     * Resource for back function which reverts back to the last step of the tree.
     *
     * @return a JSON representation of the tree.
     * @QueryParam inputText, @Param input - specified csv input to populate the tree.
     * @QueryParam configuration, @Param configuration - specified json configuration to construct the tree.
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     */
    @GET
    @Path("/back")
    @Produces(MediaType.APPLICATION_JSON)
    public Response back(@QueryParam("inputText") String input, @QueryParam("configuration") String configuration,
            @QueryParam("uid") String uid) {
        try {
            TreeTutorState state = userState.getIfPresent(uid);

            if (state == null) {
                File path = generatePath(uid);
                state = new TreeTutorState(input, configuration, path);
                userState.put(uid, state);
            }

            TutorTree tutor = state.back(input, configuration);

            if (tutor == null) {
                return Response.ok("[]").build();
            }

            return Response.ok(tutor.build().toString()).build();
        } catch (JSONException e) {
            e.printStackTrace();
            return Response.ok(e.getMessage()).build();
        } catch (NoSuchElementException e) {
            String error = "It appears that you have entered an invalid Input. Double check to make sure that you "
                           + "haven't forgotten a data value or are missing a ','.";
            e.printStackTrace();
            return Response.ok(error).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.ok(e.getMessage()).build();
        }

    }

    /**
     * Resource for query function.
     *
     * @return a table of nodes that match the query.
     * @QueryParam inputText, @Param input - specified csv input to populate the tree.
     * @QueryParam configuration, @Param configuration - specified json configuration to construct the tree.
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     * @QueryParam path, @Param path - query path that the user specifies.
     * @QueryParam ops, @Param ops - query ops that the user specifies.
     */
    @GET
    @Path("/query")
    @Produces(MediaType.APPLICATION_JSON)
    public Response query(@QueryParam("path") String path, @QueryParam("ops") String ops, @QueryParam("uid") String uid)
            throws Exception {

        TreeTutorState state = userState.getIfPresent(uid);
        TutorTree tutor = (state == null) ? null : state.getTree();

        try {
            DataTable table = (state == null) ? null : state.query(path, ops);

            if (table == null || tutor == null) {
                return Response.ok("You have to build a tree before you can run a query.").build();
            }

            if (table.size() == 0) {
                return Response.ok(tutor.checkPath(path)).build();
            }
            return Response.ok(tutor.getTable().toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            String msg = e.getMessage();
            if (tutor != null) {
                msg += "\n" + tutor.checkPath(path);
            }
            return Response.ok(msg).build();
        }
    }

    /**
     * Resource for resetting the tutor state.
     *
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     */
    @GET
    @Path("/reset")
    public String reset(@QueryParam("uid") String uid) {
        try {
            TreeTutorState state = userState.getIfPresent(uid);

            if (state == null) {
                File path = generatePath(uid);
                state = new TreeTutorState(null, null, path);
                userState.put(uid, state);
            }

            state.reset();

            return "Your session has been reset.";
        } catch (Exception e) {
            e.printStackTrace();
            return "There was an error resetting your session.\n" + e.getMessage();
        }
    }

    /**
     * Resource for updating the stash.
     *
     * @QueryParam inputText, @Param input - specified csv input to populate the tree.
     * @QueryParam configuration, @Param configuration - specified json configuration to construct the tree.
     * @QueryParam stash, @Param stash - user's current stash.
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     */
    @GET
    @Path("/updateStash")
    public String updateStash(@QueryParam("inputText") String input, @QueryParam("configuration") String configuration,
            @QueryParam("stash") String stash, @QueryParam("uid") String uid) {
        try {
            TreeTutorState state = userState.getIfPresent(uid);

            if (state == null) {
                File path = generatePath(uid);
                state = new TreeTutorState(input, configuration, path);
                userState.put(uid, state);
            }

            state.updateStash(stash);

            return "Your stash has been updated.";
        } catch (Exception e) {
            e.printStackTrace();
            return "There was an error updating your stash.\n" + e.getMessage();
        }

    }

    /**
     * Resource for accessing the users current stash.
     *
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     */
    @GET
    @Path("/getState")
    public String getState(@QueryParam("uid") String uid) {
        try {
            TreeTutorState state = userState.getIfPresent(uid);
            if (state == null) {
                File path = generatePath(uid);
                state = new TreeTutorState(path);
                userState.put(uid, state);
            }

            String result = state.getState();
            System.out.println(result);
            return result;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    /**
     * Resource for retrieving a node's data attachments.
     *
     * @QueryParam title, @Param title - name of the node whose date is being retrieved.
     * @QueryParam uid, @Param uid - cookie used to identify and look up correct user state.
     */
    @GET
    @Path("/getData")
    public String getData(@QueryParam("path") String path, @QueryParam("uid") String uid) {
        JSONObject attachments = new JSONObject();
        JSONObject links = new JSONObject();
        try {
            TreeTutorState state = userState.getIfPresent(uid);
            if (path.endsWith("*")) {
                JSONObject data = state.getData(path.substring(0, path.length() - 1));
                Iterator keys = data.keys();
                Pluggable pluggable = TreeNodeData.class.getAnnotation(Pluggable.class);
                PluginRegistry pluginRegistry = PluginRegistry.defaultRegistry();
                PluginMap pluginMap = pluginRegistry.asMap().get(pluggable.value());
                while (keys.hasNext()) {
                    JSONObject dataJSON = data.getJSONObject(keys.next().toString());
                    String dataString = dataJSON.getString("t");
                    String dataType = pluginMap.asBiMap().get(dataString).getSimpleName();
                    links.put(dataType, "http://oss-docs.addthiscode.net/hydra/latest/user-reference/com/addthis/hydra/data/tree/prop/" + dataType + ".Config.html");
                }

                attachments.put("data", "[" + data.toString() + "]");
                attachments.put("links", "[" + links + "]");
                return "[" + attachments.toString() + "]";
            } else {
                attachments.put("data", "None");
                attachments.put("links", "[" + links + "]");
                return "[" + attachments.toString() + "]";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "There was an error retrieving the data:" + e.getMessage();
        }
    }
}
