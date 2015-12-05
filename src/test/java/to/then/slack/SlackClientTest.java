package to.then.slack;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class SlackClientTest {
    
    private AmazonS3 s3;
    private Client http;
    private SlackClient slack;
    
    @Before
    public void before() throws IOException {
        
        ObjectMapper mapper = new ObjectMapper();
        
        
        
        s3 = mock(AmazonS3.class);
        S3Object oauth = mock(S3Object.class);
        when(oauth.getObjectContent()).thenReturn(new S3ObjectInputStream(SlackClientTest.class.getResourceAsStream("oauth.json"), null));
        when(s3.getObject("bucket", "slack/oauth")).thenReturn(oauth);
        
        http = mock(Client.class);
        
        mock(Client.class);
        WebTarget target = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        when(target.request(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(builder);
        when(builder.post(any(Entity.class), any(Class.class))).thenReturn(mapper.readTree(SlackClientTest.class.getResourceAsStream("postMessage-result.json")));
        when(http.target("https://slack.com/api/chat.postMessage")).thenReturn(target);
        
        slack = new SlackClient(s3, http);
    }
    
    @Test
    public void postMessage() throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        slack.apply(SlackClientTest.class.getResourceAsStream("postMessage.json"), result, mock(Context.class));
        Assert.assertEquals("{\"ok\":true,\"channel\":\"C00001\",\"ts\":\"1449311659.000030\",\"message\":{\"text\":\"message\",\"username\":\"bot\",\"type\":\"message\",\"subtype\":\"bot_message\",\"ts\":\"1449311659.000030\"}}",
                new String(result.toByteArray()));
    }
    
    
}
