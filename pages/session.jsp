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

<%@ page contentType="text/html;charset=UTF-8" language="java" session="true" %>
<%

String action = request.getParameter("action");
String data = request.getParameter("data");
String id = request.getParameter("id");
if (id == null)
	id = "";
String value = request.getParameter("value");
HttpSession currentsession = request.getSession();

String selected_before = (String) currentsession.getAttribute("selected" + data);
if (selected_before == null)
	selected_before = "";
//java.text.DateFormat df = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss"); 
//out.println("Current Date: "+df.format(new java.util.Date()) +" ("+ request.getParameter("value") +")<br>");
if (action != null) {
	if (action.equals("set")) {
		if (!data.equals("repository") && selected_before.equals((id + " " +value).trim())) {
            currentsession.setAttribute("selected" + data, "");
		} else {
            currentsession.setAttribute("selected" + data, (id + " " +value).trim());
       	}
    } else {
       	if (selected_before.contains(id + " ")) {
            currentsession.setAttribute("selected" + data, selected_before.replaceFirst(",*"+id+" [^,]+,*",",").replaceFirst(",$","").replaceFirst("^,",""));
       	} else {	
            currentsession.setAttribute("selected" + data, (selected_before +","+ id + " " +value).replaceFirst("^,",""));
       	}
    }
}

String selected = (String) currentsession.getAttribute("selected" + data);
//TODO check if all corpora are available
if (selected == null || selected.equals("")) {
	out.println("No "+data+" selected!");
} /*else {
	if (!selected.equals(""))                       
        out.println("You are using: <b>"+ selected.replaceAll(",","<br>") +"</b>");
        //out.println("<table cellspacing=0 cellpadding=0><td>You are using: <b>" +selected.replaceAll(","," ") + "</b></td></table>");
}   */
//out.println("<br>selected BEFORE=" + selected_before + "<br>param value="+value+"<br>selected CURRENT=" + selected +"<br>");
%>
