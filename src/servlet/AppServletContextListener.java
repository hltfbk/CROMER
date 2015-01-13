package servlet;

import javax.servlet.*;
import java.io.IOException;
import java.util.Enumeration;

/**
 * Created by IntelliJ IDEA.
 * User: cgirardi
 * Date: 30-may-2013
 * Time: 9.13.38
 */

public class AppServletContextListener implements ServletContextListener {

    public void contextInitialized(ServletContextEvent sce) {

        ServletContext c = sce.getServletContext();
        if (c != null) {
            Enumeration enumer = c.getInitParameterNames();
            while (enumer.hasMoreElements()) {
                System.out.println("AppServletContextListener: indexes and archives " + enumer.nextElement());
            }
        }

        
    }


    public void contextDestroyed(ServletContextEvent sce) {
        //System.out.println("ServletContextListener destroyed");
    }

}
