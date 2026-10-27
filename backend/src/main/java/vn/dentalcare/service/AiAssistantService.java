package vn.dentalcare.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.dentalcare.exception.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiAssistantService {
    private static final Logger log=LoggerFactory.getLogger(AiAssistantService.class);
    private final URI endpoint;
    private final ObjectMapper json;

    public AiAssistantService(@Value("${app.ai.url}") String url,ObjectMapper json) {
        this.endpoint=URI.create(url.endsWith("/")?url+"analyze":url+"/analyze");
        this.json=json;
    }

    public Map<?,?> analyze(String symptoms) {
        HttpURLConnection connection=null;
        try {
            byte[] requestBody=json.writeValueAsBytes(Map.of("symptoms",symptoms));
            connection=(HttpURLConnection)new URL(endpoint.toString()).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(100000);
            connection.setRequestProperty("Content-Type","application/json; charset=UTF-8");
            connection.setFixedLengthStreamingMode(requestBody.length);
            connection.setDoOutput(true);
            try(var output=connection.getOutputStream()){output.write(requestBody);}

            int status=connection.getResponseCode();
            InputStream bodyStream=status>=400?connection.getErrorStream():connection.getInputStream();
            String responseBody=bodyStream==null?null:new String(bodyStream.readAllBytes(),StandardCharsets.UTF_8);
            if(status<200||status>=300) {
                log.warn("AI service returned HTTP {}",status);
                throw new ApiException(HttpStatus.BAD_GATEWAY,"Dịch vụ tư vấn AI trả về lỗi");
            }
            Map<String,Object> result=responseBody==null?null:json.readValue(responseBody,new TypeReference<HashMap<String,Object>>(){});
            if(result==null||!result.containsKey("possible_concern")||!result.containsKey("suggested_service"))
                throw new ApiException(HttpStatus.BAD_GATEWAY,"Dịch vụ tư vấn AI trả về dữ liệu không hợp lệ");
            return result;
        } catch(ApiException ex) {
            throw ex;
        } catch(JsonProcessingException ex) {
            log.error("AI service returned invalid JSON",ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY,"Dịch vụ tư vấn AI trả về dữ liệu không hợp lệ",ex);
        } catch(IOException ex) {
            log.warn("AI service request failed",ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Dịch vụ tư vấn triệu chứng hiện chưa khả dụng",ex);
        } finally {
            if(connection!=null) connection.disconnect();
        }
    }
}
