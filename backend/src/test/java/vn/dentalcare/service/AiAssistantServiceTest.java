package vn.dentalcare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AiAssistantServiceTest {
    @Test
    void postsJsonSymptomsAndParsesAssistantResponse() throws Exception {
        AtomicReference<String> receivedBody=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/analyze",exchange->{
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] response="{\"possible_concern\":\"Sensitivity\",\"suggested_service\":\"Dental examination\",\"explanation\":\"See a dentist.\",\"note\":\"Not a diagnosis.\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200,response.length);
            try(var output=exchange.getResponseBody()){output.write(response);}
        });
        server.start();
        try {
            AiAssistantService service=new AiAssistantService("http://127.0.0.1:"+server.getAddress().getPort(),new ObjectMapper());

            Map<?,?> result=service.analyze("Sensitive tooth with cold water");

            assertEquals("Sensitive tooth with cold water",new ObjectMapper().readTree(receivedBody.get()).get("symptoms").asText());
            assertEquals("Dental examination",result.get("suggested_service"));
        } finally {
            server.stop(0);
        }
    }
}
