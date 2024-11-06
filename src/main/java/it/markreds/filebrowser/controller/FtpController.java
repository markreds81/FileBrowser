package it.markreds.filebrowser.controller;

import it.markreds.filebrowser.service.FtpService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("ftp")
@RequiredArgsConstructor
public class FtpController {
    private final FtpService ftpService;

    @GetMapping(path = "list")
    public List<FTPFile> findAll(@RequestParam(name = "path", required = false, defaultValue = "/") String remotePath) throws IOException {
        return ftpService.listFiles(remotePath);
    }

    @GetMapping(path = "get", produces = "application/octet-stream")
    public ResponseEntity<byte[]> downloadFile(@RequestParam(name = "path") String remoteFilePath) throws IOException {
        byte[] fileContent = ftpService.downloadFile(remoteFilePath);
        String fileName = Paths.get(remoteFilePath).getFileName().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }
}
