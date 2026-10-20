package com.workforce.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "import_errors")
public class ImportError {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "import_job_id")
    private Long importJobId;
    @Column(name = "line_number")
    private int lineNumber;
    @Column(name = "raw_line")
    private String rawLine;
    private String reason;

    protected ImportError() {}

    public ImportError(Long importJobId, int lineNumber, String rawLine, String reason) {
        this.importJobId = importJobId;
        this.lineNumber = lineNumber;
        this.rawLine = rawLine;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Long getImportJobId() { return importJobId; }
    public int getLineNumber() { return lineNumber; }
    public String getRawLine() { return rawLine; }
    public String getReason() { return reason; }
}
