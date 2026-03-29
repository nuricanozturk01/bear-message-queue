import {
  __name
} from "./chunk-GSAY5GLT.js";

// node_modules/mermaid/dist/chunks/mermaid.core/chunk-RGXPSUNZ.mjs
var ImperativeState = class {
  /**
   * @param init - Function that creates the default state.
   */
  constructor(init) {
    this.init = init;
    this.records = this.init();
  }
  static {
    __name(this, "ImperativeState");
  }
  reset() {
    this.records = this.init();
  }
};

export {
  ImperativeState
};
//# sourceMappingURL=chunk-NY5VD3FT.js.map
