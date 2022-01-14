package com.sxcoal.dfsp.rtsp.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.alibaba.fastjson.JSONObject;
import com.sxcoal.dfsp.rtsp.cache.CacheUtil;
import com.sxcoal.dfsp.rtsp.client.FactoryManagerClient;
import com.sxcoal.dfsp.rtsp.model.dto.CameraInfoDTO;
import com.sxcoal.dfsp.rtsp.pojo.CameraPojo;
import com.sxcoal.dfsp.rtsp.pojo.Config;
import com.sxcoal.dfsp.rtsp.thread.CameraThread;
import com.sxcoal.dfsp.rtsp.util.Utils;
import com.sxcoal.dfsp.web.common.model.OAuth2AccessToken;
import com.sxcoal.dfsp.web.common.result.Result;
import com.sxcoal.dfsp.web.mybatisplus.TenantContextHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 监控转码播放地址
 * </p>
 *
 * @author dudj
 * @since 2022-01-08
 */
@RestController
@RequestMapping("/camera")
@Slf4j
@Api(tags = {"监控转码"})
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", allowCredentials = "true")
public class CameraController {

    @Autowired
    public Config config;// 配置文件bean

    @Autowired
    private FactoryManagerClient factoryManagerClient;
    // 存放任务 线程
    public static Map<String, CameraThread.MyRunnable> JOBMAP = new HashMap<String, CameraThread.MyRunnable>();

