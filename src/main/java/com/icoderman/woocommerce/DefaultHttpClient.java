package com.icoderman.woocommerce;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;

public class DefaultHttpClient implements HttpClient {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private CloseableHttpClient httpClient;
    private ObjectMapper mapper;

    public DefaultHttpClient() {
        this.httpClient = HttpClientBuilder.create().build();
        this.mapper = new ObjectMapper();
    }

    @Override
    public Map get(String url) {
        HttpGet httpGet = new HttpGet(url);
        return getEntityAndReleaseConnection(httpGet, Map.class);
    }

    @Override
    public List getAll(String url) {
        HttpGet httpGet = new HttpGet(url);
        return getEntityAndReleaseConnection(httpGet, List.class);
    }

    @Override
    public Map post(String url, Map<String, String> params, Map<String, Object> object) {
        List<NameValuePair> postParameters = getParametersAsList(params);
        HttpPost httpPost;
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameters(postParameters);
            httpPost = new HttpPost(uriBuilder.build());
            httpPost.setHeader(CONTENT_TYPE, APPLICATION_JSON);
            return postEntity(object, httpPost);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map put(String url, Map<String, String> params, Map<String, Object> object) {
        List<NameValuePair> postParameters = getParametersAsList(params);
        HttpPut httpPut;
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameters(postParameters);
            httpPut = new HttpPut(uriBuilder.build());
            httpPut.setHeader(CONTENT_TYPE, APPLICATION_JSON);
            return postEntity(object, httpPut);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map delete(String url, Map<String, String> params) {
        List<NameValuePair> postParameters = getParametersAsList(params);
        HttpDelete httpDelete;
        try {
            URIBuilder uriBuilder = new URIBuilder(url);
            uriBuilder.addParameters(postParameters);
            httpDelete = new HttpDelete(uriBuilder.build());
            return getEntityAndReleaseConnection(httpDelete, Map.class);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Map postEntity(Map<String, Object> objectForJson, HttpUriRequestBase httpPost) {
        try {
            HttpEntity entity = new ByteArrayEntity(this.mapper.writeValueAsBytes(objectForJson), ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            return getEntityAndReleaseConnection(httpPost, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<NameValuePair> getParametersAsList(Map<String, String> params) {
        List<NameValuePair> postParameters = new ArrayList<>();
        if (params != null && params.size() > 0) {
            for (String key : params.keySet()) {
                postParameters.add(new BasicNameValuePair(key, params.get(key)));
            }
        }
        return postParameters;
    }

    private <T> T getEntityAndReleaseConnection(HttpUriRequestBase httpRequest, Class<T> objectClass) {
        try (CloseableHttpResponse httpResponse = httpClient.execute(httpRequest)) {
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpEntity == null) {
                throw new RuntimeException("Error retrieving results from http request");
            }
            Object result = mapper.readValue(httpEntity.getContent(), Object.class);
            if (objectClass.isInstance(result)) {
                return objectClass.cast(result);
            }
            throw new RuntimeException("Can't parse retrieved object: " + result.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
