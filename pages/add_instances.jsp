<%@ page import="servlet.WebController" %>
<%@ page import="java.util.*" %>
<%@ include file="setvar.jsp" %>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>CROMER - Shortcut instances</title>
    <link rel="stylesheet" href="css/jquery-ui.css" id="theme">
    <script type="text/javascript" src="js/jquery.js"></script>
    <script type="text/javascript" src="js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="js/alertify/alertify.min.js"></script>
    <link rel="stylesheet" href="js/alertify/alertify.core.css" />
    <link rel="stylesheet" href="js/alertify/alertify.default.css" />

    <%
        HttpSession currentsession = request.getSession();
        String index = request.getParameter("index");
        String docid = request.getParameter("docid");
        String instanceid = request.getParameter("instanceid");
        if (instanceid==null)
            instanceid="";
        String user = (String) currentsession.getAttribute("user");

    %>
    <script>
        CURRENTIDIVID=-1;
        ADDEDINSTANCES=0;
        function pressesckey(e) {
            if (e.keyCode == 27) {
                hideSemrolePane();
            }
        }

        function create(divid,action) {
            CURRENTIDIVID=-1;
            var name = $('#name').val();
            var type = $('#type').val();
            var descr = $('#descr').val();
            if (type == "" || name.trim().length == 0) {  //&& link.length > 0
                alertify.log('Check the mandatory fields "type" and "name" of instance! Fill in them and save again.',"error");
            } else {
                classname = type.replace(/.*\//,'');
                type = type.replace(/\/.*/,'');
                $.ajax({
                    type: "POST",
                    url: "update",
                    data: {data: "instance", action: "cart", id: "", classname: classname, type: type, name: name,
                        descr: descr, link: "", comment: "", time: "", beginterval: "", endinterval: "", haspart: ""},
                    async: true,
                    cache:false,
                    success: function(html) {
                        $('#icreate').html('<br><br><br><br><br>'+html);
                        disableMarkable(divid);
                        ADDEDINSTANCES=1;
                    }
                });
            }
        }


        function disableMarkable (divid) {
            var mdiv = document.getElementById("m"+divid);
            if (mdiv != null) {
                mdiv.style.backgroundColor="#ccc";
                mdiv.style.textDecoration="line-through";
                var cbox = document.getElementById("c"+divid);
                if (cbox != null) {
                    cbox.disabled="disabled";
                }
            }
        }

        function updateMarkable (entid, divid, type) {
            if (CURRENTIDIVID==divid && $('#icreate').is(":visible") ) {
                $('#icreate').hide();
                return;
            } else {
                $('#c'+CURRENTIDIVID).attr("checked", false);
            }

            CURRENTIDIVID=divid;
            var icreate = document.getElementById("icreate");
            if (icreate != null) {
                //$('#icreate').html('<form onsubmit="return create(this);">'+type+ '<input type=hidden name=id value=""><button>Create an instance</button></form>');
                var selectmenu ='<form action="javascript:javascript:create('+divid+')"><input type=hidden name=id value="" /><table class=table border=0 cellspacing=0 cellpadding=0><tr><td align=right>Type: </td>';
                selectmenu = selectmenu+ '<td><select name="type" id="type" onchange="javascript:showDiv(this.value);"><option value=""></option>';
                <%
                for (String option : markables.keySet()) {
                    for (String classname : markables.get(option)) {
                        out.println("selectmenu = selectmenu + '<option value=\""+option+"/"+classname+"\">"+option+"/"+classname+"</option>';");
                    }
                }
                %>
                selectmenu = selectmenu+ '</select></td></tr><tr><td align=right>Name: </td><td><input type=text name=name id=name value="'+entid+'" size=43></td></tr>';
                selectmenu = selectmenu+ '<tr><td align=right><span style="color: gray; ">Description: <span></td><td><input type=text name=descr id=descr value="" size=60></td></tr>';
                selectmenu = selectmenu+ '<tr><td></td><td><button id=createnew type=submit>Create a new instance and add it to the cart</button></td></tr></table></form>';

                $('#icreate').html('');
                $(selectmenu).appendTo('#icreate');
                $('#icreate').slideDown();

                el = document.getElementById("type");
                if (el != null) {
                    for (var i = 0; i < el.options.length; i++) {
                        if (el.options[i].value == type) {
                            el.selectedIndex = i;
                            el.options[i].selected = true;
                            break;
                        }
                    }
                }
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
            $('#icreate').hide();
            return false;
        }

        function reloadPane () {
            if (ADDEDINSTANCES == 1) {
                reloadPage('<%=index%>','<%=docid%>','<%=instanceid%>');
            } else {
                hideSemrolePane();
            }
        }
        hideSemrolePane();
    </script>
</head>

<body>
<%
    Map<String,String> CATannotation = WebController.getCATAnnotation(index, docid, user);

    List<String> keys = new LinkedList<String>(CATannotation.keySet());
    Collections.sort(keys);
%>
<button style="position: absolute; float: right; right: 20px; top: 3px" onclick="javascript:reloadPane();">close <img width=13 src="css/images/close.png"></button><br>
<div id=icreate style=' width: 100%; background: #ccc; padding: 4px; border-bottom: 1px solid #000'></div>
<div style='overflow-y: scroll; width: 100%; height: 350px'>
    <%
        Hashtable <String, String> mentions = new Hashtable<String, String>();
        String currentType="";
        int numDiv=0;
        for (String entid : keys) {
            entid =entid.replaceFirst("#m[^#]*$","").replaceAll("</*[^>]+>","");
            if (mentions.containsKey(entid)) {
                continue;
            }
            if (!currentType.equals(entid.replaceAll(" .*",""))) {
                currentType = entid.replaceAll(" .*","");
                out.println("&#9656; <b>"+ currentType+ "</b>:<br>");
            }

            mentions.put(entid,"1");
            entid = entid.replaceFirst("[^ ]* ","").replaceFirst("&#735;","");
            out.println("<div id='m"+numDiv+"' style='display: block'><input id='c"+numDiv+"' type=\"checkbox\" onclick=\"javascript:updateMarkable('"+entid+"','"+numDiv+"','"+currentType+"');\"> ");
            out.println(entid +"</div>");
            numDiv++;
        }
    %>
</div>

</body>
</html>