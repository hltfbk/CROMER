alertify.set({ buttonFocus: "cancel" });

var DISTOKENIDS = [];
var down = null;
var WHITE = "";
var SELECTID = 0;
var ENTITYID = "";
var INDEX = "";
var ENTITYID = "";
var ENTITYCOLOR = "rgb(255, 178, 102)";
var SELECTEDCOLOR = "rgb(210, 210, 210)";
//la dimensione della variabile BORDERLINE deve essere uguale alla dimensione dei border nel css div del file anotatenews.jsp
var BORDERLINE = "3px";
var BORDERCOLOR = BORDERLINE + " solid blue";
var SELECTEDBORDERCOLOR = BORDERLINE + " solid ";
var NOBORDERCOLOR = BORDERLINE + " solid white";
var PREVSELECTEDEL = null;
var PREVHIGHLIGHTEL = null;
var PREVHIGHLIGHTCOLOR = "";
var PREVORANGEHIGHLIGHTEL = null;
var PREVORANGEHIGHLIGHTCOLOR = "";

var entityFloatingIsVisible = 0;
var selectAnnotationType = "instance"; // it could be "instance" or "relation"
var EXTTOKENIDS = null;
var OVERMENU=0;
var CURRENTCLASS= null;
var cRadius = "10px";

//Detect 'Shift' Key down within mouse function
var shiftDown = false;
$(document).bind('keydown', function(e){
    if (e.keyCode === 16 || e.charCode === 16) {
        shiftDown = true;
        //$("#log").html("shift down");
        DISTOKENIDS = [];
    }
});

$(document).bind('keyup', function(e) {
    if (e.keyCode === 16 || e.charCode === 16) {
        shiftDown = false;
        if (DISTOKENIDS.length == 1) {
            setColor(DISTOKENIDS[0], SELECTEDCOLOR, 0, 2);
        } else {
            DISTOKENIDS.sort(function(a, b){return a-b});
            setColor(DISTOKENIDS[0], SELECTEDCOLOR, 0, 0);
            setColor(DISTOKENIDS[DISTOKENIDS.length -1], SELECTEDCOLOR, 0, 1);
            for (i=1; i<DISTOKENIDS.length; i++) {
                if (parseInt(DISTOKENIDS[i-1]) == (parseInt(DISTOKENIDS[i])-1)) {
                    setColor((DISTOKENIDS[i]-1)+"-"+DISTOKENIDS[i], SELECTEDCOLOR, 0, -1);
                }
            }
        }

    }
});

$('div').mouseover( function () {
    if (this.style.backgroundColor == ENTITYCOLOR) {
        //this.style.cursor = "not-allowed";
        this.style.cursor = "url(css/images/erase.ico), auto";
    }
});

function mymousedown(el) {
    down = el.id;
}

