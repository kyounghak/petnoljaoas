package com.chaigene.petnolja;

public class Constants {
    public static final String CORE_SERVICE_ACTION_ENABLED = "com.pwdr.nacky.CORE_SERVICE_ACTION_ENABLED";
    public static final String CORE_SERVICE_ACTION_DISABLED = "com.pwdr.nacky.CORE_SERVICE_ACTION_DISABLED";
    public static final String MOBILE_DATA_MANUAL_INSERTED_ACTION = "com.pwdr.nacky.MOBILE_DATA_MANUAL_INSERTED_ACTION";

    // Request code
    public static final int RC_SIGN_IN = 1000;

    public static final int CHAT_ACTIVITY = 0;
    public static final int WRITE_ACTIVITY = 1;
    public static final int IMAGE_FILTER_ACTIVITY = 2;
    public static final int PROFILE_EDIT_ACTIVITY = 3;
    public static final int DAUM_POSTCODE_ACTIVITY = 4;
    public static final int CARD_ACTIVITY = 5;
    public static final int CARD_REG_ACTIVITY = 6;
    public static final int SECURITY_PIN_ACTIVITY = 7;
    public static final int ORDER_SUMMARY_ACTIVITY = 8;
    public static final int ID_VERIFICATION_ACTIVITY = 9;

    public static final int ARTICLE_FRAGMENT = 100;
    public static final int HASHTAG_FRAGMENT = 101;
    public static final int SETTING_PROFILE_FRAGMENT = 102;
    public static final int SETTING_SHOPINFO_FRAGMENT = 103;

    public static final int PAGE_HOME = 0;
    public static final int PAGE_EXPLORE = 1;
    public static final int PAGE_GALLERY = 2;
    public static final int PAGE_NOTIFICATION = 3;
    public static final int PAGE_PROFILE = 4;

    public static final String PROVIDER_PASSWORD = "password";
    public static final String PROVIDER_FACEBOOK = "facebook.com";
    public static final String PROVIDER_KAKAO = "kakao";
    public static final String PROVIDER_NAVER = "naver";

    // OldUser
    public static final String DATABASE_PATH_USER = "user";
    public static final String DATABASE_PATH_USER_USERS = "users";
    public static final String DATABASE_PATH_USER_FOLLOW = "follow";
    public static final String DATABASE_PATH_USER_FOLLOW_FOLLOWERS = "followers";
    public static final String DATABASE_PATH_USER_FOLLOW_FOLLOWINGS = "followings";
    // public static final String DATABASE_PATH_USER_PRIVATE_INFOS = "private-infos";

    // OldUser (Firestore)
    public static final String DATABASE_PATH_USER_PRIVATE_INFOS = "userPrivateInfos";

    // Booking
    public static final String DATABASE_PATH_HOTEL_BOOKING_QUOTAS = "hotelBookingQuotas";

    // Search
    public static final String DATABASE_PATH_SEARCH = "search";
    public static final String DATABASE_PATH_SEARCH_USERS = "users";
    public static final String DATABASE_PATH_SEARCH_USERS_QUERIES = "queries";
    public static final String DATABASE_PATH_SEARCH_USERS_RESULTS = "results";

    public static final String DATABASE_PATH_SEARCH_HASHTAGS = "hashtags";
    public static final String DATABASE_PATH_SEARCH_HASHTAGS_QUERIES = "queries";
    public static final String DATABASE_PATH_SEARCH_HASHTAGS_RESULTS = "results";

    // Notification
    public static final String DATABASE_PATH_NOTIFICATION = "notification";
    public static final String DATABASE_PATH_NOTIFICATION_USER_NOTIS = "user-notis";
    public static final String DATABASE_PATH_NOTIFICATION_USER_NOTIS_FEED = "user-notis-feed";
    public static final String DATABASE_PATH_NOTIFICATION_USER_NOTIS_TALENT = "user-notis-talent";
    public static final String DATABASE_PATH_NOTI_STABLE_TOKENS = "noti-stable-tokens";

