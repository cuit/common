package com.cm.util.page;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author nobody
 * @date 2019/1/28 13:54
 */
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 4036078293839337538L;

    private Page page = new Page(1, 10, null);

    public PageRequest() {
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }

    /**
     * 获取偏移量
     */
    @JsonIgnore
    public int getOffset() {
        if (page == null) {
            return 0;
        }
        int temp = (page.pageNo - 1) * page.pageSize;
        return temp < 0 ? 0 : temp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PageRequest)) {
            return false;
        }
        PageRequest that = (PageRequest) o;
        return getPage().equals(that.getPage());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPage());
    }

    public static class Page implements Serializable {

        private static final long serialVersionUID = -6191204253367254888L;

        /**
         * 页码
         */
        private int pageNo = 1;

        /**
         * 每页显示大小
         */
        private int pageSize = 10;

        /**
         * 排序
         */
        private Sort sort;

        public Page() {
        }

        public Page(int pageNo) {
            this(pageNo, 10);
        }

        public Page(int pageNo, int pageSize) {
            this(pageNo, pageSize, null);
        }

        public Page(int pageNo, int pageSize, Sort sort) {
            this.pageNo = pageNo < 1 ? 1 : pageNo;
            this.pageSize = pageSize < 1 ? 1 : pageSize;
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

        public Sort getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = Sort.fromString(sort);
        }

        public void setSort(Sort sort) {
            this.sort = sort;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Page)) {
                return false;
            }
            Page page = (Page) o;
            return getPageNo() == page.getPageNo() &&
                    getPageSize() == page.getPageSize() &&
                    getSort().equals(page.getSort());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPageNo(), getPageSize(), getSort());
        }

        @Override
        public String toString() {
            return "Page{" +
                    "pageNo=" + pageNo +
                    ", pageSize=" + pageSize +
                    ", sort=" + sort +
                    '}';
        }
    }
}