function mymouseup(el) {
    entityFloating = document.getElementById("entityFloating");
    if (entityFloatingIsVisible==1) {
        return;
    }

    if (down != null && el.id != "log" && el.id != "entities" && el.id.indexOf("-") == -1) {
        if (shiftDown) {
            if (DISTOKENIDS.indexOf(el.id) == -1) {
                DISTOKENIDS.push(el.id);
            } else {
                DISTOKENIDS.splice(DISTOKENIDS.indexOf(el.id),1);
                setColor(parseInt(el.id), WHITE, 0, -1);
            }
            //$("#log").html("#"+el.id + " "+DISTOKENIDS);

            if (DISTOKENIDS.length > 0) {
                for (var i=0; i<DISTOKENIDS.length; i++) {
                    setColor(DISTOKENIDS[i], SELECTEDCOLOR, 0, -1);
                }
            }
        } else {

            var coloring = "";
            //el = document.getElementById(down);
            if (el.style.backgroundColor == ENTITYCOLOR) {
                alertify.confirm("ATTENTION! Do you want to REMOVE this "+ENTITYTYPE+" annotation?", function (e) {
                    if (e) {
                        down=null;

                        $.ajax({
                            url: 'update',
                            type: 'GET',
                            //dataType: 'jsonp',
                            data: {data: "annotation", action: "remove", name: ENTITYTYPE, tokenids: el.id, id: ENTITYID, index: INDEX, docid: DOCID, group: "root"},
                            //data: 'ranges='+ranges,
                            async: false,
                            cache:false,
                            crossDomain: true,
                            success: function(response) {
                                if (response == "error") {
                                    $("#log").html("");
                                    //alertify.alert("Another user is updating this instance in the same file. Try again later!");
                                    alert("ERROR! Your annotation has not been stored correctly. Try again later or contact the administrator.");
                                    window.open("annotatenews.jsp?index="+INDEX+"&docid="+DOCID+"&query=","_self");
                                } else {
                                    //$("#log").html('Removing... ' +response);
                                    //selectTokens(response, WHITE)
                                    //$("#log").html("Annotation has been removed!");
                                    window.open("annotatenews.jsp?index="+INDEX+"&docid="+DOCID+"&instance="+ENTITYID+"&updatetype="+ENTITYTYPE+"&query=","_self");

                                }

                            },
                            error: function(response, xhr,err ) {
                                //alert(err+"\nreadyState: "+xhr.readyState+"\nstatus: "+xhr.status+"\nresponseText: "+xhr.responseText);
                                //alert(ENTITYID);
                                switch(xhr.status) {
                                    case 200:
                                        $("#log").html('<font color=gray>Data saved!</font>');
                                        break;
                                    case 404:
                                        $("#log").html('<font color=red>Could not contact server.</font>');
                                        break;
                                    case 500:
                                        $("#log").html('<font color=red>A server-side error has occurred.</font>');
                                        break;
                                }
                            }
                        });
                    }
                });

                return false;
            } else {

                if (el.style.backgroundColor == WHITE) {
                    coloring = SELECTEDCOLOR;
                } else {
                    coloring = WHITE;
                }
            }
            //$("#log").html(coloring + " SET " +down + " " +el.id);

            if (parseInt(down) >= parseInt(el.id)) {
                selectBetweenTokens(el.id, down.replace(/\-.+/g, ""), coloring, 2);
            } else {
                selectBetweenTokens(down.replace(/.+\-/g, ""), el.id, coloring, 2);
            }
        }

        if (EXTTOKENIDS != null && OVERMENU==0) {
            deselectAllTokens ();
        }
        clearSelectedText();
        down = null;

    }

}

function refreshHighlight () {
    //alert(id +" " +ranges);
    var i=1;
    while (true) {
        var el = document.getElementById(i);
        if (el != null) {
            if (el.style.borderTop==BORDERCOLOR) {
                el.style.borderBottom=BORDERCOLOR;
            } else {
                el.style.borderBottom=NOBORDERCOLOR;
            }

            var elspace = document.getElementById((i-1)+"-"+i);
            if (elspace != null) {
                if (elspace.style.borderTop==BORDERCOLOR) {
                    elspace.style.borderBottom=BORDERCOLOR;
                } else {
                    elspace.style.borderBottom=NOBORDERCOLOR;
                }
            }
        } else {
            break;
        }
        i++;
    }
}

function highlightTokens2 (id, ranges, classlabel, el, classcolor, classcolorlight) {
    if (SELECTID != id) {
        refreshHighlight();
        SELECTID = id;

        if (el != null && el != PREVORANGEHIGHLIGHTEL) {
            el.style.backgroundColor=SELECTEDCOLOR;
        }
        if (PREVHIGHLIGHTEL != null && PREVHIGHLIGHTEL != PREVORANGEHIGHLIGHTEL) {
            //alert(el.id + " " + PREVHIGHLIGHTEL.id+ " "+ el.style.backgroundColor + " " +PREVHIGHLIGHTCOLOR)
            PREVHIGHLIGHTEL.style.backgroundColor=PREVHIGHLIGHTCOLOR;
        }
        PREVHIGHLIGHTCOLOR = classcolorlight;
        PREVHIGHLIGHTEL = el;

        var i=1;
        var tokids = ranges.split(",");
        if (tokids.length > 0) {
            for (i=0; i<tokids.length; i++) {
                var ids=tokids[i].split(" ");
                var previd = 0;

                for (var f=0; f<ids.length; f++) {
                    el = document.getElementById(ids[f]);
                    // alert(el + " " +ids[f] + " - " +el.style.borderBottom);
                    if (el != null) {
                        el.style.borderBottom=SELECTEDBORDERCOLOR+" "+classcolor;
                        if (ids[f] == parseInt(previd) + 1) {
                            elspace = document.getElementById(previd+"-"+ids[f]);
                            if (elspace != null) {
                                elspace.style.borderBottom=SELECTEDBORDERCOLOR+" "+classcolor;
                            }
                        }

                    }
                    previd = ids[f];
                }
            }
        }
    }

}

