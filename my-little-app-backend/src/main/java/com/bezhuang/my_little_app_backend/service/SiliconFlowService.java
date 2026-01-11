package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.config.ai.SiliconFlowConfig;
import com.bezhuang.my_little_app_backend.entity.AiConfig;
import com.bezhuang.my_little_app_backend.mapper.AiConfigMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

/**
 * 硅基流动 API 服务
 * 默认模型，无需登录，支持工具调用（Function Calling）
 */
@Service
public class SiliconFlowService {

    private static final Logger logger = LoggerFactory.getLogger(SiliconFlowService.class);
    private static final int MAX_CONVERSATION_ROUNDS = 20;
    private static final int MAX_TOOL_CALLS = 3;

    private final SiliconFlowConfig config;
    private final AiConfigMapper aiConfigMapper;
    private final ToolService toolService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public SiliconFlowService(SiliconFlowConfig config, AiConfigMapper aiConfigMapper,
                               ToolService toolService, ObjectMapper objectMapper) {
        this.config = config;
        this.aiConfigMapper = aiConfigMapper;
        this.toolService = toolService;
        this.objectMapper = objectMapper;

        // 创建信任所有证书的 SSL 上下文
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, null);

            this.webClient = WebClient.builder()
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Accept", "text/event-stream")
                    .codecs(configurer -> configurer
                            .defaultCodecs()
                            .maxInMemorySize(10 * 1024 * 1024))
                    .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 连接超时30秒
                            .responseTimeout(Duration.ofSeconds(180)))) // 响应超时180秒
                    .build();
        } catch (Exception e) {
            logger.error("创建 WebClient 失败", e);
            throw new RuntimeException("Failed to create WebClient", e);
        }
    }

    /**
     * 同步聊天（默认模型，无需登录）
     * @param userMessage 用户消息
     * @param history 历史消息
     * @param enableDeepThink 是否启用深度思考
     * @param enableWebSearch 是否启用联网搜索
     */
    public Map<String, Object> chat(String userMessage, List<Map<String, Object>> history,
                                     boolean enableDeepThink, boolean enableWebSearch) {
        logger.info("========== SiliconFlow 聊天请求 ==========");
        logger.info("enableDeepThink: {}, enableWebSearch: {}", enableDeepThink, enableWebSearch);

        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            // 系统提示 - 从数据库获取
            String systemPrompt = getSystemPrompt(enableWebSearch);
            logger.info("System Prompt: {}", systemPrompt);
            messages.add(Map.of(
                    "role", "system",
                    "content", systemPrompt
            ));

            // 历史消息
            if (history != null) {
                messages.addAll(history);
            }

            // 用户消息
            Map<String, Object> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            // 获取温度配置
            double temperature = getTemperatureFromDb();
            logger.info("Temperature: {}", temperature);

            // 使用默认模型
            String selectedModel = config.getModel();
            logger.info("选用的模型: {}", selectedModel);
            logger.info("Messages count: {}", messages.size());

            // 执行工具调用流程
            SiliconFlowToolCallResult result = executeWithToolCalls(messages, selectedModel, temperature, enableDeepThink, enableWebSearch);

            String content = result.response;
            String thinking = result.thinking;
            List<Map<String, String>> searchLinks = result.searchLinks;

            logger.info("响应内容长度: {}, 思考过程长度: {}, 搜索链接数: {}", content.length(), thinking.length(), searchLinks.size());

            Map<String, Object> resultMap = new LinkedHashMap<>();
            resultMap.put("success", true);
            resultMap.put("response", content);
            resultMap.put("thinking", thinking);
            resultMap.put("searchLinks", searchLinks);
            resultMap.put("model", selectedModel);
            // 添加错误标识，用于前端区分错误响应和正常响应
            resultMap.put("isError", result.isError);

            return resultMap;

        } catch (Exception e) {
            logger.error("SiliconFlow API 调用失败", e);
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("success", false);

            // 提取更详细的错误信息
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.length() > 200) {
                errorMessage = errorMessage.substring(0, 200) + "...";
            }
            error.put("message", "AI 响应失败: " + (errorMessage != null ? errorMessage : "未知错误"));
            return error;
        }
    }

    /**
     * 流式聊天
     */
    public SseEmitter chatStream(String userMessage, List<Map<String, Object>> history,
                                  SseEmitter emitter, boolean enableDeepThink, boolean enableWebSearch) {
        logger.info("========== SiliconFlow 流式请求 ==========");

        if (userMessage == null || userMessage.trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\": \"消息不能为空\"}"));
                emitter.complete();
            } catch (IOException e) {
                logger.error("发送空消息错误失败", e);
            }
            return emitter;
        }

        SseEmitter sseEmitter = new SseEmitter(300000L);

        // 心跳定时器
        ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(1);
        Runnable heartbeatTask = () -> {
            try {
                sseEmitter.send(SseEmitter.event().name("heartbeat").data(": keepalive\n\n"));
            } catch (IOException e) {
                logger.debug("心跳发送失败，连接可能已关闭");
            }
        };
        heartbeatScheduler.scheduleAtFixedRate(heartbeatTask, 30, 30, TimeUnit.SECONDS);

        sseEmitter.onTimeout(() -> {
            heartbeatScheduler.shutdown();
            try {
                sseEmitter.send(SseEmitter.event().name("error").data("{\"error\": \"请求超时\"}"));
            } catch (IOException e) {
                logger.error("发送超时错误失败", e);
            }
            sseEmitter.complete();
        });

        sseEmitter.onError(e -> {
            heartbeatScheduler.shutdown();
            logger.error("SSE 连接错误: {}", e.getMessage());
        });

        try {
            List<Map<String, Object>> messages = new ArrayList<>();

            // 系统提示 - 从数据库获取
            String systemPrompt = getSystemPrompt(enableWebSearch);
            logger.info("System Prompt: {}", systemPrompt);
            messages.add(Map.of(
                    "role", "system",
                    "content", systemPrompt
            ));

            // 历史消息
            if (history != null) {
                messages.addAll(history);
            }

            // 用户消息
            Map<String, Object> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            // 获取温度配置
            double temperature = getTemperatureFromDb();
            logger.info("Temperature: {}", temperature);

            // 构建请求
            String selectedModel = config.getModel();
            logger.info("选用的模型: {}", selectedModel);
            logger.info("深度思考模式: {}", enableDeepThink);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", selectedModel);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", temperature);
            requestBody.put("stream", false);  // 非流式输出

            // Qwen3-8B 支持 enable_thinking 参数开启思考模式
            if (enableDeepThink) {
                requestBody.put("enable_thinking", true);
                requestBody.put("thinking_budget", 4096);
                logger.info("已添加 enable_thinking 参数");
            }

            sseEmitter.send(SseEmitter.event().name("start").data("{\"type\": \"start\"}"));

            // 调用 API
            logger.info("Messages count: {}", messages.size());
            logger.info("开始调用 SiliconFlow API, Model: {}", selectedModel);
            String response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(180))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            JsonNode choice = root.path("choices").get(0);
            JsonNode message = choice.path("message");

            // 获取思考过程
            JsonNode reasoning = message.path("reasoning_content");
            String thinking = "";
            if (!reasoning.isNull() && !reasoning.asText().isEmpty()) {
                thinking = reasoning.asText().trim();
                // 发送思考过程
                try {
                    sseEmitter.send(SseEmitter.event().name("reasoning")
                            .data("{\"reasoning\": \"" + escapeJson(thinking) + "\"}"));
                } catch (IOException e) {
                    logger.error("发送 reasoning 失败", e);
                }
            }

            String content = message.path("content").asText("");

            // 发送内容（模拟打字机效果）
            if (!content.isEmpty()) {
                // 分段发送，每段约 20 字符
                int chunkSize = 20;
                for (int i = 0; i < content.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, content.length());
                    String chunk = content.substring(i, end);
                    try {
                        sseEmitter.send(SseEmitter.event().name("token")
                                .data("{\"token\": \"" + escapeJson(chunk) + "\"}"));
                        Thread.sleep(30); // 打字机延迟
                    } catch (IOException | InterruptedException e) {
                        logger.error("发送 token 失败", e);
                        break;
                    }
                }
            }

            // 完成
            heartbeatScheduler.shutdown();
            sseEmitter.send(SseEmitter.event().name("complete").data("{\"type\": \"complete\"}"));
            sseEmitter.complete();
            logger.info("========== SiliconFlow 流式请求正常结束 ==========");

        } catch (Exception e) {
            logger.error("SiliconFlow API 调用失败", e);
            heartbeatScheduler.shutdown();
            try {
                sseEmitter.send(SseEmitter.event().name("error")
                        .data("{\"error\": \"" + escapeJson(e.getMessage()) + "\"}"));
            } catch (IOException ex) {
                logger.error("发送错误事件失败", ex);
            }
            sseEmitter.completeWithError(e);
        }

        return sseEmitter;
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
     * 获取系统提示词 - 从数据库获取
     * @param includeWebSearch 是否包含联网搜索工具提示
     */
    private String getSystemPrompt(boolean includeWebSearch) {
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
            basePrompt = "你是 Bezhuang AI，一个智能助手。请用简洁清晰的中文回答用户问题。";
        }

        // SiliconFlow 只使用本地工具，不包含 web_search
        basePrompt += "\n\n" + toolService.getToolsPrompt(false);

        return basePrompt;
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

    /**
     * 工具调用结果类
     */
    private static class SiliconFlowToolCallResult {
        String response;
        String thinking;
        List<Map<String, String>> searchLinks;
        boolean isError;

        SiliconFlowToolCallResult(String response, String thinking, List<Map<String, String>> searchLinks) {
            this.response = response;
            this.thinking = thinking;
            this.searchLinks = searchLinks;
            this.isError = false;
        }

        SiliconFlowToolCallResult(String response, String thinking, List<Map<String, String>> searchLinks, boolean isError) {
            this.response = response;
            this.thinking = thinking;
            this.searchLinks = searchLinks;
            this.isError = isError;
        }
    }

    /**
     * 执行工具调用流程（支持多轮思考 Chain of Thoughts）
     */
    private SiliconFlowToolCallResult executeWithToolCalls(List<Map<String, Object>> messages,
                                                            String selectedModel, double temperature,
                                                            boolean enableDeepThink, boolean enableWebSearch) {
        String chatModel = config.getModel();
        String reasonerModel = config.getReasonerModel();
        List<String> allThinking = new ArrayList<>(); // 收集所有思考过程
        int toolCallCount = 0;
        List<Map<String, String>> allSearchLinks = new ArrayList<>(); // 收集所有搜索链接（包含标题）

        logger.info("开始执行工具调用流程，当前消息数: {}", messages.size());
        logger.info("enableDeepThink: {}, enableWebSearch: {}", enableDeepThink, enableWebSearch);

        while (toolCallCount < MAX_TOOL_CALLS) {
            // 选择模型：启用深度思考时每轮都使用 reasonerModel
            String currentModel = enableDeepThink ? reasonerModel : chatModel;
            // 启用深度思考时每轮都开启思考模式
            boolean enableThinking = enableDeepThink;

            // 构建请求（包含工具定义，可选启用思考模式）
            Map<String, Object> requestBody = buildRequestBody(currentModel, messages, temperature, enableWebSearch, enableThinking);

            // 调用 API
            String response = null;
            Exception apiException = null;
            try {
                response = callSiliconFlowApi(requestBody);
                logger.info("API 原始响应长度: {}", response != null ? response.length() : 0);
            } catch (Exception e) {
                apiException = e;
                logger.error("SiliconFlow API 调用失败: {}", e.getMessage());
            }

            // 检查是否是连接/超时错误
            if (apiException != null) {
                String errorMsg = apiException.getMessage();
                if (errorMsg != null && (errorMsg.contains("timeout") || errorMsg.contains("Connection") || errorMsg.contains("timed out"))) {
                    logger.warn("SiliconFlow API 连接超时，返回友好错误");
                    String thinking = String.join("\n\n", allThinking);
                    return new SiliconFlowToolCallResult("服务暂时无法响应，请稍后再试", thinking, allSearchLinks, true);
                }
                break;
            }

            JsonNode root;
            try {
                root = objectMapper.readTree(response);
            } catch (Exception e) {
                logger.error("解析响应失败", e);
                break;
            }

            JsonNode choice = root.path("choices").get(0);
            JsonNode message = choice.path("message");

            // 获取思考过程（每轮都获取，最多500字）
            if (enableDeepThink && message.has("reasoning_content")) {
                JsonNode reasoning = message.path("reasoning_content");
                if (!reasoning.isNull() && !reasoning.asText().isEmpty()) {
                    String roundThinking = reasoning.asText().trim();
                    if (!roundThinking.isEmpty()) {
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

                // 添加 assistant 消息
                Map<String, Object> assistantMessage = new LinkedHashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", message.path("content").asText(""));
                try {
                    assistantMessage.put("tool_calls", objectMapper.readValue(toolCalls.toString(), List.class));
                } catch (Exception e) {
                    logger.error("解析 tool_calls 失败", e);
                }
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
                    logger.info("工具 {} 返回结果长度: {}", toolName, toolResult.length());

                    Map<String, Object> toolMessage = new LinkedHashMap<>();
                    toolMessage.put("role", "tool");
                    toolMessage.put("tool_call_id", toolCall.path("id").asText());
                    toolMessage.put("name", toolName);
                    toolMessage.put("content", toolResult.isEmpty() ? "工具调用失败或返回空结果" : toolResult);
                    messages.add(toolMessage);
                }

                toolCallCount++;
                logger.info("工具调用轮次: {}/{}", toolCallCount, MAX_TOOL_CALLS);
                continue;
            }

            // 没有工具调用，返回结果
            String content = message.path("content").asText("");
            logger.info("无工具调用，返回内容长度: {}, 内容: [{}]", content.length(), content);
            // 合并所有思考过程
            String thinking = String.join("\n\n", allThinking);
            logger.info("共收集到 {} 轮思考，总长度: {}", allThinking.size(), thinking.length());
            return new SiliconFlowToolCallResult(content, thinking, allSearchLinks);
        }

        logger.warn("工具调用次数达到上限: {}", MAX_TOOL_CALLS);
        String thinking = String.join("\n\n", allThinking);
        if (allThinking.isEmpty() && allSearchLinks.isEmpty()) {
            // 如果既没有思考过程也没有搜索链接，说明是API调用失败
            return new SiliconFlowToolCallResult("服务暂时无法响应，请稍后再试", thinking, allSearchLinks, true);
        }
        return new SiliconFlowToolCallResult("工具调用次数过多，请重试", thinking, allSearchLinks);
    }

    /**
     * 构建请求体（包含工具定义）
     * SiliconFlow 只使用本地工具（get_current_time），不包含 web_search
     */
    private Map<String, Object> buildRequestBody(String model, List<Map<String, Object>> messages,
                                                  double temperature, boolean enableWebSearch) {
        return buildRequestBody(model, messages, temperature, enableWebSearch, false);
    }

    /**
     * 构建请求体（包含工具定义，可选启用思考模式）
     * SiliconFlow 只使用本地工具（get_current_time），不包含 web_search
     */
    private Map<String, Object> buildRequestBody(String model, List<Map<String, Object>> messages,
                                                  double temperature, boolean enableWebSearch,
                                                  boolean enableThinking) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);
        requestBody.put("max_tokens", config.getMaxTokens());
        requestBody.put("temperature", temperature);

        // Qwen3-8B 支持 enable_thinking 参数开启思考模式
        if (enableThinking) {
            requestBody.put("enable_thinking", true);
            // 减小 thinking_budget 让每次思考更简洁，生成多轮短思考
            requestBody.put("thinking_budget", 2048);
            logger.info("启用深度思考模式，thinking_budget: 2048");
        }

        // SiliconFlow 只使用本地工具（get_current_time），不包含 web_search
        List<Map<String, Object>> tools = toolService.getToolsDefinition(false);
        if (!tools.isEmpty()) {
            requestBody.put("tools", tools);
            logger.info("开始调用 SiliconFlow API（工具模式：仅本地工具）");
            logger.info("Model: {}", model);
            logger.info("Tools count: {}", tools.size());
        }

        return requestBody;
    }

    /**
     * 调用 SiliconFlow API
     */
    private String callSiliconFlowApi(Map<String, Object> requestBody) {
        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + config.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(180))
                .block();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
