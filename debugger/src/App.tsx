import React, { CSSProperties } from 'react';
import './App.css';
import { Callout, Classes, H1, ITreeNode, Pre, Toaster, Tooltip, Tree } from "@blueprintjs/core";

interface DebugData {
    javaInput: string,
    ops: Array<Op>,
    doc: Doc,
    formatterDecisions: FormatterDecisions,
    javaOutput: string,
}

// Op stuff

type Op =
    // TODO associated BreakTag id if conditional
    { type: 'break', conditional: boolean, fillMode: 'UNIFIED' | 'INDEPENDENT' | 'FORCED', toString: string } & HasId
    | { type: 'token', beforeText: string, afterText: string, text: string } & HasId
    | { type: 'openOp', toString: string } & HasId
    | { type: 'closeOp' }

// Doc stuff

type Doc = Break | Level | Comment | Space | Token;

type Token = { type: "token", flat: string };
type Space = { type: "space" };
type Break = { type: "break", flat: string, breakState: { broken: boolean, newIndent: number }, optTag: HasId };
type Comment = { type: "comment", /** Original text */ flat: string, /** Text as rendered */ text: string };
type Level = {
    type: "level",
    openOp: OpenOp,
    id: Id,
    docs: ReadonlyArray<Doc>,
};
type OpenOp = {
    id: Id, plusIndent: Indent, breakBehaviour: any, breakabilityIfLastLevel: any, columnLimitBeforeLastBreak?: any,
    debugName?: string
};
type Indent = { type: "const", amount: number } | { type: "if", condition: Id, thenIndent: Indent, elseIndent: Indent }

// FormatterDecisions formatting stuff

type ExplorationNode = {
    type: "exploration", parentId?: Id, id: Id, humanDescription: string, children: ReadonlyArray<LevelNode> };
type LevelNode = {
    type: "level", id: Id, parentId: Id, debugName?: string, flat: string, toString: string, acceptedExplorationId: Id,
    children: ReadonlyArray<ExplorationNode>
};

type FormatterDecisions = ExplorationNode;

// Main inputs and types

interface Props {
    debugData: DebugData
}

type Id = number;

interface HasId {
    id: Id
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
            <DocComponent doc={debugData.doc}/>
            <H1>Exploration</H1>
            <DecisionTree formatterDecisions={debugData.formatterDecisions}/>
            <H1>javaOutput</H1>
            <Pre>{debugData.javaOutput}</Pre>
        </div>
    );
};

const DocComponent: React.FC<{doc: Doc}> = ({doc}) => {
    function renderDoc(doc: Doc) {
        switch (doc.type) {
            case "break":
                // TODO add breakToken in here
                if (doc.breakState.broken) {
                    return <br className={"doc doc-break"}/>;
                } else {
                    return <span className={"doc doc-break"}>{doc.flat}</span>
                }
            case "level":
                // TODO other information about the doc
                return <div className={"doc doc-level"}>
                    {doc.docs.map(renderDoc)}
                </div>;
            case "comment":
                // TODO maybe display original on hover?
                return <span className={"doc doc-comment"}>{doc.text}</span>;
            case "space":
                return <span className={"doc doc-space"}>&nbsp;</span>;
            case "token":
                return <span className="doc-token">{doc.flat}</span>;

        }
    }

    return (
        <Pre>{renderDoc(doc)}</Pre>
    );

};

const Ops: React.FC<{ops: Array<Op>}> = ({ops}) => {
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

export interface ITreeState {
    nodes: ITreeNode[];
}

export class DecisionTree extends React.Component<{ formatterDecisions: FormatterDecisions }, ITreeState> {
    public state: ITreeState = { nodes: DecisionTree.createExplorationNode(this.props.formatterDecisions).childNodes!! };
    private static toaster = Toaster.create();

    public render() {
        return <Tree
            contents={this.state.nodes}
            onNodeClick={this.handleNodeClick}
            onNodeCollapse={this.handleNodeCollapse}
            onNodeExpand={this.handleNodeExpand}
            className={Classes.ELEVATION_0}
        />;
    }

    private static createExplorationNode(node: ExplorationNode, parent?: LevelNode): ITreeNode {
        return {
            id: node.id,
            childNodes: node.children.map(DecisionTree.createLevelNode),
            label: <Tooltip content={node.id.toString()}>{node.humanDescription}</Tooltip>,
            isExpanded: !parent || parent.acceptedExplorationId === node.id,
        };
    }

    private static createLevelNode(node: LevelNode): ITreeNode {
        return {
            id: node.id,
            childNodes: node.children.length > 0
                    ? node.children.map(child => DecisionTree.createExplorationNode(child, node))
                    : undefined,
            label: (
                <Tooltip content={node.id.toString()}>{node.debugName || node.id}</Tooltip>
            ),
            // secondaryLabel: node.toString,
            isExpanded: true,
        };
    }

    private handleNodeClick = (nodeData: ITreeNode, nodePath: number[], e: React.MouseEvent<HTMLElement>) => {
        DecisionTree.toaster.show({message: `Clicked on ${nodePath}`});
        const originallyExpanded = nodeData.isExpanded;
        nodeData.isExpanded = originallyExpanded == null ? true : !originallyExpanded;
        this.setState(this.state);
    };

    private handleNodeCollapse = (nodeData: ITreeNode) => {
        nodeData.isExpanded = false;
        this.setState(this.state);
    };

    private handleNodeExpand = (nodeData: ITreeNode) => {
        nodeData.isExpanded = true;
        this.setState(this.state);
    };

    private forEachNode(nodes: ITreeNode[], callback: (node: ITreeNode) => void) {
        if (nodes == null) {
            return;
        }

        for (const node of nodes) {
            callback(node);
            if (node.childNodes === undefined) {
                return;
            }
            this.forEachNode(node.childNodes, callback);
        }
    }
}

function backgroundColor(item: HasId): CSSProperties {
    return {background: `hsl(${item.id % 256}, 60%, 90%)`};
}

export default App;
