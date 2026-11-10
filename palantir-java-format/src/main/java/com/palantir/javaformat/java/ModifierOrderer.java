/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.palantir.javaformat.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeMap;
import com.palantir.javaformat.Input.Tok;
import com.palantir.javaformat.Input.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;

/** Fixes sequences of modifiers to be in JLS order. */
final class ModifierOrderer {

    /** Reorders all modifiers in the given text to be in JLS order. */
    static JavaInput reorderModifiers(String text) throws FormatterException {
        return reorderModifiers(new JavaInput(text), ImmutableList.of(Range.closedOpen(0, text.length())));
    }

    /** Reorders all modifiers in the given text and within the given character ranges to be in JLS order. */
    static JavaInput reorderModifiers(JavaInput javaInput, Collection<Range<Integer>> characterRanges)
            throws FormatterException {
        if (javaInput.getTokens().isEmpty()) {
            // There weren't any tokens, possible because of a lexing error.
            // Errors about invalid input will be reported later after parsing.
            return javaInput;
        }
        RangeSet<Integer> tokenRanges = javaInput.characterRangesToTokenRanges(characterRanges);
        Iterator<? extends Token> it = javaInput.getTokens().iterator();
        TreeRangeMap<Integer, String> replacements = TreeRangeMap.create();
        while (it.hasNext()) {
            ModifierTokens mod = nextModifier(it);
            if (mod == null || !tokenRanges.contains(mod.first.getTok().getIndex())) {
                continue;
            }

            List<ModifierTokens> mods = new ArrayList<>();

            int begin = mod.first.getTok().getPosition();
            mods.add(mod);

            int end = -1;
            while (it.hasNext()) {
                mod = nextModifier(it);
                if (mod == null) {
                    break;
                }
                mods.add(mod);
                end = mod.last.getTok().getPosition() + mod.last.getTok().length();
            }

            if (!Ordering.natural().isOrdered(mods)) {
                List<ModifierTokens> sorted = Ordering.natural().sortedCopy(mods);
                StringBuilder replacement = new StringBuilder();
                for (int i = 0; i < sorted.size(); i++) {
                    if (i > 0) {
                        addTrivia(replacement, mods.get(i).first.getToksBefore());
                    }
                    replacement.append(sorted.get(i).modifier.toString());
                    if (i < (sorted.size() - 1)) {
                        addTrivia(replacement, mods.get(i).last.getToksAfter());
                    }
                }
                replacements.put(Range.closedOpen(begin, end), replacement.toString());
            }
        }
        return applyReplacements(javaInput, replacements);
    }

    /**
     * The tokens that make up a modifier. This is usually a single token (e.g. for {@code public}), but
     * {@code non-sealed} is lexed as three tokens.
     */
    private static final class ModifierTokens implements Comparable<ModifierTokens> {
        private final Modifier modifier;
        private final Token first;
        private final Token last;

        ModifierTokens(Modifier modifier, Token first, Token last) {
            this.modifier = modifier;
            this.first = first;
            this.last = last;
        }

        @Override
        public int compareTo(ModifierTokens other) {
            return modifier.compareTo(other.modifier);
        }
    }

    /** Consumes the next modifier and returns its tokens, or returns {@code null} if the next token isn't a modifier. */
    @Nullable
    private static ModifierTokens nextModifier(Iterator<? extends Token> it) {
        Token first = it.next();
        if (!first.getTok().getText().equals("non")) {
            Modifier modifier = asModifier(first);
            return modifier == null ? null : new ModifierTokens(modifier, first, first);
        }
        if (it.hasNext() && it.next().getTok().getText().equals("-") && it.hasNext()) {
            Token last = it.next();
            if (last.getTok().getText().equals("sealed")) {
                return new ModifierTokens(Modifier.valueOf("NON_SEALED"), first, last);
            }
        }
        return null;
    }

    private static void addTrivia(StringBuilder replacement, ImmutableList<? extends Tok> toks) {
        for (Tok tok : toks) {
            replacement.append(tok.getText());
        }
    }

    /**
     * Returns the given token as a {@link javax.lang.model.element.Modifier}, or {@code null} if it is not a modifier.
     */
    @SuppressWarnings("for-rollout:NullAway")
    private static Modifier asModifier(Token token) {
        TokenKind kind = ((JavaInput.Tok) token.getTok()).kind();
        if (kind != null) {
            switch (kind) {
                case PUBLIC:
                    return Modifier.PUBLIC;
                case PROTECTED:
                    return Modifier.PROTECTED;
                case PRIVATE:
                    return Modifier.PRIVATE;
                case ABSTRACT:
                    return Modifier.ABSTRACT;
                case STATIC:
                    return Modifier.STATIC;
                case DEFAULT:
                    return Modifier.DEFAULT;
                case FINAL:
                    return Modifier.FINAL;
                case TRANSIENT:
                    return Modifier.TRANSIENT;
                case VOLATILE:
                    return Modifier.VOLATILE;
                case SYNCHRONIZED:
                    return Modifier.SYNCHRONIZED;
                case NATIVE:
                    return Modifier.NATIVE;
                case STRICTFP:
                    return Modifier.STRICTFP;
                default: // fall out
            }
        }
        switch (token.getTok().getText()) {
            case "sealed":
                return Modifier.valueOf("SEALED");
            default:
                return null;
        }
    }

    /** Applies replacements to the given string. */
    private static JavaInput applyReplacements(JavaInput javaInput, TreeRangeMap<Integer, String> replacementMap)
            throws FormatterException {
        // process in descending order so the replacement ranges aren't perturbed if any replacements
        // differ in size from the input
        Map<Range<Integer>, String> ranges = replacementMap.asDescendingMapOfRanges();
        if (ranges.isEmpty()) {
            return javaInput;
        }
        StringBuilder sb = new StringBuilder(javaInput.getText());
        for (Map.Entry<Range<Integer>, String> entry : ranges.entrySet()) {
            Range<Integer> range = entry.getKey();
            sb.replace(range.lowerEndpoint(), range.upperEndpoint(), entry.getValue());
        }
        return new JavaInput(sb.toString());
    }
}
