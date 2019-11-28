import { HasId, Id } from "./Data";

export type Doc = Break | Level | Comment | Space | Token;

export type Token = { type: "token", flat: string } & HasId;
export type Space = { type: "space" } & HasId;
export type Break =
    { type: "break", flat: string, breakState: { broken: boolean, newIndent: number }, optTag: HasId | null }
    & HasId;
export type Comment =
    { type: "comment", /** Original text */ flat: string, /** Text as rendered */ text: string }
    & HasId;
export type Level = {
    type: "level",
    openOp: OpenOp,
    docs: ReadonlyArray<Doc>,
    flat: string,
    evalPlusIndent: number,
    isOneLine: boolean,
} & HasId;
export type OpenOp = {
    id: Id, plusIndent: Indent, breakBehaviour: BreakBehaviour, breakabilityIfLastLevel: string,
    columnLimitBeforeLastBreak?: any, debugName?: string
};
export type BreakBehaviour =
    { type: "breakThisLevel" }
    | { type: "preferBreakingLastInnerLevel" }
    | { type: "breakOnlyIfInnerLevelsThenFitOnOneLine" }
export type Indent =
    { type: "const", amount: number }
    | { type: "if", condition: HasId, thenIndent: Indent, elseIndent: Indent }
