import { Id } from "./Data";
import { Doc, Level } from "./Doc";
import React, { Dispatch, useState } from "react";
import { InlineDocComponent } from "./InlineDoc";
import { Callout, Classes, Tag, Toaster, Tooltip } from "@blueprintjs/core";
import { decorators as TreebeardDecorators, Treebeard } from "react-treebeard";
import { State } from "./state";

// FormatterDecisions formatting stuff

type ExplorationNode = {
    type: "exploration", parentId?: Id, id: Id, humanDescription: string, children: ReadonlyArray<LevelNode>,
    startColumn: number, result?: ExplorationResult
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

    return <div className={"TreeAndDoc"}>
        <div className={"column1"}>
            <Callout intent={"none"} title={"Node information"} className={"node-info"}>
                {selected !== undefined ? formatNodeData(selected) : null}
            </Callout>
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
    public state: ITreeState = {nodes: DecisionTree.createExplorationNode(this.props.formatterDecisions).children!!};
    private static toaster = Toaster.create();

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
        drawer: function () {
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
        },
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
            data: {
                // Store the output level so we can display it
                result: node.result,
                startColumn: node.startColumn,
                parentLevelId: parent !== undefined ? parent.levelId : undefined,
                incomingState: parent !== undefined ? parent.incomingState : undefined,
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
        this.setState(this.state);
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
        this.setState({...this.state, selectedNodeId: nodeData.id});
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
                        ? {backgroundColor: '#2B95D6', ...style.link}
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
        Container: this.Container(this),
    });
}
