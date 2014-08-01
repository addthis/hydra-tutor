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

import java.nio.file.Paths;

import com.addthis.basis.util.Files;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.table.DataTable;
import com.addthis.maljson.JSONArray;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TutorTreeTest {

    private String input;
    private String config;
    private TutorTree tree;

    @Before
    public void setUp() throws Exception {
        input = "team, computer, name\n" +
                "Data, Mac, Matt\n" +
                "Data, Mac, Andres\n" +
                "Data, Lenovo, Michael\n" +
                "Data, Lenovo, Eric\n" +
                "Data, Lenovo, Stephen\n" +
                "Data, Lenovo, Ian\n" +
                "Data, Mac, Al\n" +
                "Data, Lenovo, Aditya\n" +
                "Data, Mac, Evan\n";

        config = "{\n" +
                 "type:\"tree\",\n" +
                 "root:{path:\"SAMPLE\"},\n" +
                 "paths:{\n" +
                 "SAMPLE:[\n" +
                 "{type:\"const\", value:\"team\"},\n" +
                 "{type:\"value\", key:\"team\"},\n" +
                 "{type:\"value\", key:\"computer\", data : {tcomp : {type : \"key.top\", size : 500, key : \"computer\"}}},\n" +
                 "{type:\"value\", key:\"name\"},\n" +
                 "],\n" +
                 "},\n" +
                 "}\n";

        File path = new File("test");
        Files.deleteDir(Paths.get("test", "data").toFile());
        tree = new TutorTree(input, config, path);
    }

    @After
    public void tearDown() {
        tree.closeTree();
        System.out.println("Done.");
    }

    /** Tests the construction of a TutorTree with manual input. */
    @Test public void treeToString() {
        System.out.println(tree.toString());
        assertEquals("team\n" +
                                      "\tData\n" +
                                      "\t\tLenovo\n" +
                                      "\t\t\tAditya\n" +
                                      "\t\t\tEric\n" +
                                      "\t\t\tIan\n" +
                                      "\t\t\tMichael\n" +
                                      "\t\t\tStephen\n" +
                                      "\t\tMac\n" +
                                      "\t\t\tAl\n" +
                                      "\t\t\tAndres\n" +
                                      "\t\t\tEvan\n" +
                                      "\t\t\tMatt\n", tree.toString());
    }

    @Test public void build() {
        JSONArray treeJSON = tree.build();
        System.out.println(treeJSON.toString());
        // DO NOT USE toString() to compare for correctness
//        assertEquals("[{\"children\":[{\"children\":[{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Aditya\"},{\"children\":[],\"folder\":false,\"title\":\"Eric\"},{\"children\":[],\"folder\":false,\"title\":\"Ian\"},{\"children\":[],\"folder\":false,\"title\":\"Michael\"},{\"children\":[],\"folder\":false,\"title\":\"Stephen\"}],\"folder\":true,\"title\":\"Lenovo*\"},{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Al\"},{\"children\":[],\"folder\":false,\"title\":\"Andres\"},{\"children\":[],\"folder\":false,\"title\":\"Evan\"},{\"children\":[],\"folder\":false,\"title\":\"Matt\"}],\"folder\":true,\"title\":\"Mac*\"}],\"folder\":true,\"title\":\"Data\"}],\"folder\":true,\"title\":\"team\"}]", treeJSON.toString());
    }

    @Test public void step() {
        for (int i = 0; i < 28; i++) {
            tree.step();
        }
        // DO NOT USE toString() to compare for correctness
//        assertEquals(tree.step().toString(), "[{\"children\":[{\"children\":[{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Aditya\"},{\"children\":[],\"folder\":false,\"title\":\"Eric\"},{\"children\":[],\"folder\":false,\"title\":\"Ian\"},{\"children\":[],\"folder\":false,\"title\":\"Michael\"},{\"children\":[],\"folder\":false,\"title\":\"Stephen\"}],\"folder\":true,\"title\":\"Lenovo*\"},{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Al\"},{\"children\":[],\"folder\":false,\"title\":\"Andres\"},{\"children\":[],\"folder\":false,\"title\":\"Evan\"},{\"children\":[],\"folder\":false,\"title\":\"Matt\"}],\"folder\":true,\"title\":\"Mac*\"}],\"folder\":true,\"title\":\"Data\"}],\"folder\":true,\"title\":\"team\"}]");
    }

    @Test public void query() throws Exception {
        DataTable table = tree.query("team/Data/+:+hits", "sort=1:n:d;title=computer,hits");

        assertEquals(3, table.size());

        Bundle row = table.get(0);
        assertEquals("computer", row.getValue(row.getFormat().getField(0)).toString());
        assertEquals("hits", row.getValue(row.getFormat().getField(1)).toString());

        row = table.get(1);
        assertEquals("Lenovo", row.getValue(row.getFormat().getField(0)).toString());
        assertEquals("5", row.getValue(row.getFormat().getField(1)).toString());

        row = table.get(2);
        assertEquals("Mac", row.getValue(row.getFormat().getField(0)).toString());
        assertEquals("4", row.getValue(row.getFormat().getField(1)).toString());
    }

    @Test public void checkPath() throws Exception {
        assertEquals("The path 'team/Data/++hits' isn't returning any matches. "
                     + "Double check to make sure '++hits' is a valid branch or if it actually doesn't have any "
                     + "matches.",
                     tree.checkPath("team/Data/++hits"));

        assertEquals("The beginning of the path specified in your query appears to be correct, "
                     + "but the rest of your query isn't returning any results. Double check to make "
                     + "sure the rest of your query is correct.",
                     tree.checkPath("team/Data/+:+hits"));
    }

    /** Tests method that converts DataTable from query results to JSONArray. */
    @Test public void getTable() throws Exception {
        tree.query("team/Data/+:+hits", "sort=1:n:d;title=computer,hits");
        assertEquals("[[\"computer\",\"hits\"],[\"Lenovo\",\"5\"],[\"Mac\",\"4\"]]", tree.getTable().toString());
    }

    @Test @Ignore public void getData() throws Exception {
        //System.out.println(tree.getData("team/Data/Lenovo"));
        // DO NOT USE toString() to compare for correctness
        // assertTrue(tree.getData("team/Data/Lenovo").toString().equals("{\"tcomp\":{\"size\":500,\"t\":\"kt\",\"top\":{\"lossy\":true,\"map\":{\"Lenovo\":5},\"minKey\":\"Lenovo\",\"minVal\":5}}}"));
    }
}