function highlightTokens (ranges, classlabel, el) {
    if (OVERMENU == 1) {
        return;
    }
    deselectAllTokens();
    //entityFloatingIsVisible=showfloating;
    CURRENTCLASS=classlabel;
    OVERMENU=1;
    EXTTOKENIDS = ranges;
    //alert("ranges: "+ranges);
    PREVSELECTEDEL=el;
    el.style.borderBottom=BORDERCOLOR;

    var tokids = ranges.split(",");

    if (tokids.length > 0) {
        for (var i=0; i<tokids.length; i++) {
            var ids=tokids[i].split(" ");
            var foundEntity = 0;
            for (var f=0; f<ids.length; f++) {
                el = document.getElementById(ids[f]);
                if (el != null) {
                    if (el.style.backgroundColor == ENTITYCOLOR) {
                        foundEntity = 1;
                        el.style.borderBottom=BORDERCOLOR;
                        el.style.borderTop=BORDERCOLOR;
                        //  break;
                    }
                }
                //coloro anche gli spazi
                if (f>0) {
                    el = document.getElementById(ids[(f-1)]+"-"+ids[f]);
                    if (el != null) {
                        if (el.style.backgroundColor == ENTITYCOLOR) {
                            foundEntity = 1;
                            el.style.borderBottom=BORDERCOLOR;
                            el.style.borderTop=BORDERCOLOR;
                            //alert(ids[(f-1)]+"-"+ids[f]);
                            //  break;
                        }
                    }
                }
            }
            if (foundEntity==0) {
                selectTokens(tokids[i],SELECTEDCOLOR);
            }
            //$("#log").html(foundEntity + " " +  ranges);
        }
    }
}

function refresh() {
    deselectAllTokens ();
    if (PREVHIGHLIGHTEL != null) {
        PREVHIGHLIGHTEL.style.backgroundColor=PREVHIGHLIGHTCOLOR;
    }
    if (PREVORANGEHIGHLIGHTEL != null) {
        PREVORANGEHIGHLIGHTEL.style.backgroundColor=PREVORANGEHIGHLIGHTCOLOR;
    }
    var el = document.getElementById("corefcolor");
    if (el != null) {
        el.options[0].selected = true;
    }

    var i=1;
    // $("#log").html("");
    while (true) {
        el = document.getElementById(i);
        if (el!=null) {
            el.style.backgroundColor = WHITE;
            elspace = document.getElementById((i-1)+"-"+i);
            if (elspace!=null) {
                elspace.style.backgroundColor = WHITE;
            }
        } else {
            break;
        }
        i++;
    }
}

function deselectAllTokens () {
    EXTTOKENIDS=null;
    CURRENTCLASS=null;
    SELECTID=0;
    if (PREVSELECTEDEL!=null) {
        PREVSELECTEDEL.style.borderBottom="none";
    }

    var i=1;
    var el;
    var elspace;
    // $("#log").html("");
    while (true) {
        el = document.getElementById(i);
        if (el != null) {
            el.style.borderBottom=NOBORDERCOLOR;
            el.style.borderTop=NOBORDERCOLOR;

            elspace = document.getElementById((i-1)+"-"+i);
            if (elspace != null) {
                elspace.style.borderBottom=NOBORDERCOLOR;
                elspace.style.borderTop=NOBORDERCOLOR;
            }
            if (el.style.backgroundColor == SELECTEDCOLOR) {
                el.style.backgroundColor = WHITE;
                el.style.borderRight = "0px solid white";
                el.style.borderLeft = "0px solid white";

                if (elspace != null) {
                    elspace.style.backgroundColor = WHITE;
                }
            }
        } else {
            break;
        }
        i++;
    }

    el = document.getElementById("entityFloating");
    if(el != null) {
        el.style.visibility="hidden";
    }
    //$("#log").html("LAST DESELECT "+i+" ("+text+"), ERRORS " +error);

}

