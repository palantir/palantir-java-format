import React, { useState } from "react";
import { Doc, Indent } from "./Doc";
import { Id } from "./Data";
import { Pre, Tag, Tooltip } from "@blueprintjs/core";

/** Render a {@link Doc} as a "tree" of nested levels. */
export const TreeDocComponent: React.FC<{ doc: Doc }> = ({doc}) => {

    const [highlightedBreakTag, setHighlightedBreakTag] = useState<Id>();

    function renderConstIndent(indent: Indent) {
        switch (indent.type) {
            case "const":
                return <Tag intent={"success"}>+{indent.amount}</Tag>;
            case "if":
                throw Error(`Expected const indent but got ${indent}`)
        }
    }

    /** Render an indent differently, depending on whether it was conditional or not. */
    function renderIndentTag(indent: Indent, evaluatedIndent: number) {
        switch (indent.type) {
            case "const":
                if (indent.amount === 0) {
                    return;
                }
                return <Tag intent={"success"}>+{indent.amount}</Tag>;
            case "if":
                return <Tooltip position={"bottom-right"} content={
                    <span>
                        thenIndent={renderConstIndent(indent.thenIndent)}{" "}
                        elseIndent={renderConstIndent(indent.elseIndent)}
                    </span>}>
                    <Tag intent={"warning"}
                         onMouseEnter={() => setHighlightedBreakTag(indent.condition.id)}
                         onMouseLeave={() => setHighlightedBreakTag(undefined)}>
                        +{evaluatedIndent}
                    </Tag>
                </Tooltip>
        }
    }

    function classForBreakTagId(id: Id) {
        return `break-tag-${id}`;
    }

    function renderDoc(doc: Doc) {
        switch (doc.type) {
            case "break":
                const clazz = doc.optTag !== null
                    ? (`conditional ${classForBreakTagId(doc.optTag.id)}`
                        + (highlightedBreakTag === doc.optTag.id ? ' referenced' : ''))
                    : '';
                if (doc.breakState.broken) {
                    return <span key={doc.id}>
                        <Tooltip content={`New indent: ${doc.breakState.newIndent}`}>
                            <span className={clazz + " doc doc-break taken"}>⏎</span>
                        </Tooltip>
                        <br/>
                    </span>;
                } else {
                    return <span key={doc.id} className={clazz + " doc doc-break"}>
                        ⏎{doc.flat ? `(${doc.flat})` : ''}
                    </span>
                }
            case "level":
                // Skip levels without any contents
                if (doc.flat === '') {
                    return null;
                }
                const plusIndent = doc.openOp.plusIndent;
                const indent = renderIndentTag(plusIndent, doc.evalPlusIndent);
                const debugName = (doc.openOp.debugName !== null)
                    ? <span>"{doc.openOp.debugName}"</span> : null;
                const breakBehaviour = (doc.openOp.breakBehaviour.type !== "breakThisLevel")
                    ? <Tag intent={"primary"}>{doc.openOp.breakBehaviour.type}</Tag> : null;
                const breakabilityIfLastLevel = (doc.openOp.breakabilityIfLastLevel !== "ABORT")
                    ? <Tag intent={"none"}>{doc.openOp.breakabilityIfLastLevel}</Tag> : null;
                // TODO other information about the doc
                return <div className={"doc doc-level"} key={doc.id}>
                    <div className={"banner"}>
                        {indent}
                        {debugName}
                        {breakBehaviour}
                        {breakabilityIfLastLevel}
                    </div>
                    {doc.docs.map(renderDoc)}
                </div>;
            case "comment":
                // Displaying original comment on hover (before formatting / reflowing)
                return <Tooltip key={doc.id} content={<div>Original: <Pre>{doc.flat}</Pre></div>}>
                    <span className={"doc doc-comment highlight"}>{doc.text}</span>
                </Tooltip>;
            case "space":
                return <span key={doc.id} className={"doc doc-space highlight"}>&nbsp;</span>;
            case "token":
                return <span key={doc.id} className={"doc-token highlight"}>{doc.flat}</span>;

        }
    }

    return (
        <Pre className="TreeDoc">{renderDoc(doc)}</Pre>
    );
};