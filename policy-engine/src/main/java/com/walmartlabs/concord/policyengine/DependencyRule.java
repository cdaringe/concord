package com.walmartlabs.concord.policyengine;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

public class DependencyRule implements Serializable {

    private final String msg;
    private final String scheme;
    private final String groupId;
    private final String artifactId;
    private final String fromVersion;
    private final String toVersion;

    @JsonCreator
    public DependencyRule(
            @JsonProperty("msg") String msg,
            @JsonProperty("scheme") String scheme,
            @JsonProperty("groupId") String groupId,
            @JsonProperty("artifactId") String artifactId,
            @JsonProperty("fromVersion") String fromVersion,
            @JsonProperty("toVersion") String toVersion) {

        this.msg = msg;
        this.scheme = scheme;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
    }

    public String getMsg() {
        return msg;
    }

    public String getScheme() {
        return scheme;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public String getToVersion() {
        return toVersion;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, Utils.NotNullToStringStyle.NOT_NULL_STYLE)
                .append("msg", msg)
                .append("scheme", scheme)
                .append("groupId", groupId)
                .append("artifactId", artifactId)
                .append("fromVersion", fromVersion)
                .append("toVersion", toVersion)
                .toString();
    }
}
