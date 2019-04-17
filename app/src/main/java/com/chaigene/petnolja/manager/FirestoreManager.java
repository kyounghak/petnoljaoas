package com.chaigene.petnolja.manager;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.model.STRObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FirestoreManager {
    public static String TAG = "FirestoreManager";

    private static volatile FirestoreManager mInstance;

    private volatile ExecutorService mDefaultExecutor;
    private volatile boolean mCancelled;

    public boolean isCancelled() {
        return mCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.mCancelled = cancelled;
    }

    public static FirestoreManager getInstance() {
        if (mInstance == null) mInstance = new FirestoreManager();
        return mInstance;
    }

    public static void releaseInstance() {
        if (mInstance != null) {
            mInstance.release();
            mInstance = null;
        }
    }

    private void release() {
        // this.mCachedPosts = null;
        this.mDefaultExecutor.shutdown();
        this.mDefaultExecutor = null;
    }

    public void cancel() {
        setCancelled(true);
    }

    public void shutdown() {
        // this.mDefaultExecutor.shutdown();
        // this.mDefaultExecutor.shutdownNow();
        // this.mDefaultExecutor = null;
        // this.mFirestore = null;
    }

    private FirestoreManager() {
        // this.mCachedPosts = new ArrayList<>();
        // this.mDefaultExecutor = Executors.newSingleThreadExecutor();
        this.mDefaultExecutor = getExecutor();
    }

    private boolean connected;

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public static FirebaseFirestore getFirestore() {
        return FirebaseFirestore.getInstance();
    }

    public ExecutorService getExecutor() {
        if (mDefaultExecutor == null || mDefaultExecutor.isShutdown() || mDefaultExecutor.isTerminated()) {
            // Create a new ThreadPoolExecutor with 2 threads for each processor on the
            // device and a 60 second keep-alive time.
            int numCores = Runtime.getRuntime().availableProcessors();
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    numCores * 2,
                    numCores * 2,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>()
            );
            mDefaultExecutor = executor;
        }
        return mDefaultExecutor;
    }

    // User
    public static CollectionReference getUsersRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_USERS);
    }

    // Event
    public static CollectionReference getEventsRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_EVENTS);
    }

    /*public static DatabaseReference getUserUsersRef() {
        return getUserRef().child(Constants.DATABASE_PATH_USER_USERS);
    }*/

    /*public static DatabaseReference getUserFollowRef() {
        return getUserRef().child(Constants.DATABASE_PATH_USER_FOLLOW);
    }*/

    /*public static DatabaseReference getUserFollowFollowersRef() {
        return getUserFollowRef().child(Constants.DATABASE_PATH_USER_FOLLOW_FOLLOWERS);
    }*/

    /*public static DatabaseReference getUserFollowFollowingsRef() {
        return getUserFollowRef().child(DATABASE_PATH_USER_FOLLOW_FOLLOWINGS);
    }*/

    /*public static DatabaseReference getUserPrivateInfosRef() {
        return getUserRef().child(DATABASE_PATH_USER_PRIVATE_INFOS);
    }*/

    // Requests
    public static CollectionReference getRequestsRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_REQUESTS);
    }

    public static DocumentReference getRequestRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_REQUESTS).document();
    }

    public static CollectionReference getUserPrivateInfosRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_USER_PRIVATE_INFOS);
    }

    public static CollectionReference getHotelBookingQuotasRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_HOTEL_BOOKING_QUOTAS);
    }

    public static CollectionReference getCountersRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_COUNTERS);
    }

    public static DocumentReference getCountersOrderNoRef() {
        return getFirestore()
                .collection(Constants.DATABASE_PATH_COUNTERS)
                .document(Constants.DATABASE_PATH_COUNTERS_ORDER_NO);
    }

    public static DocumentReference getCountersBillingKeyNoRef() {
        return getFirestore()
                .collection(Constants.DATABASE_PATH_COUNTERS)
                .document(Constants.DATABASE_PATH_COUNTERS_BILLING_KEY_NO);
    }

    public static CollectionReference getNotiStableTokensRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_NOTI_STABLE_TOKENS);
    }

    public static CollectionReference getShopOrdersRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_SHOP_ORDERS);
    }

    public static CollectionReference getRequestShopOrdersRef(DocumentReference requestRef) {
        return requestRef.collection(Constants.DATABASE_PATH_REQUEST_SHOP_ORDERS);
    }

    public static CollectionReference getResponseShopOrdersRef(DocumentReference requestRef) {
        return requestRef.collection(Constants.DATABASE_PATH_RESPONSE_SHOP_ORDERS);
    }

    public static CollectionReference getShopCardsRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_SHOP_CARDS);
    }

    public static CollectionReference getRequestShopCardsRef(DocumentReference requestRef) {
        return requestRef.collection(Constants.DATABASE_PATH_REQUEST_SHOP_CARDS);
    }

    public static CollectionReference getResponseShopCardsRef(DocumentReference requestRef) {
        return requestRef.collection(Constants.DATABASE_PATH_RESPONSE_SHOP_CARDS);
    }

    public static CollectionReference getShopSecurityPinsRef() {
        return getFirestore().collection(Constants.DATABASE_PATH_SHOP_SECURITY_PINS);
    }

    public Task<Void> set(DocumentReference docRef, Map<String, Object> map) {
        // Log.i(TAG, "set:docRef:" + docRef.toString());
        return set(docRef, map, null);
    }

    public Task<Void> set(DocumentReference docRef, Map<String, Object> map, SetOptions setOptions) {
        Log.i(TAG, "set:docRef:" + docRef.getPath());
        setCancelled(false);
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        Task<Void> setTask = setOptions != null ? docRef.set(map, setOptions) : docRef.set(map);
        setTask.addOnCompleteListener(task -> {
            if (isCancelled()) {
                tcs.setException(getCancelledException());
                return;
            }
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                Log.w(TAG, "set:ERROR:" + e.getMessage());
                tcs.setException(task.getException());
            }
            Log.d(TAG, "set:SUCCESS");
            tcs.setResult(null);
        });
        return tcs.getTask();
    }

    public Task<Void> set(DocumentReference docRef, Object o) {
        // Log.i(TAG, "set:docRef:" + docRef.toString());
        return set(docRef, o, null);
    }

    public Task<Void> set(DocumentReference docRef, Object o, SetOptions setOptions) {
        Log.i(TAG, "set:docRef:" + docRef.getPath());
        setCancelled(false);
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        Task<Void> setTask = setOptions != null ? docRef.set(o, setOptions) : docRef.set(o);
        setTask.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "set:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                }
                Log.d(TAG, "set:SUCCESS");
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> update(DocumentReference docRef, Map<String, Object> map) {
        Log.i(TAG, "update:docRef:" + docRef.getPath() + "|map:" + map);
        setCancelled(false);
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        docRef.update(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "update:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                }
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    public static DocumentReference document(CollectionReference colRef) {
        DocumentReference docRef = colRef.document();
        Log.i(TAG, "document:colRef:" + colRef.getPath() + "|docRef:" + docRef.getId());
        return docRef;
    }

    public Task<DocumentReference> add(CollectionReference colRef, Object o) {
        Log.i(TAG, "add:colRef:" + colRef.toString());
        setCancelled(false);
        final TaskCompletionSource<DocumentReference> tcs = new TaskCompletionSource<>();
        colRef.add(o).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "add:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                    return;
                }
                DocumentReference docRef = task.getResult();
                Log.d(TAG, "add:SUCCESS:" + docRef.getId());
                tcs.setResult(docRef);
            }
        });
        return tcs.getTask();
    }

    public Task<Void> delete(DocumentReference docRef) {
        Log.i(TAG, "delete:docRef:" + docRef.toString());
        setCancelled(false);
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        docRef.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "delete:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                }
                Log.d(TAG, "delete:SUCCESS");
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    public static WriteBatch batch() {
        WriteBatch writeBatch = getFirestore().batch();
        return writeBatch;
    }

    public Task<Void> commit(WriteBatch batch) {
        Log.i(TAG, "commit");
        setCancelled(false);
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "commit:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                    return;
                }
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    public <X extends STRObject> X toObject(DocumentSnapshot document, Class<X> aClass) {
        X model = document.toObject(aClass);
        model.setId(document.getId());
        model.setDocumentSnapshot(document);
        return model;
    }

    public <X extends STRObject> Task<X> get(DocumentReference docRef, final Class<X> aClass) {
        // Log.i(TAG, "get:docRef:" + docRef.getPath());
        setCancelled(false);
        return get(docRef).continueWith(getExecutor(), new Continuation<DocumentSnapshot, X>() {
            @Override
            public X then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                handleCancelled();
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    // Log.w(TAG, "get:ERROR:" + e.getMessage());
                    throw e;
                }
                DocumentSnapshot document = task.getResult();
                // Log.d(TAG, "get:SUCCESS:" + (document.exists() ? document.getId() + " => " + document.getData() : null));
                if (!document.exists()) return null;
                X model = toObject(document, aClass);
                return model;
            }
        });
    }

    public Task<DocumentSnapshot> get(DocumentReference docRef) {
        Log.i(TAG, "get:docRef:" + docRef.getPath());
        setCancelled(false);
        final TaskCompletionSource<DocumentSnapshot> tcs = new TaskCompletionSource<>();
        docRef.get().addOnCompleteListener(getExecutor(), new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "get:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                    return;
                }
                DocumentSnapshot docSnapshot = task.getResult();
                Log.d(TAG, "get:SUCCESS:" + (docSnapshot.exists() ? docSnapshot.getId() + " => " + docSnapshot.getData() : null));
                tcs.setResult(docSnapshot);
            }
        });
        return tcs.getTask();
    }

    /*public <X extends STRObject> Task<List<X>> get(final Query query, final Class<X> aClass) {
        Log.i(TAG, "get");
        final TaskCompletionSource<List<X>> tcs = new TaskCompletionSource<>();
        Tasks.call(getExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    Task<QuerySnapshot> getListTask = query.get();
                    QuerySnapshot querySnapshot = Tasks.await(getListTask);
                    if (!getListTask.isSuccessful()) {
                        Exception e = getListTask.getException();
                        Log.w(TAG, "get:ERROR:" + e.getMessage());
                        tcs.setException(getListTask.getException());
                        return null;
                    }
                    Log.d(TAG, "get:SUCCESS:" + querySnapshot.toString());
                    List<X> list = new ArrayList<>();
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "get:empty");
                        tcs.setResult(list);
                        return null;
                    }
                    for (DocumentSnapshot document : querySnapshot) {
                        Log.d(TAG, "get:loop:document:" + document.getId() + "=>" + document.getData());
                        // X model = document.toObject(aClass);
                        X model = toObject(document, aClass);
                        list.add(model);
                    }
                    tcs.setResult(list);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    tcs.setException(e);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    tcs.setException(e);
                }
                return null;
            }
        });
        return tcs.getTask();
    }*/

    /*public <X extends STRObject> Task<List<X>> get(Query query, final Class<X> aClass) {
        Log.i(TAG, "get");
        final TaskCompletionSource<List<X>> tcs = new TaskCompletionSource<>();
        this.mListener = new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "get:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                }
                QuerySnapshot querySnapshot = task.getResult();
                Log.d(TAG, "get:SUCCESS:" + querySnapshot.toString());
                List<X> list = new ArrayList<>();
                if (querySnapshot.isEmpty()) {
                    Log.d(TAG, "get:empty");
                    tcs.setResult(list);
                    return;
                }
                for (DocumentSnapshot document : querySnapshot) {
                    Log.d(TAG, "get:loop:document:" + document.getId() + "=>" + document.getData());
                    // X model = document.toObject(aClass);
                    X model = toObject(document, aClass);
                    list.add(model);
                }
                // Collections.reverse(list);
                tcs.setResult(list);
            }
        };
        get(query).addOnCompleteListener(getExecutor(), mListener);
        return tcs.getTask();
    }*/

    public <X extends STRObject> Task<List<X>> get(Query query, final Class<X> aClass) {
        Log.i(TAG, "get");
        setCancelled(false);
        return get(query).continueWith(getExecutor(), new Continuation<QuerySnapshot, List<X>>() {
            @Override
            public List<X> then(@NonNull Task<QuerySnapshot> task) throws Exception {
                handleCancelled();
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "get:ERROR:" + e.getMessage());
                    throw e;
                }
                QuerySnapshot querySnapshot = task.getResult();
                Log.d(TAG, "get:SUCCESS:" + querySnapshot.getDocuments());
                List<X> list = new ArrayList<>();
                if (querySnapshot.isEmpty()) {
                    Log.d(TAG, "get:empty");
                    return list;
                }
                for (DocumentSnapshot document : querySnapshot) {
                    Log.d(TAG, "get:loop:document:" + document.getId() + "=>" + document.getData());
                    // X model = document.toObject(aClass);
                    X model = toObject(document, aClass);
                    list.add(model);
                }
                // Collections.reverse(list);
                return list;
            }
        });
        /*colRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "get:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                }
                QuerySnapshot querySnapshot = task.getResult();
                Log.d(TAG, "get:SUCCESS:" + querySnapshot.toString());
                List list = new ArrayList<>();
                if (!querySnapshot.isEmpty()) {
                    Log.d(TAG, "get:empty");
                    tcs.setResult(list);
                    return;
                }
                for (DocumentSnapshot document : querySnapshot) {
                    Log.d(TAG, "get:loop:document:" + document.getId() + "=>" + document.getData());
                    X model = document.toObject(aClass);
                    list.add(model);
                }
                Collections.reverse(list);
                tcs.setResult(list);
            }
        });*/
    }

    public Task<QuerySnapshot> get(Query colRef) {
        // Log.i(TAG, "get:colRef:" + colRef.toString());
        setCancelled(false);
        final TaskCompletionSource<QuerySnapshot> tcs = new TaskCompletionSource<>();
        colRef.get().addOnCompleteListener(getExecutor(), new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    Log.w(TAG, "get:ERROR:" + e.getMessage());
                    tcs.setException(task.getException());
                }
                QuerySnapshot querySnapshot = task.getResult();
                Log.d(TAG, "get:SUCCESS:" + querySnapshot.getDocuments());
                tcs.setResult(querySnapshot);
            }
        });
        return tcs.getTask();
    }

    public <X> Task<X> runTransaction(Transaction.Function<X> function) {
        setCancelled(false);
        final TaskCompletionSource<X> tcs = new TaskCompletionSource<>();
        getFirestore().runTransaction(function).continueWith(new Continuation<X, X>() {
            @Override
            public X then(@NonNull Task<X> task) throws Exception {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return null;
                }
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    tcs.setException(e);
                    return null;
                }
                X result = task.getResult();
                tcs.setResult(result);
                return null;
            }
        });
        return tcs.getTask();
    }

    public <X extends STRObject> Task<X> request(final DocumentReference requestRef,
                                                 final DocumentReference responseRef,
                                                 final Object o,
                                                 final Class<X> aClass,
                                                 final Cancellable cancellable) {
        setCancelled(false);
        return request(requestRef, responseRef, o, cancellable).continueWith(new Continuation<DocumentSnapshot, X>() {
            @Override
            public X then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                handleCancelled();
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                DocumentSnapshot document = task.getResult();
                X model = toObject(document, aClass);
                return model;
            }
        });
    }

    // document() 메서드를 통해 미리 uid를 추출할 후 requestRef에 경로를 넘겨줘야한다.
    public Task<DocumentSnapshot> request(final DocumentReference requestRef,
                                          final DocumentReference responseRef,
                                          final Object o,
                                          final Cancellable cancellable) {
        Log.i(TAG, "request:" +
                "requestRef:" + requestRef.getPath() +
                "|responseRef:" + responseRef.getPath()
        );
        setCancelled(false);
        final TaskCompletionSource<DocumentSnapshot> tcs = new TaskCompletionSource<>();
        final AtomicBoolean initialValue = new AtomicBoolean(false);
        final AtomicReference<ListenerRegistration> atomicRegistration = new AtomicReference<>(null);
        ListenerRegistration registration = responseRef.addSnapshotListener(getExecutor(), new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(DocumentSnapshot documentSnapshot, FirebaseFirestoreException e) {
                if (isCancelled()) {
                    tcs.setException(getCancelledException());
                    return;
                }
                if (e != null) {
                    Log.w(TAG, "request:onEvent:ERROR:" + e.getMessage());
                    tcs.setException(e);
                    return;
                }
                // 리스너를 추가하면 무조건 최초의 값이 한번 호출된다.
                if (!initialValue.get()) {
                    Log.w(TAG, "request:onEvent:initial:" + (documentSnapshot.exists() ? documentSnapshot.getData() : null));
                    initialValue.set(true);
                    set(requestRef, o).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            handleCancelled();
                            if (!task.isSuccessful()) {
                                Exception queryError = task.getException();
                                Log.w(TAG, "request:setTask:ERROR:" + queryError.getMessage());
                                tcs.setException(queryError);
                            }
                            Log.d(TAG, "request:setTask:SUCCESS");
                            return null;
                        }
                    });
                    return;
                }
                Log.d(TAG, "request:onEvent:response:" + (documentSnapshot.exists() ? documentSnapshot.getData() : null));
                atomicRegistration.get().remove();
                tcs.setResult(documentSnapshot);
            }
        });
        atomicRegistration.set(registration);
        return tcs.getTask();
    }

    public static class Cancellable {
        private AtomicBoolean canceled;

        public Cancellable() {
            canceled = new AtomicBoolean(false);
        }

        public void cancel() {
            canceled.set(true);
        }

        public boolean isCanceled() {
            return canceled.get();
        }

    }

    /**
     * Delete all documents in a collection. Uses an Executor to perform work on a background
     * thread. This does *not* automatically discover and delete subcollections.
     */
    private Task<Void> deleteCollection(final CollectionReference collection,
                                        final int batchSize) {

        // Perform the delete operation on the provided Executor, which allows us to use
        // simpler synchronous logic without blocking the main thread.
        return Tasks.call(mDefaultExecutor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Get the first batch of documents in the collection
                Query query = collection.orderBy(FieldPath.documentId()).limit(batchSize);

                // Get a list of deleted documents
                List<DocumentSnapshot> deleted = deleteQueryBatch(query);

                // While the deleted documents in the last batch indicate that there
                // may still be more documents in the collection, page down to the
                // next batch and delete again
                while (deleted.size() >= batchSize) {
                    // Move the query cursor to start after the last doc in the batch
                    DocumentSnapshot last = deleted.get(deleted.size() - 1);
                    query = collection.orderBy(FieldPath.documentId()).startAfter(last.getId()).limit(batchSize);

                    deleted = deleteQueryBatch(query);
                }

                return null;
            }
        });

    }

    /**
     * Delete all results from a query in a single WriteBatch. Must be run on a worker thread
     * to avoid blocking/crashing the main thread.
     */
    @WorkerThread
    private List<DocumentSnapshot> deleteQueryBatch(final Query query) throws Exception {
        QuerySnapshot querySnapshot = Tasks.await(query.get());

        WriteBatch batch = query.getFirestore().batch();
        for (DocumentSnapshot snapshot : querySnapshot) {
            batch.delete(snapshot.getReference());
        }
        Tasks.await(batch.commit());

        return querySnapshot.getDocuments();
    }

    private void handleCancelled() throws FirebaseFirestoreException {
        if (isCancelled()) throw getCancelledException();
    }

    private FirebaseFirestoreException getCancelledException() {
        return new FirebaseFirestoreException("The operation has cancelled.", FirebaseFirestoreException.Code.CANCELLED);
    }
}