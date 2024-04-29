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
import com.intellij.codeInsight.actions.OptimizeImportsProcessor;
import com.intellij.formatting.service.FormattingService;
import com.intellij.lang.ImportOptimizer;
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
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PalantirJavaFormatImportOptimizerTest {
    private JavaCodeInsightTestFixture fixture;
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

        PalantirJavaFormatSettings settings = PalantirJavaFormatSettings.getInstance(fixture.getProject());
        State resetState = new State();
        resetState.setEnabled("true");
        settings.loadState(resetState);
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

    @AfterEach
    public void tearDown() throws Exception {
        fixture.tearDown();
    }

    @Test
    public void removesUnusedImports() throws Exception {
        PsiFile file = createPsiFile(
                "com/foo/ImportTest.java",
                "package com.foo;",
                "import java.util.List;",
                "import java.util.ArrayList;",
                "import java.util.Map;",
                "public class ImportTest {",
                "static final Map map;",
                "}");
        OptimizeImportsProcessor processor = new OptimizeImportsProcessor(file.getProject(), file);
        WriteCommandAction.runWriteCommandAction(file.getProject(), () -> {
            ProjectRootManager.getInstance(getProject())
                    .setProjectSdk(getProjectDescriptor().getSdk());
            processor.run();
            PsiDocumentManager.getInstance(file.getProject()).commitAllDocuments();
        });

        assertThat(file.getText()).doesNotContain("List");
        assertThat(file.getText()).contains("import java.util.Map;");
        assertThat(delegatingFormatter.wasInvoked()).isTrue();
    }

    @Test
    public void reordersImports() throws Exception {
        PsiFile file = createPsiFile(
                "com/foo/ImportTest.java",
                "package com.foo;",
                "import java.util.List;",
                "import java.util.ArrayList;",
                "import java.util.Map;",
                "public class ImportTest {",
                "static final ArrayList arrayList;",
                "static final List list;",
                "static final Map map;",
                "}");
        OptimizeImportsProcessor processor = new OptimizeImportsProcessor(file.getProject(), file);
        WriteCommandAction.runWriteCommandAction(file.getProject(), () -> {
            ProjectRootManager.getInstance(getProject())
                    .setProjectSdk(getProjectDescriptor().getSdk());
            processor.run();
            PsiDocumentManager.getInstance(file.getProject()).commitAllDocuments();
        });

        assertThat(file.getText())
                .contains("import java.util.ArrayList;\n" + "import java.util.List;\n" + "import java.util.Map;\n");
        assertThat(delegatingFormatter.wasInvoked()).isTrue();
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
        public @NotNull Set<ImportOptimizer> getImportOptimizers(@NotNull PsiFile file) {
            return super.getImportOptimizers(file).stream().map(this::wrap).collect(Collectors.toUnmodifiableSet());
        }

        private ImportOptimizer wrap(ImportOptimizer delegate) {
            return new ImportOptimizer() {
                @Override
                public boolean supports(@NotNull PsiFile file) {
                    return delegate.supports(file);
                }

                @Override
                public @NotNull Runnable processFile(@NotNull PsiFile file) {
                    return () -> {
                        invoked = true;
                        delegate.processFile(file).run();
                    };
                }
            };
        }
    }
}
