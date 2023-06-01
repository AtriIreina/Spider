import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.util.EntityUtils;

public class RequestViaProxyExample {

    public static void main(String args[]) throws Exception{

        //Creating an HttpHost object for proxy
        HttpHost proxyhost = new HttpHost("91.216.121.222", 8080);

        //Creating an HttpHost object for target
        HttpHost targethost = new HttpHost("https://dev.kdlapi.com/testproxy");

        //creating a RoutePlanner object
        HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxyhost);

        //Setting the route planner to the HttpClientBuilder object
        HttpClientBuilder clientBuilder = HttpClients.custom();
        clientBuilder = clientBuilder.setRoutePlanner(routePlanner);

        //Building a CloseableHttpClient
        CloseableHttpClient httpclient = clientBuilder.build();

        //Creating an HttpGet object
        HttpGet httpget = new HttpGet("/");

        //Executing the Get request
        HttpResponse httpresponse = httpclient.execute(targethost, httpget);

        //Printing the status line
        System.out.println(httpresponse.getStatusLine());

        //Printing all the headers of the response
        Header[] headers = httpresponse.getAllHeaders();

        for (int i = 0; i < headers.length; i++) {
            System.out.println(headers[i]);
        }

        //Printing the body of the response
        HttpEntity entity = httpresponse.getEntity();

        if (entity != null) {
            System.out.println(EntityUtils.toString(entity));
        }
    }
}