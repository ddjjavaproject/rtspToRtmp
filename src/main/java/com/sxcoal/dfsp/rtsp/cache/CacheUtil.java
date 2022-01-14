package com.sxcoal.dfsp.rtsp.cache;

import com.sxcoal.dfsp.rtsp.pojo.CameraPojo;
import com.sxcoal.dfsp.rtsp.push.CameraPush;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推流缓存信息
 */
public final class CacheUtil {
    /*
     * 保存已经开始推的流
     */
    public static Map<String, CameraPojo> STREATMAP = new ConcurrentHashMap<String, CameraPojo>();

    /*
     * 保存push
     */
    public static Map<String, CameraPush> PUSHMAP = new ConcurrentHashMap<>();
    /*
     * 保存服务启动时间
     */
    public static long STARTTIME;
}
