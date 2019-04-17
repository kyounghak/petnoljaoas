package com.chaigene.petnolja.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Order POJO.
 */
@IgnoreExtraProperties
public class Order extends Request implements Serializable {

    public static final int TYPE_PRODUCT = 0;             // 제품(실물)
    public static final int TYPE_SERVICE = 1;             // 서비스(수업)

    public static final int STATUS_ORDER_READY = 0;                 // 주문준비됨(Idle)

    public static final int STATUS_PAYMENT_REQUEST = 1;             // 결제요청
    public static final int STATUS_PAYMENT_COMPLETE = 2;            // 결제완료(=구매승인대기)

    // 제품(실물)
    public static final int STATUS_WORK_IN_PROGRESS = 3;            // 작업중(=구매승인완료)
    @Deprecated
    public static final int STATUS_WORK_COMPLETE = 4;               // 작업완료
    public static final int STATUS_SHIPPING_IN_PROGRESS = 5;        // 전달중
    public static final int STATUS_SHIPPING_COMPLETE = 6;           // 전달완료

    // 서비스(수업)
    public static final int STATUS_SERVICE_IN_PROGRESS = 7;         // 서비스 진행중
    public static final int STATUS_SERVICE_COMPLETE = 8;            // 서비스 완료(전달완료)

    public static final int STATUS_PURCHASE_COMPETE = 9;            // 구매완료

    public static final int STATUS_ORDER_CANCEL_REQUEST = 100;      // 주문취소요청
    public static final int STATUS_ORDER_CANCEL_COMPLETE = 101;     // 주문취소완료
    public static final int STATUS_ORDER_REJECT_REQUEST = 102;      // 주문거절요청
    public static final int STATUS_ORDER_REJECT_COMPLETE = 103;     // 주문거절완료
    public static final int STATUS_ISSUE_REQUEST = 104;             // 환불/교환요청
    public static final int STATUS_ISSUE_COMPLETE = 105;            // 환불/교환완료

    public static final int ISSUE_CODE_ETC = 10;                    // 기타

    public static final String FIELD_STATUS = "status";
    public static final String FIELD_BUYER_ID = "buyerId";
    public static final String FIELD_SELLER_ID = "sellerId";
    public static final String FIELD_SHIPPING_CARRIER = "shippingCarrier";
    public static final String FIELD_SHIPPING_TRACKING_NO = "shippingTrackingNo";
    public static final String FIELD_ISSUE_CODE = "issueCode";
    public static final String FIELD_ISSUE_MESSAGE = "issueMessage";
    public static final String FIELD_ORDER_TIMESTAMP = "orderTimestamp";

    // 주문 종류(제품, 서비스)
    private int type;
    // 주문진행상황. 주문완료.
    private int status;
    // 주문번호 counters/order-no/seq
    private int orderNo;
    // 주문명
    private String orderName;
    // 상품 UID
    private String productId;
    // 상품 타입
    private int productType;
    // 커버 이미지 파일명
    private String coverPhoto;
    // 커버 이미지 리전
    private List<String> regions;
    // 구매자 UID
    private String buyerId;
    // 구매자 닉네임
    private String buyerNickname;
    // 판매자 UID
    private String sellerId;
    // 판매자 닉네임
    private String sellerNickname;
    // 결제할 카드의 UID
    private String cardId;
    // 결제비밀번호
    private String securityPin;
    // 구매수량
    private int quantity;
    // 낱개 금액
    private int unitPrice;
    // 배송 금액
    private int shippingPrice;
    // 소계 금액(배송비 제외)
    private int subtotalPrice;
    // 결제 금액
    private int totalPrice;
    // 카드할부개월수. 2 이상의 할부개월수 적용. 생략될 경우 일시불. (결제금액 50,000원 이상 한정)
    private int installment;

    private String shippingReceiver;
    private String shippingAddress;
    private String shippingMessage;
    /*
    택배사 코드(Ref: http://info.sweettracker.co.kr/apidoc)

    [국내택배(24개사)]
    건영택배	    18	경동택배	        23	고려택배	    19
    굿투럭	    40	대신택배	        22	로젠택배	    06
    롯데택배	    08	애니트랙	        43	우체국택배	    01
    일양로지스	    11	천일택배	        17	쿠팡 로켓배송	36
    한덱스	    20	한의사랑택배       	16	한진택배	    05
    합동택배	    32	호남택배	        45	CJ대한통운  	04
    CU편의점택배	46	CVSnet 편의점택배	24	KGB택배	    10
    KGL네트웍스	30	KG로지스	        39	SLX	        44

    [국제택배(17개사)] - 사용할 일 없음.
    롯데글로벌 로지스	99	범한판토스	            37	에어보이익스프레스	29
    포시즌익스프레스	    35	APEX(ECMS Express)	38	CJ대한통운 국제특송	42
    DHL	            13	DHL Global Mail	    33	EMS	            12
    Fedex	        21	GSI Express	        41	GSMNtoN(인로스)	28
    i-Parcel	    34	TNT Express	        25	TPL	            27
    UPS	            14	USPS	            26
    */
    private String shippingCarrier;
    // 운송장 번호. 알파벳, 숫자로만 입력가능(공백, 특수문자 X)
    private String shippingTrackingNo;

    private boolean isAutoFinalized;

    private int issueCode;
    private String issueMessage;

    private Map<String, Object> payment;

    // 정산 여부
    private boolean isPaid;

    // 상태가 변경될 때마다 시각을 기록한다.
    private Date orderTimestamp;
    private Date paymentTimestamp;
    private Date workInProgressTimestamp;
    private Date workCompleteTimestamp;
    private Date shippingInProgressTimestamp;
    private Date shippingCompleteTimestamp;
    private Date purchaseCompleteTimestamp;
    private Date orderCancelTimestamp;
    private Date issueRequestTimestamp;
    private Date issueCompleteTimestamp;
    private Date paidTimestamp;

