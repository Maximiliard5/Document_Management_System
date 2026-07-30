package com.example.dms.dto.document;

import java.io.InputStream;

public record DocumentDownload(InputStream stream, String name, String contentType, long size) {
}
