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

import com.intellij.formatting.service.FormattingNotificationService;
import com.intellij.openapi.project.Project;

class Notifications {

    static final String GENERIC_ERROR_NOTIFICATION_GROUP = "palantir-java-format error";
    static final String PARSING_ERROR_NOTIFICATION_GROUP = "palantir-java-format parsing error";
    static final String PARSING_ERROR_TITLE = PARSING_ERROR_NOTIFICATION_GROUP;

    static String parsingErrorMessage(String filename) {
        return "palantir-java-format failed. Does " + filename + " have syntax errors?";
    }

    static void displayParsingErrorNotification(Project project, String filename) {
        FormattingNotificationService.getInstance(project)
                .reportError(
                        Notifications.PARSING_ERROR_NOTIFICATION_GROUP,
                        Notifications.PARSING_ERROR_TITLE,
                        Notifications.parsingErrorMessage(filename));
    }
}
