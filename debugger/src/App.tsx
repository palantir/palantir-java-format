import React, { CSSProperties, Dispatch, useState } from 'react';
import './App.css';
import { Callout, Classes, H1, Pre, Tag, Toaster, Tooltip } from "@blueprintjs/core";
import { decorators as TreebeardDecorators, Treebeard } from 'react-treebeard';

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

type Token = { type: "token", flat: string } & HasId;
type Space = { type: "space" } & HasId;
type Break = { type: "break", flat: string, breakState: { broken: boolean, newIndent: number }, optTag: HasId | null } & HasId;
type Comment = { type: "comment", /** Original text */ flat: string, /** Text as rendered */ text: string } & HasId;
type Level = {
    type: "level",
    openOp: OpenOp,
    docs: ReadonlyArray<Doc>,
    flat: string,
    evalPlusIndent: number,
    isOneLine: boolean,
} & HasId;
type OpenOp = {
    id: Id, plusIndent: Indent, breakBehaviour: BreakBehaviour, breakabilityIfLastLevel: string,
    columnLimitBeforeLastBreak?: any, debugName?: string
};
type BreakBehaviour =
    { type: "breakThisLevel" }
    | { type: "preferBreakingLastInnerLevel" }
    | { type: "breakOnlyIfInnerLevelsThenFitOnOneLine" }
type Indent = { type: "const", amount: number } | { type: "if", condition: HasId, thenIndent: Indent, elseIndent: Indent }

// FormatterDecisions formatting stuff

type ExplorationNode = {
    type: "exploration", parentId?: Id, id: Id, humanDescription: string, children: ReadonlyArray<LevelNode>,
    outputLevel?: Level, startColumn: number };
type LevelNode = {
    type: "level", id: Id, parentId: Id, debugName?: string, flat: string, toString: string, acceptedExplorationId: Id,
    levelId: Id, children: ReadonlyArray<ExplorationNode>,
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
            <TreeDocComponent doc={debugData.doc}/>
            <H1>Exploration</H1>
            <TreeAndDoc formatterDecisions={debugData.formatterDecisions} doc={debugData.doc}/>
            <H1>javaOutput</H1>
            <Pre>{debugData.javaOutput}</Pre>
        </div>
    );
};