function selectTokens (ranges, color) {
    //$("#log").html("SET " +ranges + " "+color);
    //alert("SET " +ranges + " "+color);
    if (ranges == null || ranges.length == 0) {
        return;
    }
    var tokenspans = ranges.split(",");
    if (tokenspans.length>1) {
        for (var s=0; s<tokenspans.length; s++) {
            selectTokens(tokenspans[s],color);
        }
        return;
    }

    var tokenids = ranges.split(" ");
    if (tokenids.length > 1) {
        var idprev=0;
        //alert(ranges);
        //if the first token id is also the first toekn of the annotation put 0
        //if the last token id is the last token of the annotation put 1
        //if there are both the first and the last token id put 2
        //-1 otherwise
        var segmentPos=0;
        for (var i=idprev; i<tokenids.length; i++) {
            if (i > 0 && parseInt(tokenids[i]) != parseInt(tokenids[i-1])+1) {
                selectBetweenTokens(tokenids[idprev],tokenids[i-1],color, segmentPos);
                segmentPos=-1;
                idprev=i;
            }
            if (i==tokenids.length-1) {
                if (segmentPos==0) {
                    segmentPos=2;
                } else {
                    segmentPos=1;
                }
                selectBetweenTokens(tokenids[idprev],tokenids[i], color, segmentPos);
            }
        }
    } else {
        //$("#log").html("SET2 " +ranges + " "+color);
        selectBetweenTokens(ranges,ranges,color, 2);
    }
}

function selectBetweenTokens (down, up, color, segmentPos) {
    //$("#log").html("selectBetweenTokens " +down + ","+up+" " + color + " " +segmentPos + " (" +EXTTOKENIDS +") -- " + OVERMENU);
    var joinPrev=0;
    down =parseInt(down);
    up=parseInt(up);
    if (up<down) {
        var valtmp = up;
        up = down;
        down = valtmp;
    }
    //if the first token id is also the first token of the annotation put 0
    //if the last token id is the last token of the annotation put 1
    //if there are both the first and the last token id put 2
    //-1 otherwise

    for (var i=down; i<=up; i++) {
        var headtail = -1;
        if (up>down && i>down) {
            joinPrev=1;
        }
        if (i == down) {
            if (segmentPos == 0 || segmentPos == 2) {
                headtail=0;
            }
        }
        if (i==up ) {
            if (segmentPos == 1 || segmentPos == 2) {
                if (headtail == 0) {
                    headtail=2;
                } else {
                    headtail=1;
                }
            }
        }
        setColor(i, color, joinPrev, headtail);
    }
}


