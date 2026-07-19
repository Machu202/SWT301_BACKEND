package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadedAsset {
    private String publicId;
    private String secureUrl;
}
