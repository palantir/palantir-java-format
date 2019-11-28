import React from 'react';
import './App.css';
import { Callout, H1, Pre } from "@blueprintjs/core";
import { Doc } from "./Doc";
import { Op, Ops } from "./Ops";
import { TreeDocComponent } from "./TreeDoc";
import { FormatterDecisions, TreeAndDoc } from "./TreeAndDoc";

interface DebugData {
    javaInput: string,
    ops: Array<Op>,
    doc: Doc,
    formatterDecisions: FormatterDecisions,
    javaOutput: string,
}

// Main inputs and types

interface Props {
    debugData: DebugData
}

const App: React.FC<Props> = ({debugData}) => {
    return (
        <div className="App">
            <H1>javaInput</H1>
            <Pre>{debugData.javaInput}</Pre>
            <H1>{"List<Op>"}</H1>
            <Callout title="Note">
                Comment and NonBreakingSpaces are not rendered here. Columns may be misaligned
            </Callout>
            <Ops ops={debugData.ops}/>
            <H1>Doc</H1>
            <TreeDocComponent doc={debugData.doc}/>
            <H1>Exploration</H1>
            <TreeAndDoc formatterDecisions={debugData.formatterDecisions} doc={debugData.doc}/>
            <H1>javaOutput</H1>
            <Pre>{debugData.javaOutput}</Pre>
        </div>
    );
};

export default App;
