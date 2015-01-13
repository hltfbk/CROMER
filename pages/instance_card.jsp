<%@ page import="java.util.Hashtable" %>
<%@ page import="servlet.WebController" %>
<%@ include file="setvar.jsp" %>

<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>CROMER - Instances</title>
<link rel="stylesheet" href="css/jquery-ui.css" id="theme">
<script type="text/javascript" src="js/jquery.js"></script>
<script type="text/javascript" src="js/jquery-ui.min.js"></script>
<script type="text/javascript" src="js/alertify/alertify.min.js"></script>
<link rel="stylesheet" href="js/alertify/alertify.core.css" />
<link rel="stylesheet" href="js/alertify/alertify.default.css" />

<script>
function pressesckey(e) {
    if (e.keyCode == 27) {
        hideSemrolePane();
    }
}

function detectkeys(e) {
    //skip ESC, shift, control, option, command, and arrows button
    if (e.keyCode == 27 || e.keyCode == 16 || e.keyCode == 17 || e.keyCode == 18 || e.keyCode == 91 || e.keyCode == 37 || e.keyCode == 38 || e.keyCode == 39 || e.keyCode == 40) {
        //do nothing
    } else {
        disableSave(false);
    }
}

$('body').keyup(function( event ) {
    pressesckey(event);
});

$('input').keyup(function( event ) {
    if(this.id != "filter") {
        detectkeys(event);
    }
});
$('textarea').keyup(function( event ) {
    detectkeys(event);
});


function saveInstance(id) {
    var action = $('#action').val();
    var type = $('#type').val();
    var classname = $('#'+type+'class').val();

    //alert(action + ": [" + id+"] "+ type + " " +classname +" name: "+ $('#name').val() +" -- haspart: "+$('#haspart').val() );
    $.ajax({
        type: "POST",
        url: "update",
        data: {data: "instance", action: action, id: id, classname: classname, type: type, name: $('#name').val(),
            descr: $('#descr').val(), link: $('#link').val(), comment: $('#comment').val(), time: $('#time').val(),
            beginterval: $('#beginterval').val(), endinterval: $('#endinterval').val(), haspart: $('#haspart').val()},
        async: true,
        cache:false,
        success: function(html) {
            INSTANCE_ISCHANGED=0;
            $('#instanceform').html(html);
            $.get('instance_list.jsp', {type: type, class: classname, filter: $('#filter').val(), mode: $('#last option:selected').val()}, function(result) {
                $("#"+type+"_"+classname+"_accordion").html(result);
                var count_item = result.match(/<br>/g);
                $("#"+type+"_"+classname+"_count").html("("+count_item.length+")");
            });
        }
    });
    return false;
}

function removeInstance(id,type,classname,filter) {
    alertify.set({ buttonFocus: "cancel" });
    alertify.confirm("ATTENTION! Are you sure you want to REMOVE this instance?", function (e) {
        if (e) {
            INSTANCE_ISCHANGED=0;
            $.ajax({
                type: "GET",
                url: "update",
                data: {id: id, data: "instance", action: "remove"},
                //data: "id="+id+"&data=instance&action=remove", // appears as $_GET['id'] @ ur backend side
                async: true,
                cache:false,
                success: function(html) {
                    // data is ur summary
                    $('#instanceform').html(html);
                    $.get('instance_list.jsp', {type: type, class: classname, filter: filter}, function(result) {
                        $("#"+type+"_"+classname+"_accordion").html(result);
                        var count_item = result.match(/<br>/g);
                        $("#"+type+"_"+classname+"_count").html("("+count_item.length+")");
                    });
                }
            });

            $.get('settings.jsp', {data: "instance", action: "add", id: id}, function(result) {
                $("#settings").html(result);
            });
        }
    });
}

function removeAnnotationOnly(id, filter) {
    alertify.set({ buttonFocus: "cancel" });
    alertify.confirm("WARNING! This instance cannot be removed because another user used it in own annotations. Do you want to REMOVE your annotations only?", function (e) {
        if (e) {
            $.ajax({
                type: "GET",
                url: "update",
                data: {id: id, data: "instance", action: "removeannonly"}, // appears as $_GET['id'] @ ur backend side
                success: function(html) {
                    // data is ur summary
                    //$('#instanceform').html(html);
                    //getInstanceList(type,filter);
                    window.open("instances.jsp", "_self");
                }
            });
        }
    });
}


