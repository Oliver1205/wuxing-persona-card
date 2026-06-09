package com.wuxing.persona.service.shortlink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.wuxing.persona.config.AppProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

class RestExternalShortLinkClientTest {

    @Test
    void shouldCallExternalCreateApiWithExpectedHeadersAndBody() {
        CapturingRequestFactory requestFactory = new CapturingRequestFactory("""
                {"code":"0","data":{"gid":"wuxing_persona","originUrl":"https://wuxing.example.com/result/R1","fullShortUrl":"http://nurl.ink:8003/Abc123"}}
                """);
        AppProperties appProperties = new AppProperties();
        appProperties.getShortLink().getExternal().setBaseUrl("http://shortlink:8003");
        appProperties.getShortLink().getExternal().setSystemUsername("wuxing_system");
        appProperties.getShortLink().getExternal().setSystemUserId("wuxing-system");
        appProperties.getShortLink().getExternal().setSystemRealName("wuxing-system");
        RestExternalShortLinkClient client = new RestExternalShortLinkClient(appProperties, requestFactory);
        ExternalShortLinkCreateRequest request = new ExternalShortLinkCreateRequest();
        request.setDomain("nurl.ink:8003");
        request.setOriginUrl("https://wuxing.example.com/result/R1");
        request.setGid("wuxing_persona");
        request.setCreatedType(0);
        request.setValidDateType(0);
        request.setDescribe("五行人格卡结果 R1");

        ExternalShortLinkCreateResponse response = client.create(request);

        assertEquals("http://nurl.ink:8003/Abc123", response.getFullShortUrl());
        assertEquals("POST", requestFactory.request.getMethod().name());
        assertEquals("http://shortlink:8003/api/short-link/v1/create", requestFactory.request.getURI().toString());
        assertEquals("wuxing_system", requestFactory.request.getHeaders().getFirst("username"));
        assertEquals("wuxing-system", requestFactory.request.getHeaders().getFirst("userId"));
        assertEquals("wuxing-system", requestFactory.request.getHeaders().getFirst("realName"));
        String body = requestFactory.request.bodyAsString();
        assertTrue(body.contains("\"domain\":\"nurl.ink:8003\""));
        assertTrue(body.contains("\"originUrl\":\"https://wuxing.example.com/result/R1\""));
        assertTrue(body.contains("\"gid\":\"wuxing_persona\""));
    }

    @Test
    void shouldCallExternalStatsApiWithExpectedHeadersAndQuery() {
        CapturingRequestFactory requestFactory = new CapturingRequestFactory("""
                {"code":"0","data":{"pv":7,"uv":3,"uip":2}}
                """);
        AppProperties appProperties = new AppProperties();
        appProperties.getShortLink().getExternal().setBaseUrl("http://shortlink:8003");
        appProperties.getShortLink().getExternal().setSystemUsername("wuxing_system");
        appProperties.getShortLink().getExternal().setSystemUserId("wuxing-system");
        appProperties.getShortLink().getExternal().setSystemRealName("wuxing-system");
        RestExternalShortLinkClient client = new RestExternalShortLinkClient(appProperties, requestFactory);
        ExternalShortLinkStatsRequest request = new ExternalShortLinkStatsRequest();
        request.setFullShortUrl("nurl.ink:8003/Abc123");
        request.setGid("wuxing_persona");
        request.setEnableStatus(0);
        request.setStartDate("2026-06-09");
        request.setEndDate("2026-06-09");

        ExternalShortLinkStatsResponse response = client.stats(request);

        assertEquals(7L, response.getPv());
        assertEquals(3L, response.getUv());
        assertEquals(2L, response.getUip());
        assertEquals("GET", requestFactory.request.getMethod().name());
        assertEquals("/api/short-link/v1/stats", requestFactory.request.getURI().getPath());
        assertEquals("wuxing_system", requestFactory.request.getHeaders().getFirst("username"));
        assertEquals("wuxing-system", requestFactory.request.getHeaders().getFirst("userId"));
        assertEquals("wuxing-system", requestFactory.request.getHeaders().getFirst("realName"));
        String query = URLDecoder.decode(requestFactory.request.getURI().getRawQuery(), StandardCharsets.UTF_8);
        assertTrue(query.contains("fullShortUrl=nurl.ink:8003/Abc123"));
        assertTrue(query.contains("gid=wuxing_persona"));
        assertTrue(query.contains("enableStatus=0"));
        assertTrue(query.contains("startDate=2026-06-09"));
        assertTrue(query.contains("endDate=2026-06-09"));
    }

    private static class CapturingRequestFactory implements ClientHttpRequestFactory {

        private final String responseBody;
        private CapturingClientHttpRequest request;

        private CapturingRequestFactory(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) {
            this.request = new CapturingClientHttpRequest(uri, httpMethod, responseBody);
            return request;
        }
    }

    private static class CapturingClientHttpRequest implements ClientHttpRequest {

        private final URI uri;
        private final HttpMethod method;
        private final String responseBody;
        private final HttpHeaders headers = new HttpHeaders();
        private final ByteArrayOutputStream body = new ByteArrayOutputStream();

        private CapturingClientHttpRequest(URI uri, HttpMethod method, String responseBody) {
            this.uri = uri;
            this.method = method;
            this.responseBody = responseBody;
        }

        @Override
        public OutputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public ClientHttpResponse execute() {
            return new CapturingClientHttpResponse(responseBody);
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        private String bodyAsString() {
            return body.toString(StandardCharsets.UTF_8);
        }
    }

    private static class CapturingClientHttpResponse implements ClientHttpResponse {

        private final String responseBody;

        private CapturingClientHttpResponse(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public HttpStatusCode getStatusCode() {
            return HttpStatusCode.valueOf(200);
        }

        @Override
        public String getStatusText() {
            return "OK";
        }

        @Override
        public void close() {
        }

        @Override
        public InputStream getBody() throws IOException {
            return new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json; charset=utf-8");
            return headers;
        }
    }
}
