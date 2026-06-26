package com.wn.service.impl.tts;

import com.wn.entity.R;
import com.wn.service.tts.TtsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class TTSServiceImpl implements TtsService {
    @Value("${tts.baidu.api-key:}")
    private String apiKey;
    @Value("${tts.baidu.secret-key:}")
    private String secretKey;

    private String token;
    private long tokenExpire;

    /**
     * 文本转语音
     */
    @Override
    public R synthesize(Map<String, Object> body) {
        String text = (String) body.getOrDefault("text", "");
        int voice = (int) body.getOrDefault("voice", 0);
        int speed = (int) body.getOrDefault("speed", 5);
        int volume = (int) body.getOrDefault("volume", 5);

        String audio = textToVoice(text, voice, speed, volume);
        if (audio != null) {
            return new R(Map.of("audio", audio, "text", text));
        }
        return new R(500, "TTS合成失败，请检查API配置");
    }

    /**
     * 文本转语音
     * @param text 要转换的文本内容
     * @param voice 语音类型
     * @param speed 语速
     * @param volume 音量大小
     * @return 语音数据
     */
    private String textToVoice(String text, int voice, int speed, int volume) {
        if (apiKey.isEmpty()) return null;

        try {
            String tok = getToken();
            String params = "lan=zh&ctp=1&cuid=jubensha&tok=" + tok
                    + "&tex=" + URLEncoder.encode(text, StandardCharsets.UTF_8)
                    + "&vol=" + volume + "&spd=" + speed + "&pit=5&per=" + voice + "&aue=3";

            URL url = new URI("http://tsn.baidu.com/text2audio?" + params).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "audio/mp3");
            conn.connect();

            String ct = conn.getContentType();
            if (ct != null && ct.contains("audio")) {
                byte[] audio = conn.getInputStream().readAllBytes();
                return "data:audio/mp3;base64," + Base64.getEncoder().encodeToString(audio);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取TTS API的access_token
     */
    private String getToken() throws Exception {
        if (token != null && System.currentTimeMillis() < tokenExpire) return token;

        String authUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials"
                + "&client_id=" + apiKey + "&client_secret=" + secretKey;
        URL url = new URI(authUrl).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String resp = new String(conn.getInputStream().readAllBytes());

        token = resp.replaceAll(".*\"access_token\":\"([^\"]+)\".*", "$1");
        tokenExpire = System.currentTimeMillis() + 29 * 24 * 3600 * 1000L;
        return token;
    }
}