function showDiv(type) {
    //if INSTANCE_TYPE is an EVENT the menu 'Create a new has-participant' can appear
    INSTANCE_TYPE=type;
    $("#commonfields").show();
    if (type.length > 0) {
        <%
        for (String key : markables.keySet()) {
            out.println("$(\"#"+key+"fields\").hide();");
        }
        %>
        var fieldname = "#"+type+"fields";
        $(fieldname).show();

        if (type == "EVENT" || type == "event" ) {
            $("#extra_timefields").show();
        } else {
            $("#extra_timefields").hide();
        }

    }
}

function checkForm(id, action) {
    var type;
    var classtype;
    if (id == "null") {
        type = document.getElementById('type');
        classtype = document.getElementById(type.value +'class');
    }
    var name = document.getElementById('name').value;
    var hiddenaction = document.getElementById("action");
    if (hiddenaction != null) {
        hiddenaction.value = action;
        //alert(id + " " + type.value + " " + name.value + " -- " + document.getElementById("action"));

        if (id == "null" && type.value.length > 0 && classtype.value.length > 0 && name.length > 0) {  //&& link.length > 0
            $('#new').submit();
        } else if (id != "null" && name.length > 0)  {
            $('#new').submit();
        } else {
            alertify.log('Check the mandatory fields "type", "class", and "name" of instance! Fill in them and save again.',"error");
        }
    } else {
        alertify.error('An error occurred in the form. Please refresh (clear) the cache from your browser and try it again.',"error");
    }
}

function disableSave (flag) {
    if (!flag) {
        INSTANCE_ISCHANGED=1;
    }
    $("#save").attr("disabled", flag);
    $("#savecart").attr("disabled", flag);
}

function checkRef (el) {
    var link = el.value;
    if (link.length > 0) {
        if (link.indexOf("http") != 0) {
            link = "http://dbpedia.org/resource/" + link;
        }
        urlExists(link, el);
    }
    disableSave(false);
}

function checkTime (el) {
    var time = el.value.trim();
    $("#"+el.name).html("");
    if (time.length > 0) {
        var time_regex = /^....-..-..$/ ;
        if(time_regex.test(time)) {
            $("#"+el.name+"log").html("<img src='css/images/confirm.gif' title='The format is valid!'>");
        } else {
            //alert("The time " + time + " is not valid. Fill in a new one using the format '....-..-..'.'");
            $("#"+el.name+"log").html("<img src='css/images/warning.gif' title='Sorry, this time is not valid!'>");
        }
    } else {
        $("#"+el.name+"log").html("");
    }
    disableSave(false);
}

function urlExists(url, el) {
    $("#reflog").html("checking <img width=22 src='css/images/blue_loader_dots.gif'>");

    $.ajax({
        url: 'update',
        type: 'GET',
        dataType: "html",
        data: {data: "instance", action: "checkref", name: url},
        async: true,
        cache:false,
        crossDomain: true,
        success: function(result) {
            if (result == "ok") {
                var linkimg = "link.gif";
                if (url.indexOf("dbpedia.org") > 0) {
                    linkimg ="dbpedia.png";
                } else if (url.indexOf("wikipedia") > 0) {
                    linkimg ="wikipedia.png";
                }
                $("#reflog").html("<a href='"+url+"' target=linkref><img src='css/images/"+linkimg+"' title='open the page'></a>");
                el.value = url;
            } else if (result == "failed") {
                $("#reflog").html("<img src='css/images/warning.gif' title='Sorry, the connection is lost!'>");
            } else {
                $("#reflog").html("<img src='css/images/database_error.png' title='Sorry, this url does not exist!'>");
            }
        },
        error: function( xhr,err ) {
            //alert(err+"\nreadyState: "+xhr.readyState+"\nstatus: "+xhr.status+"\nresponseText: "+xhr.responseText);
            //alert(ENTITYID);
            switch(xhr.status) {
                case 404:
                    $("#reflog").html('<font color=red>Could not contact server.</font>');
                    break;
                case 500:
                    $("#reflog").html('<font color=red>A server-side error has occurred.</font>');
                    break;
            }
        }
    });
}

function createHasParticipant(id) {
    $.get('add_linkto.jsp', {targetid: id}, function(result) {
        $("#linkto").html(result);
    });
    disableSave(false);
}

function delHasPart(id) {
    alertify.confirm("ATTENTION! Are you sure you want to REMOVE this has participant relation?", function (e) {
        if (e) {
            $.get('add_linkto.jsp', {targetid: id, action: "remove"}, function(result) {
                $("#linkto").html(result);
            });
            disableSave(false);
        }
    });
}

