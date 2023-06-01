import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URL;

/**
 * 使用httpclient请求隧道服务器 请求http和https网页均适用
 * https://www.kuaidaili.com/doc/dev/sdk_tps_http/#httpclient
 */
@SpringBootTest
public class KuaiDLTunnelHttpClient {

    //    private  String pageUrl = "https://south.yuanacg.com/"; // 要访问的目标网页 //HTTP/1.1 449 Foreign Host Forbidden 	禁止访问境外网址
    private String pageUrl = "https://dev.kdlapi.com/testproxy"; // 要访问的目标网页
    private String proxyIp = "b650.kdltps.com"; // 隧道服务器域名
    private int proxyPort = 15818; // 端口号
    // 用户名密码, 若已添加白名单则不需要添加
    private String username = "t18527523081462";
    private String password = "ayf91112";

    @Test
    public void test() throws Exception {
        // JDK 8u111版本后，目标页面为HTTPS协议，启用proxy用户密码鉴权
        //System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(proxyIp, proxyPort),
                new UsernamePasswordCredentials(username, password));
        CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
//        CloseableHttpClient httpclient = HttpClients.createDefault();
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
            RequestConfig config = RequestConfig.custom().setProxy(proxy).setConnectTimeout(6000)
                    .setConnectionRequestTimeout(2000).setSocketTimeout(6000).build();
            HttpGet httpget = new HttpGet(url.getPath());
            httpget.setConfig(config);
            httpget.addHeader("Accept-Encoding", "gzip"); // 使用gzip压缩传输数据让访问更快
            httpget.addHeader("Connection", "close");
            httpget.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36");
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
}