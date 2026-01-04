package com.project.mydrive.api.v1.model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FileResourceAction {
    PREVIEW("inline"),
    DOWNLOAD("attachment");

    private final String contentDisposition;

    FileResourceAction(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }

    public static FileResourceAction fromString(String s) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(s))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Action not supported for string: " + s));
    }

}
