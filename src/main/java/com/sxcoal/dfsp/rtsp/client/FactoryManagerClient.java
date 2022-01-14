package com.sxcoal.dfsp.rtsp.client;


import com.sxcoal.dfsp.rtsp.model.dto.CameraInfoDTO;
import com.sxcoal.dfsp.web.common.result.Result;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "dfsp-factory-manager")
public interface FactoryManagerClient {

    @PostMapping(value = "/cameraInfo/getListByParam")
    @ApiOperation(value = "分页查询原料")
    Result<List<CameraInfoDTO>> getListByParam(@RequestParam(value = "id", required = true) String id,
                                        @RequestParam(value = "accountId", required = true) String accountId);
}