function setRole (itemEl, targetID) {
    var semroleval = "";
    if (itemEl != null) {
        semroleval = itemEl.innerHTML;
    }
    el = document.getElementById("semrolepane");
    if (el != null) {
        el.style.visibility='visible';
        form = document.getElementById("semroleform");
        if (form != null) {
            form.targetID.value=targetID;
            var sEL = form.semrole; //document.getElementById('semrole');
            for (var i = 0; i < sEL.options.length; i++) {
                if (sEL.options[i].text == semroleval) {
                    sEL.selectedIndex = i;
                    sEL.options[i].selected = true;
                    break;
                }
            }
        }
    }
    return false;
}

function updateSemrole () {
    var semroleEl = document.getElementById("semrole");
    var semrole ="NONE";
    if (semroleEl != null) {
        semrole = semroleEl.options[semroleEl.selectedIndex].text;
    }
    var targetID = document.getElementById("targetID");
    if (targetID != null) {
        /*$.ajax({
         url: 'update',
         type: 'GET',
         data: {data: "annotation", action: action, name: "relation",
         sourceID: sourceID.value, targetID: targetID.value, relattribute: semrole,
         reltype: "HAS_PARTICIPANT", group: "root"},
         async: false,
         cache:false,
         crossDomain: true,
         success: function(response) {

         if (response == "error") {
         $("#log").html("");
         //alertify.alert("Another user is updating this instance in the same file. Try again later!");
         alert("ERROR! This relation has not been stored correctly. Try again later or contact the administrator.");

         } else {
         window.open("annotatenews.jsp?index="+index+"&docid="+ docid +"&instance="+ENTITYID,"_self");

         //ENTITYID=entityID;
         }

         }
         });
         */
        // alert(sourceID.value + " " +semrole+ " " + targetID.value);
        $.ajax({
            url: 'add_linkto.jsp',
            type: 'GET',
            data: {action: "update", targetid: targetID.value, semrole: semrole},
            async: false,
            cache:false,
            crossDomain: true,
            success: function(response) {
                if (response.length > 1)
                    disableSave(false);
                $("#linkto").html(response);
            }
        });

        hideSemrolePane();
    } else {
        alert("ERROR! Your annotation has not been stored correctly. Try again later or contact the administrator.");
    }
    return false;
}

function hideSemrolePane () {
    el = document.getElementById("semrolepane");
    if (el != null) {
        el.style.visibility='hidden';
    }
    return false;
}

function gotoInstanceCard(id) {
    window.open("instances.jsp?id="+id, "cromer");
}
</script>
</head>

