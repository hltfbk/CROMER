<script type="text/javascript" src="js/jquery-2.0.3.min.js"></script>
<style>
    div.semrolepane {
                visibility: hidden;
                height:100%;
                width:100%;
                position:fixed;
                left:0;
                top:0;
                z-index:1000 !important;
                background-color: rgba(64, 64, 64, 0.5);
            }
            div.semroleform {
                left: 30%; top: 30%;
                position: absolute; background: #efefef; z-index: 10000;
                padding: 20px;
                filter:alpha(opacity=100); /* internet explorer */
                -khtml-opacity: 1;      /* khtml, old safari */
                -moz-opacity: 1;       /* mozilla, netscape */
                opacity: 1;   		/* fx, safari, opera */
                margin: auto auto auto auto;
                vertical-align: middle;
            }

</style>
<div id=semrolepane class=semrolepane>
    <div class=semroleform>
        <form onsubmit="return updateSemrole()" id=semroleform enctype="multipart/form-data">
            <input type=hidden name=sourceID id=sourceID value="" />
            <input type=hidden name=targetID id=targetID value="" />
            <input type=hidden name=tokenids id=tokenids value="" />
            <input type=hidden name=index id=index value="" />
            <input type=hidden name=docid id=docid value="" />

            Choose the semantic role: <select id=semrole name="semrole">
            <%
                String[] smValues = {"NONE","Arg0","Arg1","Arg2","Arg3","Arg4","Argm-LOC","Argm-OTHER"};
                for (String sm : smValues) {
                    out.println("<option value='"+sm+"'>"+sm);
                }
            %>
        </select>
            <p style='float: right'>
                <input type=submit value="Save">
                <input type="button" onclick="hideSemrolePane();" value="Cancel">
            </p>
        </form>
    </div>
</div>

<script>
    function setRole (itemEl, index, docid, sourceID, targetID, tokenids) {
        var semroleval = "";
        if (itemEl != null) {
            semroleval = itemEl.innerHTML;
        }
        el = document.getElementById("semrolepane");
        if (el != null) {
            el.style.visibility='visible';
            form = document.getElementById("semroleform");
            if (form != null) {
                form.sourceID.value=sourceID;
                form.targetID.value=targetID;
                form.tokenids.value=tokenids;
                form.index.value=index;
                form.docid.value=docid;
                var sEL = form.semrole; //document.getElementById('semrole');
                for (var i = 0; i < sEL.options.length; i++) {
                    if (sEL.options[i].text == semroleval) {
                        sEL.selectedIndex = i;
                        sEL.options[i].selected = true;
                        break;
                    }
                }
            }
        }
    }

    function updateSemrole () {
        var semroleEl = document.getElementById("semrole");
        var semrole = semroleEl.options[semroleEl.selectedIndex].text;
        var sourceID = document.getElementById("sourceID");
        if (sourceID != null) {
            var targetID = document.getElementById("targetID");
            var tokenids = document.getElementById("tokenids");
            var index = document.getElementById("index");
            var docid = document.getElementById("docid");
            $.ajax({
                url: 'update',
                type: 'GET',
                data: {data: "annotation", action: action, name: "relation",
                    sourceID: sourceID.value, targetID: targetID.value, relattribute: semrole,
                    sourceTokenIDs: tokenids.value, reltype: "HAS_PARTICIPANT",
                    index: index, docid: docid, group: "root"},
                async: false,
                cache:false,
                crossDomain: true,
                success: function(response) {
                    if (response == "error") {
                        $("#log").html("");
                        //alertify.alert("Another user is updating this instance in the same file. Try again later!");
                        alert("ERROR! This relation has not been stored correctly. Try again later or contact the administrator.");

                    } else {
                        window.open("annotatenews.jsp?index="+index+"&docid="+ docid +"&instance="+ENTITYID,"_self");

                        //ENTITYID=entityID;
                    }

                }
            });

            hideSemrolePane();
            refreshHighlight();
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
    }

    function detectkeys(e){
        //hide semrolepane
        if (e.keyCode == 27) {
            hideSemrolePane();
        }
    }

    $('body').keyup(function( event ) {
        detectkeys(event);
    });
</script>