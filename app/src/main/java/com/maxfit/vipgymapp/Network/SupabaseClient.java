package com.maxfit.vipgymapp.Network;

import android.util.Log;

import com.maxfit.vipgymapp.Config.SupabaseConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class SupabaseClient {
    private static final String TAG = "SupabaseClient";
    private static SupabaseClient instance;
    private OkHttpClient client;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private SupabaseClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
                            .header("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY)
                            .header("Content-Type", "application/json")
                            .header("Prefer", "return=representation")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    // Generic GET request
    public JSONArray select(String table, String filter) throws IOException {
        String url = SupabaseConfig.BASE_URL + table;
        if (filter != null && !filter.isEmpty()) {
            url += "?" + filter;
        }

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            return new JSONArray(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Error in select: " + e.getMessage());
            throw new IOException(e);
        }
    }

    // Generic INSERT request
    public JSONArray insert(String table, JSONObject data) throws IOException {
        String url = SupabaseConfig.BASE_URL + table;

        RequestBody body = RequestBody.create(data.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response + " - " + response.body().string());
            }
            String responseBody = response.body().string();
            return new JSONArray(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Error in insert: " + e.getMessage());
            throw new IOException(e);
        }
    }

    // Generic UPDATE request
    public JSONArray update(String table, String filter, JSONObject data) throws IOException {
        String url = SupabaseConfig.BASE_URL + table + "?" + filter;

        RequestBody body = RequestBody.create(data.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .patch(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            return new JSONArray(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Error in update: " + e.getMessage());
            throw new IOException(e);
        }
    }

    // Generic DELETE request
    public void delete(String table, String filter) throws IOException {
        String url = SupabaseConfig.BASE_URL + table + "?" + filter;

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in delete: " + e.getMessage());
            throw new IOException(e);
        }
    }

    // RPC (Remote Procedure Call) for stored functions
    public JSONArray rpc(String functionName, JSONObject params) throws IOException {
        String url = SupabaseConfig.SUPABASE_URL + "/rest/v1/rpc/" + functionName;

        RequestBody body = RequestBody.create(params.toString(), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            String responseBody = response.body().string();
            return new JSONArray(responseBody);
        } catch (Exception e) {
            Log.e(TAG, "Error in rpc: " + e.getMessage());
            throw new IOException(e);
        }
    }
}