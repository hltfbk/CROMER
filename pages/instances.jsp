<!--
CROMER - a Tool for CROss-document Main Event and Entity Recognition
developted by Christian Girardi, cgirardi@fbk.eu (FBK, HLT group)


Copyright 2008 the original author or authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

<%@ page import="servlet.WebController"%>
<%@ include file="errors.lbl" %>
<%@ include file="setvar.jsp" %>

<%
    HttpSession currentsession = request.getSession();
    String filter = request.getParameter("filter");
    if (filter == null) {
        filter = (String) currentsession.getAttribute("filter");
        if (filter == null) {
            filter="";
        }
    }

    String mode = (String) currentsession.getAttribute("filtermode");
    if (mode == null) {
        mode="";
    }

    String user = (String) currentsession.getAttribute("user");
    if (user == null) {
        out.println("<script>window.location = 'header.jsp';</script>");
    } else {

%>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>CROMER - Instances</title>
    <link rel="stylesheet" href="css/cromer.css">
    <style>
        body {
            overflow-y: scroll !important;
        }
    </style>


    <script>
        var INSTANCE_ISCHANGED = 0;
        var INSTANCE_TYPE = 0;
        var ONINSTANCE_ID = 0;
        var ONINSTANCE_TYPE = 0;

        function onInstance (id, type) {
            if (ONINSTANCE_ID != id) {
                el = document.getElementById("entityFloating");
                if (el != null)
                    el.style.visibility="hidden";
            }
            if (type.substr(0,5) != "EVENT") {
                ONINSTANCE_ID=id;
                ONINSTANCE_TYPE = type;
            }
        }

        function noRelation () {
            ONINSTANCE_ID=null;
            ONINSTANCE_TYPE = null;
        }

        function loadInstances(filter) {
            $('#accordion').html("<div style='width: 400px; padding: 5px;'>instances loading <img width=22 src='css/images/blue_loader_dots.gif'></div>");
            selectlast = document.getElementById("last");
            if (selectlast != null) {
                selectlast.selectedIndex = 0;
            }

            resetfilter = document.getElementById("resetfilter");
            if (filter === undefined || filter=="") {
                filter="";
                if (resetfilter) resetfilter.style.visibility="hidden";
                el = document.getElementById("filter");
                if (el) el.value="";
            } else {
                if (resetfilter) resetfilter.style.visibility="visible";
            }

            $.ajax({
                type: "GET",
                url: "instance_list.jsp",
                data: "filter=" + filter, // appears as $_GET['id'] @ ur backend side
                success: function(html) {
                    // data is ur summary
                    $('#accordion').html(html);
                },
                error: function(XMLHttpRequest, textStatus, errorThrown) {
                    $('#accordion').html("ERROR! Connection to the server failed.");
                }
            });
        }

        function latestInstances(mode) {
            $('#accordion').html("<div style='width: 400px; padding: 5px;'>instances loading <img width=22 src='css/images/blue_loader_dots.gif'></div>");

            resetfilter = document.getElementById("resetfilter");
            if (resetfilter) resetfilter.style.visibility="visible";

            filter="";
            el = document.getElementById("filter");
            if (el) el.value="";

            el = document.getElementById("last");
            if (el != null) {
                for (var i = 0; i < el.options.length; i++) {
                    if (el.options[i].value == mode) {
                        el.selectedIndex = i;
                        el.options[i].selected = true;
                        break;
                    }
                }
            }

            $.ajax({
                type: "GET",
                url: "instance_list.jsp",
                data: "mode="+mode, // appears as $_GET['id'] @ ur backend side
                success: function(html) {
                    // data is ur summary
                    $('#accordion').html(html);
                },
                error: function(XMLHttpRequest, textStatus, errorThrown) {
                    $('#accordion').html("ERROR! Connection to the server failed.");
                }
            });
        }

        function filterInstance (mode) {
            if (mode !== undefined && mode != "") {
                latestInstances(mode);
            } else {
                el = document.getElementById("filter");
                if (el != null) {
                    loadInstances(el.value);
                }
            }
            return false;
        }

        function editInstance (id, type) {
            if (INSTANCE_ISCHANGED==1) {
                alertify.set({ buttonFocus: "cancel" });
                alertify.confirm("All changes to the template will be discarded. Do you want to continue?", function (e) {
                    if (e) {
                        INSTANCE_ISCHANGED=0;
                        $.ajax({
                            type: "GET",
                            url: "instance_card.jsp",
                            data: "id="+id+"&action=edit", // appears as $_GET['id'] @ ur backend side
                            success: function(html) {
                                // data is ur summary
                                $('#instancecard').html(html);
                            }
                        });
                    }
                });

            } else {
                if (type) {
                    INSTANCE_TYPE = type;
                }

                $.ajax({
                    type: "GET",
                    url: "instance_card.jsp",
                    data: "id="+id+"&action=edit", // appears as $_GET['id'] @ ur backend side
                    success: function(html) {
                        // data is ur summary
                        $('#instancecard').html(html);
                    }
                });
            }

        }

        function updatecart (id) {
            //update cart
            //alert("session data: instance, value: " +id+"_"+key);
            ////$.get('session.jsp', {data: "instance", action: "add", id: id, value: key}, function(result) {
            //show the current cart
            ////$.get('settings.jsp', {data: "instance"}, function(result) {
            $.get('settings.jsp', {data: "instance", action: "add", id: id}, function(result) {
                $("#settings").html(result);
            });
            ////});
        }

        function sessionSettings (id, value){
            $.get('session.jsp', {data: id, action: "set", id: "", value: value});
        }

        function getInstanceList (type, filter) {
            //$("#"+type+"accordion").accordion('destroy');
            //$("#"+type+"accordion").accordion('refresh');
            //$("#"+type+"accordion").accordion();

            alert("getInstanceList! ("+type+ ") filter:"+filter);
            $.get('instance_list.jsp', {type: type, filter: filter}, function(result) {
                $("#"+type+"accordion").html(result);
            });
        }

    </script>
</head>
<body>


<div class=header id="divheader">
    <jsp:include page="header.jsp" flush="true">
        <jsp:param name="page" value="<%= request.getRequestURL().toString() %>" />
    </jsp:include>
</div>
<div id=settings>
    <jsp:include page="settings.jsp" flush="true">
        <jsp:param name="data" value="instance" />
    </jsp:include>
</div>

<%

    boolean checkDB = WebController.checkDBConnection();
    int numInstances = WebController.getNumInstances();

//out.println("<script>alert('numInstances ' +"+numInstances+");</script>");

    if (!checkDB) {
        out.println(MYSQL_CONNECTION_FAILED);
    }
    if (numInstances > 0) {

%>

<div style="margin-top: 70px; float: right; display: inline; position: relative; ">
    <form onsubmit="return filterInstance();">
        <div style="display:inline-block; float: right; position: fixed; background: #fff; border-bottom: 1px solid #ddd; z-index:3; right: 0; top:42px; padding-top:15px; padding-left:0px; padding-bottom: 5px; padding-right:30px">
            Filter <input type="text" size=25 id=filter value="<%= filter %>" />
            or get <select id=last onchange="javascript:filterInstance(this.options[this.selectedIndex].value);"><option value=""></option>
            <option value="cart">instances in the cart</option>
            <option value="20">latest 20 instances</option>
            <option value="100">latest 100 instances</option>
            <option value="hour">last hour instances</option>
            <option value="day">last day instances</option>
        </select>

            <div id=resetfilter style='visibility: hidden; position: relative; display:inline'><a href="javascript:loadInstances();"><img src='css/images/cancel.png'></a></div>
        </div>
    </form>


    <div id=accordion></div>
    <script>
        if ("<%= mode %>" != "") {
            filterInstance("<%= mode %>");
        } else {
            loadInstances("<%= filter %>")
        }
    </script>
    <%
        } else {
            out.println("No instance found!");
        }
    %>
</div>

<br>
<div id=instancecard class=search></div>

<%
        String id = request.getParameter("id");
        if (id == null) {
            id = "";
        }
        out.println("<script>editInstance('"+ id  + "');</script>");
    }
%>

<script type="text/javascript" charset="utf-8">
    $('div').bind("contextmenu", function(e) {
        if (INSTANCE_TYPE.substr(0,5) == "EVENT" && ONINSTANCE_ID!= null) {
            $("#entityFloating").css({ top: (e.pageY-2) + "px", left: (e.pageX-20) + "px" }).show(100);
            el.style.visibility="visible";
            e.preventDefault();
        }
    });

    $('div').mousedown(function(e) {
        el = document.getElementById("entityFloating");
        if (el != null) {
            el.style.visibility="hidden";
            $('#entityFloating').html("&nbsp;<a style=\"text-decoration: none\" onclick=\"javascript:setRole(null,'"+ONINSTANCE_ID+"');\">Create an 'has participant' relation</a>&nbsp;");
        }
    });

    $('div').mouseout(function(e) {
        noRelation();
    });

</script>
<div id="entityFloating" class=entityFloating onclick="this.style.visibility='hidden';" onmouseover="javascript:noRelation();"></div>


</body>
</html>


