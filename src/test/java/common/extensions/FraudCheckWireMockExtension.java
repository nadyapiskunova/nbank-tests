package common.extensions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import common.annotations.FraudCheckMock;
import common.models.FraudCheckMockResponse;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class FraudCheckWireMockExtension implements BeforeEachCallback, AfterEachCallback {
    
    private WireMockServer wireMockServer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void beforeEach(ExtensionContext context) {
        // Find the FraudCheckMock annotation on the test method or class
        FraudCheckMock mockConfig = context.getTestMethod()
                .map(method -> method.getAnnotation(FraudCheckMock.class))
                .orElseGet(() -> context.getTestClass()
                        .map(clazz -> clazz.getAnnotation(FraudCheckMock.class))
                        .orElse(null));
        
        if (mockConfig != null) {
            setupWireMock(mockConfig);
        }
    }
    private void setupWireMock(FraudCheckMock config) {
        startWireMock(config);

        String responseBody = createResponseBody(config);

        createStub(config, responseBody);
    }

    private void startWireMock(FraudCheckMock config) {
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(config.port())
        );

        wireMockServer.start();
    }

    private String createResponseBody(FraudCheckMock config) {
        FraudCheckMockResponse response = FraudCheckMockResponse.builder()
                .status(config.status())
                .decision(config.decision())
                .riskScore(config.riskScore())
                .reason(config.reason())
                .requiresManualReview(config.requiresManualReview())
                .additionalVerificationRequired(
                        config.additionalVerificationRequired()
                )
                .build();

        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize Fraud Check mock response",
                    exception
            );
        }
    }

    private void createStub(
            FraudCheckMock config,
            String responseBody
    ) {
        wireMockServer.stubFor(
                post(urlPathEqualTo(config.endpoint()))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader(
                                                "Content-Type",
                                                "application/json"
                                        )
                                        .withBody(responseBody)
                        )
        );
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (wireMockServer != null) {
            wireMockServer.stop();
            wireMockServer = null;
        }
    }
}
