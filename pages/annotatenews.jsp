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

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.*"%>
<%@ page import="servlet.WebController" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ include file="errors.lbl" %>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link rel="stylesheet" href="css/cromer.css">
    <link rel="stylesheet" href="js/alertify/alertify.core.css" />
    <link rel="stylesheet" href="js/alertify/alertify.default.css" />
    <script type="text/javascript" src="js/alertify/alertify.min.js"></script>
    <script type="text/javascript" src="js/jquery-2.0.3.min.js"></script>
    <script type="text/javascript" src="js/annotatenews.js"></script>

    <style>
        body {
            background: #666;
            line-height: 130%;
            overflow: hidden;
        }

        div {
            display:inline-block;
            background: #fff;
            padding-left: 0;
            padding-right: 0;
            border-left: 0;
            border-right: 0;
            border-top: 2px solid #fff;
            border-bottom: 2px solid #fff;
        }

        div.dash {
            padding: 0;
            border-bottom: 1px dashed #000;
        }

        #tabs {
            overflow: hidden;
            width: 100%;
            margin: 0;
            padding: 0;
            list-style: none;
        }

        #tabs li {
            cursor: pointer;
            float: left;
            margin: 0 .5em 0 0;
        }

        #tabs a {
            position: relative;
            background: #ddd;
            background-image: -webkit-gradient(linear, left top, left bottom, from(#fff), to(#ddd));
            background-image: -webkit-linear-gradient(top, #fff, #ddd);
            background-image: -moz-linear-gradient(top, #fff, #ddd);
            background-image: -ms-linear-gradient(top, #fff, #ddd);
            background-image: -o-linear-gradient(top, #fff, #ddd);
            background-image: linear-gradient(to bottom, #fff, #ddd);
            padding: 5px 5px;
            float: left;
            text-decoration: none;
            color: #444;
            text-shadow: 0 1px 0 rgba(255,255,255,.8);
            -webkit-border-radius: 5px 0 0 0;
            -moz-border-radius: 5px 0 0 0;
            border-radius: 5px 0 0 0;
            -moz-box-shadow: 0 2px 2px rgba(0,0,0,.4);
            -webkit-box-shadow: 0 2px 2px rgba(0,0,0,.4);
            box-shadow: 0 2px 2px rgba(0,0,0,.4);
        }

        #tabs a:hover,
        #tabs a:hover::after,
        #tabs a:focus,
        #tabs a:focus::after {
            background: #fff;
        }

        #tabs a:focus {
            outline: 0;
        }

        #tabs a::after {
            cursor: pointer;
            content:'';
            position:absolute;
            z-index: 1;
            top: 0;
            right: -.5em;
            bottom: 0;
            width: 12px;
            background: #ddd;
            background-image: -webkit-gradient(linear, left top, left bottom, from(#fff), to(#ddd));
            background-image: -webkit-linear-gradient(top, #fff, #ddd);
            background-image: -moz-linear-gradient(top, #fff, #ddd);
            background-image: -ms-linear-gradient(top, #fff, #ddd);
            background-image: -o-linear-gradient(top, #fff, #ddd);
            background-image: linear-gradient(to bottom, #fff, #ddd);
            -moz-box-shadow: 2px 2px 2px rgba(0,0,0,.4);
            -webkit-box-shadow: 2px 2px 2px rgba(0,0,0,.4);
            box-shadow: 2px 2px 2px rgba(0,0,0,.4);
            -webkit-transform: skew(10deg);
            -moz-transform: skew(10deg);
            -ms-transform: skew(10deg);
            -o-transform: skew(10deg);
            transform: skew(10deg);
            -webkit-border-radius: 0 5px 0 0;
            -moz-border-radius: 0 5px 0 0;
            border-radius: 0 5px 0 0;
        }

        #tabs #current a {
            background: #fff;
            z-index: 999;
        }

        #tabs #current a::after {
            background: #fff;
            z-index: 999;
        }

            /* ------------------------------------------------- */

        #content {
            background: #fff;
            padding: 2px;
            height: 220px;
            position: relative;
            z-index: 2;
            -moz-border-radius: 0 5px 5px 5px;
            -webkit-border-radius: 0 5px 5px 5px;
            border-radius: 0 5px 5px 5px;
            -moz-box-shadow: 0 -2px 3px -2px rgba(0, 0, 0, .5);
            -webkit-box-shadow: 0 -2px 3px -2px rgba(0, 0, 0, .5);
            box-shadow: 0 -2px 3px -2px rgba(0, 0, 0, .5);
        }

            /* ------------------------------------------------- */

        #about {
            color: #999;
        }

    </style>

</head>
<body onload="this.window.focus()">

<%
    Pattern entityNamePattern = Pattern.compile("([^/]+/[^/]+)//*(.+)");

    String docid = request.getParameter("docid");
    String query = request.getParameter("query");
    if(query == null)
        query = "";
    String updateType = request.getParameter("updatetype");
    String instanceid = request.getParameter("instance");
    if (instanceid == null) {
        instanceid = "";
    }
    String index = request.getParameter("index");
    HttpSession currentsession = request.getSession();
    String user = (String) currentsession.getAttribute("user");

    Hashtable<String, String> relations = new Hashtable<String,String>();
    Hashtable<String, String> entities = new Hashtable<String,String>();


    final String YELLOW="rgb(128,128,0)";
    final String RED="rgb(128,0,0)";
    final String PURPLE="rgb(128,0,128)";
    final String BLUE="rgb(0,0,128)";
    final String GREEN="rgb(0,128,0)";
    final String GRAY="rgb(65,65,65)";

    final HashMap<String, String> lighterColor = new HashMap<String, String>();
    lighterColor.put(YELLOW, "rgb(255,255,204)");
    lighterColor.put(RED, "rgb(255,204,204)");
    lighterColor.put(PURPLE, "rgb(216,191,216)");
    lighterColor.put(BLUE, "rgb(204,255,255)");
    lighterColor.put(GREEN, "rgb(204,255,229)");
    lighterColor.put(GRAY, "rgb(230,230,230)");

    final LinkedHashMap<String, String> hashEntityColor = new LinkedHashMap<String, String>();
    hashEntityColor.put("ENTITY",RED);
    hashEntityColor.put("ENTITY_MENTION",RED);
    hashEntityColor.put("ENTITY/PER", RED);
    hashEntityColor.put("ENTITY/LOC", RED);
    hashEntityColor.put("ENTITY/ORG", RED);
    hashEntityColor.put("ENTITY/PRO", RED);
    hashEntityColor.put("ENTITY/FIN", RED);

    hashEntityColor.put("EVENT",GREEN);
    hashEntityColor.put("EVENT/MIX", GREEN);
    hashEntityColor.put("EVENT_MENTION",GREEN);
    hashEntityColor.put("EVENT/SPEECH_COGNITIVE", GREEN);
    hashEntityColor.put("EVENT/GRAMMATICAL", GREEN);
    hashEntityColor.put("GRAMMATICAL", GREEN);
    hashEntityColor.put("EVENT/OTHER",GREEN);
    hashEntityColor.put("TIMEX3",BLUE);
    hashEntityColor.put("SIGNAL",YELLOW);
    hashEntityColor.put("C-SIGNAL",YELLOW);
    hashEntityColor.put("VALUE",PURPLE);

    hashEntityColor.put("ACTION",GRAY);
    hashEntityColor.put("ACTION_OCCURRENCE",YELLOW);
    hashEntityColor.put("ACTION_PERCEPTION",YELLOW);
    hashEntityColor.put("ACTION_REPORTING",YELLOW);
    hashEntityColor.put("ACTION_ASPECTUAL",YELLOW);
    hashEntityColor.put("ACTION_STATE",YELLOW);
    hashEntityColor.put("ACTION_CAUSATIVE",YELLOW);
    hashEntityColor.put("ACTION_GENERIC",YELLOW);
    hashEntityColor.put("NEG_ACTION_OCCURRENCE",YELLOW);
    hashEntityColor.put("NEG_ACTION_PERCEPTION",YELLOW);
    hashEntityColor.put("NEG_ACTION_REPORTING",YELLOW);
    hashEntityColor.put("NEG_ACTION_ASPECTUAL",YELLOW);
    hashEntityColor.put("NEG_ACTION_STATE",YELLOW);
    hashEntityColor.put("NEG_ACTION_CAUSATIVE",YELLOW);
    hashEntityColor.put("NEG_ACTION_GENERIC",YELLOW);
    hashEntityColor.put("HUMAN_PART_PER",RED);
    hashEntityColor.put("HUMAN_PART_ORG",RED);
    hashEntityColor.put("HUMAN_PART_GPE",RED);
    hashEntityColor.put("HUMAN_PART_FAC",RED);
    hashEntityColor.put("HUMAN_PART_VEH",RED);
    hashEntityColor.put("HUMAN_PART_MET",RED);
    hashEntityColor.put("HUMAN_PART_GENERIC",RED);
    hashEntityColor.put("CONCEPT_HUMAN_PART",RED);
    hashEntityColor.put("CONCEPT_HUMAN_PARTICIPANT",RED);
    hashEntityColor.put("NON_HUMAN_PART",PURPLE);
    hashEntityColor.put("NON_HUMAN_PART_GENERIC",PURPLE);
    hashEntityColor.put("LOC_GEO",BLUE);
    hashEntityColor.put("LOC_FAC",BLUE);
    hashEntityColor.put("LOC_OTHER",BLUE);
    hashEntityColor.put("TIME_DATE",GREEN);
    hashEntityColor.put("TIME_OF_THE_DAY",GREEN);
    hashEntityColor.put("TIME_DURATION",GREEN);
    hashEntityColor.put("TIME_REPETITION",GREEN);


    //String user = (String) currentsession.getAttribute("user");
    if (user == null) {
        out.println("<script>window.location = 'header.jsp';</script>");
    } else {

        if (instanceid == null) {
            //get last session instanceid value
            instanceid = (String) currentsession.getAttribute("lastinstanceid");
        } else {
            if (instanceid.length() > 0)
                currentsession.setAttribute("lastinstanceid", instanceid);
        }

        String instances = (String) currentsession.getAttribute("selectedinstance");
        //out.println(instanceid + " - " + docid + " :: " +instances);

        String optionInstance = "", otherOptionInstance = "";
        List<String> cartinstances = new ArrayList<String>();
        Matcher matcher;
        String className="", entityName;
        Hashtable<String, String> instanceValues;
        if (instances != null && instances.length() > 0) {
            String[] items = instances.split(",");
            Arrays.sort(items);

            for (String item : items) {
                //out.println("- " +item+"<br>");
                //optionInstance += "<option value='" + items[i] + "' selected=selected>" +items[i].replaceFirst(".*_","")+"</option>\n";
                if (item.length() > 0) {
                    instanceValues = WebController.getInstance(item);

                    if (instanceValues != null) {
                        cartinstances.add(item);

                        //matcher = entityNamePattern.matcher(item.replaceFirst("[^ ]+ ", ""));
                        //if (matcher.matches()) {

                        // if (!className.equals(matcher.group(1))) {
                        if (!className.equals(instanceValues.get("type")+"/"+instanceValues.get("class"))) {
                            //className = matcher.group(1);
                            className = instanceValues.get("type")+"/"+instanceValues.get("class");
                            optionInstance += "<option value='' disabled>- " + className + "</option>\n";
                        }
                    /*if (!subclassName.equals(matcher.group(2))) {
                        subclassName = matcher.group(2);
                        optionInstance +="<option value='' disabled>&nbsp;&nbsp;- "+subclassName+"</option>";
                    }*/
                        //entityName = matcher.group(2);
                        entityName = instanceValues.get("name");
                        optionInstance += "<option value='" + item + "'";
                        if (instanceid.length() > 0 && item.startsWith(instanceid)) {
                            optionInstance += " selected='selected'";
                        }
                        optionInstance += ">" + entityName ;
                        if (instanceValues.get("descr").length() > 0) {
                            optionInstance += " - " +instanceValues.get("descr");
                        }

                        optionInstance += "</option>\n";
                        //}
                    }

                }

            }
            if (optionInstance.length() > 0) {
                optionInstance ="<option value='' disabled>&nbsp;=== your cart instances ===</option>"+optionInstance;
            }
        }
        className="";
        List<String> annotatedInstanceKeys = null;
        boolean checkDB = WebController.checkDBConnection();

        if (checkDB)
            annotatedInstanceKeys = WebController.getAllInstanceTokenIDs2(index, docid, user);
        else
            out.println(MYSQL_CONNECTION_FAILED);

        //out.println(cartinstances);
        //out.println(annotatedInstanceKeys);
        if (annotatedInstanceKeys!= null) {
            for (String item : annotatedInstanceKeys) {
                if (!cartinstances.contains(item.replaceFirst(" .*", ""))) {
                    //out.println("<br><br><br>"+item+"<br>");
                    matcher = entityNamePattern.matcher(item.replaceFirst("[^ ]+ ", ""));
                    if (matcher.matches()) {
                        cartinstances.add(item.replaceFirst(" .*", ""));
                        if (!className.equals(matcher.group(1))) {
                            className = matcher.group(1);
                            otherOptionInstance += "<option value='' disabled>- " + className + "</option>\n";
                        }
                        entityName = matcher.group(2);
                        otherOptionInstance += "<option value='" + item.replaceFirst(" .*", "") + "'";
                        if (instanceid.length() > 0 && item.startsWith(instanceid)) {
                            otherOptionInstance += " selected='selected'";
                        }
                        otherOptionInstance += ">" + entityName + "</option>\n";
                    }
                }
            }
        }
        if (otherOptionInstance.length() > 0) {
            optionInstance +="<option value='' disabled>&nbsp;===== linked instances =====</option>"+otherOptionInstance;
        }

        //build the floating menu hashmap
        className="";
        if (cartinstances.size() > 0) {
            for (String entityID : cartinstances) {
                //out.println("<script>alert('" +item+"');</script>");
                //matcher = entityNamePattern.matcher(item.replaceFirst("[^ ]+ ", ""));
                //if (matcher.matches()) {
                instanceValues = WebController.getInstance(entityID);

                if (instanceValues != null) {
                    if (!className.equals(instanceValues.get("type") +"/" + instanceValues.get("class"))) {
                        className = instanceValues.get("type") +"/" + instanceValues.get("class");
                    }
                    if (!className.equals("")) {
                        entityName = "<b>"+instanceValues.get("name")+"</b>";
                        if (instanceValues.get("descr").length() > 0) {
                            entityName += " <small>- " +instanceValues.get("descr") + "</small>";
                        }
                        //String entityID = item.replaceFirst(" .*", "");
                        if (entities.containsKey(className)) {
                            if (!entities.get(className).contains(entityID+"\t")) {
                                entities.put(className,entities.get(className) + "\n" +
                                        entityID + "\t" + entityName
                                );
                            }
                        } else {
                            entities.put(className,entityID + "\t" + entityName);
                        }

                        //add has_participant
                        String haspart = (String) instanceValues.get("haspart");
                        if (haspart != null && haspart.trim().length() > 0) {
                            for (String targetInfo : haspart.trim().split(" ")) {
                                String semrole = "NONE";
                                String targetEntityID = targetInfo;
                                String[] items =  targetInfo.split("_");
                                if (items.length > 1) {
                                    semrole = items[1];
                                    targetEntityID = items[0];
                                }
                                Hashtable<String, String> instanceInfo = WebController.getInstance(targetEntityID);
                                String targetEntityName = "<b>" +instanceInfo.get("name") +"</b>";
                                if (instanceInfo.get("descr").length() > 0) {
                                    targetEntityName += " <small>- " +instanceInfo.get("descr") + "</small>";
                                }

                                if (relations.containsKey("HAS_PARTICIPANT")) {
                                    if (!relations.get("HAS_PARTICIPANT").contains(entityID+"\t"+targetEntityID+"\t"))
                                        relations.put("HAS_PARTICIPANT", relations.get("HAS_PARTICIPANT")+ "\n"+entityID+"\t"+semrole+"\t"+targetEntityID+"\t"+entityName + "\t<button>" +semrole + "</button> "+targetEntityName);
                                } else {
                                    relations.put("HAS_PARTICIPANT",entityID+"\t"+semrole+"\t"+targetEntityID+"\t"+entityName + "\t<button>" +semrole + "</button> "+targetEntityName);
                                }
                            }
                        }

                    }
                }
                //}
            }

        }

        String docurl = WebController.getDocUrl(index, docid);
        if (docurl == null) {
            docurl = docid;
        }


        String entitiesList = "";
        Map<String, String> tokenID2EntityID = new HashMap<String, String>();
        Map<String, String> tokenIDs2EntityID = null;
        if (checkDB)
            tokenIDs2EntityID = WebController.getAnnotatedTokensByInstances(index, docid, user);
        Map<String,String> CATannotation = WebController.getCATAnnotation(index, docid, user);

        Map<String,String> entityAnnotation = new HashMap<String,String>();
        entityAnnotation.putAll(CATannotation);
        //Map<String,String> entityAnnotation = WebController.getCATAnnotation(index, docid, user);
        for (String entMention : entityAnnotation.keySet()) {
            String mTokens = CATannotation.get(entMention);
            if (tokenIDs2EntityID.containsKey(mTokens)) {
                CATannotation.remove(entMention);
                String[] entIDs = tokenIDs2EntityID.get(mTokens).split(" ");
                for (String entID : entIDs) {
                    Hashtable<String, String> entInfo = WebController.getInstance(entID);
                    if (entInfo != null) {
                        entID = entInfo.get("type")+"/"+entInfo.get("class") + " "+ entInfo.get("name") + " "+entInfo.get("id");
                        /*if (entInfo.get("descr").length() > 0) {
                            entID += " - " +entInfo.get("descr");
                        }*/
                    }
                    if (!CATannotation.containsKey(entID)) {
                        CATannotation.put(entID, mTokens);
                    } else {
                        CATannotation.put(entID,CATannotation.get(entID) +","+mTokens);
                    }
                }
            }
        }

        //add the CROMER's annotations
        for(String key : tokenIDs2EntityID.keySet()) {
            String[] entIDs = tokenIDs2EntityID.get(key).split(" ");
            for (String entID : entIDs) {
                Hashtable<String, String> entInfo = WebController.getInstance(entID);
                if (entInfo != null) {
                    entID = entInfo.get("type")+"/"+entInfo.get("class") + " "+ entInfo.get("name") + " "+entInfo.get("id");
                                        /*if (entInfo.get("descr").length() > 0) {
                                            entID += " - " +entInfo.get("descr");
                                        }*/
                }
                if (!CATannotation.containsKey(entID)) {
                    CATannotation.put(entID, key);
                } else {
                    CATannotation.put(entID,CATannotation.get(entID) +","+key);
                }
            }
        }

        //if (CATannotation != null && CATannotation.size() > 0) {
        entitiesList = "<div style='width: 318px; margin: 0px; padding: 0px; font-size: 13px; top: 15px; right: 1px; position: fixed; border-top: 2px solid #666; background: #666'><ul id=\"tabs\">" +
                "<li><a onclick=\"javascript:setCoreference();\" id=menutab1 name=\"tab1\"><i>Mentions</i> and&nbsp;<br>&#735;<u>chains</u></a></li>" +
                "<li><a onclick=\"javascript:setTlink();\" name=\"tab2\"><i>Relations <br>from CAT</i>&nbsp;</a></li>" +
                "<li><a onclick=\"javascript:setRelation();\" id=menutab3 name=\"tab3\"><i>Relation <br>occurrences</i>&nbsp;</a></li>" +
                "</ul></div>";
        entitiesList +="<div id=\"content\"> <div class=\"tab1\" id=tab1>";
        List<String> keys = new LinkedList<String>(CATannotation.keySet());
        Collections.sort(keys);
        String labelEntType = "", currentLabel="";
        String BGLABELCOLOR ="";
        String keynorm;
        for (String key : keys) {
            //out.println("<pre>"+key+"</pre>");
            if (!CATannotation.get(key).equals("null")) {
                Hashtable<String, String> entInfo = WebController.getInstance(key);
                String instID = null;
                if (entInfo != null) {
                    instID = entInfo.get("id");
                    currentLabel = entInfo.get("type")+"/"+entInfo.get("class");
                    keynorm = "&#9656; "+currentLabel + ": " + entInfo.get("name");
                    if (entInfo.get("descr").length() > 0) {
                        keynorm += " - " +entInfo.get("descr");
                    }
                } else {
                    keynorm = "&#9656; "+key.replaceFirst("#m[^#]*$","").replaceAll("</*[^>]+>","").replaceFirst(" ",": ");
                    currentLabel = key.replaceFirst(" .*","");
                }
                if (!currentLabel.equals(labelEntType)) {
                    labelEntType = currentLabel;
                    BGLABELCOLOR = hashEntityColor.get(currentLabel);
                    if (BGLABELCOLOR == null)
                        BGLABELCOLOR="#000";
                    entitiesList +="\n<span onclick=\"javascript:deselectAllTokens();\" style='background: "+BGLABELCOLOR+"; border-top: 2px solid #fff; display: inline-block; width: 310px; color: #fff; padding: 0px; margin: 0px;'><center>"+currentLabel+"</center></span>";
                }
                String color = lighterColor.get(hashEntityColor.get(currentLabel));
                String tokids = CATannotation.get(key);
                String[] tokitems = tokids.split("[,| ]");
                for (String tid : tokitems) {
                    if (!tokenID2EntityID.containsKey(tid)) {
                        tokenID2EntityID.put(tid,keynorm);
                    } else if (!tokenID2EntityID.get(tid).contains(keynorm)) {
                        tokenID2EntityID.put(tid, tokenID2EntityID.get(tid) + "\n"+keynorm);
                    }
                }
                String updateInstanceID = "";
                if (instID == null && tokenIDs2EntityID != null && tokids != null) {
                    instID = tokenIDs2EntityID.get(tokids.replaceAll(",.*",""));
                } else {
                    if (instID.equals(instanceid)) {
                        updateInstanceID = "id='updatedInstanceEL'";
                    }
                }

                entitiesList += "<span "+updateInstanceID+" style='width: 100%; display: block; margin-left: 0px; border-top: 1px solid #999; cursor: pointer; background: "+color+"' onmouseout='OVERMENU=0;' " +
                        "onmouseover=\"javascript:highlightTokens2('"+key+"','"+tokids+"','"+currentLabel+"',this,'"+BGLABELCOLOR+"','"+color+"');\" " +
                        "onclick=\"javascript:highlightTokens('"+tokids+"','"+currentLabel+"',this);\">";
                if (entInfo != null) {
                    entitiesList +="&nbsp;<a onclick=\"OVERMENU=1; select('instance','"+instID+"','"+tokids+"');\"><img style='border: 1px solid #888' width=14 src=\"css/images/link-icon.gif\" title='linked mentions'></a>";
                    entitiesList +="<a onclick=\"OVERMENU=1; showInstanceCard('"+instID+"');\"><img width=16 src=\"css/images/zoom.png\" title='show instance information'>";
                    if (entInfo.get("haspart") != null) {
                        entitiesList +="<img width=7px src=\"css/images/haspart2.png\" title='instance with participants'>";
                    }
                    entitiesList +="</a> <b>"+entInfo.get("name")+"</b>";


                    if (entInfo.get("descr").length() > 0) {
                        entitiesList += "<small> - " +entInfo.get("descr") + " </small>";
                    }

                } else
                    entitiesList += "<b>"+key.replaceFirst("^[^ ]+ ","").replaceFirst("#[^#]+$","")+"</b>";
                entitiesList += "</span>\n";

                //SHOW RELATIONS

                //SHOW RELATIONS OCCURRENCES
                /*if (entInfo != null) {
                    String haspart = entInfo.get("haspart");
                    if (haspart != null) {
                        String semrole;
                        String[] ents = haspart.trim().split(" ");
                        for (String entid : ents) {
                            String[] items =  entid.split("_");
                            if (items.length > 1) {
                                semrole = items[1];
                                entid = items[0];
                            } else {
                                semrole = "NONE";
                            }
                            Hashtable<String,String> instanceInfo = WebController.getInstance(entid);
                            if (instanceInfo != null) {
                                entitiesList +="<div style='font-size: 13px; display: block; padding: 0; margin:0; width: 100%'>&nbsp;" +
                                        "+ <a onclick=\"showInstanceCard('"+entid+"');\"><img width=16 src=\"css/images/zoom.png\"></a>" +
                                        "<font color=#b77405>"+instanceInfo.get("type")+"/"+instanceInfo.get("class") +"</font>: "+instanceInfo.get("name");

                                if (instanceInfo.get("descr").length() > 0) {
                                    entitiesList +=" - " +instanceInfo.get("descr");
                                }
                                entitiesList += " <u>["+semrole+"]</u></div>";
                            }
                        }

                    }

                }
                 */

            }
        }

            /*for (String item : tokenIDs2EntityID.keySet()) {
                entitiesList+="<br>"+item + " -- " +tokenIDs2EntityID.get(item);
            }*/
        entitiesList +="</div>\n<div class=\"tab1\" id=tab2>\n";

        Map<String,String> HasPartRel = WebController.getHasParticipant(index, docid, user);
        if(HasPartRel.size() > 0) {
            entitiesList +="<div onclick=\"javascript:deselectAllTokens();\" style='background: rgb(0,0,150); width: 310; overflow: none; color: #fff; padding: 0px; margin: 0px'><center>HAS_PARTICIPANT</center></div><br>\n";

            List<String> sortedKeys=new ArrayList<String>(HasPartRel.keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys) {
                entitiesList += "<div style='width: 100%; display: block; margin-left: 0px; border-top: 1px solid #999; cursor: pointer; background: #fff' onmouseout=\"OVERMENU=0;\" onmouseover=\"javascript:highlightTokens2('"+key+"','"+key+"','"+currentLabel+"',this,'#444','#fff');\" onclick=\"javascript:highlightTokens('"+key+"','"+key+"',this);\"><img src=\"css/images/haspart2.png\"> "+HasPartRel.get(key)+"</div>";
            }
        }


        //TODO: mostrare solo i tlink
        //TLINK
        Map<String,String> TLink = WebController.getCATRelations(index, docid, user);
        if(TLink.size() > 0) {
            entitiesList += "<div onclick=\"javascript:deselectAllTokens();\" style='background: rgb(0,0,0); width: 310; overflow: none; color: #fff; padding: 0px; margin: 0px'><center>TLINK</center></div><br>\n";

            List<String> sortedKeys=new ArrayList<String>(TLink.keySet());
            Collections.sort(sortedKeys);
            for (String key : sortedKeys) {
                entitiesList += "<div style='width: 100%; display: block; margin-left: 0px; border-top: 1px solid #999; cursor: pointer; background: #fff' onmouseout=\"OVERMENU=0;\" onmouseover=\"javascript:highlightTokens2('"+key+"','"+key+"','"+currentLabel+"',this,'#444','#fff');\" onclick=\"javascript:highlightTokens('"+key+"','"+TLink.get(key)+"',this);\"><li>"+TLink.get(key)+"</li></div>";
            }
        }
        entitiesList +="</div>\n<div class=\"tab1\" id=tab3>\n";

        //Relation occurrences
        List<String> haspart = WebController.getListAnnotatedRelations(index, docid, user);
        //out.println("<script>alert(\""+index+","+ docid+","+ user+","+haspart.size()+"\");</script>");
        if(haspart.size() > 0) {
            entitiesList +="<div onclick=\"javascript:deselectAllTokens();\" style='background: rgb(0,0,150); width: 310; overflow: none; color: #fff; padding: 0px; margin: 0px'><center>HAS_PARTICIPANT</center></div><br>\n";

            Collections.sort(haspart);
            for (String relation : haspart) {
                String[] item = relation.split("\t");
                Hashtable<String, String> sourceEntInfo = WebController.getInstance(item[2]);
                String sourceInstance = "<b>"+sourceEntInfo.get("name")+"</b>";
                if (sourceEntInfo.get("descr").length() > 0) {
                    sourceInstance += " <small>- "+ sourceEntInfo.get("descr") + "</small>";
                }
                Hashtable<String, String> targetEntInfo = WebController.getInstance(item[4]);
                String targetInstance = "<b>"+targetEntInfo.get("name")+"</b>";
                if (targetEntInfo.get("descr").length() > 0) {
                    targetInstance += " <small>- "+ targetEntInfo.get("descr")+ "</small>";
                }
                if (sourceEntInfo!=null && targetEntInfo!=null) {
                    entitiesList += "<div style='width: 100%; display: block; margin-left: 0px; border-top: 1px solid #999; cursor: pointer; background: cyano; display: block' onmouseout=\"OVERMENU=0;\" " +
                            "onmouseover=\"javascript:highlightTokens2('"+item[3]+"','"+item[3]+"','"+currentLabel+"',this,'#444','#fff');\" " +
                            "onclick=\"javascript:highlightTokens('"+item[3]+"','"+item[3]+"',this);\">" +
                            //TODO
                            "<a href=\"javascript:select('relation','"+item[0]+"','"+item[3]+"');\"><img style='border: 1px solid #888' width=14 src=\"css/images/link-icon.gif\" title='linked mentions'></a>&nbsp;"+
                            sourceInstance + " <button>"+item[6]+"</button> " +targetInstance+"</div>";
                }
            }
        }
        entitiesList +=" </div></div>";

        //out.println(WebController.getDivTokens(index, docid, query).replaceAll("<div ","<div style='padding-left: 1px; padding-right: 0px' "));
        //out.flush();

/*<div id=1>Trento</div><div id=1-2><br></div><br>
<div id=2>Da</div><div id=2-3>&nbsp;</div><div id=3>Wikipedia</div><div id=3-4></div><div id=4>,</div><div id=4-5>&nbsp;</div><div id=5>l'</div><div id=5-6></div><div id=6>enciclopedia</div><div id=6-7>&nbsp;</div><div id=7>libera</div><div id=7-8></div><div id=8>.</div><div id=8-9><br></div><br>
<div id=8-9><br></div><br>*/
        // }
%>

<!-- <a href="annotatenews.jsp?index=<%=index%>&docid=<%=docid%>&query=">REFRESH</a>   -->
<div style="position: fixed; top: 0; background: #ededed; padding: 2px; left: 0; border-radius: 15px 20px 0 0;">
    <img src='css/images/dimage.gif' title='<%= docid %>'> <b><%= index+"/"+docurl %></b>
    <form method="GET" action="annotatenews.jsp">
        <input type="hidden" name="index" value="<%= index %>">
        <input type="hidden" name="docid" value="<%= docid %>">
        <input type="hidden" name="updatetype" value="instance">
        <input type="hidden" name="query" value="">
        Show the annotations for: <select id="corefcolor" name="instance" onChange="submit()"><option value="">
            <%= optionInstance %>
    </select> <a href="javascript:reloadPage('<%=index%>','<%=docid%>','<%=instanceid%>')"><img src='css/images/refresh_icon.png' title='refresh cart'></a>
        <input type="button" onclick="javascript:refresh();" value="refresh selection">
        <br>&nbsp;&nbsp;&nbsp;<a class="LinkButton" href='view.jsp?index=<%= index %>&docid=<%= docid %>&action=showorig' target='cview'><img src='css/images/view.png' width=13px title='Show original input'></a>
        <a class="LinkButton" href='view.jsp?index=<%= index %>&docid=<%= docid %>&action=xml' target='cview'><img src='css/images/thunder.png' width=13px title='Show CROMER XML export'></a>
        <a class="LinkButton" href=" javascript:addInstances('<%= index %>','<%= docid %>');"><img src='css/images/add_instance2.png' width=13px title='Shortcut instance creation'> Create instances with shortcut</a>

    </form>


</div>

<div style="background: #666; position: relative; padding: 0; margin-top: 60px; float: left; width: 90%;">
    <div style="margin-right: 200px; margin-left:-12px; overflow-y: auto; height: 95%; border-top: solid 1px #000; border-bottom: solid 1px #555; background: #fff;">
        <!-- <button style="margin-right: 20px; font-size: 10px; margin-top: 5px; float: right" onclick="alert(ENTITYID+' '+getSelectedTokenIDs());">getSelection</button> -->
        <%
            out.println("<ol start=\"1\"><div style=\"line-height:1em;\"><li>");
            for (String div : WebController.getDivTokens(index, docid, query)) {
                if (div.equals("<br>")) {
                    out.println("\n</li></div><br><div style=\"line-height:1em;\"><li>");
                } else {
                    String getMarkableID = tokenID2EntityID.get(div.replaceFirst(".*<div id='","").replaceFirst("' .*$",""));
                    //out.println("<pre>"+ div + "</pre><br>");
                    if (getMarkableID != null) {
                        div = "<div class=dash title=\""+getMarkableID+"\" href=\"#\">"+div+"</div>";
                    }
                    out.print(div);

                }
            }
            out.println("</li></div></ol><p>&nbsp;<p>");
        %>
    </div>
</div>
<div style="float: left; width: 200px;">
    <%= entitiesList %>
</div>

<%
    }
%>

<div style="top: 5px; right: 350px; border: 0; display: block; position: fixed; font-size: 14px" id="log"></div>
<script type="text/javascript" charset="utf-8">
    var ENTITYID = "<%= instanceid %>";
    var INDEX = "<%= index %>";
    var DOCID = "<%= docid %>";

    $(document).ready(function() {
        $("#content").find("[id^='tab']").hide(); // Hide all content
        <%
            if (updateType != null && updateType.equals("relation")) {
        %>
        $("#tabs li:last").attr("id","current"); // Activate the last tab
        $("#content #tab3").fadeIn();
        <% } else { %>
        $("#tabs li:first").attr("id","current"); // Activate the first tab
        $("#content #tab1").fadeIn();
        <%
            }
        %>
        //$("#content #tab1").fadeIn(); // Show first tab's content

        $('#tabs a').click(function(e) {
            e.preventDefault();
            if ($(this).closest("li").attr("id") == "current"){ //detection for current tab
                return;
            }
            else{
                $("#content").find("[id^='tab']").hide(); // Hide all content
                $("#tabs li").attr("id",""); //Reset id's
                $(this).parent().attr("id","current"); // Activate this
                $('#' + $(this).attr('name')).fadeIn(); // Show content for the current tab
            }
        });
    });

    function showInstanceCard (id) {
        $.ajax({
            type: "GET",
            url: "instance_card.jsp",
            data: "id="+id+"&action=show", // appears as $_GET['id'] @ ur backend side
            success: function(html, e) {
                // data is ur summary
                $('#semroleform').html(html);

                el = document.getElementById("semrolepane");
                if (el != null) {
                    el.style.visibility='visible';
                }
            }
        });
    }

    function addInstances(index,docid,instanceid) {
        $.ajax({
            type: "GET",
            url: "add_instances.jsp",
            data: "index="+index+"&docid="+docid+"&instanceid="+instanceid, // appears as $_GET['id'] @ ur backend side
            async: true,
            cache:false,
            success: function(html, e) {
                // data is ur summary
                $('#semroleform').html(html);

                el = document.getElementById("semrolepane");
                if (el != null) {
                    el.style.visibility='visible';
                }
            }
        });

        return false;
    }
</script>


<div id="entityFloating" class=entityFloating onclick="this.style.visibility='hidden';">
    <table border=0 cellspacing=0 cellpadding=2 bgcolor=#efefef>
        <%
            StringBuilder listents = new StringBuilder();
            String className = "";
            if (entities.size() > 0) {
                //instances
                List<String> keys = new LinkedList<String>(entities.keySet());
                Collections.sort(keys);

                for (String key : keys) {
                    if (!key.equals(className)) {
                        className=key;
                        String BGLABELCOLOR = (String) hashEntityColor.get(className);
                        if (BGLABELCOLOR != null) {
                            listents.append("<tr><td style='border-bottom: solid #444 1px;background:" + BGLABELCOLOR + "; color:#eee;'>" + className + "</td></tr>\n");
                        } else {
                            className = "";
                        }
                    }
                    if (!className.equals("")) {
                        String[] items =  entities.get(key).split("\n");
                        for (String entity : items) {
                            String[] entval = entity.split("\t");
                            if (entval.length == 2)
                                listents.append("<tr><td style='border-bottom: solid #444 1px; font-size:16px' align=left onmouseover='this.style.backgroundColor=\"peachpuff\";' onmouseout='this.style.backgroundColor=\"#efefef\";' " +
                                        "onClick=\"javascript:saveAnnotation('" + entval[0] + "','"+className+"')\">" + entval[1] +"</td></tr>\n");
                        }
                    }

                }

                //relations
                if (relations.size() > 0) {
                    for (String key : relations.keySet()) {
                        String[] items =  relations.get(key).split("\n");
                        if (items.length > 0) {
                            listents.append("<tr><td style='border-bottom: solid #444 1px;background: rgb(0,0,150); color:#eee;'>"+key+"</td></tr>\n");

                            for (String relation : items) {
                                String[] entval = relation.split("\t");
                                listents.append("<tr><td style='border-bottom: solid #444 1px; font-size:16px' align=left onmouseover='this.style.backgroundColor=\"peachpuff\";' onmouseout='this.style.backgroundColor=\"#efefef\";' " +
                                        "onClick=\"javascript:saveRelation('" + entval[0] + "','" + entval[1] + "','" + entval[2] + "','"+key+"')\"><li>" + entval[3] + " " +entval[4] +"</td></tr>\n");
                            }
                        }
                    }
                }
            } else {
                listents.append("<tr><td>&nbsp;<i>No instances&nbsp;</i></td></tr>");
            }
            out.println(listents.toString());
        %>
    </table>
</div>

<div id=semrolepane class=semrolepane>
    <div id=semroleform class=semroleform>
    </div>
</div>

<%
    out.flush();
    if (instanceid != null) {
        String tokenids = "";
        if (updateType.equals("instance"))
            tokenids = WebController.getInstanceTokenIDs(instanceid, index, docid, user);
        else
            tokenids = WebController.getRelationTokenIDs(instanceid, user);
        out.println("<script>select('"+updateType+"','" +instanceid+"','"+tokenids +"');\n" +
                "    var el = document.getElementById(\"updatedInstanceEL\");\n" +
                "    if (el != null) {\n" +
                "       PREVORANGEHIGHLIGHTEL = el;\n" +
                "       PREVORANGEHIGHLIGHTCOLOR = el.style.backgroundColor;\n" +
                "       el.style.backgroundColor = ENTITYCOLOR;\n" +
                "   }" +
                "</script>");
    }
%>


</body>
</html>