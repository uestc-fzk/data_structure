package com.fzk.web;

import okhttp3.*;

/**
 * @author zhike.feng
 * @datetime 2023-10-15 21:50:00
 */
public class OkHttpTest {

    public static void boot() {
        OkHttpClient client = new OkHttpClient.Builder().build();
        RequestBody requestBody = RequestBody.create(MediaType.get("application/json;charset=utf-8"), "{\"key\":\"5b0608cb3b41175bacd5a098576dd729\"}");
        Request request = new Request.Builder().
                url("https://fzk-tx.top/fs/api/download?key=5b0608cb3b41175bacd5a098576dd729").
                post(requestBody).
                build();
        try (Response response = client.newCall(request).execute()) {
            System.out.println(response);
//            System.out.println(response.body().string());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