    // Chat
    public static final String DATABASE_PATH_CHAT = "chat";
    public static final String DATABASE_PATH_CHAT_ROOMS = "rooms";
    public static final String DATABASE_PATH_CHAT_HOST_ROOMS = "host-rooms";
    public static final String DATABASE_PATH_CHAT_USER_ROOMS = "user-rooms";
    public static final String DATABASE_PATH_CHAT_MESSAGES = "messages";
    public static final String DATABASE_PATH_CHAT_USER_MESSAGES = "user-messages";
    public static final String DATABASE_PATH_CHAT_LAST_READED_MESSAGES = "last-readed-messages";
    public static final String DATABASE_PATH_CHAT_TOTAL_UNREAD_COUNT = "total-unread-count";

    // Article
    public static final String DATABASE_PATH_ARTICLE = "article";
    public static final String DATABASE_PATH_ARTICLE_POSTS = "posts";
    public static final String DATABASE_PATH_ARTICLE_POSTS_FEED = "posts-feed";
    public static final String DATABASE_PATH_ARTICLE_POSTS_TALENT = "posts-talent";
    public static final String DATABASE_PATH_ARTICLE_USER_POSTS = "user-posts";
    public static final String DATABASE_PATH_ARTICLE_USER_POSTS_FEED = "user-posts-feed";
    public static final String DATABASE_PATH_ARTICLE_USER_POSTS_TALENT = "user-posts-talent";
    public static final String DATABASE_PATH_ARTICLE_USER_SAVES = "user-saves";
    public static final String DATABASE_PATH_ARTICLE_TIMELINE = "timeline";
    public static final String DATABASE_PATH_ARTICLE_HASHTAGS = "hashtags";
    public static final String DATABASE_PATH_ARTICLE_HASHTAG_COUNT = "hashtag-count";
    public static final String DATABASE_PATH_ARTICLE_COMMENTS = "comments";
    public static final String DATABASE_PATH_ARTICLE_COMMENT_COUNT = "comment-count";

    // Abuse
    public static final String DATABASE_PATH_ABUSE = "abuse";
    public static final String DATABASE_PATH_ABUSE_ABUSES = "abuses";

    public static final String DATABASE_PATH_SIGNATURES = "signatures";

    // User
    public static final String DATABASE_PATH_USERS = "users";

    // Event
    public static final String DATABASE_PATH_EVENTS = "events";

    // Requests
    public static final String DATABASE_PATH_REQUESTS = "requests";

    // Shop
    public static final String DATABASE_PATH_SHOP_ORDERS = "shop-orders";
    public static final String DATABASE_PATH_REQUEST_SHOP_ORDERS = "request-shop-orders";
    public static final String DATABASE_PATH_RESPONSE_SHOP_ORDERS = "response-shop-orders";
    public static final String DATABASE_PATH_SHOP_CARDS = "shop-cards";
    public static final String DATABASE_PATH_REQUEST_SHOP_CARDS = "request-shop-cards";
    public static final String DATABASE_PATH_RESPONSE_SHOP_CARDS = "response-shop-cards";
    public static final String DATABASE_PATH_SHOP_SECURITY_PINS = "shop-security-pins";
    public static final String DATABASE_PATH_COUNTERS = "counters";
    public static final String DATABASE_PATH_COUNTERS_ORDER_NO = "order-no";
    public static final String DATABASE_PATH_COUNTERS_BILLING_KEY_NO = "billing-key-no";

    // Storage
    @Deprecated
    public static final String STORAGE_REGION_USA = "usa";
    public static final String STORAGE_REGION_TOKYO = "tokyo";

    public static final String STORAGE_PATH_IMAGES = "images";
    public static final String STORAGE_PATH_USER = "user";
    public static final String STORAGE_PATH_PROFILE_IMAGE = "profile";

    // Event
    public static final String STORAGE_PATH_EVENTS = "events";

