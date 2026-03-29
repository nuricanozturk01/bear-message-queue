import {
  __name
} from "./chunk-ISZED5UE.js";
import {
  __async
} from "./chunk-NXAKKGBW.js";

// node_modules/@mermaid-js/parser/dist/mermaid-parser.core.mjs
var parsers = {};
var initializers = {
  info: __name(() => __async(null, null, function* () {
    const { createInfoServices: createInfoServices2 } = yield import("./info-46DW6VJ7-KQH5YEA3.js");
    const parser = createInfoServices2().Info.parser.LangiumParser;
    parsers.info = parser;
  }), "info"),
  packet: __name(() => __async(null, null, function* () {
    const { createPacketServices: createPacketServices2 } = yield import("./packet-W2GHVCYJ-ECOB47S3.js");
    const parser = createPacketServices2().Packet.parser.LangiumParser;
    parsers.packet = parser;
  }), "packet"),
  pie: __name(() => __async(null, null, function* () {
    const { createPieServices: createPieServices2 } = yield import("./pie-BEWT4RHE-WNNK5UN2.js");
    const parser = createPieServices2().Pie.parser.LangiumParser;
    parsers.pie = parser;
  }), "pie"),
  architecture: __name(() => __async(null, null, function* () {
    const { createArchitectureServices: createArchitectureServices2 } = yield import("./architecture-I3QFYML2-IFDYBW56.js");
    const parser = createArchitectureServices2().Architecture.parser.LangiumParser;
    parsers.architecture = parser;
  }), "architecture"),
  gitGraph: __name(() => __async(null, null, function* () {
    const { createGitGraphServices: createGitGraphServices2 } = yield import("./gitGraph-YCYPL57B-7VB6MB55.js");
    const parser = createGitGraphServices2().GitGraph.parser.LangiumParser;
    parsers.gitGraph = parser;
  }), "gitGraph")
};
function parse(diagramType, text) {
  return __async(this, null, function* () {
    const initializer = initializers[diagramType];
    if (!initializer) {
      throw new Error(`Unknown diagram type: ${diagramType}`);
    }
    if (!parsers[diagramType]) {
      yield initializer();
    }
    const parser = parsers[diagramType];
    const result = parser.parse(text);
    if (result.lexerErrors.length > 0 || result.parserErrors.length > 0) {
      throw new MermaidParseError(result);
    }
    return result.value;
  });
}
__name(parse, "parse");
var MermaidParseError = class extends Error {
  constructor(result) {
    const lexerErrors = result.lexerErrors.map((err) => err.message).join("\n");
    const parserErrors = result.parserErrors.map((err) => err.message).join("\n");
    super(`Parsing failed: ${lexerErrors} ${parserErrors}`);
    this.result = result;
  }
  static {
    __name(this, "MermaidParseError");
  }
};

export {
  parse
};
//# sourceMappingURL=chunk-XISEHBTP.js.map
