package it.markreds.filebrowser.service;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
public class FtpService {
    @Value("${ftp.host}")
    private String ftpHost;

    @Value("${ftp.port}")
    private int ftpPort;

    @Value("${ftp.username}")
    private String ftpUsername;

    @Value("${ftp.password}")
    private String ftpPassword;

    private FTPClient configureClient() throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(ftpHost, ftpPort);
        ftpClient.login(ftpUsername, ftpPassword);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftpClient;
    }

    private void releaseClient(FTPClient client) throws IOException {
        if (client.isConnected()) {
            client.logout();
            client.disconnect();
        }
    }

    public List<FTPFile> listFiles(String path) throws IOException {
        FTPClient ftpClient = configureClient();
        try {
            return Arrays.asList(ftpClient.listFiles(path));
        } finally {
            releaseClient(ftpClient);
        }
    }

    public byte[] downloadFile(String remoteFilePath) throws IOException {
        FTPClient ftpClient = configureClient();
        try {
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new IOException("Could not connect to FTP server.");
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            if (!ftpClient.retrieveFile(remoteFilePath, stream)) {
                throw new IOException("Could not retrieve FTP remote file.");
            }
            return stream.toByteArray();
        } finally {
            releaseClient(ftpClient);
        }
    }
}
