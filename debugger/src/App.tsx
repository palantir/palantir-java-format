import React, { CSSProperties } from 'react';
import './App.css';
import { Tooltip } from "@blueprintjs/core";

type Op = { type: 'break', conditional: boolean, fillMode: 'UNIFIED' | 'INDEPENDENT' | 'FORCED', toString: string } & HasId
    | { type: 'token', beforeText: string, afterText: string, text: string } & HasId
    | { type: 'openOp', toString: string } & HasId
    | { type: 'closeOp' }

interface DebugData {
    javaInput: string,
    ops: Array<Op>,
    doc: any,
    javaOutput: string,
}

interface Props {
    debugData: DebugData
}

interface HasId {
    id: number
}

const App: React.FC<Props> = ({debugData}) => {
    return (
        <div className="App">
            <h1>javaInput</h1>
            <code>{debugData.javaInput}</code>
            <h1>{"List<Op>"}</h1>
            <p><i>Note: Comment and NonBreakingSpaces are not rendered here. Columns may be misaligned</i></p>
            <code>{renderOps(debugData.ops)}</code>
            <h1>Doc</h1>
            <code></code>
            <h1>javaOutput</h1>
            <code>{debugData.javaOutput}</code>
        </div>
    );
};

function renderOps(ops: Array<Op>) {
    return ops.map(op => {
        switch (op.type) {
            case "break":
                const classes = ["break-tag", `FillMode-${op.fillMode}`];
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
    })
}

function backgroundColor(item: HasId): CSSProperties {
    return {background: `hsl(${item.id % 256}, 60%, 90%)`};
}

export default App;
