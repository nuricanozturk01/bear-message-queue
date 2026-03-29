import {
  __name,
  getConfig2,
  select_default
} from "./chunk-GSAY5GLT.js";

// node_modules/mermaid/dist/chunks/mermaid.core/chunk-HK56VNYQ.mjs
var selectSvgElement = __name((id) => {
  const { securityLevel } = getConfig2();
  let root = select_default("body");
  if (securityLevel === "sandbox") {
    const sandboxElement = select_default(`#i${id}`);
    const doc = sandboxElement.node()?.contentDocument ?? document;
    root = select_default(doc.body);
  }
  const svg = root.select(`#${id}`);
  return svg;
}, "selectSvgElement");

export {
  selectSvgElement
};
//# sourceMappingURL=chunk-XJ47M54B.js.map
