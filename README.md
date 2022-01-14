项目目录结构的解释

1.com.sxcoal.dfsp.camera包里的类为SpringBoot项目启动类。

2.com.sxcoal.dfsp.rtsp.cache包里的类为保存推流信息的缓存类。
‘
3.com.sxcoal.dfsp.rtsp.controller包里的类为项目controller API接口。

4.com.sxcoal.dfsp.rtsp.pojo包里的类为相机信息和配置文件映射的bean。

5.com.sxcoal.dfsp.rtsp.thread包里的类为线程池管理类。

6.com.sxcoal.dfsp.rtsp.util包里的类为拉流推流业务处理类和定时任务Timer类。

7.application.yml为项目配置文件。

构建项目注意事项：

https://www.showdoc.com.cn/182553561910083/8295327034446164


项目中包含了rtsp转rtmp，通过浏览器安装flash进行播放，21年之前的版本支持。

rtsp转m3u8，通过video播放
