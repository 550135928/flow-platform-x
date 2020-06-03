/*
 * Copyright 2018 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flowci.core.common.config;

import com.flowci.core.job.service.SessionCmdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * @author yang
 */
@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    /**
     * To subscribe git test status update for flow
     * Ex: /topic/flows/git/test/{flow id}
     */
    private final String gitTestTopic = "/topic/flows/git/test";

    /**
     * To subscribe new job
     */
    private final String jobsTopic = "/topic/jobs";

    /**
     * To subscribe step update for job
     * Ex: /topic/steps/{job id}
     */
    private final String stepsTopic = "/topic/steps";

    /**
     * To subscribe task update for job
     * Ex: /topic/tasks/{job id}
     */
    private final String tasksTopic = "/topic/tasks";

    /**
     * To subscribe real time logging for all jobs.
     * Ex: /topic/logs
     */
    private final String logsTopic = "/topic/logs";

    /**
     * To subscribe agent update
     */
    private final String agentsTopic = "/topic/agents";

    /**
     * To subscribe agent host update
     */
    private final String agentHostTopic = "/topic/hosts";

    @Autowired
    private SessionCmdService sessionCmdService;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker(
                jobsTopic,
                stepsTopic,
                tasksTopic,
                logsTopic,
                agentsTopic,
                agentHostTopic,
                gitTestTopic
        );
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sessionCmdService, "/session").setAllowedOrigins("*");
    }

    @Bean
    @ConditionalOnProperty(prefix = "app", name = "socket-container", havingValue = "true")
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean c = new ServletServerContainerFactoryBean();
        c.setMaxTextMessageBufferSize(8192);
        c.setMaxBinaryMessageBufferSize(8192);
        c.setMaxSessionIdleTimeout(30L * 1000);
        return c;
    }

    @Bean("topicForGitTest")
    public String topicForGitTest() {
        return gitTestTopic;
    }

    @Bean("topicForJobs")
    public String topicForJobs() {
        return jobsTopic;
    }

    @Bean("topicForSteps")
    public String topicForSteps() {
        return stepsTopic;
    }

    @Bean("topicForTasks")
    public String topicForTasks() {
        return tasksTopic;
    }

    @Bean("topicForLogs")
    public String topicForLogs() {
        return logsTopic;
    }

    @Bean("topicForAgents")
    public String topicForAgents() {
        return agentsTopic;
    }

    @Bean("topicForAgentHost")
    public String topicForAgentHost() {
        return agentHostTopic;
    }
}
