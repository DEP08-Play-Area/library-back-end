package lk.ijse.dep8.library.dto;


import java.sql.Date;

public class IssueDTO {
    private String issueId;
    private String nic;
    private String isbn;
    private Date date;

    public IssueDTO() {
    }

    public IssueDTO(String issueId, String nic, String isbn, Date date) {
        this.issueId = issueId;
        this.nic = nic;
        this.isbn = isbn;
        this.date = date;
    }

    public String getIssueId() {
        return issueId;
    }

    public void setIssueId(String issueId) {
        this.issueId = issueId;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "IssueDTO{" +
                "issueId='" + issueId + '\'' +
                ", nic='" + nic + '\'' +
                ", isbn='" + isbn + '\'' +
                ", date=" + date +
                '}';
    }
}
