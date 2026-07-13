package com.ardom.automotive_event_api.storage;

import com.ardom.automotive_event_api.storage.dto.request.PresignedUploadRequest;
import com.ardom.automotive_event_api.storage.dto.response.PresignedDownloadResponse;
import com.ardom.automotive_event_api.storage.dto.response.PresignedUploadResponse;

public interface FileStorageService {
    PresignedUploadResponse generateUploadUrl(Long userId, PresignedUploadRequest request);

    PresignedDownloadResponse generateDownloadUrl(String key);

    void delete(String key);
}
