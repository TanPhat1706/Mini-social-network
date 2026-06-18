package com.example.backend.PostMedia;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaResponse {
    private Long id;
    private String url;
    private String type;
}
