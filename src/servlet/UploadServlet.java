package servlet;

import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;


public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    public static UploadArchives cgtarchives = null;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        ServletContext application = getServletConfig().getServletContext();

        // config initialization
        String uploadArchivesPath = application.getRealPath(servletConfig.getInitParameter("uploadPath"));

        try {
            cgtarchives = UploadArchives.getInstance(uploadArchivesPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     *
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String archivename = request.getSession().toString().replaceFirst(".*@","");
        String action = request.getParameter("action");

        PrintWriter writer = response.getWriter();
        response.setContentType("text/plain");
        if (action.equals("getcache")) {
            String[] urls = cgtarchives.getUrls(archivename);
            StringBuffer buffer = new StringBuffer();
            for (String url : urls) {
                buffer.append(url).append("<br>");
            }

            writer.write(buffer.toString());

        } else if (action.equals("removecache")) {
            if (cgtarchives.removeArchive(archivename)) {
                writer.write("success");
            } else {
                writer.write("fail");
            }
        }
        writer.close();
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     *
     */
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ServletFileUpload.isMultipartContent(request)) {
            throw new IllegalArgumentException("Request is not multipart, please 'multipart/form-data' enctype for your form.");
        }

        ServletFileUpload uploadHandler = new ServletFileUpload(new DiskFileItemFactory());
        PrintWriter writer = response.getWriter();
        response.setContentType("text/plain");
        try {
            List<FileItem> items = uploadHandler.parseRequest(request);
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    File file = File.createTempFile(item.getName(), "");
                    item.write(file);
                    writer.write("{\"name\":\""+ item.getName() + "\",\"type\":\"" + item.getContentType() + "\",\"size\":\"" + item.getSize() + "\",\"tmpname\":\"" + file.getName() + "\"}");

                    String archivename = request.getSession().toString().replaceFirst(".*@","");

                    DataInputStream in = new DataInputStream (new FileInputStream (file));
                    byte[] page = new byte[in.available()];
                    in.readFully (page);
                    in.close ();
                    //System.err.println("archivename " + Arrays.toString(cgtarchives.getArchiveNames()));

                    if (cgtarchives.addDocument(archivename, item.getName(), page)) {
                        file.delete();
                    }

                    break; // assume we only get one file at a time
                }
            }
        } catch (FileUploadException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            writer.close();
        }

    }

}
