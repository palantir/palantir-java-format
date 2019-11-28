import React from "react";
import { Pre } from "@blueprintjs/core";
import { Doc } from "./Doc";
import { Id } from "./Data";

/** Render a {@link Doc} with the same newlines as the final result. */
export const InlineDocComponent: React.FC<{
    doc: Doc,
    statingColumn: number,
    className: string,
    highlightedLevelId?: Id
}> = ({doc, statingColumn, className, highlightedLevelId}) => {
    function renderDoc(doc: Doc) {
        switch (doc.type) {
            case "break":
                // TODO add breakToken in here
                if (doc.breakState.broken) {
                    return <span key={doc.id} className={"doc doc-break taken highlight"}>
                        <br/>{' '.repeat(doc.breakState.newIndent)}
                    </span>;
                } else {
                    return <span key={doc.id} className={"doc doc-break highlight"}>{doc.flat}</span>
                }
            case "level":
                // TODO other information about the doc
                return <span key={doc.id}
                             className={`doc doc-level ${doc.id === highlightedLevelId ? "referenced" : ""}`}>
                    {doc.docs.map(renderDoc)}
                </span>;
            case "comment":
                // TODO maybe display original on hover?
                return <span key={doc.id} className={"doc doc-comment highlight"}>{doc.text}</span>;
            case "space":
                return <span key={doc.id} className={"doc doc-space highlight"}>&nbsp;</span>;
            case "token":
                return <span key={doc.id} className={"doc-token highlight"}>{doc.flat}</span>;
        }
    }

    return (
        <Pre className={className}>
            {' '.repeat(statingColumn)}
            {renderDoc(doc)}
        </Pre>
    );
};