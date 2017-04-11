package com.walmartlabs.concord.project;

import com.walmartlabs.concord.project.model.Profile;
import com.walmartlabs.concord.project.model.ProjectDefinition;
import com.walmartlabs.concord.project.yaml.YamlFormConverter;
import com.walmartlabs.concord.project.yaml.YamlParser;
import com.walmartlabs.concord.project.yaml.YamlProcessConverter;
import com.walmartlabs.concord.project.yaml.YamlProjectConverter;
import com.walmartlabs.concord.project.yaml.model.*;
import io.takari.bpm.model.ProcessDefinition;
import io.takari.bpm.model.form.FormDefinition;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

public class ProjectDirectoryLoader {

    public static final String PROJECT_FILE_NAME = ".concord.yml";

    private final YamlParser parser = new YamlParser();

    public ProjectDefinition load(Path baseDir) throws IOException {
        ProjectDefinitionBuilder b = new ProjectDefinitionBuilder(parser);

        Path projectFile = baseDir.resolve(PROJECT_FILE_NAME);
        if (Files.exists(projectFile)) {
            b.addProjectFile(projectFile);
        }

        Path defsDir = baseDir.resolve(Constants.Files.DEFINITIONS_DIR_NAME);
        if (Files.exists(defsDir)) {
            b.addDefinitions(defsDir);
        }

        return b.build();
    }

    private static class ProjectDefinitionBuilder {

        private final YamlParser parser;

        private Map<String, ProcessDefinition> flows;
        private Map<String, FormDefinition> forms;
        private ProjectDefinition projectDefinition;

        private ProjectDefinitionBuilder(YamlParser parser) {
            this.parser = parser;
        }

        public ProjectDefinitionBuilder addProjectFile(Path path) throws IOException {
            YamlProject yml = parser.parseProject(path);
            this.projectDefinition = YamlProjectConverter.convert(yml);
            return this;
        }

        public void addDefinitions(Path path) throws IOException {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    loadDefinitions(file);
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        private void loadDefinitions(Path file) throws IOException {
            String n = file.getFileName().toString();
            if (!n.endsWith(".yml") && !n.endsWith(".yaml")) {
                return;
            }

            YamlDefinitionFile df = parser.parseDefinitionFile(file);
            Map<String, YamlDefinition> m = df.getDefinitions();
            if (m != null) {
                for (Map.Entry<String, YamlDefinition> e : m.entrySet()) {
                    String k = e.getKey();
                    YamlDefinition v = e.getValue();

                    if (v instanceof YamlProcessDefinition) {
                        if (flows == null) {
                            flows = new HashMap<>();
                        }
                        flows.put(k, YamlProcessConverter.convert((YamlProcessDefinition) v));
                    } else if (v instanceof YamlFormDefinition) {
                        if (forms == null) {
                            forms = new HashMap<>();
                        }
                        forms.put(k, YamlFormConverter.convert((YamlFormDefinition) v));
                    }
                }
            }

            return;
        }

        public ProjectDefinition build() {
            if (flows == null) {
                flows = new HashMap<>();
            }

            if (forms == null) {
                forms = new HashMap<>();
            }

            Map<String, Object> variables = new HashMap<>();
            Map<String, Profile> profiles = new HashMap<>();

            if (projectDefinition != null) {
                if (projectDefinition.getFlows() != null) {
                    flows.putAll(projectDefinition.getFlows());
                }

                if (projectDefinition.getForms() != null) {
                    forms.putAll(projectDefinition.getForms());
                }

                if (projectDefinition.getVariables() != null) {
                    variables.putAll(projectDefinition.getVariables());
                }

                if (projectDefinition.getProfiles() != null) {
                    profiles.putAll(projectDefinition.getProfiles());
                }
            }

            return new ProjectDefinition(flows, forms, variables, profiles);
        }
    }
}
