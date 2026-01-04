package com.project.mydrive.core.domain;

public enum FileType {
    Image,
    Video,
    Audio,
    Pdf,
    Text,
    Word,
    Archive,
    Executable,
    Unknown;

    public FileCapability getCapability() {
        return switch (this) {
            case Image, Pdf, Text -> new FileCapability(true, false, false, true);
            case Video, Audio -> new FileCapability(true, false, true, true);
            case Word, Executable, Archive -> new FileCapability(false, false, false, true);
            case Unknown -> new FileCapability(false, false, false, false);
        };
    }

    public static FileType getFileTypeFromContentType(String contentType) {
        if(contentType.startsWith("image/")) return Image;
        if(contentType.startsWith("video/")) return Video;
        if(contentType.startsWith("audio/")) return Audio;

        // TODO implement pending cases
        return switch (contentType) {
            case "application/pdf" -> Pdf;
            case "text/plain", "text/markdown","text/html", "application/json" -> Text; //need to think of a better way to do this
            default -> Unknown;
        };
    }
}
