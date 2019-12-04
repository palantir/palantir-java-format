import { Id } from "./Data";
import { Doc, Indent, Level } from "./Doc";
import React, { Dispatch, FunctionComponent, useState } from "react";
import { InlineDocComponent } from "./InlineDoc";
import './TreeAndDoc.css';
import { Callout, Classes, Tag, Tooltip } from "@blueprintjs/core";
import {
    decorators as TreebeardDecorators,
    Treebeard,
    TreebeardProps,
    TreeNode as TreebeardTreeNode,
} from "react-treebeard";
import { State } from "./state";

// FormatterDecisions formatting stuff

type ExplorationNode = {
    type: "exploration", parentId?: Id, id: Id, humanDescription: string, children: ReadonlyArray<LevelNode>,
    startColumn: number, result?: ExplorationResult, incomingState?: State,
};
type LevelNode = {
    type: "level", id: Id, parentId: Id, debugName?: string, flat: string, toString: string, acceptedExplorationId: Id,
    levelId: Id, children: ReadonlyArray<ExplorationNode>, incomingState: State
};

interface ExplorationResult {
    outputLevel: Level;
    finalState: State;
}

export type FormatterDecisions = ExplorationNode;


export interface ITreeState {
    nodes: TreeNode[];
    selectedNodeId?: string,
    /** Treebeard's caching is broken as it mutates its props... Therefore, we force it to only render when we want it to. */
    treeCacheBust: number,
}

/** Treebeard doesn't have typescript bindings, so have to manually specify them. */
interface TreeNode extends TreebeardTreeNode {
    // Custom
    data: NodeData,
}

type ExplorationNodeData = { startColumn: number, result?: ExplorationResult, parentLevelId?: Id, incomingState?: State };
type LevelNodeData = { levelId: Id, incomingState: State };
type NodeData = LevelNodeData | ExplorationNodeData;

/** A doc that should be displayed because it's currently being highlighted in the {@link DecisionTree}. */
type DisplayableLevel = { level: Level, startingColumn: number };
type Highlighted = DisplayableLevel | undefined;


export const TreeAndDoc: React.FC<{ formatterDecisions: FormatterDecisions, doc: Doc }> = props => {
    const [highlighted, setHighlighted] = useState<Highlighted>();
    const [highlightedLevelId, setHighlightedLevelId] = useState<Id>();
    const [selected, setSelected] = useState<NodeData>();

    function formatState(state: State) {
        return <span>{JSON.stringify(state)}</span>
    }

    function formatNodeData(data: NodeData): JSX.Element | undefined {
        if ("levelId" in data) {
            return <table>
                <tbody>
                <tr>
                    <td><Tag intent={"primary"}>Incoming</Tag></td>
                    <td>{formatState(data.incomingState)}</td>
                </tr>
                </tbody>
            </table>
        }
        if (data.result) {
            // We always have an incoming state unless we're the root.
            return <table>
                <tbody>
                <tr>
                    <td><Tag intent={"primary"}>Incoming</Tag></td>
                    <td>{formatState(data.incomingState!!)}</td>
                </tr>
                {data.result
                    ? <tr>
                        <td><Tag intent={"success"}>Result</Tag></td>
                        <td>{formatState(data.result.finalState)}</td>
                    </tr>
                    : null}
                </tbody>
            </table>;
        }
    }

    function nodeInfoBlock() {
        const title = selected !== undefined
            ? "levelId" in selected ? "Level node" : "Exploration node"
            : "<no node selected>";
        return <Callout intent={"none"} title={title} className={"node-info"}>
            {selected !== undefined ? formatNodeData(selected) : null}
        </Callout>;
    }

    return <div className={"TreeAndDoc"}>
        <div className={"column1"}>
            {nodeInfoBlock()}
            <DecisionTree
                formatterDecisions={props.formatterDecisions}
                highlightDoc={setHighlighted}
                highlightLevelId={setHighlightedLevelId}
                select={setSelected}/>
        </div>
        <div className={"InlineDocs"}>
            <InlineDocComponent key={"entire-doc"} doc={props.doc} statingColumn={0} className={"InlineDoc"}
                                highlightedLevelId={highlightedLevelId}/>
            {/* TODO grab this from `selected` */}
            {highlighted !== undefined ? ([
                <Callout intent={"primary"} title={"Rendered exploration output"}/>,
                <InlineDocComponent key={"exploration"} doc={highlighted.level}
                                    statingColumn={highlighted.startingColumn} className={"HighlightInlineDoc"}/>,
            ]) : null
            }
        </div>
    </div>
};


