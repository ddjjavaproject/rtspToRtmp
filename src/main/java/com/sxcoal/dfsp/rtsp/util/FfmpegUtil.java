package com.sxcoal.dfsp.rtsp.util;

import com.sxcoal.dfsp.rtsp.model.dto.CameraInfoDTO;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ffmpeg linux调用
 */
@Slf4j
public class FfmpegUtil {
    public void resart(String url, String fileName) {
        ProcessBuilder pb = new ProcessBuilder("./" + "restart.sh", fileName);
        pb.directory(new File(url));
        int runningStatus = 0;
        String s = null;
        try {
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while ((s = stdInput.readLine()) != null) {
                log.info(s);
            }
            while ((s = stdError.readLine()) != null) {
                log.info(s);
            }
            runningStatus = p.waitFor();
        } catch (InterruptedException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }
    /**
     * 视频转码
     * @param ffmpegPath    转码工具的存放路径
     * @param codcFilePath    格式转换后的的文件保存路径
     * @param cameraInfoDTO    流相关信息
     * @return
     * @throws Exception
     * demo：ffmpeg -f rtsp -rtsp_transport tcp -i "rtsp://admin:a1234567@10.172.110.10:554/Streaming/Channels/101?transportmode=unicast" -strict -2 -c:v libx264 -vsync 2 -c:a aac -f hls -hls_time 4 -hls_list_size 5 -t 00:05:00 -hls_wrap 10 /usr/local/nginx/temp/hls/test.m3u8
     * 参数详解
     * -f  rtsp  指定格式(音频或视频格式)
     * -rtsp_transport tcp:指明传输方式是tcp方式(也可以是udp)
     * -i 输入视频文件 可以有效防止丢包
     * -strict -2 之前是实验参数表示 aac音频编码 如果不使用aac音频编码使用使其的编码好像还需要导入第三方的音频编码库 比较麻烦 使用FFmpeg自带的aac音频编码要带上-strict -2 参数就可以了。带这个参数是为了使用aac音频编码
     * -c:v 输出视频格式
     * -c:a 输出音频格式
     * -f hls 输出视频为HTTP Live Stream(M3U8)
     * -hls_time 设置每片的长度，默认为2，单位为秒
     * -hls_list_size 设置播放列表保存的最多条目，设置为0会保存所有信息，默认为5
     * -hls_wrap 设置多少片之后开始覆盖，如果设置为0则不会覆盖，默认值为0。这个选项能够避免在磁盘上存储过多的片，而且能够限制写入磁盘的最多片的数量。
     *
     * -t 30：指明我录制30秒 或者 00:00:30
     * -y 覆盖已存在的文件
     */
    public static boolean toM3u8(String ffmpegPath, String codcFilePath, CameraInfoDTO cameraInfoDTO) throws Exception {
        // 创建一个List集合来保存转换视频文件为flv格式的命令
        List<String> convert = new ArrayList<String>();
        convert.add(ffmpegPath); // 添加转换工具路径
        convert.add("-f");//指定格式(音频或视频格式)
        convert.add("rtsp");
        convert.add("-rtsp_transport");//tcp:指明传输方式是tcp方式(也可以是udp)
        convert.add("tcp");
        convert.add("-t");
        convert.add(cameraInfoDTO.getTimes());
        convert.add("-i"); // 添加参数＂-i＂，该参数指定要转换的文件
        convert.add(cameraInfoDTO.getUrl()); // 添加要转换格式的视频文件的路径
        convert.add("-strict");
        convert.add("-2");
        convert.add("-c:v");
        convert.add("libx264");
        convert.add("-vsync");
        convert.add("2");
        convert.add("-c:a");
        convert.add("aac");
        convert.add("-f");
        convert.add("hls");
        convert.add("-hls_time");
        convert.add("4");
        convert.add("-hls_list_size");
        convert.add("5");
        convert.add("-hls_wrap");
        convert.add("10");
        convert.add("-y"); // 添加参数＂-y＂，该参数指定将覆盖已存在的文件
        convert.add(codcFilePath);

        boolean mark = true;
        ProcessBuilder builder = new ProcessBuilder();
        try {
            log.info("命令为：{}",convert);
            builder.command(convert);
            builder.redirectErrorStream(true);
            // 如果此属性为 true，则任何由通过此对象的 start() 方法启动的后续子进程生成的错误输出都将与标准输出合并，
            //因此两者均可使用 Process.getInputStream() 方法读取。这使得关联错误消息和相应的输出变得更容易
            Process process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader inst = new InputStreamReader(is, "GBK");
            BufferedReader br = new BufferedReader(inst);//输入流缓冲区
            String res = null;
            StringBuilder sb = new StringBuilder();
            while ((res = br.readLine()) != null) {//循环读取缓冲区中的数据
                sb.append(res+"\n");
            }
            log.info("转码后的结果为：{}",sb.toString());
            is.close();
        } catch (Exception e) {
            mark = false;
            log.error("转码出现异常：{}，详细信息为：{}",e.getMessage(),e);
            System.out.println(e);
            e.printStackTrace();
        }
        return mark;
    }

    /*public static void main(String orgs[]) throws Exception {
        toM3u8("ffmpeg", "rtsp://admin:a1234567@10.172.110.81:10081/Streaming/Channels/101?transportmode=unicast", "C:\\images\\201907\\1111.flv");
    }*/
}
