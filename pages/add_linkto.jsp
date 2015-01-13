<%
    /*
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
    */

    String id = request.getParameter("targetid");
    String action = request.getParameter("action");

%>

<%@ page contentType="text/raw; charset=UTF8" pageEncoding="UTF8"%>
<%@ page import="servlet.WebController"%>
<%@ page import="java.util.Hashtable" %>

<%

    HttpSession currentsession = request.getSession();
    String haspart = (String) currentsession.getAttribute("haspart");
    if (haspart == null)
        haspart = "";
    String semrole = request.getParameter("semrole");
    if (id != null) {
        if (action != null) {
            if (action.equals("remove")) {
                haspart = haspart.replaceFirst("\\s*"+id+"_*[^\\s]*\\s*"," ").trim();
            } else if (action.equals("update")) {
                haspart = haspart.replaceFirst("\\s*"+id+"_*[^\\s]*\\s*"," ");
                haspart = (haspart.trim() +" "+ id+"_"+semrole).trim();
            }
            currentsession.setAttribute("haspart", haspart);
        }
    }
    out.println("<input type=\"hidden\" name=\"haspart\" id=\"haspart\" value=\""+haspart+"\">\n");
    if (haspart.trim().length() > 0) {
        out.print("Has participants: <br><b>");
        String[] entities = haspart.trim().split(" ");
        for (String entid : entities) {
            String[] items =  entid.split("_");
            if (items.length > 1) {
                semrole = items[1];
                entid = items[0];
            } else {
                semrole = "NONE";
            }
            Hashtable<String,String> instanceInfo = WebController.getInstance(entid);
            if (instanceInfo != null) {
                if (action.equals("edit")) {
                    out.print("<a href=\"javascript:delHasPart('"+entid+"');\"><img src='css/images/trash.png' width=12px title='Remove this has participant relation'></a>");
                } else {
                    out.println("<li>");
                }
                out.println("&nbsp;<font color=#c77405>"+instanceInfo.get("type")+"/"+instanceInfo.get("class") +"</font>: "+instanceInfo.get("name"));
                if (instanceInfo.get("descr").length() > 0) {
                    out.print(" - " +instanceInfo.get("descr"));
                }

                if (action.equals("edit")) {
                    out.print(" <button onclick=\"return setRole(this,'"+entid+"');\">"+semrole+"</button><br>");
                }  else {
                    out.print(" ["+semrole+"]<br>");
                }

            }
        }
        out.print("</b>");

    }
%>
