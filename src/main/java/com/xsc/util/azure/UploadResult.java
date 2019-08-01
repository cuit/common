package com.xsc.util.azure;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @author xia
 * @date 2019/7/30 18:00
 */
public class UploadResult {

    /**
     * 上传的原始文件名
     */
    private String originFileName;

    /**
     * 重命名之后的文件名
     */
    private String renameFileName;

    /**
     * 文件的访问url（只有访问权限是公开，此url才可以直接访问，否则需要再次请求后端获取ASA限时访问链接）
     */
    private String fileUrl;

    /**
     * 是否是公开访问的
     */
    private boolean openFlag;

    public String getOriginFileName() {
        return originFileName;
    }

    public void setOriginFileName(String originFileName) {
        this.originFileName = originFileName;
    }

    public String getRenameFileName() {
        return renameFileName;
    }

    public void setRenameFileName(String renameFileName) {
        this.renameFileName = renameFileName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public boolean isOpenFlag() {
        return openFlag;
    }

    public void setOpenFlag(boolean openFlag) {
        this.openFlag = openFlag;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