interface DecisionTreeProps {
    formatterDecisions: FormatterDecisions;
    highlightDoc: Dispatch<Highlighted>;
    highlightLevelId: Dispatch<Id | undefined>;
    select: Dispatch<NodeData>;
}

export class DecisionTree extends React.PureComponent<DecisionTreeProps, ITreeState> {
    public state: ITreeState = {
        nodes: DecisionTree.createExplorationNode(this.props.formatterDecisions).children!!,
        treeCacheBust: 0,
    };

    private static readonly duration = 50;
    /** Default TreeBeard animations are too slow (300), sadly have to reimplement this to get a shorter duration. */
    static Animations = {
        toggle: function (_ref: { node: { toggled: boolean } }): { duration: number; animation: { rotateZ: number } } {
            const toggled = _ref.node.toggled;
            const duration = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : DecisionTree.duration;
            return {
                animation: {
                    rotateZ: toggled ? 90 : 0,
                },
                duration: duration,
            };
        },
        drawer: function() {
            return (
                /* props */
                {
                    enter: {
                        animation: 'slideDown',
                        duration: DecisionTree.duration,
                    },
                    leave: {
                        animation: 'slideUp',
                        duration: DecisionTree.duration,
                    },
                }
            );
        }
    };

    private static CastTreebeard<N extends TreeNode>(): FunctionComponent<TreebeardProps<N>> {
        return Treebeard;
    }

    /**
     * Treebeard isn't defined as a memoized thing so we need to.
     * We use this memoization to indicate that only decorators.cacheBust should matter when determining up-to-dateness.
     */
    private static MemoTreebeard: React.NamedExoticComponent<TreebeardProps<TreeNode>> = React.memo(
        DecisionTree.CastTreebeard<TreeNode>(),
        (prevProps, nextProps) =>
            prevProps.decorators.cacheBust === nextProps.decorators.cacheBust);

    public render() {
        return <div className={`${Classes.ELEVATION_0} DecisionTree`}>
            {/* Indicate which line is selected without refreshing the tree, because that is too slow. */}
            {/* See https://medium.learnreact.com/the-style-tag-and-react-24d6dd3ca974 */}
            <style dangerouslySetInnerHTML={{
                __html: `
                    .node-${this.state.selectedNodeId} {
                        background-color: #2B95D6 !important
                    }
                `,
            }}/>
            <DecisionTree.MemoTreebeard
                data={this.state.nodes}
                onToggle={this.onToggle}
                animations={DecisionTree.Animations}
                decorators={this.Decorators()}
            />
        </div>;
    }

    private static createExplorationNode(node: ExplorationNode, parent?: LevelNode): TreeNode {
        const onAcceptedPath = !parent || parent.acceptedExplorationId === node.id;
        const outputLevel = this.getOutputLevelForExploration(node);
        const indent = outputLevel !== undefined
            ? this.renderIndentTag(outputLevel.openOp.plusIndent, outputLevel.evalPlusIndent)
            : null;

        return {
            id: node.id.toString(),
            children: node.children.length > 0
                ? node.children.map(child => DecisionTree.createLevelNode(child, onAcceptedPath))
                : undefined,
            name: <div>
                <Tooltip content={node.id.toString()}>
                    <span className={"explorationNode-description"}>{node.humanDescription}</span>
                </Tooltip>
                {" "}{indent}
            </div>,
            toggled: onAcceptedPath,
            active: onAcceptedPath,
            data: {
                // Store the output level so we can display it
                result: node.result,
                startColumn: node.startColumn,
                parentLevelId: parent !== undefined ? parent.levelId : undefined,
                incomingState: node.incomingState,
            },
        };
    }

