package com.sxcoal.dfsp.rtsp.pojo;

import lombok.Data;
import java.io.Serializable;
@Data
public class CameraPojo implements Serializable{
    private static final long serialVersionUID = 8183688502930584159L;
    private String username;// 摄像头账号
    private String password;// 摄像头密码
    private String ip;// 摄像头ip
    private String channel;// 摄像头通道
    private String stream;// 摄像头码流
    private String rtsp;// rtsp地址
    private String rtmp;// rtmp地址
    private String url;// 播放地址
    private String starttime;// 回放开始时间
    private String endtime;// 回放结束时间
    private String opentime;// 打开时间
    private int count = 0;// 使用人数
    private String token;
    private String port;//端口
}
