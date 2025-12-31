package com.bezhuang.my_little_app_backend.service;

import com.bezhuang.my_little_app_backend.config.websearch.BochaWebSearchConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 工具服务 - 实现 DeepSeek Function Calling 所需的工具函数
 *
 * 可用工具：
 * 1. get_weather - 查询天气（使用 Open-Meteo 免费API）
 * 2. get_current_time - 获取当前时间
 * 3. web_search - 网页搜索（使用博查AI Web Search API）
 */
@Service
public class ToolService {

    private static final Logger logger = LoggerFactory.getLogger(ToolService.class);
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final BochaWebSearchConfig bochaConfig;
    private final WebClient bochaWebClient;

    public ToolService(BochaWebSearchConfig bochaConfig) {
        this.bochaConfig = bochaConfig;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.open-meteo.com")
                .build();
        this.objectMapper = new ObjectMapper();
        this.bochaWebClient = WebClient.builder()
                .baseUrl("https://api.bochaai.com")
                .build();
    }

    /**
     * 执行工具调用
     */
    public String executeTool(String toolName, String arguments) {
        try {
            logger.info("执行工具: {}, 参数: {}", toolName, arguments);

            return switch (toolName) {
                case "get_current_time" -> executeGetCurrentTime(arguments);
                case "web_search" -> executeWebSearch(arguments);
                default -> "未知工具: " + toolName;
            };
        } catch (Exception e) {
            logger.error("工具执行失败: {} - {}", toolName, e.getMessage());
            return "工具调用失败: " + e.getMessage();
        }
    }

    /**
     * 获取工具定义（供 DeepSeek API 使用）
     * @param includeWebSearch 是否包含联网搜索工具
     */
    public List<Map<String, Object>> getToolsDefinition(boolean includeWebSearch) {
        List<Map<String, Object>> tools = new ArrayList<>();

        // 1. get_current_time 工具
        Map<String, Object> timeTool = new LinkedHashMap<>();
        timeTool.put("type", "function");
        Map<String, Object> timeFunction = new LinkedHashMap<>();
        timeFunction.put("name", "get_current_time");
        timeFunction.put("description", "获取当前日期和时间。不带参数调用时默认返回上海时区(Asia/Shanghai)的时间。");

        Map<String, Object> timeParams = new LinkedHashMap<>();
        timeParams.put("type", "object");
        Map<String, Object> timeProperties = new LinkedHashMap<>();

        Map<String, Object> timezoneParam = new LinkedHashMap<>();
        timezoneParam.put("type", "string");
        timezoneParam.put("description", "时区字符串，如'Asia/Shanghai'（默认）、'America/New_York'、'Europe/London'");
        timeProperties.put("timezone", timezoneParam);

        timeParams.put("properties", timeProperties);
        timeParams.put("required", List.of()); // 时区参数可选，默认上海时间
        timeFunction.put("parameters", timeParams);
        timeTool.put("function", timeFunction);
        tools.add(timeTool);

        // 2. web_search 工具（仅在开启联网搜索时包含）
        if (includeWebSearch) {
            Map<String, Object> searchTool = new LinkedHashMap<>();
            searchTool.put("type", "function");
            Map<String, Object> searchFunction = new LinkedHashMap<>();
            searchFunction.put("name", "web_search");
            searchFunction.put("description", "搜索互联网获取实时信息、新闻、资料等");

            Map<String, Object> searchParams = new LinkedHashMap<>();
            searchParams.put("type", "object");
            Map<String, Object> searchProperties = new LinkedHashMap<>();

            Map<String, Object> queryParam = new LinkedHashMap<>();
            queryParam.put("type", "string");
            queryParam.put("description", "搜索关键词或问题");
            searchProperties.put("query", queryParam);

            searchParams.put("properties", searchProperties);
            searchParams.put("required", List.of("query"));
            searchFunction.put("parameters", searchParams);
            searchTool.put("function", searchFunction);
            tools.add(searchTool);
        }

        return tools;
    }

    /**
     * 获取默认工具定义（不包含联网搜索）
     */
    public List<Map<String, Object>> getToolsDefinition() {
        return getToolsDefinition(false);
    }

