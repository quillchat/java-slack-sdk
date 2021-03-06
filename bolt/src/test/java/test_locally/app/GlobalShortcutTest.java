package test_locally.app;

import com.google.gson.Gson;
import com.slack.api.Slack;
import com.slack.api.SlackConfig;
import com.slack.api.app_backend.SlackSignature;
import com.slack.api.app_backend.interactive_components.payload.GlobalShortcutPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.request.RequestHeaders;
import com.slack.api.bolt.request.builtin.GlobalShortcutRequest;
import com.slack.api.bolt.response.Response;
import com.slack.api.util.json.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import util.AuthTestMockServer;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

@Slf4j
public class GlobalShortcutTest {

    AuthTestMockServer server = new AuthTestMockServer();
    SlackConfig config = new SlackConfig();
    Slack slack = Slack.getInstance(config);

    @Before
    public void setup() throws Exception {
        server.start();
        config.setMethodsEndpointUrlPrefix(server.getMethodsEndpointPrefix());
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
    }

    final Gson gson = GsonFactory.createSnakeCase();
    final String secret = "foo-bar-baz";
    final SlackSignature.Generator generator = new SlackSignature.Generator(secret);

    String realPayload = "{\"type\":\"shortcut\",\"token\":\"legacy-fixed-value\",\"action_ts\":\"123.123\",\"team\":{\"id\":\"T123\",\"domain\":\"seratch-test\"},\"user\":{\"id\":\"U123\",\"username\":\"seratch\",\"team_id\":\"T123\"},\"callback_id\":\"test-global-shortcut\",\"trigger_id\":\"123.123.123\"}\n";

    @Test
    public void withPayload() throws Exception {
        App app = buildApp();
        app.globalShortcut("test-global-shortcut", (req, ctx) -> ctx.ack());

        String requestBody = "payload=" + URLEncoder.encode(realPayload, "UTF-8");

        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(requestBody, rawHeaders, timestamp);

        GlobalShortcutRequest req = new GlobalShortcutRequest(requestBody, realPayload, new RequestHeaders(rawHeaders));
        assertEquals("seratch", req.getPayload().getUser().getUsername()); // a bit different from message_actions payload

        Response response = app.run(req);
        assertEquals(200L, response.getStatusCode().longValue());
    }

    @Test
    public void handled() throws Exception {
        App app = buildApp();
        app.globalShortcut("callback$@+*", (req, ctx) -> ctx.ack());

        GlobalShortcutPayload payload = buildPayload();

        String p = gson.toJson(payload);
        String requestBody = "payload=" + URLEncoder.encode(p, "UTF-8");

        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(requestBody, rawHeaders, timestamp);

        GlobalShortcutRequest req = new GlobalShortcutRequest(requestBody, p, new RequestHeaders(rawHeaders));
        Response response = app.run(req);
        assertEquals(200L, response.getStatusCode().longValue());
    }

    @Test
    public void regexp() throws Exception {
        App app = buildApp();
        app.globalShortcut(Pattern.compile("callback.+$"), (req, ctx) -> ctx.ack());

        GlobalShortcutPayload payload = buildPayload();

        String p = gson.toJson(payload);
        String requestBody = "payload=" + URLEncoder.encode(p, "UTF-8");

        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(requestBody, rawHeaders, timestamp);

        GlobalShortcutRequest req = new GlobalShortcutRequest(requestBody, p, new RequestHeaders(rawHeaders));
        Response response = app.run(req);
        assertEquals(200L, response.getStatusCode().longValue());
    }

    @Test
    public void unhandled() throws Exception {
        App app = buildApp();
        app.globalShortcut("callback$@+*", (req, ctx) -> ctx.ack());

        GlobalShortcutPayload payload = buildPayload();
        payload.setCallbackId("unexpected-callback$@+*");

        String p = gson.toJson(payload);
        String requestBody = "payload=" + URLEncoder.encode(p, "UTF-8");

        Map<String, List<String>> rawHeaders = new HashMap<>();
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        setRequestHeaders(requestBody, rawHeaders, timestamp);

        GlobalShortcutRequest req = new GlobalShortcutRequest(requestBody, p, new RequestHeaders(rawHeaders));
        Response response = app.run(req);
        assertEquals(404L, response.getStatusCode().longValue());
    }

    App buildApp() {
        return new App(AppConfig.builder()
                .signingSecret(secret)
                .singleTeamBotToken(AuthTestMockServer.ValidToken)
                .slack(slack)
                .build());
    }

    void setRequestHeaders(String requestBody, Map<String, List<String>> rawHeaders, String timestamp) {
        rawHeaders.put(SlackSignature.HeaderNames.X_SLACK_REQUEST_TIMESTAMP, Arrays.asList(timestamp));
        rawHeaders.put(SlackSignature.HeaderNames.X_SLACK_SIGNATURE, Arrays.asList(generator.generate(timestamp, requestBody)));
    }

    GlobalShortcutPayload buildPayload() {
        GlobalShortcutPayload.Team team = new GlobalShortcutPayload.Team();
        team.setId("T123");
        GlobalShortcutPayload.User user = new GlobalShortcutPayload.User();
        team.setId("U123");
        GlobalShortcutPayload payload = GlobalShortcutPayload.builder()
                .callbackId("callback$@+*")
                .triggerId("xxxx")
                .team(team)
                .user(user)
                .build();
        return payload;
    }

}
