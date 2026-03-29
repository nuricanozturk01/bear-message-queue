import {
  __name
} from "./chunk-GSAY5GLT.js";

// node_modules/mermaid/dist/chunks/mermaid.core/chunk-BAOP5US2.mjs
function populateCommonDb(ast, db) {
  if (ast.accDescr) {
    db.setAccDescription?.(ast.accDescr);
  }
  if (ast.accTitle) {
    db.setAccTitle?.(ast.accTitle);
  }
  if (ast.title) {
    db.setDiagramTitle?.(ast.title);
  }
}
__name(populateCommonDb, "populateCommonDb");

export {
  populateCommonDb
};
//# sourceMappingURL=chunk-ATZDELQH.js.map
