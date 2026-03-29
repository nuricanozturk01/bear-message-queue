import "./shared/binding-BLUhc7Nl.mjs";
import { C as Plugin, It as MaybePromise } from "./shared/define-config-Cj8yDgkV.mjs";

//#region src/plugin/parallel-plugin-implementation.d.ts
type ParallelPluginImplementation = Plugin;
type Context = {
  /**
  * Thread number
  */
  threadNumber: number;
};
declare function defineParallelPluginImplementation<Options>(plugin: (Options: Options, context: Context) => MaybePromise<ParallelPluginImplementation>): (Options: Options, context: Context) => MaybePromise<ParallelPluginImplementation>;
//#endregion
export { type Context, type ParallelPluginImplementation, defineParallelPluginImplementation };