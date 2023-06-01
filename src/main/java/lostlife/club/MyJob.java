package lostlife.club;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyJob {

    /*
     * ================================================================================================
     * ===================================== 定义全局变量 ===============================================
     * ================================================================================================
     * */

    static ArrayList<Map<String, String>> downloadInfoList = new ArrayList<>();
    static List<String> cantReplyLinks = new ArrayList<>();     //不能回复(闲谈/不是资源)
    static List<String> stopReplyLinks = new ArrayList<>();     //禁止回复(过期资源/不是资源)
    static List<String> httpFailLinks = new ArrayList<>();  //网络异常(http访问失败)
    static List<String> loginFailUsers = new ArrayList<>(); //登录失败

    static Date startDate = new Date();
    static Date endDate;


    static CountDownLatch countDownLatchTotal;
    static List<CountDownLatch> countDownLatchBatchList = new ArrayList<>();

    static List<String> accountsFilePath = new ArrayList<>();

    static {
        accountsFilePath.add("D:\\___earn\\_lostlife.snyh.top\\ACG次元网账号1.txt");
        accountsFilePath.add("D:\\___earn\\_lostlife.snyh.top\\ACG次元网账号2.txt");
    }

    static List<String> linksFilePath = new ArrayList<>();

    static {
        linksFilePath.add("D:\\___earn\\_lostlife.snyh.top\\_south.yuanacg.com_游戏_HTML_列表_详情2.xlsx");
        linksFilePath.add("D:\\___earn\\_lostlife.snyh.top\\_south.yuanacg.com_游戏_合集_列表_详情2.xlsx");
        linksFilePath.add("D:\\___earn\\_lostlife.snyh.top\\_south.yuanacg.com_游戏_国产_列表_详情2.xlsx");
        linksFilePath.add("D:\\___earn\\_lostlife.snyh.top\\_south.yuanacg.com_游戏_日韩_列表_详情2.xlsx");
        linksFilePath.add("D:\\___earn\\_lostlife.snyh.top\\_south.yuanacg.com_游戏_欧美_列表_详情2.xlsx");
    }

    static List<String> linksOverFilePath = new ArrayList<>();

    static {
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_HTML_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_合集_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_国产_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_日韩_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_欧美_列表_详情2.txt");
    }


    /**
     * 当前IP地址
     */
    static String INIT_IP_ADDRESS = IpUtil.getPublicIp1("https://ipv4.icanhazip.com/");
    /**
     * 防止IP封禁, 限制每个IP可回复的链接数量
     */
    final static int IP_REPLY_LIMIT = 200;
    /**
     * 暂停时间 (秒)
     */
    final static int PAUSE_TIME = 1000 * 10;
    /**
     * 用户登录后, 访问页面前, 回复帖子前, 获取下载信息前
     */
    final static int[] THREAD_SLEEP_SECOND = new int[]{3, 10, 1, 1};
    /**
     * 线程不能开太多, 访问过快会导致封禁IP
     */
    final static int THREAD_COUNT = Runtime.getRuntime().availableProcessors() * 13;
    /**
     * 0 HTML(44), 1 合集(287)) 2 国产(710), 3 日韩(3314), 4 欧美(3402)
     */
    final static int NOW_LINKS_INDEX = 1;
    final static String NOW_LINKS_FILE_PATH = linksFilePath.get(NOW_LINKS_INDEX);
    final static String NOW_LINKS_OVER_FILE_PATH = linksOverFilePath.get(NOW_LINKS_INDEX);
    /**
     * 1 从头开始, 2 从尾开始, 3 左逆右顺, 4 右逆左顺
     */
    final static int ACCOUNT_USE_TYPE = 1;

    static RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(45000).setConnectTimeout(45000).setConnectionRequestTimeout(45000).build();

    /*
     * ================================================================================================
     * =================================== 整体代码逻辑 =================================================
     * ================================================================================================
     * */

    public static void main(String[] args) throws Exception {
        //获取账号列表
        List<String> accountsTotal = getAccountsFromFile(accountsFilePath);
        System.out.println("accountListSize:" + accountsTotal.size());
        System.out.println("accountList:" + accountsTotal);

        //查询页面链接列表
        List<String> linksTotal = getLinksFromFile(NOW_LINKS_FILE_PATH);
        System.out.println("linksTotalSize:" + linksTotal.size());
        System.out.println("linksTotal:" + linksTotal);

        //获取已完成页面链接
        List<String> linksOverTotal = getLinksOverFromFile(NOW_LINKS_OVER_FILE_PATH);
        System.out.println("linksOverTotalSize:" + linksOverTotal.size());
        System.out.println("linksOverTotal:" + linksOverTotal);

        //去除已完成页面链接
        linksTotal.removeAll(linksOverTotal);
        countDownLatchTotal = new CountDownLatch(linksTotal.size());
        System.out.println("linksTotalSize(去除已完成):" + linksTotal.size());
        System.out.println("linksTotal(去除已完成):" + linksTotal);

        System.out.println("==============================================");

        //根据链接数量暂停线程, 等待手动切换IP
        waitFlushIP(accountsTotal, linksTotal);

        countDownLatchTotal.await();
        success(linksTotal);
    }

    private static void waitFlushIP(List<String> accountsTotal, List<String> linksTotal) throws InterruptedException {
        //回复所有链接需要的IP数量
        int iPCount = linksTotal.size() / IP_REPLY_LIMIT;
        for (int ipIndex = 0; ipIndex <= iPCount; ipIndex++) {
            List<String> linksBatch = new ArrayList<>();
            int start = ipIndex * IP_REPLY_LIMIT;
            int end = (ipIndex + 1) * IP_REPLY_LIMIT;
            if (ipIndex <= iPCount - 1) {
                linksBatch.addAll(linksTotal.subList(start, end));
            } else {
                end = linksTotal.size();
                linksBatch.addAll(linksTotal.subList(start, end));
            }

            //启动线程池进行工作
            System.out.println("第[" + ipIndex + "/" + iPCount + "]批任务开始执行, 开始时间: " + new Date().toLocaleString());

            countDownLatchBatchList.add(ipIndex, new CountDownLatch(linksBatch.size()));
            startThreadDoJob(accountsTotal, linksBatch, countDownLatchBatchList.get(ipIndex));
            countDownLatchBatchList.get(ipIndex).await();
            System.out.println("第[" + ipIndex + "/" + iPCount + "]批任务执行完毕, 结束时间: " + new Date().toLocaleString());

            //等待当前批次的线程都完成任务, 暂停线程等待手动切换IP
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            if (iPCount == 0) {
                System.out.println("链接数量" + linksTotal.size() + "<=400条, 无需切换IP即可完成任务");
            } else {
                System.out.println("第[" + ipIndex + "/" + iPCount + "]批, 链接范围[" + start + "/" + end + "], 链接:" + linksBatch);
                String nowIP = IpUtil.getPublicIp1("https://ipv4.icanhazip.com/");
//                while (nowIP.equals(INIT_IP_ADDRESS)) {
                System.out.println("主线程开始暂停[" + PAUSE_TIME / 1000 + "]秒, " + new Date().toLocaleString() + ", 等待切换IP[当前: " + nowIP + "]");
//                    //Thread.sleep(PAUSE_TIME);
//                    nowIP = IpUtil.getPublicIp1("https://ipv4.icanhazip.com/");
//                }
//                INIT_IP_ADDRESS = nowIP;
                System.out.println("主线程结束暂停[" + PAUSE_TIME / 1000 + "]秒, " + new Date().toLocaleString() + ", 已经切换IP[当前: " + INIT_IP_ADDRESS + "]");
            }
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        }
    }

    private static void startThreadDoJob(List<String> accountsTotal, List<String> linksTotal, CountDownLatch countDownLatch) throws InterruptedException {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        //线程池执行并行任务
        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadIndex = i;
            int finalI = i;
            threadPool.execute(() -> {
                try {
                    //错峰, 让线程等待时间错开, 减少CPU调度时的拥挤
                    Thread.sleep(1000);
                    doJob(threadIndex, accountsTotal, linksTotal, countDownLatch);
                } catch (Exception e) {
                    e.printStackTrace();
                    exception(finalI, linksTotal);
                }
            });
        }

        System.out.println("线程已分配,等待任务完成");
    }

    private static void printData(List<String> linksTotal){
        endDate = new Date();
        int seconds = (int) ((endDate.getTime() - startDate.getTime()) / 1000);
        System.out.println("开始时间:" + startDate.toLocaleString() + ",结束时间:" + endDate.toLocaleString() + ",耗时" + seconds + "秒");
        System.out.println("一共" + linksTotal.size() + "条记录, 有效记录[" + (linksTotal.size() - cantReplyLinks.size()) + "]条, 无效记录[" + cantReplyLinks.size() + "]条, 在[" + THREAD_COUNT + "]线程下, 每条记录平均耗时: " + ((float) seconds / linksTotal.size()) + "秒");

        //添加已有链接
        try {
            StringBuilder stringBuilder = new StringBuilder();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(NOW_LINKS_OVER_FILE_PATH));
            while (bufferedReader.ready()) {
                stringBuilder.append(bufferedReader.readLine());
            }
            List<Map> oldOverList = JSONObject.parseArray(stringBuilder.toString(), Map.class);
            oldOverList.forEach(item -> downloadInfoList.add(item));
        } catch (Exception e){
            e.printStackTrace();
            System.out.println("==================================添加已有链接失败==================================");
            System.out.println("==================================添加已有链接失败==================================");
            System.out.println("==================================添加已有链接失败==================================");
        }

        String downloadInfoListStr = JSONObject.toJSONString(downloadInfoList);
        System.out.println(downloadInfoList.size() + " : " + downloadInfoListStr);
        write2File(downloadInfoListStr, NOW_LINKS_FILE_PATH.replace("_south.yuanacg.com", "__链接__south.yuanacg.com").replace(".xlsx", ".txt"));

        System.out.println("==============================================");
        System.out.println("以下是不能回复(闲谈/不是资源)=======================");
        System.out.println("==============================================");
        String cantReplyLinksStr = JSONObject.toJSONString(cantReplyLinks);
        System.out.println(cantReplyLinks.size() + " : " + cantReplyLinksStr);
        write2File(cantReplyLinksStr, NOW_LINKS_FILE_PATH.replace("_south.yuanacg.com", "__不能__south.yuanacg.com").replace(".xlsx", ".txt"));


        System.out.println("==============================================");
        System.out.println("以下是禁止回复(过期资源/不是资源)=======================");
        System.out.println("==============================================");
        String stopReplyLinksStr = JSONObject.toJSONString(stopReplyLinks);
        System.out.println(stopReplyLinks.size() + " : " + stopReplyLinksStr);
        write2File(stopReplyLinksStr, NOW_LINKS_FILE_PATH.replace("_south.yuanacg.com", "__禁止__south.yuanacg.com").replace(".xlsx", ".txt"));

        System.out.println("==============================================");
        System.out.println("以下是网络异常(http访问失败)=======================");
        System.out.println("==============================================");
        String httpFailLinksStr = JSONObject.toJSONString(httpFailLinks);
        System.out.println(httpFailLinks.size() + " : " + httpFailLinksStr);
        write2File(httpFailLinksStr, NOW_LINKS_FILE_PATH.replace("_south.yuanacg.com", "__失败__south.yuanacg.com").replace(".xlsx", ".txt"));

        System.exit(0);
    }

    private static void fail(List<String> linksTotal) {
        System.out.println("==============================================");
        System.out.println("用户账号已全部使用, 可能不足以访问全部链接=======================");
        System.out.println("==============================================");
        printData(linksTotal);
    }

    private static void success(List<String> linksTotal) {
        System.out.println("==============================================");
        System.out.println("线程任务已完成=======================");
        System.out.println("==============================================");
        printData(linksTotal);
    }

    private static void exception(int threadIndex, List<String> linksTotal) {
        System.out.println("==============================================");
        System.out.println("线程[" + threadIndex + "]任务执行异常=======================");
        System.out.println("==============================================");
        printData(linksTotal);
    }

    private static void write2File(String downloadInfoListStr, String filePath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            writer.write(downloadInfoListStr);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * ================================================================================================
     * =================================== 从文件读取用户和页面链接 ======================================
     * ================================================================================================
     * */


    private static List<String> getLinksOverFromFile(String filePath) throws Exception {
        List<String> links = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
        while (bufferedReader.ready()) {
            stringBuilder.append(bufferedReader.readLine());
        }
        List<Map> downloadTypeList = JSONObject.parseArray(stringBuilder.toString(), Map.class);
        downloadTypeList.forEach(item -> {
            links.add("https://south.yuanacg.com/" + item.get("link"));
        });

        return links;
    }

    static class DownloadType {
        String downloadInfo;
        String link;
        String title;
        String user;

        @Override
        public String toString() {
            return "DownloadType{" +
                    "downloadInfo='" + downloadInfo + '\'' +
                    ", link='" + link + '\'' +
                    ", title='" + title + '\'' +
                    ", user='" + user + '\'' +
                    '}';
        }
    }

    private static List<String> getLinksFromFile(String filePath) throws Exception {
        List<String> links = new ArrayList<>();

        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(filePath));
        XSSFSheet sheet = workbook.getSheetAt(0);
        int firstRowIndex = sheet.getFirstRowNum() + 1;
        int lastRowIndex = sheet.getLastRowNum();
        for (int i = firstRowIndex; i <= lastRowIndex; i++) {
            XSSFRow row = sheet.getRow(i);
            XSSFCell cell = row.getCell(2);
            if (cell != null && cell.toString().isEmpty()) {
                System.out.println(filePath + "[" + i + "]行数据为空, 上一行的标题为: " + sheet.getRow(i - 1).getCell(1));
            } else {
                links.add(cell.toString());
            }
        }
        return links;
    }

    private static List<String> getAccountsFromFile(List<String> accountsFilePath) {
        List<String> accounts = new ArrayList<>();
        accountsFilePath.forEach(filePath -> {
            try {
                BufferedReader in = new BufferedReader(new FileReader(filePath));
                while (in.ready()) {
                    String line = in.readLine();
                    if (line != null && !line.isEmpty()) {
                        accounts.add(line);
                    }
                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        //1 从头开始, 2 从尾开始, 3 左顺右逆(向中间), 4 右顺左逆(向两边)
        List<String> accounts1 = accounts.subList(0, accounts.size() / 2);
        List<String> accounts2 = accounts.subList(accounts.size() / 2, accounts.size());
        List<String> accounts3 = new ArrayList<>();
        switch (ACCOUNT_USE_TYPE) {
            case 1:
                accounts3.addAll(accounts1);
                accounts3.addAll(accounts2);
                break;
            case 2:
                Collections.reverse(accounts1);
                Collections.reverse(accounts2);
                accounts3.addAll(accounts2);
                accounts3.addAll(accounts1);
                break;
            case 3:
                Collections.reverse(accounts2);
                accounts3.addAll(accounts1);
                accounts3.addAll(accounts2);
                break;
            case 4:
                Collections.reverse(accounts1);
                accounts3.addAll(accounts2);
                accounts3.addAll(accounts1);
                break;
            default:
                break;
        }

        return accounts3;
    }

    /*
     * ================================================================================================
     * =================================== 具体执行逻辑 =================================================
     * ================================================================================================
     * */


    private static void doJob(int threadIndex, List<String> accountsTotal, List<String> linksTotal, CountDownLatch countDownLatch) throws Exception {
        //按照线程数量 分配用户和链接
        List<String> accounts = new ArrayList<>();
        List<String> links = new ArrayList<>();
        for (int i = 0; i < accountsTotal.size(); i++) {
            if (i % THREAD_COUNT == threadIndex) {
                accounts.add(accountsTotal.get(i));
            }
        }
        for (int i = 0; i < linksTotal.size(); i++) {
            if (i % THREAD_COUNT == threadIndex) {
                links.add(linksTotal.get(i));
            }
        }

        System.out.println("线程" + threadIndex + "开始工作==============================================");
        System.out.println("线程" + threadIndex + ", accountsSize:" + accounts.size() + ", accounts:" + accounts);
        System.out.println("线程" + threadIndex + ", linksSize:" + links.size() + ", links:" + links);

        String loginUrl = "https://south.yuanacg.com/member.php";

        int accountReplyLimit = 20; //一个用户能回帖的次数限制 (每个用户 在1小时内 限制回帖 20条)
        int accountNeedCount = (int) Math.ceil(links.size() / (float) accountReplyLimit); //回帖总共需要的账号数量
        for (int userIndex = 0; userIndex < accountNeedCount; userIndex++) {
            //防止访问过快, 暂停线程 (手段换IP)
//            int userPauseCount = 400 / THREAD_COUNT / 20;
//            if (userIndex % userPauseCount == 0) {
//                System.out.println("+++++++++++++++++++++++++++++++++++++++++");
//                System.out.println("线程[" + threadIndex + "]暂停" + PAUSE_TIME + "秒, 请手段切换IP");
//                Thread.sleep(1000 * PAUSE_TIME);
//                System.out.println("线程暂停" + PAUSE_TIME + "秒结束");
//                System.out.println("+++++++++++++++++++++++++++++++++++++++++");
//            }

            //用户登录 (睡眠1s)
            String username = accounts.get(userIndex);
            String password = accounts.get(userIndex);

            int finalUserIndex = userIndex;
            CloseableHttpClient httpClient = HttpClients.createDefault();
            doLogin(httpClient, loginUrl, username, password, countDownLatch, () -> {
                System.out.println("work-用户循环:线程[" + threadIndex + "], 用户[" + finalUserIndex + "/" + accountNeedCount + "]" + username);

                //用户登录回调事件

                //每个用户回帖间隔不能低于10s
                for (int accountReplyCounter = 0; accountReplyCounter < accountReplyLimit; accountReplyCounter++) {

                    //获取页面访问链接
                    int linkIndex = accountReplyLimit * finalUserIndex + accountReplyCounter;
                    if (accountReplyCounter >= links.size()) {//链接数量 < 用户数量*限制回复数量, 提前退出 (比如每个用户只需要处理11个请求, 小于20个)
                        return;
                    }
                    if (linkIndex >= links.size()) {//链接分4组后每组数量有差异, 比如 70 69 69 69
                        return;
                    }
                    String pageLink = links.get(linkIndex);

                    Map<String, Boolean> stepMap = new HashMap<>(); //needReply 需要进行回帖, needGetDownInfo 需要获取下载信息
                    stepMap.put("needReply", true);
                    stepMap.put("needGetDownInfo", true);
                    Map<String, String> replyInfoMap = new HashMap<>(); //pageUrl 帖子链接, replyUrl 回帖链接, formHash 表单验证值
                    replyInfoMap.put("pageUrl", null);
                    replyInfoMap.put("replyUrl", null);
                    replyInfoMap.put("formHash", null);

                    //获取回帖链接
                    doGetReplyLink(stepMap, replyInfoMap, httpClient, username, pageLink, countDownLatch, () -> {
                        //进行回帖
                        doReplyPost(stepMap, httpClient, replyInfoMap, countDownLatch, () -> {
                            //获取下载信息
                            doGetDownloadInfo(stepMap, username, httpClient, pageLink, countDownLatch);
                        });
                    });

                    //最后关闭登录的客户端 (释放资源)
                    if (accountReplyCounter >= accountReplyLimit - 1) {
                        httpClient.close();
                    }

                    System.out.println("work-页面循环:线程[" + threadIndex + "], 用户[" + finalUserIndex + "/" + accountNeedCount + "], 访问[" + accountReplyCounter + "/" + accountReplyLimit + "], 链接[" + linkIndex + "], 用户名[" + username + "], 链接:" + pageLink);
                }
            });
        }
    }

    public static void doLogin(CloseableHttpClient httpClient, String loginUrl, String username, String password, CountDownLatch countDownLatch, JobHandler jobHandler) throws URISyntaxException {
        String loginUrl2 = "https://south.yuanacg.com/member.php";
        if (loginUrl.isEmpty() || Objects.isNull(loginUrl)) {
            loginUrl = loginUrl2;
        }
        HttpUriRequest postLoginRequest = RequestBuilder.post().setConfig(requestConfig)
                // 登陆url
                .setUri(new URI(loginUrl)).addHeader("Accept-Encoding", "gzip").setHeader("Upgrade-Insecure-Requests", "1").setHeader("Accept", "application/json").setHeader("Content-Type", "application/x-www-form-urlencoded").setHeader("X-Requested-With", "XMLHttpRequest").setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                // 设置登录参数
                .addParameter("mod", "logging").addParameter("action", "login").addParameter("loginsubmit", "yes").addParameter("loginhash", "LVTNY")
                // 设置账号信息
                .addParameter("username", username).addParameter("password", password).build();
        // 模拟登陆
        try {
            httpClient.execute(postLoginRequest);
            System.out.println(username + "登录成功");

            postLoginRequest.abort();
            Thread.sleep(1000 * THREAD_SLEEP_SECOND[0]);

            jobHandler.doJob();
        } catch (Exception e) {
            System.out.println("用户[" + username + "]登陆失败");
            e.printStackTrace();
            loginFailUsers.add(username);
            for (int i = 0; i < 20; i++) {
                countDownLatchTotal.countDown();
                countDownLatch.countDown();
            }
        }
    }

    private static void doGetReplyLink(Map<String, Boolean> stepMap, Map<String, String> replyInfoMap, CloseableHttpClient httpClient, String username, String pageUrl, CountDownLatch countDownLatch, JobHandler jobHandler) throws InterruptedException, URISyntaxException {
        Thread.sleep(1000 * THREAD_SLEEP_SECOND[1]);

        //=================================访问帖子===============================================
        HttpUriRequest getPageRequest = RequestBuilder.get().setConfig(requestConfig).setUri(new URI(pageUrl)).addHeader("Accept-Encoding", "gzip").setHeader("Upgrade-Insecure-Requests", "1").setHeader("Accept", "application/json").setHeader("Content-Type", "application/x-www-form-urlencoded").setHeader("X-Requested-With", "XMLHttpRequest").setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36").build();

        try {
            CloseableHttpResponse response = httpClient.execute(getPageRequest);
            String html = EntityUtils.toString(response.getEntity(), "utf-8");

            //使用jsoup获取formHash和replyUrl (formHash 每次登录都不同)
            Document doc = Jsoup.parse(html);
            String action = doc.select("#fastpostform").attr("action");
            String replyUrl = "https://south.yuanacg.com/" + action;
            String formHash = doc.select("#fastpostform > table > tbody > tr > td.plc > input[name=formhash]").attr("value");
            //String formHash = doc.select("#fastpostform > table > tbody > tr > td.plc > input[type=hidden]:nth-child(6)").attr("value");

            //当前httpClient未登录
            if (doc.select("form").attr("name").equals("login")) {
                System.out.println("当前httpClient未登录" + username + ":" + pageUrl + ", formHash:" + formHash + ", action: " + action);
                System.exit(1);
            }
            //ip被封, 线程休眠等待换IP
            if (doc.select("#fastpostform").isEmpty()) {
                System.out.println("抱歉[获取帖子信息]匹配不到模块, 可能是ip被封禁了," + username + ":" + pageUrl + ", formHash:" + formHash + ", action: " + action);

                //无法回帖, 也无法获取下载信息
                stepMap.put("needReply", false);
                stepMap.put("needGetDownInfo", false);
                httpFailLinks.add(pageUrl);

                //提前结束判断
                doCountdown(countDownLatch);
                getPageRequest.abort();
                response.close();

                jobHandler.doJob();
                return;
            }

            //没有解锁链接: 已解锁链接/ 不是资源贴 / 无需解锁的资源贴
            if (doc.select("div.locked > a") == null || doc.select("div.locked > a").isEmpty()) {
                //已解锁链接
                if (doc.select(".showhide") != null && !doc.select(".showhide").isEmpty()) {
                    System.out.println("[惊喜]直接获取下载信息, " + username + ":" + pageUrl + ", formHash:" + formHash + ", action: " + action);

                    //无需回帖再获取下载信息
                    stepMap.put("needReply", false);
                    stepMap.put("needGetDownInfo", false);

                    //直接获取下载信息
                    doGetDownloadInfoFromDoc(html, username, pageUrl, countDownLatch);

                    //提前结束判断
                    getPageRequest.abort();
                    response.close();

                    jobHandler.doJob();
                    return;
                }
                //不是资源贴 / 无需解锁的资源贴
                else {
                    System.out.println("[可恶]不是资源贴 / 无需解锁的资源贴(太老的资源不要), " + username + ":" + pageUrl + ", formHash:" + formHash + ", action: " + action);

                    //不需要回帖, 也不需要获取下载信息
                    stepMap.put("needReply", false);
                    stepMap.put("needGetDownInfo", false);
                    cantReplyLinks.add(pageUrl);

                    //提前结束判断
                    doCountdown(countDownLatch);
                    getPageRequest.abort();
                    response.close();

                    jobHandler.doJob();
                    return;
                }
            }

            //存在解锁链接, 获取信息失败
            if (formHash == null || formHash.isEmpty() || action == null || action.isEmpty()) {
                System.out.println("[获取帖子信息失败]" + username + ":" + pageUrl + ", formHash:" + formHash + ", action: " + action);

                //无法回帖, 也无法获取下载信息
                stepMap.put("needReply", false);
                stepMap.put("needGetDownInfo", false);
                stopReplyLinks.add(pageUrl);

                //提前结束判断
                doCountdown(countDownLatch);
                getPageRequest.abort();
                response.close();

                jobHandler.doJob();
            } else {
                //存在解锁链接, 继续执行任务 ( 回帖 + 获取下载信息)
                System.out.println("[获取帖子信息成功]" + username + ":" + pageUrl + ", formHash:" + formHash + ", action: " + action);

                //需要回帖 来 获取下载信息
                stepMap.put("needReply", true);
                stepMap.put("needGetDownInfo", true);

                replyInfoMap.put("pageUrl", pageUrl);
                replyInfoMap.put("replyUrl", replyUrl);
                replyInfoMap.put("formHash", formHash);

                jobHandler.doJob();
            }
        } catch (Exception e) {
            System.out.println("用户[" + username + "]访问链接[" + pageUrl + "]失败");
            e.printStackTrace();
            httpFailLinks.add(pageUrl);
            doCountdown(countDownLatch);
        }
    }

    public static void doReplyPost(Map<String, Boolean> stepMap, CloseableHttpClient httpClient, Map<String, String> replyInfoMap, CountDownLatch countDownLatch, JobHandler jobHandler) throws URISyntaxException, InterruptedException {
        Thread.sleep(1000 * THREAD_SLEEP_SECOND[2]);

        if (!stepMap.get("needReply")) {
            return;
        }

        String pageUrl = replyInfoMap.get("pageUrl");
        String replyUrl = replyInfoMap.get("replyUrl");
        String formHash = replyInfoMap.get("formHash");

        //=================================回复帖子===============================================
        HttpUriRequest postReplyRequest = RequestBuilder.post().setUri(new URI(replyUrl)).setConfig(requestConfig).addHeader("Accept-Encoding", "gzip").setHeader("Upgrade-Insecure-Requests", "1").setHeader("Accept", "application/json").setHeader("Content-Type", "application/x-www-form-urlencoded").setHeader("X-Requested-With", "XMLHttpRequest").setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36")
                // 设置回帖信息
                .addParameter("message", "我终将会赢的").addParameter("formhash", formHash).addParameter("usesig", "").addParameter("subject", "").build();
        try {
            CloseableHttpResponse response2 = httpClient.execute(postReplyRequest);
            String html2 = EntityUtils.toString(response2.getEntity(), "utf-8");
            if (html2.contains("抱歉，本主题已关闭，不再接受新内容")) {
                System.out.println("[回帖失败]本主题已关闭(不能回帖), 应该不是资源贴或已经过期了, pageUrl:" + pageUrl + ",formHash:" + formHash + ",replyUrl:" + replyUrl);

                stopReplyLinks.add(pageUrl);

                //回复失败, 不能获取下载信息
                stepMap.put("needGetDownInfo", false);

                //流程提前结束结束, 计数器-1
                doCountdown(countDownLatch);
            } else if (html2.contains("抱歉，您所在的用户组每小时限制发回帖")) {
                System.out.println("[回帖失败]抱歉，您所在的用户组每小时限制发回帖, pageUrl:" + pageUrl + ",formHash:" + formHash + ",replyUrl:" + replyUrl);

                stopReplyLinks.add(pageUrl);

                //回复失败, 不能获取下载信息
                stepMap.put("needGetDownInfo", false);

                //流程提前结束结束, 计数器-1
                doCountdown(countDownLatch);
            } else if (response2.getStatusLine().getStatusCode() == 200 || response2.getStatusLine().getStatusCode() == 301) {
                System.out.println("[回帖成功]pageUrl:" + pageUrl + ",formHash:" + formHash + ",replyUrl:" + replyUrl);
            }
            postReplyRequest.abort();
            response2.close();

            jobHandler.doJob();
        } catch (Exception e) {
            System.out.println("回复帖子[" + pageUrl + "]失败");
            e.printStackTrace();
            httpFailLinks.add(pageUrl);
            doCountdown(countDownLatch);
        }
    }

    private static void doGetDownloadInfo(Map<String, Boolean> stepMap, String username, CloseableHttpClient httpClient, String pageUrl, CountDownLatch countDownLatch) throws URISyntaxException {
        if (!stepMap.get("needGetDownInfo")) {
            return;
        }

        //=================================获取下载链接===============================================
        HttpUriRequest getPageRequest = RequestBuilder.get().setUri(new URI(pageUrl)).setConfig(requestConfig).addHeader("Accept-Encoding", "gzip").setHeader("Upgrade-Insecure-Requests", "1").setHeader("Accept", "application/json").setHeader("Content-Type", "application/x-www-form-urlencoded").setHeader("X-Requested-With", "XMLHttpRequest").setHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.100 Safari/537.36").build();

        try {
            CloseableHttpResponse response3 = httpClient.execute(getPageRequest);
            String html = EntityUtils.toString(response3.getEntity(), "utf-8");

            getPageRequest.abort();
            response3.close();

            doGetDownloadInfoFromDoc(html, username, pageUrl, countDownLatch);
        } catch (Exception e) {
            System.out.println("获取下载信息[" + pageUrl + "]失败");
            e.printStackTrace();
            httpFailLinks.add(pageUrl);
            doCountdown(countDownLatch);
        }
    }

    private static void doGetDownloadInfoFromDoc(String html, String username, String pageUrl, CountDownLatch countDownLatch) throws InterruptedException {
        Thread.sleep(1000 * THREAD_SLEEP_SECOND[3]);

        Document doc = Jsoup.parse(html);
        String title = doc.select("#thread_subject").text();
        String link = doc.select("#pt > div > a:nth-child(9)").attr("href");
        String downloadInfoStr = doc.select(".t_f > div > font > strong").outerHtml();

        if (title == null || link == null || downloadInfoStr == null) {
            System.out.println("[获取下载失败]" + username + ":" + pageUrl);
            System.exit(1);
        } else {
            System.out.println("[获取下载成功]" + username + ":" + pageUrl);
        }

        Map<String, String> downloadInfo = new HashMap<>();
        downloadInfo.put("user", username);
        downloadInfo.put("title", title);
        downloadInfo.put("link", link);
        downloadInfo.put("downloadInfo", downloadInfoStr);
        downloadInfoList.add(downloadInfo);

        //流程结束, 计数器-1
        doCountdown(countDownLatch);
    }

    private static void doCountdown(CountDownLatch countDownLatch) {
        countDownLatch.countDown();
        countDownLatchTotal.countDown();
        System.out.println("countDownLatchBatch-count:" + countDownLatch.getCount());
        System.out.println("countDownLatchTotal-count:" + countDownLatchTotal.getCount());
    }

}