function setColor (elID, color, joinPrev, headtail) {
    //$("#log").html("setColor " +elID + " " + color + " " +joinPrev);
    //$("#log").html($("#log").html() + "; setColor:" + +elID + " " + color + " " +joinPrev+ " "+headtail+"<br>");
    var BRACKETSCOLOR = "#777";
    if (color == WHITE || color == "") {
        BRACKETSCOLOR="#fff";
    }
    var myel = null;
    if (color == WHITE) {
        myel = document.getElementById((elID-1)+"-"+elID);
        if (myel != null) {
            myel.style.backgroundColor = color;
            myel.style.borderBottom=NOBORDERCOLOR;
            myel.style.borderTop=NOBORDERCOLOR;
        }
        myel = document.getElementById(elID+"-"+(elID+1));
        if (myel != null) {
            myel.style.backgroundColor = color;
            myel.style.borderBottom=NOBORDERCOLOR;
            myel.style.borderTop=NOBORDERCOLOR;
        }
        myel.style.cursor="text";
    } else {
        if (joinPrev == 1) {
            myel = document.getElementById((elID-1)+"-"+elID);
            if (myel != null) {
                myel.style.backgroundColor = color;
                if (color == SELECTEDCOLOR) {
                    myel.style.borderBottom=BORDERCOLOR;
                    myel.style.borderTop=BORDERCOLOR;
                }
            }
        }

    }
    myel = document.getElementById(elID);
    myel.style.cursor="text";
    if (myel != null) {
        /*var style = "background: "+color;
         if (headtail == 0) {
         style = style+";border-radius: "+cRadius+" 0px 0px "+cRadius;
         } else if (headtail == 1) {
         style = style+";border-radius: 0px "+cRadius+" "+cRadius+" 0px;";
         } else if (headtail == 2) {
         style = style+";border-radius: "+ cRadius;
         } else {
         style = style+";border-radius: 0px 0px 0px 0px";
         }
         myel.style = style;
         */

        //code for webkit based browser
        if (headtail == 0) {
            myel.style.borderRadius = cRadius+" 0px 0px 0px";
            if (color == WHITE || color == "") {
                myel.style.borderRight = "0px solid #fff";
            } else {
                myel.style.borderRight = "0px solid "+ color;
            }
            myel.style.borderLeft = "0px solid "+BRACKETSCOLOR;
            //myel.style.borderBottom = "1px solid "+BRACKETSCOLOR;

        } else if (headtail == 1) {
            myel.style.borderRadius = "0px "+cRadius+" 0px 0px";
            if (color == WHITE || color == "") {
                myel.style.borderLeft = "0px solid #fff";
            } else {
                myel.style.borderLeft = "0px solid "+ color;
            }
            myel.style.borderRight = "0px solid "+BRACKETSCOLOR;
            //myel.style.borderBottom = "1px solid "+BRACKETSCOLOR;
        } else if (headtail == 2) {
            myel.style.borderRadius = cRadius+" "+cRadius+" 0px 0px";
            myel.style.borderLeft = "0px solid "+BRACKETSCOLOR;
            myel.style.borderRight = "0px solid "+BRACKETSCOLOR;
            //myel.style.borderBottom = "1px solid "+BRACKETSCOLOR;
        } else {
            myel.style.borderRadius = "0px 0px 0px 0px";
            if (color == WHITE || color == "") {
                myel.style.borderLeft="0px solid #fff";
                myel.style.borderRight="0px solid #fff";
                //myel.style.borderBottom = "1px solid #fff";
            } else {
                myel.style.borderLeft = "0px solid "+color;
                myel.style.borderRight = "0px solid "+color;
                //myel.style.borderBottom = "1px solid "+color;
            }
        }
        myel.style.background=color;
        if (color == SELECTEDCOLOR) {
            myel.style.borderBottom=BORDERCOLOR;
            myel.style.borderTop=BORDERCOLOR;
        }  else {
            myel.style.borderBottom=NOBORDERCOLOR;
            myel.style.borderTop=NOBORDERCOLOR;

        }

        //myel.style = "background: "+color+";-moz-border-radius-topleft: 10px; -webkit-border-top-left-radius: 10px; border-radius: 10px;";
    }
    return true;
}

function clearSelectedText() {
    if (window.getSelection) {
        window.getSelection().removeAllRanges();
    } else if (document.getSelection) {
        document.getSelection().empty();
    } else {
        var selection = document.selection && document.selection.createRange();
        if (selection.text) {
            selection.collapse(true);
            selection.select();
        }
    }

    //check the unballanced highlights
    var OPENED=0;
    var elNotClosed=null;
    var i=1;
    var el;
    $("#log").html("");
    while (true) {
        el = document.getElementById(i);
        if (el != null) {
            if (el.style.backgroundColor == SELECTEDCOLOR) {
                if (el.style.borderBottomLeftRadius == cRadius) {
                    OPENED++;
                } else if (OPENED==0) {
                    //el.style.borderBottomLeftRadius = cRadius;
                    el.style.borderTopLeftRadius = cRadius;
                    break;
                }
                if (el.style.borderBottomRightRadius == cRadius) {
                    OPENED=OPENED-1;
                    if (OPENED == 0)
                        elNotClosed=null;
                } else {
                    if (OPENED == 1)
                        elNotClosed=el;
                }
            }
        } else {
            break;
        }
        i++;
    }
    if (elNotClosed != null) {
        elNotClosed.style.borderBottomRightRadius = cRadius;
        //elNotClosed.style.borderTopRightRadius = cRadius;
    }
}

