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
<html>
<head>
    <link rel="stylesheet" href="css/jquery-ui.css" id="theme">
    <link rel="stylesheet" href="css/cromer.css">
</head>
<body>
<div class=header id="divheader">
    <jsp:include page="header.jsp" flush="true">
        <jsp:param name="id" value="" />
    </jsp:include>
</div>
<%
    HttpSession currentsession = request.getSession();
    String user = (String) currentsession.getAttribute("user");
    if (user!=null) {

%>
<div class=settings>
    <jsp:include page="settings.jsp" flush="true">
        <jsp:param name="action" value="null" />
        <jsp:param name="data" value="repository" />
    </jsp:include>
</div>
<script type="text/javascript">
    window.name = 'cromer';

    function openNew(url, target) {
        newWindow = window.open(url, target,"toolbar=no,menubar=0,status=0,copyhistory=0,scrollbars=yes,resizable=1,location=0,Width=1500,Height=760,modal=yes	,alwaysRaised=yes,fullscreen=yes'") ;
        newWindow.location = url;
        newWindow.focus();
    }
</script>


<div class=search>

    <%

        String query = request.getParameter("query");
        String filter = request.getParameter("filter");
        String pag = request.getParameter("pag");
        if (pag == null) {
            pag = "1";
        }
        int docspage = 10;
        String[] repodirs = WebController.getIndexNames();
        if  (repodirs == null || repodirs.length == 0) {
            out.println("<font color=red>ATTENTION!</font> Create a repository and then make a query on its documents.");
        } else {
    %>

    <form action="index.jsp">
        Search <input type="text" size=32 name="query" id="query" value="<% if (query != null) out.print(query); %>" />
        <!--
        &nbsp;&nbsp; Order by <select name="order" onChange="javascript:alert('change ordering');">
                    <option  value="0">Relevance
                    <option selected value="1">Date
                </select>
        <div style='right: 10px; float:right; display: inline'>
            Skip the documents already annotated <input type="checkbox" name="filter" value="alreadyannotated"
                <%
                    if (filter != null) {
                        //out.print("checked");
                    }
                %>/></div>
                -->

    </form>
</div>

<div class=resultdocs>
    <%
                String repos = (String) currentsession.getAttribute("selectedrepository");
//check if the session values are aligned with the dirs on the server
                if (repos == null) {
                    out.println("<font color=red>WARNING!</font> Select one or more repositories from the list and then make a query.");
                } else {
                    String[] repolist = repos.split(",");
                    repos = "";
                    for (String repo : repolist) {
                        for (String repodir : repodirs) {
                            if (repodir.equalsIgnoreCase(repo)) {
                                repos += ","+repodir;
                                continue;
                            }
                        }
                    }

                    repos = repos.replaceFirst("^,","");


//show the retrieved docs
                    if  (repos.length() == 0) {
                        out.println("<font color=red>WARNING!</font> Select one or more repositories from the list and then make a query.");
                    } else {
                        //get the instance's docs
                        String id = request.getParameter("id");
                        if ((id != null && id.length() > 0) || (query!=null && query.startsWith("instanceid:"))) {
                            if (id != null && id.length() > 0)
                                query = "instanceid:" +id;
                            String[] idDocs = WebController.getIndexSearch(repos.split(","), query, user);
                            if (idDocs.length > 1) {
                                out.println("<div style='position: relative; z-index: 20'>Found <b>"+(idDocs.length/2)+"</b> documents</div>");
                                String docurl;
                                for (int i=0; i< idDocs.length; i=i+2) {
                                    docurl = WebController.getDocUrl(idDocs[i], idDocs[i+1]);
                                    if (docurl == null) {
                                        docurl = idDocs[i+1];
                                    }
                                    out.println("<div class=doctitle><a href='annotatenews.jsp?index="+idDocs[i]+"&docid="+idDocs[i+1]+"&query=' target='annotatenews'><img src='css/images/dimage.gif' onclick='return openHelp(this);'> "+docurl+"</a> <div style='float: right; font-size: 11px'>"+idDocs[i]+"</div></div>");
                                    String[] lines = WebController.getInstanceDocSentences(idDocs[i], idDocs[i+1], id, user);
                                    for (String line : lines) {
                                        out.println("- " + line + "<br>");
                                    }
                                }
                            } else {
                                out.println("No data found for the instance: <b>"+id+"</b>");
                            }
                        } else {
                            if (query == null) {
                                query = (String) currentsession.getAttribute("selectedquery");
                            }
                            if (query != null && query.length() > 0) {
                                String[] idDocs = WebController.getIndexSearch(repos.split(","), query, user);

                                if (idDocs.length > 0) {
                                    out.println("<div style='position: relative; z-index: 20'>Found <b>"+idDocs.length+"</b> documents</div>");
                                    //PAGING
                                    /*
                                    out.println("<center>");
                                    int pages = idDocs.length / docspage;
                                    for (int i=1; i<=pages; i++) {
                                        if (Integer.valueOf(pag) == i) {
                                            out.print(i + " ");
                                        } else {
                                            out.print("<a href='index.jsp?query="+query+"&pag="+i+"'>"+i+"</a> ");
                                        }
                                    }
                                    //out.println("QUERY: " +query + " - " +repos);
                                    out.println("</center>");
                                    */
                                    String[] docinfo;
                                    String docurl;
                                    for (String docid_index : idDocs) {
                                        docinfo = docid_index.split(",");
                                        docurl = WebController.getDocUrl(docinfo[0], docinfo[1]);
                                        if (docurl == null) {
                                            docurl = docinfo[1];
                                        }
                                        out.println("<div class=doctitle><a href='javascript:openNew(\"annotatenews.jsp?index="+docinfo[0]+"&docid="+docinfo[1]+"&query="+query+"\",\"annotatenews\")'><img src='css/images/dimage.gif'> "+docurl+"</a> <div style='float: right; font-size: 11px'>"+docinfo[0]+"</div></div>");
                                        String[] lines = WebController.getDocSentences(docinfo[0], docinfo[1], query);
                                        for (String line : lines) {
                                            out.println(line+"<br>");
                                        }
                                    }
                                    session.setAttribute("selectedquery", query);
                                    out.println("<script>$('#query').val('"+query+"');</script>");
                                } else {
                                    out.println("No data found for query: <b>"+query+"</b>");
                                }

                            }
                        }
                    }
                }
            }
        }
    %>
</div>
</body>
</html>