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

    String action = request.getParameter("action");
    if (action != null) {
        String index = request.getParameter("index");
        String docid = request.getParameter("docid");
        boolean checkDB = WebController.checkDBConnection();
        String user = null;
        if (checkDB) {
            HttpSession currentsession = request.getSession();
            user = (String) currentsession.getAttribute("user");
        }
        if (action.equals("showorig")) {
            String text = WebController.getOriginalDoc(index,docid);
            out.println(text);
        } else if (action.equals("xml")) {
            out.println(WebController.getXML(index,docid,user));
            //EXPORT
        } else if (action.equals("export")) {
            String export = request.getParameter("export");

            if (export != null) {
                String mode = request.getParameter("mode");
                String format = request.getParameter("format");
                if (export.equals("instance")) {
                    Hashtable<String, String> instances = WebController.listInstances(null);
                    if (format.equals("csv")) {
                        if (mode!=null && mode.length()==0)
                            mode =null;
                        // String type, id;
                        out.println("#ID\tTYPE\tCLASS\tINSTANCE_NAME\tDESCR\tEXT_LINK\tCOMMENT"+
                                "\tTIME\tBEGINTERVAL\tENDINTERVAL\tN.ANN_DOC.USER\tN.ANN_DOC.ALL\tHAS_PARTS");

                        for (String key : instances.keySet()) {
                            //type = key.replaceFirst("/[^/]+$","");
                            //id = key.replaceAll(".+/","");
                            String id = key.replaceAll(".+/","");

                            Hashtable<String, String> instanceInfo = WebController.getInstance(id);
                            if (instanceInfo != null) {
                                String line = id +"\t"+
                                        instanceInfo.get("type") +"\t"+
                                        instanceInfo.get("class")+"\t" +
                                        //instances.get(key).replaceFirst("^\\d+ ","")+"\t"+
                                        instanceInfo.get("name")+"\t"+
                                        instanceInfo.get("descr")+"\t"+
                                        instanceInfo.get("link")+"\t"+
                                        instanceInfo.get("comment").replaceAll("\n","<br>")+"\t";
                                if (instanceInfo.containsKey("time")) {
                                    line += instanceInfo.get("time");
                                }
                                line += "\t";
                                if (instanceInfo.containsKey("beginterval")) {
                                    line += instanceInfo.get("beginterval");
                                }
                                line += "\t";
                                if (instanceInfo.containsKey("endinterval")) {
                                    line += instanceInfo.get("endinterval");
                                }
                                line += "\t"+WebController.getNumAnnotatedDocs(id,user)+"\t"+
                                        WebController.getNumAnnotatedDocs(id,null)+"\t";
                                if (instanceInfo.containsKey("haspart")) {
                                    line += instanceInfo.get("haspart");
                                }

                                out.println(line);
                            }
                        }
                    } else if (format.equals("xml")) {
                        //Hashtable instanceInfo = WebController.getInstance(linkedInstances.get(tids));
                        out.println("<?xml version=\"1.0\" encoding=\"UTF8\"?>\n<xml>");
                        Format xmlformat = Format.getPrettyFormat();
                        xmlformat.setEncoding("UTF8");
                        XMLOutputter xml = new XMLOutputter(xmlformat);
                        Element elInstance, el;
                        for (String key : instances.keySet()) {
                            String id = key.replaceAll(".+/","");
                            Hashtable<String, String> instanceInfo = WebController.getInstance(id);

                            elInstance = new org.jdom.Element("instance");
                            elInstance.setAttribute("id", id);

                            elInstance.setAttribute("type", instanceInfo.get("type"));
                            elInstance.setAttribute("class", instanceInfo.get("class"));
                            elInstance.setAttribute("extlink", instanceInfo.get("link"));
                            elInstance.setAttribute("numAnnDocsByUser", String.valueOf(WebController.getNumAnnotatedDocs(id,user)));
                            elInstance.setAttribute("numAnnDocs", String.valueOf(WebController.getNumAnnotatedDocs(id,null)));
                            el = new org.jdom.Element("name");
                            //name.addContent(instances.get(key).replaceFirst("^\\d+ ",""));
                            el.addContent(instanceInfo.get("name"));
                            elInstance.addContent(el);
                            //description
                            if (instanceInfo.containsKey("descr") && !instanceInfo.get("descr").equals("")) {
                                el = new org.jdom.Element("descr");
                                el.addContent(instanceInfo.get("descr"));
                                elInstance.addContent(el);
                            }
                            //comment
                            if (instanceInfo.containsKey("comment") && !instanceInfo.get("comment").equals("")) {
                                el = new org.jdom.Element("comment");
                                el.addContent(instanceInfo.get("comment"));
                                elInstance.addContent(el);
                            }
                            if (instanceInfo.containsKey("time") && !instanceInfo.get("time").equals("")) {
                                el = new org.jdom.Element("time");
                                el.addContent(instanceInfo.get("time"));
                                elInstance.addContent(el);
                            }
                            //beginterval
                            if (instanceInfo.containsKey("beginterval") && !instanceInfo.get("beginterval").equals("")) {
                                el = new org.jdom.Element("beginterval");
                                el.addContent(instanceInfo.get("beginterval"));
                                elInstance.addContent(el);
                            }
                            //endinterval
                            if (instanceInfo.containsKey("endinterval") && !instanceInfo.get("endinterval").equals("")) {
                                el = new org.jdom.Element("endinterval");
                                el.addContent(instanceInfo.get("endinterval"));
                                elInstance.addContent(el);
                            }
                            //haspart
                            if (instanceInfo.containsKey("haspart")) {
                                el = new org.jdom.Element("has_participant");
                                String haspart = instanceInfo.get("haspart");
                                String[] entities = haspart.trim().split(" ");
                                for (String entid : entities) {
                                    String semrole = "NONE";
                                    String[] items =  entid.split("_");
                                    if (items.length > 1) {
                                        semrole = items[1];
                                        entid = items[0];
                                    }
                                    Hashtable<String, String> targetInstanceInfo = WebController.getInstance(entid);
                                    if (targetInstanceInfo != null) {
                                        Element target = new org.jdom.Element("target");
                                        target.setAttribute("semrole",semrole);
                                        target.setAttribute("id",entid);
                                        target.setAttribute("type",targetInstanceInfo.get("type"));
                                        target.setAttribute("class",targetInstanceInfo.get("class"));
                                        String descr = targetInstanceInfo.get("name");
                                        if (targetInstanceInfo.get("descr").trim().length() >0 ) {
                                            descr += " - " + targetInstanceInfo.get("descr");
                                        }
                                        target.addContent(descr);
                                        el.addContent(target);
                                    }
                                }
                                elInstance.addContent(el);
                            }


                            out.println(xml.outputString(elInstance));
                        }
                        out.println("</xml>");
                    }
                } else if (export.equals("document")) {
                    if (format.equals("xml")) {
                        Hashtable<String, String> repos = WebController.getReposToExport(user);
                        WebController.hash_counter_type.clear();
                        if (repos.size() > 0) {
                            String zipname = "export/cromerXML-"+user+".zip";
                            File fzip = new File(request.getRealPath(zipname));
                            fzip.delete();
                            ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream(fzip));
                            for (String repo : repos.keySet()) {
                                String[] docids = repos.get(repo).split(" ");
                                for (String did : docids) {
                                    try {
                                        String xml = WebController.getXML(repo, did, user);
                                        if (xml != null) {
                                            // name the file inside the zip  file
                                            String filename = WebController.getDocUrl(repo, did);
                                            if (filename == null) {
                                                filename = did +".xml";
                                            } else {
                                                if (!filename.endsWith(".xml")) {
                                                    filename += ".xml";
                                                }
                                            }
                                            zipout.putNextEntry(new ZipEntry(repo+"/"+ filename));
                                            //System.err.println(repo+"/"+ did+".xml");
                                            zipout.write(xml.getBytes());
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            }
                            zipout.close();
                            if (fzip.exists()) {
                                out.println("Go to "+request.getRequestURL().toString().replaceFirst("/pages/[^/]*$","/") +zipname+" to download the zip archive.");
                            }
                            //for (String types : WebController.hash_counter_type.keySet()) {
                            //    out.println(types +" = " + WebController.hash_counter_type.get(types));
                            //}
                        } else {
                            out.println("No annotation found!");
                        }
                    } else if (format.equals("csv")) {
                        out.println(WebController.exportAnnotations(user, mode, format));
                    }
                }
            }
        }
    }
%>
<%@ page contentType="text/raw; charset=UTF8" pageEncoding="UTF8"%>
<%@ page import="servlet.WebController"%>
<%@ page import="java.util.Hashtable"%>
<%@ page import="java.util.zip.ZipOutputStream"%>
<%@ page import="java.io.FileOutputStream"%>
<%@ page import="java.util.zip.ZipEntry"%>
<%@ page import="java.io.File"%>
<%@ page import="org.jdom.Element" %>
<%@ page import="org.jdom.output.Format" %>
<%@ page import="org.jdom.output.XMLOutputter" %>