package git.schvarts11.aicompagnon.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import git.schvarts11.aicompagnon.AicompagnonMod;
import git.schvarts11.aicompagnon.config.AIConfig;

/**
 * Async HTTP client for OpenAI-compatible chat completions API.
 */
public final class AIClient {

    private static final Gson GSON = new Gson();
    private static final String CHAT_PATH = "/v1/chat/completions";

    private final HttpClient httpClient;
    private final String apiEndpoint;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;

    private AIClient(HttpClient httpClient, String apiEndpoint, String apiKey, String model, int timeoutSeconds) {
        this.httpClient = httpClient;
        this.apiEndpoint = apiEndpoint;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public static synchronized AIClient create(AIConfig config) {
        Objects.requireNonNull(config, "config");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, config.apiTimeoutSeconds())))
                .version(HttpClient.Version.HTTP_2)
                .build();
        return new AIClient(client, config.apiEndpoint(), config.apiKey(), config.model(), config.apiTimeoutSeconds());
    }

    /**
     * Sends a context snapshot to the AI and requests the next action.
     *
     * @param systemPrompt system instructions
     * @param userContext  world state + history
     * @return future completing with parsed Action, or Wait on any failure
     */
    public CompletableFuture<Action> requestAction(String systemPrompt, String userContext) {
        Objects.requireNonNull(systemPrompt, "systemPrompt");
        Objects.requireNonNull(userContext, "userContext");

        try {
            String endpoint = apiEndpoint.endsWith("/") ? apiEndpoint.substring(0, apiEndpoint.length() - 1) : apiEndpoint;
            URI uri = URI.create(endpoint + CHAT_PATH);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("temperature", 0.7);
            body.addProperty("max_tokens", 150);

            com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
            {
                JsonObject systemMsg = new JsonObject();
                systemMsg.addProperty("role", "system");
                systemMsg.addProperty("content", systemPrompt);
                messages.add(systemMsg);
            }
            {
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userContext);
                messages.add(userMsg);
            }
            body.add("messages", messages);

            String jsonBody = GSON.toJson(body);

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (!apiKey.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }

            HttpRequest request = reqBuilder.build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        int status = response.statusCode();
                        if (status != 200) {
                            AicompagnonMod.plugin.getLogger().warning("AI API returned status " + status + ": " + response.body());
                            return (Action) Action.waitAction("http_" + status);
                        }
                        try {
                            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                            String content = json.getAsJsonArray("choices")
                                    .get(0).getAsJsonObject()
                                    .getAsJsonObject("message")
                                    .get("content").getAsString();
                            return ActionParser.parse(content);
                        } catch (Exception e) {
                            AicompagnonMod.plugin.getLogger().warning("Failed to parse AI response: " + e.getMessage());
                            return Action.waitAction("parse_failed");
                        }
                    })
                    .exceptionally(ex -> {
                        AicompagnonMod.plugin.getLogger().warning("AI API call failed: " + ex.getMessage());
                        return Action.waitAction("api_timeout");
                    });
        } catch (Exception e) {
            AicompagnonMod.plugin.getLogger().warning("Failed to build AI request: " + e.getMessage());
            return CompletableFuture.completedFuture(Action.waitAction("request_build_failed"));
        }
    }
}
