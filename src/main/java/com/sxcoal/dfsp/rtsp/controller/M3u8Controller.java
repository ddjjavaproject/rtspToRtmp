package com.sxcoal.dfsp.rtsp.controller;

import com.sxcoal.dfsp.common.model.common.Result;
import com.sxcoal.dfsp.rtsp.client.FactoryManagerClient;
import com.sxcoal.dfsp.rtsp.model.dto.CameraInfoDTO;
import com.sxcoal.dfsp.rtsp.util.FfmpegUtil;
import com.sxcoal.dfsp.web.common.model.OAuth2AccessToken;
import com.sxcoal.dfsp.web.mybatisplus.TenantContextHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * <p>
 * 监控转码播放地址
 * </p>
 *
 * @author dudj
 * @since 2022-01-08
 */
@RestController
@RequestMapping("/m3u8")
@Slf4j
@Api(tags = {"rtsp转m3u8"})
@CrossOrigin(originPatterns = "*", allowedHeaders = "*", allowCredentials = "true")
public class M3u8Controller {
    @Autowired
    private FactoryManagerClient factoryManagerClient;
    /**
     * 开启视频流
     * @param id
     * @param accountId
     * @return
     * 1.通过当前用户登录信息，获取租户ID和用户信息
     * 2.定义文件转码存放路径，并拼接当前用户访问地址
     */
    @RequestMapping(value = "/getVideoUrl", method = RequestMethod.POST)
    @ApiOperation(value = "通过监控id打开视频流，转化id")
    public Result getVideoUrl(@RequestParam(value = "id",required = true) String id,
                             @RequestParam(value = "accountId",required = false) String accountId) {
        Optional<OAuth2AccessToken> user = Optional.ofNullable(TenantContextHolder.getUser());
        if (StringUtils.isEmpty(accountId))
            accountId = user.map(OAuth2AccessToken::getAccountId).orElse(null);

        String userId = user.map(OAuth2AccessToken::getId).orElse("");
        List<CameraInfoDTO> cameraInfoDTOList = factoryManagerClient.getListByParam(id, accountId).getContent();
        log.info("获取流信息为：{}",cameraInfoDTOList);
        if(null == cameraInfoDTOList || cameraInfoDTOList.size() == 0){
            return Result.fail("参数有误");
        }
        CameraInfoDTO cameraInfoDTO = cameraInfoDTOList.get(0);
        try{
            //租户ID-用户ID-视频流信息ID
            String fileName = accountId + "-" + userId +"-"+cameraInfoDTO.getId() + ".m3u8";
            String outPut = "/usr/local/nginx/temp/hls/" + fileName;
            log.info("转码信息，转码前url：{}，转码后存放路径：{}", cameraInfoDTO.getUrl(), outPut);
            boolean ffmpeg = FfmpegUtil.toM3u8("ffmpeg", outPut, cameraInfoDTO);
            if(ffmpeg){
                return Result.success(cameraInfoDTO.getRequestUrl() + "/" + fileName);
            }else{
                return Result.fail("转码出现异常，等待一会儿");
            }
        }catch (Exception e){
            log.error("转码出现问题：{}，详细信息：{}",e.getMessage(),e);
            return Result.fail(e.getMessage());
        }
    }
}
