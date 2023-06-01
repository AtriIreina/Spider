package lostlife.club;

import com.alibaba.fastjson.JSONObject;
import com.mysql.cj.util.StringUtils;
import org.jsoup.Jsoup;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MyJob2 {

    static List<String> linksOverFilePath = new ArrayList<>();

    static {
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_HTML_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_合集_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_国产_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_日韩_列表_详情2.txt");
        linksOverFilePath.add("D:\\___earn\\_lostlife.snyh.top\\__链接__south.yuanacg.com_游戏_欧美_列表_详情2.txt");
    }

    public static void main(String[] args) {
        linksOverFilePath.forEach(filePath -> {
            try {
                int resCount = 0;
                write2csv(parseFile(filePath, resCount), filePath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    private static void write2csv(String resultStr, String filePath) throws IOException {
        String outFilePath = filePath.replace("txt", "csv");
        System.out.println(" ==> " + outFilePath + " <==");
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFilePath));
        writer.write(resultStr);
        writer.flush();
        writer.close();
    }

    private static String parseFile(String filePath, int resCount) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
        while (bufferedReader.ready()) {
            stringBuilder.append(bufferedReader.readLine());
        }
        //System.out.println("stringBuilder:" + stringBuilder);

        StringBuilder result = new StringBuilder();
        List<Map> downloadTypeList = JSONObject.parseArray(stringBuilder.toString(), Map.class);

        for (Map map : downloadTypeList) {
            if (!StringUtils.isNullOrEmpty((String)map.get("downloadInfo"))){
                String downloadInfo = Jsoup.parse(map.get("downloadInfo") + "").text();
                if (result.indexOf(downloadInfo) == -1){
                    result.append(downloadInfo.replaceAll(" ", ",")).append("\n");
                    resCount++;
                }
            }
        }
        new Robot();
//        //去处空行
//        String resultStr = result.toString();
//        resultStr.replace("\n\n", "\n");

        System.out.println(filePath + " [" + resCount + "条资源链接]");
        //System.out.println("result:" + result);
        return result.toString();
    }

}
