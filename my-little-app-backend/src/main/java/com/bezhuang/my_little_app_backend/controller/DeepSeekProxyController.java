package com.bezhuang.my_little_app_backend.controller;

import com.bezhuang.my_little_app_backend.config.ai.DeepSeekConfig;
import com.bezhuang.my_little_app_backend.config.security.CustomUserDetails;
import com.bezhuang.my_little_app_backend.entity.AiConfig;
import com.bezhuang.my_little_app_backend.entity.ApiUsage;
import com.bezhuang.my_little_app_backend.mapper.AiConfigMapper;
import com.bezhuang.my_little_app_backend.service.ApiUsageService;
import com.bezhuang.my_little_app_backend.service.ToolService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * DeepSeek API 代理控制器
 * 支持深度思考模式和工具调用（Function Calling）
 */
@RestController
@RequestMapping("/api/ai")
public class DeepSeekProxyController {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekProxyController.class);
    private static final int MAX_CONVERSATION_ROUNDS = 10;
    private static final int MAX_TOOL_CALLS = 5;

    private final DeepSeekConfig deepSeekConfig;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AiConfigMapper aiConfigMapper;
    private final ToolService toolService;
    private final ApiUsageService apiUsageService;

    public DeepSeekProxyController(DeepSeekConfig deepSeekConfig, ObjectMapper objectMapper,
                                   AiConfigMapper aiConfigMapper, ToolService toolService,
                                   ApiUsageService apiUsageService) {
        this.deepSeekConfig = deepSeekConfig;
        this.objectMapper = objectMapper;
        this.aiConfigMapper = aiConfigMapper;
        this.toolService = toolService;
        this.apiUsageService = apiUsageService;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekConfig.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", "text/event-stream")
                .build();
    }

    /**
     * 流式发送消息（支持深度思考和工具调用）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        logger.info("========== SSE 流式请求开始 ==========");
        logger.info("请求路径: /api/ai/chat/stream");

        if (userDetails == null) {
            logger.warn("SSE 请求未登录");
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\": \"请先登录\"}"));
                emitter.complete();
            } catch (IOException e) {
                logger.error("发送未登录错误失败", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        Boolean enableDeepThink = request.get("enableDeepThink") != null &&
                Boolean.parseBoolean(request.get("enableDeepThink").toString());
        Boolean enableWebSearch = request.get("enableWebSearch") != null &&
                Boolean.parseBoolean(request.get("enableWebSearch").toString());

        logger.info("用户ID: {}, 消息数: {}, 深度思考: {}, 联网搜索: {}",
                userDetails.getId(), messages != null ? messages.size() : 0, enableDeepThink, enableWebSearch);

        if (messages == null || messages.isEmpty()) {
            logger.warn("SSE 请求消息为空");
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\": \"消息不能为空\"}"));
                emitter.complete();
            } catch (IOException e) {
                logger.error("发送空消息错误失败", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 计算对话轮数
        int userMessageCount = 0;
        for (Map<String, String> msg : messages) {
            if ("user".equals(msg.get("role"))) {
                userMessageCount++;
            }
        }

        logger.info("对话轮数: {}/{}", userMessageCount, MAX_CONVERSATION_ROUNDS);

        if (userMessageCount >= MAX_CONVERSATION_ROUNDS) {
            logger.warn("用户 {} 达到对话轮数上限: {}", userDetails.getId(), MAX_CONVERSATION_ROUNDS);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("warning")
                        .data("{\"warning\": \"已达10轮对话上限，请点击右上角+号开启新对话\"}"));
                emitter.complete();
                logger.info("已发送对话轮数上限警告");
            } catch (IOException e) {
                logger.error("发送对话轮数警告失败", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // 检查配额（启用联网搜索时才检查搜索次数）
        String quotaWarning = apiUsageService.getQuotaWarning(userDetails.getId(), enableWebSearch);
        if (quotaWarning != null) {
            logger.warn("用户 {} 配额不足: {}", userDetails.getId(), quotaWarning);
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("warning")
                        .data("{\"warning\": " + escapeJson(quotaWarning) + "}"));
                emitter.complete();
                logger.info("已发送配额不足警告");
            } catch (IOException e) {
                logger.error("发送配额警告失败", e);
                emitter.completeWithError(e);
            }
            return emitter;
        }

        logger.info("SSE 连接建立成功，准备发送流式响应");

        SseEmitter emitter = new SseEmitter(300000L);

        // 心跳定时器
        ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
        Runnable heartbeatTask = () -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data(": keepalive\n\n"));
            } catch (IOException e) {
                logger.debug("心跳发送失败，连接可能已关闭");
            }
        };
        heartbeatScheduler.scheduleAtFixedRate(heartbeatTask, 30, 30, TimeUnit.SECONDS);

        emitter.onTimeout(() -> {
            heartbeatScheduler.shutdown();
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\": \"请求超时\"}"));
            } catch (IOException e) {
                logger.error("发送超时错误失败", e);
            }
            emitter.complete();
        });

        emitter.onError(e -> {
            heartbeatScheduler.shutdown();
            logger.error("SSE 连接错误: {}", e.getMessage());
        });

        try {
            // 构建消息列表
            List<Map<String, Object>> messagesToSend = new ArrayList<>();
            String systemPrompt = getSystemPrompt(enableWebSearch);
            messagesToSend.add(Map.of("role", "system", "content", systemPrompt));

            for (Map<String, String> msg : messages) {
                messagesToSend.add(new HashMap<>(msg));
            }

            logger.info("发送 SSE start 事件");
            emitter.send(SseEmitter.event().name("start").data("{\"type\": \"start\"}"));

            // 执行工具调用流程（AI 自主决定是否调用工具）
            logger.info("开始执行工具调用流程");
            executeWithToolCalls(messagesToSend, enableDeepThink, enableWebSearch, emitter,
                    heartbeatScheduler, userDetails.getId());

        } catch (Exception e) {
            logger.error("调用 DeepSeek API 失败: {}", e.getMessage(), e);
            try {
                logger.info("发送 SSE error 事件");
                emitter.send(SseEmitter.event().name("error")
                        .data("{\"error\": " + escapeJson(e.getMessage()) + "}"));
            } catch (IOException ioException) {
                logger.error("发送 SSE 错误事件失败", ioException);
                emitter.completeWithError(ioException);
            }
            emitter.completeWithError(e);
            logger.info("SSE 流式请求异常结束");
        }

        return emitter;
    }

    /**
     * 获取用户API配额信息
     */
    @GetMapping("/quota")
    public Map<String, Object> getQuota(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return Map.of("success", false, "message", "请先登录");
        }

        ApiUsage usage = apiUsageService.getUserQuota(userDetails.getId());
        String warning = apiUsageService.getQuotaWarning(userDetails.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("tokensRemaining", usage.getTokensRemaining() != null ? usage.getTokensRemaining() : 0);
        result.put("searchRemaining", usage.getSearchRemaining() != null ? usage.getSearchRemaining() : 0);

        if (warning != null) {
            result.put("warning", warning);
        }

        return result;
    }

    /**
     * 同步发送消息（支持深度思考和工具调用）
     */
    @PostMapping("/chat")
    public Map<String, Object> chat(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return Map.of("success", false, "message", "请先登录");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        Boolean enableDeepThink = request.get("enableDeepThink") != null &&
                Boolean.parseBoolean(request.get("enableDeepThink").toString());
        Boolean enableWebSearch = request.get("enableWebSearch") != null &&
                Boolean.parseBoolean(request.get("enableWebSearch").toString());

        if (messages == null || messages.isEmpty()) {
            return Map.of("success", false, "message", "消息不能为空");
        }

        // 计算对话轮数
        int userMessageCount = 0;
        for (Map<String, String> msg : messages) {
            if ("user".equals(msg.get("role"))) {
                userMessageCount++;
            }
        }

        if (userMessageCount >= MAX_CONVERSATION_ROUNDS) {
            return Map.of("success", false, "warning", true, "message", "已达10轮对话上限，请开启新对话");
        }

        // 检查配额（启用联网搜索时才检查搜索次数）
        String quotaWarning = apiUsageService.getQuotaWarning(userDetails.getId(), enableWebSearch);
        if (quotaWarning != null) {
            return Map.of("success", false, "warning", true, "message", quotaWarning);
        }

        logger.info("========== 开始新的聊天请求 ==========");
        logger.info("用户ID: {}, 消息数: {}, 深度思考: {}, 联网搜索: {}",
                userDetails.getId(), userMessageCount, enableDeepThink, enableWebSearch);

        try {
            // 构建消息列表
            List<Map<String, Object>> messagesToSend = new ArrayList<>();
            String systemPrompt = getSystemPrompt(enableWebSearch);
            messagesToSend.add(Map.of("role", "system", "content", systemPrompt));

            for (Map<String, String> msg : messages) {
                messagesToSend.add(new HashMap<>(msg));
            }

            // 执行工具调用（AI 自主决定是否调用工具）
            ToolCallResult result = executeToolCallsSync(messagesToSend, enableDeepThink, enableWebSearch,
                    userDetails.getId());

            // 消耗Token配额
            if (result.inputTokens > 0 || result.outputTokens > 0) {
                boolean consumed = apiUsageService.consumeTokens(userDetails.getId(), result.inputTokens, result.outputTokens);
                if (!consumed) {
                    // Token不足，返回错误（不返回AI响应）
                    logger.warn("用户 {} Token不足，阻止响应", userDetails.getId());
                    return Map.of("success", false, "warning", true, "message", "Token不足，请联系管理员充值");
                }
                logger.info("用户 {} 消耗 tokens: 输入 {}, 输出 {}", userDetails.getId(), result.inputTokens, result.outputTokens);
            }

            // 获取更新后的配额
            ApiUsage usage = apiUsageService.getUserQuota(userDetails.getId());
            String newWarning = apiUsageService.getQuotaWarning(userDetails.getId());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("response", result.response);
            response.put("thinking", result.thinking);
            response.put("searchLinks", result.searchLinks);
            response.put("tokensRemaining", usage.getTokensRemaining() != null ? usage.getTokensRemaining() : 0);
            response.put("searchRemaining", usage.getSearchRemaining() != null ? usage.getSearchRemaining() : 0);
            if (newWarning != null) {
                response.put("warning", newWarning);
            }

            return response;

        } catch (Exception e) {
            logger.error("调用 DeepSeek API 失败", e);
            return Map.of("success", false, "message", "AI 响应失败: " + e.getMessage());
        }
    }

    /**
     * 工具调用结果类
     */
    private static class ToolCallResult {
        String response;
        String thinking;
        long inputTokens;
        long outputTokens;
        int searchUsed;
        List<Map<String, String>> searchLinks;

        ToolCallResult(String response, String thinking, long inputTokens, long outputTokens, int searchUsed, List<Map<String, String>> searchLinks) {
            this.response = response;
            this.thinking = thinking;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.searchUsed = searchUsed;
            this.searchLinks = searchLinks != null ? searchLinks : new ArrayList<>();
        }
    }

    /**
     * 同步执行工具调用流程（支持多轮思考 Chain of Thoughts）
     */
    private ToolCallResult executeToolCallsSync(List<Map<String, Object>> messages, boolean enableDeepThink,
                                                boolean enableWebSearch, Long userId)
            throws JsonProcessingException {
        String chatModel = deepSeekConfig.getModel();
        String reasonerModel = deepSeekConfig.getReasonerModel();
        List<String> allThinking = new ArrayList<>(); // 收集所有思考过程
        int toolCallCount = 0;
        int totalSearchUsed = 0;
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        List<Map<String, String>> allSearchLinks = new ArrayList<>(); // 收集所有搜索链接（包含标题）

        logger.info("开始执行工具调用流程，当前消息数: {}", messages.size());

        while (toolCallCount < MAX_TOOL_CALLS) {
            // 选择模型：启用深度思考时每轮都使用 reasonerModel
            String currentModel = enableDeepThink ? reasonerModel : chatModel;

            // 传递工具定义
            Map<String, Object> requestBody = buildRequestBody(currentModel, messages, true, enableWebSearch);
            String response = callDeepSeekApi(requestBody);

            JsonNode root = objectMapper.readTree(response);

            // 统计Token使用量
            JsonNode usage = root.path("usage");
            long inputTokens = usage.path("prompt_tokens").asLong(0);
            long outputTokens = usage.path("completion_tokens").asLong(0);
            totalInputTokens += inputTokens;
            totalOutputTokens += outputTokens;

            JsonNode choice = root.path("choices").get(0);
            JsonNode message = choice.path("message");

            logger.info("第 {} 轮响应: content={}, tool_calls present={}",
                    toolCallCount + 1,
                    message.path("content").asText("").substring(0, Math.min(100, message.path("content").asText("").length())),
                    message.has("tool_calls"));

            // 获取思考过程（每轮都获取，最多500字）
            if (enableDeepThink && message.has("reasoning_content")) {
                JsonNode reasoning = message.path("reasoning_content");
                if (!reasoning.isNull() && !reasoning.asText().isEmpty()) {
                    String roundThinking = reasoning.asText().trim();
                    if (!roundThinking.isEmpty()) {
                        // 限制每轮思考不超过500字
                        if (roundThinking.length() > 500) {
                            roundThinking = roundThinking.substring(0, 500) + "...";
                        }
                        allThinking.add("=== 第 " + (toolCallCount + 1) + " 轮思考 ===\n" + roundThinking);
                        logger.info("第 {} 轮思考过程长度: {}", toolCallCount + 1, roundThinking.length());
                    }
                } else {
                    logger.info("第 {} 轮 reasoning_content 为空", toolCallCount + 1);
                }
            } else {
                logger.info("第 {} 轮无 reasoning_content 字段", toolCallCount + 1);
            }

            // 检查是否有工具调用
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && toolCalls.size() > 0) {
                logger.info("检测到工具调用，数量: {}", toolCalls.size());

                // 获取当前轮的 reasoning_content（如果有的话）
                String reasoningContent = "";
                if (message.has("reasoning_content") && !message.path("reasoning_content").isNull()) {
                    reasoningContent = message.path("reasoning_content").asText("");
                }

                // 添加 assistant 消息（包含 tool_calls 和 reasoning_content）
                Map<String, Object> assistantMessage = new LinkedHashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", message.path("content").asText(""));
                assistantMessage.put("tool_calls", objectMapper.readValue(toolCalls.toString(), List.class));
                // DeepSeek reasoner 模型要求：包含 tool_calls 的消息必须也有 reasoning_content
                // 必须包含该字段，即使为空
                assistantMessage.put("reasoning_content", reasoningContent);
                messages.add(assistantMessage);

                // 执行工具调用
                for (JsonNode toolCall : toolCalls) {
                    String toolName = toolCall.path("function").path("name").asText();
                    String arguments = toolCall.path("function").path("arguments").asText();

                    logger.info("执行工具调用: {}, 参数: {}", toolName, arguments);

                    // 如果是搜索工具，使用新方法获取链接
                    String toolResult;
                    if ("web_search".equals(toolName) && enableWebSearch) {
                        ToolService.WebSearchResult searchResult = toolService.executeWebSearchTool(toolName, arguments);
                        toolResult = searchResult.getContent();
                        allSearchLinks.addAll(searchResult.getLinks());
                        logger.info("搜索工具返回链接数: {}", searchResult.getLinks().size());
                    } else {
                        toolResult = toolService.executeTool(toolName, arguments);
                    }

                    // 统计搜索使用次数
                    if ("web_search".equals(toolName)) {
                        boolean consumed = apiUsageService.consumeSearch(userId);
                        if (consumed) {
                            totalSearchUsed++;
                            logger.info("用户 {} 消耗1次搜索配额", userId);
                        } else {
                            logger.warn("用户 {} 搜索次数已用尽", userId);
                            toolResult = "搜索次数已用尽，请联系管理员充值。";
                        }
                    }

                    logger.info("工具 {} 返回结果长度: {}", toolName, toolResult.length());

                    Map<String, Object> toolMessage = new LinkedHashMap<>();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCall.path("id").asText());
                    toolMessage.put("name", toolName);
                    toolMessage.put("content", toolResult.isEmpty() ? "工具调用失败或返回空结果" : toolResult);
                    messages.add(toolMessage);

                    if (!toolResult.isEmpty()) {
                        String truncatedResult = toolResult.length() > 200
                            ? toolResult.substring(0, 200) + "..."
                            : toolResult;
                        logger.debug("工具 {} 返回结果: {}", toolName, truncatedResult);
                    } else {
                        logger.warn("工具 {} 返回空结果", toolName);
                    }
                }

                toolCallCount++;
                logger.info("工具调用轮次: {}/{}", toolCallCount, MAX_TOOL_CALLS);
                continue;
            }

            // 没有工具调用，返回结果
            String content = message.path("content").asText("");
            logger.info("无工具调用，返回内容长度: {}", content.length());

            // 注意：reasoning_content 已经在上面（第401-419行）添加过了，不需要重复添加

            // 合并所有思考过程
            String thinking = String.join("\n\n", allThinking);
            logger.info("共收集到 {} 轮思考，总长度: {}", allThinking.size(), thinking.length());

            // 如果没有工具调用，content 可能包含思考内容，需要提取最终回复
            if (!message.has("tool_calls") || message.path("tool_calls").isEmpty()) {
                // 检查 content 是否以"第X轮思考"开头且包含"最终"关键词
                // 如果是，说明 content 包含思考内容，需要提取最终回复部分
                if (content.contains("最终") || content.contains("总结") || content.contains("所以")) {
                    // 尝试提取最终回复（通常在"最终"、"总结"、"所以"等关键词之后）
                    int finalStart = Math.max(
                        content.lastIndexOf("最终"),
                        Math.max(
                            content.lastIndexOf("总结"),
                            content.lastIndexOf("所以")
                        )
                    );
                    if (finalStart > 0 && finalStart > content.length() / 2) {
                        // 只保留最终回复部分（去掉前面的思考过程）
                        String finalAnswer = content.substring(finalStart).trim();
                        // 如果最终回复太短，可能是"最终解决方案："这样的格式
                        if (finalAnswer.length() < 50) {
                            // 找到冒号后的内容
                            int colonIndex = finalAnswer.indexOf("：");
                            if (colonIndex > 0 && colonIndex < finalAnswer.length() - 1) {
                                finalAnswer = finalAnswer.substring(colonIndex + 1).trim();
                            }
                        }
                        // 如果提取的内容合理，使用它作为最终回复
                        if (finalAnswer.length() > 20) {
                            content = finalAnswer;
                            logger.info("提取最终回复，长度: {}", content.length());
                        }
                    }
                }
            }

            // 消耗Token配额
            if (userId != null) {
                apiUsageService.consumeTokens(userId, totalInputTokens, totalOutputTokens);
                logger.info("Token使用：输入 {}, 输出 {}", totalInputTokens, totalOutputTokens);
                logger.info("联网搜索使用：{} 次", totalSearchUsed);
            }

            return new ToolCallResult(content, thinking, totalInputTokens, totalOutputTokens, totalSearchUsed, allSearchLinks);
        }

        logger.warn("工具调用次数达到上限: {}", MAX_TOOL_CALLS);
        // 即使达到上限也消耗当前轮的token
        if (userId != null) {
            apiUsageService.consumeTokens(userId, totalInputTokens, totalOutputTokens);
        }
        String thinking = String.join("\n\n", allThinking);
        return new ToolCallResult("工具调用次数过多，请重试", thinking, totalInputTokens, totalOutputTokens, totalSearchUsed, allSearchLinks);
    }

    /**
     * 流式执行工具调用流程（支持多轮思考 Chain of Thoughts）
     * 使用同步 API 调用 DeepSeek，前端通过打字机效果逐字显示
     */
    private void executeWithToolCalls(List<Map<String, Object>> messages, boolean enableDeepThink,
                                      boolean enableWebSearch, SseEmitter emitter,
                                      ScheduledExecutorService heartbeatScheduler, Long userId) {
        String chatModel = deepSeekConfig.getModel();
        String reasonerModel = deepSeekConfig.getReasonerModel();
        List<String> allThinking = new ArrayList<>(); // 收集所有思考过程
        int toolCallCount = 0;
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        int totalSearchUsed = 0;
        List<Map<String, String>> allSearchLinks = new ArrayList<>(); // 收集所有搜索链接（包含标题）

        logger.info("深度思考模式: {}", enableDeepThink);
        logger.info("开始执行工具调用流程，当前消息数: {}", messages.size());

        while (toolCallCount < MAX_TOOL_CALLS) {
            try {
                // 选择模型：启用深度思考时每轮都使用 reasonerModel
                String currentModel = enableDeepThink ? reasonerModel : chatModel;

                // 传递工具定义
                Map<String, Object> requestBody = buildRequestBody(currentModel, messages, true, enableWebSearch);

                // 同步调用 DeepSeek API
                String response = callDeepSeekApi(requestBody);
                JsonNode root = objectMapper.readTree(response);

                // 统计Token使用量
                JsonNode usage = root.path("usage");
                long inputTokens = usage.path("prompt_tokens").asLong(0);
                long outputTokens = usage.path("completion_tokens").asLong(0);
                totalInputTokens += inputTokens;
                totalOutputTokens += outputTokens;

                JsonNode choice = root.path("choices").get(0);
                JsonNode message = choice.path("message");

                logger.info("第 {} 轮响应: tool_calls present={}", toolCallCount + 1, message.has("tool_calls"));

                // 获取思考过程（每轮都获取，最多500字）
                if (enableDeepThink && message.has("reasoning_content")) {
                    JsonNode reasoning = message.path("reasoning_content");
                    if (!reasoning.isNull() && !reasoning.asText().isEmpty()) {
                        String roundThinking = reasoning.asText().trim();
                        if (!roundThinking.isEmpty()) {
                            // 限制每轮思考不超过500字
                            if (roundThinking.length() > 500) {
                                roundThinking = roundThinking.substring(0, 500) + "...";
                            }
                            allThinking.add("=== 第 " + (toolCallCount + 1) + " 轮思考 ===\n" + roundThinking);
                        }
                    }
                }

                // 检查工具调用
                JsonNode toolCalls = message.path("tool_calls");
                if (toolCalls.isArray() && toolCalls.size() > 0) {
                    logger.info("检测到工具调用，数量: {}", toolCalls.size());

                    // 获取当前轮的 reasoning_content（如果有的话）
                    String reasoningContent = "";
                    if (message.has("reasoning_content") && !message.path("reasoning_content").isNull()) {
                        reasoningContent = message.path("reasoning_content").asText("");
                    }

                    // 添加 assistant 消息（包含 tool_calls 和 reasoning_content）
                    Map<String, Object> assistantMessage = new LinkedHashMap<>();
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", message.path("content").asText(""));
                    assistantMessage.put("tool_calls", objectMapper.readValue(toolCalls.toString(), List.class));
                    // DeepSeek reasoner 模型要求：包含 tool_calls 的消息必须也有 reasoning_content
                    // 必须包含该字段，即使为空
                    assistantMessage.put("reasoning_content", reasoningContent);
                    messages.add(assistantMessage);

                    // 执行工具调用
                    for (JsonNode toolCall : toolCalls) {
                        String toolName = toolCall.path("function").path("name").asText();
                        String arguments = toolCall.path("function").path("arguments").asText();

                        logger.info("执行工具调用: {}, 参数: {}", toolName, arguments);

                        // 如果是搜索工具，使用新方法获取链接
                        String toolResult;
                        if ("web_search".equals(toolName) && enableWebSearch) {
                            ToolService.WebSearchResult searchResult = toolService.executeWebSearchTool(toolName, arguments);
                            toolResult = searchResult.getContent();
                            allSearchLinks.addAll(searchResult.getLinks());
                            logger.info("搜索工具返回链接数: {}", searchResult.getLinks().size());
                        } else {
                            toolResult = toolService.executeTool(toolName, arguments);
                        }

                        // 统计搜索使用次数
                        if ("web_search".equals(toolName)) {
                            boolean consumed = apiUsageService.consumeSearch(userId);
                            if (!consumed) {
                                logger.warn("用户 {} 搜索次数已用尽", userId);
                                toolResult = "搜索次数已用尽，请联系管理员充值。";
                            } else {
                                totalSearchUsed++;
                            }
                        }

                        logger.info("工具 {} 返回结果长度: {}", toolName, toolResult.length());

                        Map<String, Object> toolMessage = new LinkedHashMap<>();
                        toolMessage.put("role", "tool");
                        toolMessage.put("tool_call_id", toolCall.path("id").asText());
                        toolMessage.put("name", toolName);
                        toolMessage.put("content", toolResult.isEmpty() ? "工具调用失败" : toolResult);
                        messages.add(toolMessage);

                        if (!toolResult.isEmpty()) {
                            String truncatedResult = toolResult.length() > 200
                                ? toolResult.substring(0, 200) + "..."
                                : toolResult;
                            logger.debug("工具 {} 返回结果: {}", toolName, truncatedResult);
                        } else {
                            logger.warn("工具 {} 返回空结果", toolName);
                        }
                    }

                    toolCallCount++;
                    logger.info("工具调用轮次: {}/{}", toolCallCount, MAX_TOOL_CALLS);
                    continue;
                }

                // 没有工具调用，发送内容
                String content = message.path("content").asText("");
                logger.info("无工具调用，返回内容长度: {}", content.length());

                // 如果有 reasoning_content，将其添加到思考列表
                if (enableDeepThink && message.has("reasoning_content")) {
                    JsonNode reasoning = message.path("reasoning_content");
                    if (!reasoning.isNull() && !reasoning.asText().isEmpty()) {
                        String roundThinking = reasoning.asText().trim();
                        if (!roundThinking.isEmpty()) {
                            if (roundThinking.length() > 500) {
                                roundThinking = roundThinking.substring(0, 500) + "...";
                            }
                            allThinking.add("=== 第 " + (toolCallCount + 1) + " 轮思考 ===\n" + roundThinking);
                            logger.info("第 {} 轮思考过程长度: {}", toolCallCount + 1, roundThinking.length());
                        }
                    }
                }

                // 发送思考过程（合并所有轮次的思考）
                String thinking = String.join("\n\n", allThinking);
                if (enableDeepThink && !thinking.isEmpty()) {
                    try {
                        logger.info("发送 SSE reasoning 事件，内容长度: {}", thinking.length());
                        emitter.send(SseEmitter.event().name("reasoning")
                                .data("{\"reasoning\": " + escapeJson(thinking) + "}"));
                    } catch (IOException e) {
                        logger.error("发送 reasoning 失败", e);
                    }
                }

                // 如果没有工具调用，content 可能包含思考内容，需要提取最终回复
                if (content.contains("最终") || content.contains("总结") || content.contains("所以")) {
                    int finalStart = Math.max(
                        content.lastIndexOf("最终"),
                        Math.max(
                            content.lastIndexOf("总结"),
                            content.lastIndexOf("所以")
                        )
                    );
                    if (finalStart > 0 && finalStart > content.length() / 2) {
                        String finalAnswer = content.substring(finalStart).trim();
                        if (finalAnswer.length() < 50) {
                            int colonIndex = finalAnswer.indexOf("：");
                            if (colonIndex > 0 && colonIndex < finalAnswer.length() - 1) {
                                finalAnswer = finalAnswer.substring(colonIndex + 1).trim();
                            }
                        }
                        if (finalAnswer.length() > 20) {
                            content = finalAnswer;
                            logger.info("提取最终回复，长度: {}", content.length());
                        }
                    }
                }

                // 发送完整内容（前端打字机效果逐字显示）
                if (!content.isEmpty()) {
                    try {
                        logger.info("发送 SSE token 事件，内容长度: {}", content.length());
                        emitter.send(SseEmitter.event().name("token")
                                .data("{\"token\": " + escapeJson(content) + "}"));
                    } catch (IOException e) {
                        logger.error("发送 token 失败", e);
                    }
                }

                // 消耗Token配额
                if (userId != null) {
                    boolean consumed = apiUsageService.consumeTokens(userId, totalInputTokens, totalOutputTokens);
                    if (!consumed) {
                        logger.warn("用户 {} Token不足，关闭连接", userId);
                        try {
                            emitter.send(SseEmitter.event().name("error")
                                    .data("{\"error\": \"Token不足，请联系管理员充值\"}"));
                            emitter.completeWithError(new RuntimeException("Token不足"));
                        } catch (IOException ex) {
                            logger.error("发送Token不足错误失败", ex);
                        }
                        heartbeatScheduler.shutdown();
                        return;
                    }
                    logger.info("Token使用：输入 {}, 输出 {}", totalInputTokens, totalOutputTokens);
                    logger.info("联网搜索使用：{} 次", totalSearchUsed);

                    // 发送配额信息
                    ApiUsage userQuota = apiUsageService.getUserQuota(userId);
                    try {
                        String quotaData = String.format("{\"quota\":{\"tokensRemaining\":%d,\"searchRemaining\":%d}}",
                                userQuota.getTokensRemaining() != null ? userQuota.getTokensRemaining() : 0,
                                userQuota.getSearchRemaining() != null ? userQuota.getSearchRemaining() : 0);
                        logger.info("发送 SSE quota 事件: tokensRemaining={}, searchRemaining={}",
                                userQuota.getTokensRemaining(), userQuota.getSearchRemaining());
                        emitter.send(SseEmitter.event().name("quota").data(quotaData));

                        String warning = apiUsageService.getQuotaWarning(userId, enableWebSearch);
                        if (warning != null) {
                            logger.info("发送 SSE warning 事件: {}", warning);
                            emitter.send(SseEmitter.event().name("warning")
                                    .data("{\"warning\": " + escapeJson(warning) + "}"));
                        }
                    } catch (IOException ex) {
                        logger.error("发送配额信息失败", ex);
                    }
                }

                // 发送搜索链接
                if (!allSearchLinks.isEmpty()) {
                    try {
                        String linksJson = objectMapper.writeValueAsString(allSearchLinks);
                        logger.info("发送 SSE searchLinks 事件，链接数: {}", allSearchLinks.size());
                        emitter.send(SseEmitter.event().name("searchLinks").data("{\"searchLinks\": " + linksJson + "}"));
                    } catch (IOException e) {
                        logger.error("发送搜索链接失败", e);
                    }
                }

                // 完成
                heartbeatScheduler.shutdown();
                try {
                    logger.info("发送 SSE complete 事件");
                    logger.info("========== SSE 流式请求正常结束 ==========");
                    emitter.send(SseEmitter.event().name("complete").data("{\"type\": \"complete\"}"));
                    emitter.complete();
                } catch (IOException e) {
                    logger.error("发送完成信号失败", e);
                }
                return;

            } catch (Exception e) {
                logger.error("工具调用流程错误: {}", e.getMessage(), e);
                try {
                    logger.info("发送 SSE error 事件");
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"error\": " + escapeJson(e.getMessage()) + "}"));
                } catch (IOException ex) {
                    logger.error("发送错误事件失败", ex);
                }
                emitter.completeWithError(e);
                logger.info("========== SSE 流式请求异常结束 ==========");
                return;
            }
        }

        // 工具调用次数过多
        try {
            logger.warn("工具调用次数超过上限: {}", MAX_TOOL_CALLS);
            logger.info("发送 SSE error 事件（工具调用过多）");
            emitter.send(SseEmitter.event().name("error").data("{\"error\": \"工具调用次数过多\"}"));
            emitter.complete();
        } catch (IOException e) {
            logger.error("发送工具调用过多错误失败", e);
            emitter.completeWithError(e);
        }
    }

    /**
     * 构建请求体
     */
    private Map<String, Object> buildRequestBody(String model, List<Map<String, Object>> messages,
                                                  boolean includeTools, boolean enableWebSearch) {
        double temperature = getTemperatureFromDb();
        String systemPrompt = getSystemPrompt(enableWebSearch);

        logger.info("深度思考模式: {}", false);  // 会在调用方设置
        logger.info("System Prompt: {}", systemPrompt);
        logger.info("Temperature: {}", temperature);
        logger.info("选用的模型: {}", model);
        logger.info("Messages count: {}", messages.size());

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        requestBody.put("temperature", temperature);

        // 添加工具定义
        if (includeTools) {
            List<Map<String, Object>> tools = toolService.getToolsDefinition(enableWebSearch);
            if (!tools.isEmpty()) {
                requestBody.put("tools", tools);
                logger.info("开始调用 DeepSeek API...");
                logger.info("联网搜索: {}", enableWebSearch ? "启用" : "关闭");
                logger.info("Tools count: {}", tools.size());
                for (Map<String, Object> tool : tools) {
                    logger.info("Tool: {}", tool.get("function"));
                }
            }
        }

        return requestBody;
    }

    /**
     * 从数据库获取温度配置
     */
    private double getTemperatureFromDb() {
        try {
            AiConfig config = aiConfigMapper.selectByConfigKey("temperature");
            if (config != null && config.getConfigValue() != null) {
                return Double.parseDouble(config.getConfigValue());
            }
        } catch (Exception e) {
            logger.warn("读取温度配置失败，使用默认值: {}", e.getMessage());
        }
        // 默认值 0.7
        return 0.7;
    }

    /**
     * 调用 DeepSeek API
     */
    private String callDeepSeekApi(Map<String, Object> requestBody) throws JsonProcessingException {
        String response = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + deepSeekConfig.getApiKey())
                .header("User-Agent", "Spring-WebClient")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("DeepSeek API 4xx Error: {} - {}", clientResponse.statusCode(), body);
                                    return Mono.error(new RuntimeException("DeepSeek API Error: " + body));
                                })
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("DeepSeek API 5xx Error: {} - {}", clientResponse.statusCode(), body);
                                    return Mono.error(new RuntimeException("DeepSeek Server Error: " + body));
                                })
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(180))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
                        .filter(throwable -> throwable instanceof org.springframework.web.reactive.function.client.WebClientRequestException
                                || throwable instanceof java.net.ConnectException
                                || (throwable.getMessage() != null && throwable.getMessage().contains("Connection"))))
                .block();

        logger.debug("DeepSeek API response: {}", response);
        return response;
    }

    /**
     * 获取系统提示词
     */
    private String getSystemPrompt(boolean enableWebSearch) {
        String basePrompt = "";
        try {
            AiConfig config = aiConfigMapper.selectByConfigKey("system_prompt");
            if (config != null && config.getConfigValue() != null) {
                Object configValue = config.getConfigValue();
                String value;
                if (configValue instanceof byte[]) {
                    value = new String((byte[]) configValue, StandardCharsets.UTF_8);
                } else if (configValue instanceof String) {
                    value = (String) configValue;
                } else {
                    value = configValue.toString();
                }
                value = cleanInvalidChars(value);
                if (value != null && !value.trim().isEmpty() && isValidText(value)) {
                    basePrompt = value;
                }
            }
        } catch (Exception e) {
            logger.warn("读取 System Prompt 失败，使用默认配置", e);
        }

        if (basePrompt.isEmpty()) {
            basePrompt = "你是 Bezhuang AI，一个智能助手。当用户询问实时信息（天气、时间、新闻等）时，务必调用相应的工具获取准确数据，然后用自然的中文回答。";
        }

        // 追加工具使用说明
        String toolsPrompt = toolService.getToolsPrompt(enableWebSearch);
        return basePrompt + "\n\n" + toolsPrompt;
    }

    private String cleanInvalidChars(String value) {
        if (value == null) return null;
        StringBuilder cleaned = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || !Character.isISOControl(c)) {
                cleaned.append(c);
            } else {
                cleaned.append(' ');
            }
        }
        return cleaned.toString();
    }

    private boolean isValidText(String value) {
        if (value == null || value.isEmpty()) return false;
        int readableCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) ||
                isChineseChar(c) || isCommonPunctuation(c)) {
                readableCount++;
            }
        }
        return readableCount >= value.length() * 0.5;
    }

    private boolean isChineseChar(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION;
    }

    private boolean isCommonPunctuation(char c) {
        String punctuation = "，。！？、；：\"''（）【】《》…—· ";
        return punctuation.indexOf(c) >= 0;
    }

    private String escapeJson(String text) {
        if (text == null) return "\"\"";
        return "\"" + text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
