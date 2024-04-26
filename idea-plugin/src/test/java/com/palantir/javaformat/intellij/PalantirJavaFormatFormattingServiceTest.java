/*
 * (c) Copyright 2024 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.javaformat.intellij;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.formatting.service.FormattingService;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.palantir.javaformat.intellij.PalantirJavaFormatSettings.State;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PalantirJavaFormatFormattingServiceTest {
    private JavaCodeInsightTestFixture fixture;
    private PalantirJavaFormatSettings settings;
    private DelegatingFormatter delegatingFormatter;

    @BeforeEach
    public void setUp() throws Exception {
        TestFixtureBuilder<IdeaProjectTestFixture> projectBuilder = IdeaTestFixtureFactory.getFixtureFactory()
                .createLightFixtureBuilder(getProjectDescriptor(), getClass().getName());
        fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(projectBuilder.getFixture());
        fixture.setUp();

        delegatingFormatter = new DelegatingFormatter();
        ExtensionTestUtil.maskExtensions(
                FormattingService.EP_NAME, ImmutableList.of(delegatingFormatter), fixture.getProjectDisposable());

        settings = PalantirJavaFormatSettings.getInstance(fixture.getProject());
        State resetState = new State();
        resetState.setEnabled("true");
        settings.loadState(resetState);
    }

    @AfterEach
    public void tearDown() throws Exception {
        fixture.tearDown();
    }

    @Test
    public void defaultFormatSettings() throws Exception {
        String input = Files.readString(
                Paths.get("../palantir-java-format/src/test/resources/com/palantir/javaformat/java/testdata/A.input"));
        String output = Files.readString(
                Path.of("../palantir-java-format/src/test/resources/com/palantir/javaformat/java/testdata/A.output"));
        PsiFile file = createPsiFile("com/foo/FormatTest.java", input);
        ReformatCodeProcessor processor = new ReformatCodeProcessor(file, false);
        WriteCommandAction.runWriteCommandAction(file.getProject(), () -> {
            ProjectRootManager.getInstance(getProject())
                    .setProjectSdk(getProjectDescriptor().getSdk());

            processor.run();
            PsiDocumentManager.getInstance(file.getProject()).commitAllDocuments();
        });

        assertThat(file.getText()).isEqualTo(output);
        assertThat(delegatingFormatter.wasInvoked()).isTrue();
    }

    protected Project getProject() {
        return fixture.getProject();
    }

    @NotNull
    protected LightProjectDescriptor getProjectDescriptor() {
        return new DefaultLightProjectDescriptor() {
            @Override
            public Sdk getSdk() {
                try {
                    return JavaSdk.getInstance()
                            .createJdk(
                                    "java 1.11", new File(System.getProperty("java.home")).getCanonicalPath(), false);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private PsiFile createPsiFile(String path, String... contents) throws IOException {
        VirtualFile virtualFile = fixture.getTempDirFixture().createFile(path, String.join("\n", contents));
        fixture.configureFromExistingVirtualFile(virtualFile);
        PsiFile psiFile = fixture.getFile();
        assertThat(psiFile).isNotNull();
        return psiFile;
    }

    private static final class DelegatingFormatter extends PalantirJavaFormatFormattingService {

        private boolean invoked = false;

        private boolean wasInvoked() {
            return invoked;
        }

        @Override
        protected FormattingTask createFormattingTask(AsyncFormattingRequest request) {
            FormattingTask delegateTask = super.createFormattingTask(request);
            return new FormattingTask() {
                @Override
                public boolean cancel() {
                    return delegateTask.cancel();
                }

                @Override
                public void run() {
                    invoked = true;
                    delegateTask.run();
                }
            };
        }
    }
}