function getSelectedTokenIDs () {
    var ranges = "";
    if (EXTTOKENIDS != null) {
        ranges = EXTTOKENIDS;
    } else {
        var elid = 1;
        while(true) {
            el = document.getElementById(elid);
            if (el == null) {
                break;
            } else {
                if (el.style.backgroundColor == SELECTEDCOLOR) {
                    ranges += elid;
                    if (el.style.borderTopRightRadius == cRadius) {
                        ranges +=",";
                    } else {
                        ranges +=" ";
                    }
                }
            }
            elid++;
        }
        ranges = ranges.replace(/[\s|,]+$/g, "");
    }
    return ranges;
}

function saveRelation(sourceID, semrole, targetID, classname) {
    var ranges = getSelectedTokenIDs();

    $.ajax({
        url: 'update',
        type: 'GET',
        data: {data: "annotation", action: "add", name: "relation",
            sourceID: sourceID, targetID: targetID, relattribute: semrole,
            sourceTokenIDs: ranges, reltype: "HAS_PARTICIPANT",
            index: INDEX, docid: DOCID, group: "root"},
        async: false,
        cache:false,
        crossDomain: true,
        success: function(response) {
            //$("#log").html("update?index=<%= index %>&docid=<%= docid %>&data=annotation&action=add&name=relation&sourceID="+sourceID+"&targetid="+targetID+"&relattribute="+semrole+"&sourceTokenIDs="+ranges+"&reltype=HAS_PARTICIPANT");
            if (response == "error") {
                $("#log").html("");
                //alertify.alert("Another user is updating this instance in the same file. Try again later!");
                alert("ERROR! This relation has not been stored correctly. Try again later or contact the administrator.");

            } else {
                window.open("annotatenews.jsp?index="+INDEX+"&docid="+DOCID+"&instance="+response+"&updatetype=relation","_self");
                //select("relation",null,response);
                //selectTokens(ranges, ENTITYCOLOR);
                //ENTITYID=entityID;
            }

        }
    });
}

function saveAnnotation(entityID,classname) {
    entityFloatingIsVisible = 0;
    if (CURRENTCLASS != null) {
        if (classname.indexOf(CURRENTCLASS) == -1) {
            alertify.confirm("ATTENTION! The type of some selected mentions and the type of the instance are not the same. Do you want to continue?", function (e) {
                if (e) {
                    CURRENTCLASS=null;
                    saveAnnotation(entityID,classname);
                }
            });
            /*if (!confirm("ATTENTION! The mention type is not equal to the instance type. Do you want to continue?")) {
             return;
             } */
        } else {
            CURRENTCLASS=null;
            saveAnnotation(entityID,classname);
        }
    } else {
        var ranges = getSelectedTokenIDs();
        if (ranges.trim() != "") {
            //alert(ranges);

            $.ajax({
                url: 'update',
                type: 'GET',
                //dataType: 'jsonp',
                data: {data: "annotation", action: "add",
                    name: selectAnnotationType, tokenids: ranges,
                    id: entityID, index: INDEX, docid: DOCID, group: "root"},
                //data: 'ranges='+ranges,
                async: false,
                cache:false,
                crossDomain: true,
                success: function(response) {
                    if (response == "error") {
                        $("#log").html("");
                        //alertify.alert("Another user is updating this instance in the same file. Try again later!");
                        alert("ERROR! Your annotation has not been stored correctly. Try again later or contact the administrator.");
                        window.open("annotatenews.jsp?index="+INDEX+"&docid="+DOCID+"&query=","_self");
                    } else {
                        //update list of annotated mentions
                        window.open("annotatenews.jsp?index="+INDEX+"&docid="+DOCID+"&instance="+entityID+"&updatetype=instance&query=","_self");

                        ENTITYID=entityID;
                    }
                },
                error: function(response, xhr,err ) {
                    //alert(err+"\nreadyState: "+xhr.readyState+"\nstatus: "+xhr.status+"\nresponseText: "+xhr.responseText);
                    switch(xhr.status) {
                        case 200:
                            $("#log").html('<font color=gray>Data saved!</font>');
                            break;
                        case 404:
                            $("#log").html('<font color=red>Could not contact server.</font>');
                            break;
                        case 500:
                            $("#log").html('<font color=red>A server-side error has occurred.</font>');
                            break;
                    }
                }
            });

        }
    }
}

