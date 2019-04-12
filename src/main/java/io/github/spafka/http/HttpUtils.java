package io.github.spafka.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author spafka
 * add okhttp
 * <p>只实现了httpget 和 application/json 提交的方式</P>
 */

@Slf4j
public class HttpUtils {


    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS).build();

    public static String doGet(String url) {

        Request request = new Request.Builder()
                .url(url)
                .build();
        String result = "";
        try {
            Response response = HttpUtils.client.newCall(request).execute();
            result = response.body().string();
        } catch (IOException e) {
        }
        return result;
    }

    public static void doGetAsync(String url) {

        Request request = new Request.Builder()
                .url(url)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("url call Fail");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.error(response.body().string());

            }
        });

    }


    public static String doPost(String url, String json) {
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

        }

        return result;
    }


    public static void doPostAsync(String url, String json) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("url call Fail");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.info(response.body().string());

            }
        });

    }


}
