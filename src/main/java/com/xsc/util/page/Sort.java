package com.xsc.util.page;

import com.xsc.util.assertions.XAssert;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * @author nobody
 * @date 2019/1/29 18:08
 */
public class Sort implements Serializable {

    private static final long serialVersionUID = -8086470374568383949L;

    private static final Direction DEFAULT_DIRECTION = Direction.ASC;

    private List<Order> orders;

    protected Sort() {
        this(new ArrayList<>(0));
    }

    public Sort(Order... orders) {
        this(Arrays.asList(orders));
    }

    @JsonCreator
    public Sort(@JsonProperty("orders") List<Order> orders) {
        XAssert.notEmpty(orders, "The sort property can`t be null or empty");
        this.orders = orders;
    }

    public Order getOrderByProperpt(String property) {
        for (Order order : orders) {
            if (order.getProperty().equals(property)) {
                return order;
            }
        }
        return null;
    }

    public List<Order> getOrders() {
        return orders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Sort)) {
            return false;
        }
        Sort sort = (Sort) o;
        return getOrders().equals(sort.getOrders());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrders());
    }

    @Override
    public String toString() {
        return "Sort{" +
                "orders=" + orders +
                '}';
    }

    public static class Order implements Serializable {

        private static final long serialVersionUID = 6870444458311930418L;

        private Direction direction = DEFAULT_DIRECTION;

        private String property;

        public Order(String property) {
            this(null, property);
        }

        @JsonCreator
        public Order(@JsonProperty("direction") Direction direction, @JsonProperty("property") String property) {
            if (direction != null) {
                this.direction = direction;
            }
            this.property = property;
        }

        public static List<Order> create(Direction direction, Collection<String> properties) {
            List<Order> orderList = Lists.newArrayListWithCapacity(properties.size());
            for (String property : properties) {
                orderList.add(new Order(direction, property));
            }
            return orderList;
        }

        public Direction getDirection() {
            return direction;
        }

        public void setDirection(Direction direction) {
            this.direction = direction;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Order)) {
                return false;
            }
            Order order = (Order) o;
            return getDirection() == order.getDirection() &&
                    getProperty().equals(order.getProperty());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDirection(), getProperty());
        }

        @Override
        public String toString() {
            return "Order{" +
                    "direction=" + direction +
                    ", property='" + property + '\'' +
                    '}';
        }
    }

    public enum Direction {

        // 降序
        DESC,
        // 升序
        ASC;

        public static Direction fromString(String sort) {
            try {
                return Direction.valueOf(sort.toUpperCase(Locale.US));
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Invalid value '%s' for sort given! Has to be 'desc' or 'asc'.", sort), e);
            }
        }

        public static Direction fromStringOrNull(String sort) {
            try {
                return fromString(sort);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
