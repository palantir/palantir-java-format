package com.palantir.javaformat;

import com.palantir.javaformat.Doc.Break;
import com.palantir.javaformat.Doc.Level;
import com.palantir.javaformat.Doc.Space;
import com.palantir.javaformat.Doc.Tok;
import com.palantir.javaformat.Doc.Token;
import com.palantir.javaformat.StartsWithBreakVisitor.Result;

class StartsWithBreakVisitor implements DocVisitor<Result> {

  enum Result {
    EMPTY,
    NO,
    YES,
  }

  @Override
  public Result visitSpace(Space doc) {
    return Result.NO;
  }

  @Override
  public Result visitTok(Tok doc) {
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
