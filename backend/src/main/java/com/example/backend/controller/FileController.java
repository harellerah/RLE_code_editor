package com.example.backend.controller;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired private GridFsOperations operations;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws IOException {
        System.out.println(file.getOriginalFilename());
        ObjectId id = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(), file.getContentType());
        return id.toString();
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<?> download(@PathVariable String id) throws IOException {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(id)));
        if (file == null) return ResponseEntity.notFound().build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(operations.getResource(file).getInputStream(), out);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMetadata().get("_contentType").toString()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                .body(out.toByteArray());
    }
}

