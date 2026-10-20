package com.workforce.controller;

import com.workforce.dto.ImportJobResponse;
import com.workforce.dto.PageResponse;
import com.workforce.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports")
@Tag(name = "Bulk import")
public class ImportController {
    private final ImportService service;

    public ImportController(ImportService service) { this.service = service; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Upload a CSV (name,email,department,role,salary,joiningDate) and start an import job")
    public ImportJobResponse upload(@RequestParam("file") MultipartFile file, Principal principal) {
        return service.upload(file, principal.getName());
    }

    @GetMapping
    public PageResponse<ImportJobResponse> list(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return service.list(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "id")));
    }

    @GetMapping("/{id}")
    public ImportJobResponse get(@PathVariable Long id) { return service.get(id); }

    @PostMapping("/{id}/restart")
    @Operation(summary = "Restart a failed/stopped job from its last committed chunk")
    public ImportJobResponse restart(@PathVariable Long id) { return service.restart(id); }

    @GetMapping(value = "/{id}/errors.csv", produces = "text/csv")
    @Operation(summary = "Download rejected rows with error reasons")
    public ResponseEntity<String> errors(@PathVariable Long id) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import-" + id + "-rejected.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(service.errorsCsv(id));
    }
}
