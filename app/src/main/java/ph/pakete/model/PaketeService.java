package ph.pakete.model;

import android.os.Build;
import android.support.v4.BuildConfig;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.realm.RealmObject;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;
import rx.Observable;

public interface PaketeService {

    @GET("couriers")
    Observable<List<Courier>> couriers();

    @GET("track")
    Observable<Package> track(@QueryMap Map<String, String> options);

    class Factory {
        public static PaketeService create() {
            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder()
                    .readTimeout(1, TimeUnit.MINUTES)
                    .writeTimeout(1, TimeUnit.MINUTES)
                    .addInterceptor(chain -> {
                        Request original = chain.request();

                        String user_agent = "Pakete/ph.pakete " + Build.FINGERPRINT + " (" + BuildConfig.VERSION_NAME + ")";
                        Request request = original.newBuilder()
                                .header("User-Agent", user_agent)
                                .header("Authorization", new Token().toString())
                                .method(original.method(), original.body())
                                .build();

                        return chain.proceed(request);
                    });

            if(BuildConfig.DEBUG) {
                // enable logging for debug builds
                // set your desired log level
                HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
                logging.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClientBuilder.addInterceptor(logging);
            }

            // set GSON date formatter
            Gson gson = new GsonBuilder()
                    .setDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'.'SSS'Z'")
                    .setExclusionStrategies(new ExclusionStrategy() {
                        @Override
                        public boolean shouldSkipField(FieldAttributes f) {
                            return f.getDeclaringClass().equals(RealmObject.class);
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> clazz) {
                            return false;
                        }
                    })
                    .create();

            OkHttpClient httpClient = httpClientBuilder.build();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://pakete-api-staging.herokuapp.com/v1/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(httpClient)
                    .build();
            return retrofit.create(PaketeService.class);
        }
    }
}
