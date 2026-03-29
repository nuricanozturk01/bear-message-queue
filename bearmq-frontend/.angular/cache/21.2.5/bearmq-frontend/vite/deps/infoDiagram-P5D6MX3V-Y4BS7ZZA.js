import {
  parse
} from "./chunk-XISEHBTP.js";
import "./chunk-STFPWVMT.js";
import "./chunk-4MXOG7R5.js";
import "./chunk-L5U72XPQ.js";
import "./chunk-ZGYP6DU3.js";
import "./chunk-CLOX3JZH.js";
import "./chunk-ISZED5UE.js";
import "./chunk-VBM6H7VM.js";
import {
  version
} from "./chunk-36A5CBOI.js";
import {
  selectSvgElement
} from "./chunk-XJ47M54B.js";
import {
  __name,
  configureSvgSize,
  log
} from "./chunk-GSAY5GLT.js";
import "./chunk-QVNUNGAT.js";
import "./chunk-QP2KWXU6.js";
import {
  __async
} from "./chunk-NXAKKGBW.js";

// node_modules/mermaid/dist/chunks/mermaid.core/infoDiagram-P5D6MX3V.mjs
var parser = {
  parse: __name((input) => __async(null, null, function* () {
    const ast = yield parse("info", input);
    log.debug(ast);
  }), "parse")
};
var DEFAULT_INFO_DB = { version };
var getVersion = __name(() => DEFAULT_INFO_DB.version, "getVersion");
var db = {
  getVersion
};
var draw = __name((text, id, version2) => {
  log.debug("rendering info diagram\n" + text);
  const svg = selectSvgElement(id);
  configureSvgSize(svg, 100, 400, true);
  const group = svg.append("g");
  group.append("text").attr("x", 100).attr("y", 40).attr("class", "version").attr("font-size", 32).style("text-anchor", "middle").text(`v${version2}`);
}, "draw");
var renderer = { draw };
var diagram = {
  parser,
  db,
  renderer
};
export {
  diagram
};
//# sourceMappingURL=infoDiagram-P5D6MX3V-Y4BS7ZZA.js.map
