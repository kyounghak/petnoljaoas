package com.chaigene.petnolja.util;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.chaigene.petnolja.manager.ConfigManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class EventUtil {
    public static final String TAG = "EventUtil";

    /**
     * 펫놀자 앱은 내장디비를 사용하지 않기 때문에 캐쉬화를 오직 메모리를 통해서만 구현한다.
     * 한번 로드되어진 글은 이 객체에 저장되고 재사용되어진다.
     */
    private Event mCachedEventPopup;

    /**
     * 이벤트의 캐쉬화를 위해 싱글톤을 사용한다.
     */
    private static volatile EventUtil sInstance;

    public static EventUtil getInstance() {
        if (sInstance == null) sInstance = new EventUtil();
        return sInstance;
    }

    public static void releaseInstance() {
        if (sInstance != null) {
            sInstance.release();
            sInstance = null;
        }
    }

    private void release() {
        this.mCachedEventPopup = null;
    }

    private EventUtil() {
        this.mCachedEventPopup = null;
    }

    // TODO: 이벤트 팝업을 가져오기 위해서 일부 쿼리가 이루어져야 한다.
    // 아직 스펙이 정확하게 나오진 않았지만 우선은 단일 이벤트 팝업만
    // 가능하다는 전제하에 개발한다. 따라서 isPopup이 true인 가장 최신 팝업을
    // 하나만 가져오는 쿼리가 구현되어야 한다.
    // 만약 해당 쿼리에 대한 결과값의 size가 0이면 팝업을 띄우지 않는 방식으로 구현한다.
    // 또한 다시 보지 않기를 터치할 경우 현재 앱의 프리퍼런에 저장해서 클라이언트 한정으로 구현한다.
    // 다시 보지 않기에 대한 처리도 현재 클래스에 유틸화하여 처리하기로 한다.

    // TODO: 가장 이상적인 방법은 setPopup을 저장할 때 기존에 저장되어진 이벤트들이 setPopup을 false로 변경하는 것이다.
    // 따라서 다음과 같은 쿼리를 날리더라도 2개 이상의 Event 객체가 반환될 일은 없다.

    // Ref: https://stackoverflow.com/a/48493431/4729203
    public Task<Event> getEventPopup() {
        Date nowDate = Calendar.getInstance().getTime();
        Query eventQuery = FirestoreManager.getEventsRef()
                .whereLessThan(Event.FIELD_END_DATE, nowDate)
                .whereEqualTo(Event.FIELD_IS_POPUP, true)
                .whereEqualTo(Event.FIELD_IS_ENABLED, true);

        TaskCompletionSource<Event> tcs = new TaskCompletionSource<>();
        TasksManager.call(() -> {
            Task<List<Event>> getEventsTask = FirestoreManager.getInstance().get(eventQuery, Event.class);
            List<Event> activeEvents = Tasks.await(getEventsTask);

            // IT WILL NEVER HAPPEN, BUT JUST IN SPECIAL CASE.
            Event lastestActiveEvent = null;
            for (Event event : activeEvents) {
                if (lastestActiveEvent == null) {
                    lastestActiveEvent = event;
                    continue;
                }
                Date beforeDate = lastestActiveEvent.getCreatedDate();
                Date currentDate = event.getCreatedDate();
                if (currentDate.after(beforeDate)) lastestActiveEvent = event;
            }

            // 캐쉬에 저장한다.
            saveCachedEventPopup(lastestActiveEvent);

            tcs.setResult(lastestActiveEvent);
            return null;
        }).addOnFailureListener(e -> {
            Log.w(TAG, "getEventPopup:ERROR:", e);
            tcs.setException(e);
        });
        return tcs.getTask();
    }

    public boolean isDoNotShowAgainPopup(Context context, String eventId) {
        Context c = context.getApplicationContext();
        String skippedEventId = ConfigManager.getInstance(c).getEventPopupSkipId();
        return eventId.equals(skippedEventId);
    }

    public void setDoNotShowAgainPopup(Context context, String eventId) {
        Context c = context.getApplicationContext();
        ConfigManager.getInstance(c).setEventPopupSkipId(eventId);
    }

    public Task<List<Event>> getEventList() {
        return null;
    }

    public Task<Event> getEvent(String eventId) {
        return null;
    }

    public synchronized void saveCachedEventPopup(Event newEventPopup) {
        // boolean isExists = false;
        /*for (Event eventPopup : mCachedEventPopup) {
            if (newEventPopup.getId().equals(eventPopup.getId())) {
                isExists = true;
                int index = mCachedUsers.indexOf(eventPopup);
                mCachedUsers.set(index, newUser);
                // Log.d(TAG, "saveOldCachedUser:saved:cachedUsers:" + mCachedOldUsers.toString());
                break;
            }
        }
        if (!isExists) {
            mCachedUsers.add(newUser);
            // Log.d(TAG, "saveOldCachedUser:saved:cachedUsers:" + mCachedOldUsers.toString());
        }*/
        this.mCachedEventPopup = newEventPopup;
    }

    // TODO: eventId가 굳이 필요할 것인가???

    // 어디서 호출할 것인가?
    // 네트워크 통신 여부를 어디서 결정할 것인가???
    // LauncherActivity에서는 서버로부터 가져오고 이벤트 팝업을 띄우는 시점에서는 캐쉬에서 가져올 것인가?
    // 이벤트 팝업은 어느 시점에서 띄울 것인가?
    // 메인액티비티 내에서 띄워줘야 할 것 같다. initView 메서드 내에 삽입하는 것이 좋을 것 같다.
    //        if (!isForceRefresh) {
    //            User cachedUser = getInstance().loadCachedUser(userId);
    //            if (cachedUser != null) return Tasks.forResult(cachedUser);
    //        }

    public synchronized Event loadCachedEventPopup(@NonNull String eventId) {
        // Log.i(TAG, "loadOldCachedUser:uid:" + uid);
        return this.mCachedEventPopup;
    }
}