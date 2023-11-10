package com.enterprise.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.enterprise.service.data.entity.EnterpriseDataService;
import com.enterprise.service.wechatService.WxCoreService;
import com.enterprise.vo.enums.PushMode;
import com.enterprise.vo.pojo.UserListVo;
import com.enterprise.vo.pojo.WeatherVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * api工具类
 *
 * @author PrefersMin
 * @version 2.1
 */
@Component
@RequiredArgsConstructor
public class ApiUtil {

    /**
     * 企业微信核心服务
     */
    private final WxCoreService wxCoreService;

    /**
     * 配置数据接口
     */
    private final EnterpriseDataService enterpriseDataService;

    /**
     * 获取彩虹屁
     *
     * @author PrefersMin
     *
     * @return 返回彩虹屁
     */
    public String getCaiHongPi() {

        // 固定请求地址，详见 https://www.tianapi.com/apiview/181
        String url = "https://api.tianapi.com/pyqwenan/index?key=";
        String str = "阳光落在屋里，爱你藏在心里";
        String key = enterpriseDataService.queryingEnterpriseData("tianApiKey").getDataValue();

        try {
            JSONObject jsonObject = JSONObject.parseObject(HttpUtil.getUrl(url + key));
            assert jsonObject != null;
            if (jsonObject.getIntValue("code") == 200) {
                // 转换返回json数据
                str = jsonObject.getJSONArray("newslist").getJSONObject(0).getString("content");
                return str;
            }
            LogUtil.error("返回码错误：获取彩虹屁失败");
        } catch (IOException e) {
            LogUtil.error("try异常：获取彩虹屁失败");
        }
        LogUtil.error("try失败：获取彩虹屁失败");
        return str;

    }

    /**
     * 获取天气数据
     *
     * @author PrefersMin
     *
     * @return 返回天气实体对象
     */
    public WeatherVo getWeather() {

        JSONObject jsonObject = null;
        WeatherVo weatherVo = new WeatherVo();
        String key, url;
        String city = enterpriseDataService.queryingEnterpriseData("weatherValue").getDataValue();
        int pushMode = Integer.parseInt(enterpriseDataService.queryingEnterpriseData("pushMode").getDataValue());
        boolean dataSources = enterpriseDataService.queryingEnterpriseData("dataSources").getDataValue().equals("1");

        if (dataSources) {
            // 天行api固定请求地址，详见 https://www.tianapi.com/apiview/72
            url = "https://api.tianapi.com/tianqi/index?key=";
            key = enterpriseDataService.queryingEnterpriseData("tianApiKey").getDataValue();
            // 天行请求
            try {
                jsonObject = JSONObject.parseObject(HttpUtil.getUrl(url + key + "&city=" + city));
            } catch (IOException e) {
                LogUtil.error("天行数据获取失败");
                LogUtil.error(e.getMessage());
            }
        } else {
            // 高德api固定请求地址，详见 https://lbs.amap.com/api/webservice/guide/api/weatherinfo/#t1
            url = "https://restapi.amap.com/v3/weather/weatherInfo?key=";
            key = enterpriseDataService.queryingEnterpriseData("amapKey").getDataValue();
            // 高德请求
            try {
                jsonObject = JSONObject.parseObject(HttpUtil.getUrl(url + key + "&city=" + city + "&extensions=all"));
            } catch (IOException e) {
                LogUtil.error("高德数据获取失败");
                LogUtil.error(e.getMessage());
            }
        }

        // 非空断言
        assert jsonObject != null;

        if (dataSources) {
            // 天行数据反序列化
            weatherVo = JSON.parseObject(jsonObject.getJSONArray("newslist").getJSONObject(pushMode).toString(), WeatherVo.class);

        } else {
            // 高德数据反序列化
            JSONObject Gao = jsonObject.getJSONArray("forecasts").getJSONObject(0).getJSONArray("casts").getJSONObject(pushMode);
            weatherVo.setArea(jsonObject.getJSONArray("forecasts").getJSONObject(0).getString("city"));
            weatherVo.setDate(Gao.getString("date"));
            weatherVo.setWeather(Gao.getString("dayweather"));
            weatherVo.setHighest(Gao.getString("daytemp_float"));
            weatherVo.setLowest(Gao.getString("nighttemp_float"));
        }

        weatherVo.setState(pushMode);
        LogUtil.info((PushMode.NIGHT.getValue() == pushMode) ? "当前推送的是明日天气" : "当前推送的是今日天气");
        return weatherVo;
    }

    /**
     * 获取部门id下所有成员的列表
     *
     * @author PrefersMin
     *
     * @return 返回一串拼接好的成员String
     */
    public String getParticipants() {

        // 固定请求地址，详见 https://developer.work.weixin.qq.com/document/path/90200
        String url = "https://qyapi.weixin.qq.com/cgi-bin/user/simplelist?access_token=";

        try {
            // 发送GET请求
            String sendGet = HttpUtil.getUrl(url + wxCoreService.getAccessToken() + "&department_id=" + enterpriseDataService.queryingEnterpriseData("departmentId").getDataValue());

            // 转换返回json数据
            JSONObject jsonObject = JSONObject.parseObject(sendGet);

            // 判断是否请求成功
            assert jsonObject != null;
            if (jsonObject.getIntValue("errcode") == 0) {
                // 截取userlist部门成员对象数组
                JSONArray participants = jsonObject.getJSONArray("userlist");
                // 通过部门成员类获取成员name与userid
                List<UserListVo> userListVo = participants.toJavaList(UserListVo.class);

                // 遍历拼接成员userid
                StringBuilder temp = new StringBuilder();
                for (UserListVo userlist : userListVo) {
                    if (!temp.toString().isEmpty()) {
                        temp.append("|");
                    }
                    temp.append(userlist.getUserid());
                }
                return temp.toString();
            }
            LogUtil.error("返回码错误：获取部门成员列表失败");
        } catch (IOException e) {
            LogUtil.error("try异常：获取部门成员列表失败");
        }
        LogUtil.error("try失败：获取部门成员列表失败");
        return null;
    }

}