    /**
     * 获取工具使用提示词
     * @param includeWebSearch 是否包含联网搜索提示
     */
    public String getToolsPrompt(boolean includeWebSearch) {
        StringBuilder sb = new StringBuilder();
        sb.append("【工具使用指南】\n");
        sb.append("你拥有以下工具，当用户提出相关问题时必须调用：\n\n");
        sb.append("- get_current_time: 查询时间，当用户问\"现在几点了\"、\"日期\"等问题时调用\n");
        if (includeWebSearch) {
            sb.append("- web_search: 联网搜索，当用户需要实时信息、新闻、知识库外的内容时调用\n");
        }
        sb.append("\n使用规则：\n");
        sb.append("1. 必须调用工具获取实时数据，不要编造信息\n");
        sb.append("2. 工具调用后，用自然的中文回答用户\n");
        sb.append("3. 如果工具调用失败，明确告知用户\n");
        return sb.toString();
    }

    /**
     * 获取默认工具提示词（不包含联网搜索）
     */
    public String getToolsPrompt() {
        return getToolsPrompt(false);
    }

    // ==================== 工具实现 ====================

    /**
     * 获取当前时间
     */
    private String executeGetCurrentTime(String arguments) {
        try {
            JsonNode argsNode = objectMapper.readTree(arguments);
            String timezone = argsNode.path("timezone").asText("Asia/Shanghai");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of(timezone));

            return String.format("【当前时间】\n时区: %s\n时间: %s",
                    timezone, now.format(formatter));

        } catch (Exception e) {
            logger.error("获取时间失败: {}", e.getMessage());
            return "获取时间失败: " + e.getMessage();
        }
    }

    /**
     * 网页搜索（使用博查AI Web Search API）
     */
    private String executeWebSearch(String arguments) {
        try {
            JsonNode argsNode = objectMapper.readTree(arguments);
            String query = argsNode.path("query").asText("");

            if (query.isEmpty()) {
                return "请提供搜索关键词";
            }

            logger.info("执行网页搜索: {}", query);

            // 使用博查AI API
            if (bochaConfig.isConfigured()) {
                String result = bochaWebSearch(query);
                return result != null ? result : "联网搜索服务暂时不可用，请稍后重试。";
            }

            return "联网搜索服务未配置，请联系管理员。";

        } catch (Exception e) {
            logger.error("网页搜索失败: {}", e.getMessage());
            return "搜索失败: " + e.getMessage();
        }
    }

    /**
     * 博查AI Web Search API
     */
    private String bochaWebSearch(String query) {
        try {
            String apiKey = bochaConfig.getApiKey();

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("query", query);
            requestBody.put("freshness", "noLimit");
            requestBody.put("summary", true);
            requestBody.put("count", 5);

            String response = bochaWebClient.post()
                    .uri("/v1/web-search")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .block();

            logger.debug("博查AI响应: {}", response);

            JsonNode jsonResponse = objectMapper.readTree(response);

            // 检查响应状态
            if (jsonResponse.path("code").asInt() != 200) {
                String errorMsg = jsonResponse.path("message").asText("未知错误");
                logger.warn("博查AI搜索失败: {}", errorMsg);
                return null;
            }

            // 解析搜索结果
            JsonNode data = jsonResponse.path("data");
            if (!data.has("webPages") || !data.path("webPages").has("value")) {
                logger.warn("博查AI返回数据格式异常");
                return null;
            }

            JsonNode webpages = data.path("webPages").path("value");
            if (!webpages.isArray() || webpages.size() == 0) {
                return "未找到相关结果。";
            }

            StringBuilder result = new StringBuilder();
            result.append(String.format("【%s 的搜索结果】\n\n", query));

            int count = 0;
            for (JsonNode page : webpages) {
                if (count >= 5) break;

                String name = page.path("name").asText("");
                String url = page.path("url").asText("");
                String summary = page.path("summary").asText("");
                String siteName = page.path("siteName").asText("");
                String dateLastCrawled = page.path("dateLastCrawled").asText("");

                // 截取摘要
                if (summary.length() > 200) {
                    summary = summary.substring(0, 200) + "...";
                }

                result.append(String.format("【引用 %d】\n", count + 1));
                if (!name.isEmpty()) {
                    result.append(String.format("标题: %s\n", name));
                }
                if (!url.isEmpty()) {
                    result.append(String.format("链接: %s\n", url));
                }
                if (!summary.isEmpty()) {
                    result.append(String.format("摘要: %s\n", summary));
                }
                if (!siteName.isEmpty()) {
                    result.append(String.format("来源: %s\n", siteName));
                }
                if (!dateLastCrawled.isEmpty()) {
                    result.append(String.format("时间: %s\n", dateLastCrawled));
                }
                result.append("\n");
                count++;
            }

            return result.toString().trim();

        } catch (Exception e) {
            logger.warn("博查AI搜索失败: {}", e.getMessage());
            return null;
        }
    }
}
