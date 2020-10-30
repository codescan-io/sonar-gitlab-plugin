/*
 * SonarQube :: GitLab Plugin
 * Copyright (C) 2016-2017 Talanlabs
 * gabriel.allaigre@gmail.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.talanlabs.sonar.plugins.gitlab;

import com.talanlabs.sonar.plugins.gitlab.models.JsonMode;
import com.talanlabs.sonar.plugins.gitlab.models.QualityGate;
import com.talanlabs.sonar.plugins.gitlab.models.QualityGateFailMode;
import com.talanlabs.sonar.plugins.gitlab.models.Rule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;

public class ReporterTest {

    private static final String GITLAB_URL = "https://gitlab.com/test/test";

    private MapSettings settings;
    private GitLabPluginConfiguration config;
    private Reporter reporter;

    @Before
    public void setup() {
        settings = new MapSettings(new PropertyDefinitions(PropertyDefinition.builder(CoreProperties.SERVER_BASE_URL).name("Server base URL").description("HTTP URL of this SonarQube server, such as <i>http://yourhost.yourdomain/sonar</i>. This value is used i.e. to create links in emails.")
                .category(CoreProperties.CATEGORY_GENERAL).defaultValue("http://localhost:9000").build()).addComponents(GitLabPlugin.definitions()));

        settings.setProperty(CoreProperties.SERVER_BASE_URL, "http://myserver");
        settings.setProperty(GitLabPlugin.GITLAB_COMMIT_SHA, "abc123");

        config = new GitLabPluginConfiguration(settings.asConfig(), new System2());
        reporter = new Reporter(config);
    }

    @Test
    public void noIssues() {
        Assertions.assertThat(reporter.getIssueCount()).isEqualTo(0);
    }

    @Test
    public void oneIssue() {
        reporter.process(Utils.newIssue("component", null, null, Severity.INFO, true, "Issue", "rule"), null, null, GITLAB_URL, "file", "http://myserver", true);

        Assertions.assertThat(reporter.getIssueCount()).isEqualTo(1);
        Assertions.assertThat(reporter.getNotReportedIssueCount()).isEqualTo(0);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.INFO)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.BLOCKER)).isEqualTo(0);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.CRITICAL)).isEqualTo(0);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.MAJOR)).isEqualTo(0);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.MINOR)).isEqualTo(0);
        Assertions.assertThat(reporter.getReportIssues()).hasSize(1);
    }

    @Test
    public void shouldFormatIssuesForMarkdownNoInline() {
        reporter.process(Utils.newIssue("component", null, null, Severity.INFO, true, "Issue", "rule"), null, null, GITLAB_URL, "file", "http://myserver", true);
        reporter.process(Utils.newIssue("component", null, null, Severity.MINOR, true, "Issue", "rule"), null, null, GITLAB_URL, "file", "http://myserver", true);
        reporter.process(Utils.newIssue("component", null, null, Severity.MAJOR, true, "Issue", "rule"), null, null, GITLAB_URL, "file", "http://myserver", true);
        reporter.process(Utils.newIssue("component", null, null, Severity.CRITICAL, true, "Issue", "rule"), null, null, GITLAB_URL, "file", "http://myserver", true);
        reporter.process(Utils.newIssue("component", null, null, Severity.BLOCKER, true, "Issue", "rule"), null, null, GITLAB_URL, "file", "http://myserver", true);

        Assertions.assertThat(reporter.getIssueCount()).isEqualTo(5);
        Assertions.assertThat(reporter.getNotReportedIssueCount()).isEqualTo(0);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.INFO)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.BLOCKER)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.CRITICAL)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.MAJOR)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.MINOR)).isEqualTo(1);
        Assertions.assertThat(reporter.getReportIssues()).hasSize(5);
    }

    @Test
    public void shouldFormatIssuesForMarkdownMixInlineGlobal() {
        reporter.process(Utils.newIssue("component", null, null, Severity.INFO, true, "Issue 0", "rule0"), null, null, GITLAB_URL, "file", "http://myserver", true);
        reporter.process(Utils.newIssue("component", null, null, Severity.MINOR, true, "Issue 1", "rule1"), null, null, GITLAB_URL, "file", "http://myserver", false);
        reporter.process(Utils.newIssue("component", null, null, Severity.MAJOR, true, "Issue 2", "rule2"), null, null, GITLAB_URL, "file", "http://myserver", true);
        reporter.process(Utils.newIssue("component", null, null, Severity.CRITICAL, true, "Issue 3", "rule3"), null, null, GITLAB_URL, "file", "http://myserver", false);
        reporter.process(Utils.newIssue("component", null, null, Severity.BLOCKER, true, "Issue 4", "rule4"), null, null, GITLAB_URL, "file", "http://myserver", true);

        Assertions.assertThat(reporter.getIssueCount()).isEqualTo(5);
        Assertions.assertThat(reporter.getNotReportedIssueCount()).isEqualTo(2);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.INFO)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.BLOCKER)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.CRITICAL)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.MAJOR)).isEqualTo(1);
        Assertions.assertThat(reporter.getIssueCountForSeverity(Severity.MINOR)).isEqualTo(1);
        Assertions.assertThat(reporter.getReportIssues()).hasSize(5);
        Assertions.assertThat(reporter.getNotReportedOnDiffReportIssueForSeverity(Severity.MINOR)).hasSize(1);
        Assertions.assertThat(reporter.getNotReportedOnDiffReportIssueForSeverity(Severity.CRITICAL)).hasSize(1);
    }

    @Test
    public void oneIssueNoSast() {
        settings.setProperty(GitLabPlugin.GITLAB_JSON_MODE, JsonMode.NONE.name());

        reporter.process(Utils.newIssue("component", null, null, Severity.INFO, true, "Issue", "rule"), null, null, GITLAB_URL, "file", "http://myserver", true);

        Assertions.assertThat(reporter.buildJson()).isEqualTo("[]");
    }

    @Test
    public void oneIssueSast() {
        settings.setProperty(GitLabPlugin.GITLAB_JSON_MODE, JsonMode.SAST.name());
        Rule rule = Rule.newBuilder().key("rule").build();

        reporter.process(Utils.newIssue("123", "component", null, 10, Severity.INFO, true, "Issue \"NULL\"", "rule"), rule, null, GITLAB_URL, "file", "http://myserver", true);

        Assertions.assertThat(reporter.buildJson()).isEqualTo("{\"version\":\"8.0.1\",\"vulnerabilities\":\"[{\"id\":\"123\",\"category\":\"sast\",\"name\":\"null\",\"description\":\"null\",\"identifiers\":[{\"type\":\"null\",\"value\":\"rule\",\"name\":\"null\"}],\"message\":\"Issue \\\"NULL\\\"\",\"severity\":\"Info\",\"solution\":\"http://myserver\",\"location\":{\"file\":\"file\",\"start_line\":\"10\"},\"scanner\":{\"id\":\"codescan\",\"name\":\"CodeScan\"}}]\"}");
    }

    @Test
    public void oneIssueCodeClimate() {
        settings.setProperty(GitLabPlugin.GITLAB_JSON_MODE, JsonMode.CODECLIMATE.name());
        Rule rule = Rule.newBuilder().key("rule").build();

        reporter.process(Utils.newIssue("456", "component", null, 20, Severity.INFO, true, "Issue \"NULL\"", "rule"), rule, null, GITLAB_URL, "file", "http://myserver", true);

        Assertions.assertThat(reporter.buildJson()).isEqualTo("[{\"fingerprint\":\"456\",\"description\":\"Issue \\\"NULL\\\"\",\"location\":{\"path\":\"file\",\"lines\": { \"begin\":20,\"end\":20}}}]");
    }

    @Test
    public void issuesSast() {
        settings.setProperty(GitLabPlugin.GITLAB_JSON_MODE, JsonMode.SAST.name());
        Rule rule = Rule.newBuilder().key("rule").build();

        for (int i = 0; i < 5; i++) {
            reporter.process(Utils.newIssue("toto_" + i, "component", null, null, Severity.INFO, true, "Issue", "rule" + i), rule, null, GITLAB_URL, "file", "http://myserver/rule" + i, true);
        }

        Assertions.assertThat(reporter.buildJson()).isEqualTo("{\"version\":\"8.0.1\",\"vulnerabilities\":\"[{\"id\":\"toto_0\",\"category\":\"sast\",\"name\":\"null\",\"description\":\"null\",\"identifiers\":[{\"type\":\"null\",\"value\":\"rule\",\"name\":\"null\"}],\"message\":\"Issue\",\"severity\":\"Info\",\"solution\":\"http://myserver/rule0\",\"location\":{\"file\":\"file\",\"start_line\":\"1\"},\"scanner\":{\"id\":\"codescan\",\"name\":\"CodeScan\"}},{\"id\":\"toto_1\",\"category\":\"sast\",\"name\":\"null\",\"description\":\"null\",\"identifiers\":[{\"type\":\"null\",\"value\":\"rule\",\"name\":\"null\"}],\"message\":\"Issue\",\"severity\":\"Info\",\"solution\":\"http://myserver/rule1\",\"location\":{\"file\":\"file\",\"start_line\":\"1\"},\"scanner\":{\"id\":\"codescan\",\"name\":\"CodeScan\"}},{\"id\":\"toto_2\",\"category\":\"sast\",\"name\":\"null\",\"description\":\"null\",\"identifiers\":[{\"type\":\"null\",\"value\":\"rule\",\"name\":\"null\"}],\"message\":\"Issue\",\"severity\":\"Info\",\"solution\":\"http://myserver/rule2\",\"location\":{\"file\":\"file\",\"start_line\":\"1\"},\"scanner\":{\"id\":\"codescan\",\"name\":\"CodeScan\"}},{\"id\":\"toto_3\",\"category\":\"sast\",\"name\":\"null\",\"description\":\"null\",\"identifiers\":[{\"type\":\"null\",\"value\":\"rule\",\"name\":\"null\"}],\"message\":\"Issue\",\"severity\":\"Info\",\"solution\":\"http://myserver/rule3\",\"location\":{\"file\":\"file\",\"start_line\":\"1\"},\"scanner\":{\"id\":\"codescan\",\"name\":\"CodeScan\"}},{\"id\":\"toto_4\",\"category\":\"sast\",\"name\":\"null\",\"description\":\"null\",\"identifiers\":[{\"type\":\"null\",\"value\":\"rule\",\"name\":\"null\"}],\"message\":\"Issue\",\"severity\":\"Info\",\"solution\":\"http://myserver/rule4\",\"location\":{\"file\":\"file\",\"start_line\":\"1\"},\"scanner\":{\"id\":\"codescan\",\"name\":\"CodeScan\"}}]\"}");
    }

    @Test
    public void issuesCodeClimate() {
        settings.setProperty(GitLabPlugin.GITLAB_JSON_MODE, JsonMode.CODECLIMATE.name());

        for (int i = 0; i < 5; i++) {
            reporter.process(Utils.newIssue("tata_" + i, "component", null, null, Severity.INFO, true, "Issue", "rule" + i), null, null, GITLAB_URL, "file", "http://myserver/rule" + i, true);
        }

        Assertions.assertThat(reporter.buildJson()).isEqualTo("[{\"fingerprint\":\"tata_0\",\"description\":\"Issue\",\"location\":{\"path\":\"file\",\"lines\": { \"begin\":0,\"end\":0}}},{\"fingerprint\":\"tata_1\",\"description\":\"Issue\",\"location\":{\"path\":\"file\",\"lines\": { \"begin\":0,\"end\":0}}},{\"fingerprint\":\"tata_2\",\"description\":\"Issue\",\"location\":{\"path\":\"file\",\"lines\": { \"begin\":0,\"end\":0}}},{\"fingerprint\":\"tata_3\",\"description\":\"Issue\",\"location\":{\"path\":\"file\",\"lines\": { \"begin\":0,\"end\":0}}},{\"fingerprint\":\"tata_4\",\"description\":\"Issue\",\"location\":{\"path\":\"file\",\"lines\": { \"begin\":0,\"end\":0}}}]");
    }

    @Test
    public void issuesJsonLine() {
        settings.setProperty(GitLabPlugin.GITLAB_JSON_MODE, JsonMode.SAST.name());
        Rule rule = Rule.newBuilder().key("rule").build();

        reporter.process(Utils.newIssue("toto", "component", null, null, Severity.INFO, true, "Issue\nline1\n\rline2", "rule"), rule, null, GITLAB_URL, "file", "http://myserver/rule", true);

        Assertions.assertThat(reporter.buildJson()).isEqualTo("{\"version\":\"8.0.1\",\"vulnerabilities\":\"[{\"id\":\"toto\",\"category\":\"sast\",\"name\":\"null\",\"description\":\"null\",\"identifiers\":[{\"type\":\"null\",\"value\":\"rule\",\"name\":\"null\"}],\"message\":\"Issue\\nline1\\n\\rline2\",\"severity\":\"Info\",\"solution\":\"http://myserver/rule\",\"location\":{\"file\":\"file\",\"start_line\":\"1\"},\"scanner\":{\"id\":\"codescan\",\"name\":\"CodeScan\"}}]\"}");
    }

    @Test
    public void reportSucceedsIfQualityGateIsNone() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.OK).build();
        reporter.setQualityGate(qualityGate);

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsOk() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.OK).build();
        reporter.setQualityGate(qualityGate);

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsWarn() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.WARN).build();
        reporter.setQualityGate(qualityGate);

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportFailsIfQualityGateIsError() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.ERROR).build();
        reporter.setQualityGate(qualityGate);

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.FAILED_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsNoneAndFailModeIsWarn() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.NONE).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.WARN.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsOkAndFailModeIsWarn() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.OK).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.WARN.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportFailsIfQualityGateIsWarnAndFailModeIsWarn() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.WARN).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.WARN.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.FAILED_GITLAB_STATUS);
    }

    @Test
    public void reportFailsIfQualityGateIsErrorAndFailModeIsWarn() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.ERROR).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.WARN.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.FAILED_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsNoneAndFailModeIsNone() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.NONE).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.NONE.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsOkAndFailModeIsNone() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.OK).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.NONE.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsWarnAndFailModeIsNone() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.WARN).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.NONE.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }

    @Test
    public void reportSucceedsIfQualityGateIsErrorAndFailModeIsNone() {
        QualityGate qualityGate = QualityGate.newBuilder().status(QualityGate.Status.ERROR).build();
        reporter.setQualityGate(qualityGate);
        settings.setProperty(GitLabPlugin.GITLAB_QUALITY_GATE_FAIL_MODE, QualityGateFailMode.NONE.getMeaning());

        Assertions.assertThat(reporter.getStatus()).isEqualTo(MessageHelper.SUCCESS_GITLAB_STATUS);
    }
}
