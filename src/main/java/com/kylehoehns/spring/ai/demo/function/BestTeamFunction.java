package com.kylehoehns.spring.ai.demo.function;

import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BestTeamFunction implements Function<BestTeamFunction.Request, BestTeamFunction.Response> {

    @Override
    public Response apply(Request request) {

        log.info("Best Team Request: {}", request);

        var response = new Response("Iowa");

        log.info("Best Team Response: {}", response);

        return response;
    }

    public record Request(String question) {}
    public record Response(String team) {}
}
