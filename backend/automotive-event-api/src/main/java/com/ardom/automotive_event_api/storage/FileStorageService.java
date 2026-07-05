package com.ardom.automotive_event_api.storage;

import com.ardom.automotive_event_api.storage.dto.response.PresignedDownloadResponse;
import com.ardom.automotive_event_api.storage.dto.response.PresignedUploadResponse;

public interface FileStorageService {
    PresignedUploadResponse generateUploadUrl(Long userId, String originalFilename, String contentType);

    PresignedDownloadResponse generateDownloadUrl(String key);

    void delete(String key);
}
