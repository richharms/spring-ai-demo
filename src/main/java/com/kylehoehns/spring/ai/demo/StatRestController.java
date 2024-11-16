package com.kylehoehns.spring.ai.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@Slf4j
public class StatRestController {

  private final ChatModel chatModel;
  private final ResourceLoader resourceLoader;
  private final SimpleVectorStore simpleVectorStore;

  @Value("classpath:/vector-storage/vector-store.json")
  private Resource vectorStoreResource;

  @PostMapping("/embeddings")
  void generateEmbeddings() throws IOException {
    // load all our markdown files
    var resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
        .getResources("classpath:/documents/*.md");

    // turn them into spring ai "documents"
    var documents = new ArrayList<Document>();
    for (var resource : resources) {
      TextReader textReader = new TextReader(resource);
      documents.addAll(textReader.get());
    }

    // add these to our vector store
    simpleVectorStore.add(documents);

    // persist to file
    simpleVectorStore.save(vectorStoreResource.getFile());
  }

  @PostMapping
  StatResponse getStats(@RequestBody StatRequest request) {
    log.info("\nRequest\n {}", request);

    var userMessage = new UserMessage(request.question());

    var outputConverter = new BeanOutputConverter<>(StatResponse.class);

    var similarDocuments = simpleVectorStore.similaritySearch(request.question());

    var documentText = similarDocuments.stream()
        .map(Document::getContent)
        .collect(Collectors.joining("\n"));

    var systemMessageText = """
      You are an expert {sport} analyst answering questions about {sport}.
      If the question provided is not about {sport}, simply state that you don't know.
      Please utilize the information in the documents section to answer any questions.
      
      Documents:
      {documents}
      
      {format}
    """;
    var systemPromptTemplate = new SystemPromptTemplate(systemMessageText);
    var systemMessage = systemPromptTemplate.createMessage(
        Map.of(
            "sport", request.sport(),
            "format", outputConverter.getFormat(),
            "documents", documentText
        )
    );

    var prompt = new Prompt(
      List.of(userMessage, systemMessage)
    );

    log.info("\nPrompt\n {}", prompt);

    var response = chatModel.call(prompt);

    log.info("Total Tokens {}", response.getMetadata().getUsage().getTotalTokens());

    var chatResponse = response.getResult().getOutput().getContent();

    log.info("\nResponse\n {}", chatResponse);

    return outputConverter.convert(chatResponse);
  }

  record StatRequest(String question, String sport) {}

  record StatResponse(String teamName, List<StatItem> item) {}

  record StatItem(String statName, int year, float value, String playerName) {}

}