    public static final String STORAGE_PATH_ARTICLE = "article";
    public static final String STORAGE_PATH_ARTICLE_POSTS = "posts";

    public static final int ARTICLE_SCOPE_ALL = 0;
    public static final int ARTICLE_SCOPE_PROFILE = 1;
    public static final int ARTICLE_SCOPE_SAVE = 2;

    public static final int ARTICLE_TYPE_ALL = 0;
    public static final int ARTICLE_TYPE_FEED = 1;
    public static final int ARTICLE_TYPE_TALENT = 2;

    public static final int FOLLOW_TYPE_FOLLOWER = 0;
    public static final int FOLLOW_TYPE_FOLLOWING = 1;

    public static final int SEARCH_TYPE_USER = 0;
    public static final int SEARCH_TYPE_HASHTAG = 1;

    public static final int SHOP_TYPE_BUY = 0;
    public static final int SHOP_TYPE_SELL = 1;

    // Intent values
    public static final String EXTRA_HELPER = "extra_child_fragment";
    public static final String EXTRA_CHILD_FRAGMENT = "extra_child_fragment";
    public static final String EXTRA_PAGE_FRAGMENT = "extra_page_fragment";
    public static final String EXTRA_REMOTE_MESSAGE = "extra_remote_message";
    public static final String EXTRA_TARGET_USER = "extra_target_user";
    public static final String EXTRA_TARGET_USER_ID = "extra_target_user_id";
    public static final String EXTRA_TARGET_NICKNAME = "extra_target_nickname";
    public static final String EXTRA_TARGET_PATHS = "extra_target_paths";
    public static final String EXTRA_TARGET_REGIONS = "extra_target_regions";
    public static final String EXTRA_TARGET_POSITION = "extra_target_position";
    public static final String EXTRA_TARGET_HASHTAG = "extra_target_hashtag";
    public static final String EXTRA_TARGET_POST = "extra_target_post";
    public static final String EXTRA_TARGET_POST_ID = "extra_target_post_id";
    public static final String EXTRA_TARGET_ROOM_ID = "extra_target_room_uid";
    public static final String EXTRA_TARGET_FRAGMENT = "extra_target_fragment";
    public static final String EXTRA_TARGET_CARD = "extra_target_card";
    public static final String EXTRA_ARTICLE_SCOPE = "extra_article_scope";
    public static final String EXTRA_ARTICLE_TYPE = "extra_article_type";
    public static final String EXTRA_FOLLOW_TYPE = "extra_follow_type";
    public static final String EXTRA_SEARCH_TYPE = "extra_search_type";
    public static final String EXTRA_IS_MODIFY = "extra_is_modify";
    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_IMAGE_URIS = "extra_image_uris";
    public static final String EXTRA_ACTION_STATUS = "extra_action_status";
    public static final String EXTRA_TOOLBAR_TITLE = "extra_toolbar_title";
    public static final String EXTRA_URL = "extra_url";

    public static final String EXTRA_SHOP_TYPE = "extra_shop_type";
    public static final String EXTRA_ORDER_ID = "extra_order_id";
    public static final String EXTRA_PRODUCT_ID = "extra_product_id";
    public static final String EXTRA_PRODUCT_TYPE = "extra_product_type";
    public static final String EXTRA_PRODUCT_TITLE = "extra_product_title";
    public static final String EXTRA_PRODUCT_PRICE = "extra_product_price";
    public static final String EXTRA_COVER_PHOTO = "extra_cover_photo";
    public static final String EXTRA_QUANTITY = "extra_quantity";
    public static final String EXTRA_SHIPPING_PRICE = "extra_shipping_price";
    public static final String EXTRA_SELLER_ID = "extra_seller_id";
    public static final String EXTRA_SELLER_NICKNAME = "extra_seller_nickname";
    public static final String EXTRA_SHIPPING_RECEIVER = "extra_shipping_receiver";
    public static final String EXTRA_SHIPPING_ADDRESS = "extra_shipping_address";
    public static final String EXTRA_SHIPPING_MESSAGE = "extra_shipping_message";
    public static final String EXTRA_CARD_ID = "extra_card_id";
    public static final String EXTRA_INSTALLMENT = "extra_installment";

