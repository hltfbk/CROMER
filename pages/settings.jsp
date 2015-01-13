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
<%@ page import="servlet.WebController"%>
<%@ page import="java.io.File"%>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.Hashtable" %>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>CROMER - Selection widget</title>
    <link rel="stylesheet" type="text/css" href="js/jquery-ui-multiselect-widget/css/jquery.multiselect.css" />
    <link rel="stylesheet" type="text/css" href="js/jquery-ui-multiselect-widget/css/jquery.multiselect.filter.css" />
    <link rel="stylesheet" type="text/css" href="js/jquery-ui-multiselect-widget/assets/style.css" />
    <link rel="stylesheet" type="text/css" href="js/jquery-ui-multiselect-widget/assets/prettify.css" />
    <link rel="stylesheet" type="text/css" href="css/jquery-ui.css" />
    <script type="text/javascript" src="js/jquery.js"></script>
    <script type="text/javascript" src="js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="js/jquery-ui-multiselect-widget/jquery.multiselect.js"></script>
    <script type="text/javascript" src="js/jquery-ui-multiselect-widget/jquery.multiselect.filter.js"></script>
    <script type="text/javascript" src="js/jquery-ui-multiselect-widget/assets/prettify.js"></script>

</head>
<body>
    <%

    String data = request.getParameter("data");
    if (data == null)
        data = "instance";
    HttpSession currentsession = request.getSession();

    if (data.contains("repository")) {

    } else if (data.contains("instance")) {
        String optionInstance = "";
        String selectText = "The instance cart is empty";
        //TODO creare una procedura che salvi un Hashtable<String, String>
        // nel caso delle Istanze metto
        //come chiave doc.get("type") +"/"+doc.get("class")+"/"+doc.get("name").replaceAll("[/|\\s]","_")
        //e valore l'instance id

        String instances = (String) currentsession.getAttribute("selected"+data);
        if (instances == null)
            instances = "";
        String action = request.getParameter("action");
        Hashtable<String, String> instanceValues = null;
        String id = request.getParameter("id");
        if (id == null) {
            id = "";
        } else {
            instanceValues = WebController.getInstance(id);
        }
        //java.text.DateFormat df = new java.text.SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        //out.println("Current Date: "+df.format(new java.util.Date()) +" ("+ request.getParameter("value") +")<br>");
        if (action != null && instanceValues != null) {
            if (action.equals("set")) {
                if (instances.equals(id + ",")) {
                    instances = "";
                } else {
                    instances = id+",";
                }
            } else {
                if (instances.contains(id + ",")) {
                    instances = instances.replaceFirst(",*"+id+",",",");
                } else {
                    instances += id + ",";
                }
            }
            instances=instances.replaceFirst("^,","");
            //out.println("{"+instances+"}");

            currentsession.setAttribute("selected" + data, instances);
        }

        if (instances.length() > 0) {
            String[] items = instances.split(",");
            Arrays.sort(items);
            String ctype="";
            for (String item : items) {
                instanceValues = WebController.getInstance(item);
                if (instanceValues != null) {
                    if (!ctype.equals(instanceValues.get("type")+"/"+instanceValues.get("class"))) {
                       if (ctype.length() != 0) {
                          optionInstance += "</optgroup>";
                       }
                       ctype = instanceValues.get("type")+"/"+instanceValues.get("class");
                       optionInstance += "<optgroup label='"+ctype +"'>";
                    }

                    optionInstance += "<option value='" + item + "/"+instanceValues.get("type")+ "/"+instanceValues.get("class")+"'>"+
                    //"<input type='checkbox' onclick='javascript:updatecart(\""+item+"\");' checked>" +
                    instanceValues.get("name");
                    if (instanceValues.get("descr").length() > 0) {
                         optionInstance += " - " +instanceValues.get("descr");
                    }
                    optionInstance += "</option>\n";

                } else {
                    currentsession.setAttribute("selected" + data, instances.replaceAll(",*"+item+",",",").replaceFirst("^,",""));
                }
            }
            if (ctype.length() != 0) {
                optionInstance += "</optgroup>";
            }
            if (items.length > 1) {
                selectText = items.length + " instances in the cart";
            } else {
                selectText = items.length + " instance in the cart";
            }
        }
%>

<div style='float: right;'><a href="javascript:refreshcart()" title="refresh cart"><img src='css/images/refresh_icon.png'></a>
    <img width=14 src='css/images/cart-icon.gif'>
    <select multiple="multiple" id="instance" style="margin-top: 10px; width:380px">
        <%= optionInstance %>
    </select>
</div>
<br>
<script>
    function refreshcart () {
        //update cart
        //alert("session data: instance, value: " +id+"_"+key);
        //$.get('session.jsp', {data: "instance", action: "set", id: "", value: ""}, function(result) {
            //reload the current page
            window.open("instances.jsp", "_self");
        //});
    }

    //$("#instance").multiselect().multiselectfilter();
    //$("#instance").multiselect("checkAll");
    $("#instance").multiselect({
        //multiple: true,
        multiple: false,
        header: false,
        selectedList: 0,
        noneSelectedText: "<%= selectText %>",
        click: function(event, ui) {
            if ($(this).multiselect("getChecked").length == 0) {
            }
            //var info = ui.value.split("/");
            //    alert(info[0]);

            //$.get('instance_list.jsp', {type: type, class: classname, filter: filter}, function(result) {
            //    $("#"+type+"_"+classname+"_accordion").html(result);
            //});
            //$.get('session.jsp', {data: "instance", action: "remove", value: ui.value}, function(result) {
            //$usedcorpora.html(result);
            //});
            //updatecart(ui.value)


            //$usedcorpora.text(ui.value + ' ' + (ui.checked ? 'checked' : 'unchecked') );
            //alert(ui.value + ' ' + (ui.checked ? 'checked' : 'unchecked') );
        },
        uncheckAll: function(event, ui) {
            //$.get('session.jsp', {data: "instance", action: "set", value: ""}, function(result) {
            //$usedcorpora.html(result);
            //});

            $(this).multiselect("widget").find(":checkbox").each(function(){
                this.checked = false;
            });

            //$(this).multiselect({noneSelectedText: "Select instances"});
            //$(this).multiselect("close");
        },
        selectedText: function(numChecked, numTotal, checkedItems) {
            /*var values = $.map(checkedItems, function(checkbox){
             return checkbox.value;
             }).join(",");
             $.get('session.jsp', {data: "instance", action: "add", value: values}, function(result) {
             $usedcorpora.html(result);
             });

             if (numChecked > 1) {
             return numChecked + ' instances in the cart';
             } else {
             return numChecked + ' instance in the cart';
             }
             */
        }

    });
</script>

    <%
    }
    if (data==null || data.contains("repository")) {
        String[] repodirs = WebController.getIndexNames();
        if  (repodirs.length == 0) {
            out.println("<br>No repositories is available!");
        } else {

            String repos = (String) currentsession.getAttribute("selectedrepository");
            if (repos == null) {
                repos = "";
            } else {
                String[] selectedrepos = repos.split(",");
                repos = "";
                for (String irepo : selectedrepos) {
                    for (String rep : repodirs) {
                        if (rep.equals(irepo)) {
                            repos += irepo+",";
                            break;
                        }
                    }
                }
                //if (!repos.equals("")) {
                //repos = "You are using: <b>" + repos + "</b>";
                //}
            }

            String optionText = "";
            String optgroup = "";
            if (repodirs.length > 0) {
                for (int i=0; i<repodirs.length; i++) {
                    String[] item = repodirs[i].split(File.separator);
                    if (!optgroup.equals(item[0])) {
                        if (optgroup.length() != 0) {
                            optionText += "</optgroup>\n";
                        }
                        optgroup = item[0];
                        optionText += "<optgroup label='"+optgroup+"'>\n";
                    }
                    optionText += "<option value='" + repodirs[i] + "'";

                    if (repos.contains(repodirs[i]+",")) {
                        optionText += " selected=selected";
                    }
                    optionText += ">"+item[1]+"</option>\n";
                }
                optionText += "</optgroup>\n";
            }

%>


<div style='float: right;'><img src='css/images/repository.png'> <select multiple="multiple" id="corpora" style="width:370px">
    <%= optionText %>
</select>
    <div style="direction: rtl" id="usedcorpora"></div>  <!--<%= repos %> -->
</div>

<script type="text/javascript">

    var $usedcorpora = $("#usedcorpora");

    $("#corpora").multiselect().multiselectfilter();
    $("#corpora").multiselect({
        noneSelectedText: "Select repositories",
        click: function(event, ui){
            if ($(this).multiselect("getChecked").length == 0) {
                $.get('session.jsp', {data: "repository", action: "set", id: "", value: ""}, function(result) {
                    $usedcorpora.html(result);
                });
            }
            //$usedcorpora.text(ui.value + ' ' + (ui.checked ? 'checked' : 'unchecked') );
            //alert(ui.value + ' ' + (ui.checked ? 'checked' : 'unchecked') + " " + checkedboxes);
        },
        uncheckAll: function(event, ui) {
            $.get('session.jsp', {data: "repository", action: "set", id: "", value: ""}, function(result) {
                $usedcorpora.html(result);
            });

            $(this).multiselect("widget").find(":checkbox").each(function(){
                this.checked = false;
            });

            $(this).multiselect({noneSelectedText: "Select repositories"});
            $(this).multiselect("close");

        },
        selectedText: function(numChecked, numTotal, checkedItems){
            var values = $.map(checkedItems, function(checkbox){
                return checkbox.value;
            }).join(",");
            $.get('session.jsp', {data: "repository", action: "set", id: "", value: values}, function(result) {
                $usedcorpora.html(result);
            });
            if (numChecked == 0) {
                $usedcorpora.html("No repository selected");
            }

            if (numChecked > 1) {
                return 'Selected ' + numChecked + ' of ' + numTotal + ' repositories';
            } else {
                return 'Selected ' + numChecked + ' of ' + numTotal + ' repository';
            }
        }
    });
</script>

    <% }
}

%>
