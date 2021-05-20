package pers.fjl.webmagic.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.nd4j.shade.jackson.core.type.TypeReference;
import org.nd4j.shade.jackson.databind.DeserializationFeature;
import org.nd4j.shade.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import pers.fjl.webmagic.dao.CommentDao;
import pers.fjl.webmagic.po.RisingListPo;
import pers.fjl.webmagic.po.User;
import pers.fjl.webmagic.dao.RisingListDao;
import pers.fjl.webmagic.po.Comment;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CommentServiceImpl {
    @Resource
    private CommentDao commentDao;
    @Resource
    private RisingListDao risingListDao;

    public void getComments(String songNum,String limit) throws Exception {
        List<RisingListPo> risingListPos = getSongId(songNum);
        for (RisingListPo risingListPo : risingListPos) {
            crawlComment(limit,risingListPo.getSongId());
        }
    }

    /**
     * 获取指定数量的歌曲的id
     *
     * @param songNum
     * @return
     */
    public List<RisingListPo> getSongId(String songNum) {
        QueryWrapper<RisingListPo> wrapper = new QueryWrapper<>();
        wrapper.select("song_id").last("limit 0," + songNum);
        return risingListDao.selectList(wrapper);
    }

    public void crawlComment(String limit, String songId) throws Exception {
        //        1.打开浏览器，创建HttpClient对象
        CloseableHttpClient httpClient = HttpClients.createDefault();

        //2.输入网址，发起get请求创建HttpGet对象
        HttpGet httpGet = new HttpGet("http://music.163.com/api/v1/resource/comments/R_SO_4_" + songId + "?limit=" + limit + "&offset=0");
//        HttpGet httpGet = new HttpGet("http://music.163.com/api/v1/resource/comments/R_SO_4_1843319714?limit=100&offset=100");
        RequestConfig requestConfig = RequestConfig.custom()
//                .setProxy(proxy)
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(3000)
                .build();
        httpGet.setConfig(requestConfig);
        //设置请求头消息
        httpGet.setHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");

        //3.按回车发起请求，返回响应
        CloseableHttpResponse response = httpClient.execute(httpGet);

        //4.解析响应，获取数据
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity httpEntity = response.getEntity();
            String content = EntityUtils.toString(httpEntity, "utf-8");
            JSONObject jsonObject = JSONObject.parseObject(content);
            System.out.println(jsonObject);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            List<Comment> commentList = mapper.readValue(jsonObject.get("comments").toString(), new TypeReference<List<Comment>>() {
            });
            for (Comment comment : commentList) {   //将user的属性复制到comment对象里
                User user = comment.getUser();
                BeanUtils.copyProperties(user, comment);
                commentDao.insert(comment);
            }
        }
    }
}
