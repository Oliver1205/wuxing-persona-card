package com.wuxing.persona.service.shortlink;

public interface ExternalShortLinkClient {

    ExternalShortLinkCreateResponse create(ExternalShortLinkCreateRequest request);

    ExternalShortLinkStatsResponse stats(ExternalShortLinkStatsRequest request);
}
