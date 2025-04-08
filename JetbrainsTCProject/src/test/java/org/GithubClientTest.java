package org;

import org.example.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GithubClientTest {

  private GithubClient githubClient;
  private HttpClient mockHttpClient;
  private static final String TEST_TOKEN = "test-token";
  private TypeReference<List<String>> typeReference;

  @BeforeEach
  void setUp() throws Exception {
    githubClient = new GithubClient(TEST_TOKEN);

    typeReference = new TypeReference<List<String>>() {};

    mockHttpClient = mock(HttpClient.class);
    var httpClientField = GithubClient.class.getDeclaredField("httpClient");
    httpClientField.setAccessible(true);
    httpClientField.set(githubClient, mockHttpClient);
  }

  @Test
  void testSuccessfulRequest() throws IOException, InterruptedException {
    GHRequest request = new GHRequest("https://api.github.com/repos/owner/repo/commits");

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn("[\"commit1\", \"commit2\"]");

    HttpHeaders mockHeaders = HttpHeaders.of(
            Map.of("Link", List.of("<https://api.github.com/repos/owner/repo/commits?page=2>; rel=\"next\"")),
            (name, value) -> true
    );
    when(mockResponse.headers()).thenReturn(mockHeaders);

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse);

    GHResponse<List<String>> response = githubClient.send(request, typeReference);

    assertNotNull(response);
    assertNotNull(response.response);
    assertEquals(2, response.response.getData().size());
    assertEquals("commit1", response.response.getData().get(0));
    assertEquals("commit2", response.response.getData().get(1));
    assertTrue(response.response.hasNext());

    ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
    verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));

    HttpRequest capturedRequest = requestCaptor.getValue();
    assertEquals(URI.create("https://api.github.com/repos/owner/repo/commits"), capturedRequest.uri());
    assertEquals("Bearer " + TEST_TOKEN, capturedRequest.headers().firstValue("Authorization").orElse(null));
  }

  @Test
  void testErrorResponse() throws IOException, InterruptedException {
    GHRequest request = new GHRequest("https://api.github.com/repos/owner/repo/commits");

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(404);
    when(mockResponse.body()).thenReturn("{\"message\":\"Not Found\"}");

    HttpHeaders mockHeaders = HttpHeaders.of(Map.of(), (name, value) -> true);
    when(mockResponse.headers()).thenReturn(mockHeaders);

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse);

    GHApiException exception = assertThrows(GHApiException.class, () -> {
      githubClient.send(request, typeReference);
    });

    assertTrue(exception.getMessage().contains("404"));
    assertTrue(exception.getMessage().contains("Not Found"));
  }

  @Test
  void testNetworkError() throws IOException, InterruptedException {
    GHRequest request = new GHRequest("https://api.github.com/repos/owner/repo/commits");

    IOException ioException = new IOException("Network error");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(ioException);

    GHApiException exception = assertThrows(GHApiException.class, () -> {
      githubClient.send(request, typeReference);
    });

    assertTrue(exception.getMessage().contains("Error sending GitHub API request"));
    assertEquals(ioException, exception.getCause());
  }

  @Test
  void testInterruptedException() throws IOException, InterruptedException {
    GHRequest request = new GHRequest("https://api.github.com/repos/owner/repo/commits");

    InterruptedException interruptedException = new InterruptedException("Thread interrupted");
    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(interruptedException);

    GHApiException exception = assertThrows(GHApiException.class, () -> {
      githubClient.send(request, typeReference);
    });

    assertTrue(exception.getMessage().contains("Error sending GitHub API request"));
    assertEquals(interruptedException, exception.getCause());
  }

  @Test
  void testDeserializationError() throws IOException, InterruptedException {
    GHRequest request = new GHRequest("https://api.github.com/repos/owner/repo/commits");

    @SuppressWarnings("unchecked")
    HttpResponse<String> mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body()).thenReturn("invalid json");

    HttpHeaders mockHeaders = HttpHeaders.of(Map.of(), (name, value) -> true);
    when(mockResponse.headers()).thenReturn(mockHeaders);

    when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(mockResponse);

    assertThrows(GHApiException.class, () -> {
      githubClient.send(request, typeReference);
    });
  }
}