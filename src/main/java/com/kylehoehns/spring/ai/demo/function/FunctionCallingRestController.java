package com.kylehoehns.spring.ai.demo.function;

import java.util.List;
import java.util.Set;

import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/function")
@Slf4j
public class FunctionCallingRestController {

    private final ChatModel chatModel;

    @PostMapping
    public String askSportsQuestion(@RequestBody SportsQuestionRequest request) {

        String systemMessageText = """
            You are an expert college football analyst answering questions about college football.
            If the question provided is not about college football, simply state that you don't know.
        """;

        UserMessage userMessage = new UserMessage(request.question());
        SystemMessage systemMessage = new SystemMessage(systemMessageText);
        Prompt prompt = new Prompt(
                List.of(userMessage, systemMessage),
                OpenAiChatOptions.builder().withFunctions(Set.of("bestTeam", "liveScore")).build()
        );

        log.info("\nPrompt\n {}", prompt);

        var response = chatModel.call(prompt);

        log.info("Total Tokens {}", response.getMetadata().getUsage().getTotalTokens());

        var chatResponse = response.getResult().getOutput().getContent();
        log.info("\nResponse\n {}", chatResponse);

        return response.getResult().getOutput().getContent();
    }

    public record SportsQuestionRequest(String question) {}

}