    public Order() {
    }

    public Order(int status,
                 int orderNo,
                 String orderName,
                 String productId,
                 int productType,
                 String coverPhoto,
                 List<String> regions,
                 String buyerId,
                 String buyerNickname,
                 String sellerId,
                 String sellerNickname,
                 String cardId,
                 String securityPin,
                 int quantity,
                 int unitPrice,
                 int shippingPrice,
                 int subtotalPrice,
                 int totalPrice,
                 int installment,
                 String shippingReceiver,
                 String shippingAddress,
                 String shippingMessage) {
        this.status = status;
        this.orderNo = orderNo;
        this.orderName = orderName;
        this.productId = productId;
        this.productType = productType;
        this.coverPhoto = coverPhoto;
        this.regions = regions;
        this.buyerId = buyerId;
        this.buyerNickname = buyerNickname;
        this.sellerId = sellerId;
        this.sellerNickname = sellerNickname;
        this.cardId = cardId;
        this.securityPin = securityPin;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.shippingPrice = shippingPrice;
        this.subtotalPrice = subtotalPrice;
        this.totalPrice = totalPrice;
        this.installment = installment;
        this.shippingReceiver = shippingReceiver;
        this.shippingAddress = shippingAddress;
        this.shippingMessage = shippingMessage;
        this.isPaid = false;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public int getProductType() {
        return productType;
    }

    public void setProductType(int productType) {
        this.productType = productType;
    }

    public String getCoverPhoto() {
        return coverPhoto;
    }

    public void setCoverPhoto(String coverPhoto) {
        this.coverPhoto = coverPhoto;
    }

    public List<String> getRegions() {
        return regions;
    }

    public void setRegions(List<String> regions) {
        this.regions = regions;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public String getBuyerNickname() {
        return buyerNickname;
    }

    public void setBuyerNickname(String buyerNickname) {
        this.buyerNickname = buyerNickname;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public void setSellerNickname(String sellerNickname) {
        this.sellerNickname = sellerNickname;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getSecurityPin() {
        return securityPin;
    }

    public void setSecurityPin(String securityPin) {
        this.securityPin = securityPin;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(int unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getShippingPrice() {
        return shippingPrice;
    }

    public void setShippingPrice(int shippingPrice) {
        this.shippingPrice = shippingPrice;
    }

    public int getSubtotalPrice() {
        return subtotalPrice;
    }

    public void setSubtotalPrice(int subtotalPrice) {
        this.subtotalPrice = subtotalPrice;
    }

    public int getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getInstallment() {
        return installment;
    }

    public void setInstallment(int installment) {
        this.installment = installment;
    }

    public String getShippingReceiver() {
        return shippingReceiver;
    }

    public void setShippingReceiver(String shippingReceiver) {
        this.shippingReceiver = shippingReceiver;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getShippingMessage() {
        return shippingMessage;
    }

    public void setShippingMessage(String shippingMessage) {
        this.shippingMessage = shippingMessage;
    }

    public String getShippingCarrier() {
        return shippingCarrier;
    }

    public void setShippingCarrier(String shippingCarrier) {
        this.shippingCarrier = shippingCarrier;
    }

    public String getShippingTrackingNo() {
        return shippingTrackingNo;
    }

    public void setShippingTrackingNo(String shippingTrackingNo) {
        this.shippingTrackingNo = shippingTrackingNo;
    }

    public boolean isAutoFinalized() {
        return isAutoFinalized;
    }

    public void setAutoFinalized(boolean autoFinalized) {
        isAutoFinalized = autoFinalized;
    }

    public int getIssueCode() {
        return issueCode;
    }

    public void setIssueCode(int issueCode) {
        this.issueCode = issueCode;
    }

    public String getIssueMessage() {
        return issueMessage;
    }

    public void setIssueMessage(String issueMessage) {
        this.issueMessage = issueMessage;
    }

    public Map<String, Object> getPayment() {
        return payment;
    }

    public void setPayment(Map<String, Object> payment) {
        this.payment = payment;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    // Ref: https://firebase.google.com/docs/firestore/reference/android/ServerTimestamp
    @ServerTimestamp
    public Date getOrderTimestamp() {
        return orderTimestamp;
    }

    public void setOrderTimestamp(Date orderTimestamp) {
        this.orderTimestamp = orderTimestamp;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);

        result.put("status", status);
        result.put("orderNo", orderNo);
        result.put("orderName", orderName);

        result.put("productId", productId);
        result.put("productType", productType);
        result.put("coverPhoto", coverPhoto);
        result.put("regions", regions);

        result.put("buyerId", buyerId);
        result.put("buyerNickname", buyerNickname);
        result.put("sellerId", sellerId);
        result.put("sellerNickname", sellerNickname);

        result.put("cardId", cardId);
        result.put("securityPin", securityPin);
        result.put("quantity", quantity);
        result.put("unitPrice", unitPrice);
        result.put("shippingPrice", shippingPrice);
        result.put("subtotalPrice", subtotalPrice);
        result.put("totalPrice", totalPrice);
        result.put("installment", installment);

        result.put("shippingReceiver", shippingReceiver);
        result.put("shippingAddress", shippingAddress);
        result.put("shippingMessage", shippingMessage);
        result.put("shippingCarrier", shippingCarrier);
        result.put("shippingTrackingNo", shippingTrackingNo);

        result.put("issueCode", issueCode);
        result.put("issueMessage", issueMessage);

        result.put("payment", payment);
        result.put("isPaid", isPaid);

        result.put("orderTimestamp", orderTimestamp);
        return result;
    }
}