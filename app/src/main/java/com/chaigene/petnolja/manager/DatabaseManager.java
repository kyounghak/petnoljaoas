package com.chaigene.petnolja.manager;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.Constants;
import com.chaigene.petnolja.model.FIRObject;
import com.chaigene.petnolja.util.CommonUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.chaigene.petnolja.Constants.DATABASE_PATH_USER_FOLLOW_FOLLOWINGS;
import static com.chaigene.petnolja.Constants.DATABASE_PATH_USER_PRIVATE_INFOS;

public class DatabaseManager {
    public static String TAG = "DatabaseManager";

    private static volatile DatabaseManager mInstance;

    private volatile ExecutorService mDefaultExecutor;
    private volatile boolean mCancelled;

    public boolean isCancelled() {
        return mCancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.mCancelled = cancelled;
    }

    public static DatabaseManager getInstance() {
        if (mInstance == null) mInstance = new DatabaseManager();
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

    private DatabaseManager() {
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
                    new LinkedBlockingQueue<Runnable>()
            );
            mDefaultExecutor = executor;
        }
        return mDefaultExecutor;
    }

    public static void setPersistenceEnabled(boolean isEnabled) {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    public static DatabaseReference getRef() {
        return FirebaseDatabase.getInstance().getReference();
    }

    public static DatabaseReference getRef(String path) {
        return FirebaseDatabase.getInstance().getReference(path);
    }

    // OldUser
    @Deprecated
    public static DatabaseReference getUserRef() {
        return getRef().child(Constants.DATABASE_PATH_USER);
    }

    @Deprecated
    public static DatabaseReference getUserUsersRef() {
        return getUserRef().child(Constants.DATABASE_PATH_USER_USERS);
    }

    @Deprecated
    public static DatabaseReference getUserFollowRef() {
        return getUserRef().child(Constants.DATABASE_PATH_USER_FOLLOW);
    }

    @Deprecated
    public static DatabaseReference getUserFollowFollowersRef() {
        return getUserFollowRef().child(Constants.DATABASE_PATH_USER_FOLLOW_FOLLOWERS);
    }

    @Deprecated
    public static DatabaseReference getUserFollowFollowingsRef() {
        return getUserFollowRef().child(DATABASE_PATH_USER_FOLLOW_FOLLOWINGS);
    }

    @Deprecated
    public static DatabaseReference getUserPrivateInfosRef() {
        return getUserRef().child(DATABASE_PATH_USER_PRIVATE_INFOS);
    }

    // Search
    public static DatabaseReference getSearchRef() {
        return getRef().child(Constants.DATABASE_PATH_SEARCH);
    }

    public static DatabaseReference getSearchUsersRef() {
        return getSearchRef().child(Constants.DATABASE_PATH_SEARCH_USERS);
    }

    public static DatabaseReference getSearchUsersQueriesRef() {
        return getSearchUsersRef().child(Constants.DATABASE_PATH_SEARCH_USERS_QUERIES);
    }

    public static DatabaseReference getSearchUsersResultsRef() {
        return getSearchUsersRef().child(Constants.DATABASE_PATH_SEARCH_USERS_RESULTS);
    }

    public static DatabaseReference getSearchHashtagsRef() {
        return getSearchRef().child(Constants.DATABASE_PATH_SEARCH_HASHTAGS);
    }

    public static DatabaseReference getSearchHashtagsQueriesRef() {
        return getSearchHashtagsRef().child(Constants.DATABASE_PATH_SEARCH_HASHTAGS_QUERIES);
    }

    public static DatabaseReference getSearchHashtagsResultsRef() {
        return getSearchHashtagsRef().child(Constants.DATABASE_PATH_SEARCH_HASHTAGS_RESULTS);
    }

    // Notification
    public static DatabaseReference getNotificationRef() {
        return getRef().child(Constants.DATABASE_PATH_NOTIFICATION);
    }

    public static DatabaseReference getNotificationUserNotisRef() {
        return getNotificationRef().child(Constants.DATABASE_PATH_NOTIFICATION_USER_NOTIS);
    }

    public static DatabaseReference getNotificationUserNotisFeedRef() {
        return getNotificationRef().child(Constants.DATABASE_PATH_NOTIFICATION_USER_NOTIS_FEED);
    }

    public static DatabaseReference getNotificationUserNotisTalentRef() {
        return getNotificationRef().child(Constants.DATABASE_PATH_NOTIFICATION_USER_NOTIS_TALENT);
    }

    // Chat
    public static DatabaseReference getChatRef() {
        return getRef().child(Constants.DATABASE_PATH_CHAT);
    }

    public static DatabaseReference getChatHostRoomsRef() {
        return getChatRef().child(Constants.DATABASE_PATH_CHAT_HOST_ROOMS);
    }

    public static DatabaseReference getChatUserRoomsRef() {
        return getChatRef().child(Constants.DATABASE_PATH_CHAT_USER_ROOMS);
    }

    public static DatabaseReference getChatRoomsRef() {
        return getChatRef().child(Constants.DATABASE_PATH_CHAT_ROOMS);
    }

    // TODO: room id 하위에 Message 객체들이 저장되어야 한다.
    public static DatabaseReference getChatMessagesRef() {
        return getChatRef().child(Constants.DATABASE_PATH_CHAT_MESSAGES);
    }

    public static DatabaseReference getChatUserMessagesRef() {
        return getChatRef().child(Constants.DATABASE_PATH_CHAT_USER_MESSAGES);
    }

    public static DatabaseReference getChatLastReadedMessagesRef() {
        return getChatRef().child(Constants.DATABASE_PATH_CHAT_LAST_READED_MESSAGES);
    }

    public static DatabaseReference getChatTotalUnreadCountRef() {
        return getChatRef().child(Constants.DATABASE_PATH_CHAT_TOTAL_UNREAD_COUNT);
    }

    // Article
    public static DatabaseReference getArticleRef() {
        return getRef().child(Constants.DATABASE_PATH_ARTICLE);
    }

    public static DatabaseReference getArticlePostsRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_POSTS);
    }

    public static DatabaseReference getArticlePostsFeedRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_POSTS_FEED);
    }

    public static DatabaseReference getArticlePostsTalentRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_POSTS_TALENT);
    }

    public static DatabaseReference getArticleUserPostsRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_USER_POSTS);
    }

    public static DatabaseReference getArticleUserPostsFeedRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_USER_POSTS_FEED);
    }

    public static DatabaseReference getArticleUserPostsTalentRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_USER_POSTS_TALENT);
    }

    public static DatabaseReference getArticleUserSavesRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_USER_SAVES);
    }

    public static DatabaseReference getArticleTimelineRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_TIMELINE);
    }

    public static DatabaseReference getArticleHashtagsRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_HASHTAGS);
    }

    public static DatabaseReference getArticleHashtagCountRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_HASHTAG_COUNT);
    }

    public static DatabaseReference getArticleCommentsRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_COMMENTS);
    }

    public static DatabaseReference getArticleCommentCountRef() {
        return getArticleRef().child(Constants.DATABASE_PATH_ARTICLE_COMMENT_COUNT);
    }

    // Abuse
    public static DatabaseReference getAbuseRef() {
        return getRef().child(Constants.DATABASE_PATH_ABUSE);
    }

    public static DatabaseReference getAbuseAbusesRef() {
        return getAbuseRef().child(Constants.DATABASE_PATH_ABUSE_ABUSES);
    }

    public static Task<Void> setValue(DatabaseReference ref, Object o) {
        /*if (o instanceof String) {
            Log.i(TAG, "setValue:value:" + o);
        } else {
            Log.i(TAG, "setValue:value:" + o);
        }*/
        Log.i(TAG, "setValue:ref:" + (ref != null ? ref.toString() : "null"));
        return ref.setValue(o);
    }

    public static Task<Void> updateChildren(Map<String, Object> map) {
        Log.i(TAG, "updateChildren:map:" + map.toString());
        return getRef().updateChildren(map);
    }

    public static Task<Void> updateChildren(DatabaseReference ref, Map<String, Object> map) {
        Log.i(TAG, "updateChildren:map:" + map.toString());
        return ref.updateChildren(map);
    }

    public static Task<DataSnapshot> getValue(DatabaseReference ref) {
        final TaskCompletionSource<DataSnapshot> tcs = new TaskCompletionSource<>();
        Log.d(TAG, "getValue:ref:" + ref.toString());
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "getValue:onDataChange");
                tcs.setResult(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getValue:onCancelled:" + databaseError.getMessage());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    public static <X extends FIRObject> X getValue(final DataSnapshot snapshot, final Class<X> aClass) {
        // Log.i(TAG, "getValue");
        X firObject = snapshot.getValue(aClass);
        firObject.setKey(snapshot.getKey());
        return firObject;
    }

    public static <X extends FIRObject> X getValue(MutableData mutableData, String key, final Class<X> aClass) {
        // Log.i(TAG, "getValue");
        X firObject = mutableData.getValue(aClass);
        if (firObject == null) return null;
        firObject.setKey(key);
        return firObject;
    }

    public static <X extends FIRObject> Task<X> getValue(final DatabaseReference ref, final Class<X> aClass) {
        Log.d(TAG, "getValue:ref:" + ref.toString());

        final TaskCompletionSource<X> tcs = new TaskCompletionSource<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    NullDataException e = new NullDataException(
                            CommonUtil.format("%s (%s)", "The value for the path is null.", ref.toString())
                    );
                    Log.w(TAG, "getValue:onCancelled:error:" + e.getLocalizedMessage());
                    tcs.setException(e);
                    return;
                }

                X x = dataSnapshot.getValue(aClass);
                x.setKey(dataSnapshot.getKey());
                tcs.setResult(x);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getValue:onCancelled:error:" + databaseError.getMessage());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    /*public static Task<DataSnapshot> getValue(final Query query) {
        Log.i(TAG, "getValue:ref:" + query.getRef().toString());

        final TaskCompletionSource<DataSnapshot> tcs = new TaskCompletionSource<>();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d(TAG, "getValue:onDataChange");
                tcs.setResult(dataSnapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "getValue:onCancelled", databaseError.toException());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }*/

    // TODO: 10초가 넘으면 작업을 자동으로 취소시킨다.
    public static Task<DataSnapshot> getValue(final Query query) {
        Log.i(TAG, "getValue:ref:" + query.getRef().toString());

        final TaskCompletionSource<DataSnapshot> tcs = new TaskCompletionSource<>();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "getValue:onDataChange");
                // 어차피 DataSnapshot를 반환하기 때문에 현재 패스의 존재 유무는 사용하는 곳에서 처리하도록 한다.
                tcs.setResult(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getValue:onCancelled", databaseError.toException());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    public static <X extends FIRObject> Task<X> getValue(final Query query, final Class<X> aClass) {
        Log.i(TAG, "getValue:ref:" + query.toString());
        final TaskCompletionSource<X> tcs = new TaskCompletionSource<>();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "getValue:onDataChange:dataSnapshot:" + dataSnapshot.getValue());

                if (!dataSnapshot.exists()) {
                    NullDataException e = new NullDataException(
                            CommonUtil.format("%s (%s)", "The Value for the path is null.", query.toString())
                    );
                    Log.w(TAG, "getValue:onCancelled", e);
                    tcs.setException(e);
                    return;
                }

                tcs.setResult(getValue(dataSnapshot, aClass));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getValue:onCancelled", databaseError.toException());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    public static <X extends FIRObject> Task<X> getChildValue(final Query query, final Class<X> aClass) {
        Log.i(TAG, "getValue:ref:" + query.toString());
        final TaskCompletionSource<X> tcs = new TaskCompletionSource<>();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "getValue:onDataChange:dataSnapshot:" + dataSnapshot.getValue());
                if (!dataSnapshot.exists()) {
                    NullDataException e = new NullDataException(
                            CommonUtil.format("%s (%s)", "The Value for the path is null.", query.toString())
                    );
                    Log.w(TAG, "getValue:onCancelled", e);
                    tcs.setException(e);
                    return;
                }
                tcs.setResult(getValue(dataSnapshot.getChildren().iterator().next(), aClass));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getValue:onCancelled", databaseError.toException());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    public static Task<String> getString(final DatabaseReference ref) {
        Log.i(TAG, "getString:ref:" + ref.toString());
        final TaskCompletionSource<String> tcs = new TaskCompletionSource<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "getString:onDataChange:dataSnapshot:" + dataSnapshot.getValue());

                if (!dataSnapshot.exists()) {
                    NullDataException e = new NullDataException(
                            CommonUtil.format("%s (%s)", "The Value for the path is null.", ref.toString())
                    );
                    Log.w(TAG, "getString:onCancelled:" + e.getMessage());
                    tcs.setException(e);
                    return;
                }

                tcs.setResult(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getString:onCancelled:" + databaseError.toException().getMessage());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    public static Task<Boolean> getBoolean(final DatabaseReference ref) {
        // Log.i(TAG, "getBoolean:ref:" + ref.toString());
        final TaskCompletionSource<Boolean> tcs = new TaskCompletionSource<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Log.d(TAG, "getBoolean:onDataChange:dataSnapshot:" + dataSnapshot.getValue());

                if (!dataSnapshot.exists()) {
                    NullDataException e = new NullDataException(
                            CommonUtil.format("%s (%s)", "The Value for the path is null.", ref.toString())
                    );
                    Log.w(TAG, "getBoolean:onCancelled:error:" + e.getLocalizedMessage());
                    tcs.setException(e);
                    return;
                }

                tcs.setResult(dataSnapshot.getValue(Boolean.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getBoolean:onCancelled:error:" + databaseError.getMessage());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    public static Task<Integer> getInteger(final DatabaseReference ref) {
        // Log.i(TAG, "getInteger:ref:" + ref.toString());
        final TaskCompletionSource<Integer> tcs = new TaskCompletionSource<>();
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Log.d(TAG, "getInteger:onDataChange:dataSnapshot:" + dataSnapshot.getValue());

                if (!dataSnapshot.exists()) {
                    NullDataException e = new NullDataException(CommonUtil.format("%s (%s)", "The Value for the path is null.", ref.toString()));
                    Log.w(TAG, "getInteger:onCancelled:error:" + e.getLocalizedMessage());
                    tcs.setResult(0);
                    return;
                }

                tcs.setResult(dataSnapshot.getValue(Integer.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getInteger:onCancelled:error:" + databaseError.getMessage());
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    @Deprecated
    public static Task<Void> getTest() {
        return Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Cancellable cancellable = new Cancellable();
                Task task = getCancellableTask(getDelayValue(getRef()), cancellable);
                Object obj = Tasks.await(task, 10000, TimeUnit.MILLISECONDS);
                Exception e = task.getException();
                if (e != null & e instanceof TimeoutException) {
                    cancellable.cancel();
                }
                return null;
            }
        });
    }

    public static Task<DataSnapshot> getDelayValue(final DatabaseReference queryRef,
                                                   final DatabaseReference resultRef,
                                                   final Object o,
                                                   final Cancellable cancellable) {
        final TaskCompletionSource<DataSnapshot> tcs = new TaskCompletionSource<>();
        final AtomicBoolean initialValue = new AtomicBoolean(false);
        resultRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!initialValue.get()) {
                    Log.w(TAG, "getDelayValue:onDataChange:initial:" + dataSnapshot);
                    initialValue.set(true);
                    setValue(queryRef, o).continueWith(new Continuation<Void, Void>() {
                        @Override
                        public Void then(@NonNull Task<Void> task) throws Exception {
                            if (!task.isSuccessful()) {
                                // ERROR
                                Log.w(TAG, "getDelayValue:queryTask:ERROR", task.getException());
                                tcs.setException(task.getException());
                            }
                            Log.d(TAG, "getDelayValue:queryTask:SUCCESS");
                            return null;
                        }
                    });
                    return;
                }
                Log.d(TAG, "getDelayValue:onDataChange:result:" + dataSnapshot);
                resultRef.removeEventListener(this);
                tcs.setResult(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getDelayValue:onCancelled:", databaseError.toException());
                resultRef.removeEventListener(this);
                tcs.setException(databaseError.toException());
            }
        });
        return tcs.getTask();
    }

    public static Task<DataSnapshot> getDelayValue(final DatabaseReference ref) {
        Log.i(TAG, "getDelayValue");
        final TaskCompletionSource<DataSnapshot> tcs = new TaskCompletionSource<>();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        Tasks.call(executor, new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final AtomicBoolean initialValue = new AtomicBoolean(false);
                ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (!initialValue.get()) {
                            Log.w(TAG, "getDelayValue:inital:onDataChange:" + dataSnapshot);
                            initialValue.set(true);
                            return;
                        }
                        Log.d(TAG, "getDelayValue:result:onDataChange:" + dataSnapshot);
                        executor.shutdown();
                        ref.removeEventListener(this);
                        tcs.trySetResult(dataSnapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w(TAG, "getDelayValue:onCancelled:", databaseError.toException());
                        executor.shutdown();
                        ref.removeEventListener(this);
                        tcs.setException(databaseError.toException());
                    }
                });
                long timeout = 0;
                while (timeout < 10000) {
                    if (tcs.getTask().isComplete()) {
                        Log.d(TAG, "getDelayValue:looping:complete");
                        executor.shutdown();
                        return null;
                    }
                    Log.d(TAG, "getDelayValue:looping");
                    Thread.sleep(1000);
                    timeout += 1000;
                }
                Log.d(TAG, "getDelayValue:timeout");
                executor.shutdown();
                tcs.setException(new TimeoutException());
                return null;
            }
        });
        return tcs.getTask();
    }

    /*public static Task<DataSnapshot> getDelayValue(final DatabaseReference ref) {
        Log.i(TAG, "getDelayValue");
        final TaskCompletionSource<DataSnapshot> tcs = new TaskCompletionSource<>();
        final AtomicBoolean initialValue = new AtomicBoolean(false);
        final ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "getDelayValue:onDataChange:" + dataSnapshot);
                if (!initialValue.getUserPosts()) {
                    initialValue.set(true);
                    return;
                }
                ref.removeEventListener(this);
                tcs.setResult(dataSnapshot);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "getDelayValue:onCancelled:", databaseError.toException());
                ref.removeEventListener(this);
                tcs.setException(databaseError.toException());
            }
        };
        ref.addValueEventListener(listener);
        return tcs.getTask();
    }*/

    public static <TResult> Task<TResult> getDelayValue(final DatabaseReference ref, final Class<TResult> aClass) {
        final TaskCompletionSource<TResult> tcs = new TaskCompletionSource<>();
        final AtomicBoolean initialValue = new AtomicBoolean(false);
        final ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!initialValue.get()) {
                    initialValue.set(true);
                    return;
                }
                ref.removeEventListener(this);
                TResult tResult = dataSnapshot.getValue(aClass);
                tcs.setResult(tResult);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ref.removeEventListener(this);
                tcs.setException(databaseError.toException());
            }
        };
        ref.addValueEventListener(listener);
        return tcs.getTask();
    }

    @WorkerThread
    public static <TResult> Task<TResult> getCancellableTask(final Task<TResult> task, final Cancellable cancellable) {
        Log.i(TAG, "getCancellableTask");
        final TaskCompletionSource<TResult> tcs = new TaskCompletionSource<>();
        // final ExecutorService executor = Executors.newSingleThreadExecutor();
        Tasks.call(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                OnCompleteListener<TResult> listener = new OnCompleteListener<TResult>() {
                    final String TAG = "OnCompleteListener";

                    @Override
                    public void onComplete(@NonNull Task<TResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "onComplete:ERROR:" + task.getException());
                        }
                        Log.d(TAG, "onComplete:SUCCESS:");
                    }
                };
                task.addOnCompleteListener(listener);
                /*Continuation<TResult, Void> aaa = new Continuation<TResult, Void>() {
                    @Override
                    public Void then(@NonNull Task<TResult> task) throws Exception {
                        return null;
                    }
                };*/
                while (!cancellable.isCanceled()) {
                    Log.d(TAG, "getCancellableTask:looping");
                    if (task.isComplete()) {
                        // executor.shutdown();
                        tcs.setResult(null);
                        return null;
                    }
                    Thread.sleep(1000);
                }
                listener = null;
                // executor.shutdown();
                tcs.setException(new CancellationException());
                return null;
            }
        });
        return tcs.getTask();
    }

    /*public static Task getCancellableValue(DatabaseReference ref, final Cancellable cancellable) {
        Log.i(TAG, "getCancellableValue:ref:" + ref.toString());

        final TaskCompletionSource tcs = new TaskCompletionSource<>();
        Tasks.call(Executors.newSingleThreadExecutor(), new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Task<OldUser> userTask = UserUtil.getUser(AuthManager.getUserId());
                OnCompleteListener<OldUser> listener = new OnCompleteListener<OldUser>() {
                    @Override
                    public void onComplete(@NonNull Task<OldUser> task) {
                    }
                };
                userTask.addOnCompleteListener(listener);
                while (!cancellable.isCanceled()) {
                    if (userTask.isComplete()) {
                        tcs.setResult(null);
                        return null;
                    }
                    Thread.sleep(100);
                }
                listener = null;
                tcs.setException(new CancellationException());
                return null;
            }
        });
        return tcs.getTask();
    }*/

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

    public static Task<Integer> getIntAsync(final Cancellable cancellable) {
        final TaskCompletionSource<Integer> tcs = new TaskCompletionSource<>();
        new Thread() {
            @Override
            public void run() {
                if (cancellable.isCanceled()) {
                    tcs.setException(new CancellationException());
                    return;
                }
                int result = 0;
                while (result < 100) {
                    if (cancellable.isCanceled()) {
                        tcs.setException(new CancellationException());
                        return;
                    }
                    result++;
                }
                tcs.setResult(result);
            }
        }.start();
        return tcs.getTask();
    }

    public class CancellableTask implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {

            }
        }
    }

    public static Task<Void> runTransaction(DatabaseReference ref, final TransactionHandler handler) {
        Log.i(TAG, "runTransaction");
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        ref.runTransaction(new Transaction.Handler() {
            final String TAG = "Handler";

            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                // 이유는 모르겠지만 최초에 null이 한번 반환되고 두번째에 제대로 된 값이 반환된다.
                if (!mutableData.hasChildren()) return Transaction.success(mutableData);
                Log.i(TAG, "doTransaction:mutableData:" + mutableData.toString());
                return handler.doTransaction(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (!b) {
                    tcs.setException(databaseError.toException());
                }
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    public static Task<Void> runTransaction(DatabaseReference ref, final Transaction.Handler handler) {
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        ref.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                return handler.doTransaction(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (!b) {
                    tcs.setException(databaseError.toException());
                }
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    public interface TransactionHandler {
        Transaction.Result doTransaction(MutableData mutableData);
    }

    public static Task<Void> removeValue(DatabaseReference ref) {
        Log.i(TAG, "removeValue");
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();
        ref.removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError != null) {
                    Log.w(TAG, "removeValue:ERROR:" + databaseError.getMessage());
                    tcs.setException(databaseError.toException());
                }
                Log.d(TAG, "removeValue:SUCCESS");
                tcs.setResult(null);
            }
        });
        return tcs.getTask();
    }

    @Deprecated
    public static DatabaseReference getSignatureRef(String storageRef) {
        DatabaseReference ref = getRef()
                .child(Constants.DATABASE_PATH_SIGNATURES)
                .child(storageRef);
        Log.d(TAG, "getSignatureRef:" + ref.toString());
        return ref;
    }

    // 시그너쳐는 확장자를 무시하고 파일명을 기준으로만 저장된다. 따라서 동일한 파일명에 복수의 확장자를 가진 파일이 존재해서는 안된다.
    public static DatabaseReference getSignatureRef(StorageReference storageRef) {
        Log.i(TAG, "getSignatureRef:storageRef:" + storageRef.getPath());
        String path = storageRef.getPath();
        if (path.contains(".")) path = path.substring(0, path.lastIndexOf("."));
        DatabaseReference ref = getRef()
                .child(Constants.DATABASE_PATH_SIGNATURES)
                .child(path);
        // Log.d(TAG, "getSignatureRef:" + Constants.DATABASE_PATH_SIGNATURES + storageRef.getPath());
        return ref;
    }

    // 해당 이미지의 signature를 반환한다.
    public static Task<String> getSignature(DatabaseReference signatureRef) {
        Log.i(TAG, "getSignature:signatureRef:" + signatureRef.toString());
        return getString(signatureRef);
    }

    public static String getPath(DatabaseReference ref) {
        String path = "/" + ref.getKey();
        while (ref.getParent().getKey() != null) {
            ref = ref.getParent();
            path = "/" + ref.getKey() + path;
        }
        return path;
    }

    public static String getPath(DatabaseReference ref, String lastKey) {
        return CommonUtil.format("%s/%s", getPath(ref), lastKey);
    }

    public static boolean isLastDataReached(DataSnapshot snapshot, int requestedAmount) {
        int count = (int) snapshot.getChildrenCount();
        boolean isReached = count < requestedAmount;
        Log.d(TAG, "isLastDataReached:count:" + count + "/requestedLimit:" + requestedAmount + "/result:" + isReached);
        return isReached;
    }

    @Deprecated
    public static <X> LinkedHashMap<String, X> convertToMap(DataSnapshot dataSnapshot, Class<X> aClass) {
        LinkedHashMap<String, X> map = new LinkedHashMap<>();
        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
            map.put(snapshot.getKey(), snapshot.getValue(aClass));
        }
        return map;
    }

    public static class NullDataException extends DatabaseException {
        @SuppressLint("RestrictedApi")
        public NullDataException(String detailMessage) {
            super(detailMessage);
        }
    }

    private void handleCancelled() throws DatabaseException {
        if (isCancelled()) throw getCancelledException();
    }

    @SuppressLint("RestrictedApi")
    private DatabaseException getCancelledException() {
        // return new DatabaseException("The operation has cancelled.", FirebaseFirestoreException.Code.CANCELLED);
        // return DatabaseError.zzpm("write_canceled").toException();
        return DatabaseError.fromCode(DatabaseError.WRITE_CANCELED).toException();
    }
}
