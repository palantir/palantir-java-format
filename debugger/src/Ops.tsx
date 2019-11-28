import React from "react";
import { Pre, Tooltip } from "@blueprintjs/core";
import { HasId } from "./Data";
import { backgroundColor } from "./common";

export type Op =
// TODO associated BreakTag id if conditional
    { type: 'break', conditional: boolean, fillMode: 'UNIFIED' | 'INDEPENDENT' | 'FORCED', toString: string } & HasId
    | { type: 'token', beforeText: string, afterText: string, text: string } & HasId
    | { type: 'openOp', toString: string } & HasId
    | { type: 'closeOp' }
export const Ops: React.FC<{ ops: Array<Op> }> = ({ops}) => {
    const renderOps = ops.map(op => {
        switch (op.type) {
            case "break":
                const classes = ["break", `FillMode-${op.fillMode}`];
                if (op.conditional) {
                    classes.push('conditional')
                }
                return <Tooltip content={op.toString}><span className={classes.join(" ")}/></Tooltip>;
            case "token":
                return <span className={"token"} style={backgroundColor(op)}>
                        {op.beforeText}
                    <span className={"tokenBody"}>{op.text}</span>
                    {op.afterText}
                    </span>;
            case "openOp":
                return <Tooltip content={op.toString}>
                    <span className={"open-op"} key={op.id}/>
                </Tooltip>;
            case "closeOp":
                return <span className={"close-op"}/>;
        }
    });

    return <Pre className={"Ops"}>{renderOps}</Pre>;
};