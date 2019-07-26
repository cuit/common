package com.xsc.util.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @author nobody
 * @date 2019/1/29 17:58
 */
public class Pager<T> implements Serializable {

    private static final long serialVersionUID = -4207240896912124652L;

    private PageData page;

    private List<T> data;

    public Pager() {
    }

    @JsonCreator
    public Pager(@JsonProperty("pageData") PageData page, @JsonProperty("data") List<T> data) {
        this.page = page;
        this.data = Lists.newArrayList();
        this.data.addAll(data);
    }

    public static <T> Builder<T> builder(List<T> data) {
        return new Builder<T>().data(data);
    }

    public PageData getPage() {
        return page;
    }

    public void setPage(PageData page) {
        this.page = page;
    }

    public List<T> getData() {
        return data == null ? Lists.newArrayList() : data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public static class Builder<T> {

        private PageRequest.Page page;

        private List<T> data;

        private int totalSize = 0;

        private Builder() {
        }

        public Builder<T> current(PageRequest.Page page) {
            this.page = page;
            return this;
        }

        public Builder<T> total(int totalSize) {
            this.totalSize = totalSize;
            return this;
        }

        public Builder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public Pager<T> create() {
            return new Pager<>(new PageData(page, this.totalSize), data);
        }

    }

    public static class PageData {

        private int pageNo;

        private int pageSize;

        private int totalSize;

        private Sort sort;

        public PageData(int pageNo, int pageSize, int totalSize) {
            this(pageNo, pageSize, totalSize, null);
        }

        public PageData(PageRequest.Page page, int totalSize) {
            if (page != null) {
                this.pageNo = page.getPageNo();
                this.pageSize = page.getPageSize();
                this.sort = page.getSort();
            }
            this.totalSize = totalSize;
        }

        @JsonCreator
        public PageData(@JsonProperty("pageNo") int pageNo, @JsonProperty("pageSize") int pageSize, @JsonProperty("totalSize") int totalSize, @JsonProperty("sort") Sort sort) {
            this.pageNo = pageNo;
            this.pageSize = pageSize;
            this.totalSize = totalSize;
            this.sort = sort;
        }

        public int getPageNo() {
            return pageNo;
        }

        public void setPageNo(int pageNo) {
            this.pageNo = pageNo;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }

        public int getTotalSize() {
            return totalSize;
        }

        public void setTotalSize(int totalSize) {
            this.totalSize = totalSize;
        }

        public Sort getSort() {
            return sort;
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }

        public boolean hasPreviousPage() {
            return getPageNo() > 1;
        }

        public boolean isFirstPage() {
            return !hasPreviousPage();
        }

        public int getTotalPage() {
            return getPageSize() == 0 ? 1 : (int) Math.ceil((double) totalSize / (double) getPageSize());
        }

        public boolean hasNextPage() {
            return getPageNo() < getTotalPage();
        }

        public boolean isLastPage() {
            return !hasNextPage();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PageData)) {
                return false;
            }
            PageData pageData = (PageData) o;
            return getPageNo() == pageData.getPageNo() &&
                    getPageSize() == pageData.getPageSize() &&
                    getTotalSize() == pageData.getTotalSize() &&
                    getSort() == pageData.getSort();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPageNo(), getPageSize(), getTotalSize(), getSort());
        }

        @Override
        public String toString() {
            return "PageData{" +
                    "pageNo=" + pageNo +
                    ", pageSize=" + pageSize +
                    ", totalSize=" + totalSize +
                    ", sort=" + sort +
                    '}';
        }
    }
}
