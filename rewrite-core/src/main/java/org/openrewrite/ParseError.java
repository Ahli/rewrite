/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite;

import lombok.*;
import org.openrewrite.internal.EncodingDetectingInputStream;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Markers;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;

import static java.util.Collections.singletonList;

@Value
public class ParseError implements SourceFile {
    @With
    @EqualsAndHashCode.Include
    @Getter
    UUID id;

    @With
    @Getter
    Markers markers;

    @With
    @Getter
    Path sourcePath;

    @With
    @Getter
    @Nullable
    FileAttributes fileAttributes;

    @Nullable // for backwards compatibility
    @With(AccessLevel.PRIVATE)
    String charsetName;

    @Override
    public Charset getCharset() {
        return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SourceFile withCharset(Charset charset) {
        return withCharsetName(charset.name());
    }

    @With
    @Getter
    boolean charsetBomMarked;

    @With
    @Getter
    @Nullable
    Checksum checksum;

    @With
    String text;

    @Override
    public <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return true;
    }

    public static ParseError build(Parser parser,
                                   Parser.Input input,
                                   @Nullable Path relativeTo,
                                   ExecutionContext ctx,
                                   Throwable t) {
        EncodingDetectingInputStream is = input.getSource(ctx);
        return new ParseError(
                Tree.randomId(),
                new Markers(Tree.randomId(), singletonList(ParseExceptionResult.build(parser, t))),
                input.getRelativePath(relativeTo),
                input.getFileAttributes(),
                parser.getCharset(ctx).name(),
                is.isCharsetBomMarked(),
                null,
                is.readFully()
        );
    }
}
