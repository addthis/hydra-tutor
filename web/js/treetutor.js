window.ttapp = {};

ttapp.url = "/validate/post";
ttapp.reset = "/validate/reset";

ttapp.stash = [];
ttapp.stash_query = function(temp_query_obj) {
    ttapp.stash.push(temp_query_obj);
};

ttapp.current = "";

ttapp.path = function() 
{
    var pathArray = window.location.pathname.split( "/" );
    return pathArray.pop();
};



$(document).ready(function()
{
    ttapp.stashchange = $.Callbacks();
    ttapp.source   = $("#library-template").html();
    ttapp.template = Handlebars.compile(ttapp.source);

    ttapp.library_rerender = function(data)
    {
        var context = ttapp.stash;
        var data = { items: ttapp.stash };
        $("#librarybox").html(ttapp.template(data));
    }

    var inputbox = ace.edit("inputbox");
    inputbox.setTheme("ace/theme/xcode");
    inputbox.getSession().setMode("ace/mode/text");

    var configbox = ace.edit("configbox");
    configbox.setTheme("ace/theme/xcode");
    configbox.getSession().setMode("ace/mode/json");

    var populate_textareas = function(singleresultarray) 
    {
        var inputText       = inputbox.setValue(singleresultarray[0].input);
        var configuration   = configbox.setValue(singleresultarray[0].config);
        var path            = $("#path").val(singleresultarray[0].path);
        var ops             = $("#ops").val(singleresultarray[0].ops);
    };
//--------------------------------------------------------------------------------------------------------------------//
	///////////////
	//* COOKIES *//
	///////////////

    COOK = "uid"; //value for cookie

    var get_new_cookie = function()
    {
        return $.md5(String(Math.random()));
    }

    var delete_user_cookie = function()
    {
        $.cookie(COOK, null, { path: "/"});
    }

    var stash_state = function(input, config, path, ops) 
    {
        temp_query_obj = 
        {
            uid : $.cookie(COOK),
            input : input,
            config : config,
            path : path,
            ops : ops,
            date : String(new Date()),
            href :"#",
            id : get_new_cookie()
        };
        ttapp.stash_query(temp_query_obj);
    }

    // check for cookie
    if (!($.cookie(COOK))) // if cookie doesn"t exist for user, give them one
    {
        usercookie = get_new_cookie();
        $.cookie(COOK, get_new_cookie(), { path: "/", expires: 9999 });
    }
    else
    {
        $.ajax(
            {
                url: "/tree/getState",
                type: "GET",
                data:
                {
                    uid: $.cookie(COOK)
                },
                success: function(data)
                {
                    var state = JSON.parse(data);
                    inputbox.setValue(state[0].input);
                    configbox.setValue(state[0].configuration);
                    $("#path").val(state[0].path);
                    $("#ops").val(state[0].ops);
                    ttapp.stash = JSON.parse(state[0].stash);
                    ttapp.library_rerender();
                },
                error: function(data)
                {
                    $("#messagebox").val(data[0]);
                }
            });
    }
//--------------------------------------------------------------------------------------------------------------------//
	/////////////////////
	// Button Handlers //
	/////////////////////

    inputText = inputbox.getValue();
    configuration = configbox.getValue();
    path = $("#path").val();
    ops = $("#ops").val();
    var uid = $.cookie(COOK);
    var gotData = false;
    var step = false;

    //Retrieves the data attachments associated with the node from the given path and also provides links to those data attachments.
    var getDataAttachments = function(path)
    {
        $.ajax(
            {
                url: "/tree/getData",
                type: "GET",
                data:
                {
                    path: path,
                    uid: $.cookie(COOK)
                },
                success: function(data)
                {
                    $("#links").empty();
                    var attachments = JSON.parse(data);
                    $("#databox").val("");
                    if(attachments[0].data == "None")
                    {
                        $("#databox").val("None");
                    }
                    else
                    {
                        var dattachments = JSON.parse(attachments[0].data);
                        for(var attachment in dattachments[0])
                        {
                            $("#databox").val($("#databox").val() + "\"" + attachment + "\"" + ":" + JSON.stringify(dattachments[0][attachment]) + "\n");
                        }
                    }
                    var links = JSON.parse(attachments[0].links);
                    var list = $("<ul></ul>");
                    for(var link in links[0])
                    {
                        list.append("<li><a target=\"_blank\" href=\"" + links[0][link] + "\">" + link + "</a></li>");
                    }
                    $("#links").append(list);
                },
                error: function(data)
                {
                    $("#messagebox").val(data.responseText);
                }
            });
    }

    //Builds the tree.
    $("#build-button").click(function(event)
    {
        if(inputbox.getValue() == "" || configbox.getValue() == "")
        {
            $("#messagebox").val("Please enter a tree input and configuration before building.");
        }
        else if(inputbox.getValue() == inputText && configbox.getValue() == configuration && step != true)
        {
            $("#messagebox").val("You've already built a tree with that input and configuration.");
        }
        else
        {
            $.ajax(
            {
                url: "/tree/build",
                type: "GET",
                data:
                {
                    inputText: inputbox.getValue(),
                    configuration: configbox.getValue(),
                    uid: $.cookie(COOK)
                },
                success: function(data)
                {
                    $("#tree").fancytree(
                    {
                        source: data,
                        fx: null,
                        activate: function(event, data)
                        {
                            var node = data.node;
                            var nodePath = node.title;
                            var parent = node.parent;
                            while(parent != null)
                            {
                                nodePath = parent.title + "/" + nodePath;
                                parent = parent.parent;
                            }
                            nodePath = nodePath.substring(5, nodePath.length);
                            getDataAttachments(nodePath);
                        },
                        dblclick: function(event, data)
                        {
                            event.preventDefault();
                            var node = data.node;
                            var nodePath = node.title;
                            if(nodePath.charAt(nodePath.length - 1) == "*")
                            {
                                nodePath = nodePath.substring(0, nodePath.length - 1);
                            }
                            var parent = node.parent;
                            while(parent != null)
                            {
                                if(parent.title.charAt(parent.title.length - 1) == "*")
                                {
                                    nodePath = parent.title.substring(0, parent.title.length - 1) + "/" + nodePath;
                                }
                                else
                                {
                                    nodePath = parent.title + "/" + nodePath;
                                }
                                parent = parent.parent;
                            }
                            nodePath = nodePath.substring(5, nodePath.length);
                            $("#path").val(nodePath + ":+hits");
                            $("#ops").val("title=hits");
                            $("#query-button").click();
                        }
                    });
                    var tree = $("#tree").data("fancytree").getTree();
                    tree.reload(data);
                    gotData = true;
                    step = false;
                    inputText = inputbox.getValue();
                    configuration = configbox.getValue();
                    path = "";
                    ops = "";
                    $("#resultsTable").empty();
                    $("#messagebox").val("");
                    $("#databox").val("");
                    $("#linksbox").val("");
                },
                error: function(data)
                {
                    $("#messagebox").val(data.responseText);
                }
            });
        }
    });

    //Builds the tree one bundle at a time.
    $("#step-button").click(function(event)
    {
        $.ajax(
        {
            url: "/tree/step",
            type: "GET",
            data:
            {
                inputText: inputbox.getValue(),
                configuration: configbox.getValue(),
                uid: $.cookie(COOK)
            },
            success: function(data)
            {
                $("#tree").fancytree(
                {
                    source: data,
                    fx: null,
                    activate: function(event, data)
                    {
                        var node = data.node;
                        var path = node.title;
                        var parent = node.parent;
                        while(parent != null)
                        {
                            path = parent.title + "/" + path;
                            parent = parent.parent;
                        }
                        path = path.substring(5, path.length);
                        getDataAttachments(path);
                    },
                    dblclick: function(event, data)
                    {
                        event.preventDefault();
                        var node = data.node;
                        var nodePath = node.title;
                        if(nodePath.charAt(nodePath.length - 1) == "*")
                        {
                            nodePath = nodePath.substring(0, nodePath.length - 1);
                        }
                        var parent = node.parent;
                        while(parent != null)
                        {
                            if(parent.title.charAt(parent.title.length - 1) == "*")
                            {
                                nodePath = parent.title.substring(0, parent.title.length - 1) + "/" + nodePath;
                            }
                            else
                            {
                                nodePath = parent.title + "/" + nodePath;
                            }
                            parent = parent.parent;
                        }
                        nodePath = nodePath.substring(5, nodePath.length);
                        $("#path").val(nodePath + ":+hits");
                        $("#ops").val("title=hits");
                        $("#query-button").click();
                    }
                });
                var tree = $("#tree").data("fancytree").getTree();
                tree.reload(data);
                $("#tree").fancytree("getRootNode").visit(function(node)
                {
                    node.setExpanded(true);
                });
                gotData = true;
                step = true;
                inputText = inputbox.getValue();
                configuration = configbox.getValue();
                path = "";
                ops = "";
                $("#resultsTable").empty();
                $("#messagebox").val("");
                $("#databox").val("");
                $("#linksbox").val("");
            },
            error: function(data)
            {
                $("#messagebox").val(data.responseText);
            }
        });
    });

    //Goes back to the last step in the tree.
    $("#back-button").click(function(event)
        {
            $.ajax(
            {
                url: "/tree/back",
                type: "GET",
                data:
                {
                    inputText: inputbox.getValue(),
                    configuration: configbox.getValue(),
                    uid: $.cookie(COOK)
                },
                success: function(data)
                {
                    $("#tree").fancytree(
                    {
                        source: data,
                        fx: null,
                        activate: function(event, data)
                        {
                            var node = data.node;
                            var path = node.title;
                            var parent = node.parent;
                            while(parent != null)
                            {
                                path = parent.title + "/" + path;
                                parent = parent.parent;
                            }
                            path = path.substring(5, path.length);
                            getDataAttachments(path);
                        },
                        dblclick: function(event, data)
                        {
                            event.preventDefault();
                            var node = data.node;
                            var nodePath = node.title;
                            if(nodePath.charAt(nodePath.length - 1) == "*")
                            {
                                nodePath = nodePath.substring(0, nodePath.length - 1);
                            }
                            var parent = node.parent;
                            while(parent != null)
                            {
                                if(parent.title.charAt(parent.title.length - 1) == "*")
                                {
                                    nodePath = parent.title.substring(0, parent.title.length - 1) + "/" + nodePath;
                                }
                                else
                                {
                                    nodePath = parent.title + "/" + nodePath;
                                }
                                parent = parent.parent;
                            }
                            nodePath = nodePath.substring(5, nodePath.length);
                            $("#path").val(nodePath + ":+hits");
                            $("#ops").val("title=hits");
                            $("#query-button").click();
                        }
                    });
                    var tree = $("#tree").data("fancytree").getTree();
                    tree.reload(data);
                    $("#tree").fancytree("getRootNode").visit(function(node)
                    {
                        node.setExpanded(true);
                    });
                    gotData = true;
                    step = true;
                    inputText = inputbox.getValue();
                    configuration = configbox.getValue();
                    path = "";
                    ops = "";
                    $("#resultsTable").empty();
                    $("#messagebox").val("");
                    $("#databox").val("");
                    $("#linksbox").val("");
                },
                error: function(data)
                {
                    $("#messagebox").val(data.responseText);
                }
            });
        });

    //These allow querying by hitting the Enter/Return key.
    $("#path").keyup(function(event)
    {
        if(event.keyCode == 13)
        {
            $("#query-button").click();
        }
    });

    $("#ops").keyup(function(event)
    {
        if(event.keyCode == 13 && $("#path").val() != "")
        {
            $("#query-button").click();
        }
    });

    //Queries the current tree.
    $("#query-button").click(function(event)
    {
        if($("#querydiv").is(":hidden"))
        {
            $("#librarydiv").hide(); 
            $("#querydiv").show();
        }
        else if($("#tree").html() == "")
        {
            $("#messagebox").val("You have to build a tree before you can run a query.");
        }
        else if(inputbox.getValue() != inputText || configbox.getValue() != configuration)
        {
            $("#messagebox").val("Please rebuild your tree with the current input and configuration before querying.");
        }
        else if($("#path").val() == "" && $("#ops").val() == "")
        {
            $("#messagebox").val("Please enter path and ops before trying to run a query.");
        }
        else if($("#path").val() == path && $("#ops").val() == ops)
        {
            $("#messagebox").val("You've already run a query with that path and ops.");
        }
        else
        {

            $("#resultsTable").empty();
            
            $.ajax(
            {
                url: "/tree/query",
                type: "GET",
                data:
                {
                    path: $("#path").val(),
                    ops: $("#ops").val(),
                    uid: $.cookie(COOK)
                },
                dataType:"json",
                success: function(data)
                {
                    var table = $("<table class = \"table table-bordered table-striped table-hover\"></table>");
                    for(var i = 0; i < data.length; i++)
                    {
                        var tr = $("<tr></tr>");
                        var row = data[i];
                        for(var j = 0; j < row.length; j++)
                        {
                            var td = $("<td>" + row[j] + "</td>");
                            tr.append(td);
                        }
                        table.append(tr);
                    }
                    path = $("#path").val();
                    ops = $("#ops").val();   
                    $("#resultsTable").append(table);
                    $("#messagebox").val("");
                },
                error: function(data)
                {
                    path = null;
                    ops = null;
                    $("#messagebox").val(data.responseText);
                }
            });
        }
    });

    //Saves the current session and stashes it in the library.
    $("#stash-button").on("click", function(e)
    {
        if(inputbox.getValue() == "" || configbox.getValue() == "")
        {
            $("#messagebox").val("Please enter a tree input and configuration before stashing.");
        }
        else
        {
            e.preventDefault();
            stash_state(inputbox.getValue(), configbox.getValue(), $("#path").val(), $("#ops").val());
            ttapp.library_rerender();
            ttapp.current=temp_query_obj.id;
            $.ajax(
            {
                url: "/tree/updateStash",
                type: "GET",
                data:
                {
                    inputText: inputbox.getValue(),
                    configuration: configbox.getValue(),
                    stash: JSON.stringify(ttapp.stash),
                    uid: $.cookie(COOK)
                },
                success: function(data)
                {
                    $("#messagebox").val(data);
                },
                error: function(data)
                {
                    $("#messagebox").val(data.responseText);
                }
            });
        }
    });

    //Clears out the library.
    $("#librarydiv").on("click", ".delete-lib-all", function(e)
    {
        e.preventDefault();
        ttapp.stash = [];
        $.ajax(
        {
            url: "/tree/updateStash",
            type: "GET",
            data:
            {
                inputText: inputbox.getValue(),
                configuration: configbox.getValue(),
                stash: JSON.stringify(ttapp.stash),
                uid: $.cookie(COOK)
            },
            success: function(data)
            {
                $("#messagebox").val(data);
            },
            error: function(data)
            {
                $("#messagebox").val(data.responseText);
            }
        });
        ttapp.library_rerender();
    });

    //Deletes the individual entry from the library.
    $("#librarybox").on("click", ".delete-lib-item", function(e)
    {
        e.preventDefault();
        var myid = $(this).parent().data("id");

        var results = jQuery.grep(ttapp.stash, function(element, index)
        {
            return element.id!==myid; // retain appropriate elements
        });

        ttapp.stash = results;
        $.ajax(
        {
            url: "/tree/updateStash",
            type: "GET",
            data:
            {
                inputText: inputbox.getValue(),
                configuration: configbox.getValue(),
                stash: JSON.stringify(ttapp.stash),
                uid: $.cookie(COOK)
            },
            success: function(data)
            {
                $("#messagebox").val(data);
            },
            error: function(data)
            {
                $("#messagebox").val(data.responseText);
            }
        }); 
        ttapp.library_rerender();

        // lookup local storage and delete
    });

    //Loads the saved session to the tutor.
    $("#librarybox").on("click", ".library-link", function(e)
    {
        e.preventDefault();
        var myid = $(this).parent().data("id");
        var result = jQuery.grep(ttapp.stash, function(element, index)
        {
            return element.id===myid; // retain appropriate elements
        });
        ttapp.current=myid;
        populate_textareas(result);
        $("#resultsTable").empty();
        $("#messagebox").val("");
        $("#databox").val("");
        $("#links").empty();
        inputText = "";
        configuration = "";
        path = "";
        ops = "";
        // lookup local storage and delete
        if(gotData == true)
        {
            $("#tree").data("fancytree").destroy();
            $.ajax(
            {
                url: "/tree/reset",
                type: "GET",
                data:
                {
                    uid: $.cookie(COOK)
                }
            });
            gotData = false;
        }
    });

    //Resets the current session.
    $("#reset-button").click(function(event)
    {
        inputText = "";
        configuration = "";
        path = "";
        ops = "";
        inputbox.setValue("");
        configbox.setValue("{\n\t\"type\":\"tree\",\n\t\"live\":true,\n\t\"root\":{\"path\":\"Tutor Tree\"},\n\t\"paths\":\n\t{\n\t\t\"Tutor Tree\":\n\t\t[\n\t\t\t{\"type\":\"const\", \"value\":\"INSERT PATH HERE\"}\n\t\t]\n\t}\n}");
        $("#path").val("");
        $("#ops").val("");
        $("#resultsTable").empty();
        $("#databox").val("");
        $("#links").empty();
        if(gotData == true)
        {
            $("#tree").data("fancytree").destroy();
        }
        $.ajax(
        {
            url: "/tree/reset",
            type: "GET",
            data:
            {
                uid: $.cookie(COOK)
            },
            success: function(data)
            {
                $("#messagebox").val(data);
                gotData = false;
            },
            error: function(data)
            {
                $("#messagebox").val(data.responseText);
            }
        });
    });

    //Toggles between the library and query divs.
    $("#library-button").click(function(event)
    {
        $("#querydiv").hide();
        $("#librarydiv").show();
    });
//--------------------------------------------------------------------------------------------------------------------//
});