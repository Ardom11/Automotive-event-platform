package com.ardom.automotive_event_api.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A single car photo with its resolved public URL")
public record CarPhotoResponse(

        @Schema(description = "Unique photo ID", example = "301")
        Long id,

        @Schema(description = "Publicly accessible URL of the photo (pre-signed S3 URL)",
                example = "https://s3-eu-north-1.amazonaws.com/photos/42/porsche-front.jpg")
        String url
) {
}
