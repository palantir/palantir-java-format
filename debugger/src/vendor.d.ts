/// <reference types="react" />


declare module 'react-treebeard' {
    import { ReactComponentElement } from "react";

    export let Treebeard: ReactComponentElement;
    export let decorators: {
        // I don't know what type to give this so that I can 'extend' from it in App.tsx
        // declaring it via `declare class Container extends React.Component` also doesn't seem to work
        Container: any;
    };
}
