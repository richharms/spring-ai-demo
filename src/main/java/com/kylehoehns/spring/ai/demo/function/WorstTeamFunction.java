package com.kylehoehns.spring.ai.demo.function;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorstTeamFunction implements Function<WorstTeamFunction.Request, WorstTeamFunction.Response> {

    @Override
    public Response apply(Request request) {

        log.info("Worst Team Request: {}", request);

        var response = new Response("Michigan");

        log.info("Worst Team Response: {}", response);

        return response;
    }

    public record Request(String question) {}
    public record Response(String team) {}
}
