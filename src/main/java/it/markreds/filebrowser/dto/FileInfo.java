package it.markreds.filebrowser.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class FileInfo implements Transferable {
    private String name;
    private String path;
    private String icon;
    private String href;
    private String description;
    private Long size;
    private String sizeFormat;
    private LocalDateTime timestamp;
    private String owner;
    private String group;
    private String permission;
}
