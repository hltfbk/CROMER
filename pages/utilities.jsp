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

<%@ page import="servlet.WebController" %>
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ include file="header.jsp" %>
<%@ include file="errors.lbl" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>CROMER - Repositories</title>
    <link rel="stylesheet" href="css/cromer.css" id="theme">
    <link rel="stylesheet" href="css/jquery-ui.css">
    <script type="text/javascript" src="js/alertify/alertify.min.js"></script>
    <link rel="stylesheet" href="js/alertify/alertify.core.css" />
    <link rel="stylesheet" href="js/alertify/alertify.default.css" />
    <script>
        function metaexport (meta, mode) {
            format = "";
            el = document.getElementById("exportformat");
            if (el != null) {
                format = el.options[el.selectedIndex].value;
            }
            window.open("view.jsp?export="+meta+"&mode="+mode+"&format="+format+"&action=export", "export");
            //window.open("utilities.jsp?export="+meta+"&mode="+mode+"&format="+format, "_self");
        }
    </script>
</head>
<body>

<%
    if (user == null) {
        out.println("<script>window.location = 'header.jsp';</script>");
    } else {
        boolean checkDB = WebController.checkDBConnection();
        String format = request.getParameter("format");

%>


<div class=search>
    <form action="update?data=instance&action=upload" method="POST" enctype="multipart/form-data">

        <div style="display:inline; float: left"><li> Import instances: <input type="file" name="file" multiple>
            <input type=submit value="Submit"><br>
        </div>
        <br>
        <br>
        <hr>
        <br>
        <div style="display:inline; float: right">
            Choose the export format <select name=format id=exportformat><option value='csv'>CSV<option value='xml'>XML
        </select></div><br>

        <li> <input type=button onclick="javascript:metaexport('instance','byuser');" value="Export"> instances<br> <!--<input type=button onclick="javascript:metaexport('instance');" value="All">-->
            <br><br>
        <li> <input type=button onclick="javascript:metaexport('document','byuser');" value="Export"> the annotated documents<br>
                <%
           if (user.equals("admin")) {
            out.println("<input type=button onclick=\"javascript:metaexport('document');\" value=\"All\">");
           }
        %>

                <%
            /*
            String[] repodirs = WebController.getIndexNames();
            if  (repodirs.length == 0) {
        	    out.println("NONE");
            } else {
                out.println("<select name=repo><option value=''>NONE");

                out.println("</select>");
            }
             */

        %>
    </form>

</div>

<div style="display: inline; position: absolute; float:right;">
    <%
            String errorlines = request.getParameter("errorlines");
            if (errorlines != null && errorlines.length() > 0) {
                out.println("WARNING! Some instances have not been saved (check the lines "+errorlines+")<br>");
            }
            String added = request.getParameter("added");
            if (added != null) {
                out.println("<b>Added "+added + " instances!</b>");
            }

            Hashtable<String, String> instances = WebController.listInstances(null);
            out.println("<br><br><b>Saved instances</b>: " +instances.size());

            out.println("<br><br><b>Average annotation time</b>: " +WebController.getAverageAnnotationTime(user) + " sec. (to connect a mention, or a chain, to an instance)<br><br>");

            if (user.equals("admin")) {
                //user tables
                out.println( "<b>Users</b> "+users.size()+" ");
                for (String key : users.keySet()) {
                    out.println("<li>"+key + " ("+users.get(key)+")");
                }
                out.println("<br><br>");

                List<String> linkedInstances = WebController.getLinkedInstances(null);
                // String type, id;
                Hashtable<String, String> similarEntities  = new Hashtable<String, String>();
                int simCount = 0;
                for (String key : instances.keySet()) {
                    //type = instances.get(key).replaceFirst("/[^/]+$","");
                    //id = instances.get(key).replaceAll(".+/","");
                    String simKey = key.replaceFirst("/[^/]+$","")+"/"+instances.get(key).replaceFirst("^\\d+ ","");
                    if (similarEntities.containsKey(simKey)) {
                        simCount++;
                        out.println(simCount +" Check these similar instance:<br>=> " +key.replaceFirst("/[^/]+$","") +"/"+similarEntities.get(simKey).replaceFirst(" 0 "," ") + "<br><= " +key + " "+instances.get(key).replaceFirst("^\\d+ ","") + "<br>");
                        out.println("UPDATE annotation SET entityid='"+similarEntities.get(simKey).replaceFirst(" 0 .+","") +"' WHERE entityid='"+key.replaceAll(".+/","")+"';<br>");
                        out.println("<a href=\"update?data=instance&action=remove&id="+ key.replaceAll(".+/","")+ "\">DELETE!</a><br>");
                    } else {
                        similarEntities.put(simKey,key.replaceAll(".+/","") + " " +instances.get(key));
                    }
                    linkedInstances.remove(key.replaceAll(".+/",""));
                    //out.println("#" + instances.get(key) +" <b>" + key.replaceFirst("^\\d+ ","")+"</b><br>");
                }

                // controllo quali istanze hanno lo stesso nome (indipendentemente dalla categoria)
                similarEntities.clear();
                for (String key : instances.keySet()) {
                    //key => ECBp-LOC/LOC_GEO/LOC17666661445836424
                    //instances.get(key) => 0 t35b_pittsburgh

                    String simKey = instances.get(key).replaceFirst("^\\d+ ","");
                    if (similarEntities.containsKey(simKey)) {
                        similarEntities.put(simKey,similarEntities.get(simKey) + " " +key);
                    } else {
                        similarEntities.put(simKey,key);
                    }

                }

                simCount=0;
                //mostro quali istanze hanno lo stesso nome (indipendentemente dalla categoria)
                for (String key : similarEntities.keySet()) {
                    if(similarEntities.get(key).contains(" ")) {
                        String[] ids = similarEntities.get(key).split(" ");
                        simCount++;
                        out.println("> " +simCount +". Check these instances with the same naming: "+key+"<br>");
                        for (String path : ids) {
                            out.println(path +"<br>");
                        }
                    }
                }

                //controllo che non siano state cancellate delle schede instance e siano rimaste delle annotazioni con tali istanze
                if (linkedInstances.size() > 0) {
                    out.println("<br><br>Checked errors: ANNOTATED MENTION WITHOUT INSTANCE INFORMATION<br>");
                    for (String entid : linkedInstances) {
                        out.println("! DELETE "+ entid+"<br>");
                    }
                }


            }

            if (checkDB) {
                //statistiche
                if (!user.equals("admin")) {
                    LinkedHashMap<String, Hashtable<String, String>> hash = WebController.getStats(user);
                    if (hash.size() > 0) {
                        out.println("<table border=1 cellspacing=0 cellpadding=2><tr><td colspan=5 align=center><i>Annotation statistics</i></td></tr><tr bgcolor=#ccc><td>DAY</td><td>DOCs</td><td>INSTANCEs</td><td>MENTIONs</td></tr>");
                        for (String key : hash.keySet()) {
                            Hashtable h = hash.get(key);
                            out.println("<tr><td>"+key+"</td><td align=right>"+h.get("doc")+"</td><td align=right>"+h.get("ins")+"</td><td align=right>"+h.get("mnt")+"</td></tr>");
                        }
                        out.println("</table>");
                    }
                }
            } else {
                out.println(MYSQL_CONNECTION_FAILED);
            }
        }
    %>
</div>
</body>
</html>