package lk.ijse.dep8.library.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep8.library.dto.BookDTO;
import lk.ijse.dep8.library.exception.ValidationException;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "BookServlet", value = "/books/*")
public class BookServlet extends HttpServlet {
    @Resource(name = "java:comp/env/jdbc/pool4library")
    private volatile DataSource pool;

    private void doSaveOrUpdate(HttpServletRequest req, HttpServletResponse res) throws IOException{
        if (req.getContentType() == null ||
                !req.getContentType().toLowerCase().startsWith("application/json")) {
            res.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        String method = req.getMethod();
        String pathInfo = req.getPathInfo();

        if (method.equals("POST") &&
                !((req.getServletPath().equalsIgnoreCase("/books") ||
                        req.getServletPath().equalsIgnoreCase("/books/")))) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } else if (method.equals("PUT") && !(pathInfo != null &&
                pathInfo.substring(1).matches("[Vv]\\d{3}[/]?"))) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Book does not exist");
            return;
        }

        try {
            Jsonb jsonb = JsonbBuilder.create();
            BookDTO book = jsonb.fromJson(req.getReader(), BookDTO.class);
            if (method.equals("POST") &&
                    (book.getIsbn() == null || !book.getIsbn().matches("[Vv]\\d{3}"))) {
                throw new ValidationException("Invalid ISBN");
            } else if (book.getName() == null || !book.getName().matches("[A-Za-z ]+")) {
                throw new ValidationException("Invalid Name");
            } else if (book.getAuthor() == null || !book.getAuthor().matches("[A-Za-z ]+")) {
                throw new ValidationException("Invalid Author");
            }

            if (method.equals("PUT")) {
                book.setIsbn(pathInfo.replaceAll("[/]", ""));
            }

            try (Connection connection = pool.getConnection()) {
                PreparedStatement stm = connection.prepareStatement("SELECT * FROM book WHERE nic=?");
                stm.setString(1, book.getIsbn());
                ResultSet rst = stm.executeQuery();

                if (rst.next()) {
                    if (method.equals("POST")) {
                        res.sendError(HttpServletResponse.SC_CONFLICT, "Book already exists");
                    } else {
                        stm = connection.prepareStatement("UPDATE book SET name=?, author=? WHERE isbn=?");
                        stm.setString(1, book.getName());
                        stm.setString(2, book.getAuthor());
                        stm.setString(3, book.getIsbn());
                        if (stm.executeUpdate() != 1) {
                            throw new RuntimeException("Failed to update the book");
                        }
                        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                    }
                } else {
                    stm = connection.prepareStatement("INSERT INTO book (nic, name, contact) VALUES (?,?,?)");
                    stm.setString(1, book.getIsbn());
                    stm.setString(2, book.getName());
                    stm.setString(3, book.getAuthor());
                    if (stm.executeUpdate() != 1) {
                        throw new RuntimeException("Failed to register the book");
                    }
                    res.setStatus(HttpServletResponse.SC_CREATED);
                }
            }

        } catch (JsonbException | ValidationException e) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    (e instanceof JsonbException) ? "Invalid JSON" : e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doSaveOrUpdate(request,response);
    }
}
