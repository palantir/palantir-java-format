/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A component that replaces the default IntelliJ {@link CodeStyleManager} with {@link PalantirCodeStyleManager}.
 *
 */
final class PalantirJavaFormatInstaller implements ProjectComponent {
    private static final Logger log = LoggerFactory.getLogger(PalantirJavaFormatInstaller.class);
    private static final String CODE_STYLE_MANAGER_KEY = CodeStyleManager.class.getName();

    private final Project project;

    private PalantirJavaFormatInstaller(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        installFormatter(project);
    }

    private static void installFormatter(Project project) {
        if (isNewApi()) {
            log.info(
                    "Not performing legacy install of {} via programmatic component replacement as the API is new "
                            + "enough, using ",
                    PalantirCodeStyleManager.class.getCanonicalName());
            return;
        }
        CodeStyleManager currentManager = CodeStyleManager.getInstance(project);

        if (currentManager instanceof PalantirCodeStyleManager) {
            currentManager = ((PalantirCodeStyleManager) currentManager).getDelegate();
        }

        setManager(project, new PalantirCodeStyleManager(currentManager));
    }

    private static void setManager(Project project, CodeStyleManager newManager) {
        MutablePicoContainer container = (MutablePicoContainer) project.getPicoContainer();
        container.unregisterComponent(CODE_STYLE_MANAGER_KEY);
        container.registerComponentInstance(CODE_STYLE_MANAGER_KEY, newManager);
    }

    private static boolean isNewApi() {
        ApplicationInfo appInfo = ApplicationInfoImpl.getInstance();
        return appInfo.getBuild().getBaselineVersion() >= 193;
    }
}
