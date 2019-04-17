package com.chaigene.petnolja.manager;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static com.chaigene.petnolja.BuildConfig.STORAGE_BUCKET_TOKYO;
import static com.chaigene.petnolja.Constants.STORAGE_PATH_ARTICLE;
import static com.chaigene.petnolja.Constants.STORAGE_PATH_ARTICLE_POSTS;
import static com.chaigene.petnolja.Constants.STORAGE_PATH_EVENTS;
import static com.chaigene.petnolja.Constants.STORAGE_PATH_USER;

@SuppressWarnings("VisibleForTests")
public class StorageManager {
    public static final String TAG = "StorageManager";

    private static StorageManager instance;
    private FirebaseStorage mFirebaseStorage;

    /*public static synchronized StorageManager getInstance() {
        if (instance == null) instance = new StorageManager();
        return instance;
    }*/

    /*public static synchronized void releaseInstance() {
        if (instance != null) {
            instance.release();
            instance = null;
        }
    }*/

    /*private void release() {
         this.mFirebaseStorage = null;
    }*/

    /*public StorageManager() {
        this.mFirebaseStorage = FirebaseStorage.getInstance();
    }*/

    // 무조건 tokyo 리전 인스턴스를 반환한다.
    public static FirebaseStorage getInstance() {
        return FirebaseStorage.getInstance(STORAGE_BUCKET_TOKYO);
    }

    public static StorageReference getRef() {
        return getInstance().getReference();
    }

    public static StorageReference getRef(String path) {
        return getInstance().getReference(path);
    }

    // Users
    public static StorageReference getUsersRef() {
        return getRef().child(STORAGE_PATH_USER);
    }

    // Events
    public static StorageReference getEventsRef() {
        return getRef().child(STORAGE_PATH_EVENTS);
    }

    // Articles
    public static StorageReference getArticleRef() {
        return getRef().child(STORAGE_PATH_ARTICLE);
    }

    public static StorageReference getArticlePostsRef() {
        return getArticleRef().child(STORAGE_PATH_ARTICLE_POSTS);
    }

    public static Task<List<String>> putFilesFix(final StorageReference ref, final List<Uri> uris) {
        Log.i(TAG, "putFiles");

        // 문제는 여기서 값을 반환받을 수 있냐는 것이다.

        // TODO: 빈 ArrayList를 생성하여 값을 최초에 넘겨준다.
        // List<String> filenames = new ArrayList<>();
        // return loop(ref, uris, 0, filenames);

        return TasksManager.call(() -> {
            List<String> filenames = new ArrayList<>();
            for (Uri uri : uris) {
                String filename = UUID.randomUUID().toString() + ".jpg";
                Task<UploadTask.TaskSnapshot> putFileTask = putFile(ref.child(filename), uri);
                UploadTask.TaskSnapshot snapshot = Tasks.await(putFileTask);

                /*if (!putFileTask.isSuccessful()) {
                    Exception e = putFileTask.getException();
                    Log.w(TAG, "putFiles:putFileTask:ERROR:", putFileTask.getException());
                    throw e;
                }*/

                String uploadedName = snapshot.getMetadata().getName();
                Log.d(TAG, "putFiles:putFileTask:uploadedName:" + uploadedName);

                filenames.add(filename);
            }
            return filenames;
        });
    }

    public static Task<List<String>> putFiles(final StorageReference ref, final List<Uri> uris) {
        Log.i(TAG, "putFiles");

        // 문제는 여기서 값을 반환받을 수 있냐는 것이다.

        // TODO: 빈 ArrayList를 생성하여 값을 최초에 넘겨준다.
        List<String> filenames = new ArrayList<>();
        return loop(ref, uris, 0, filenames);
    }

