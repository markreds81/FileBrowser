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
    private static final Map<String, String> ICONS = new HashMap<>();

    static {
        ICONS.put("gif", "image2.gif");
        ICONS.put("png", "image2.gif");
        ICONS.put("jpg", "image2.gif");
        ICONS.put("sit", "compressed.gif");
        ICONS.put("zip", "compressed.gif");
        ICONS.put("smi", "screw2.gif");
        ICONS.put("dmg", "screw2.gif");
        ICONS.put("bin", "binhex.gif");
        ICONS.put("c", "c.gif");
        ICONS.put("hqx", "binhex.gif");
        ICONS.put("pdf", "pdf.gif");
        ICONS.put("ps", "ps.gif");
        ICONS.put("tar", "tar.gif");
        ICONS.put("txt", "text.gif");
        ICONS.put("rtf", "text.gif");
    }

    private final FtpService ftpService;

    private static String hypertextReferenceOf(FTPFile file, String path) {
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

    private static String parentHypertextReferenceOf(String path) {
        Path parent = Path.of(path).getParent();
        return (parent != null) ? "/list?path=" + parent.toString() : "/list";
    }

    private static String permissionOf(FTPFile file) {
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

    private static String sizeFormatOf(FTPFile file) {
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

    private static String iconOf(FTPFile file) {
        if (file.isDirectory()) {
            return "folder.gif";
        }

        if (file.isSymbolicLink()) {
            return "link.gif";
        }

        String iconName = ICONS.get(FilenameUtils.getExtension(file.getName()));

        return iconName != null ? iconName : "generic.gif";
    }

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
                        .setSizeFormat(sizeFormatOf(file))
                        .setIcon(iconOf(file))
                        .setHref(hypertextReferenceOf(file, remotePath))
                        .setOwner(file.getUser())
                        .setGroup(file.getGroup())
                        .setPermission(permissionOf(file))
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
        model.addAttribute("hrefParent", parentHypertextReferenceOf(remotePath));
        model.addAttribute("files", files);
        return "list";
    }
}
