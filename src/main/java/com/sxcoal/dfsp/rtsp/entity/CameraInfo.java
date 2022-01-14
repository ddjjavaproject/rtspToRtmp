package com.sxcoal.dfsp.rtsp.entity;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


/**
 * <p>
 * 监控信息表
 * </p>
 *
 * @author dudj
 * @since 2021-08-22
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "监控信息表", description = "监控信息表")
@EqualsAndHashCode(callSuper = false)
public class CameraInfo {

    private static final long serialVersionUID = 1L;

    /**
     * 采购商id
     */
    @ApiModelProperty(value = "账户", example = "账户")
    private String username;
    /**
     * 密码
     */
    @ApiModelProperty(value = "密码", example = "密码")
    private String password;
    /**
     * ip
     */
    @ApiModelProperty(value = "ip", example = "ip")
    private String ip;
    /**
     * 渠道
     */
    @ApiModelProperty(value = "渠道", example = "渠道")
    private String channel;
    /**
     * 码流
     */
    @ApiModelProperty(value = "码流", example = "码流")
    private String stream;
    /**
     * 地址
     */
    @ApiModelProperty(value = "地址", example = "地址")
    private String url;
    /**
     * 是否禁用 0 禁用 1 正常
     */
    @ApiModelProperty(value = "是否禁用 0 禁用 1 正常", example = "是否禁用 0 禁用 1 正常")
    private Integer status;
    /**
     * 类型
     */
    @ApiModelProperty(value = "类型 1 海康", example = "类型 1 海康")
    private Integer type;
    /**
     * 监控名称
     */
    @ApiModelProperty(value = "监控名称", example = "监控名称")
    private String name;
    /**
     * 租户id
     */
    @ApiModelProperty(value = "租户id", example = "租户id")
    private String accountId;
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
