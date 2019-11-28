import { HasId } from "./Data";
import { CSSProperties } from "react";

export function backgroundColor(item: HasId): CSSProperties {
    return {background: `hsl(${item.id % 256}, 60%, 90%)`};
}