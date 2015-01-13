<%@ page import="servlet.WebController"%>
<%@ page import="java.util.LinkedHashMap"%>
<%@ page import="java.util.Map"%>
<%@ include file="errors.lbl" %>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>CROMER - Documents</title>
    <link rel="stylesheet" href="css/jquery-ui.css" id="theme">
    <link rel="stylesheet" href="css/cromer.css">
    <link rel="stylesheet" href="js/fileupload/jquery.fileupload-ui.css">
    <link rel="stylesheet" href="js/alertify/alertify.core.css" />
    <link rel="stylesheet" href="js/alertify/alertify.default.css" />
    <script type="text/javascript" src="js/alertify/alertify.min.js"></script>
</head>
<body>

<%
    boolean checkDB = WebController.checkDBConnection();
    String indexname = request.getParameter("index");
    String action = request.getParameter("action");

    String user = (String) request.getSession().getAttribute("user");
    if (user == null) {
        out.println("<script>window.location = 'header.jsp';</script>");
    } else {
        String fail = request.getParameter("fail");
        if (fail != null) {
            //out.println("FAIL: " + fail + " " + request.getParameter("name"));
            if (fail.equals("0")) {
                if (action != null) {
                    if (action.equals("add")) {
                        out.println("<script>alertify.log('All documents have been added successfully.', 'success');</script>");
                    } else if (action.equals("remove")) {
                        out.println("<script>alertify.log('The document has been deleted.', 'success');</script>");
                    }
                }
            } else {
                out.println("<script>alertify.log('ERROR! The update is failed.','error');</script>");
            }
        } else {
            if (action != null) {
                if (action.equals("list")) {
                    if (indexname != null) {
                        LinkedHashMap<String, String> docinfo = WebController.getInfoDocs(indexname);
                        if (docinfo.size() > 0) {
                            out.println("<b>"+indexname+"</b> (" +docinfo.size()+ ")<hr>");
                            for (Map.Entry entry : docinfo.entrySet()) {
                                out.println("<a href=\"javascript:removedoc('"+ indexname + "','"+ entry.getKey() +"');\" title=\"delete document\"><img width=14 src='css/images/trash.png'></a> <a href='javascript:openNew(\"annotatenews.jsp?index="+indexname+"&docid="+entry.getKey()+"&query=\",\"annotatenews\");'><img src='css/images/dimage.gif'></a> " +entry.getValue());
                                if (checkDB) {
                                    int linkedInstances = WebController.getLinkedInstancesFromDoc(entry.getKey().toString(),user);

                                    if (linkedInstances > 0) {
                                        out.println(" (<img src=\"css/images/link-icon.gif\"><b>"+linkedInstances+"</b>)");
                                    }
                                }
                                out.println("<br>");
                            }

                        } else {
                            out.println("<script>alertify.log('Repository <b>"+indexname+"</b> is empty!');</script>");
                        }
                        return;
                    }
                }
            }
        }


        String[] repodirs = WebController.getIndexNames();
%>
<div class=header id="divheader">
    <jsp:include page="header.jsp" flush="true">
        <jsp:param name="page" value="<%= request.getRequestURL().toString() %>" />
    </jsp:include>
</div>


<div class=search>
    <%
        if (repodirs.length == 0) {
            out.println("<font color=red>ATTENTION!</font> Create a repository and then add some documents to it.");
        } else {



    %>

    <form id="file_upload" action="upload" method="POST" enctype="multipart/form-data">
        <div style="display: inline-block; margin-right: 10px; margin-top: 10px; float: left">Step 1:</div>
        <div id="drop_zone_1">
            <input id="file_1" type="file" name="file_1" multiple>
            <div>Upload one or more new files</div>
        </div>
        <div style="margin-left: 55px;overflow-y: auto; height: 300px"><table id="files_1" style="font-size: 12px; background: #ccffcc	; left: 10px" cellspacing=3 cellpadding=1></table>
        </div>
        <div id=index style="display: none">
            <br>Step 2: <b>Select the language of the files:</b>
            <select id="langselect">
                <option value="" selected="selected"></option>
                <option value="english">English</option>
                <option value="italian">Italian</option>
            </select>
            <br><br>Step 3: <b>Select a repository target: </b><select id="reposelect"><option value=""></option>
            <%
                for (int i=0; i<repodirs.length; i++) {
                    out.println("<option value='"+ repodirs[i] + "'>"+repodirs[i]+"</option>");
                }
            %>
        </select>
            <br>
            <div style="float: right"><input type=button value="Submit" onclick="javascript:indexdoc();"></div>
            <div style="float: right"><input type=button value="Cancel" onclick="javascript:removecache();"></div>
        </div>
    </form>
    <% } %>

</div>



<script src="js/jquery.js"></script>
<script src="js/jquery-ui.min.js"></script>
<script src="js/fileupload/jquery.fileupload.js"></script>
<script src="js/fileupload/jquery.fileupload-ui.js"></script>

<%
    if (action == null || !action.equals("removecache")) {
%>
<script>
    $.get('upload', {data: "document", action: "getcache"}, function(result) {
        if (result.length > 0) {
            $("#files_1").html(result);
            $("#files_1").show();
            $("#index").show();
        }
    });

    function openNew(url, target) {
        newWindow = window.open(url, target,"toolbar=no,menubar=0,status=0,copyhistory=0,scrollbars=yes,resizable=1,location=0,Width=1500,Height=760,modal=yes	,alwaysRaised=yes,fullscreen=yes'") ;
        newWindow.location = url;
        newWindow.focus();
    }
</script>
<%
    }
%>

<script>
    $("#doclist").hide();
    function listdoc(el) {
        index = el.options[el.selectedIndex].value;
        $.get('documents.jsp', {action: "list", index: index}, function(result) {
            $("#doclist").html(result);
            if (result.length > 620) {
                $("#doclist").show();
            } else {
                $('#doclist').hide();
            }
        });
    }

    function removedoc(index, docid) {
        alertify.set({ buttonFocus: "cancel" });
        alertify.confirm("ATTENTION! Are you sure you want to REMOVE this document?<br>(Note that all document information and its annotations will disappear!)", function (e) {
            if (e) {
                $.get('update', {data: "document", action: "remove", name: index, id: docid}, function(result) {
                    if (result.length > 0) {
                        alertify.log('The document has been deleted from the repository.','success');
                        listdoc(index);
                    }
                });
            }
        });
    }

    function removecache () {
        alertify.set({ buttonFocus: "cancel" });
        alertify.confirm("ATTENTION! Are you sure to DELETE all cached documents on the server?<br>", function (e) {
            if (e) {
                $.get('upload', {data: "document", action: "removecache"}, function(result) {
                    if (result.length > 0) {
                        $("#files_1").text("");
                        $("#files_1").hide();
                        $("#index").hide();
                        alertify.log('All cached documents have been deleted from the server.','log');
                    } else {
                        alertify.log('ERROR! The cache removing is failed.','error');
                    }
                });
            }
        });
    }

    function indexdoc() {
        var repoval = document.getElementById("reposelect").value;
        var langval = document.getElementById("langselect").value;
        if (repoval.length > 0 && langval.length > 0) {
            window.open("update?data=document&action=add&name="+repoval+"&group="+langval, "_self");

            //$.get('upload', {data: "document", action: "add", name: repoval}, function(result) {
            //	alertify.log(result, 'success');
            //});
        } else {
            alertify.error('Choose the language of the documents and a repository target, please!');
        }
    }

    function isValidType (filename) {
        if (filename.match(/.txt$/) != null ||
                filename.match(/.xml$/) != null ||
                filename.match(/.zip$/) != null) {
            return true;
        }
        return false;
    }

    function stopUpload(obj) {
        obj.stop();
        alertify.log('Uploading has been stopped!');
    }

    /*global $ */
    $(function () {
        var initFileUpload = function (suffix) {
            $('#file_upload').fileUploadUI({
                namespace: 'file_upload_' + suffix,
                fileInputFilter: '#file_' + suffix,
                dropZone: $('#drop_zone_' + suffix),
                uploadTable: $('#files_' + suffix),
                downloadTable: $('#files_' + suffix),
                buildUploadRow: function (files, index) {
                    $("#doclist").hide();
                    $("#files_1").show();
                    if (files[index].size == 0) {
                        alert("ERROR! The file is empty.");
                        return false;
                    } else if (!isValidType(files[index].name)) {
                        //alert(files[index].name + " " + getBytesWithUnit(files[index].size));
                        alert("ERROR! File type not allowed (use .txt or .xml files, or zip archives of the pure text files).");
                        return false;
                    } else {
                        return $('<tr><td>' + files[index].name + '<\/td>' +
                                '<td>' +getBytesWithUnit(files[index].size)+ '<\/td>' +
                                '<td class="file_upload_progress"><div><\/div><\/td>' +
                                '<td class="file_upload_cancel">' +
                                '<a href="javascript:stopUpload(this);" class="ui-icon ui-icon-close" title="stop uploading">Stop<\/a><\/td><\/tr>');
                    }
                },
                buildDownloadRow: function (file) {
                    if (isValidType(file.name) && file.size > 0) {
                        $("#index").show();
                        return $('<tr><td>' + file.name + '<\/td><td align=right><font color=#888> ...cached!</font></td><\/tr>');
                    }
                }
            });
        };
        initFileUpload(1);
        $("#index").hide();
    });

    /**
     * @function: getBytesWithUnit()
     * @purpose: Converts bytes to the most simplified unit.
     * @param: (number) bytes, the amount of bytes
     * @returns: (string)
     */
    var getBytesWithUnit = function( bytes ){
        if( isNaN( bytes ) ){ return; }
        var units = [ ' bytes', ' KB', ' MB', ' GB', ' TB', ' PB', ' EB', ' ZB', ' YB' ];
        var amountOf2s = Math.floor( Math.log( +bytes )/Math.log(2) );
        if( amountOf2s < 1 ){
            amountOf2s = 0;
        }
        var i = Math.floor( amountOf2s / 10 );
        bytes = +bytes / Math.pow( 2, 10*i );

        // Rounds to 3 decimals places.
        if( bytes.toString().length > bytes.toFixed(1).toString().length ){
            bytes = bytes.toFixed(1);
        }
        return bytes + units[i];
    };
</script>

<div style="display: inline-block; position:absolute; top: 30px; right: 0px;z-index:99; float:right;">
    <div class=settings id=settings>
        <img src='css/images/repository.png'>
        <%
            if (!checkDB) {
                out.println(MYSQL_CONNECTION_FAILED);
            }

            if  (repodirs.length == 0) {
                out.println("No repositories is available! &nbsp;");
            } else {
                out.println("Repositories found: <b>" +repodirs.length +"</b>");

                //String[] repodocs= WebController.getIndexDocs();
                String groupname = "";
                if (repodirs.length > 0) {
                    out.println("<br>Show documents from repository: <select name=repo id=repo onchange=\"javascript:listdoc(this);\">");
                    for (int i=0; i<repodirs.length; i++) {
                        String item = repodirs[i].replaceFirst("[/|\\\\].+","");
                        if (!item.equals(groupname)) {
                            groupname = item;
                            //out.println("<tr><td bgcolor=#ddd align=center colspan=3>"+groupname+"</td></tr>");
                            out.println("<option value='' disable>\n<option value='' disabled>" + groupname + "</option>\n");
                            out.println("<option value='' disabled>-----------------</option>\n");
                        }
                        String reponame = repodirs[i].replaceFirst(".+[/|\\\\]","");
                        out.print("<option value='"+repodirs[i]+"'");
                        if (indexname != null && indexname.equals(repodirs[i])) {
                            out.print(" selected");
                        }
                        out.println(">" + reponame + "</option>\n");
                        //out.println("<tr bgcolor='#fff'><td><a href=\"javascript:listdoc('"+repodirs[i]+"');\"><img width=20 src='css/images/listview.gif'></a> <b>"+ reponame +"</b>&nbsp;</td><td align=right>&nbsp;"+repodocs[i]+" docs&nbsp;</td><tr>");
                    }
                    out.println("</select>");

                }


            }
            out.println("</div>");

        %>

        <br>
        <div id=doclist style='background: #fff; position: relative; top: 20px; right: 20px; display: none; float: right; border: 2px solid #bbb; padding: 3px; font-size: 14px'></div>
    </div>
        <%
}
    if (indexname != null && action == null) {
       out.print("<script>listdoc(document.getElementById('repo'));</script>");
    }
%>
</body>
</html>
