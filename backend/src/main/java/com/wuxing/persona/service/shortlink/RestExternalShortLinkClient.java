package com.wuxing.persona.service.shortlink;

import com.wuxing.persona.common.BusinessException;
import com.wuxing.persona.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestExternalShortLinkClient implements ExternalShortLinkClient {

    private final AppProperties appProperties;
    private final ClientHttpRequestFactory requestFactoryOverride;

    @Autowired
    public RestExternalShortLinkClient(AppProperties appProperties) {
        this(appProperties, null);
    }

    RestExternalShortLinkClient(AppProperties appProperties, ClientHttpRequestFactory requestFactoryOverride) {
        this.appProperties = appProperties;
        this.requestFactoryOverride = requestFactoryOverride;
    }

    @Override
    public ExternalShortLinkCreateResponse create(ExternalShortLinkCreateRequest request) {
        AppProperties.ExternalShortLinkProperties external = appProperties.getShortLink().getExternal();
        try {
            ExternalShortLinkApiResponse response = RestClient.builder()
                    .requestFactory(requestFactory(external))
                    .baseUrl(external.getBaseUrl())
                    .defaultHeader("username", external.getSystemUsername())
                    .defaultHeader("userId", external.getSystemUserId())
                    .defaultHeader("realName", external.getSystemRealName())
                    .build()
                    .post()
                    .uri("/api/short-link/v1/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ExternalShortLinkApiResponse.class);
            if (response == null) {
                throw new BusinessException("external short link service returned empty response");
            }
            if (!response.isSuccess()) {
                throw new BusinessException("external short link service failed: " + response.getMessage());
            }
            if (response.getData() == null) {
                throw new BusinessException("external short link service returned empty data");
            }
            return response.getData();
        } catch (RestClientException ex) {
            throw new BusinessException("external short link service unavailable: " + ex.getClass().getSimpleName());
        }
    }

    private ClientHttpRequestFactory requestFactory(AppProperties.ExternalShortLinkProperties external) {
        if (requestFactoryOverride != null) {
            return requestFactoryOverride;
        }
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(external.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(external.getReadTimeoutMillis());
        return requestFactory;
    }
}