    /**
     * 开启视频流
     * @param id
     * @param accountId
     * @return
     */
    @RequestMapping(value = "/open", method = RequestMethod.POST)
    @ApiOperation(value = "通过监控id打开视频流，进行转化")
    public Map<String, Object> openCamera(@RequestParam(value = "id",required = true) String id,
                                          @RequestParam(value = "accountId",required = false) String accountId,
                                          @RequestParam(value = "startTime",required = false) String startTime,
                                          @RequestParam(value = "endTime",required = false) String endTime) {
        Optional<OAuth2AccessToken> user = Optional.ofNullable(TenantContextHolder.getUser());
        if(StringUtils.isEmpty(accountId))
            accountId = user.map(OAuth2AccessToken::getAccountId).orElse(null);
        // 返回结果
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        // openStream返回结果
        Map<String, Object> openMap = new HashMap<>();
        List<CameraInfoDTO> cameraInfoDTOList = factoryManagerClient.getListByParam(id, accountId).getContent();
        if(null == cameraInfoDTOList || cameraInfoDTOList.size() == 0){
            map.put("msg", "参数有误");
            map.put("code", 1);
            return map;
        }
        log.info("监控信息的返回结果：{}",cameraInfoDTOList);
        if(null == cameraInfoDTOList || cameraInfoDTOList.size() < 0){
            map.put("msg", "输入参数不完整");
            map.put("code", 1);
            return map;
        }
        CameraInfoDTO cameraInfoDTO = cameraInfoDTOList.get(0);
        log.info("流相关信息：{}",cameraInfoDTO);
        // ip格式校验
        if (!Utils.isTrueIp(cameraInfoDTO.getIp())) {
            map.put("msg", "ip格式输入错误");
            map.put("code", 2);
            return map;
        }
        if (null != startTime || "".equals(startTime)) {
            // 开始时间校验
            if (!Utils.isTrueTime(startTime)) {
                map.put("msg", "starttime格式输入错误");
                map.put("code", 3);
                return map;
            }
            if (null != endTime || "".equals(endTime)) {
                if (!Utils.isTrueTime(endTime)) {
                    map.put("msg", "endtime格式输入错误");
                    map.put("code", 4);
                    return map;
                }
                // 结束时间要大于开始时间
                try {
                    long starttime = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").parse(startTime).getTime();
                    long endtime = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").parse(endTime).getTime();
                    if (endtime - starttime < 0) {
                        map.put("msg", "endtime需要大于starttime");
                        map.put("code", 5);
                        return map;
                    }
                } catch (ParseException e) {
                    log.error("结束时间要大于开始时间出现异常:{}，详细信息：{}",e.getMessage(),e);
                }
            }
        }
        CameraPojo cameraPojo = new CameraPojo();
        // 获取当前时间
        String opentime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime());
        Set<String> keys = CacheUtil.STREATMAP.keySet();
        log.info("缓存keys：{}",keys);
        // 缓存是否为空
        if (0 == keys.size()) {
            // 开始推流
            openMap = openStream(cameraInfoDTO.getIp(), cameraInfoDTO.getUsername(), cameraInfoDTO.getPassword(), cameraInfoDTO.getChannel(), cameraInfoDTO.getStream(),cameraInfoDTO.getPort(), startTime, endTime, opentime);
            log.info("缓存---推流信息：{}",openMap);
            if (Integer.parseInt(openMap.get("errorcode").toString()) == 0) {
                map.put("url", ((CameraPojo) openMap.get("pojo")).getUrl());
                map.put("token", ((CameraPojo) openMap.get("pojo")).getToken());
                map.put("msg", "打开视频流成功");
                map.put("code", 0);
            } else {
                map.put("msg", openMap.get("message"));
                map.put("code", openMap.get("errorcode"));
            }
        } else {
            // 是否存在的标志；false：不存在；true：存在
            boolean sign = false;
            if (null == startTime) {// 直播流
                for (String key : keys) {
                    //端口和ip一致 渠道目前都是一致的
                    if (cameraInfoDTO.getIp().equals(CacheUtil.STREATMAP.get(key).getIp())
                            && cameraInfoDTO.getPort().equals(CacheUtil.STREATMAP.get(key).getPort())
                            && cameraInfoDTO.getChannel().equals(CacheUtil.STREATMAP.get(key).getChannel())
                            && null == CacheUtil.STREATMAP.get(key).getStarttime()) {// 存在直播流
                        cameraPojo = CacheUtil.STREATMAP.get(key);
                        sign = true;
                        break;
                    }
                }
                if (sign) {// 存在
                    cameraPojo.setCount(cameraPojo.getCount() + 1);
                    cameraPojo.setOpentime(opentime);
                    map.put("url", cameraPojo.getUrl());
                    map.put("token", cameraPojo.getToken());
                    map.put("msg", "打开视频流成功");
                    map.put("code", 0);
                } else {
                    openMap = openStream(cameraInfoDTO.getIp(), cameraInfoDTO.getUsername(), cameraInfoDTO.getPassword(), cameraInfoDTO.getChannel(),cameraInfoDTO.getStream(),cameraInfoDTO.getPort(), startTime, endTime, opentime);
                    if (Integer.parseInt(openMap.get("errorcode").toString()) == 0) {
                        map.put("url", ((CameraPojo) openMap.get("pojo")).getUrl());
                        map.put("token", ((CameraPojo) openMap.get("pojo")).getToken());
                        map.put("msg", "打开视频流成功");
                        map.put("code", 0);
                    } else {
                        map.put("msg", openMap.get("message"));
                        map.put("code", openMap.get("errorcode"));
                    }
                }

            } else {// 历史流
                for (String key : keys) {
                    if (cameraInfoDTO.getIp().equals(CacheUtil.STREATMAP.get(key).getIp())
                            && CacheUtil.STREATMAP.get(key).getStarttime() != null) {// 存在历史流
                        sign = true;
                        cameraPojo = CacheUtil.STREATMAP.get(key);
                        break;
                    }
                }
                if (sign && cameraPojo.getCount() == 0) {
                    map.put("msg", "设备正在结束回放,请稍后再试");
                    map.put("code", 9);
                } else if (sign && cameraPojo.getCount() != 0) {
                    map.put("msg", "设备正在进行回放,请稍后再试");
                    map.put("code", 8);
                } else {
                    openMap = openStream(cameraInfoDTO.getIp(), cameraInfoDTO.getUsername(), cameraInfoDTO.getPassword(), cameraInfoDTO.getChannel(),cameraInfoDTO.getStream(),cameraInfoDTO.getPort(), startTime, endTime, opentime);
                    if (Integer.parseInt(openMap.get("errorcode").toString()) == 0) {
                        map.put("url", ((CameraPojo) openMap.get("pojo")).getUrl());
                        map.put("token", ((CameraPojo) openMap.get("pojo")).getToken());
                        map.put("msg", "打开视频流成功");
                        map.put("code", 0);
                    } else {
                        map.put("msg", openMap.get("message"));
                        map.put("code", openMap.get("errorcode"));
                    }
                }
            }
        }
        log.info("推流信息：{}",openMap);
        return map;
    }
    /**
     * 开启视频流
     * @param pojo
     * channel   通道
     * stream    码流
     * @return
     */
    @RequestMapping(value = "/openJson", method = RequestMethod.POST)
    @ApiOperation(value = "通过json数据打开视频流")
    public Map<String, Object> openCameraByJson(@RequestBody CameraPojo pojo) {
        // 返回结果
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        // openStream返回结果
        Map<String, Object> openMap = new HashMap<>();
        JSONObject cameraJson = JSONObject.parseObject(JSONObject.toJSON(pojo).toString());
        // 需要校验非空的参数
        String[] isNullArr = { "ip", "username", "password", "channel", "stream" };
        // 空值校验
        if (!Utils.isNullParameters(cameraJson, isNullArr)) {
            map.put("msg", "输入参数不完整");
            map.put("code", 1);
            return map;
        }
        // ip格式校验
        if (!Utils.isTrueIp(pojo.getIp())) {
            map.put("msg", "ip格式输入错误");
            map.put("code", 2);
            return map;
        }
        if (null != pojo.getStarttime() || "".equals(pojo.getStarttime())) {
            // 开始时间校验
            if (!Utils.isTrueTime(pojo.getStarttime())) {
                map.put("msg", "starttime格式输入错误");
                map.put("code", 3);
                return map;
            }
            if (null != pojo.getEndtime() || "".equals(pojo.getEndtime())) {
                if (!Utils.isTrueTime(pojo.getEndtime())) {
                    map.put("msg", "endtime格式输入错误");
                    map.put("code", 4);
                    return map;
                }
                // 结束时间要大于开始时间
                try {
                    long starttime = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").parse(pojo.getStarttime()).getTime();
                    long endtime = new SimpleDateFormat("yyyy-MM-dd HH:ss:mm").parse(pojo.getEndtime()).getTime();
                    if (endtime - starttime < 0) {
                        map.put("msg", "endtime需要大于starttime");
                        map.put("code", 5);
                        return map;
                    }
                } catch (ParseException e) {
                    log.error("结束时间要大于开始时间出现异常:{}，详细信息：{}",e.getMessage(),e);
                }
            }
        }
        CameraPojo cameraPojo = new CameraPojo();
        // 获取当前时间
        String opentime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime());
        Set<String> keys = CacheUtil.STREATMAP.keySet();
        // 缓存是否为空
        if (0 == keys.size()) {
            // 开始推流
            openMap = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
                    pojo.getStream(),pojo.getPort(), pojo.getStarttime(), pojo.getEndtime(), opentime);
            if (Integer.parseInt(openMap.get("errorcode").toString()) == 0) {
                map.put("url", ((CameraPojo) openMap.get("pojo")).getUrl());
                map.put("token", ((CameraPojo) openMap.get("pojo")).getToken());
                map.put("msg", "打开视频流成功");
                map.put("code", 0);
            } else {
                map.put("msg", openMap.get("message"));
                map.put("code", openMap.get("errorcode"));
            }
        } else {
            // 是否存在的标志；false：不存在；true：存在
            boolean sign = false;
            if (null == pojo.getStarttime()) {// 直播流
                for (String key : keys) {
                    if (pojo.getIp().equals(CacheUtil.STREATMAP.get(key).getIp())
                            && pojo.getChannel().equals(CacheUtil.STREATMAP.get(key).getChannel())
                            && null == CacheUtil.STREATMAP.get(key).getStarttime()) {// 存在直播流
                        cameraPojo = CacheUtil.STREATMAP.get(key);
                        sign = true;
                        break;
                    }
                }
                if (sign) {// 存在
                    cameraPojo.setCount(cameraPojo.getCount() + 1);
                    cameraPojo.setOpentime(opentime);
                    map.put("url", cameraPojo.getUrl());
                    map.put("token", cameraPojo.getToken());
                    map.put("msg", "打开视频流成功");
                    map.put("code", 0);
                } else {
                    openMap = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
                            pojo.getStream(),pojo.getPort(), pojo.getStarttime(), pojo.getEndtime(), opentime);
                    if (Integer.parseInt(openMap.get("errorcode").toString()) == 0) {
                        map.put("url", ((CameraPojo) openMap.get("pojo")).getUrl());
                        map.put("token", ((CameraPojo) openMap.get("pojo")).getToken());
                        map.put("msg", "打开视频流成功");
                        map.put("code", 0);
                    } else {
                        map.put("msg", openMap.get("message"));
                        map.put("code", openMap.get("errorcode"));
                    }
                }

            } else {// 历史流
                for (String key : keys) {
                    if (pojo.getIp().equals(CacheUtil.STREATMAP.get(key).getIp())
                            && CacheUtil.STREATMAP.get(key).getStarttime() != null) {// 存在历史流
                        sign = true;
                        cameraPojo = CacheUtil.STREATMAP.get(key);
                        break;
                    }
                }
                if (sign && cameraPojo.getCount() == 0) {
                    map.put("msg", "设备正在结束回放,请稍后再试");
                    map.put("code", 9);
                } else if (sign && cameraPojo.getCount() != 0) {
                    map.put("msg", "设备正在进行回放,请稍后再试");
                    map.put("code", 8);
                } else {
                    openMap = openStream(pojo.getIp(), pojo.getUsername(), pojo.getPassword(), pojo.getChannel(),
                            pojo.getStream(),pojo.getPort(), pojo.getStarttime(), pojo.getEndtime(), opentime);
                    if (Integer.parseInt(openMap.get("errorcode").toString()) == 0) {
                        map.put("url", ((CameraPojo) openMap.get("pojo")).getUrl());
                        map.put("token", ((CameraPojo) openMap.get("pojo")).getToken());
                        map.put("msg", "打开视频流成功");
                        map.put("code", 0);
                    } else {
                        map.put("msg", openMap.get("message"));
                        map.put("code", openMap.get("errorcode"));
                    }
                }
            }
        }
        return map;
    }
    /**
     * @Title: openStream
     * @Description: 推流器
     * @param ip
     * @param username
     * @param password
     * @param channel
     * @param stream
     * @param port
     * @param starttime
     * @param endtime
     * @param opentime
     * @return
     * @return CameraPojo
     * @throws IOException
     **/
    private Map<String, Object> openStream(String ip, String username, String password, String channel, String stream,String port,
                                           String starttime, String endtime, String opentime) {
        Map<String, Object> map = new HashMap<>();
        CameraPojo cameraPojo = new CameraPojo();
        // 生成token
        String token = UUID.randomUUID().toString();
        String rtsp = "";
        String rtmp = "";
        String IP = Utils.IpConvert(ip);
        String url = "";
        boolean sign = false;// 该nvr是否再回放，true：在回放；false： 没在回放
        // 历史流
        if (null != starttime && !"".equals(starttime)) {
            if (null != endtime && !"".equals(endtime)) {
                rtsp = "rtsp://" + username + ":" + password + "@" + IP + ":" + port + "/Streaming/tracks/"
                        + (Integer.valueOf(channel) - 32) + "01?starttime=" + Utils.getTime(starttime).substring(0, 8)
                        + "t" + Utils.getTime(starttime).substring(8) + "z'&'endtime="
                        + Utils.getTime(endtime).substring(0, 8) + "t" + Utils.getTime(endtime).substring(8) + "z";
                cameraPojo.setStarttime(Utils.getTime(starttime));
                cameraPojo.setEndtime(Utils.getTime(endtime));
            } else {
                String startTime = Utils.getStarttime(starttime);
                String endTime = Utils.getEndtime(starttime);
                rtsp = "rtsp://" + username + ":" + password + "@" + IP + ":" + port + "/Streaming/tracks/"
                        + (Integer.valueOf(channel) - 32) + "01?starttime=" + startTime.substring(0, 8) + "t"
                        + startTime.substring(8) + "z'&'endtime=" + endTime.substring(0, 8) + "t" + endTime.substring(8)
                        + "z";
                cameraPojo.setStarttime(Utils.getStarttime(starttime));
                cameraPojo.setEndtime(Utils.getEndtime(starttime));
            }
//			rtmp = "rtmp://" + Utils.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/history/"
//					+ token;
            rtmp = "rtmp://" + Utils.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/history/test";
            if (config.getHost_extra().equals("127.0.0.1")) {
                url = rtmp;
            } else {
                url = "rtmp://" + Utils.IpConvert(config.getHost_extra()) + ":" + config.getPush_port() + "/history/"
                        + token;
            }
        } else {// 直播流
            rtsp = "rtsp://" + username + ":" + password + "@" + IP + ":" + port + "/Streaming/Channels/" + channel + "?transportmode=" + stream;
            rtmp = "rtmp://" + Utils.IpConvert(config.getPush_host()) + ":" + config.getPush_port() + "/live/" + token;
            if (config.getHost_extra().equals("127.0.0.1")) {
                url = rtmp;
            } else {
                url = "rtmp://" + Utils.IpConvert(config.getHost_extra()) + ":" + config.getPush_port() + "/live/"
                        + token;
            }
        }

        cameraPojo.setUsername(username);
        cameraPojo.setPassword(password);
        cameraPojo.setIp(IP);
        cameraPojo.setChannel(channel);
        cameraPojo.setStream(stream);
        cameraPojo.setRtsp(rtsp);
        cameraPojo.setRtmp(rtmp);
        cameraPojo.setUrl(url);
        cameraPojo.setOpentime(opentime);
        cameraPojo.setCount(1);
        cameraPojo.setToken(token);
        cameraPojo.setPort(port);

        // 解决ip输入错误时，grabber.start();出现阻塞无法释放grabber而导致后续推流无法进行；
        Socket rtspSocket = new Socket();
        Socket rtmpSocket = new Socket();
        log.info("拼凑信息cameraPojo:{}",cameraPojo);
        // 建立TCP Scoket连接，超时时间1s，如果成功继续执行，否则return
        try {
            rtspSocket.connect(new InetSocketAddress(cameraPojo.getIp(), Integer.parseInt(port)), 50000);
        } catch (IOException e) {
            log.error("建立TCP Socket链接失败，信息为：{}，详细信息为：{}",e.getMessage(),e);
            log.error("与拉流IP：  {}   端口：   {}    建立TCP连接失败！",cameraPojo.getIp(),cameraPojo.getPort());
            map.put("pojo", cameraPojo);
            map.put("errorcode", 6);
            map.put("message", "与拉流IP：   " + cameraPojo.getIp() + "   端口：   "+cameraPojo.getPort()+"    建立TCP连接失败！");
            return map;
        }
        try {
            rtmpSocket.connect(new InetSocketAddress(Utils.IpConvert(config.getPush_host()),
                    Integer.parseInt(config.getPush_port())), 50000);
        } catch (IOException e) {
            log.error("rtmpSocket 建立TCP Socket链接失败，信息为：{}，详细信息为：{}",e.getMessage(),e);
            log.error("与拉流IP：  {}   端口：   {}    建立TCP连接失败！",config.getPush_host(),config.getPush_port());
            map.put("pojo", cameraPojo);
            map.put("errorcode", 7);
            map.put("message",
                    "与推流IP:" + config.getPush_host() + " 端口: " + config.getPush_port() + " 建立连接失败,请检查nginx服务");
            return map;
        }
        // 执行任务
        CameraThread.MyRunnable job = new CameraThread.MyRunnable(cameraPojo);
        CameraThread.MyRunnable.es.execute(job);
        JOBMAP.put(token, job);
        map.put("pojo", cameraPojo);
        map.put("errorcode", 0);
        map.put("message", "打开视频流成功");
        return map;
    }

    /**
     * @Title: closeCamera
     * @Description:关闭视频流
     * @param tokens
     * @return void
     **/
    @RequestMapping(value = "/cameras/{tokens}", method = RequestMethod.DELETE)
    @ApiOperation(value = "关闭视频流")
    public void closeCamera(@PathVariable("tokens") String tokens) {
        if (null != tokens && !"".equals(tokens)) {
            String[] tokenArr = tokens.split(",");
            for (String token : tokenArr) {
                if (JOBMAP.containsKey(token) && CacheUtil.STREATMAP.containsKey(token)) {
                    // 回放手动关闭
                    if (null != CacheUtil.STREATMAP.get(token).getStarttime()) {
                        if (0 == CacheUtil.STREATMAP.get(token).getCount() - 1) {
                            CacheUtil.PUSHMAP.get(token).setExitcode(1);
                            CacheUtil.STREATMAP.get(token).setCount(CacheUtil.STREATMAP.get(token).getCount() - 1);
                        } else {
                            CacheUtil.STREATMAP.get(token).setCount(CacheUtil.STREATMAP.get(token).getCount() - 1);
                            log.info("当前设备正在进行回放，使用人数为" + CacheUtil.STREATMAP.get(token).getCount() + " 设备信息：[ip："
                                    + CacheUtil.STREATMAP.get(token).getIp() + " channel:"
                                    + CacheUtil.STREATMAP.get(token).getChannel() + " stream:"
                                    + CacheUtil.STREATMAP.get(token).getStream() + " statrtime:"
                                    + CacheUtil.STREATMAP.get(token).getStream() + " endtime:"
                                    + CacheUtil.STREATMAP.get(token).getEndtime() + " url:"
                                    + CacheUtil.STREATMAP.get(token).getUrl() + "]");
                        }
                    } else {
                        if (0 < CacheUtil.STREATMAP.get(token).getCount()) {
                            // 人数-1
                            CacheUtil.STREATMAP.get(token).setCount(CacheUtil.STREATMAP.get(token).getCount() - 1);
                            log.info("关闭成功 当前设备使用人数为" + CacheUtil.STREATMAP.get(token).getCount() + " 设备信息：[ip："
                                    + CacheUtil.STREATMAP.get(token).getIp() + " channel:"
                                    + CacheUtil.STREATMAP.get(token).getChannel() + " stream:"
                                    + CacheUtil.STREATMAP.get(token).getStream() + " statrtime:"
                                    + CacheUtil.STREATMAP.get(token).getStream() + " endtime:"
                                    + CacheUtil.STREATMAP.get(token).getEndtime() + " url:"
                                    + CacheUtil.STREATMAP.get(token).getUrl() + "]");
                        }
                    }

                }
            }
        }
    }

    /**
     * @Title: getCameras
     * @Description:获取视频流
     * @return Map<String, CameraPojo>
     **/
    @RequestMapping(value = "/cameras", method = RequestMethod.GET)
    @ApiOperation(value = "获取视频流信息")
    public Result<Map<String, CameraPojo>> getCameras() {
        log.info("获取视频流信息：{}", CacheUtil.STREATMAP.toString());
        return Result.success(CacheUtil.STREATMAP);
    }

    /**
     * @Title: keepAlive
     * @Description:视频流保活
     * @param tokens
     * @return void
     **/
    @RequestMapping(value = "/keepAlive/{tokens}", method = RequestMethod.PUT)
    @ApiOperation(value = "重新激活当前视频流")
    public void keepAlive(@PathVariable("tokens") String tokens) {
        // 校验参数
        if (null != tokens && !"".equals(tokens)) {
            String[] tokenArr = tokens.split(",");
            for (String token : tokenArr) {
                CameraPojo cameraPojo = new CameraPojo();
                // 直播流token
                if (null != CacheUtil.STREATMAP.get(token)) {
                    cameraPojo = CacheUtil.STREATMAP.get(token);
                    // 更新当前系统时间
                    cameraPojo.setOpentime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date().getTime()));
                    log.info("保活成功 设备信息：[ip：{} channel:{} stream:{} starttime:{} endtime:{} url:{}]",cameraPojo.getIp(),cameraPojo.getChannel(),cameraPojo.getStream(),cameraPojo.getStarttime(),cameraPojo.getEndtime(),cameraPojo.getUrl());
                }
            }
        }
    }

    /**
     * @Title: getConfig
     * @Description: 获取服务信息
     * @return Map<String, Object>
     **/
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ApiOperation(value = "获取服务信息")
    public Map<String, Object> getConfig() {
        // 获取当前时间
        long nowTime = new Date().getTime();
        String upTime = (nowTime - CacheUtil.STARTTIME) / (1000 * 60 * 60) + "h"
                + (nowTime - CacheUtil.STARTTIME) % (1000 * 60 * 60) / (1000 * 60) + "m"
                + (nowTime - CacheUtil.STARTTIME) % (1000 * 60 * 60) / (1000) + "s";
        log.info("获取服务信息：{};服务运行时间：{}",config.toString(),upTime);
        Map<String, Object> status = new HashMap<String, Object>();
        status.put("config", config);
        status.put("uptime", upTime);
        return status;
    }

}
