package lk.ijse.dep8.library.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep8.library.dto.BookDTO;
import lk.ijse.dep8.library.exception.ValidationException;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.awt.print.Book;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@MultipartConfig(location = "/tmp", maxFileSize = 15 * 1024 * 1024)
@WebServlet(name = "BookServlet2", value = "/v2/books/*")
public class BookServlet2 extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doSaveOrUpdate(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doSaveOrUpdate(request,response);
    }

    private void doSaveOrUpdate(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("multipart/form-data")){
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }


        String method = request.getMethod();
        String pathInfo = request.getPathInfo();

        if (method.equals("POST") &&
                !((request.getRequestURI().equalsIgnoreCase("/books") ||
                        request.getRequestURI().equalsIgnoreCase("/books/")))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } else if (method.equals("PUT") && !(pathInfo != null &&
                pathInfo.substring(1).matches("\\d+[/]?"))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Book does not exist");
            return;
        }

        try{
            String isbn = request.getParameter("isbn");
            String name = request.getParameter("name");
            String author = request.getParameter("author");
            Part preview = request.getPart("preview");

            BookDTO book;
            if (preview != null && preview.getSubmittedFileName().isEmpty()){
                if (!preview.getContentType().toLowerCase().startsWith("image/")){
                    throw new ValidationException("Invalid Preview");
                }
                byte[] buffer = new byte[(int) preview.getSize()];
                preview.getInputStream().read(buffer);
                book = new BookDTO(isbn, name, author, buffer);
            } else{
                book = new BookDTO(isbn, name, author);
            }

            if (method.equals("POST") && (book.getIsbn() == null || !book.getIsbn().matches("\\d+"))){
                throw new ValidationException("Invalid isbn");
            } else if (book.getName() == null || !book.getName().matches(".+")){
                throw new ValidationException("Invalid Book name");
            } else if (book.getAuthor() == null || !book.getAuthor().matches("[A-Za-z0-9 ]")){
                throw new ValidationException("Invalid Author name");
            }

        } catch (ValidationException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }


    }

}
