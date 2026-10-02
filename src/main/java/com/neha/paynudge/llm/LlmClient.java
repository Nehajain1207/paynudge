package com.neha.paynudge.llm;

import java.util.List;
import java.util.Map;

/**
 * One turn with a chat model. Messages and tools use the OpenAI "chat completions" shape,
 * which Gemini, OpenAI, Groq and others all accept. Returns the assistant message
 * (either text in "content", or a list in "tool_calls").
 */
public interface LlmClient {
    Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools);
}
