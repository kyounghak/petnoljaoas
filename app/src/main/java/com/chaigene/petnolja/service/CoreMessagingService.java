package com.chaigene.petnolja.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.RingtoneManager;
import android.media.ThumbnailUtils;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.bumptech.glide.load.engine.GlideException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.GlideManager;
import com.chaigene.petnolja.manager.StorageManager;
import com.chaigene.petnolja.ui.activity.LauncherActivity;
import com.chaigene.petnolja.util.CommonUtil;
import com.chaigene.petnolja.util.NotificationUtil;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_ARTICLE;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_CHAT;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_PROFILE;
import static com.chaigene.petnolja.Constants.ACTION_STATUS_SHOP;
import static com.chaigene.petnolja.Constants.EXTRA_ACTION_STATUS;
import static com.chaigene.petnolja.Constants.EXTRA_ORDER_ID;
import static com.chaigene.petnolja.Constants.EXTRA_SHOP_TYPE;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_POST_ID;
import static com.chaigene.petnolja.Constants.EXTRA_TARGET_USER_ID;
import static com.chaigene.petnolja.Constants.PROFILE_IMAGE_FILENAME;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_CHAT_MESSAGE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_COMMENT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_FOLLOW;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_FOLLOW_ACCEPT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_FOLLOW_REQUEST;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_LIKE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_MENTION_ARTICLE;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_MENTION_COMMENT;
import static com.chaigene.petnolja.model.FIRNotification.TYPE_SHOP;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ISSUE_REQUEST;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_CANCEL_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_ORDER_REJECT_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_PAYMENT_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_PURCHASE_COMPETE;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_COMPLETE;
import static com.chaigene.petnolja.model.Order.STATUS_SHIPPING_IN_PROGRESS;
import static com.chaigene.petnolja.model.Order.STATUS_WORK_IN_PROGRESS;

