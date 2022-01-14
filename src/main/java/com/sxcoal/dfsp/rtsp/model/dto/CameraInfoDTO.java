package com.sxcoal.dfsp.rtsp.model.dto;

import com.sxcoal.dfsp.web.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * <p>
 * 监控信息表
 * </p>
 *
 * @author dudj
 * @since 2022-01-08
 */
@Data
@ApiModel(value = "监控信息表", description = "监控信息表")
public class CameraInfoDTO extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 摄像头账户
     */
    @ApiModelProperty(value = "摄像头账户", example = "摄像头账户")
    private String username;
    /**
     * 摄像头密码
     */
    @ApiModelProperty(value = "摄像头密码", example = "摄像头密码")
    private String password;
    /**
     * 摄像头ip
     */
    @ApiModelProperty(value = "摄像头ip", example = "摄像头ip")
    private String ip;
    /**
     * 摄像头渠道
     */
    @ApiModelProperty(value = "摄像头渠道", example = "摄像头渠道")
    private String channel;
    /**
     * 摄像头码流
     */
    @ApiModelProperty(value = "摄像头码流", example = "摄像头码流")
    private String stream;
    /**
     * 地址
     */
    @ApiModelProperty(value = "地址", example = "地址")
    private String url;
    /**
     * 类型 1 海康
     */
    @ApiModelProperty(value = "类型 1 海康", example = "类型 1 海康")
    private Integer type;
    /**
     * 监控名称
     */
    @ApiModelProperty(value = "监控名称", example = "监控名称")
    private String name;
    /**
     * 是否禁用 0 禁用 1 正常
     */
    @ApiModelProperty(value = "是否禁用 0 禁用 1 正常", example = "是否禁用 0 禁用 1 正常")
    private Integer status;
    /**
     * 租户id
     */
    @ApiModelProperty(value = "租户id", example = "租户id")
    private Long accountId;
    /**
     * 端口
     */
    @ApiModelProperty(value = "端口", example = "端口")
    private String port;
    /**
     * 访问地址
     */
    @ApiModelProperty(value = "访问地址", example = "访问地址")
    private String requestUrl;
    /**
     * 转码时长 秒
     */
    @ApiModelProperty(value = "转码时长 秒", example = "转码时长 秒")
    private String times;
}
