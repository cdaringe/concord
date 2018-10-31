package com.walmartlabs.concord.project.yaml.validator;

/*-
 * *****
 * Concord
 * -----
 * Copyright (C) 2017 - 2018 Walmart Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import com.walmartlabs.concord.common.validation.ConcordKey;
import com.walmartlabs.concord.project.yaml.model.YamlCheckpoint;

public class YamlCheckpointValidator implements StepValidator<YamlCheckpoint> {

    public void validate(ValidatorContext ctx, YamlCheckpoint s) {
        if (!s.getKey().matches(ConcordKey.PATTERN)) {
            throw new IllegalArgumentException("Invalid checkpoint name: " + s.getKey() + ". Error in @:" + s.getLocation());
        }

        ctx.assertUnique("checkpoint", s.getKey(), s.getLocation());
    }
}