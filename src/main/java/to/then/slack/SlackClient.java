package to.then.slack;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

public class SlackClient implements Function<JsonNode, JsonNode> {

    private final AmazonS3 s3;
    private final ObjectMapper mapper;
    private final Client client;
    
    public SlackClient() {
        this(new AmazonS3Client(), new ResteasyClientBuilder().httpEngine(
                new ApacheHttpClient4Engine(HttpClients.custom()
                        .setConnectionManager(new PoolingHttpClientConnectionManager())
                        .build()))
                .register(ResteasyJackson2Provider.class).build());
    }

    public SlackClient(AmazonS3 s3, Client client) {
        this.s3 = s3;
        this.client = client;
        mapper = new ObjectMapper();
    }

    public void apply(InputStream source, OutputStream result, Context context) throws IOException {
        JsonNode request = mapper.readTree(source);        
        if (request.size() != 1) {
            throw new IllegalArgumentException("Specify a single api method.");
        }
        mapper.writeValue(result, apply(request));
    }

    @Override
    public JsonNode apply(JsonNode request) {
        String methodName = request.fieldNames().next();
        JsonNode params = request.get(methodName);
        Form form = new Form();
        params.fieldNames().forEachRemaining((fieldName) -> {
            JsonNode valueNode = params.get(fieldName);
            if (valueNode.has("$ref")) {
                valueNode = resolveRef(valueNode.get("$ref").textValue());
            }
            form.param(fieldName, valueNode.asText());
        });
        WebTarget target = client.target(new StringBuilder().append("https://slack.com/api/").append(methodName).toString());
        Invocation.Builder builder = target.request(MediaType.APPLICATION_FORM_URLENCODED);
        JsonNode response = builder.post(Entity.form(form), JsonNode.class);
        return response;
    }
    
    private JsonNode resolveRef(String jsonReference) {
        int fragmentIndex = jsonReference.indexOf("#");
        String uri = jsonReference.substring(0, fragmentIndex);
        String pointer = jsonReference.substring(fragmentIndex + 1, jsonReference.length());
        AmazonS3URI s3Uri = new AmazonS3URI(uri);
        try (InputStream in = s3.getObject(s3Uri.getBucket(), s3Uri.getKey()).getObjectContent()) {
            JsonNode result = mapper.readTree(in);
            return result.at(pointer);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
