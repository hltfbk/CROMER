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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>CROMER - Repositories</title>
    <script type="text/javascript" src="js/alertify/alertify.min.js"></script>
    <link rel="stylesheet" href="js/alertify/alertify.core.css" />
    <link rel="stylesheet" href="js/alertify/alertify.default.css" />

    <link rel="stylesheet" href="css/cromer.css">
    <link rel="stylesheet" href="css/jquery-ui-1.11.0.css">
    <script src="js/jquery-1.10.2.js"></script>
    <script src="js/jquery-ui-1.11.0.js"></script>

</head>
<body>
<%
    String user = (String) request.getSession().getAttribute("user");
    //String user = (String) session.getAttribute("user");
    if (user == null) {
        out.println("<script>window.location = 'header.jsp';</script>");
    } else {
%>
<div class=header id="divheader">
    <jsp:include page="header.jsp" flush="true">
        <jsp:param name="page" value="<%= request.getRequestURL().toString() %>" />
    </jsp:include>
</div>

<script>
    function disablediv(el) {
        var selectedgroup = document.getElementById('groupselect');
        if (el.value.length != 0) {
            selectedgroup.disabled=true;
        } else {
            selectedgroup.disabled=false;
        }
    }

    function create() {
        var reponame = document.getElementById('name').value;
        if (reponame.length == 0) {
            alert("Please, insert a not empty repository name!");
            return;
        }

        var groupname = document.getElementById('newgroup').value;

        if (groupname.length == 0) {
            groupname = document.getElementById('groupselect').value;
        }
        if (groupname.length == 0) {
            alert("Please, insert a valid group name or choose an existing one!");
            return;
        }
        window.open("update?data=repository&action=add&name="+reponame+"&group="+groupname, "_self");
    }

    function remove(group, name, numdocs) {
        if (numdocs == 0) {
            alertify.set({ buttonFocus: "cancel" });
            alertify.confirm("ATTENTION! Are you sure you want to REMOVE this repository?<br>(all documents and annotations will disappear)", function (e) {
                if (e) {
                    window.open("update?data=repository&action=remove&name="+name+"&group="+group, "_self");
                }
            });
        } else {
            alertify.alert("ATTENTION! The repository <b>" + name + "</b> contains some documents.<br>Just an empty repository can be deleted.");
        }
    }
</script>

<%
    String fail = request.getParameter("fail");
    if (fail != null) {
        //out.println("FAIL: " + fail + " " + request.getParameter("name"));
        if (fail.equals("0")) {
            if (request.getParameter("action").equals("add")) {
                out.println("<script>alertify.log('The repository <b>" +request.getParameter("group")+ "/" +request.getParameter("name")+ "</b> has been added successfully.', 'success');</script>");
            } else if (request.getParameter("action").equals("remove")) {
                out.println("<script>alertify.log('The repository <b>" +request.getParameter("group")+ "/" +request.getParameter("name")+ "</b> has been removed.', 'success');</script>");
            }
        } else {
            out.println("<script>alertify.log('ERROR! The update is failed.','error');</script>");
        }
    }

%>

<div class=search>
    <form id="newrepo" action="javascript:create()" method="POST">
        Create a new repository named: <input type="text" size=22 name="name" id="name" value=""/>
        <br><br>
        <div id="group">
            add it to a new group named: <input onkeyup="javascript:disablediv(this);" type="text" size=22 name="newgroup" id="newgroup" value=""/>
        </div>
        <%
            String[] repodirs = WebController.getIndexNames();
            String groupname = "";
            if (repodirs.length > 0) {
                out.println("or <div id='groups'>join it to an existing group: <select id='groupselect'><option value=''></option>");
                for (int i=0; i<repodirs.length; i++) {
                    String item = repodirs[i].replaceFirst("[/|\\\\].+","");
                    if (!item.equals(groupname)) {
                        groupname = item;
                        out.println("<option value='"+ groupname + "'>"+groupname+"</option>");
                    }
                }
                out.println("</select></div><br>");
            }
        %>
        <br>
        <div style="float: right"><input type=submit value="Submit"></div>
        &nbsp;&nbsp;
    </form>
</div>

<div style="display: inline; position:absolute; top: 30px; padding: 10px; right: 0px; float:right; z-index:99; background: #fff">
    <div class=settings id=settings><img src='css/images/repository.png'>
    <% if  (repodirs.length == 0) {
        out.println("No repositories is available! &nbsp;</div>");
    } else {
        out.println("Repositories found: <b>" +repodirs.length +"</b></div>");
        out.println("<div id=\"repolist\" style=\"width: 400px; padding: 5px;\">\n");


        String[] repodocs= WebController.getIndexDocs();
        groupname = "";
        if (repodirs.length > 0) {
            for (int i=0; i<repodirs.length; i++) {
                String item = repodirs[i].replaceFirst("[/|\\\\].+","");
                if (!item.equals(groupname)) {
                    if (!groupname.equals("")) {
                        out.println("</div>");
                    }
                    groupname = item;
                    out.println("<h3 style='margin-bottom: 3px'>"+groupname+"</h3>\n<div style='padding: 2px'>");
                }
                String reponame = repodirs[i].replaceFirst(".+[/|\\\\]","");

                out.println("<a href=\"javascript:remove('"+ groupname + "','"+ reponame +"',"+repodocs[i]+");\" title=\"delete repository\"><img width=14 src='css/images/trash.png'></a>&nbsp;<a href='documents.jsp?index="+groupname+"/"+reponame+"'>"+ reponame +"</a><span style='float: right'>("+repodocs[i]+" docs)</span><br>\n");
            }
        }
        if (!groupname.equals("")) {
            out.println("</div>");
        }
        out.println("</div>");
    }
    }
    %>

</div>
<script>
    $(function() {
        $("#repolist" ).accordion({
            animate: false,
            active: false,
            collapsible: true,
            heightStyle: "content"
        });
    });
</script>

</body>
</html>
