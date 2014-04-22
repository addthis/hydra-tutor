window.htapp = {};

htapp.url = "/validate/post";
htapp.reset = "/validate/reset";

htapp.stash = [];
htapp.stash_query = function(temp_query_obj) {
	htapp.stash.push(temp_query_obj);
};

htapp.current = "";

htapp.path = function() {
	var pathArray = window.location.pathname.split( '/' );
	return pathArray.pop();
};

$(document).ready( function() {

	htapp.stashchange = $.Callbacks();
	htapp.source   = $("#library-template").html();
	htapp.template = Handlebars.compile(htapp.source);

	htapp.library_rerender = function(data){
		var context = htapp.stash;
		var data = { items: htapp.stash };
		$('#librarybox').html(htapp.template(data));
	}


	var populate_textareas = function(singleresultarray) {
        var inputbox   = $('#inputbox').val(singleresultarray[0].input);
        var filterbox  = $('#filterbox').val(singleresultarray[0].filterbox)

        if (singleresultarray[0].filtertype == "auto") {
            var filtertype = $('input:radio[name=filtertype]')[0].checked = true;
        } else if (singleresultarray[0].filtertype == "bundle") {
            var filtertype = $('input:radio[name=filtertype]')[1].checked = true;
        } else {
            var filtertype = $('input:radio[name=filtertype]')[2].checked = true;
        }
    };

//-------------------------------------------------//
	///////////////
	//* COOKIES *//
	///////////////
	COOK = "uid"; //value for cookie

	var get_new_cookie = function(){
		return $.md5(String(Math.random()));
	}

	var delete_user_cookie = function(){
		$.cookie(COOK, null, { path: '/'});
	}

    var stash_state = function(input, filter, type) {
	    type = type || "auto";
	    temp_query_obj = {
		    uid : $.cookie(COOK),
		    input : input,
		    filterbox : filter,
		    filtertype : type,
		    date : String(new Date()),
	      	href :"#",
		    id : get_new_cookie()
	    };
	    htapp.stash_query(temp_query_obj);
    }

	// check for cookie
	if (!($.cookie(COOK))){ // if cookie doesn't exist for user, give them one
		usercookie = get_new_cookie();
		$.cookie(COOK, get_new_cookie(), { path: '/', expires: 9999 });
	}

//-------------------------------------------------//
	///////////////
	// AJAX CALL //
	///////////////
	var send_form = function(){

		var inputbox = $('#inputbox').val();
		var filterbox = $('#filterbox').val();
		var filtertype = $('input:radio[name=filtertype]:checked').val();

	  window.response = $.ajax({
		  type: "POST",
		  url: htapp.url,
		  data: {"input":inputbox,"filter":filterbox, "filtertype":filtertype, "uid":$.cookie(COOK)},
		  dataType:"json",
		  error:function(data){
		  	response.output = data.output;
		  	response.messages = data.messages;
		    $('#run-button').attr("disabled", false);
		  },
		  success:function(data){
		  	response.output = data.output;
           	response.messages = data.messages;
	        $('#run-button').attr("disabled", false);
		  },
		  always:function(data){
		  	response.output = data.output;
	        response.messages = data.messages;
	        $('#run-button').attr("disabled", false);
		  }
		});

		$.when(response).then(function(){
			$('#outputbox').val(response.output);
			$('#messagebox').val(response.messages);
		});

	  // add the ajax response to the htapp namespaced callback queue
	}

	// when server returns value it's here
	$('#run-button').on('click', function(e){
	    $('#run-button').attr("disabled", true);
	    $('#outputbox').val("");
		$('#messagebox').val("");
		send_form();
	})

	$('#reset-button').on('click', function(e){
		
		$.ajax({
		  type: "POST",
		  url: htapp.reset,
		  data: {"uid":$.cookie(COOK)},
		  dataType:"json",
		  error:function(data){
		  },
		  success:function(data){
		  },
		  always:function(data){
		  }
		});

		$('#outputbox').val('');
		$('#messagebox').val('');
	    $('#run-button').attr("disabled", false);
	});

//-------------------------------------------------//
	///////////////
	//   SAVE    //
	///////////////

	$('#examples-button').on('click', function(e){
		e.preventDefault();
		stash_state("\"harry potter\"\n\"harry\"\n\"potter\"" , "{op : \"require\", value : [\"gandalf\", \"merlin\", \"harry potter\"]}")
		stash_state("\"harry potter\"\n\"merlin\"\n\"gandalf\"" , "{op : \"case\", upper : true}")
		stash_state("\"foo\"" , "{op : \"count\", format : \"0000000\"}")
		stash_state("{hello:\"foo\", world:\"bar\"}" , "{op : \"debug\"}")
		htapp.library_rerender();
		htapp.current=temp_query_obj.id;
	});

	$('#stash-button').on('click', function(e){
		e.preventDefault();
		stash_state($('#inputbox').val(), $('#filterbox').val(), $('input:radio[name=filtertype]:checked').val());
		htapp.library_rerender();
		htapp.current=temp_query_obj.id;
	});

	$('#libraryheader').on('click', '.delete-lib-all', function(e){
		e.preventDefault();
		htapp.stash = [];
		htapp.library_rerender();
	});

	$('#librarybox').on('click', '.delete-lib-item', function(e){
		e.preventDefault();
		//$(this).parent().remove();
		var myid = $(this).parent().data('id');

		var results = jQuery.grep(htapp.stash, function(element, index){
  		return element.id!==myid; // retain appropriate elements
		});

		htapp.stash = results;
		htapp.library_rerender();

		// lookup local storage and delete
	});

	$('#librarybox').on('click', '.library-link', function(e){
		e.preventDefault();
		//$(this).parent().remove();
		var myid = $(this).parent().data('id');
		var result = jQuery.grep(htapp.stash, function(element, index){
  		return element.id===myid; // retain appropriate elements
		});
		htapp.current=myid;
		populate_textareas(result);
		$('#outputbox').val('');
		$('#messagebox').val('');
		// lookup local storage and delete
	});

	$('#store-button').on('click', function(e){
			if (!(htapp.current)){
				temp_query_obj = {
					uid : $.cookie(COOK),
					input : $('#inputbox').val(),
					filterbox : $('#filterbox').val(),
					filtertype : $('input:radio[name=filtertype]:checked').val(),
					date : String(new Date()),
					href : "#",
					id : get_new_cookie()
				};
				htapp.stash_query(temp_query_obj);
				htapp.library_rerender();
				htapp.current=temp_query_obj.id;
			}
			href=htapp.current;
			history.pushState('', 'New URL: '+href, href);




	});

	var has_push_state = function(){
	  pathArray = window.location.pathname.split( '/' );
	  laststr = pathArray.pop();
	  if(laststr==="index.html"){ return false; }
	  if(laststr===""){ return false; }
	  return true;
	};

	var fetch_data_from_server = function(objid){
		fetch_object = {
			"id":objid
		};
		escval = escape(JSON.stringify(fetch_object));
	};

	if(has_push_state()){ //if it has push state
		pathArray = window.location.pathname.split( '/' );
	    laststr = pathArray.pop();
		fetch_data_from_server(laststr);
	}

});