package com.chaigene.petnolja.util;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.AuthManager;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.model.Card;
import com.chaigene.petnolja.model.Comment;
import com.chaigene.petnolja.model.Order;
import com.chaigene.petnolja.model.SecurityPin;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static com.chaigene.petnolja.Constants.SHOP_TYPE_BUY;
import static com.chaigene.petnolja.Constants.SHOP_TYPE_SELL;
import static com.chaigene.petnolja.model.Card.FIELD_IS_PRIMARY;
import static com.chaigene.petnolja.model.Card.FIELD_USER_ID;
import static com.chaigene.petnolja.model.Order.STATUS_PURCHASE_COMPETE;
import static com.chaigene.petnolja.model.Request.STATUS_SUCCESS;

public class ShopUtil {
    public static final String TAG = "ShopUtil";

    public static Task<Integer> getNextOrderNo() {
        final DocumentReference orderNoRef = FirestoreManager.getCountersOrderNoRef();
        Transaction.Function<Integer> function = new Transaction.Function<Integer>() {
            @Override
            public Integer apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot orderNoDoc = transaction.get(orderNoRef);
                Double newSeq = orderNoDoc.getDouble("seq") + 1;
                transaction.update(orderNoRef, "seq", newSeq);
                Integer result = newSeq.intValue();
                return result;
            }
        };
        return FirestoreManager.getInstance().runTransaction(function);
    }

    public static Task<Integer> getNextBillingKeyNo() {
        final DocumentReference billingKeyNoRef = FirestoreManager.getCountersBillingKeyNoRef();
        Transaction.Function<Integer> function = new Transaction.Function<Integer>() {
            @Override
            public Integer apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot billingKeyNoDoc = transaction.get(billingKeyNoRef);
                Double newSeq = billingKeyNoDoc.getDouble("seq") + 1;
                transaction.update(billingKeyNoRef, "seq", newSeq);
                Integer result = newSeq.intValue();
                return result;
            }
        };
        return FirestoreManager.getInstance().runTransaction(function);
    }

    /**
     * 트랜잭션은 이미 다른 요청이 진행되고 있다면 계속 반복해서 호출된다.
     * Firestore는 단일 도큐먼트에 1초당 1번만 업데이트가 가능하다.
     * 따라서 여러 클라이언트에서 동시적으로 getNextSequence를 요청할 경우 60개의 단말기가 동시에 요청한다면 최장 1분이 걸리수도 있다.
     * Shard 방식을 사용하게 되면 파편 개수만큼 속도를 줄일 수 있다. 파편이 5개일 경우 60개의 단말기가 요청하더라도 최장 12초로 줄일 수 있다.
     * 파편이 적으면 slow writing, 파편이 많으면 slow reading, expensive cost 이슈가 있다.
     *
     * @return
     */
    public static Task<Integer> getNextSequence2() {
        final int NUM_SHARDS = 5;
        final CollectionReference orderNoShardsRef = FirestoreManager.getCountersOrderNoRef().collection("shards");
        Transaction.Function<Integer> function = new Transaction.Function<Integer>() {
            @Override
            public Integer apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                int count = 0;
                int updateShardId = (int) Math.floor(Math.random() * NUM_SHARDS);
                DocumentReference updateShardRef = orderNoShardsRef.document(String.valueOf(updateShardId));
                Integer updateShardSeq = 0;
                for (int i = 0; i < NUM_SHARDS; i++) {
                    DocumentReference shardRef = orderNoShardsRef.document(String.valueOf(i));
                    DocumentSnapshot shardDoc = transaction.get(shardRef);
                    int seq = shardDoc.getDouble("seq").intValue();
                    count += seq;
                    if (i == updateShardId) updateShardSeq = seq;
                }
                transaction.update(updateShardRef, "seq", updateShardSeq + 1);
                // 최종적으로 보여질 모든 shard를 합한 값.
                count = count + 1;
                return count;
            }
        };
        return FirestoreManager.getInstance().runTransaction(function);
    }

    // 본인인증을 했는지에 대한 여부
    // 카드가 등록되어있는지 여부
    // 결제비밀번호가 등록되어있는지 여부

    // 이 정보들은 UserUtil을 통해서 값을 가져올 수 있어야 한다.

    // Card
    public static Task<Boolean> isCardExists() {
        Query cardsRef = FirestoreManager.getShopCardsRef().whereEqualTo(FIELD_USER_ID, AuthManager.getUserId());
        return FirestoreManager.getInstance().get(cardsRef).continueWith(new Continuation<QuerySnapshot, Boolean>() {
            @Override
            public Boolean then(@NonNull Task<QuerySnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    throw e;
                }
                QuerySnapshot snapshot = task.getResult();
                return !snapshot.isEmpty();
            }
        });
    }

    // Primary 카드가 있는지 먼저 검색하고 없으면 일반 카드를 검색해서 아무거나 가져온다.
    public static Task<Card> getPrimaryCard() {
        ExecutorService excutor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(excutor, new Callable<Card>() {
            @Override
            public Card call() throws Exception {
                Query cardsRef = FirestoreManager.getShopCardsRef()
                        .whereEqualTo(FIELD_USER_ID, AuthManager.getUserId())
                        .whereEqualTo(FIELD_IS_PRIMARY, true);
                Task<List<Card>> getTask = FirestoreManager.getInstance().get(cardsRef, Card.class);

                List<Card> cards = Tasks.await(getTask);
                if (!getTask.isSuccessful()) {
                    Exception getError = getTask.getException();
                    throw getError;
                }

                if (cards != null && !cards.isEmpty()) {
                    return cards.iterator().next();
                }

                Task<List<Card>> getCardListTask = getCardList(1, null);

                List<Card> normalCards = Tasks.await(getCardListTask);
                if (!getCardListTask.isSuccessful()) {
                    Exception getCardListError = getCardListTask.getException();
                    throw getCardListError;
                }

                if (normalCards != null && !normalCards.isEmpty()) {
                    return normalCards.iterator().next();
                }

                return null;
            }
        });
    }

    public static Task<Card> setPrimaryCard(final String cardId) {
        ExecutorService excutor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(excutor, new Callable<Card>() {
            @Override
            public Card call() throws Exception {
                Query cardsRef = FirestoreManager.getShopCardsRef()
                        .whereEqualTo(FIELD_USER_ID, AuthManager.getUserId())
                        .whereEqualTo(FIELD_IS_PRIMARY, true);
                Task<List<Card>> getTask = FirestoreManager.getInstance().get(cardsRef, Card.class);
                List<Card> cards = Tasks.await(getTask);
                if (!getTask.isSuccessful()) {
                    Exception getError = getTask.getException();
                    throw getError;
                }
                WriteBatch batch = FirestoreManager.batch();
                for (Card card : cards) {
                    DocumentReference primaryCardRef = FirestoreManager.getShopCardsRef().document(card.getId());
                    batch.update(primaryCardRef, FIELD_IS_PRIMARY, false);
                }
                DocumentReference cardRef = FirestoreManager.getShopCardsRef().document(cardId);
                batch.update(cardRef, FIELD_IS_PRIMARY, true);
                Task<Void> batchTask = batch.commit();
                if (!batchTask.isSuccessful()) {
                    Exception batchError = batchTask.getException();
                    throw batchError;
                }
                return null;
            }
        });
    }

    public static Task<List<Card>> getCardList(int amount, DocumentSnapshot cursor) {
        Log.i(TAG, "getCardList:amout:" + amount + "|cursor:" + (cursor != null ? cursor.getData() : null));
        CollectionReference cardsRef = FirestoreManager.getShopCardsRef();
        Query cardsQuery = cardsRef
                .whereEqualTo(Card.FIELD_USER_ID, AuthManager.getUserId())
                .orderBy(Card.FIELD_IS_PRIMARY, Query.Direction.DESCENDING)
                .orderBy(Card.FIELD_TIMESTAMP, Query.Direction.ASCENDING);
        if (cursor != null) cardsQuery = cardsQuery.startAfter(cursor);
        cardsQuery = cardsQuery.limit(amount);
        return FirestoreManager.getInstance().get(cardsQuery, Card.class);
    }

    public static Task<Card> insertCard(String cardNo1,
                                        String cardNo2,
                                        String cardNo3,
                                        String cardNo4,
                                        String expDateMM,
                                        String expDateYY,
                                        final String pin,
                                        final boolean isPrimary) {
        Log.i(TAG, "insertCard");

        // TODO: 무조건 birthday 값은 서버에서 값을 가져온다.
        // birthday가 없다면 본인인증을 다시 시도하도록 유도해야 한다.

        if (cardNo1 == null ||
                cardNo2 == null ||
                cardNo3 == null ||
                cardNo4 == null ||
                expDateMM == null ||
                expDateYY == null ||
                pin == null) {
            return Tasks.forException(new NullPointerException("The parameters should not be null."));
        }

        if (cardNo1.length() < 4 ||
                cardNo2.length() < 4 ||
                cardNo3.length() < 4 ||
                cardNo4.length() < 4) {
            return Tasks.forException(new IllegalArgumentException("Card no parameter must be 4 digits."));
        }

        if (expDateMM.length() < 2 ||
                expDateYY.length() < 2 ||
                pin.length() < 2) {
            return Tasks.forException(new IllegalArgumentException("Exp date(month, year), pin parameter must be 2 digits."));
        }

        final String sourceNo = CommonUtil.format("%s-%s-%s-%s", cardNo1, cardNo2, cardNo3, cardNo4);
        final String expDate = CommonUtil.format("20%s-%s", expDateYY, expDateMM);

        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Card>() {
            @Override
            public Card call() throws Exception {

                Task<String> getBirthdayTask = UserUtil.getBrithday();
                String birthday = Tasks.await(getBirthdayTask);

                if (!getBirthdayTask.isSuccessful()) {
                    Exception getBirthdayError = getBirthdayTask.getException();
                    Log.w(TAG, "insertCard:getBirthdayError:" + getBirthdayError.getMessage());
                    throw getBirthdayError;
                }

                Task<Integer> getNextBillingKeyNoTask = getNextBillingKeyNo();
                int billingKeyNo = Tasks.await(getNextBillingKeyNoTask);
                if (!getNextBillingKeyNoTask.isSuccessful()) {
                    Exception getNextBillingKeyNoError = getNextBillingKeyNoTask.getException();
                    Log.w(TAG, "insertCard:getNextBillingKeyNoTask:ERROR:" + getNextBillingKeyNoError.getMessage());
                    throw getNextBillingKeyNoError;
                }
                Log.i(TAG, "insertCard:getNextBillingKeyNoTask:SUCCESS:billingKeyNo:" + billingKeyNo);

                Card card = new Card(
                        String.valueOf(billingKeyNo),
                        AuthManager.getUserId(),
                        sourceNo,
                        expDate,
                        pin,
                        birthday,
                        isPrimary
                );

                // TODO: request 호출.
                DocumentReference reqRef = FirestoreManager.getRequestRef();
                DocumentReference reqCardRef = FirestoreManager.getRequestShopCardsRef(reqRef).document();

                // 여기서 새로운 cardId를 추출한다.
                String cardId = reqCardRef.getId();
                DocumentReference resCardRef = FirestoreManager.getResponseShopCardsRef(reqRef).document(cardId);

                Task<Card> requestTask = FirestoreManager.getInstance().request(
                        reqCardRef,
                        resCardRef,
                        card,
                        Card.class,
                        null
                );

                Card newCard = Tasks.await(requestTask);
                if (!requestTask.isSuccessful()) {
                    Exception requestError = requestTask.getException();
                    Log.w(TAG, "insertCard:requestTask:ERROR:" + requestError.getMessage());
                    throw requestError;
                }
                Log.i(TAG, "insertCard:requestTask:SUCCESS:newCard:" + newCard.toMap());

                return newCard;
            }
        });
    }

    /**
     * Debug용 테스트카드를 삽입한다.
     *
     * @param cardNo1
     * @param cardNo2
     * @param cardNo3
     * @param cardNo4
     * @param expDateMM
     * @param expDateYY
     * @param pin
     * @param isPrimary
     * @return
     */
    public static Task<Card> fakeInsertCard(String cardNo1,
                                            String cardNo2,
                                            String cardNo3,
                                            String cardNo4,
                                            String expDateMM,
                                            String expDateYY,
                                            final String pin,
                                            final boolean isPrimary) {
        Log.i(TAG, "fakeInsertCard");

        if (cardNo1 == null ||
                cardNo2 == null ||
                cardNo3 == null ||
                cardNo4 == null ||
                expDateMM == null ||
                expDateYY == null ||
                pin == null) {
            return Tasks.forException(new NullPointerException("The parameters should not be null."));
        }

        if (cardNo1.length() < 4 ||
                cardNo2.length() < 4 ||
                cardNo3.length() < 4 ||
                cardNo4.length() < 4) {
            return Tasks.forException(new IllegalArgumentException("Card no parameter must be 4 digits."));
        }

        if (expDateMM.length() < 2 ||
                expDateYY.length() < 2 ||
                pin.length() < 2) {
            return Tasks.forException(new IllegalArgumentException("Exp date(month, year), pin parameter must be 2 digits."));
        }

        final String sourceNo = CommonUtil.format("%s-%s-%s-%s", cardNo1, cardNo2, cardNo3, cardNo4);
        final String expDate = CommonUtil.format("20%s-%s", expDateYY, expDateMM);

        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Card>() {
            @Override
            public Card call() throws Exception {

                Task<String> getBirthdayTask = UserUtil.getBrithday();
                String birthday = Tasks.await(getBirthdayTask);

                /*if (!getBirthdayTask.isSuccessful()) {
                    Exception getBirthdayError = getBirthdayTask.getException();
                    Log.w(TAG, "insertCard:getBirthdayError:" + getBirthdayError.getMessage());
                    throw getBirthdayError;
                }*/

                Task<Integer> getNextBillingKeyNoTask = getNextBillingKeyNo();
                int billingKeyNo = Tasks.await(getNextBillingKeyNoTask);
                /*if (!getNextBillingKeyNoTask.isSuccessful()) {
                    Exception getNextBillingKeyNoError = getNextBillingKeyNoTask.getException();
                    Log.w(TAG, "insertCard:getNextBillingKeyNoTask:ERROR:" + getNextBillingKeyNoError.getMessage());
                    throw getNextBillingKeyNoError;
                }*/
                Log.i(TAG, "fakeInsertCard:getNextBillingKeyNoTask:SUCCESS:billingKeyNo:" + billingKeyNo);

                Card card = new Card(
                        String.valueOf(billingKeyNo),
                        AuthManager.getUserId(),
                        sourceNo,
                        expDate,
                        pin,
                        birthday,
                        isPrimary
                );

                card.setName("테스트카드");
                card.setDisplayNo(card.getSourceNo());

                /*DocumentReference reqRef = FirestoreManager.getRequestRef();
                DocumentReference reqCardRef = FirestoreManager.getRequestShopCardsRef(reqRef).document();
                DocumentReference resCardRef = FirestoreManager.getResponseShopCardsRef(reqRef).document(reqCardRef.getId());
                Task<Card> requestTask = FirestoreManager.getInstance().request(
                        reqCardRef,
                        resCardRef,
                        card,
                        Card.class,
                        null
                );
                Card newCard = Tasks.await(requestTask);*/

                /*if (!requestTask.isSuccessful()) {
                    Exception requestError = requestTask.getException();
                    Log.w(TAG, "fakeInsertCard:requestTask:ERROR:" + requestError.getMessage());
                    throw requestError;
                }*/

                DocumentReference cardRef = FirestoreManager.getShopCardsRef().document();
                Task<Void> setTask = FirestoreManager.getInstance().set(cardRef, card);
                Tasks.await(setTask);

                Card resCard = card;
                resCard.setResponseStatus(STATUS_SUCCESS);
                Log.i(TAG, "fakeInsertCard:requestTask:SUCCESS:resCard:" + resCard.toMap());

                return resCard;
            }
        });
    }

    // Security pin
    public static Task<Boolean> isSecurityPinExists() {
        DocumentReference securityPinRef = FirestoreManager.getShopSecurityPinsRef().document(AuthManager.getUserId());
        return FirestoreManager.getInstance().get(securityPinRef).continueWith(new Continuation<DocumentSnapshot, Boolean>() {
            @Override
            public Boolean then(@NonNull Task<DocumentSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    Exception e = task.getException();
                    throw e;
                }
                DocumentSnapshot snapshot = task.getResult();
                return snapshot.exists();
            }
        });
    }

    public static Task<Void> updateSecurityPin(String source) {
        Log.i(TAG, "updateSecurityPin:source:" + source);
        SecurityPin securityPin = new SecurityPin(source);
        DocumentReference securityPinRef = FirestoreManager.getShopSecurityPinsRef().document(AuthManager.getUserId());
        return FirestoreManager.getInstance().set(securityPinRef, securityPin, SetOptions.merge());
    }

    public static Task<Order> buy(final String productId,
                                  final int productType,
                                  final String productTitle,
                                  final String productPrice,
                                  final String coverPhoto,
                                  final List<String> regions,
                                  final int quantity,
                                  final String shippingPrice,
                                  final String sellerId,
                                  final String sellerNickname,
                                  final String shippingReceiver,
                                  final String shippingAddress,
                                  final String shippingMessage,
                                  final String cardId,
                                  final String securityPin,
                                  final int installment) {
        Log.i(TAG, "buy");
        if (quantity < 1) {
            return Tasks.forException(new IllegalArgumentException("Quantity value should be at least 1."));
        }

        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Order>() {
            @Override
            public Order call() throws Exception {

                String buyerId = AuthManager.getUserId();

                Task<String> getUserNicknameTask = UserUtil.getUserNickname(buyerId);
                String buyerNickname = Tasks.await(getUserNicknameTask);
                /*if (!getUserNicknameTask.isSuccessful()) {
                    Exception getUserNicknameError = getUserNicknameTask.getException();
                    Log.w(TAG, "buy:getUserNicknameTask:ERROR:" + getUserNicknameError.getMessage());
                    throw getUserNicknameError;
                }*/
                Log.i(TAG, "buy:getUserNicknameTask:SUCCESS:buyerNickname:" + buyerNickname);

                Task<Integer> getNextSequenceTask = getNextOrderNo();
                int orderNo = Tasks.await(getNextSequenceTask);
                /*if (!getNextSequenceTask.isSuccessful()) {
                    Exception getNextSequenceError = getNextSequenceTask.getException();
                    Log.w(TAG, "buy:getNextSequenceTask:ERROR:" + getNextSequenceError.getMessage());
                    throw getNextSequenceError;
                }*/
                Log.i(TAG, "buy:getNextSequenceTask:SUCCESS:orderNo:" + orderNo);

                int unitPrice = Integer.parseInt(productPrice);
                int shippingPriceInt = shippingPrice != null ? Integer.valueOf(shippingPrice) : 0;
                int subtotalPrice = unitPrice * quantity;
                int totalPrice = subtotalPrice + shippingPriceInt;

                Order order = new Order(Order.STATUS_PAYMENT_REQUEST,
                        orderNo,
                        productTitle,
                        productId,
                        productType,
                        coverPhoto,
                        regions,
                        buyerId,
                        buyerNickname,
                        sellerId,
                        sellerNickname,
                        cardId,
                        securityPin,
                        quantity,
                        unitPrice,
                        shippingPriceInt,
                        subtotalPrice,
                        totalPrice,
                        installment,
                        shippingReceiver,
                        shippingAddress,
                        shippingMessage
                );

                DocumentReference reqRef = FirestoreManager.getRequestRef();
                DocumentReference reqOrderRef = FirestoreManager.getRequestShopOrdersRef(reqRef).document();
                // 여기서 새로운 orderId를 추출한다.
                String orderId = reqOrderRef.getId();
                DocumentReference resOrderRef = FirestoreManager.getResponseShopOrdersRef(reqRef).document(orderId);

                Task<Order> requestTask = FirestoreManager.getInstance().request(
                        reqOrderRef,
                        resOrderRef,
                        order,
                        Order.class,
                        null
                );

                Order resOrder = Tasks.await(requestTask);
                /*if (!requestTask.isSuccessful()) {
                    Exception requestError = requestTask.getException();
                    Log.w(TAG, "buy:requestTask:ERROR:" + requestError.getMessage());
                    throw requestError;
                }*/
                Log.i(TAG, "buy:requestTask:SUCCESS:resOrder:" +
                        (resOrder != null ? resOrder.getId() + " => " + resOrder.toMap() : "null"));
                return resOrder;
            }
        });
    }

    public static Task<Order> fakeBuy(final String productId,
                                      final int productType,
                                      final String productTitle,
                                      final String productPrice,
                                      final String coverPhoto,
                                      final List<String> regions,
                                      final int quantity,
                                      final String shippingPrice,
                                      final String sellerId,
                                      final String sellerNickname,
                                      final String shippingReceiver,
                                      final String shippingAddress,
                                      final String shippingMessage,
                                      final String cardId,
                                      final String securityPin,
                                      final int installment) {
        Log.i(TAG, "fakeBuy");
        if (quantity < 1) {
            return Tasks.forException(new IllegalArgumentException("Quantity value should be at least 1."));
        }
        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Order>() {
            @Override
            public Order call() throws Exception {
                String buyerId = AuthManager.getUserId();

                Task<String> getUserNicknameTask = UserUtil.getUserNickname(buyerId);
                String buyerNickname = Tasks.await(getUserNicknameTask);
                Log.i(TAG, "fakeBuy:getUserNicknameTask:SUCCESS:buyerNickname:" + buyerNickname);

                Task<Integer> getNextSequenceTask = getNextOrderNo();
                int orderNo = Tasks.await(getNextSequenceTask);
                Log.i(TAG, "fakeBuy:getNextSequenceTask:SUCCESS:orderNo:" + orderNo);

                int unitPrice = Integer.parseInt(productPrice);
                int shippingPriceInt = shippingPrice != null ? Integer.valueOf(shippingPrice) : 0;
                int subtotalPrice = unitPrice * quantity;
                int totalPrice = subtotalPrice + shippingPriceInt;

                Order order = new Order(Order.STATUS_PAYMENT_COMPLETE,
                        orderNo,
                        productTitle,
                        productId,
                        productType,
                        coverPhoto,
                        regions,
                        buyerId,
                        buyerNickname,
                        sellerId,
                        sellerNickname,
                        cardId,
                        securityPin,
                        quantity,
                        unitPrice,
                        shippingPriceInt,
                        subtotalPrice,
                        totalPrice,
                        installment,
                        shippingReceiver,
                        shippingAddress,
                        shippingMessage
                );

                DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document();
                Task<Void> setTask = FirestoreManager.getInstance().set(orderRef, order);
                Tasks.await(setTask);

                Order resOrder = order;
                resOrder.setResponseStatus(STATUS_SUCCESS);
                Log.i(TAG, "fakeBuy:requestTask:SUCCESS:resOrder:" + resOrder.getId());
                return resOrder;
            }
        });
    }

    public static Task<Order> finalize(final Order order, final String review) {
        ExecutorService executor = FirestoreManager.getInstance().getExecutor();
        return Tasks.call(executor, new Callable<Order>() {
            @Override
            public Order call() throws Exception {
                String postId = order.getProductId();
                Task<Comment> insertTask = CommentUtil.insert(postId, review);
                try {
                    Comment newComment = Tasks.await(insertTask);
                } catch (Exception e) {
                    Log.w(TAG, "finalize:insertTask:ERROR:" + e);
                    throw e;
                }
                Task<Order> changeStatusTask = changeStatus(STATUS_PURCHASE_COMPETE, order);
                Order newOrder;
                try {
                    newOrder = Tasks.await(changeStatusTask);
                } catch (Exception e) {
                    Log.w(TAG, "finalize:changeStatusTask:ERROR:" + e);
                    throw e;
                }
                return newOrder;
            }
        });
    }

    public static Task<Void> reportIssue(String orderId, int issueCode, String issueMessage) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put(Order.FIELD_STATUS, Order.STATUS_ISSUE_REQUEST);
        orderMap.put(Order.FIELD_ISSUE_CODE, issueCode);
        orderMap.put(Order.FIELD_ISSUE_MESSAGE, issueMessage);
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        return FirestoreManager.getInstance().set(orderRef, orderMap, SetOptions.merge());
    }

    public static Task<Void> resolveIssue(String orderId) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put(Order.FIELD_STATUS, Order.STATUS_ISSUE_COMPLETE);
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        return FirestoreManager.getInstance().set(orderRef, orderMap, SetOptions.merge());
    }

    // 사용해서는 안된다. int가 포함되어 있을 경우에는 모든 값을 0으로 업데이트 해버리는 이슈가 있다.
    @Deprecated
    public static Task<Void> updateOrder(String orderId, Order order) {
        Log.i(TAG, "updateOrder:order:" + order.toMap());
        Map<String, Object> orderMap = order.toMap();
        orderMap.remove("id");
        orderMap.values().removeAll(Collections.singleton(null));
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        return FirestoreManager.getInstance().set(orderRef, orderMap, SetOptions.merge());
    }

    public static Task<Void> ship(String orderId, String carrier, String trackingNo) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put(Order.FIELD_STATUS, Order.STATUS_SHIPPING_IN_PROGRESS);
        orderMap.put(Order.FIELD_SHIPPING_CARRIER, carrier);
        orderMap.put(Order.FIELD_SHIPPING_TRACKING_NO, trackingNo);
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        return FirestoreManager.getInstance().set(orderRef, orderMap, SetOptions.merge());
    }

    public static Task<Void> updateShippingInfo(String orderId, String carrier, String trackingNo) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put(Order.FIELD_SHIPPING_CARRIER, carrier);
        orderMap.put(Order.FIELD_SHIPPING_TRACKING_NO, trackingNo);
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        return FirestoreManager.getInstance().set(orderRef, orderMap, SetOptions.merge());
    }

    public static String getShippingCarrierName(Context context, String carrierCode) {
        Context c = context.getApplicationContext();
        String[] namesArr = c.getResources().getStringArray(R.array.shipping_tracking_carrier_names);
        String[] codesArr = c.getResources().getStringArray(R.array.shipping_tracking_carrier_codes);
        List<String> codes = Arrays.asList(codesArr);
        int codeIndex = codes.indexOf(carrierCode);
        return namesArr[codeIndex];
    }

    public static Task<Order> cancelOrder(Order order) {
        return requestStatus(Order.STATUS_ORDER_CANCEL_REQUEST, order);
    }

    public static Task<Order> rejectOrder(Order order) {
        return requestStatus(Order.STATUS_ORDER_REJECT_REQUEST, order);
    }

    /*public static Task<Void> changeStatus(int status, final String orderId) {
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        orderRef.update(Order.FIELD_STATUS, status).co
        return FirestoreManager.getInstance().update(orderRef, Order.FIELD_STATUS, status).continueWith(new Continuation<Void, Void>() {
            @Override
            public Order then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return order;
            }
        });
    }*/

    public static Task<Order> changeStatus(int status, final Order order) {
        order.setStatus(status);
        String orderId = order.getId();
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        return FirestoreManager.getInstance().set(orderRef, order).continueWith(new Continuation<Void, Order>() {
            @Override
            public Order then(@NonNull Task<Void> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                return order;
            }
        });
    }

    public static Task<Order> requestStatus(int status, Order order) {
        order.setStatus(status);
        String orderId = order.getId();
        DocumentReference reqRef = FirestoreManager.getRequestRef();
        DocumentReference reqOrderRef = FirestoreManager.getRequestShopOrdersRef(reqRef).document(orderId);
        DocumentReference resOrderRef = FirestoreManager.getResponseShopOrdersRef(reqRef).document(orderId);
        // DocumentReference queryRef = FirestoreManager.getRequestsRef().document();
        // DocumentReference reqOrderRef = queryRef.collection("request-shop-orders").document(orderId);
        // DocumentReference resOrderRef = queryRef.collection("response-shop-orders").document(orderId);
        return FirestoreManager.getInstance().request(reqOrderRef, resOrderRef, order, Order.class, null);
    }

    public static Task<Order> getOrder(String orderId) {
        DocumentReference orderRef = FirestoreManager.getShopOrdersRef().document(orderId);
        return FirestoreManager.getInstance().get(orderRef, Order.class);
    }

    public static Task<List<Order>> getBuyOrderList(String buyerUid, int amount, DocumentSnapshot cursor) {
        return getOrderList(SHOP_TYPE_BUY, buyerUid, amount, cursor);
    }

    public static Task<List<Order>> getSellOrderList(String sellerUid, int amount, DocumentSnapshot cursor) {
        return getOrderList(SHOP_TYPE_SELL, sellerUid, amount, cursor);
    }

    public static Task<List<Order>> getOrderList(int shopType, String targetUid, int amount, DocumentSnapshot cursor) {
        Log.i(TAG, "getOrderList:" +
                "shopType:" + (shopType == SHOP_TYPE_BUY ? "SHOP_TYPE_BUY" : shopType == SHOP_TYPE_SELL ? "SHOP_TYPE_SELL" : null) +
                "|targetUid:" + targetUid +
                "|amout:" + amount +
                "|cursor:" + (cursor != null ? cursor.getData() : null)
        );
        CollectionReference ordersRef = FirestoreManager.getShopOrdersRef();
        String where = null;
        if (shopType == SHOP_TYPE_BUY) where = Order.FIELD_BUYER_ID;
        if (shopType == SHOP_TYPE_SELL) where = Order.FIELD_SELLER_ID;
        Query ordersQuery = ordersRef
                .whereEqualTo(where, targetUid)
                .orderBy(Order.FIELD_ORDER_TIMESTAMP, Query.Direction.DESCENDING);
        if (cursor != null) ordersQuery = ordersQuery.startAfter(cursor);
        ordersQuery = ordersQuery.limit(amount);
        return FirestoreManager.getInstance().get(ordersQuery, Order.class);
    }
}