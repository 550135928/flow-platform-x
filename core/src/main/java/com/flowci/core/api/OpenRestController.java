/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.core.api;

import com.flowci.core.api.domain.AddStatsItem;
import com.flowci.core.api.domain.CreateJobArtifact;
import com.flowci.core.api.domain.CreateJobReport;
import com.flowci.core.api.service.OpenRestService;
import com.flowci.core.config.domain.Config;
import com.flowci.core.flow.domain.StatsCounter;
import com.flowci.core.secret.domain.RSASecret;
import com.flowci.core.secret.domain.Secret;
import com.flowci.core.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Provides API which calling from agent plugin
 */
@RestController
@RequestMapping("/api")
public class OpenRestController {

    @Autowired
    private OpenRestService openRestService;

    @GetMapping("/secret/{name}")
    public Secret getSecret(@PathVariable String name) {
        Secret secret = openRestService.getSecret(name);

        if (secret instanceof RSASecret) {
            RSASecret rsa = (RSASecret) secret;
            rsa.setPublicKey(null);
        }

        return secret;
    }

    @GetMapping("/config/{name}")
    public Config getConfig(@PathVariable String name) {
        return openRestService.getConfig(name);
    }

    @GetMapping("/config/{name}/download")
    public ResponseEntity<Resource> downloadConfigFile(@PathVariable String name) {
        Config config = openRestService.getConfig(name);
        Pair<String, Resource> pair = openRestService.getResource(config);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pair.getFirst() + "\"")
                .body(pair.getSecond());
    }

    @GetMapping("/flow/{name}/users")
    public List<User> listFlowUserEmail(@PathVariable String name) {
        return openRestService.users(name);
    }

    @PostMapping("/flow/{name}/stats")
    public void addStatsItem(@PathVariable String name,
                             @Validated @RequestBody AddStatsItem body) {
        openRestService.saveStatsForFlow(name, body.getType(), StatsCounter.from(body.getData()));
    }

    @PostMapping("/flow/{name}/job/{number}/context")
    public void addJobContext(@PathVariable String name,
                              @PathVariable long number,
                              @RequestBody Map<String, String> vars) {
        openRestService.addToJobContext(name, number, vars);
    }

    @PostMapping("/flow/{name}/job/{number}/report")
    public void uploadJobReport(@PathVariable String name,
                                @PathVariable long number,
                                @Validated @RequestPart("body") CreateJobReport meta,
                                @RequestPart("file") MultipartFile file) {

        openRestService.saveJobReport(name, number, meta, file);
    }

    @PostMapping("/flow/{name}/job/{number}/artifact")
    public void uploadJobArtifact(@PathVariable String name,
                                  @PathVariable long number,
                                  @Validated @RequestPart("body") CreateJobArtifact meta,
                                  @RequestPart("file") MultipartFile file) {
        openRestService.saveJobArtifact(name, number, meta, file);
    }
}
