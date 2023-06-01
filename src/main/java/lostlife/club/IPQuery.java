package lostlife.club;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

public class IPQuery {

    static String queryUrl1 = "https://ipv4.icanhazip.com/";
    static String queryUrl2 = "https://www.ip138.com/";

    public static void main(String[] args) throws Exception {
        String hostAddress = IpUtil.getHostAddress();
        System.out.println("内外IP: " + hostAddress);


        //翻墙IP
        String publicIp1 = "";
        publicIp1 = IpUtil.getPublicIp1(queryUrl1);
        //真实IP
        String publicIp2 = "";
//        publicIp2 = IpUtil.getPublicIp2(queryUrl2);
        if (!publicIp1.equals(publicIp2)) {
            System.out.println("翻墙了");
            System.out.println("外网IP_1(翻墙IP) " + publicIp1 + ", 查询地址: " + queryUrl1);
            System.out.println("外网IP_2(真实IP): " + publicIp2 + ", 查询地址: " + queryUrl2);
        } else {
            System.out.println("没翻墙");
            System.out.println("外网IP_1: " + publicIp1 + ", 查询地址: " + queryUrl1);
            System.out.println("外网IP_2: " + publicIp2 + ", 查询地址: " + queryUrl2);
        }
    }
}


class IpUtil {

    /**
     * 获取当前服务器的IP地址(内网)
     */
    public static String getHostAddress() throws Exception {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("获取当前服务器的IP地址(内网)失败");
        }
    }

    private static final Pattern IPV4_PATTERN = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    /**
     * 通过 https://ipv4.icanhazip.com/ 查询外网IP (可查到翻墙后的 IP)
     */
//    public static String getPublicIp1(String queryUrl1) throws Exception {
    public static String getPublicIp1(String queryUrl1) {
        String ip = get(queryUrl1);
//        if (ip != null && !ip.isEmpty()) {
        return ip;
//        } else {
//            throw new Exception("获取本机外网ip失败");
//        }
    }

    private static String get(String url) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String ip = in.readLine();
            if (IPV4_PATTERN.matcher(ip).matches()) {
                return ip;
            } else {
                throw new IOException("invalid IPv4 address: " + ip);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 通过 https://www.ipshudi.com/ 查询外网IP (只可查到国内真实 IP)
     */
    public static String getPublicIp2(String queryUrl2) throws URISyntaxException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpUriRequest request = RequestBuilder.get()
                .setUri(new URI(queryUrl2))
                .setHeader("Upgrade-Insecure-Requests", "1")
                .setHeader("Accept", "text/html")
                .setHeader("X-Requested-With", "XMLHttpRequest")
                .setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                .build();
        CloseableHttpResponse response = httpClient.execute(request);
        String html = EntityUtils.toString(response.getEntity(), "utf-8");
        System.out.println(queryUrl2 + "获取到的页面为: " + html);
        Document doc = Jsoup.parse(html);

        String ip = doc.select("body > p:nth-child(1) > a:nth-child(1)").text();
        String info = doc.select("body > p:nth-child(1)").text();
        System.out.println(queryUrl2 + "获取到的信息为: " + info);
        return ip;
    }
}