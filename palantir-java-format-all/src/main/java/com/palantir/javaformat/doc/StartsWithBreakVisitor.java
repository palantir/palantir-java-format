/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.javaformat.doc;

import com.palantir.javaformat.doc.StartsWithBreakVisitor.Result;

enum StartsWithBreakVisitor implements DocVisitor<Result> {
    INSTANCE;

    enum Result {
        EMPTY,
        NO,
        YES,
    }

    @Override
    public Result visitSpace(NonBreakingSpace doc) {
        return Result.NO;
    }

    @Override
    public Result visitComment(Comment doc) {
        return Result.NO;
    }

    @Override
    public Result visitToken(Token doc) {
        return Result.NO;
    }

    @Override
    public Result visitBreak(Break doc) {
        return Result.YES;
    }

    @Override
    public Result visitLevel(Level doc) {
        return doc.getDocs().stream()
                .map(this::visit)
                .filter(result -> result != Result.EMPTY)
                .findFirst()
                .orElse(Result.EMPTY);
    }
}
