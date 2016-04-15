package ph.pakete.model;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.realm.RealmObject;
import ph.pakete.SelfSigningClientBuilder;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.RxJavaCallAdapterFactory;
import retrofit.http.GET;
import retrofit.http.QueryMap;
import rx.Observable;

public interface PaketeService {

    @GET("couriers")
    Observable<List<Courier>> couriers();

    @GET("track")
    Observable<Package> track(@QueryMap Map<String, String> options);

    class Factory {
        public static PaketeService create() {
            OkHttpClient httpClient = SelfSigningClientBuilder.createClient();
            httpClient.setReadTimeout(1, TimeUnit.MINUTES);
            httpClient.setWriteTimeout(1, TimeUnit.MINUTES);
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // set your desired log level
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.interceptors().add(logging);  // <-- this is the important line!
            httpClient.interceptors().add(chain -> {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .header("Authorization", new Token().toString())
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            });

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
