package com.example.backend.Post;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.example.backend.Enum.Visibility;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostRequest {
    @NotBlank(message = "Content cannot be empty")
    private String content;
    private Visibility visibility;
    private List<MultipartFile> mediaFiles;
}