/** Render a {@link Doc} with the same newlines as the final result. */
const InlineDocComponent: React.FC<{
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
                return <span key={doc.id} className={`doc doc-level ${doc.id === highlightedLevelId ? "referenced" : ""}`}>
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

/** Render a {@link Doc} as a "tree" of nested levels. */
const TreeDocComponent: React.FC<{doc: Doc}> = ({doc}) => {

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
    nodes: TreeNode[];
    selectedNodeId?: string,
}

/** Treebeard doesn't have typescript bindings, so have to manually specify them. */
interface TreeNode {
    id: string,
    name: JSX.Element | string,
    children?: Array<TreeNode>,
    toggled?: boolean,
    active?: boolean,
    loading?: boolean,
    // Custom
    data: NodeData,
}

type ExplorationNodeData = { outputLevel?: DisplayableLevel, parentLevelId?: Id };
type LevelNodeData = { levelId: Id };
type NodeData = LevelNodeData | ExplorationNodeData;

/** A doc that should be displayed because it's currently being highlighted in the {@link DecisionTree}. */
type DisplayableLevel = { level: Level, startingColumn: number };
type Highlighted = DisplayableLevel | undefined;


const TreeAndDoc: React.FC<{ formatterDecisions: FormatterDecisions, doc: Doc }> = props => {
    const [highlighted, setHighlighted] = useState<Highlighted>();
    const [highlightedLevelId, setHighlightedLevelId] = useState<Id>();

    return <div className={"TreeAndDoc"}>
        <DecisionTree
                formatterDecisions={props.formatterDecisions}
                highlightDoc={setHighlighted}
                highlightLevelId={setHighlightedLevelId}/>
        <div className={"InlineDocs"}>
            <InlineDocComponent key={"entire-doc"} doc={props.doc} statingColumn={0} className={"InlineDoc"}
                highlightedLevelId={highlightedLevelId}/>
            {highlighted !== undefined ? ([
                <Callout intent={"primary"} title={"Rendered exploration output"}/>,
                <InlineDocComponent key={"exploration"} doc={highlighted.level} statingColumn={highlighted.startingColumn} className={"HighlightInlineDoc"}/>
            ]) : null}
        </div>
    </div>
};


export class DecisionTree extends React.Component<{
            formatterDecisions: FormatterDecisions,
            highlightDoc: Dispatch<Highlighted>,
            highlightLevelId: Dispatch<Id | undefined>,
        }, ITreeState> {
    public state: ITreeState = { nodes: DecisionTree.createExplorationNode(this.props.formatterDecisions).children!! };
    private static toaster = Toaster.create();

    private static readonly duration = 50;
    /** Default TreeBeard animations are too slow (300), sadly have to reimplement this to get a shorter duration. */
    static Animations = {
        toggle: function(_ref: {node: { toggled: boolean }}): { duration: number; animation: { rotateZ: number } } {
            const toggled = _ref.node.toggled;
            const duration = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : DecisionTree.duration;
            return {
                animation: {
                    rotateZ: toggled ? 90 : 0
                },
                duration: duration
            };
        },
        drawer: function() {
            return (
                /* props */
                {
                    enter: {
                        animation: 'slideDown',
                        duration: DecisionTree.duration
                    },
                    leave: {
                        animation: 'slideUp',
                        duration: DecisionTree.duration
                    }
                }
            );
        }
    };

    public render() {
        return <div className={`${Classes.ELEVATION_0} DecisionTree`}>
            <Treebeard
                data={this.state.nodes}
                onToggle={this.onToggle}
                animations={DecisionTree.Animations}
                decorators={this.Decorators}
            />
        </div>;
    }

    private static createExplorationNode(node: ExplorationNode, parent?: LevelNode): TreeNode {
        const onAcceptedPath = !parent || parent.acceptedExplorationId === node.id;
        return {
            id: node.id.toString(),
            children: node.children.length > 0
                ? node.children.map(child => DecisionTree.createLevelNode(child, onAcceptedPath))
                : undefined,
            name: <Tooltip content={node.id.toString()}>{node.humanDescription}</Tooltip>,
            toggled: onAcceptedPath,
            active: onAcceptedPath,
            // Store the output level so we can display it
            data: {
                outputLevel: node.outputLevel !== undefined ? {
                    level: node.outputLevel,
                    // TODO probably makes sense to steal startColumn from the `parent` too (needs change to JsonSink)
                    startingColumn: node.startColumn,
                } : undefined,
                parentLevelId: parent !== undefined ? parent.levelId : undefined
            },
        };
    }

    private static createLevelNode(node: LevelNode, parentAccepted: boolean): TreeNode {
        return {
            id: node.id.toString(),
            children: node.children.length > 0
                ? node.children.map(child => DecisionTree.createExplorationNode(child, node))
                : undefined,
            name: (
                <Tooltip content={node.id.toString()}>{node.debugName || node.id}</Tooltip>
            ),
            // secondaryLabel: node.toString,
            toggled: true,
            active: parentAccepted,
            data: {
                levelId: node.levelId,
            },
        };
    }

    private onToggle = (nodeData: TreeNode, toggled: boolean) => {
        nodeData.toggled = toggled;
        this.setState(this.state);
    };

    private highlightInlineDocLevel(id: Id) {
        this.props.highlightLevelId(id);
    }

    private highlightLevel(nodeData: TreeNode) {
        if ("levelId" in nodeData.data) {
            this.highlightInlineDocLevel(nodeData.data.levelId);
        } else {
            if (nodeData.data.parentLevelId !== undefined) {
                this.highlightInlineDocLevel(nodeData.data.parentLevelId);
            }
            if (nodeData === this.state.nodes[0]) {
                this.props.highlightDoc(undefined); // no point highlighting the root
            } else {
                this.props.highlightDoc(nodeData.data.outputLevel);
            }
        }
    }

    private onMouseEnter = (nodeData: TreeNode) => {
        this.state.selectedNodeId = nodeData.id;
        // TODO select this node somehow so it's obvious that it's highlighted
        this.highlightLevel(nodeData);
        this.setState(this.state);
    };

    /**
     * This is a hack to override the Container of {@link TreebeardDecorators} and still have access to the main
     * component object, because we don't control what gets passed to this inner component via props.
     */
    private Container = (outer: DecisionTree) => class extends TreebeardDecorators.Container {

        render() {
            const {style, decorators, terminal, onClick, node} = this.props;
            return (
                <div
                    onClick={onClick}
                    style={outer.state.selectedNodeId === node.id
                        ? {backgroundColor: '#2B95D6'}
                        : (node.active ? {...style.container} : {...style.link})
                    }
                    onMouseEnter={() => outer.onMouseEnter(node)}
                >
                    {!terminal ? this.renderToggle() : null}
                    <decorators.Header node={node} style={style.header}/>
                </div>
            );
        }
    };

    private Decorators = Object.assign({}, TreebeardDecorators, {
        Container: this.Container(this)
    });
}

function backgroundColor(item: HasId): CSSProperties {
    return {background: `hsl(${item.id % 256}, 60%, 90%)`};
}

export default App;
