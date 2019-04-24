

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.github.spafka.http;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author spafka
 * add okhttp
 * <p>只实现了httpget 和 application/json 提交的方式</P>
 */
@Slf4j
public class OkHttpUtils {


    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS).build();

    public static String doGet(String baseUrl, @NonNull HashMap<String, Object> param) throws IOException {

        Iterator<Map.Entry<String, Object>> it = param.entrySet().iterator();

        StringBuffer buffer = new StringBuffer();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            Object key = entry.getKey();
            buffer.append(key);
            buffer.append('=');
            Object value = entry.getValue();
            buffer.append(value);
            if (it.hasNext()) {
                buffer.append("&");
            }
        }

        Request request = new Request.Builder()
                .url(baseUrl + buffer)
                .build();
        String result = "";
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                result = response.body().string();
            }
        } catch (IOException e) {
            log.error("{}", ExceptionUtils.getStackTrace(e));
            throw e;
        }
        return result;
    }


    public static String doGet(String url) throws IOException {


        return doGet(url, new HashMap<String, Object>());
    }

    public static void doGetAsync(String baseUrl, @NonNull HashMap<String, Object> param, @NonNull Callback callback) {

        Iterator<Map.Entry<String, Object>> it = param.entrySet().iterator();

        StringBuffer buffer = new StringBuffer();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            Object key = entry.getKey();
            buffer.append(key);
            buffer.append('=');
            Object value = entry.getValue();
            buffer.append(value);
            if (it.hasNext()) {
                buffer.append("&");
            }
        }

        Request request = new Request.Builder()
                .url(baseUrl + buffer)
                .build();
        client.newCall(request).enqueue(callback);

    }

    public static String doPost(String url, @NonNull String json) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        String result = "";

        try {
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                result = response.body().string();
            }
        } catch (IOException e) {
            log.error("e= {}", ExceptionUtils.getStackTrace(e));
        }

        return result;
    }

    public static void doPost(String url, String json, @NonNull Callback callback) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(callback);

    }

    public static void doPostAsync(String url, @NonNull String json) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("{}", ExceptionUtils.getStackTrace(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.info(response.body().string());

            }
        });

    }

    public static void doPostAsync(String url, @NonNull String json, @NonNull Callback callback) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();


        client.newCall(request).enqueue(callback);

    }


}