    private static renderConstIndent(indent: Indent) {
        switch (indent.type) {
            case "const":
                return <Tag intent={"success"}>+{indent.amount}</Tag>;
            case "if":
                throw Error(`Expected const indent but got ${indent}`)
        }
    }

    /** Render an indent differently, depending on whether it was conditional or not. */
    private static renderIndentTag(indent: Indent, evaluatedIndent?: number) {
        switch (indent.type) {
            case "const":
                if (indent.amount === 0) {
                    return;
                }
                return <Tag intent={"success"}>+{indent.amount}</Tag>;
            case "if":
                return <Tooltip position={"bottom-right"} content={
                    <span>
                        thenIndent={this.renderConstIndent(indent.thenIndent)}{" "}
                        elseIndent={this.renderConstIndent(indent.elseIndent)}
                    </span>}>
                    <Tag intent={"warning"}>
                        {evaluatedIndent !== undefined ? `+${evaluatedIndent}` : '(unknown)'}
                    </Tag>
                </Tooltip>
        }
    }

    private static getOutputLevelForExploration(exploration: ExplorationNode): Level | undefined {
        const result = exploration.result;
        if (result === undefined) return;
        return result.outputLevel;
    }

    private static createLevelNode(node: LevelNode, parentAccepted: boolean): TreeNode {
        return {
            id: node.id.toString(),
            children: node.children.length > 0
                ? node.children.map(child => DecisionTree.createExplorationNode(child, node))
                : undefined,
            name: (
                <div>
                    <Tooltip content={`Node ID: ${node.id.toString()}, Level ID: ${node.levelId.toString()}`}>
                        {node.debugName || node.id}
                    </Tooltip>
                </div>
            ),
            toggled: true,
            active: parentAccepted,
            data: {
                levelId: node.levelId,
                incomingState: node.incomingState,
            },
        };
    }

    private onToggle = (nodeData: TreeNode, toggled: boolean) => {
        nodeData.toggled = toggled;
        this.setState(prevState => ({...prevState, treeCacheBust: prevState.treeCacheBust + 1}));
    };

    private highlightInlineDocLevel(id: Id) {
        this.props.highlightLevelId(id);
    }

    private highlightLevel(nodeData: TreeNode) {
        const data = nodeData.data;
        if ("levelId" in data) {
            this.highlightInlineDocLevel(data.levelId);
        } else {
            // It's an ExplorationNodeData
            if (data.parentLevelId !== undefined) {
                this.highlightInlineDocLevel(data.parentLevelId);
            }
            if (nodeData === this.state.nodes[0]) {
                this.props.highlightDoc(undefined); // no point highlighting the root
            } else if (data.result) {
                // The exploration had a result
                this.props.highlightDoc({
                    level: data.result.outputLevel,
                    startingColumn: data.startColumn,
                });
            } else {
                this.props.highlightDoc(undefined);
            }
        }
        this.props.select(data);
    }

    private onMouseEnter = (nodeData: TreeNode) => {
        this.highlightLevel(nodeData);
        this.setState(prevState => ({...prevState, selectedNodeId: nodeData.id}));
    };

    /**
     * This is a hack to override the Container of {@link TreebeardDecorators} and still have access to the main
     * component object, because we don't control what gets passed to this inner component via props.
     */
    private Container = ((outer: DecisionTree) => class extends TreebeardDecorators.Container {
        render() {
            const {style, decorators, terminal, onClick, node} = this.props;
            return (
                <div
                    onClick={onClick}
                    className={`node-${node.id}`}
                    style={node.active ? {...style.container} : {...style.link}}
                    onMouseEnter={() => outer.onMouseEnter(node)}
                >
                    {!terminal ? this.renderToggle() : null}
                    <decorators.Header node={node} style={style.header}/>
                </div>
            );
        }
    })(this);

    private Decorators = () => Object.assign({}, TreebeardDecorators, {
        Container: this.Container,
        // Force the tree to re-render
        cacheBust: this.state.treeCacheBust,
    });
}
