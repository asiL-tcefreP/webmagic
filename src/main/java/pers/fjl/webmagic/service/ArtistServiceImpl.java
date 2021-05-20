package pers.fjl.webmagic.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.nd4j.shade.jackson.databind.DeserializationFeature;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pers.fjl.webmagic.po.Artist;
import pers.fjl.webmagic.dao.ArtistDao;

import javax.annotation.Resource;

@Service
public class ArtistServiceImpl {
    @Resource
    private ArtistDao artistDao;

    /**
     * 获取单个歌手的详情信息
     *
     * @param artistId
     * @throws Exception
     */
    public void crawlArtist(Long artistId) throws Exception {
        //        1.打开浏览器，创建HttpClient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        //2.输入网址，发起get请求创建HttpGet对象
        HttpGet httpGet = new HttpGet("http://music.163.com/api/artist/albums/" + artistId + "?id=" + artistId + "&offset=0&total=true&limit=1");

        RequestConfig requestConfig = RequestConfig.custom()
//                .setProxy(proxy)
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(3000)
                .build();
        httpGet.setConfig(requestConfig);
        //设置请求头消息
        httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");

        //3.按回车发起请求，返回响应
        CloseableHttpResponse response = httpClient.execute(httpGet);

        //4.解析响应，获取数据
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity httpEntity = response.getEntity();
            String content = EntityUtils.toString(httpEntity, "utf-8");
            JSONObject jsonObject = JSONObject.parseObject(content);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            Artist artist = mapper.readValue(jsonObject.get("artist").toString(), Artist.class);
            artistDao.insert(artist);
        }
    }
}