    public static final int ACTION_STATUS_DEFAULT = 0;
    public static final int ACTION_STATUS_ARTICLE = 1;
    public static final int ACTION_STATUS_PROFILE = 2;
    public static final int ACTION_STATUS_SHOP = 3;
    public static final int ACTION_STATUS_CHAT = 4;

    // Image option
    public static final int IMAGE_OPTION_MAX_COUNT = 5;
    public static final int IMAGE_OPTION_QUALITY = 90;
    public static final int IMAGE_OPTION_MAX_WIDTH = 1080;
    public static final int IMAGE_OPTION_MAX_HEIGHT = 1440;
    public static final int IMAGE_OPTION_PROFILE_MAX_WIDTH = 1080;
    public static final int IMAGE_OPTION_PROFILE_MAX_HEIGHT = 1080;
    public static final String IMAGE_OPTION_CACHE_FILENAME = "photo_cache_%d.jpg";
    public static final String PROFILE_IMAGE_FILENAME = "profile.jpg";

    // List count
    public static final int COUNT_ARTICLE_TIMELINE = 3;
    // TODO: 스크린이 크면 무한로딩이 호출되지 않는 버그가 발생한다.
    // 추후에는 스크린 사이즈에 따라서 불러올 갯수를 자동으로 추출하는 방식으로 수정한다.
    public static final int COUNT_ARTICLE_EXPLORE = 15;
    public static final int COUNT_ARTICLE_PROFILE = 12;
    public static final int COUNT_ARTICLE_SAVE = 12;
    public static final int COUNT_NOTIFICATION = 10;
    public static final int COUNT_FOLLOW_LIST = 10;
    public static final int COUNT_SHOP_ORDER = 10;
    public static final int COUNT_SHOP_CARD = 10;

    // 택배사 코드(Ref: http://info.sweettracker.co.kr/apidoc)
    public static final String SHIPPING_CARRIER_건영택배 = "18";
    public static final String SHIPPING_CARRIER_경동택배 = "23";
    public static final String SHIPPING_CARRIER_고려택배 = "19";
    public static final String SHIPPING_CARRIER_굿투럭 = "40";
    public static final String SHIPPING_CARRIER_대신택배 = "22";
    public static final String SHIPPING_CARRIER_로젠택배 = "06";
    public static final String SHIPPING_CARRIER_롯데택배 = "08";
    public static final String SHIPPING_CARRIER_애니트랙 = "43";
    public static final String SHIPPING_CARRIER_우체국택배 = "01";
    public static final String SHIPPING_CARRIER_일양로지스 = "11";
    public static final String SHIPPING_CARRIER_천일택배 = "17";
    public static final String SHIPPING_CARRIER_쿠팡로켓배송 = "36";
    public static final String SHIPPING_CARRIER_한덱스 = "20";
    public static final String SHIPPING_CARRIER_한의사랑택배 = "16";
    public static final String SHIPPING_CARRIER_한진택배 = "05";
    public static final String SHIPPING_CARRIER_합동택배 = "32";
    public static final String SHIPPING_CARRIER_호남택배 = "45";
    public static final String SHIPPING_CARRIER_CJ대한통운 = "04";
    public static final String SHIPPING_CARRIER_CU편의점택배 = "46";
    public static final String SHIPPING_CARRIER_CVSnet편의점택배 = "24";
    public static final String SHIPPING_CARRIER_KGB택배 = "10";
    public static final String SHIPPING_CARRIER_KGL네트웍스 = "30";
    public static final String SHIPPING_CARRIER_KG로지스 = "39";
    public static final String SHIPPING_CARRIER_SLX = "44";
}