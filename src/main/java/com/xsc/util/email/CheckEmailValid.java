package com.xsc.util.email;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.xsc.util.assertions.XAssert;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xia
 * @date 2020/1/9 14:44
 */
public class CheckEmailValid {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckEmailValid.class);

    /**
     * 邮件发送方的邮件地址
     */
    private static final String SENDER_EMAIL_ADDRESS = "XXXXXX@xia.com";

    /**
     * 获取邮件的域名
     *
     * @return 邮件地址的域名
     */
    private static String getSenderEmailAddressDomain(String emailAddress) {
        XAssert.isTrue(emailAddress.indexOf('@') != -1, "邮件格式不正确!");
        return StringUtils.substringAfter(emailAddress, "@");
    }

    private static int hear(BufferedReader in) throws IOException {
        String line;
        int res = 0;
        while ((line = in.readLine()) != null) {
            String pfx = line.substring(0, 3);
            try {
                res = Integer.parseInt(pfx);
            } catch (Exception ex) {
                res = -1;
            }
            if (line.charAt(3) != '-') {
                break;
            }
        }
        return res;
    }

    private static void say(BufferedWriter wr, String text)
            throws IOException {
        wr.write(text + "\r\n");
        wr.flush();
    }

    private static List<String> getMX(String hostName) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        DirContext dirContext = new InitialDirContext(env);
        Attributes attrs = dirContext.getAttributes(hostName, new String[]{"MX"});
        Attribute attr = attrs.get("MX");
        if ((attr == null) || (attr.size() == 0)) {
            attrs = dirContext.getAttributes(hostName, new String[]{"A"});
            attr = attrs.get("A");
            if (attr == null) {
                throw new NamingException("No match for name '" + hostName + "'");
            }
        }
        List<String> res = new ArrayList<>();
        NamingEnumeration en = attr.getAll();
        while (en.hasMore()) {
            String x = (String) en.next();
            String[] f = x.split(" ");
            if (f[1].endsWith(".")) {
                f[1] = f[1].substring(0, (f[1].length() - 1));
            }
            res.add(f[1]);
        }
        return res;
    }

    private static List<Pair<String, Boolean>> isAddressValid(Collection<String> addressList, List<String> mxList) {
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(addressList),
                "Address list can`t be empty!");
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(mxList),
                "MX list can`t be empty!");
        List<Pair<String, Boolean>> pairs = addressList.stream()
                .map(x -> MutablePair.of(x, Boolean.FALSE)).collect(Collectors.toList());
        Map<String, Boolean> hasCheckedAddressMap = addressList.stream()
                .collect(Collectors.toMap(Function.identity(), s -> Boolean.FALSE));
        for (String o : mxList) {
            try (Socket skt = new Socket(o, 25);
                 BufferedReader rdr = new BufferedReader(new InputStreamReader(skt.getInputStream()));
                 BufferedWriter wtr = new BufferedWriter(new OutputStreamWriter(skt.getOutputStream()))) {
                int res;
                res = hear(rdr);
                if (res != 220) {
                    throw new Exception("Invalid header");
                }
                say(wtr, "EHLO " + getSenderEmailAddressDomain(SENDER_EMAIL_ADDRESS));
                res = hear(rdr);
                if (res != 250) {
                    throw new Exception("Not ESMTP");
                }
                // validate the sender address
                say(wtr, "MAIL FROM:<" + SENDER_EMAIL_ADDRESS + ">");
                res = hear(rdr);
                if (res != 250) {
                    throw new Exception("Sender rejected");
                }
                for (String address : addressList) {
                    if (!hasCheckedAddressMap.get(address)) {
                        say(wtr, "RCPT TO: <" + address + ">");
                        res = hear(rdr);
                        if (res == 250) {
                            pairs.stream().filter(x -> StringUtils.equals(x.getKey(), address))
                                    .forEach(x -> x.setValue(Boolean.TRUE));
                            hasCheckedAddressMap.put(address, Boolean.TRUE);
                        }
                    }
                }
                say(wtr, "RSET");
                hear(rdr);
                say(wtr, "QUIT");
                hear(rdr);
                if (hasCheckedAddressMap.values().stream().allMatch(x -> x.equals(Boolean.TRUE))) {
                    break;
                }
            } catch (Exception ex) {
                // Do nothing but try next host
            }
        }
        return pairs;
    }

    public static List<Pair<String, Boolean>> isAddressValid(List<String> strings) throws NamingException {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Multimap<String, String> domainMap = HashMultimap.create();
        for (String s : strings) {
            String domain = getSenderEmailAddressDomain(s);
            domainMap.put(domain, s);
        }
        List<Pair<String, Boolean>> pairs = Lists.newArrayList();
        for (String k : domainMap.keySet()) {
            List<String> mxList = getMX(k);
            List<Pair<String, Boolean>> pairList = isAddressValid(domainMap.get(k), mxList);
            pairs.addAll(pairList);
        }
        LOGGER.info("verify email address is legal consuming {} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return pairs;
    }

}
