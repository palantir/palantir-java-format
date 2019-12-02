/// <reference types="react" />


declare module 'react-treebeard' {
    import { CSSProperties, PureComponent, ReactComponentElement } from "react";

    export let Treebeard: ReactComponentElement;

    interface TreebeardProps<Node extends TreeNode> {
        decorators: Decorators;
        data: Node[];
        onToggle: (node: Node, toggled: boolean) => void;
        animations: any;
    }

    interface TreeNode {
        id: string,
        name: JSX.Element | string,
        children?: Array<this>,
        toggled?: boolean,
        active?: boolean,
        loading?: boolean,
    }

    type Style = any;

    type ContainerProps = {
        style: Style,
        decorators: Decorators,
        terminal: boolean,
        onClick: (...args: any[]) => any,
        animations: Object | boolean,
        node: any
    };

    export interface Container extends PureComponent<ContainerProps> {
        renderToggle(): JSX.Element;

        // So we can extend it
        // new<P>(props: P): Container<P>;

        new(): Container;
    }

    export let decorators: Decorators;

    interface Decorators {
        // I don't know what type to give this so that I can 'extend' from it in App.tsx
        // declaring it via `declare class Container extends React.Component` also doesn't seem to work
        Container: Container;
        Header: Header;
        // Hack we added so we can control when treebeard refreshes
        cacheBust: number,
    }

    interface Header extends PureComponent<{node: Object, style: CSSProperties}> {
        new(): Header;
    }
}
