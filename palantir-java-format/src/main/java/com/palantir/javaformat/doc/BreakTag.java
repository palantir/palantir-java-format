package com.palantir.javaformat.doc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.errorprone.annotations.Immutable;
import java.io.IOException;

/**
 * Unique identifier for a break. A BreakTag can correspond to one or more {@link Break breaks}, and the state of the
 * BreakTag is determined by whether any of the breaks were taken. Then, conditional structures like {@link
 * com.palantir.javaformat.OpsBuilder.BlankLineWanted.ConditionalBlankLine} and {@link
 * com.palantir.javaformat.Indent.If} behave differently based on whether this BreakTag was 'broken' or not.
 *
 * @see State#wasBreakTaken
 */
@Immutable
@JsonSerialize(using = BreakTag.BreakTagSerializer.class)
public final class BreakTag extends HasUniqueId {

    static class BreakTagSerializer extends JsonSerializer<BreakTag> {
        @Override
        public void serialize(
                BreakTag value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(Integer.toString(value.id()));
        }
    }
}
