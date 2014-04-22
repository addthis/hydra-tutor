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

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import java.nio.file.Paths;

import com.addthis.basis.util.Files;

import com.addthis.bundle.core.Bundle;
import com.addthis.bundle.table.DataTable;
import com.addthis.maljson.JSONArray;

/**
 * Provides test cases for the TreeTutor.
 */
public class TutorTreeTest extends TestCase {

    private String input;
    private String config;
    private TutorTree tree;

    /**
     * Constructs a TutorTreeTestObject.
     */
    public TutorTreeTest() {
        super();
    }

    /**
     * Sets up the tests for TutorTreeTest.
     */
    @Before
    public void setUp() {
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

        try {
            File path = new File("test");
            Files.deleteDir(Paths.get("test", "data").toFile());
            tree = new TutorTree(input, config, path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tears down the test cases after completion.
     */
    @After
    public void tearDown() {
        tree.closeTree();
        System.out.println("Done.");
    }

    /**
     * Tests the construction of a TutorTree with manual input.
     */
    @Test
    public void testToString() {
        System.out.println(tree.toString());
        assertTrue(tree.toString().equals("team\n" +
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
                                          "\t\t\tMatt\n"));
    }

    /**
     * Test build()
     */
    @Test
    public void testBuild() {
        JSONArray treeJSON = tree.build();
        System.out.println(treeJSON.toString());
        // DO NOT USE toString() to compare for correctness
//        assertEquals("[{\"children\":[{\"children\":[{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Aditya\"},{\"children\":[],\"folder\":false,\"title\":\"Eric\"},{\"children\":[],\"folder\":false,\"title\":\"Ian\"},{\"children\":[],\"folder\":false,\"title\":\"Michael\"},{\"children\":[],\"folder\":false,\"title\":\"Stephen\"}],\"folder\":true,\"title\":\"Lenovo*\"},{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Al\"},{\"children\":[],\"folder\":false,\"title\":\"Andres\"},{\"children\":[],\"folder\":false,\"title\":\"Evan\"},{\"children\":[],\"folder\":false,\"title\":\"Matt\"}],\"folder\":true,\"title\":\"Mac*\"}],\"folder\":true,\"title\":\"Data\"}],\"folder\":true,\"title\":\"team\"}]", treeJSON.toString());
    }

    /**
     * Test step()
     */
    @Test
    public void testStep() {
        for (int i = 0; i < 28; i++) {
            tree.step();
        }
        // DO NOT USE toString() to compare for correctness
//        assertEquals(tree.step().toString(), "[{\"children\":[{\"children\":[{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Aditya\"},{\"children\":[],\"folder\":false,\"title\":\"Eric\"},{\"children\":[],\"folder\":false,\"title\":\"Ian\"},{\"children\":[],\"folder\":false,\"title\":\"Michael\"},{\"children\":[],\"folder\":false,\"title\":\"Stephen\"}],\"folder\":true,\"title\":\"Lenovo*\"},{\"children\":[{\"children\":[],\"folder\":false,\"title\":\"Al\"},{\"children\":[],\"folder\":false,\"title\":\"Andres\"},{\"children\":[],\"folder\":false,\"title\":\"Evan\"},{\"children\":[],\"folder\":false,\"title\":\"Matt\"}],\"folder\":true,\"title\":\"Mac*\"}],\"folder\":true,\"title\":\"Data\"}],\"folder\":true,\"title\":\"team\"}]");
    }

    /**
     * Provides some tests for query features.
     */
    @Test
    public void testQuery() throws Exception {
        DataTable table = tree.query("team/Data/+:+hits", "sort=1:n:d;title=computer,hits");

        assertEquals(3, table.size());

        Bundle row = table.get(0);
        assertTrue(row.getValue(row.getFormat().getField(0)).asString().equals("computer"));
        assertTrue(row.getValue(row.getFormat().getField(1)).asString().equals("hits"));

        row = table.get(1);
        assertTrue(row.getValue(row.getFormat().getField(0)).asString().equals("Lenovo"));
        assertTrue(row.getValue(row.getFormat().getField(1)).asString().equals("5"));

        row = table.get(2);
        assertTrue(row.getValue(row.getFormat().getField(0)).asString().equals("Mac"));
        assertTrue(row.getValue(row.getFormat().getField(1)).asString().equals("4"));
    }

    /**
     *
     */
    public void testCheckPath() throws Exception {
        assertTrue(tree.checkPath("team/Data/++hits").equals("The path 'team/Data/++hits' isn't returning any matches. "
                                                             + "Double check to make sure '++hits' is a valid branch or if it actually doesn't have any matches."));

        assertTrue(tree.checkPath("team/Data/+:+hits").equals("The beginning of the path specified in your query " +
                                                              "appears to be correct, but the rest of your query isn't returning any results. Double check to make " +
                                                              "sure the rest of your query is correct."));
    }

    /**
     * Tests method that converts DataTable from query results to JSONArray.
     */
    @Test
    public void testGetTable() throws Exception {
        tree.query("team/Data/+:+hits", "sort=1:n:d;title=computer,hits");
        assertTrue(tree.getTable().toString().equals("[[\"computer\",\"hits\"],[\"Lenovo\",\"5\"],[\"Mac\",\"4\"]]"));
    }

    /**
     * Tests retrieving data from a specified node.
     */
    public void testGetData() throws Exception {
        //System.out.println(tree.getData("team/Data/Lenovo"));
        // DO NOT USE toString() to compare for correctness
        // assertTrue(tree.getData("team/Data/Lenovo").toString().equals("{\"tcomp\":{\"size\":500,\"t\":\"kt\",\"top\":{\"lossy\":true,\"map\":{\"Lenovo\":5},\"minKey\":\"Lenovo\",\"minVal\":5}}}"));
    }
}