<body>
<div id=instanceform>
<%
    String id = request.getParameter("id");
    if (id != null && id.trim().length() == 0) {
        id = null;
    }
    HttpSession currentsession = request.getSession();
    currentsession.removeAttribute("haspart");

    String action = request.getParameter("action");
    String fail = request.getParameter("fail");
    if (fail != null) {
        if (fail.equals("0") || fail.equals("false")) {
            if(action != null) {
                if (action.equals("remove")) {
                    out.println("<script>alertify.log('The instance has been removed successfully.', 'success');</script>");
                    id=null;
                } else if (action.equals("removeannonly")) {
                    out.println("<script>alertify.log('Your annotations for the instance "+id+" has been removed successfully.', 'success');</script>");
                } else if (action.equals("cart")) {
                    out.println("<script>$.get('settings.jsp', {data: \"instance\", action: \"add\", id: \""+id+"\"}, function(result) {\n" +
                            "           $(\"#settings\").html(result);\n" +
                            "              alertify.log('The instance has been added to the cart.', 'success');\n" +
                            "});</script>");
                } else {
                    out.println("<script>alertify.log('The instance has been saved successfully.', 'success');</script>");
                }
                action="edit";
                id=null;

            }
        } else if (fail.equals("1")) {
            out.println("<script>removeAnnotationOnly('"+id+"','');</script>");
        } else {
            out.println("<script>alertify.log('The updating database failed!');</script>");
        }
        // id = null;
    }


    Hashtable<String, String> instanceValues = new Hashtable<String, String>();
    String disabled = "";
    String type = "", classname="";
    if (id != null) {
        instanceValues = WebController.getInstance(id);
        if (instanceValues == null) {
            out.println("<img src='css/images/warning.gif'> ERROR! No information found about the instance " + id);
            return;
        } else {
            type = instanceValues.get("type");
            classname = instanceValues.get("class");

            if (!instanceValues.containsKey("time")) {
                instanceValues.put("time", "");
            }
            if (!instanceValues.containsKey("beginterval")) {
                instanceValues.put("beginterval", "");
            }
            if (!instanceValues.containsKey("endinterval")) {
                instanceValues.put("endinterval", "");
            }
            if (!instanceValues.containsKey("lastmodified")) {
                instanceValues.put("lastmodified", "");
            }
            disabled = "disabled";
        }
    } else {
        instanceValues.put("type", "");
        instanceValues.put("class", "");
        instanceValues.put("name", "");
        instanceValues.put("descr", "");
        instanceValues.put("link", "");
        instanceValues.put("comment", "");
        instanceValues.put("time", "");
        instanceValues.put("beginterval", "");
        instanceValues.put("endinterval", "");
        instanceValues.put("lastmodified", "");
    }


    if (action != null) {
        if (action.equals("edit")) {


%>
<form onsubmit="return saveInstance('<% if (id != null) out.print(id); %>');" id="new" method="POST">
    <input type=hidden name="action" id="action" value="cart" />
    <%
        if (id == null) {
            out.println("Create a new instance of <select name=\"type\" id=\"type\" onchange=\"javascript:showDiv(this.value);\" "+disabled +"><option value=''");
            if (type.equals("")) {
                out.print(" selected='selected'");
            }
            out.println("></option>");
            for (String key : markables.keySet()) {
                out.println("<option value='"+key+"'");
                if (type.equals(key)) {
                    out.print(" selected='selected'");
                }
                out.println(">"+key+"</option>");
            }
            out.println("<select>");
        } else {
    %>

    <input type=hidden name="type" id="type" value="<%= type %>" />
    <input type=hidden name="<%= type %>class" id="<%= type %>class" value="<%= classname %>" />
    <table class=table><tr><td>Instance ID: </td><td><b><%= id %></b></td></tr>
        <tr><td align=right>Type: </td><td><b><%= type %></b></td></tr>
        <tr><td align=right>Class: </td><td><b><%= classname %></b></td></tr>
    </table>
    <%
        }

        out.println("<div id=\"commonfields\" style=\"display:none; margin-top: 10px\">");
        if (id == null) {
            for (String key : markables.keySet()) {
                out.println("<div id='"+key+"fields' style='display:none;'>");
                out.println("Choose a class: <select name='"+key+"class' id='"+key+"class' "+ disabled+">");
                out.print("<option value=''");
                if (instanceValues.get("class").equals("")) {
                    out.print(" selected='selected'");
                }
                out.println("></option>");
                String[] items = markables.get(key);
                for (String item : items) {
                    out.print("<option value='"+item.toUpperCase()+"'");
                    if (item.equals(instanceValues.get("class"))) {
                        out.print(" selected='selected'");

                    }
                    out.println(">"+item+"</option>\n");
                }
                out.println("</select>\n</div>\n");
            }
        }
    %>
    <br>
    Insert a human-friendly name: <br><input type=text name=name id=name value="<%= instanceValues.get("name") %>" size=43>
    <br>
    <span style="color: gray; ">a short description:</span><br><input type=text name=descr id=descr value="<%= instanceValues.get("descr") %>" size=48>
    <br><br>
    <span style="color: gray; ">Insert external reference (DBPedia URL, ...):</span> <br>
    <div style="display: inline-block">
        <input type=text name=link id=link size=48 value="<%= instanceValues.get("link") %>" onclick="javascript:disableSave(true);" onblur="javascript:checkRef(this);">
    </div>
    <div style="display: inline; position: relative" id="reflog"></div>

    <div id="extra_timefields" style="display: table">
        <br>
        <table style="font-size: 14px; font-family: Arial" cellpadding=0 cellspacing=0>
            <tr><td><span style="color: gray">Time:</span></td>
                <td>
                    <input type=text name=time id=time size=20 value="<%= instanceValues.get("time") %>" onclick="javascript:disableSave(true);" onblur="javascript:checkTime(this);">
                    <div style="display: inline; position: relative" id="timelog"></div> <span style="color: gray">(....-..-..)</span>
                </td></tr>


            <tr><td><span style="color: gray">Begin interval:</span></td>
                <td>
                    <input type=text name=beginterval id=beginterval size=20 value="<%= instanceValues.get("beginterval") %>" onclick="javascript:disableSave(true);" onblur="javascript:checkTime(this);">
                    <div style="display: inline; position: relative" id="begintervallog"></div> <span style="color: gray">(....-..-..)</span>
                </td></tr>

            <tr><td><span style="color: gray;">End interval:</span></td>
                <td>
                    <input type=text name=endinterval id=endinterval size=20 value="<%= instanceValues.get("endinterval") %>" onclick="javascript:disableSave(true);" onblur="javascript:checkTime(this);">
                    <div style="display: inline; position: relative" id="endintervallog"></div> <span style="color: gray">(....-..-..)</span>
                </td></tr>
        </table>
    </div>

    <br>
    <span style="color: gray; ">Comments:</span><br>
    <textarea name=comment id=comment rows=3 cols="50"><%= instanceValues.get("comment") %></textarea>
    <br>

    <%
        String haspart = instanceValues.get("haspart");
        if (haspart == null)
            haspart ="";
        currentsession.setAttribute("haspart", haspart);
    %>

    <div id="linkto">
        <jsp:include page="add_linkto.jsp?action=edit"></jsp:include>
    </div>
    <br>


    <div style="float: left">
        <input type=button value="Delete card" onclick="javascript:removeInstance('<%= id %>','<%= type %>','<%= classname %>','');" />
    </div>


    <div style="float: right; margin-bottom: 10px">
        <input type=button value="Cancel" onclick="javascript:editInstance('');" />
    </div>
    <div style="float: right; margin-bottom: 10px"><input type=button id=save value="Save" onclick="checkForm('<%= id %>','add');" disabled></div>

    <%
        String instances = (String) currentsession.getAttribute("selectedinstance");
        if (instances == null)
            instances = "";
        if (id==null || !instances.contains(id)) {
    %>
    <div style="float: right; margin-bottom: 10px"><input type=button id=savecart value="Save and add to cart" onclick="checkForm('<%= id %>','cart');" disabled></div>
    <%}%>
    <br>
    <%
        if (instanceValues.get("lastmodified").length() > 0) {
            out.println("<br><span style=\"color: gray\">Last modified: " +instanceValues.get("lastmodified") + "</span>");
        }
    %>
</form>

<div id=semrolepane class=semrolepane>
    <div class=semroleform>
        <form onsubmit="return updateSemrole();" id=semroleform>
            <input type=hidden name=targetID id=targetID value="" />

            Choose the semantic role: <select id=semrole name="semrole">
            <%
                String[] smValues = {"NONE","Arg0","Arg1","Arg2","Arg3","Arg4","Argm-LOC","Argm-OTHER"};
                for (String sm : smValues) {
                    out.println("<option value='"+sm+"'>"+sm);
                }
            %>
        </select>
            <br>
            <p style='float: right'>
                <button onclick="return updateSemrole();">Save</button>
                <button onclick="return hideSemrolePane();">Cancel</button>
            </p>
        </form>
    </div>
</div>


<%
} else if (action.equals("show")) {

    String haspart = instanceValues.get("haspart");
    if (haspart == null)
        haspart ="";
    currentsession.setAttribute("haspart", haspart);
%>
<table style="font-size: 14px; font-family: Arial; margin:5px" border=0>
    <tr><td align=right colspan=2>
        <button onclick="javascript:gotoInstanceCard('<%= id %>');">edit <img width=13 src="css/images/edit-icon.png"></button>
        <button onclick="javascript:hideSemrolePane();">close <img width=13 src="css/images/close.png"></button><hr></td></tr>
    <tr><td align=right>Instance ID: </td><td><b><%= id %></b></td></tr>
    <tr><td align=right>Type: </td><td><b><%= type %></b></td></tr>
    <tr><td align=right>Class: </td><td><b><%= classname %></b></td></tr>
    <tr><td align=right>Human-friendly name: </td><td><b><%= instanceValues.get("name") %></b></td></tr>
    <tr><td align=right>Description: </td><td><b><%= instanceValues.get("descr") %></b></td></tr>
    <tr><td align=right>External reference: </td><td><b><%= instanceValues.get("link") %></b></td></tr>

    <tr><td align=right>Time:</td><td><b><%= instanceValues.get("time") %></b></td></tr>
    <tr><td align=right>Begin interval:</td><td><b><%= instanceValues.get("beginterval") %></b></td></tr>
    <tr><td align=right>End interval:</td><td><b><%= instanceValues.get("endinterval") %></b></td></tr>

    <tr><td align=right>Comment: </td><td><b><%= instanceValues.get("comment") %></b></td></tr>
    <tr><td colspan=2>
        <hr><jsp:include page="add_linkto.jsp"></jsp:include>
    </td></tr>
</table>

<%
        }
    }
    if (id != null) {
        out.println("<script>showDiv('"+type+"')</script>");
    }
%>
</div>
</body>
</html>