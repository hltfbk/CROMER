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
<%@ page import="java.util.Hashtable" %>
<%@ page import="java.util.Arrays" %>
<%@ include file="errors.lbl" %>
<%@ include file="setvar.jsp" %>

<link rel="stylesheet" href="css/jquery-ui-1.11.0.css">
<script src="js/jquery-1.10.2.js"></script>
<script src="js/jquery-ui-1.11.0.js"></script>


<%
    String filter = request.getParameter("filter");
    if (filter == null) {
        filter="";
    }
    String mode = request.getParameter("mode");
    if (mode == null) {
        mode="";
    }
    HttpSession currentsession = request.getSession();
    session.setAttribute("filter", filter);
    session.setAttribute("filtermode", mode);

    String instance_type = request.getParameter("type");
    String instance_class = request.getParameter("class");

    boolean checkDB = WebController.checkDBConnection();
    Hashtable<String, String> instances;
    if (!mode.equals("")) {
        if (mode.equals("cart")) {
            instances = new Hashtable<String, String>();
            String cartinstances = (String) currentsession.getAttribute("selectedinstance");
            if (cartinstances == null)
                cartinstances = "";
            String[] items = cartinstances.split(",");
            Arrays.sort(items);
            Hashtable<String, String> instanceValues = null;
            for (String item : items) {
                instanceValues = WebController.getInstance(item);
                if (instanceValues != null) {
                    String descr = instanceValues.get("descr");
                    if (descr == null) {
                        descr = "";
                    } else {
                        if (descr.length() > 0)
                            descr = " - " + descr;
                    }
                    instances.put(instanceValues.get("type") +"/"+instanceValues.get("class") +"/"+instanceValues.get("name").replaceAll("[/|\\s]","_")+"/"+instanceValues.get("id"), instanceValues.get("numdocs")+ " " +instanceValues.get("name") + descr);
                }
            }
        } else {
            instances = WebController.listLastInstances(mode);
        }
    } else {
        instances = WebController.listInstances(filter);
    }

    if (instances.size() > 0) {
        String user = (String) currentsession.getAttribute("user");
        String selectedinstance = (String) currentsession.getAttribute("selectedinstance");

        Hashtable<String, Integer> instances_count = new Hashtable<String, Integer>();
        Hashtable<String, String> instances_group = new Hashtable<String, String>();

        String[] keys = instances.keySet().toArray(new String[0]);
        Arrays.sort(keys, String.CASE_INSENSITIVE_ORDER);
        //Arrays.sort(keys, new AlphanumComparator());

        String itype, item, id, selected;
        int numdocs;
        for (String key : keys) {
            itype = key.replaceFirst("/[^/]+/[^/]+$","");
            id = key.replaceAll(".+/","");
            //out.println (instances.size()+ " " +itype+ " -- "+ key+"<br>");
            //if (key.startsWith(instance_type) || key.toLowerCase().startsWith(instance_type.toLowerCase())) {
            selected = "";
            if (selectedinstance != null && selectedinstance.contains(id))
                selected = " checked";
            //item = "<input type='checkbox' onclick='javascript:updatecart(\""+id+"\",\""+itype+"/"+instances.get(key).replaceFirst("^\\d* ","").replaceAll("['|\"]","&#39;")+"\");'"+selected+"> [";
            item = "<input type='checkbox' onclick='javascript:updatecart(\""+id+"\");'"+selected+"> [";

            numdocs = 0;
            if (checkDB) {
                numdocs = WebController.getNumAnnotatedDocs(id, user);
                if (numdocs < 0) {
                    checkDB = false;
                }
            }
            if (numdocs==0) {
                item += "0 <small>docs</small>";
            } else {
                item += "<a href='index.jsp?id=" +id+ "'>" +numdocs+ " <small>docs</small></a>";
            }
            item += "] <a href=\"javascript:editInstance('"+id+"','"+itype+"');\" onmouseover=\"javascript:onInstance('"+id+"','"+itype+"');\"><b>" + instances.get(key).replaceFirst("^\\d+ ","") + "</b></a><br>\n";
            if (instances_group.containsKey(itype)) {
                instances_group.put(itype, instances_group.get(itype) +item);
                instances_count.put(itype, instances_count.get(itype) +1);
            } else {
                instances_group.put(itype, item);
                instances_count.put(itype, 1);
            }
            //}
        }

        if (instance_type != null) {
            if (instances_group.containsKey(instance_type+"/"+instance_class))
                out.println(instances_group.get(instance_type+"/"+instance_class));
            else
                out.println("");
        } else {

%>
<script>
    $(function() {
        $("#entitylist" ).accordion({
            animate: false,
            active: false,
            collapsible: true,
            expandable: true,
            heightStyle: "content",
            beforeActivate: function(event, ui) {
                // The accordion believes a panel is being opened
                if (ui.newHeader[0]) {
                    var currHeader  = ui.newHeader;
                    var currContent = currHeader.next('.ui-accordion-content');
                    // The accordion believes a panel is being closed
                } else {
                    var currHeader  = ui.oldHeader;
                    var currContent = currHeader.next('.ui-accordion-content');
                }
                // Since we've changed the default behavior, this detects the actual status
                var isPanelSelected = currHeader.attr('aria-selected') == 'true';

                // Toggle the panel's header
                currHeader.toggleClass('ui-corner-all',isPanelSelected).toggleClass('accordion-header-active ui-state-active ui-corner-top',!isPanelSelected).attr('aria-selected',((!isPanelSelected).toString()));

                // Toggle the panel's icon
                currHeader.children('.ui-icon').toggleClass('ui-icon-triangle-1-e',isPanelSelected).toggleClass('ui-icon-triangle-1-s',!isPanelSelected);

                // Toggle the panel's content
                currContent.toggleClass('accordion-content-active',!isPanelSelected)
                if (isPanelSelected) { currContent.slideUp(); }  else { currContent.slideDown(); }

                return false; // Cancel the default action
            }
        });
    });
</script>

<button style="right: 0px; float: right; display: block" class="accordion-expand-all" href="#">Expand all</button>
<div id="entitylist" style="visibility: hidden; width: 400px; padding: 5px; background: #ddd">

    <%
                //out.println("filter: " + filter + ", mode: "+mode + ", instances_group: " + instances_group.size() + " "+ "instances_count: " + instances_count.size());
                StringBuilder htmlaccordion = new StringBuilder();
                for (String type : markables.keySet()) {
                    htmlaccordion.setLength(0);
                    int counter, accounter =0;
                    int i=-1;

                    for (String cl : markables.get(type)) {
                        i++;
                        if (instances_count.containsKey(type+"/"+cl)) {
                            counter = instances_count.get(type+"/"+cl);
                        } else {
                            counter=0;
                        }
                        //out.println(type+"/"+cl + " counter: "+ counter+"<br>");
                        if (filter.equals("") && mode.equals("") || (filter.length() > 0 && counter > 0) || (mode.length() > 0 && counter > 0)) {
                            accounter += counter;
                            //out.println(key+"accordion<br>");

                            htmlaccordion.append("<h3 style='margin-bottom: 0px'>").append(cl).append(" <font color=#000 size='2px' id='"+type+"_"+cl+"_count'>(").append(counter).append(")</font></h3>\n");
                            htmlaccordion.append("<div style='padding: 2px' class='ui-accordion-content ' id='"+type+"_"+cl+"_accordion'>");
                            if (instances_group.containsKey(type+"/"+cl)) {
                                htmlaccordion.append(instances_group.get(type+"/"+cl));
                            }
                            htmlaccordion.append("</div>\n");
                        }
                    }
                    if (htmlaccordion.length() > 0) {
                        //out.println("<div style='display: block; width: 400px; margin-top: 5px; float: left; margin-right: 10px; padding:0px' id='"+key+"accordion'>"+key+": ("+accounter+")"+htmlaccordion.toString() + "</div><br>");
                        //out.println(key+": ("+accounter+")"+htmlaccordion.toString() + "<br>");
                        out.println("&#9656; "+type+": ("+accounter+")" + htmlaccordion.toString());
                    }
                }
            }
        } else {
            out.println("No instance found!");
        }
    %>
</div>

<%
    if (instance_type == null) {
%>
<script>
    var contentAreas = $('#entitylist .ui-accordion-content');
    var expandLink = $('.accordion-expand-all');
    var allopen=0;

    // hook up the expand/collapse all
    expandLink.click(function(){
        //var isAllOpen = $('#entitylist').data('isAllOpen');
        if (allopen == 0) {
            contentAreas['show']().trigger('show');
            expandLink.text('Collapse All');
            allopen=1;
            // contentAreas[isAllOpen? 'hide': 'show']()
            //     .trigger(isAllOpen? 'hide': 'show');
        } else {
            contentAreas['hide']().trigger('hide');
            expandLink.text('Expand All');
            allopen=0;
        }
    });


    var acc=document.getElementById('entitylist');
    if (acc != null) {
        setTimeout(function(){acc.style.visibility='visible'},300);
    }
</script>
<%
    }
%>