package it.markreds.filebrowser.controller;

import it.markreds.filebrowser.dto.FileInfo;
import it.markreds.filebrowser.service.FtpService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Controller
@RequiredArgsConstructor
public class FileBrowserController {
    private static final Set<String> FILES_TO_SKIP = Set.of(".", "..");
    private static final Map<String, String> ICONS = Map.of(
            "gif", "box2.gif",
            "sit", "compressed.gif"
    );

    private final FtpService ftpService;

    @RequestMapping({"/", "/list"})
    public String list(@RequestParam(name = "path", required = false, defaultValue = "/") String remotePath,
                       @RequestParam(name = "order", required = false, defaultValue = "name") String orderBy,
                       Model model) throws IOException {
        List<FileInfo> files = new LinkedList<>();
        for (FTPFile file : ftpService.listFiles(remotePath)) {
            if (!FILES_TO_SKIP.contains(file.getName()))
                files.add(new FileInfo()
                        .setName(file.getName())
                        .setPath(remotePath)
                        .setSize(file.getSize())
                        .setSizeFormat(getSizeFormat(file))
                        .setIcon(findIcon(file))
                        .setHref(getHypertextReference(file, remotePath))
                        .setOwner(file.getUser())
                        .setGroup(file.getGroup())
                        .setPermission(getPermission(file))
                        .setTimestamp(LocalDateTime.ofInstant(file.getTimestampInstant(), ZoneId.systemDefault()))
                );
        }
        switch (orderBy) {
            case "timestamp" ->
                    files.sort(Comparator.comparing(FileInfo::getTimestamp, Comparator.nullsFirst(LocalDateTime::compareTo)));
            case "size" -> files.sort(Comparator.comparing(FileInfo::getSize, Comparator.nullsFirst(Long::compareTo)));
            case "description" ->
                    files.sort(Comparator.comparing(FileInfo::getDescription, Comparator.nullsFirst(String::compareTo)));
            default -> files.sort(Comparator.comparing(FileInfo::getName, Comparator.nullsFirst(String::compareTo)));
        }
        model.addAttribute("currentPath", remotePath);
        model.addAttribute("hrefParent", getParentHypertextReference(remotePath));
        model.addAttribute("files", files);
        return "list";
    }

    private String findIcon(FTPFile file) {
        if (file.isDirectory()) {
            return "folder.gif";
        }

        if (file.isSymbolicLink()) {
            return "link.gif";
        }

        String iconName = ICONS.get(FilenameUtils.getExtension(file.getName()));

        return iconName != null ? iconName : "generic.gif";
    }

    private String getHypertextReference(FTPFile file, String path) {
        if (file.isDirectory()) {
            if (file.getName().equals("/")) {
                return "/list";
            }
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            return "/list?path=" + path + file.getName();
        }

        if (file.isSymbolicLink()) {
            // TODO da verificare
            return "/list?path=" + file.getLink();
        }

        if (file.isFile()) {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            return "/ftp/download?file=" + path + file.getName();
        }

        return "#";
    }

    private String getParentHypertextReference(String path) {
        Path parent = Path.of(path).getParent();
        return (parent != null) ? "/list?path=" + parent.toString() : "/list";
    }

    private String getPermission(FTPFile file) {
        return String.valueOf(file.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION) ? 'r' : '-') +
                (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION) ? 'w' : '-') +
                (file.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION) ? 'x' : '-') +
                (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION) ? 'r' : '-') +
                (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION) ? 'w' : '-') +
                (file.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION) ? 'x' : '-') +
                (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION) ? 'r' : '-') +
                (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION) ? 'w' : '-') +
                (file.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION) ? 'x' : '-');
    }

    private String getSizeFormat(FTPFile file) {
        if (!file.isDirectory()) {
            long size = file.getSize();
            String[] units = {"b", "K", "M", "G", "T"};
            int unitIndex = 0;

            while (size >= 4096 && unitIndex < units.length - 1) {
                size /= 1024;
                unitIndex++;
            }

            return String.format("%.1f%s", (double) size, units[unitIndex]);
        }
        return "-";
    }
}
