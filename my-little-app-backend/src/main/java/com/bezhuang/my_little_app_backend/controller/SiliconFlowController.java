package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.service.SiliconFlowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 硅基流动 AI 控制器
 * 默认模型，无需登录
 */
@RestController
@RequestMapping("/api/ai/siliconflow")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SiliconFlowController {

    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowController.class);

    private final SiliconFlowService siliconFlowService;

    public SiliconFlowController(SiliconFlowService siliconFlowService) {
        this.siliconFlowService = siliconFlowService;
    }

    /**
     * 同步聊天接口
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> request) {
        String userMessage = (String) request.getOrDefault("message", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) request.get("history");
        Boolean enableDeepThink = (Boolean) request.getOrDefault("enableDeepThink", false);
        Boolean enableWebSearch = (Boolean) request.getOrDefault("enableWebSearch", false);

        logger.info("硅基流动同步聊天请求: message={}, enableDeepThink={}, enableWebSearch={}", userMessage, enableDeepThink, enableWebSearch);

        return siliconFlowService.chat(userMessage, history, enableDeepThink, enableWebSearch);
    }

    /**
     * 流式聊天接口 (SSE)
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestBody Map<String, Object> request,
            ServerHttpResponse response) {

        String userMessage = (String) request.getOrDefault("message", "");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) request.get("history");
        Boolean enableDeepThink = (Boolean) request.getOrDefault("enableDeepThink", false);
        Boolean enableWebSearch = (Boolean) request.getOrDefault("enableWebSearch", false);

        logger.info("硅基流动流式聊天请求: message={}, enableDeepThink={}, enableWebSearch={}", userMessage, enableDeepThink, enableWebSearch);

        SseEmitter emitter = new SseEmitter(300000L);
        siliconFlowService.chatStream(userMessage, history, emitter, enableDeepThink, enableWebSearch);
        return emitter;
    }
}
