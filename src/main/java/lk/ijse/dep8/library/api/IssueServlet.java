package lk.ijse.dep8.library.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep8.library.dto.IssueDTO;
import lk.ijse.dep8.library.exception.ValidationException;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;

@WebServlet(name = "IssueServlet", value = {"/issues", "/issues/"})
public class IssueServlet extends HttpServlet {
    @Resource(name = "java:comp/env/jdbc/pool4library")
    private DataSource pool;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("application/json")) {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        String method = request.getMethod();
        String pathInfo = request.getPathInfo();

        if (request.getRequestURI().equalsIgnoreCase("/issues") || request.getRequestURI().equalsIgnoreCase("/issues/")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try (Connection connection = pool.getConnection()) {
            Jsonb jsonb = JsonbBuilder.create();
            IssueDTO issue = jsonb.fromJson(request.getReader(), IssueDTO.class);

            String nic = issue.getNic();
            String isbn = issue.getIsbn();


            boolean isNicExists = false;
            boolean isIsbnExists = false;


            PreparedStatement stm = connection.prepareStatement("SELECT * FROM member WHERE nic=?");
            stm.setString(1, nic);

            if (stm.executeQuery().next()) {
                isNicExists = true;
            }

            stm = connection.prepareStatement("SELECT * FROM book WHERE isbn=?");
            stm.setString(1, isbn);

            if (stm.executeQuery().next()) {
                isIsbnExists = true;
            }


            if ((issue.getIssueId() == null || !issue.getIssueId().matches("\\d+")) && !isNicExists) {
                throw new ValidationException("Invalid Issue Id");
            } else if ((issue.getIsbn() == null || !issue.getIsbn().matches("\\d+")) && !isIsbnExists) {
                throw new ValidationException("Invalid isbn");
            }

            //check availability of the book
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM issue WHERE isbn=?");
            preparedStatement.setString(1, isbn);
            if (preparedStatement.executeQuery().next()) {
                response.sendError(HttpServletResponse.SC_GONE, "Book is not available");
                return;
            }

            issue.setDate(Date.valueOf(LocalDate.now()));
            preparedStatement = pool.getConnection().prepareStatement("INSERT INTO issue (nic,isbn,date) VALUES (?,?,?)", Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, nic);
            preparedStatement.setString(2, isbn);
            preparedStatement.setDate(3, issue.getDate());

            if (preparedStatement.executeUpdate() != 1) {
                throw new RuntimeException("Failed to register the issue");
            }
            response.setStatus(HttpServletResponse.SC_CREATED);

        } catch (JsonbException | ValidationException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    (e instanceof JsonbException) ? "Invalid JSON" : e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