public class CoreMessagingService extends FirebaseMessagingService {
    private static final String TAG = "CoreMessagingService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "onMessageReceived:from:" + remoteMessage.getFrom() + "|messageId:" + remoteMessage.getMessageId());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "onMessageReceived:data: " + remoteMessage.getData());
            handleData(remoteMessage.getData());
        }

        // 최신 버전 부터는 Notification이 null 인지 여부는 신경쓰지 않는다.
        if (remoteMessage.getNotification() != null) {
            String body = remoteMessage.getNotification().getBody();
            Log.d(TAG, "onMessageReceived:body: " + body);
        }

        Intent intent = new Intent();
        intent.setAction("com.pwdr.nacky.intent.action.MESSAGING_EVENT");
        intent.putExtra("extra_remote_message", remoteMessage);
        sendOrderedBroadcast(intent, null);
    }

    // 실제적으로 Notification 알림을 생성하는 것은 여기서 만든다.
    // 여기서 NotificationUtil 메모리 캐쉬에 저장한다.
    private void handleData(Map<String, String> data) {
        Log.i(TAG, "handleData:data:" + data);

        if (data == null || data.size() == 0) return;
        if (!data.containsKey("type")) return;

        sendNotification(data);

        // 채팅일 경우는 뱃지를 증가시키지 않는다.
        int type = Integer.parseInt(data.get("type"));
        if (type == TYPE_CHAT_MESSAGE) return;
        NotificationUtil.getInstance().increaseUncheckedCount();
    }

    // 모든 notification은 여기서 보여져야 한다.
    @SuppressWarnings("ConstantConditions")
    private void sendNotification(final Map<String, String> data) {
        Log.i(TAG, "sendNotification:data:" + data);

        boolean isForeground = CommonUtil.isAppInForeground(this);
        Log.d(TAG, "sendNotification:isForeground:" + isForeground);
        if (isForeground) return;

        com.chaigene.petnolja.model.Notification notification = NotificationUtil.parse(data);

        // 닉네임이 null 일 경우 NullPointerException이 발생한다.
        String nickname = notification.getTargetNickname();

        Bitmap photo = null;
        String message = "";
        Intent intent = new Intent(getApplicationContext(), LauncherActivity.class);
        /*.putExtra(EXTRA_ACTION_STATUS, actionStatus)
        .putExtra(EXTRA_TARGET_POST_ID, postId)
        .putExtra(EXTRA_TARGET_USER_ID, userId)
        .putExtra(EXTRA_SHOP_TYPE, shopType)
        .putExtra(EXTRA_ORDER_ID, orderId);*/

        boolean isPhotoExists = true;
        StorageReference photoRef = null;
        switch (notification.getType()) {
            case TYPE_LIKE:
                photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                message = getString(R.string.label_notification_like, nickname);
                intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_ARTICLE);
                intent.putExtra(EXTRA_TARGET_POST_ID, notification.getPostId());
                break;
            case TYPE_COMMENT:
                photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                String comment = CommonUtil.ellipsize(notification.getComment(), 20);
                message = getString(R.string.label_notification_comment, nickname, comment);
                intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_ARTICLE);
                intent.putExtra(EXTRA_TARGET_POST_ID, notification.getPostId());
                break;
            case TYPE_FOLLOW:
                photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                message = getString(R.string.label_notification_follow, nickname);
                intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_PROFILE);
                intent.putExtra(EXTRA_TARGET_USER_ID, notification.getTargetUid());
                break;
            case TYPE_FOLLOW_REQUEST:
                break;
            case TYPE_FOLLOW_ACCEPT:
                break;
            case TYPE_SHOP:

                Log.d(TAG, "sendNotification:type:TYPE_SHOP|shopType:" + notification.getShopType());

                // 구매자
                if (notification.getShopType() == SHOP_TYPE_BUY) {

                    // 주문하신 product1 작품에 대한 구매요청이 승인되었습니다.
                    // 주문하신 product1 작품에 대한 구매요청이 거절되었습니다.
                    // 주문하신 product1 작품에 대한 배송이 시작되었습니다.
                    // 주문하신 product1 작품이 전달완료 되었습니다. 작품에 대한 후기를 공유해주세요 :) (12시, 3시, 6시, 9시)
                    // 주문하신 product1 작품에 대한 환불/교환이 완료되었습니다. 정상적으로 해결되었다면 요청을 철회해주세요 :)

                    switch (notification.getOrderStatus()) {
                        case STATUS_WORK_IN_PROGRESS:
                            // orderStatus = "작업중";
                            photoRef = StorageManager.getArticlePostsRef()
                                    .child(notification.getProductId())
                                    .child(notification.getPhotoName());
                            message = CommonUtil.format(
                                    "주문하신 %s 작품에 대한 구매요청이 승인되었습니다.",
                                    notification.getProductTitle()
                            );
                            break;
                        case STATUS_SHIPPING_IN_PROGRESS:
                            // orderStatus = "전달중";
                            photoRef = StorageManager.getArticlePostsRef()
                                    .child(notification.getProductId())
                                    .child(notification.getPhotoName());
                            message = CommonUtil.format(
                                    "주문하신 %s 작품에 대한 배송이 시작되었습니다.",
                                    notification.getProductTitle()
                            );
                            break;
                        case STATUS_SHIPPING_COMPLETE:
                            // orderStatus = "전달완료";
                            photoRef = StorageManager.getArticlePostsRef()
                                    .child(notification.getProductId())
                                    .child(notification.getPhotoName());
                            message = CommonUtil.format(
                                    "주문하신 %s 작품이 전달완료 되었습니다. 작품에 대한 후기를 공유해주세요 :)",
                                    notification.getProductTitle()
                            );
                            break;
                        case STATUS_ORDER_REJECT_COMPLETE:
                            // orderStatus = "주문거절";
                            photoRef = StorageManager.getArticlePostsRef()
                                    .child(notification.getProductId())
                                    .child(notification.getPhotoName());
                            message = CommonUtil.format(
                                    "주문하신 %s 작품에 대한 구매요청이 거절되었습니다.",
                                    notification.getProductTitle()
                            );
                            break;
                        case STATUS_ISSUE_COMPLETE:
                            // orderStatus = "환불/교환완료";
                            photoRef = StorageManager.getArticlePostsRef()
                                    .child(notification.getProductId())
                                    .child(notification.getPhotoName());
                            message = CommonUtil.format(
                                    "주문하신 %s 작품에 대한 환불/교환이 완료되었습니다. 확인 후 요청을 철회해주세요 :)",
                                    notification.getProductTitle()
                            );
                            break;
                        default:
                            isPhotoExists = false;
                            message = notification.getMessage();
                            break;
                    }
                }

                // 판매자
                if (notification.getShopType() == SHOP_TYPE_SELL) {

                    // user1님이 product1 작품을 1개 구매하셨습니다. 구매를 승인해주세요 :)
                    // user1님이 product1 작품에 대한 주문을 취소하셨습니다.
                    // user1님에게 product1 작품이 전달완료 되었습니다. (12시, 3시, 6시, 9시)
                    // user1님이 product1 작품을 구매결정 하셨습니다.
                    // user1님의 product1 작품에 대한 주문이 자동구매결정 되었습니다.
                    // user1님이 product1 작품을 환불/교환 요청하셨습니다.
                    // user1님이 product1 작품에 대한 환불/교환 요청을 철회하셨습니다. => X (철회 타이밍을 알수없음)

                    switch (notification.getOrderStatus()) {
                        case STATUS_PAYMENT_COMPLETE:
                            // orderStatus = "결제완료";
                            photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                            message = CommonUtil.format(
                                    "%s님이 %s 작품을 %d개 구매하셨습니다. 구매를 승인해주세요 :)",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle(),
                                    notification.getQuantity()
                            );
                            break;
                        case STATUS_SHIPPING_COMPLETE:
                            // orderStatus = "전달완료";
                            photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                            message = CommonUtil.format(
                                    "%s님에게 %s 작품이 전달완료 되었습니다.",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle()
                            );
                            break;
                        case STATUS_PURCHASE_COMPETE:
                            // orderStatus = "구매완료";
                            if (!notification.isAutoFinalized()) {
                                photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                                message = CommonUtil.format(
                                        "%s님이 %s 작품을 구매결정 하셨습니다.",
                                        notification.getTargetNickname(),
                                        notification.getProductTitle()
                                );
                            } else {
                                photoRef = StorageManager.getArticlePostsRef()
                                        .child(notification.getProductId())
                                        .child(notification.getPhotoName());
                                message = CommonUtil.format(
                                        "%s님의 %s 작품에 대한 주문이 자동구매결정 되었습니다.",
                                        notification.getTargetNickname(),
                                        notification.getProductTitle()
                                );
                            }
                            break;
                        case STATUS_ORDER_CANCEL_COMPLETE:
                            // orderStatus = "주문취소";
                            photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                            message = CommonUtil.format(
                                    "%s님이 %s 작품에 대한 주문을 취소하셨습니다.",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle()
                            );
                            break;
                        case STATUS_ISSUE_REQUEST:
                            // orderStatus = "환불/교환요청";
                            photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                            message = CommonUtil.format(
                                    "%s님이 %s 작품을 환불/교환 요청하셨습니다.",
                                    notification.getTargetNickname(),
                                    notification.getProductTitle()
                            );
                            break;
                        default:
                            isPhotoExists = false;
                            message = notification.getMessage();
                            break;
                    }
                }

                intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_SHOP);
                intent.putExtra(EXTRA_SHOP_TYPE, notification.getShopType());
                intent.putExtra(EXTRA_ORDER_ID, notification.getOrderId());

                break;
            case TYPE_MENTION_ARTICLE:
                photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                String mentionContent = CommonUtil.ellipsize(notification.getContent(), 20);
                // message = getString(R.string.label_notification_comment, nickname, comment);
                message = CommonUtil.format("%s님이 게시글에서 회원님을 언급했습니다. \"%s\"", nickname, mentionContent);
                intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_ARTICLE);
                intent.putExtra(EXTRA_TARGET_POST_ID, notification.getPostId());
                break;
            case TYPE_MENTION_COMMENT:
                photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                String mentionComment = CommonUtil.ellipsize(notification.getComment(), 20);
                message = getString(R.string.label_notification_comment, nickname, mentionComment);
                intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_ARTICLE);
                intent.putExtra(EXTRA_TARGET_POST_ID, notification.getPostId());
                break;
            case TYPE_CHAT_MESSAGE:
                photoRef = StorageManager.getUsersRef().child(notification.getTargetUid()).child(PROFILE_IMAGE_FILENAME);
                String chatMessage = CommonUtil.ellipsize(notification.getChatMessage(), 20);
                // message = getString(R.string.label_notification_comment, nickname, chatMessage);
                message = CommonUtil.format("%s님이 메세지를 보냈습니다. \"%s\"", nickname, chatMessage);
                intent.putExtra(EXTRA_ACTION_STATUS, ACTION_STATUS_CHAT);
                intent.putExtra(EXTRA_TARGET_USER_ID, notification.getTargetUid());
                // intent.putExtra(EXTRA_TARGET_ROOM_ID, notification.getChatRoomId());
                break;
            default:
                isPhotoExists = false;
                message = notification.getMessage();
                break;
        }

        final long timestamp = notification.getTimestamp(true);

        if (isPhotoExists) {

            Task<Bitmap> loadImageTask = GlideManager.loadImage(getApplicationContext(), photoRef);
            try {
                photo = Tasks.await(loadImageTask);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (!loadImageTask.isSuccessful()) {
                Exception loadImageError = loadImageTask.getException();
                Log.w(TAG, "sendNotification:loadImageTask:ERROR:", loadImageError);
                if (loadImageError instanceof GlideException) {
                    GlideException glideEx = (GlideException) loadImageError;
                    Throwable throwable = glideEx.getRootCauses().iterator().next();
                    if (CommonUtil.instanceOfException(throwable, StorageException.class)) {
                        StorageException storageEx = (StorageException) throwable;
                        if (storageEx.getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.w(TAG, "sendNotification:loadImage:ERROR:ERROR_OBJECT_NOT_FOUND");
                            // photo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_user_anonymous);
                            photo = null;
                        }
                    }
                }
            }
            notifyNotification(message, photo, timestamp, intent);
        } else {
            // photo = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            notifyNotification(message, photo, timestamp, intent);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void notifyNotification(String message,
                                   Bitmap photo,
                                   Long timestamp,
                                   Intent intent) {
        Log.i(TAG, "notifyNotification:message:" + message + "|photo:" + photo + "|timestamp:" + timestamp);

        Context c = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (photo != null) {
            photo = ThumbnailUtils.extractThumbnail(
                    photo,
                    CommonUtil.dpToPx(this, 64),
                    CommonUtil.dpToPx(this, 64)
            );
            photo = getRoundedBitmap(photo, CommonUtil.dpToPx(c, 10));
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // The id of the channel.
        final String CHANNEL_ID = "default";
        // The user-visible name of the channel.
        final String CHANNEL_NAME = "Default";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Ref: https://developer.android.com/training/notify-user/channels.html#importance
            NotificationChannel defaultChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            // defaultChannel.setDescription("");
            // defaultChannel.setSound(defaultSoundUri, Notification.AUDIO_ATTRIBUTES_DEFAULT);
            // defaultChannel.enableVibration(true);
            // notificationManager.deleteNotificationChannel(CHANNEL_ID);
            // createNotificationChannel는 반복적으로 호출되어도 이미 존재하는 채널일 경우 무시된다.
            notificationManager.createNotificationChannel(defaultChannel);
        }

        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        // Ref: https://stackoverflow.com/a/9330144/4729203
        PendingIntent pendingIntent = PendingIntent.getActivity(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(c, CHANNEL_ID)
                .setLargeIcon(photo)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(ContextCompat.getColor(c, R.color.notification))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setWhen(timestamp)
                // .setWhen(System.currentTimeMillis())
                .setSound(defaultSoundUri)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setLights(CommonUtil.getColor(c, R.color.colorAccentLight), 5000, 5000)
                .setAutoCancel(true)
                // .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_MAX) // Priority는 API21 이상에서만 적용된다.
                .setContentIntent(pendingIntent);

        notificationBuilder.setStyle(
                new NotificationCompat.BigTextStyle(notificationBuilder)
                        // .setSummaryText("...")
                        .setBigContentTitle(getString(R.string.app_name))
                        .bigText(message)
        );

        notificationManager.notify("pandaz", 0, notificationBuilder.build());
        Log.i(TAG, "notifyNotification:SUCCESS");
    }

    public Bitmap getCircularBitmap(@NonNull Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    public Bitmap getRoundedBitmap(@NonNull Bitmap bitmap, int radius) {
        int color;
        final Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(result);
        color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return result;
    }

    /*public Bitmap getRoundedBitmap(@NonNull Bitmap bitmap, int radius) {
        Bitmap result = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        BitmapShader shader;
        shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(shader);

        RectF rect = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());

        // rect contains the bounds of the shape
        // radius is the radius in pixels of the rounded corners
        // paint contains the shader that will texture the shape
        canvas.drawRoundRect(rect, radius, radius, paint);
        return result;
    }*/
}