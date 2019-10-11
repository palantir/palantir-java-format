package com.palantir.javaformat.doc;

/** Clear all breaks inside a level, recursively. */
public enum ClearBreaksVisitor implements DocVisitor<Void> {
  INSTANCE;

  @Override
  public Void visitSpace(Space doc) {
    return null;
  }

  @Override
  public Void visitTok(Tok doc) {
    return null;
  }

  @Override
  public Void visitToken(Token doc) {
    return null;
  }

  @Override
  public Void visitBreak(Break doc) {
    doc.clearBreak();
    return null;
  }

  @Override
  public Void visitLevel(Level doc) {
    doc.getDocs().forEach(this::visit);
    return null;
  }
}
