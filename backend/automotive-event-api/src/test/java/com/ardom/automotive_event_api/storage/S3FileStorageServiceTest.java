package com.ardom.automotive_event_api.storage;

import com.ardom.automotive_event_api.storage.dto.request.PresignedUploadRequest;
import com.ardom.automotive_event_api.storage.dto.response.PresignedDownloadResponse;
import com.ardom.automotive_event_api.storage.dto.response.PresignedUploadResponse;
import com.ardom.automotive_event_api.storage.exception.FileTooBigException;
import com.ardom.automotive_event_api.storage.exception.UnsupportedFiletypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private S3FileStorageService s3FileStorageService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final int EXPIRATION_MINUTES = 5;
    private static final int MAX_FILESIZE = 5242880;
    private static final int VALID_FILESIZE = 1048576;

    @BeforeEach
    void setUp() {
        s3FileStorageService = new S3FileStorageService(s3Client, s3Presigner);
        ReflectionTestUtils.setField(s3FileStorageService, "bucketName", BUCKET_NAME);
        ReflectionTestUtils.setField(s3FileStorageService, "expirationMinutes", EXPIRATION_MINUTES);
        ReflectionTestUtils.setField(s3FileStorageService, "MAX_FILE_SIZE", MAX_FILESIZE);
    }

    private URL fakeUrl(String value) {
        try {
            return new URL(value);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Method generateUploadUrl()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("generateUploadUrl()")
    class GenerateUploadUrl {

        @Test
        @DisplayName("Should throw UnsupportedFiletypeException when content type is not allowed")
        void generateUploadUrl_shouldThrowUnsupportedFiletypeException_whenContentTypeNotAllowed() {
            // given
            Long userId = 1L;
            PresignedUploadRequest request = new PresignedUploadRequest("video.mp4", "video/mp4", VALID_FILESIZE);

            // when / then
            assertThatThrownBy(() -> s3FileStorageService.generateUploadUrl(userId, request))
                    .isInstanceOf(UnsupportedFiletypeException.class);

            verifyNoInteractions(s3Presigner);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when filename has no extension")
        void generateUploadUrl_shouldThrowIllegalArgumentException_whenFilenameHasNoExtension() {
            // given
            Long userId = 1L;
            PresignedUploadRequest request = new PresignedUploadRequest("photo_without_extension", "image/jpeg", VALID_FILESIZE);

            // when / then
            assertThatThrownBy(() -> s3FileStorageService.generateUploadUrl(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("File has no extension");

            verifyNoInteractions(s3Presigner);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when extension does not match content type")
        void generateUploadUrl_shouldThrowIllegalArgumentException_whenExtensionDoesNotMatchContentType() {
            // given
            Long userId = 1L;
            PresignedUploadRequest request = new PresignedUploadRequest("photo.png", "image/jpeg", VALID_FILESIZE);

            // when / then
            assertThatThrownBy(() -> s3FileStorageService.generateUploadUrl(userId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not supported");

            verifyNoInteractions(s3Presigner);
        }

        @Test
        @DisplayName("Should throw FileTooBigException when size is too big")
        void generateUploadUrl_shouldThrowFileTooBigException_whenFileSizeTooBig() {
            // given
            Long userId = 1L;
            PresignedUploadRequest request = new PresignedUploadRequest("photo.png", "image/jpeg", (VALID_FILESIZE * 100));

            // when / then
            assertThatThrownBy(() -> s3FileStorageService.generateUploadUrl(userId, request))
                    .isInstanceOf(FileTooBigException.class)
                    .hasMessageContaining("too big");

            verifyNoInteractions(s3Presigner);
        }

        @Test
        @DisplayName("Should generate presigned upload response when file is valid jpeg")
        void generateUploadUrl_shouldGeneratePresignedUploadResponse_whenFileIsValidJpeg() {
            // given
            Long userId = 42L;
            PresignedUploadRequest request = new PresignedUploadRequest("car-photo.jpg", "image/jpeg", VALID_FILESIZE);

            when(presignedPutObjectRequest.url()).thenReturn(fakeUrl("https://s3.example.com/upload"));
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(presignedPutObjectRequest);

            // when
            PresignedUploadResponse response =
                    s3FileStorageService.generateUploadUrl(userId, request);

            // then
            assertThat(response.uploadUrl()).isEqualTo("https://s3.example.com/upload");
            assertThat(response.key()).startsWith("photos/42/");
            assertThat(response.key()).endsWith(".jpg");
            assertThat(response.expiresAt()).isNotNull();
        }

        @Test
        @DisplayName("Should include correct bucket, key and content type in presigned request")
        void generateUploadUrl_shouldBuildPresignRequestWithCorrectBucketKeyAndContentType_whenFileIsValid() {
            // given
            Long userId = 7L;
            PresignedUploadRequest request = new PresignedUploadRequest("car.png", "image/png", VALID_FILESIZE);

            when(presignedPutObjectRequest.url()).thenReturn(fakeUrl("https://s3.example.com/upload"));
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(presignedPutObjectRequest);

            ArgumentCaptor<PutObjectPresignRequest> captor =
                    ArgumentCaptor.forClass(PutObjectPresignRequest.class);

            // when
            s3FileStorageService.generateUploadUrl(userId, request);

            // then
            verify(s3Presigner).presignPutObject(captor.capture());
            PutObjectRequest capturedObjectRequest = captor.getValue().putObjectRequest();

            assertThat(capturedObjectRequest.bucket()).isEqualTo(BUCKET_NAME);
            assertThat(capturedObjectRequest.key()).startsWith("photos/7/");
            assertThat(capturedObjectRequest.key()).endsWith(".png");
            assertThat(capturedObjectRequest.contentType()).isEqualTo(request.contentType());
        }

        @Test
        @DisplayName("Should generate unique keys for different upload calls")
        void generateUploadUrl_shouldGenerateUniqueKeys_whenCalledMultipleTimes() {
            // given
            Long userId = 1L;
            PresignedUploadRequest request = new PresignedUploadRequest("photo.webp", "image/webp", VALID_FILESIZE);

            when(presignedPutObjectRequest.url()).thenReturn(fakeUrl("https://s3.example.com/upload"));
            when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                    .thenReturn(presignedPutObjectRequest);

            // when
            PresignedUploadResponse first = s3FileStorageService.generateUploadUrl(userId, request);
            PresignedUploadResponse second = s3FileStorageService.generateUploadUrl(userId, request);

            // then
            assertThat(first.key()).isNotEqualTo(second.key());
        }
    }

    // -------------------------------------------------------------------------
    // Method generateDownloadUrl()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("generateDownloadUrl()")
    class GenerateDownloadUrl {

        @Test
        @DisplayName("Should return presigned download response when key is provided")
        void generateDownloadUrl_shouldReturnPresignedDownloadResponse_whenKeyProvided() {
            // given
            String key = "photos/42/some-uuid.jpg";

            when(presignedGetObjectRequest.url()).thenReturn(fakeUrl("https://s3.example.com/download"));
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            // when
            PresignedDownloadResponse response = s3FileStorageService.generateDownloadUrl(key);

            // then
            assertThat(response.downloadUrl()).isEqualTo("https://s3.example.com/download");
            assertThat(response.expiresAt()).isNotNull();
        }

        @Test
        @DisplayName("Should build presigned get request with correct bucket and key")
        void generateDownloadUrl_shouldBuildPresignRequestWithCorrectBucketAndKey_whenKeyProvided() {
            // given
            String key = "photos/42/some-uuid.jpg";

            when(presignedGetObjectRequest.url()).thenReturn(fakeUrl("https://s3.example.com/download"));
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedGetObjectRequest);

            ArgumentCaptor<GetObjectPresignRequest> captor =
                    ArgumentCaptor.forClass(GetObjectPresignRequest.class);

            // when
            s3FileStorageService.generateDownloadUrl(key);

            // then
            verify(s3Presigner).presignGetObject(captor.capture());
            assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo(BUCKET_NAME);
            assertThat(captor.getValue().getObjectRequest().key()).isEqualTo(key);
        }
    }

    // -------------------------------------------------------------------------
    // Method delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Should delete object when key is provided")
        void delete_shouldDeleteObject_whenKeyProvided() {
            // given
            String key = "photos/42/some-uuid.jpg";

            // when
            s3FileStorageService.delete(key);

            // then
            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(captor.capture());

            assertThat(captor.getValue().bucket()).isEqualTo(BUCKET_NAME);
            assertThat(captor.getValue().key()).isEqualTo(key);
        }

        @Test
        @DisplayName("Should throw RuntimeException when S3 throws an exception")
        void delete_shouldThrowRuntimeException_whenS3ExceptionOccurs() {
            // given
            String key = "photos/42/some-uuid.jpg";
            S3Exception s3Exception = (S3Exception) S3Exception.builder().message("S3 is down").build();

            when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(s3Exception);

            // when / then
            assertThatThrownBy(() -> s3FileStorageService.delete(key))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining(key)
                    .hasCause(s3Exception);
        }
    }
}
