package com.palantir.javaformat;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.errorprone.annotations.Immutable;
import com.palantir.javaformat.doc.Doc;
import com.palantir.javaformat.doc.Level;
import java.io.IOException;
import org.derive4j.ArgOption;
import org.derive4j.Data;

@Data(arguments = ArgOption.checkedNotNull)
@Immutable
@JsonSerialize(using = BreakBehaviour.Json.class)
public abstract class BreakBehaviour {
    public interface Cases<R> {

        R breakThisLevel();

        /**
         * If the last level is breakable, prefer breaking it if it will keep the rest of this level on line line.
         *
         * @param keepIndentWhenInlined whether to keep this level's indent when inlined as a recursive level (when
         *     reached via a previous `preferBreakingLastInnerLevel` whose breakability was
         *     {@link LastLevelBreakability#CHECK_INNER})
         */
        R preferBreakingLastInnerLevel(boolean keepIndentWhenInlined);

        /**
         * Attempt to inline the suffix of this level (which must be a {@link Level} and the last doc), recursing into
         * the {@link Level} just before the last {@link Level} (if there is such a level) to see if that can be broken
         * instead.
         *
         * <p>This behaves like {@link #breakThisLevel()} if we couldn't recurse into such an inner level, or if the
         * suffix level doesn't fit on the last line.
         */
        R inlineSuffix();

        /**
         * Break if by doing so all inner levels then fit on a single line. However, don't break if we can fit in the
         * {@link Doc docs} up to the first break (which might be nested inside the next doc if it's a {@link Level}),
         * in order to prevent exceeding the maxLength accidentally.
         */
        R breakOnlyIfInnerLevelsThenFitOnOneLine(boolean keepIndentWhenInlined);
    }

    public abstract <R> R match(Cases<R> cases);

    /** For {@link com.palantir.javaformat.doc.LevelDelimitedFlatValueDocVisitor}. */
    @Override
    public abstract String toString();

    /**
     * This is gross but just wanted to get something working. See https://github.com/derive4j/derive4j/issues/51 for a
     * potential better implementation.
     */
    static class Json extends JsonSerializer<BreakBehaviour> {

        @Override
        public void serialize(BreakBehaviour value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            gen.writeStartObject();
            BreakBehaviours.caseOf(value)
                    .breakThisLevel(() -> {
                        try {
                            gen.writeObjectField("type", "breakThisLevel");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    })
                    .preferBreakingLastInnerLevel(keepIndentWhenInlined -> {
                        try {
                            gen.writeObjectField("type", "preferBreakingLastInnerLevel");
                            gen.writeObjectField("keepIndentWhenInlined", keepIndentWhenInlined);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    })
                    .inlineSuffix(() -> {
                        try {
                            gen.writeObjectField("type", "inlineSuffix");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    })
                    .breakOnlyIfInnerLevelsThenFitOnOneLine(keepIndentWhenInlined -> {
                        try {
                            gen.writeObjectField("type", "breakOnlyIfInnerLevelsThenFitOnOneLine");
                            gen.writeObjectField("keepIndentWhenInlined", keepIndentWhenInlined);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        return null;
                    });
            gen.writeEndObject();
        }
    }
}