    // for문을 돌릴 수 없기 때문에 계속 같은 함수를 누군가 반복해서 호출해줘야 한다.
    // 또한 putFiles는 2번 이상 호출되지 않을 것이기 때문에 Uri를 한 인덱스씩 넘겨줄 수 없다 따라서 통채로 넘겨줘야 한다.
    private static Task<List<String>> loop(final StorageReference parentRef, final List<Uri> uris, final int currentIndex, final List<String> filenames) {
        Log.i(TAG, "loop");

        Log.d(TAG, "loop:uris:" + uris.toString());
        Log.d(TAG, "loop:currentIndex:" + currentIndex);
        Log.d(TAG, "loop:filenames:" + filenames.toString());

        final int totalCount = uris.size();

        final String filename = UUID.randomUUID().toString() + ".jpg";
        filenames.add(filename);
        StorageReference ref = parentRef.child(filename);

        return putFile(ref, uris.get(currentIndex)).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "loop:putFile:ERROR:");
                return Tasks.forException(task.getException());
            }
            Log.d(TAG, "loop:putFile:SUCCESS:");
            if (currentIndex >= totalCount - 1) {
                Log.d(TAG, "loop:putFile:SUCCESS:FINISH:filenames:" + filenames.toString());
                return Tasks.forResult(filenames);
            }
            int nextIndex = currentIndex + 1;
            return loop(parentRef, uris, nextIndex, filenames);
        });
    }

    public static Task<UploadTask.TaskSnapshot> putFile(final StorageReference ref, final Uri uri) {
        Log.d(TAG, "putFile:uri: " + uri.toString());
        return ref.putFile(uri).continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "putFile:ERROR", task.getException());
                throw task.getException();
            }
            Log.d(TAG, "putFile:SUCCESS:" + task.getResult().getMetadata().getPath());
            return task;
        });
    }

    // 이 메서드를 통해서 업로드 된 이미지 파일만 Glide 이미지뷰에 다운로드 할 수 있다.
    @Deprecated
    public static Task<Void> putFileWithSignature(final StorageReference ref, final Uri uri) {
        Log.d(TAG, "putFileWithSignature:uri: " + uri.toString());
        return ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "putFileWithSignature:ERROR", task.getException());
                        throw task.getException();
                    }
                    Log.d(TAG, "putFileWithSignature:SUCCESS:" + task.getResult().getMetadata().getPath());
                    String signature = String.valueOf(task.getResult().getMetadata().getCreationTimeMillis());
                    return DatabaseManager.getSignatureRef(ref).setValue(signature);
                });
    }

    public static Task<String> putFileWithSignature(final StorageReference fileRef,
                                                    final DatabaseReference signatureRef,
                                                    final Uri uri) {
        Log.d(TAG, "putFileWithSignature:uri: " + uri.toString());
        final AtomicReference<String> atomicSignature = new AtomicReference<>();
        return fileRef.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "putFileWithSignature:ERROR", task.getException());
                        throw task.getException();
                    }
                    String signature = String.valueOf(task.getResult().getMetadata().getCreationTimeMillis());
                    atomicSignature.set(signature);
                    Log.d(TAG, "putFileWithSignature:SUCCESS:" +
                            "downloadUrl:" + task.getResult().getMetadata().getPath() + "|" +
                            "signature:" + signature);
                    return signatureRef.setValue(signature);
                }).continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "putFileWithSignature:ERROR", task.getException());
                        throw task.getException();
                    }
                    return atomicSignature.get();
                });
    }

    public static Task<Void> delete(StorageReference ref) {
        Log.d(TAG, "delete");
        return ref.delete();
    }

    public static Task<Void> delete(final StorageReference ref, final List<String> filenames) {
        Log.i(TAG, "delete");
        return Tasks.call(Executors.newSingleThreadExecutor(), () -> {
            for (String filename : filenames) {
                StorageReference fileRef = ref.child(filename);
                Log.i(TAG, "delete:loop:path:" + fileRef.getPath());
                Task<Void> deleteTask = delete(fileRef);
                Tasks.await(deleteTask);
                if (!deleteTask.isSuccessful()) {
                    Exception e = deleteTask.getException();
                    Log.w(TAG, "delete:loop:deleteTask:ERROR:" + e.getMessage());
                    throw e;
                }
                Log.i(TAG, "delete:loop:deleteTask:SUCCESS");
            }
            return null;
        });
    }

    // 파일이 리모트에 존재하든지 안하든지간에 시그너쳐를 삭제해준다.
    @Deprecated
    public static Task<Void> deleteWithSignature(StorageReference ref) {
        Log.d(TAG, "deleteWithSignature");
        final DatabaseReference signatureRef = DatabaseManager.getSignatureRef(ref);
        return ref.delete().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                /*try {
                    throw task.getException();
                } catch (StorageException e) {
                    Log.w(TAG, "deleteProfileImage:ERROR:" + e.getErrorCode());
                    if (e.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        return signatureRef.removeValue();
                    }
                }*/
                Exception deleteError = task.getException();
                if (deleteError instanceof StorageException) {
                    StorageException e = (StorageException) deleteError;
                    Log.w(TAG, "deleteProfileImage:ERROR:" + e.getErrorCode());
                    if (e.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        return signatureRef.removeValue();
                    }
                }
                Log.w(TAG, "deleteProfileImage:ERROR:" + deleteError.getMessage());
                throw deleteError;
            }
            Log.d(TAG, "deleteProfileImage:onSuccess");
            return signatureRef.removeValue();
        });
    }

    public static String getPath(StorageReference ref) {
        return ref.getPath();
    }
}
