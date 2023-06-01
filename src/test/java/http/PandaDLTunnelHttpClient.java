package http;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URL;
import java.util.Date;

/**
 * 使用httpclient请求隧道服务器 请求http和https网页均适用
 * http://www.xiongmaodaili.com/exampleCode
 */
@SpringBootTest
public class PandaDLTunnelHttpClient {

    //动态并发产品代理设置为dtbf.xiongmaodaili.com:8089, 动态按量产品需将代理设置为dtan.xiongmaodaili.com:8088
    final String pageUrl = "https://dev.kdlapi.com/testproxy";
    final String proxyIp = "dtan.xiongmaodaili.com";//这里以正式服务器ip地址为准
    final int proxyPort = 8088;//这里以正式服务器端口地址为准
    final int timestamp = (int) (new Date().getTime() / 1000);
    //以下订单号，secret参数 须自行改动；最后一个参数: true-换ip ,false-不换ip
    //final String orderNo = "SDL20230530235427Mz0DaKHH";
    final String orderNo = "SDL20230531003920iSv8AWfx";
    final String secret = "cbf479c58faf10006ba91ecc57cc81fa";
    final String authHeader = authHeader(orderNo, secret, timestamp, "false");

    @Test
    public void test() throws Exception {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            URL url = new URL(pageUrl);
            HttpHost target = new HttpHost(url.getHost());
            HttpHost proxy = new HttpHost(proxyIp, proxyPort);

            /*
            httpclient各个版本设置超时都略有不同, 此处对应版本4.5.6
            setConnectTimeout：设置连接超时时间
            setConnectionRequestTimeout：设置从connect Manager获取Connection 超时时间
            setSocketTimeout：请求获取数据的超时时间
            */
            RequestConfig config = RequestConfig.custom().setProxy(proxy).setConnectTimeout(6000).setConnectionRequestTimeout(2000).setSocketTimeout(6000).build();
            HttpGet httpget = new HttpGet(url.getPath());
            httpget.setConfig(config);
            httpget.addHeader("Accept-Encoding", "gzip"); // 使用gzip压缩传输数据让访问更快
            httpget.addHeader("Connection", "close");
            httpget.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36");
            httpget.addHeader("Proxy-Authorization", authHeader);
            CloseableHttpResponse response = httpclient.execute(target, httpget);
            try {
                System.out.println(response.getStatusLine());
                System.out.println(EntityUtils.toString(response.getEntity()));
            } finally {
                response.close();
            }
        } finally {
            httpclient.close();
        }
    }

    //change 参数: false-换ip ，true-不换ip
    private String authHeader(String orderno, String secret, int timestamp, String change) {
        //拼装签名字符串
        String planText = String.format("orderno=%s,secret=%s,timestamp=%d", orderno, secret, timestamp);

        //计算签名
        String sign = org.apache.commons.codec.digest.DigestUtils.md5Hex(planText).toUpperCase();

        //拼装请求头Proxy-Authorization的值;change 参数: false-换ip ,true-不换ip
        return String.format("sign=%s&orderno=%s&timestamp=%d&change=%s", sign, orderno, timestamp, change);
    }
}