package com.xsc.util.azure;

import com.google.common.collect.Maps;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import com.xsc.util.ConfigUtils;
import com.xsc.util.assertions.XAssert;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 微软云上传工具类
 *
 * @author xia
 * @date 2019/7/30 17:57
 */
public class AzureCloudBlobUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudBlobUtils.class);

    private static final String CONNECTION_STRING = ConfigUtils.getPropertyByKey("azure.connection");

    private static final long MAX_FILE_SIZE = Arrays.stream(ConfigUtils.getPropertyByKey("azure.max-file-size").split("\\*"))
            .map(Long::parseLong).reduce(1L, (x, y) -> x * y);

    private static Map<ContainerEnum, CloudBlobContainer> containerMap = Maps.newHashMap();

    private static CloudStorageAccount storageAccount;

    static {
        try {
            storageAccount = CloudStorageAccount.parse(CONNECTION_STRING);
        } catch (URISyntaxException | InvalidKeyException e) {
            LOGGER.error("Error in connection Azure! {}, {}", e.getMessage(), e);
        }
    }

    /**
     * 创建一个带访问控制权限的容器(容器第一次创建的时候，设置好权限，以后不允许更改)
     *
     * @param containerEnum 容器名称 + 访问权限
     * @return 容器对象
     */
    private static CloudBlobContainer createContainer(ContainerEnum containerEnum) {
        Objects.requireNonNull(containerEnum, "container name is null!");
        if (containerMap.containsKey(containerEnum)) {
            return containerMap.get(containerEnum);
        }
        CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
        try {
            CloudBlobContainer container = blobClient.getContainerReference(containerEnum.name);
            // 默认是禁止匿名访问
            boolean flag = container.createIfNotExists();
            if (flag) {
                BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
                containerPermissions.setPublicAccess(BlobContainerPublicAccessType.valueOf(containerEnum.accessType.name()));
                container.uploadPermissions(containerPermissions);
            }
            containerMap.put(containerEnum, container);
            return container;
        } catch (URISyntaxException | StorageException e) {
            LOGGER.error("Azure container upload permission error! {}, {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 获取blob对象
     *
     * @param containerEnum 容器名称 + 容器的访问权限
     * @param blobName      blob名称
     * @return blob对象
     */
    private static CloudBlockBlob getBlockBlob(ContainerEnum containerEnum, String blobName) {
        CloudBlobContainer container = createContainer(containerEnum);
        try {
            assert container != null;
            return container.getBlockBlobReference(blobName);
        } catch (URISyntaxException | StorageException e) {
            LOGGER.error("File cloud get block blob error! {}, {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 上传文件
     *
     * @param stream        文件流
     * @param containerEnum 容器名称 + 容器的访问权限
     * @param blobName      文件名称
     * @param length        文件大小
     * @return 上传结果
     */
    public static UploadResult upload(InputStream stream, ContainerEnum containerEnum, String blobName, long length) {
        return upload(stream, containerEnum, blobName, length, 0);
    }

    /**
     * 上传文件（支持自定义文件上传限制大小）
     *
     * @param stream        文件流
     * @param containerEnum 容器名称 + 容器的访问权限
     * @param blobName      文件名称
     * @param length        文件大小
     * @param maxFileSize   自定义文件上传大小限制
     * @return 上传结果
     */
    public static UploadResult upload(InputStream stream, ContainerEnum containerEnum, String blobName, long length,
                                      long maxFileSize) {
        // 如果传入了maxFileSize，就以maxFileSize的大小作为文件上传大小限制
        UploadResult result = new UploadResult();
        try {
            if (maxFileSize > 0) {
                XAssert.isTrue(length <= maxFileSize, "Upload file size must not exceed %s!", maxFileSize);
            }
            // 如果没有传递maxFileSize或者maxFileSize的值不大于0，就走默认限制文件大小
            else {
                XAssert.isTrue(length <= MAX_FILE_SIZE, "Upload file size must not exceed %s!", MAX_FILE_SIZE);
            }
            StopWatch stopWatch = StopWatch.createStarted();
            stopWatch.split();
            CloudBlobContainer container = createContainer(containerEnum);
            String renameFileName = DateFormatUtils.format(new Date(), "yyyyMMdd") + File.separator
                    + UUID.randomUUID().toString().replaceAll("-", "") + StringUtils.substring(blobName, blobName.lastIndexOf('.'));
            assert container != null;
            CloudBlockBlob blockBlob = getBlockBlob(containerEnum, renameFileName);
            assert blockBlob != null;
            LOGGER.info("Upload file preparation time {} ms", stopWatch.getSplitTime());
            blockBlob.upload(stream, length);
            LOGGER.info("Overall time to upload files {} ms", stopWatch.getTime());
            result.setOriginFileName(blobName);
            result.setFileUrl(blockBlob.getUri().toString());
            result.setRenameFileName(renameFileName);
            if (!container.getProperties().getPublicAccess().equals(BlobContainerPublicAccessType.OFF)) {
                result.setOpenFlag(true);
            }
        } catch (StorageException | IOException e) {
            LOGGER.error("File cloud blob file not found! {}, {}", e.getMessage(), e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    LOGGER.error("File upload input stream close error! {}, {}", e.getMessage(), e);
                }
            }
        }
        return result;
    }

    /**
     * 删除一个附件
     *
     * @param containerEnum 容器名称 + 容器的访问权限
     * @param blobName      blob 名称
     * @return 是否操作成功
     */
    public static boolean delete(ContainerEnum containerEnum, String blobName) {
        CloudBlockBlob blockBlob = getBlockBlob(containerEnum, blobName);
        try {
            assert blockBlob != null;
            return blockBlob.deleteIfExists();
        } catch (StorageException e) {
            LOGGER.error("File cloud delete blob error! {}, {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 获取ASA加密的访问链接
     *
     * @param blockBlob blob对象
     * @param seconds   过期秒数 (时间不宜设的过小)
     * @return 限时访问的链接
     */
    private static String getASAUrl(CloudBlockBlob blockBlob, int seconds) {
        SharedAccessBlobPolicy policy = new SharedAccessBlobPolicy();
        policy.setSharedAccessStartTime(new Date());
        if (seconds > 0) {
            policy.setSharedAccessExpiryTime(DateUtils.addSeconds(new Date(), seconds));
        } else {
            // 默认过期时间为60s
            policy.setSharedAccessExpiryTime(DateUtils.addSeconds(new Date(), 60));
        }
        // SAS 只读权限
        policy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
        try {
            return blockBlob.getUri() + "?" + blockBlob.generateSharedAccessSignature(policy, null);
        } catch (InvalidKeyException | StorageException e) {
            LOGGER.error("File cloud obtain asa url error! {}, {}", e.getMessage(), e);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 对外提供获取ASA加密的访问链接
     *
     * @param containerName 容器名称
     * @param blobName      blob名称
     * @return 限时访问的链接
     */
    public static String getASAUrl(ContainerEnum containerName, String blobName) {
        return getASAUrl(Objects.requireNonNull(getBlockBlob(containerName, blobName)), 0);
    }

    /**
     * 对外提供获取ASA加密的访问链接(支持自定义过期时间)
     *
     * @param containerName 容器名称
     * @param blobName      blob名称
     * @param seconds       过期秒数（当前时间 + seconds 秒 后过期，不宜设的过小）
     * @return 限时访问链接
     */
    public static String getASAUrl(ContainerEnum containerName, String blobName, int seconds) {
        return getASAUrl(Objects.requireNonNull(getBlockBlob(containerName, blobName)), seconds);
    }

    /**
     * 通过url获取容器名称和blob名称
     * fileUrl:https://blob2019.blob.core.windows.net/public/20190506/9553f9d071ed4b4780a4214e72c1d.jpg
     *
     * @param fileUrl url
     * @return 容器名称和blob名称
     */
    public static ImmutablePair<ContainerEnum, String> getContainerAndBlobNameByUrl(String fileUrl) {
        try {
            URL url = new URL(Objects.requireNonNull(fileUrl));
            // 获取url的path：/public/20190506/9553f9d071ed4b4780a4214e72c1d.jpg
            String path = url.getPath();
            // 行首分隔符
            int firstDelimiter = 1;
            if (StringUtils.isNotBlank(path) && path.length() > firstDelimiter) {
                // 去掉行首的 "/",path：public/20190506/9553f9d071ed4b4780a4214e72c1d.jpg
                path = path.substring(firstDelimiter);
                // 按文件分隔符截取成2部分
                int twoPart = 2;
                String[] strings = StringUtils.split(path, File.separator, twoPart);
                // public
                String containerName = strings[0];
                // 20190506/9553f9d071ed4b4780a4214e72c1d.jpg
                String blobName = strings[1];
                return ImmutablePair.of(ContainerEnum.getByName(containerName), blobName);
            }
        } catch (MalformedURLException e) {
            LOGGER.error("Parse url error! {}, {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 设置容器的访问权限
     */
    enum AccessType {
        // 只允许单个文件级别的访问控制
        BLOB,
        // 只允许单个容器界别的访问控制
        CONTAINER,
        // 不对外提供访问权限，只有容器的owner可以访问
        OFF
    }

    /**
     * 容器名称（定义成枚举，以免容器不好维护，容器名称必须为小写字母、数字和连接符，且必须是字母或数字开头）
     */
    public enum ContainerEnum {

        /**
         * 容器名称 + 容器的访问权限
         */
        ACCESS("access", AccessType.OFF),
        PUBLIC("public", AccessType.CONTAINER);

        String name;
        AccessType accessType;

        ContainerEnum(String name, AccessType accessType) {
            this.name = name;
            this.accessType = accessType;
        }

        public static ContainerEnum getByName(String name) {
            ContainerEnum[] containerEnums = ContainerEnum.class.getEnumConstants();
            for (ContainerEnum containerEnum : containerEnums) {
                if (containerEnum.name.equalsIgnoreCase(name)) {
                    return containerEnum;
                }
            }
            return null;
        }
    }

}
