package com.chaigene.petnolja.net;

import android.content.Context;

import com.android.volley.toolbox.HurlStack;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class OkHttpStack extends HurlStack {
    private final OkUrlFactory mFactory;
    //    private final OkHttpClient client;

    private static String[] PINS = new String[]{"4da198c95b5f423693dd121cd3fe2f7f938209dc"};

    public OkHttpStack(Context context) {
        // Volley가 cache를 관리하고 OkHttpClient에서 하고 싶지 않으면 setCache(null)를 적용해야 한다
        // With SSLSocketFactory (Ref: http://stackoverflow.com/a/24007536/4729203)
        this(new OkHttpClient()
                        .setCache(null)
//                        .setSslSocketFactory(new Pinning(context).getPinnedCertSSLSocketFactory())
        );

        // With CertificatePinner (Ref: https://square.github.io/okhttp/javadoc/com/squareup/okhttp/CertificatePinner.html)
//        this(new OkHttpClient()
//                        .setCache(null)
//                        .setCertificatePinner(
//                                new CertificatePinner.Builder().add("", "").build()
//                        )
//        );
    }

    public OkHttpStack(OkHttpClient client) {
        if (client == null) {
            throw new NullPointerException("Client must not be null.");
        }
        mFactory = new OkUrlFactory(client);
//        this.client = client;
    }

    @Override
    protected HttpURLConnection createConnection(URL url) throws IOException {
        return mFactory.open(url);
    }

//    @Override protected HttpURLConnection createConnection(URL url) throws IOException {
//        return client.open(url);
//    }

}