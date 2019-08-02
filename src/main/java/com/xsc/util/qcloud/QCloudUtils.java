package com.xsc.util.qcloud;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.AnonymousCOSCredentials;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.CreateBucketRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import com.xsc.util.ConfigUtils;
import com.xsc.util.azure.UploadResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯云上传工具类
 *
 * @author xia
 * @date 2019/8/1 20:47
 */
public class QCloudUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(QCloudUtils.class);

    private static final String SECRET_ID = ConfigUtils.getPropertyByKey("qcloud.secretId");

    private static final String SECRET_KEY = ConfigUtils.getPropertyByKey("qcloud.secretKey");

    private static final String APP_ID = ConfigUtils.getPropertyByKey("qcloud.appId");

    private static final String REGION_AREA = ConfigUtils.getPropertyByKey("qcloud.regionArea");

    private static final int EXPIRATION = 10;

    private static ConcurrentMap<BucketEnum, Bucket> bucketMap = Maps.newConcurrentMap();

    private static ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("qcloud-pool-%d").build();

    private static ExecutorService executorService = new ThreadPoolExecutor(4, 4,
            10, TimeUnit.SECONDS, new LinkedBlockingDeque<>(100), threadFactory, new ThreadPoolExecutor.AbortPolicy());

    private static COSClient CLIENT;

    private static COSClient ANONYMOUS_CLIENT;

    private static TransferManager TRANSFER_MANAGER;

    static {
        // 初始化云的 client 端
        try {
            COSCredentials cred = new BasicCOSCredentials(SECRET_ID, SECRET_KEY);
            Region region = new Region(StringUtils.isNotBlank(REGION_AREA) ? REGION_AREA : REGION.BEI_JING.getName());
            ClientConfig clientConfig = new ClientConfig(region);
            CLIENT = new COSClient(cred, clientConfig);
            COSCredentials anonymous = new AnonymousCOSCredentials();
            ANONYMOUS_CLIENT = new COSClient(anonymous, clientConfig);
            TRANSFER_MANAGER = new TransferManager(CLIENT, executorService);
        } catch (Exception e) {
            LOGGER.error("Error create client in QCloud!", e);
        }
    }

    /**
     * 获取容器的bucket
     *
     * @param bucketEnum 容器
     * @return bucket
     */
    private static Bucket getBucket(BucketEnum bucketEnum) {
        if (bucketMap.containsKey(bucketEnum)) {
            return bucketMap.getOrDefault(bucketEnum, createBucket(bucketEnum));
        }
        Bucket bucket = createBucket(bucketEnum);
        bucketMap.putIfAbsent(bucketEnum, bucket);
        return bucket;
    }

    /**
     * 创建文件存储的bucket
     *
     * @param bucketEnum 容器
     * @return bucket
     */
    private static Bucket createBucket(BucketEnum bucketEnum) {
        // bucket的命名规则必须遵守 BucketName-APP_ID
        String bucketName = bucketEnum.getName();
        // 首先判断云上是否已存在容器
        boolean bucketExistFlag = CLIENT.doesBucketExist(bucketName);
        if (bucketExistFlag) {
            return new Bucket(bucketName);
        }
        CreateBucketRequest request = new CreateBucketRequest(bucketName);
        request.setCannedAcl(bucketEnum.getCannedAccessControlList());
        Bucket bucket = null;
        try {
            bucket = CLIENT.createBucket(request);
        } catch (CosServiceException serverException) {
            LOGGER.error("Service error create bucket in QCloud!", serverException);
        } catch (CosClientException clientException) {
            LOGGER.error("Client error create bucket in QCloud!", clientException);
        }
        return bucket;
    }

    /**
     * 文件上传
     *
     * @param bucketEnum     容器桶
     * @param inputStream    文件输入流
     * @param originFileName 文件的原始名称
     * @return 上传结果
     */
    public static UploadResult upload(BucketEnum bucketEnum, InputStream inputStream, String originFileName, long size) {
        String renameFileName = DateFormatUtils.format(new Date(), "yyyyMMdd") + File.separator
                + StringUtils.substring(originFileName, 0, originFileName.lastIndexOf('.'))
                + UUID.randomUUID().toString().replaceAll("-", "") + StringUtils.substring(originFileName, originFileName.lastIndexOf('.'));
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        com.qcloud.cos.model.UploadResult upload = upload(bucketEnum, inputStream, renameFileName, metadata);
        if (Objects.isNull(upload)) {
            LOGGER.error("上传失败!");
        }
        UploadResult result = new UploadResult();
        result.setOriginFileName(originFileName);
        result.setRenameFileName(renameFileName);
        String fileUrl;
        if (!bucketEnum.getCannedAccessControlList().equals(CannedAccessControlList.Private)) {
            fileUrl = getAnonymousUrl(bucketEnum, renameFileName).toString();
            result.setOpenFlag(true);
        } else {
            fileUrl = bucketEnum.getName() + File.separator + renameFileName;
            result.setOpenFlag(false);
        }
        result.setFileUrl(fileUrl);
        return result;
    }

    /**
     * 上传文件
     *
     * @param bucketEnum  容器
     * @param inputStream 文件流
     * @param key         对象键（Key）是对象在存储桶中的唯一标识
     * @param metadata    除用户自定义元信息以外的其他头部
     * @return 上传成功对象
     */
    private static com.qcloud.cos.model.UploadResult upload(BucketEnum bucketEnum, InputStream inputStream, String key, ObjectMetadata metadata) {
        com.qcloud.cos.model.UploadResult result = null;
        try {
            Bucket bucket = getBucket(bucketEnum);
            String bucketName = bucket.getName();
            PutObjectRequest request = new PutObjectRequest(bucketName, key, inputStream, metadata);
            Upload upload = TRANSFER_MANAGER.upload(request);
            result = upload.waitForUploadResult();
        } catch (CosClientException | InterruptedException clientException) {
            LOGGER.error("Client error upload in QCloud!", clientException);
        }
        return result;
    }

    /**
     * 删除文件
     *
     * @param fileUrl 文件的url
     */
    public static void delete(String fileUrl) {
        ImmutablePair<BucketEnum, String> immutablePair = parsingFileUrl(fileUrl);
        CLIENT.deleteObject(immutablePair.left.getName(), immutablePair.right);
    }

    /**
     * 生成匿名（公开）访问链接 （除私有读，私有写桶之外）
     *
     * @param bucketEnum 容器桶
     * @param key        对象键（Key）是对象在存储桶中的唯一标识
     * @return URL
     */
    private static URL getAnonymousUrl(BucketEnum bucketEnum, String key) {
        if (bucketEnum.getCannedAccessControlList().equals(CannedAccessControlList.Private)) {
            LOGGER.error("Private read private write bucket cannot set anonymous access link!");
        }
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketEnum.getName(), key, HttpMethodName.GET);
        return ANONYMOUS_CLIENT.generatePresignedUrl(request);
    }

    /**
     * 获取限时访问链接
     *
     * @param fileUrl 需要生成限时访问链接的url
     * @return URL
     */
    public static URL getLimitedAccessUrl(String fileUrl) {
        return getLimitedAccessUrl(fileUrl, 0);
    }

    /**
     * 获取限时访问链接
     *
     * @param fileUrl 需要生成限时访问链接的url
     * @param seconds 过期时间
     * @return URL
     */
    public static URL getLimitedAccessUrl(String fileUrl, int seconds) {
        ImmutablePair<BucketEnum, String> immutablePair = parsingFileUrl(fileUrl);
        // 点击链接是直接下载而不是预览
        ResponseHeaderOverrides responseHeaderOverrides = new ResponseHeaderOverrides();
        responseHeaderOverrides.setContentType("application/octet-stream");
        return getLimitedAccessUrl(immutablePair.getLeft(), immutablePair.getRight(), responseHeaderOverrides, seconds);
    }

    /**
     * 生成带签名的下载链接
     *
     * @param bucketEnum              容器桶
     * @param key                     对象键（Key）是对象在存储桶中的唯一标识
     * @param responseHeaderOverrides 重写response的header
     * @param seconds                 过期时间
     * @return URL
     */
    private static URL getLimitedAccessUrl(BucketEnum bucketEnum, String key, ResponseHeaderOverrides responseHeaderOverrides, int seconds) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketEnum.getName(), key, HttpMethodName.GET);
        if (Objects.nonNull(responseHeaderOverrides)) {
            request.setResponseHeaders(responseHeaderOverrides);
        }
        Date expiration;
        if (seconds > 0) {
            expiration = DateUtils.addSeconds(new Date(), seconds);
        } else {
            expiration = DateUtils.addSeconds(new Date(), EXPIRATION);
        }
        request.setExpiration(expiration);
        return CLIENT.generatePresignedUrl(request);
    }

    /**
     * @param fileUrl 文件的访问URL
     * @return 生成容器桶以及key
     */
    private static ImmutablePair<BucketEnum, String> parsingFileUrl(String fileUrl) {
        try {
            URL url = new URL(Objects.requireNonNull(fileUrl));
            fileUrl = StringUtils.split(url.getHost(), ".")[0] + url.getPath();
        } catch (MalformedURLException ignored) {
        }
        String[] paths = StringUtils.split(fileUrl, File.separator, 2);
        assert paths.length == 2;
        String bucketName = paths[0];
        String key = paths[1];
        return ImmutablePair.of(BucketEnum.getByName(bucketName), key);
    }

    /**
     * 腾讯云对象存储 COS 支持多地域存储，不同地区的默认访问域名不同。创建存储桶时选定的地域不可修改，
     * 建议根据自己的业务场景选择就近的地域存储，可以提高对象上传、下载速度。
     */
    enum REGION {
        // 地区
        BEI_JING("ap-beijing"),
        HONG_KONG("ap-hongkong"),
        SINGAPORE("ap-singapore");

        String name;

        REGION(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 容器桶定义及访问权限
     */
    public enum BucketEnum {
        /**
         * @see CannedAccessControlList#PublicRead 公有读私有写(owner 可以读写， 其他客户可以读）
         * @see CannedAccessControlList#Private 私有读写（仅有 owner 可以读写）
         */
        OPEN("open", CannedAccessControlList.PublicRead),
        PRIVATE("private", CannedAccessControlList.Private);

        /**
         * 容器名称 (必须遵守规范：BucketName-AppID)
         */
        String name;

        /**
         * 容器的访问权限
         */
        CannedAccessControlList cannedAccessControlList;

        BucketEnum(String name, CannedAccessControlList cannedAccessControlList) {
            this.name = name;
            this.cannedAccessControlList = cannedAccessControlList;
        }

        public String getName() {
            return name + "-" + APP_ID;
        }

        public CannedAccessControlList getCannedAccessControlList() {
            return cannedAccessControlList;
        }

        public static BucketEnum getByName(String name) {
            BucketEnum[] bucketEnums = BucketEnum.class.getEnumConstants();
            for (BucketEnum bucketEnum : bucketEnums) {
                if (StringUtils.equalsIgnoreCase(bucketEnum.getName(), name)) {
                    return bucketEnum;
                }
            }
            return null;
        }
    }
}
