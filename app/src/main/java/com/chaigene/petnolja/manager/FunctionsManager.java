package com.chaigene.petnolja.manager;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.functions.HttpsCallableResult;
import com.chaigene.petnolja.model.HashtagLog;

import java.util.HashMap;
import java.util.Map;

public class FunctionsManager {

    public static final String TAG = "FunctionsManager";

    public static FirebaseFunctions getFunctions() {
        return FirebaseFunctions.getInstance();
    }

    private Task<String> addMessage(String text) {
        // Create the arguments to the callable function, which is just one string
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);

        return getFunctions().getHttpsCallable("addMessage").call(data).continueWith((task) -> {
            // This continuation runs on either success or failure, but if the task
            // has failed then getResult() will throw an Exception which will be
            // propagated down.

            String result = (String) task.getResult().getData();
            return result;
        });


    }


    public static Task<String> hello(String text) {
        // Create the arguments to the callable function, which is just one string
        Map<String, Object> data = new HashMap<>();
        data.put("text", text);

        return getFunctions()
                .getHttpsCallable("article-hello")
                .call(data)
                .continueWith(new Continuation<HttpsCallableResult, String>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public String then(@NonNull Task<HttpsCallableResult> task) throws Exception {

//                        DocumentSnapshot documentSnapshot = (DocumentSnapshot) task.getResult().getData();
//                        Post post = documentSnapshot.toObject(Post.class);
//
//                        QuerySnapshot querySnapshot = (QuerySnapshot) task.getResult().getData();
//                        List<DocumentSnapshot> documentSnapshots = querySnapshot.getDocuments();

                        // return result;

                        Log.d(TAG, "call:result:" + task.getResult().getData());

                        // Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        
                        QueryDocumentSnapshot documentSnapshot = (QueryDocumentSnapshot) task.getResult().getData();
                        Log.d(TAG, "call:documentSnapshot:" + documentSnapshot.getId());

                        HashtagLog hashtagLog = documentSnapshot.toObject(HashtagLog.class);

                        Log.d(TAG, "call:hashtagLog:" + hashtagLog.toMap());

                        String result = "done!";
                        return result;
                    }
                }).addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e instanceof FirebaseFunctionsException) {
                                FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                FirebaseFunctionsException.Code code = ffe.getCode();
                                Object details = ffe.getDetails();
                                Log.w(TAG, "ERROR:code:" + code + "|detail:" + details, ffe);
                            }

                            // ...
                        }

                        // ...
                    }
                });
    }
}