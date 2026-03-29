import {
  insertEdge,
  insertEdgeLabel,
  markers_default,
  positionEdgeLabel
} from "./chunk-O27ZXGXO.js";
import {
  insertCluster,
  insertNode,
  labelHelper
} from "./chunk-66JO2TML.js";
import {
  interpolateToCurve
} from "./chunk-BPNPFWKF.js";
import {
  __name,
  common_default,
  getConfig,
  log
} from "./chunk-GSAY5GLT.js";
import {
  __async
} from "./chunk-NXAKKGBW.js";

// node_modules/mermaid/dist/chunks/mermaid.core/chunk-TM57GTNK.mjs
var internalHelpers = {
  common: common_default,
  getConfig,
  insertCluster,
  insertEdge,
  insertEdgeLabel,
  insertMarkers: markers_default,
  insertNode,
  interpolateToCurve,
  labelHelper,
  log,
  positionEdgeLabel
};
var layoutAlgorithms = {};
var registerLayoutLoaders = __name((loaders) => {
  for (const loader of loaders) {
    layoutAlgorithms[loader.name] = loader;
  }
}, "registerLayoutLoaders");
var registerDefaultLayoutLoaders = __name(() => {
  registerLayoutLoaders([
    {
      name: "dagre",
      loader: __name(() => __async(null, null, function* () {
        return yield import("./dagre-SWNTG5WE-PNGGIY6J.js");
      }), "loader")
    }
  ]);
}, "registerDefaultLayoutLoaders");
registerDefaultLayoutLoaders();
var render = __name((data4Layout, svg) => __async(null, null, function* () {
  if (!(data4Layout.layoutAlgorithm in layoutAlgorithms)) {
    throw new Error(`Unknown layout algorithm: ${data4Layout.layoutAlgorithm}`);
  }
  const layoutDefinition = layoutAlgorithms[data4Layout.layoutAlgorithm];
  const layoutRenderer = yield layoutDefinition.loader();
  return layoutRenderer.render(data4Layout, svg, internalHelpers, {
    algorithm: layoutDefinition.algorithm
  });
}), "render");
var getRegisteredLayoutAlgorithm = __name((algorithm = "", { fallback = "dagre" } = {}) => {
  if (algorithm in layoutAlgorithms) {
    return algorithm;
  }
  if (fallback in layoutAlgorithms) {
    log.warn(`Layout algorithm ${algorithm} is not registered. Using ${fallback} as fallback.`);
    return fallback;
  }
  throw new Error(`Both layout algorithms ${algorithm} and ${fallback} are not registered.`);
}, "getRegisteredLayoutAlgorithm");

export {
  registerLayoutLoaders,
  render,
  getRegisteredLayoutAlgorithm
};
//# sourceMappingURL=chunk-CJVXW2U5.js.map
