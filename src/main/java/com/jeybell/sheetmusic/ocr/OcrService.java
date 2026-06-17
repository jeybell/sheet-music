package com.jeybell.sheetmusic.ocr;

import com.jeybell.sheetmusic.song.dto.OcrResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private final RestTemplate restTemplate;
    private final String ocrServiceUrl;

    public OcrService(@Value("${ocr.service.url:}") String ocrServiceUrl) {
        this.ocrServiceUrl = ocrServiceUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(60_000);
        this.restTemplate = new RestTemplate(factory);
    }

    public OcrResult analyze(byte[] imageBytes, String fileName) {
        if (ocrServiceUrl == null || ocrServiceUrl.isBlank()) {
            return null;
        }
        try {
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", imageResource);

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
            OcrRawResponse response = restTemplate.postForObject(
                    ocrServiceUrl + "/ocr", request, OcrRawResponse.class
            );

            if (response == null) return null;
            return new OcrResult(response.title(), response.key(), response.chords(), response.rawText());
        } catch (Exception e) {
            log.warn("OCR 분석 실패 (무시됨): {}", e.getMessage());
            return null;
        }
    }

    private record OcrRawResponse(
            String title,
            String key,
            List<String> chords,
            String rawText
    ) {}
}
