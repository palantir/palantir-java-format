import React from 'react';
import './App.css';

type Op = { type: 'break', conditional: boolean, fillMode: 'UNIFIED' | 'INDEPENDNT' | 'FORCED' }
    | { type: 'token', beforeText: string, afterText: string, text: string }
    | { type: 'openOp' }
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

const App: React.FC<Props> = ({debugData}) => {
    function renderOps(ops: Array<Op>) {
        return ops.map(op => {
            switch (op.type) {
                case "break":
                    const classes = ["break-tag", `FillMode-${op.fillMode}`];
                    if (op.conditional) {
                        classes.push('conditional')
                    }
                    return <span className={classes.join(" ")}/>;
                case "token":
                    return <span className={"token"}>
                        {op.beforeText}
                        <span className={"tokenBody"}>{op.text}</span>
                        {op.afterText}
                    </span>;
                case "openOp":
                    return <span className={"open-op"}/>;
                case "closeOp":
                    return <span className={"close-op"}/>;
            }
        })
    }

    return (
        <div className="App">
            <h1>javaInput</h1>
            <code>{debugData.javaInput}</code>
            <h1>{"List<Op>"}</h1>
            {renderOps(debugData.ops)}
            <h1>Doc</h1>
            <h1>javaOutput</h1>
            {debugData.javaOutput}
        </div>
    );
}

export default App;
