package com.example.backend.controller;

import com.example.backend.JwtUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired private GridFsOperations operations;

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file, @RequestHeader("Authorization") String token,
                                         @RequestParam("role") String role,
                                         @RequestParam("type") String type,
                                         @RequestParam("assignment") String assignment) throws IOException {
        String username = JwtUtil.extractUsername(token);
        String filename = file.getOriginalFilename();
        System.out.println("Attempting to upload: " + filename);

        if (filename == null || filename.isEmpty())
            return ResponseEntity.badRequest().body("File name cannot be empty.");


        if (username == null)
            return ResponseEntity.badRequest().body("Username cannot be empty.");

        // 1. Check if a file with the same name already exists
        Query query = Query.query(Criteria.where("filename").is(filename));
        GridFSFile existingFile = gridFsTemplate.findOne(query);

        if (existingFile != null) {
            // File with this name already exists
            System.out.println("File with name '" + filename + "' already exists. Aborting upload.");
            // You can choose different HTTP statuses or messages based on your requirements
            return ResponseEntity.status(HttpStatus.CONFLICT).body("File with name '" + filename + "' already exists. Existing ID: " + existingFile.getId().toString());
            // return ResponseEntity.ok("File with name '" + filename + "' already exists. Not uploading a duplicate."); // Or just return OK if you consider it a non-error state
        } else {
            // 2. Prepare metadata
            BasicDBObject metaData = new BasicDBObject();
            metaData.put("uploader", username);
            metaData.put("role", role);
            metaData.put("assignment", assignment);
            metaData.put("type", type);
            // You can add more metadata fields as needed:
            // metaData.put("category", "documents");
            // metaData.put("sizeBytes", file.getSize()); // Already available, but can be useful for quick queries

            // 3. Store the file with metadata
            // Use the store method that accepts metadata (DBObject/Document)
            ObjectId id = gridFsTemplate.store(file.getInputStream(), filename, file.getContentType(), metaData);

            System.out.println("File uploaded successfully. ID: " + id.toString());
            return ResponseEntity.ok("File uploaded successfully. ID: " + id.toString());
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<?> download(@PathVariable String filename,
                                      @RequestHeader("Authorization") String token,
                                      @RequestParam("role") String role) throws IOException {
        String username = JwtUtil.extractUsername(token);
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("filename").is(filename)));
        if (file == null) return ResponseEntity.notFound().build();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(operations.getResource(file).getInputStream(), out);

        if (file.getMetadata() != null) {
            // Accessing metadata using keys
            String retrievedUploader = file.getMetadata().getString("uploader");
            String retrievedRole = file.getMetadata().getString("role");

            if (!username.equals(retrievedUploader) && !retrievedRole.equals(
                    "teacher") && !role.equals("teacher"))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                        "Permission denied");

            System.out.println("Retrieved File Metadata:");
            System.out.println("  Uploader: " + retrievedUploader);
            System.out.println("  Full Metadata: " + file.getMetadata().toJson()); // Prints the full BSON document
        } else {
            System.out.println("No metadata found for retrieved file.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getMetadata().get("_contentType").toString()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getFilename())
                .body(out.toByteArray());
    }
}

