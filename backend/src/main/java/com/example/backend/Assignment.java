package com.example.backend;

import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/files")
public class Assignment {

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFsOperations gridFsOperations;

    @PostMapping("/assignments/upload")
    public ResponseEntity<?> uploadAssignment(@RequestParam("file") MultipartFile file,
                                              @RequestParam("role") String role) throws IOException {
        if (!role.equals("teacher"))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        ObjectId id = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(),
                file.getContentType(), Map.of("type", "assignment"));
        return ResponseEntity.ok(id.toString());
    }

    @GetMapping("/assignments")
    public List<String> listAssignments(@RequestHeader("Authorization") String token) {
        String username = JwtUtil.extractUsername(token);
        GridFSFindIterable files =
                gridFsTemplate.find(new Query(Criteria.where("metadata.type").is("assignment").andOperator(Criteria.where("metadata.uploader").is(username))));
        return StreamSupport.stream(files.spliterator(), false)
                .map(GridFSFile::getFilename)
                .toList();
    }

    @PostMapping("/submissions")
    @PreAuthorize("hasAuthority('student')")
    public ResponseEntity<?> submitAssignment(@RequestParam("file") MultipartFile file,
                                              @AuthenticationPrincipal Jwt principal) throws IOException {
        ObjectId id = gridFsTemplate.store(file.getInputStream(), file.getOriginalFilename(),
                file.getContentType(), Map.of("type", "submission"));
        return ResponseEntity.ok(id.toString());
    }

    @GetMapping("/submissions")
    public List<GridFSFile> viewSubmissions(@RequestParam("role") String role
            , @RequestHeader("Authorization") String token) {
        String username = JwtUtil.extractUsername(token);
        if (!role.equals("teacher"))
            return gridFsTemplate.find(new Query(Criteria.where(
                    "metadata.type").is("submission").andOperator(Criteria.where("metadata.uploader").is(username))))
                .into(new ArrayList<>());
        List<GridFSFile> all_subs =
                gridFsTemplate.find(new Query(Criteria.where(
                "metadata.type").is("submission")))
                .into(new ArrayList<>());
        List<GridFSFile> res = new ArrayList<>();
        for (GridFSFile f : all_subs) {
            if (!gridFsTemplate.find(new Query(Criteria.where("filename").is(
                    f.getMetadata().get("assignment")).andOperator(Criteria.where("metadata" +
                    ".uploader").is(username)))).into(new ArrayList<>()).isEmpty())
                res.add(f);
        }
        return res;
    }

    @DeleteMapping("/assignments/{filename}")
    public ResponseEntity<?> deleteAssignment(@PathVariable String filename,
                                              @RequestParam("role") String role) {
        if (!role.equals("teacher"))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        gridFsTemplate.delete(new Query(Criteria.where("filename").is(filename)
                .and("metadata.type").is("assignment")));
        return ResponseEntity.ok("Assignment deleted: " + filename);
    }

    @DeleteMapping("/submissions/{filename}")
    public ResponseEntity<?> deleteSubmission(@PathVariable String filename,
                                              @RequestHeader("Authorization") String token, @RequestParam("role") String role) {
        String username = JwtUtil.extractUsername(token);
        if (!role.equals("student"))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        gridFsTemplate.delete(new Query(Criteria.where("filename").is(filename)
                .and("metadata.type").is("submission")
                .and("metadata.uploader").is(username)));
        return ResponseEntity.ok("Submission deleted: " + filename);
    }

}
