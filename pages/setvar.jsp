<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.HashMap" %>
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

<%
    final String[] menu = {"CROMER!", "Repositories", "Documents", "Instances", "Utilities", "Info", "Logout"};
    final String[] menulink = {"index.jsp", "repositories.jsp", "documents.jsp", "instances.jsp", "utilities.jsp", "info.jsp", "index.jsp?logout=yes"};

    //final String[] entityClasses = {"PERSON", "LOCATION", "ORGANIZATION", "PRODUCT", "FINANCIAL"};
    final String[] entityClasses = {"PER", "LOC", "ORG", "PRO", "FIN"};
    final String[] eventClasses = {"SPEECH_COGNITIVE", "GRAMMATICAL", "OTHER"};
    final String[] actionClasses = {"ACTION_OCCURRENCE","ACTION_PERCEPTION","ACTION_REPORTING","ACTION_ASPECTUAL","ACTION_STATE","ACTION_CAUSATIVE","ACTION_GENERIC","NEG_ACTION_OCCURRENCE","NEG_ACTION_PERCEPTION","NEG_ACTION_REPORTING","NEG_ACTION_ASPECTUAL","NEG_ACTION_STATE","NEG_ACTION_CAUSATIVE","NEG_ACTION_GENERIC"};
    final String[] humanpartClasses = {"HUMAN_PART_PER","HUMAN_PART_ORG","HUMAN_PART_GPE","HUMAN_PART_FAC","HUMAN_PART_VEH","HUMAN_PART_MET","HUMAN_PART_GENERIC","CONCEPT_HUMAN_PART"};
    final String[] nonhumanpartClasses = {"NON_HUMAN_PART","NON_HUMAN_PART_GENERIC"};
    final String[] locClasses = {"LOC_GEO","LOC_FAC","LOC_OTHER"};
    final String[] timeClasses = {"TIME_DATE","TIME_OF_THE_DAY","TIME_DURATION","TIME_REPETITION"};

    final LinkedHashMap<String,String[]> markables = new LinkedHashMap<String,String[]>();
    markables.put("ENTITY", entityClasses);
    markables.put("EVENT", eventClasses);
    markables.put("ECBp-ACTION", actionClasses);
    markables.put("ECBp-HUMAN-PART", humanpartClasses);
    markables.put("ECBp-NON-HUMAN-PART", nonhumanpartClasses);
    markables.put("ECBp-LOC", locClasses);
    markables.put("ECBp-TIME", timeClasses);

    HashMap<String, String> users = null;

%>
