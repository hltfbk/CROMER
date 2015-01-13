<%@ page import="java.io.*" %>
<%@ include file="setvar.jsp" %>

<html>
<head>
    <title>CROMER - A tool for CROss-document Main Events and Entities Recognition!</title>
    <meta http-equiv="description" content="" />
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <link rel="stylesheet" href="css/boxes.css" type="text/css" />
    <script type="text/javascript" src="js/jquery.js"></script>
    <script type="text/javascript" src="js/jquery-ui.min.js"></script>
    <link rel="stylesheet" type="text/css" href="js/jquery-ui-multiselect-widget/assets/style.css" />
</head>
<body>

<%
    File userObjFile = new File(request.getRealPath("/users.ser"));
    if (users == null) {
        //load credential from file
        if (!userObjFile.exists()) {
            userObjFile.createNewFile();
        }
        if (userObjFile.length() > 0) {
            FileInputStream fstream = new FileInputStream(userObjFile);
            try {
                ObjectInputStream ostream = new ObjectInputStream(fstream);
                while (true) {
                    try {
                        users = (HashMap) ostream.readObject();
                    } catch (EOFException e) {
                        break;
                    }
                    // do something with obj
                }
            } finally {
                fstream.close();
            }
        } else {
            users = new HashMap<String,String>();
            users.put("admin","admroot");

            OutputStream fostream = new FileOutputStream(userObjFile);
            try {

                OutputStream buffer = new BufferedOutputStream(fostream);
                ObjectOutput output = new ObjectOutputStream(buffer);

                output.writeObject(users);
                output.close();
            }
            catch(IOException ex){
                out.println("Cannot perform load user file.");
            } finally {
                fostream.close();
            }

        }
    }

    String pagename = request.getRequestURL().toString();
    if (pagename == null) {
        pagename = "index.jsp";
    }
    String logout = request.getParameter("logout");
    HttpSession currentsession = request.getSession();

    if (logout != null) {
        currentsession.setAttribute("user",null);
        currentsession.setAttribute("selectedinstance",null);
        currentsession.setAttribute("lastinstanceid",null);
        currentsession.setAttribute("selectedrepository",null);
        currentsession.setAttribute("selectedquery",null);
        currentsession.setAttribute("selectedaccordion",null);
    }
    String user = (String) currentsession.getAttribute("user");
    if (user == null) {
        String login = request.getParameter("login");
        String password = request.getParameter("password");
        if (login != null && login.trim().length() > 0 && password != null && password.trim().length() >0) {
            if (users.get(login) != null && users.get(login).equals(password)) {
                currentsession.setAttribute("user",login);
                pageContext.include("header.jsp");
                return;

            } else {
                out.println("<font color=red>ERROR!</font> The login or password is wrong. Sign in again, please!");
                /*OutputStream fostream = new FileOutputStream(userObjFile);
                try {

                    OutputStream buffer = new BufferedOutputStream(fostream);
                    ObjectOutput output = new ObjectOutputStream(buffer);
                    users.put(login,password);
                    output.writeObject(users);
                    output.close();
                }
                catch(IOException ex){
                    out.println("Cannot perform load user file.");
                } finally {
                    fostream.close();
                }
                */
            }
        }
%>
<br>

<div style='margin: 0px; padding: 9px; background: #efefef;'>Welcome to <b>CROMER</b>, a tool for <u><b>CRO</b></u>ss-document <u><b>M</b></u>ain <u><b>E</b></u>vents and <u><b>E</b></u>ntities <u><b>R</b></u>ecognition!<br>
    <form method="post" name="form" action="index.jsp">
        <br> &nbsp;&nbsp;User: <input type="text" name="login" maxlength=30 size=10 value="">
        &nbsp;&nbsp;Password: <input type="password" name="password" maxlength=30 size=10>
        &nbsp;&nbsp;<input type=submit value="Login">
    </form>  </div>
<%
    } else {
        out.println("<img width=14 src='css/images/boxes-bw1.png' style=\"margin-top: -10px; margin-left:-10px; margin-right: 6px\">\n<div class='box' id='menubox'>");
        for (int p=0; p<menu.length; p++) {
            if (pagename.endsWith(menulink[p])) {
                out.println("<a href='#' style='border-bottom: 2px solid #efefef;'><div style='margin: 0px; padding: 9px; background: #efefef; color=#f6a828; border-top:7px solid #444; border-right: 1px solid #444; border-left:1px solid #444; cursor: default'><font color=#1c94c4>"+ menu[p] +"</font></div></a>");
            } else {
                out.println("<a href='" + menulink[p] +"'><span>"+ menu[p]);
                if (menu[p].equals("Logout")) {
                    out.println(": "+ user);
                }
                out.println("</span></a>");
            }
        }
        out.println("<img width=14 src='css/images/boxes-bw1.png'><img width=22 src='css/images/corner-brown.png'></div>");
        if (pagename.endsWith("info.jsp")) {
            out.println("<br><div class=search>A tool for <u><b>CRO</b></u>ss-document <u><b>M</b></u>ain <u><b>E</b></u>vents and <u><b>E</b></u>ntities <u><b>R</b></u>ecognition!</div>");
        }
    }
%>

</body>
</html>