function setCoreference() {
    selectAnnotationType = "instance";
    $("#headerEntityFloating").html("=&gt; Add coreference to: &nbsp;&nbsp;");
}

function setRelation() {
    selectAnnotationType = "relation";
    $("#headerEntityFloating").html("=&gt; Add has participant to:&nbsp;");
}

function setTlink() {
    selectAnnotationType = "";
    $("#headerEntityFloating").html("");
}

function select (type, id, tokensids) {
    ENTITYTYPE=type;
    ENTITYID=id;
    refresh();
    selectTokens(tokensids, ENTITYCOLOR);
    if (PREVORANGEHIGHLIGHTEL != null && PREVORANGEHIGHLIGHTEL != PREVHIGHLIGHTEL) {
        if (PREVHIGHLIGHTEL != null) {
            PREVHIGHLIGHTEL.style.backgroundColor = ENTITYCOLOR;
        }
        if (PREVORANGEHIGHLIGHTEL != null) {
            PREVORANGEHIGHLIGHTEL.style.backgroundColor = PREVORANGEHIGHLIGHTCOLOR;
        }
    } else {
        if (PREVHIGHLIGHTEL != null)
            PREVHIGHLIGHTEL.style.backgroundColor = ENTITYCOLOR;
    }
    PREVORANGEHIGHLIGHTEL = PREVHIGHLIGHTEL;
    PREVORANGEHIGHLIGHTCOLOR = PREVHIGHLIGHTCOLOR;

    selectcoref = document.getElementById("corefcolor");
    if (selectcoref != null) {
        for (var i = 0; i < selectcoref.options.length; i++) {
            if (selectcoref.options[i].value == id) {
                selectcoref.selectedIndex = i;
                selectcoref.options[i].selected = true;
                break;
            }
        }
    }

}

$('div').bind("contextmenu", function(e) {
    $("#entityFloating").css({ top: (e.pageY-2) + "px", left: (e.pageX-20) + "px" }).show(100);
    e.preventDefault();

});


$(document).ready(function() {
    try {
        $(document).bind("contextmenu", function(e) {
            var elid = 1;
            entityFloatingIsVisible=0;
            while(true) {
                el = document.getElementById(elid);
                if (el == null) {
                    break;
                } else {
                    if (el.style.backgroundColor == SELECTEDCOLOR) {
                        entityFloatingIsVisible=1;
                        break;
                    }
                }
                elid++;
            }
            if (entityFloatingIsVisible==1) {
                e.preventDefault();
                $("#entityFloating").css({ top: (e.pageY-4) + "px", left: (e.pageX-20) + "px" });
                el = document.getElementById("entityFloating");
                el.style.visibility="visible";
            }
        });
        $(document).mousedown(function(e) {
            var container = $("#entityFloating");
            if (container.has(e.target).length == 0) {
                el = document.getElementById("entityFloating");
                el.style.visibility="hidden";
                entityFloatingIsVisible = 0;
            }
        });
        /*$(document).mouseup(function(e) {
         var container = $("#entityFloating");
         if (container.has(e.target).length == 0) {
         el = document.getElementById("entityFloating");
         el.style.visibility="hidden";
         entityFloatingIsVisible = 0;
         }
         });*/
    } catch (err) {
        alert(err);
    }
});


function reloadPage (index, docid, entityid) {
    window.open("annotatenews.jsp?index="+index+"&docid="+docid+"&instance="+entityid, "_self");
}

